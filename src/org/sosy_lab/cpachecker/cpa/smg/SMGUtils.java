/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.smg;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cpa.smg.SMGOptions.SMGExportLevel;
import org.sosy_lab.cpachecker.cpa.smg.graphs.UnmodifiableSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsToFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.exceptions.NoException;

/**
 * This class contains smg utilities, for example filters.
 */
public final class SMGUtils {

  private SMGUtils() {}

  public static Set<SMGEdgeHasValue> getFieldsOfObject(
      SMGObject pSmgObject, UnmodifiableSMG pInputSMG) {
    SMGEdgeHasValueFilter edgeFilter = SMGEdgeHasValueFilter.objectFilter(pSmgObject);
    return pInputSMG.getHVEdges(edgeFilter);
  }

  public static Set<SMGEdgePointsTo> getPointerToThisObject(
      SMGObject pSmgObject, UnmodifiableSMG pInputSMG) {
    SMGEdgePointsToFilter objectFilter = SMGEdgePointsToFilter.targetObjectFilter(pSmgObject);
    return pInputSMG.getPtEdges(objectFilter);
  }

  public static Set<SMGEdgeHasValue> getFieldsofThisValue(
      SMGValue value, UnmodifiableSMG pInputSMG) {
    SMGEdgeHasValueFilter valueFilter = SMGEdgeHasValueFilter.valueFilter(value);
    return pInputSMG.getHVEdges(valueFilter);
  }

  public static boolean isRecursiveOnOffset(CType pType, long fieldOffset, MachineModel pModel) {

    CFieldTypeVisitor v = new CFieldTypeVisitor(fieldOffset, pModel);

    CType typeAtOffset = pType.accept(v);

    if (CFieldTypeVisitor.isUnknownInstance(typeAtOffset)) {
      return false;
    }

    return pType.getCanonicalType().equals(typeAtOffset.getCanonicalType());
  }

  private static class CFieldTypeVisitor implements CTypeVisitor<CType, NoException> {

    private final long fieldOffset;
    private final MachineModel model;
    private static final CType UNKNOWN = new CSimpleType(false, false, CBasicType.UNSPECIFIED,
        false, false, false, false, false, false, false);

    public CFieldTypeVisitor(long pFieldOffset, MachineModel pModel) {
      fieldOffset = pFieldOffset;
      model = pModel;
    }

    public static boolean isUnknownInstance(CType type) {
      return type == UNKNOWN;
    }

    @Override
    public CType visit(CArrayType pArrayType) {
      if (fieldOffset % model.getSizeofInBits(pArrayType).longValueExact() == 0) {
        return pArrayType.getType();
      } else {
        return UNKNOWN;
      }
    }

    @Override
    public CType visit(CCompositeType pCompositeType) {

      List<CCompositeTypeMemberDeclaration> members = pCompositeType.getMembers();

      long memberOffset = 0;
      for (CCompositeTypeMemberDeclaration member : members) {

        if (fieldOffset == memberOffset) {
          return member.getType();
        } else if (memberOffset > fieldOffset) {
          return UNKNOWN;
        } else {
          memberOffset = memberOffset + model.getSizeofInBits(member.getType()).longValueExact();
        }
      }

      return UNKNOWN;
    }

    @Override
    public CType visit(CElaboratedType pElaboratedType) {
      return pElaboratedType.getRealType().accept(this);
    }

    @Override
    public CType visit(CEnumType pEnumType) {
      return UNKNOWN;
    }

    @Override
    public CType visit(CFunctionType pFunctionType) {
      return UNKNOWN;
    }

    @Override
    public CType visit(CPointerType pPointerType) {
      return pPointerType.getType().accept(this);
    }

    @Override
    public CType visit(CProblemType pProblemType) {
      return UNKNOWN;
    }

    @Override
    public CType visit(CSimpleType pSimpleType) {
      return UNKNOWN;
    }

    @Override
    public CType visit(CTypedefType pTypedefType) {
      return pTypedefType.getRealType();
    }

    @Override
    public CType visit(CVoidType pVoidType) {
      return UNKNOWN;
    }

    @Override
    public CType visit(CBitFieldType pCBitFieldType) {
      return pCBitFieldType.getType().accept(this);
    }
  }

  public static void plotWhenConfigured(
      String pSMGName,
      UnmodifiableSMGState pState,
      String pLocation,
      LogManager pLogger,
      SMGExportLevel pLevel,
      SMGExportDotOption pExportOption) {

    if (pExportOption.exportSMG(pLevel)) {
      dumpSMGPlot(pLogger, pSMGName, pState, pLocation, pExportOption);
    }
  }

  private static void dumpSMGPlot(
      LogManager pLogger,
      String pSMGName,
      UnmodifiableSMGState pCurrentState,
      String pLocation,
      SMGExportDotOption pExportOption) {
    if (pCurrentState != null && pExportOption.hasExportPath()) {
      Path outputFile = pExportOption.getOutputFilePath(pSMGName);
      dumpSMGPlot(pLogger, pCurrentState, pLocation, outputFile);
    }
  }

  public static void dumpSMGPlot(
      LogManager pLogger, UnmodifiableSMGState currentState, String location, Path pOutputFile) {
    try {
      String dot = currentState.toDot("SMG" + currentState.getId(), location);
      IO.writeFile(pOutputFile, Charset.defaultCharset(), dot);
    } catch (IOException e) {
      pLogger.logUserException(Level.WARNING, e, "Could not write SMG " + currentState.getId() + " to file");
    }
  }
}
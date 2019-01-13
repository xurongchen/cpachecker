/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSArrayLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBooleanLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBracketPropertyAccess;
import org.sosy_lab.cpachecker.cfa.ast.js.JSDeclaredByExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFieldAccess;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSNullLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSObjectLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSObjectLiteralField;
import org.sosy_lab.cpachecker.cfa.ast.js.JSRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.js.JSRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.js.JSSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSThisExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSUndefinedLiteralExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.smt.FloatingPointFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

/** Management of formula encoding of JavaScript expressions. */
@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class ExpressionToFormulaVisitor extends ManagerWithEdgeContext
    implements JSExpressionFormulaManager,
        JSRightHandSideVisitor<TypedValue, UnrecognizedCodeException> {

  ExpressionToFormulaVisitor(final EdgeManagerContext pCtx) {
    super(pCtx);
  }

  @Override
  public TypedValue visit(final JSFunctionCallExpression pFunctionCallExpression)
      throws UnrecognizedCodeException {
    throw new UnrecognizedCodeException(
        "JSFunctionCallExpression not implemented yet", pFunctionCallExpression);
  }

  @Override
  public TypedValue makeExpression(final JSRightHandSide pExpression)
      throws UnrecognizedCodeException {
    return pExpression.accept(this);
  }

  @Override
  public TypedValue visit(final JSBinaryExpression pBinaryExpression)
      throws UnrecognizedCodeException {
    final TypedValue leftOperand = visit(pBinaryExpression.getOperand1());
    final TypedValue rightOperand = visit(pBinaryExpression.getOperand2());
    switch (pBinaryExpression.getOperator()) {
      case EQUALS:
        // Treat == like === until differences between these operators are implemented.
        // It might lead to false positives in some cases, but it allows to analyze many real world
        // programs that could not be analyzed otherwise right now.
      case EQUAL_EQUAL_EQUAL:
        return tvmgr.createBooleanValue(makeEqual(leftOperand, rightOperand));
      case NOT_EQUALS:
        // Treat != like !== until differences between these operators are implemented.
        // It might lead to false positives in some cases, but it allows to analyze many real world
        // programs that could not be analyzed otherwise right now.
      case NOT_EQUAL_EQUAL:
        return tvmgr.createBooleanValue(fmgr.makeNot(makeEqual(leftOperand, rightOperand)));
      case PLUS:
        return tvmgr.createNumberValue(
            fpfmgr.add(valConv.toNumber(leftOperand), valConv.toNumber(rightOperand)));
      case MINUS:
        return tvmgr.createNumberValue(
            fpfmgr.subtract(valConv.toNumber(leftOperand), valConv.toNumber(rightOperand)));
      case TIMES:
        return tvmgr.createNumberValue(
            fpfmgr.multiply(valConv.toNumber(leftOperand), valConv.toNumber(rightOperand)));
      case DIVIDE:
        return tvmgr.createNumberValue(
            fpfmgr.divide(valConv.toNumber(leftOperand), valConv.toNumber(rightOperand)));
      case REMAINDER:
        return tvmgr.createNumberValue(makeRemainder(leftOperand, rightOperand));
      case LESS:
        return tvmgr.createBooleanValue(
            fpfmgr.lessThan(valConv.toNumber(leftOperand), valConv.toNumber(rightOperand)));
      case LESS_EQUALS:
        return tvmgr.createBooleanValue(
            fpfmgr.lessOrEquals(valConv.toNumber(leftOperand), valConv.toNumber(rightOperand)));
      case GREATER:
        return tvmgr.createBooleanValue(
            fpfmgr.greaterThan(valConv.toNumber(leftOperand), valConv.toNumber(rightOperand)));
      case GREATER_EQUALS:
        return tvmgr.createBooleanValue(
            fpfmgr.greaterOrEquals(valConv.toNumber(leftOperand), valConv.toNumber(rightOperand)));
      default:
        throw new UnrecognizedCodeException(
            "JSBinaryExpression not implemented yet", pBinaryExpression);
    }
  }

  @Nonnull
  private FloatingPointFormula makeRemainder(
      final TypedValue pLeftOperand, final TypedValue pRightOperand) {
    final FloatingPointFormulaManagerView f = fpfmgr;
    final FloatingPointFormula dividend = valConv.toNumber(pLeftOperand);
    final FloatingPointFormula divisor = valConv.toNumber(pRightOperand);
    final BooleanFormula nanCase =
        jsOptions.useNaN ? bfmgr.or(f.isNaN(dividend), f.isNaN(divisor)) : bfmgr.makeFalse();
    return bfmgr.ifThenElse(
        bfmgr.or(nanCase, f.isInfinity(dividend), f.isZero(divisor)),
        f.makeNaN(Types.NUMBER_TYPE),
        bfmgr.ifThenElse(bfmgr.or(f.isInfinity(divisor), f.isZero(dividend)), dividend, dividend));
  }

  @Nonnull
  private BooleanFormula makeEqual(final TypedValue pLeftOperand, final TypedValue pRightOperand) {
    final IntegerFormula leftType = pLeftOperand.getType();
    final IntegerFormula rightType = pRightOperand.getType();
    final BooleanFormula nanCase =
        jsOptions.useNaN
            ? bfmgr.and(
                fmgr.makeNot(fpfmgr.isNaN(valConv.toNumber(pLeftOperand))),
                fmgr.makeNot(fpfmgr.isNaN(valConv.toNumber(pRightOperand))))
            : bfmgr.makeTrue();
    return fmgr.makeAnd(
        fmgr.makeEqual(leftType, rightType),
        bfmgr.or(
            fmgr.makeEqual(typeTags.UNDEFINED, leftType),
            bfmgr.and(
                fmgr.makeEqual(typeTags.NUMBER, leftType),
                nanCase,
                fmgr.makeEqual(valConv.toNumber(pLeftOperand), valConv.toNumber(pRightOperand))),
            fmgr.makeAnd(
                fmgr.makeEqual(typeTags.BOOLEAN, leftType),
                fmgr.makeEqual(valConv.toBoolean(pLeftOperand), valConv.toBoolean(pRightOperand))),
            fmgr.makeAnd(
                fmgr.makeEqual(typeTags.OBJECT, leftType),
                fmgr.makeEqual(valConv.toObject(pLeftOperand), valConv.toObject(pRightOperand))),
            fmgr.makeAnd(
                fmgr.makeEqual(typeTags.STRING, leftType),
                fmgr.makeEqual(
                    valConv.toStringFormula(pLeftOperand), valConv.toStringFormula(pRightOperand))),
            fmgr.makeAnd(
                fmgr.makeEqual(typeTags.FUNCTION, leftType),
                fmgr.makeEqual(
                    valConv.toFunction(pLeftOperand), valConv.toFunction(pRightOperand)))));
  }

  @Override
  public TypedValue visit(final JSStringLiteralExpression pStringLiteralExpression) {
    return tvmgr.createStringValue(strMgr.getStringFormula(pStringLiteralExpression.getValue()));
  }

  @Override
  public TypedValue visit(final JSFloatLiteralExpression pLiteral) {
    return makeNumber(pLiteral.getValue());
  }

  @Nonnull
  private TypedValue makeNumber(final BigDecimal pValue) {
    return tvmgr.createNumberValue(
        fpfmgr.makeNumber(pValue, FormulaType.getDoublePrecisionFloatingPointType()));
  }

  @Override
  public TypedValue visit(final JSUnaryExpression pUnaryExpression)
      throws UnrecognizedCodeException {
    final TypedValue operand = visit(pUnaryExpression.getOperand());
    switch (pUnaryExpression.getOperator()) {
      case NOT:
        return tvmgr.createBooleanValue(fmgr.makeNot(valConv.toBoolean(operand)));
      case PLUS:
        return tvmgr.createNumberValue(valConv.toNumber(operand));
      case MINUS:
        return tvmgr.createNumberValue(fmgr.makeNegate(valConv.toNumber(operand)));
      case VOID:
        return tvmgr.getUndefinedValue();
      default:
        throw new UnrecognizedCodeException(
            "JSUnaryExpression not implemented yet", pUnaryExpression);
    }
  }

  @Override
  public TypedValue visit(final JSIntegerLiteralExpression pIntegerLiteralExpression) {
    return makeNumber(new BigDecimal(pIntegerLiteralExpression.getValue()));
  }

  @Override
  public TypedValue visit(final JSBooleanLiteralExpression pBooleanLiteralExpression) {
    return tvmgr.createBooleanValue(bfmgr.makeBoolean(pBooleanLiteralExpression.getValue()));
  }

  @Override
  public TypedValue visit(final JSNullLiteralExpression pNullLiteralExpression) {
    return tvmgr.getNullValue();
  }

  @Override
  public TypedValue visit(final JSObjectLiteralExpression pObjectLiteralExpression)
      throws UnrecognizedCodeException {
    return ctx.objMgr.createObject(pObjectLiteralExpression);
  }

  @Override
  public TypedValue visit(final JSArrayLiteralExpression pArrayLiteralExpression)
      throws UnrecognizedCodeException {
    final IntegerFormula objectId = objIdMgr.createObjectId();
    final TypedValue objectValue = tvmgr.createObjectValue(objectId);
    // TODO assign elements to new array object
    final List<JSExpression> elements = pArrayLiteralExpression.getElements();
    final JSObjectLiteralField lengthField =
        new JSObjectLiteralField(
            "length",
            new JSIntegerLiteralExpression(
                FileLocation.DUMMY, BigInteger.valueOf(elements.size())));
    ctx.objMgr.setObjectFields(objectId, Collections.singletonList(lengthField));
    return objectValue;
  }

  @Override
  public TypedValue visit(final JSUndefinedLiteralExpression pUndefinedLiteralExpression) {
    return tvmgr.getUndefinedValue();
  }

  @Override
  public TypedValue visit(final JSThisExpression pThisExpression) throws UnrecognizedCodeException {
    throw new UnrecognizedCodeException("JSThisExpression not implemented yet", pThisExpression);
  }

  @Override
  public TypedValue visit(final JSDeclaredByExpression pDeclaredByExpression) {
    // TODO move code of case to JSFunctionDeclarationFormulaManager
    assert pDeclaredByExpression.getIdExpression().getDeclaration() != null;
    final IntegerFormula variable =
        ctx.scopeMgr.scopedVariable(pDeclaredByExpression.getIdExpression().getDeclaration());
    final IntegerFormula functionDeclarationId =
        fmgr.makeNumber(
            Types.FUNCTION_DECLARATION_TYPE,
            functionDeclarationIds.get(pDeclaredByExpression.getJsFunctionDeclaration()));
    return tvmgr.createBooleanValue(
        fmgr.makeEqual(
            jsFunDeclMgr.declarationOf(typedVarValues.functionValue(variable)),
            functionDeclarationId));
  }

  @Override
  public TypedValue visit(final JSIdExpression pIdExpression) throws UnrecognizedCodeException {
    final JSSimpleDeclaration declaration = pIdExpression.getDeclaration();
    if (declaration == null) {
      return handlePredefined(pIdExpression);
    } else if (declaration instanceof JSFunctionDeclaration) {
      // TODO move code of case to JSFunctionDeclarationFormulaManager
      final JSFunctionDeclaration functionDeclaration = (JSFunctionDeclaration) declaration;
      final IntegerFormula functionDeclarationId =
          fmgr.makeNumber(Types.FUNCTION_TYPE, functionDeclarationIds.get(functionDeclaration));
      final IntegerFormula functionValueFormula =
          functionDeclaration.isGlobal()
              ? functionDeclarationId
              : ctx.scopeMgr.scopedVariable(functionDeclaration);
      ctx.constraints.addConstraint(
          fmgr.makeEqual(jsFunDeclMgr.declarationOf(functionValueFormula), functionDeclarationId));
      // TODO function might be declared outside of current function
      ctx.constraints.addConstraint(
          fmgr.makeEqual(
              ctx.scopeMgr.scopeOf(functionValueFormula), ctx.scopeMgr.getCurrentScope()));
      return tvmgr.createFunctionValue(functionValueFormula);
    }
    final IntegerFormula variable = ctx.scopeMgr.scopedVariable(declaration);
    return new TypedValue(typedVarValues.typeof(variable), variable);
  }

  @Override
  public TypedValue visit(final JSFieldAccess pFieldAccess) throws UnrecognizedCodeException {
    final JSSimpleDeclaration objectDeclaration =
        ctx.propMgr.getObjectDeclarationOfFieldAccess(pFieldAccess);
    final IntegerFormula objectId =
        typedVarValues.objectValue(ctx.scopeMgr.scopedVariable(objectDeclaration));
    final IntegerFormula fieldName = strMgr.getStringFormula(pFieldAccess.getFieldName());
    return ctx.propMgr.accessField(objectId, fieldName);
  }

  @Override
  public TypedValue visit(final JSBracketPropertyAccess pPropertyAccess)
      throws UnrecognizedCodeException {
    final JSSimpleDeclaration objectDeclaration =
        ctx.propMgr.getObjectDeclarationOfObjectExpression(pPropertyAccess.getObjectExpression());
    final IntegerFormula objectId =
        typedVarValues.objectValue(ctx.scopeMgr.scopedVariable(objectDeclaration));
    final JSExpression propertyNameExpression = pPropertyAccess.getPropertyNameExpression();
    final TypedValue propertyNameValue = visit(propertyNameExpression);
    final IntegerFormula fieldName =
        (propertyNameExpression instanceof JSStringLiteralExpression)
            ? strMgr.getStringFormula(
                ((JSStringLiteralExpression) propertyNameExpression).getValue())
            : valConv.toStringFormula(propertyNameValue);
    final TypedValue propertyValue = ctx.propMgr.accessField(objectId, fieldName);
    final TypedValue lengthProperty =
        ctx.propMgr.accessField(objectId, strMgr.getStringFormula("length"));
    // TODO check if object is an array (otherwise `length` is no indicator if property is defined)
    final BooleanFormula isUndefinedArrayElementIndex =
        bfmgr.and(
            fmgr.makeEqual(propertyNameValue.getType(), typeTags.NUMBER),
            fpfmgr.greaterOrEquals(
                valConv.toNumber(propertyNameValue), valConv.toNumber(lengthProperty)));
    return tvmgr.ifThenElse(isUndefinedArrayElementIndex, tvmgr.getUndefinedValue(), propertyValue);
  }

  private TypedValue handlePredefined(final JSIdExpression pIdExpression)
      throws UnrecognizedCodeException {
    final String name = pIdExpression.getName();
    switch (name) {
      case "Infinity":
        return tvmgr.createNumberValue(fpfmgr.makePlusInfinity(Types.NUMBER_TYPE));
      case "NaN":
        if (!jsOptions.useNaN) {
          logger.log(
              Level.WARNING,
              "NaN is used in "
                  + pIdExpression.getFileLocation()
                  + " even though cpa.predicate.js.useNaN=false");
        }
        return tvmgr.createNumberValue(fpfmgr.makeNaN(Types.NUMBER_TYPE));
      default:
        throw new UnrecognizedCodeException(
            "Variable without declaration is not defined on global object", pIdExpression);
    }
  }

}

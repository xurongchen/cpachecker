/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.ast;


import java.util.Objects;

import org.sosy_lab.cpachecker.cfa.types.Type;

/**
 *
 * Abstract Super class for all possible right-hand sides of an assignment.
 * This class is only SuperClass of all abstract Classes and their Subclasses.
 * The Interface {@link IARightHandSide} contains all language specific
 * AST Nodes as well.
 */
public  abstract class ALeftHandSide extends AExpression implements IALeftHandSide {

  private final Type type;

  public ALeftHandSide(FileLocation pFileLocation, Type pType) {
    super(pFileLocation, pType);
    type = pType;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(type);
    result = prime * result + super.hashCode();
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof ALeftHandSide)
        || !super.equals(obj)) {
      return false;
    }

    ALeftHandSide other = (ALeftHandSide) obj;

    return Objects.equals(other.type, type);
  }


}

/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.lock;

import com.google.common.base.Preconditions;
import java.util.Objects;
import org.sosy_lab.cpachecker.core.interfaces.InferenceObject;


public class LockInferenceObject implements InferenceObject {

  private final AbstractLockState state;

  private LockInferenceObject(AbstractLockState pState) {
    Preconditions.checkNotNull(pState);
    state = pState;
  }

  public static LockInferenceObject create(AbstractLockState pState) {
    return new LockInferenceObject(pState.prepareToStore());
  }

  public AbstractLockState getState() {
    return state;
  }

  @Override
  public boolean hasEmptyAction() {
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Objects.hashCode(state);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null ||
        getClass() != obj.getClass()) {
      return false;
    }
    LockInferenceObject other = (LockInferenceObject) obj;
    return Objects.equals(state, other.state);
  }


}
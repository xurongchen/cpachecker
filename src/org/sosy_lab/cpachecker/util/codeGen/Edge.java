/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.codeGen;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

public class Edge {
  
  private ARGState parent;
  private ARGState child;
  private CFAEdge edge;
  
  public Edge(ARGState parent, ARGState child) {
    this.parent = parent;
    this.child = child;
    this.edge = parent.getEdgeToChild(child);
  }
  
  public ARGState getParent() {
    return parent;
  }

  public ARGState getChild() {
    return child;
  }

  public CFAEdge getEdge() {
    return edge;
  }
}

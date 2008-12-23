/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
/**
 *
 */
package programtesting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Map.Entry;

import predicateabstraction.ThreeValuedBoolean;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAEdgeType;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.CallToReturnEdge;

import common.Pair;
import compositeCPA.CompositeMergeOperator;
import compositeCPA.CompositePrecision;
import compositeCPA.CompositePrecisionAdjustment;
import compositeCPA.CompositeStopOperator;

import cpa.common.CPAAlgorithm;
import cpa.common.CallElement;
import cpa.common.CallStack;
import cpa.common.CompositeDomain;
import cpa.common.CompositeElement;
import cpa.common.automaton.Automaton;
import cpa.common.automaton.AutomatonCPADomain;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import cpa.common.interfaces.Precision;
import cpa.location.LocationCPA;
import cpa.scoperestriction.ScopeRestrictionCPA;
import cpa.symbpredabs.CounterexampleTraceInfo;
import cpa.symbpredabs.SymbolicFormulaManager;
import cpa.symbpredabs.explicit.ExplicitAbstractElement;
import cpa.symbpredabs.explicit.ExplicitAbstractFormulaManager;
import cpa.symbpredabs.explicit.ExplicitCPA;
import cpa.symbpredabs.explicit.ExplicitTransferRelation;
import cpa.testgoal.TestGoalCPA;
import cpa.testgoal.TestGoalCPA.TestGoalPrecision;
import exceptions.CPAException;
import exceptions.CPATransferException;
import exceptions.RefinementNeededException;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class QueryDrivenProgramTesting {
  
  public static class MyCompositeCPA implements ConfigurableProgramAnalysis {

    public class CompositeTransferRelation implements TransferRelation{

      private final CompositeDomain compositeDomain;
      private final List<TransferRelation> transferRelations;

      // private LocationTransferRelation locationTransferRelation;

      public CompositeTransferRelation (CompositeDomain compositeDomain, List<TransferRelation> transferRelations)
      {
        this.compositeDomain = compositeDomain;
        this.transferRelations = transferRelations;

        //TransferRelation first = transferRelations.get (0);
        //if (first instanceof LocationTransferRelation)
        //{
        //  locationTransferRelation = (LocationTransferRelation) first;
        //}
      }

      public AbstractDomain getAbstractDomain ()
      {
        return compositeDomain;
      }

      public AbstractElement getAbstractSuccessor (AbstractElement element, CFAEdge cfaEdge, Precision precision) throws CPATransferException
      {
        assert(precision instanceof CompositePrecision);
        CompositePrecision lCompositePrecision = (CompositePrecision)precision;
        
        CompositeElement compositeElement = (CompositeElement) element;
        List<AbstractElement> inputElements = compositeElement.getElements ();
        List<AbstractElement> resultingElements = new ArrayList<AbstractElement> ();

        CallStack updatedCallStack = compositeElement.getCallStack();

        // TODO add some check here for unbounded recursive calls
        if(cfaEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge)
        {
          String functionName = cfaEdge.getSuccessor().getFunctionName();
          CFANode callNode = cfaEdge.getPredecessor();
          CallElement ce = new CallElement(functionName, callNode, compositeElement);
          CallStack cs = compositeElement.getCallStack();
          updatedCallStack = cs.clone();
          updatedCallStack.push(ce);
        }

        // handling the return from a function
        else if(cfaEdge.getEdgeType() == CFAEdgeType.ReturnEdge)
        {
          CallElement topCallElement = compositeElement.getCallStack().peek();
          assert(cfaEdge.getPredecessor().getFunctionName().
              equals(topCallElement.getFunctionName()));
          CallElement returnElement = compositeElement.getCallStack().getSecondTopElement();

          if(! topCallElement.isConsistent(cfaEdge.getSuccessor()) ||
              ! returnElement.isConsistent(cfaEdge.getSuccessor().getFunctionName()) ){
            return compositeDomain.getBottomElement();
          }

          // TODO we are saving the abstract state on summary edge, that works for
          // now but this is a terrible design practice. Add another method
          // getAbstractSuccessorOnReturn(subElement, prevElement, cfaEdge)
          // and implement it for all CPAs later.
          else{
            CallStack cs = compositeElement.getCallStack();
            updatedCallStack = cs.clone();
            CallElement ce = updatedCallStack.pop();
            CompositeElement compElemBeforeCall = ce.getState();
            // TODO use summary edge as a cache later
            CallToReturnEdge summaryEdge = cfaEdge.getSuccessor().getEnteringSummaryEdge();
            summaryEdge.setAbstractElement(compElemBeforeCall);
          }
        }

        for (int idx = 0; idx < transferRelations.size (); idx++)
        {
          TransferRelation transfer = transferRelations.get (idx);
          AbstractElement subElement = null;
          AbstractElement successor = null;
          subElement = inputElements.get (idx);
          // handling a call edge

          Precision lPresicion = lCompositePrecision.get(idx);
          
          successor = transfer.getAbstractSuccessor (subElement, cfaEdge, lPresicion);
          
          // as soon as a component returns bottom we return composite bottom
          if (compositeDomain.getDomains().get(idx).getBottomElement().equals(successor)) {
            return compositeDomain.getBottomElement();
          }
          
          resultingElements.add (successor);
        }

        CompositeElement successorState = new CompositeElement (resultingElements, updatedCallStack);
        return successorState;
      }

      public List<AbstractElementWithLocation> getAllAbstractSuccessors(AbstractElementWithLocation element, Precision precision) throws CPAException, CPATransferException
      {

        //TODO CPACheckerStatistics.noOfTransferRelations++;

        CompositeElement compositeElement = (CompositeElement) element;
        CFANode node = compositeElement.getLocationNode();

        List<AbstractElementWithLocation> results = new ArrayList<AbstractElementWithLocation> ();

        for (int edgeIdx = 0; edgeIdx < node.getNumLeavingEdges (); edgeIdx++)
        {
          CFAEdge edge = node.getLeavingEdge (edgeIdx);
          results.add ((CompositeElement) getAbstractSuccessor (element, edge, precision));
        }

        return results;
      }
    }
    
    private final CompositeDomain mDomain;
    private final TransferRelation mTransferRelation;
    private final MergeOperator mMergeOperator;
    private final StopOperator mStopOperator;
    private final PrecisionAdjustment mPrecisionAdjustment;
    private final CompositeElement mInitialElement;
    private final Precision mInitialPrecision;
    
    public MyCompositeCPA(List<ConfigurableProgramAnalysis> cpas, CFAFunctionDefinitionNode node) {
      List<AbstractDomain> domains = new ArrayList<AbstractDomain> ();
      List<TransferRelation> transferRelations = new ArrayList<TransferRelation> ();
      List<MergeOperator> mergeOperators = new ArrayList<MergeOperator> ();
      List<StopOperator> stopOperators = new ArrayList<StopOperator> ();
      List<PrecisionAdjustment> precisionAdjustments = new ArrayList<PrecisionAdjustment> ();
      List<AbstractElement> initialElements = new ArrayList<AbstractElement> ();
      List<Precision> initialPrecisions = new ArrayList<Precision> ();

      for(ConfigurableProgramAnalysis sp : cpas) {
        domains.add(sp.getAbstractDomain());
        transferRelations.add(sp.getTransferRelation());
        mergeOperators.add(sp.getMergeOperator());
        stopOperators.add(sp.getStopOperator());
        precisionAdjustments.add(sp.getPrecisionAdjustment());
        initialElements.add(sp.getInitialElement(node));
        initialPrecisions.add(sp.getInitialPrecision(node));
      }

      mDomain = new CompositeDomain(domains);
      mTransferRelation = new CompositeTransferRelation(mDomain, transferRelations);
      mMergeOperator = new CompositeMergeOperator(mDomain, mergeOperators);
      mStopOperator = new CompositeStopOperator(mDomain, stopOperators);
      mPrecisionAdjustment = new CompositePrecisionAdjustment(precisionAdjustments);
      mInitialElement = new CompositeElement(initialElements, null);
      mInitialPrecision = new CompositePrecision(initialPrecisions);
      
      // set call stack
      CallStack initialCallStack = new CallStack();
      CallElement initialCallElement = new CallElement(node.getFunctionName(), node, mInitialElement);
      initialCallStack.push(initialCallElement);
      mInitialElement.setCallStack(initialCallStack);
    }
    
    @Override
    public AbstractDomain getAbstractDomain() {
      return mDomain;
    }

    @Override
    public CompositeElement getInitialElement(CFAFunctionDefinitionNode pNode) {
      // TODO create an initial element
      return mInitialElement;
    }

    @Override
    public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
      return mInitialPrecision;
    }

    @Override
    public MergeOperator getMergeOperator() {
      return mMergeOperator;
    }

    @Override
    public PrecisionAdjustment getPrecisionAdjustment() {
      return mPrecisionAdjustment;
    }

    @Override
    public StopOperator getStopOperator() {
      return mStopOperator;
    }

    @Override
    public TransferRelation getTransferRelation() {
      return mTransferRelation;
    }
    
  }
  
  public static class MyCollection implements Collection<Pair<AbstractElementWithLocation,Precision>> {
    class MyIterator implements Iterator<Pair<AbstractElementWithLocation,Precision>> {
      private Iterator<Pair<AbstractElementWithLocation,Precision>> lInnerIterator;
      
      private int lMapIndex;
      
      public MyIterator() {
        lMapIndex = TOP_INDEX;
        lInnerIterator = null;
      }
      
      private void init() {
        if (lInnerIterator == null) {
          lInnerIterator = mMap.get(lMapIndex).iterator();
        }
      }
      
      @Override
      public boolean hasNext() {
        init();
        
        if (lInnerIterator.hasNext()) {
          return true;
        }
        
        if (lMapIndex > BOTTOM_INDEX) {
          lMapIndex--;
          lInnerIterator = mMap.get(lMapIndex).iterator();
          
          return hasNext();
        }
        
        return false;
      }

      @Override
      public Pair<AbstractElementWithLocation,Precision> next() {
        init();
        
        if (hasNext()) {
          return lInnerIterator.next();
        }

        throw new NoSuchElementException();
      }

      @Override
      public void remove() {
        init();
        
        lInnerIterator.remove();
      }
      
    }
    
    private Map<Integer, Set<Pair<AbstractElementWithLocation,Precision>>> mMap;
    private AutomatonCPADomain<CFAEdge> mAutomatonDomain;
    
    private final int TOP_INDEX;
    private final int BOTTOM_INDEX;
    
    public MyCollection(AutomatonCPADomain<CFAEdge> pAutomatonDomain) {
      assert(pAutomatonDomain != null);
      
      mAutomatonDomain = pAutomatonDomain;
      
      mMap = new HashMap<Integer, Set<Pair<AbstractElementWithLocation,Precision>>>();
      
      // top
      assert(pAutomatonDomain.getAutomaton().getFinalStates().size() < Integer.MAX_VALUE);
      TOP_INDEX = pAutomatonDomain.getAutomaton().getFinalStates().size() + 1;
      mMap.put(TOP_INDEX, new HashSet<Pair<AbstractElementWithLocation,Precision>>());
      
      for (int i = 0; i <= pAutomatonDomain.getAutomaton().getFinalStates().size(); i++) {
        mMap.put(i, new HashSet<Pair<AbstractElementWithLocation,Precision>>());
      }
      
      // bottom
      BOTTOM_INDEX = -1;
      mMap.put(BOTTOM_INDEX, new HashSet<Pair<AbstractElementWithLocation,Precision>>());
    }
    
    @Override
    public boolean add(Pair<AbstractElementWithLocation,Precision> pE) {
      assert(pE != null);
      assert(pE.getFirst() != null);
      assert(pE.getFirst() instanceof CompositeElement);
      
      CompositeElement lCompositeElement = (CompositeElement)pE.getFirst();
      
      AbstractElement lTmpElement = lCompositeElement.get(mTestGoalCPAIndex);
      
      if (mAutomatonDomain.getBottomElement().equals(lTmpElement)) {
        Set<Pair<AbstractElementWithLocation,Precision>> lSet = mMap.get(TOP_INDEX);
        
        assert(lSet != null);
        
        return lSet.add(pE);
      }
      
      if (mAutomatonDomain.getBottomElement().equals(lTmpElement)) {
        Set<Pair<AbstractElementWithLocation,Precision>> lSet = mMap.get(BOTTOM_INDEX);
        
        assert(lSet != null);
        
        return lSet.add(pE);
      }
      
      AutomatonCPADomain<CFAEdge>.StateSetElement lTestGoalCPAElement = mAutomatonDomain.castToStateSetElement(lTmpElement);
      
      final Set<Automaton<CFAEdge>.State> lStates = lTestGoalCPAElement.getStates();
      
      int lNumberOfFinalStates = 0;
      
      for (Automaton<CFAEdge>.State lState : lStates) {
        if (lState.isFinal()) {
          lNumberOfFinalStates++;
        }
      }
      
      Set<Pair<AbstractElementWithLocation,Precision>> lSet = mMap.get(lNumberOfFinalStates);
      
      assert(lSet != null);
      
      return lSet.add(pE);
    }

    @Override
    public boolean addAll(Collection<? extends Pair<AbstractElementWithLocation,Precision>> pC) {
      assert(pC != null);
      
      boolean lWasChanged = false;
      
      for (Pair<AbstractElementWithLocation,Precision> lElement : pC) {
        if (add(lElement)) {
          lWasChanged = true;
        }
      }
      
      return lWasChanged;
    }

    @Override
    public void clear() {
      for (Map.Entry<Integer, Set<Pair<AbstractElementWithLocation,Precision>>> lEntry : mMap.entrySet()) {
        lEntry.getValue().clear();
      }
    }

    @Override
    public boolean contains(Object pO) {
      assert(pO != null);
      assert(pO instanceof Pair<?,?>);
      
      Pair<AbstractElementWithLocation,Precision> lPair = (Pair<AbstractElementWithLocation,Precision>)pO;
      
      assert(lPair.getFirst() instanceof CompositeElement);
      
      CompositeElement lCompositeElement = (CompositeElement)lPair.getFirst();
      
      AbstractElement lTmpElement = lCompositeElement.get(mTestGoalCPAIndex);
      
      if (mAutomatonDomain.getBottomElement().equals(lTmpElement)) {
        Set<Pair<AbstractElementWithLocation,Precision>> lSet = mMap.get(TOP_INDEX);
        
        assert(lSet != null);
        
        return lSet.contains(pO);
      }
      
      if (mAutomatonDomain.getBottomElement().equals(lTmpElement)) {
        Set<Pair<AbstractElementWithLocation,Precision>> lSet = mMap.get(BOTTOM_INDEX);
        
        assert(lSet != null);
        
        return lSet.contains(pO);
      }
      
      AutomatonCPADomain<CFAEdge>.StateSetElement lTestGoalCPAElement = mAutomatonDomain.castToStateSetElement(lTmpElement);
      
      final Set<Automaton<CFAEdge>.State> lStates = lTestGoalCPAElement.getStates();
      
      int lNumberOfFinalStates = 0;
      
      for (Automaton<CFAEdge>.State lState : lStates) {
        if (lState.isFinal()) {
          lNumberOfFinalStates++;
        }
      }
      
      Set<Pair<AbstractElementWithLocation,Precision>> lSet = mMap.get(lNumberOfFinalStates);
      
      assert(lSet != null);
      
      return lSet.contains(pO);
    }

    @Override
    public boolean containsAll(Collection<?> pC) {
      assert(pC != null);
      
      for (Object lObject : pC) {
        if (!contains(lObject)) {
          return false;
        }
      }
      
      return true;
    }

    @Override
    public boolean isEmpty() {
      for (Map.Entry<Integer, Set<Pair<AbstractElementWithLocation,Precision>>> lEntry : mMap.entrySet()) {
        if (!lEntry.getValue().isEmpty()) {
          return false;
        }
      }
      
      return true;
    }

    @Override
    public Iterator<Pair<AbstractElementWithLocation,Precision>> iterator() {
      return new MyIterator();
    }

    @Override
    public boolean remove(Object pO) {
      assert(pO != null);
      
      assert(pO instanceof CompositeElement);
      
      CompositeElement lCompositeElement = (CompositeElement)pO;
      
      AbstractElement lTmpElement = lCompositeElement.get(mTestGoalCPAIndex);
      
      if (mAutomatonDomain.getBottomElement().equals(lTmpElement)) {
        Set<Pair<AbstractElementWithLocation,Precision>> lSet = mMap.get(TOP_INDEX);
        
        assert(lSet != null);
        
        return lSet.remove(pO);
      }
      
      if (mAutomatonDomain.getBottomElement().equals(lTmpElement)) {
        Set<Pair<AbstractElementWithLocation,Precision>> lSet = mMap.get(BOTTOM_INDEX);
        
        assert(lSet != null);
        
        return lSet.remove(pO);
      }
      
      AutomatonCPADomain<CFAEdge>.StateSetElement lTestGoalCPAElement = mAutomatonDomain.castToStateSetElement(lTmpElement);
      
      final Set<Automaton<CFAEdge>.State> lStates = lTestGoalCPAElement.getStates();
      
      int lNumberOfFinalStates = 0;
      
      for (Automaton<CFAEdge>.State lState : lStates) {
        if (lState.isFinal()) {
          lNumberOfFinalStates++;
        }
      }
      
      Set<Pair<AbstractElementWithLocation,Precision>> lSet = mMap.get(lNumberOfFinalStates);
      
      assert(lSet != null);
      
      return lSet.remove(pO);
    }

    @Override
    public boolean removeAll(Collection<?> pC) {
      assert(pC != null);
      
      boolean lWasChanged = false;
      
      for (Object lObject : pC) {
        if (remove(lObject)) {
          lWasChanged = true;
        }
      }
      
      return lWasChanged;
    }

    @Override
    public boolean retainAll(Collection<?> pC) {
      assert(pC != null);
      
      boolean lWasChanged = false;
      
      for (Pair<AbstractElementWithLocation,Precision> lElement : this) {
        if (!pC.contains(lElement)) {
          if (remove(lElement)) {
            lWasChanged = true;
          }
        }
      }
      
      return lWasChanged;
    }

    @Override
    public int size() {
      int lSize = 0;
      
      for (Map.Entry<Integer, Set<Pair<AbstractElementWithLocation,Precision>>> lEntry : mMap.entrySet()) {
        lSize += lEntry.getValue().size();
      }
      
      return lSize;
    }

    @Override
    public Object[] toArray() {
      int lSize = size();
      
      if (lSize == 0) {
        return new Object[0];
      }
      
      Object[] lObjects = new Object[lSize];
      
      int lIndex = 0;
      
      for (Object lObject : this) {
        lObjects[lIndex] = lObject;
        lIndex++;
      }
      
      return lObjects;
    }

    @Override
    public <T> T[] toArray(T[] pA) {
      assert(pA != null);
      
      int lSize = size();
      
      if (lSize <= pA.length) {
        int lIndex = 0;
        
        for (Object lObject : this) {
          pA[lIndex] = (T)lObject;
        }
        
        for (int i = lSize; i < pA.length; i++) {
          pA[i] = null;
        }
        
        return pA;
      }
      else {
        return (T[])toArray();
      }
    }
    
    @Override
    public String toString() {
      return mMap.toString();
    }
  }
  
  public static class WrapperCPA implements ConfigurableProgramAnalysis {
    private MyCompositeCPA mCompositeCPA;
    private AutomatonCPADomain<CFAEdge> mAutomatonDomain;
    
    public WrapperCPA(MyCompositeCPA pCompositeCPA, AutomatonCPADomain<CFAEdge> pAutomatonDomain) {
      assert(pCompositeCPA != null);
      assert(pAutomatonDomain != null);
      
      mCompositeCPA = pCompositeCPA;
      mAutomatonDomain = pAutomatonDomain;
    }
    
    @Override
    public AbstractDomain getAbstractDomain() {
      return mCompositeCPA.getAbstractDomain();
    }

    @Override
    public <AE extends AbstractElement> AE getInitialElement(CFAFunctionDefinitionNode pNode) {
      return (AE) mCompositeCPA.getInitialElement(pNode);
    }

    @Override
    public MergeOperator getMergeOperator() {
      return mCompositeCPA.getMergeOperator();
    }

    @Override
    public StopOperator getStopOperator() {
      return mCompositeCPA.getStopOperator();
    }

    @Override
    public TransferRelation getTransferRelation() {
      return mCompositeCPA.getTransferRelation();
    }
    
    // TODO: Move newReachedSet into interface of ConfigurableProgramAnalysis and
    // provide an abstract ConfigurableProgramAnalysisImpl-Class that implements
    // it by default by creating a hash set?
    // TODO: During ART creation establish an order
    // that allows efficient querying for test goals
    public Collection<Pair<AbstractElementWithLocation,Precision>> newReachedSet() {
      return new MyCollection(mAutomatonDomain);
    }

    @Override
    public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
      return mCompositeCPA.getInitialPrecision(pNode);
    }

    @Override
    public PrecisionAdjustment getPrecisionAdjustment() {
      return mCompositeCPA.getPrecisionAdjustment();
    }
  }
  
  public static Deque<ExplicitAbstractElement> getAbstractPath(ExplicitAbstractElement pElement) {
    // TODO: Remove this output
    System.out.println("Abstract Path >>> BEGIN");
    
    ExplicitAbstractElement lPathElement = pElement;
    
    Deque<ExplicitAbstractElement> lPath = new LinkedList<ExplicitAbstractElement>();
    
    while (lPathElement != null) {
      // TODO: Remove this output
      System.out.println(lPathElement.toString());
      
      lPath.addFirst(lPathElement);
      
      lPathElement = lPathElement.getParent();
    }
    
    // TODO: Remove this output
    System.out.println("Abstract Path >>> BEGIN");    
    
    return lPath;
  }
  
  //private final static int mLocationCPAIndex = 0;
  //private final static int mScopeRestrictionCPAIndex = 1;
  private final static int mTestGoalCPAIndex = 3;
  private final static int mAbstractionCPAIndex = 2;
  
  public static Set<Deque<ExplicitAbstractElement>> doIt (CFAFunctionDefinitionNode pMainFunction) {
    // create compositeCPA from automaton CPA and pred abstraction
    // TODO this must be a CPAPlus actually
    List<ConfigurableProgramAnalysis> cpas = new ArrayList<ConfigurableProgramAnalysis> ();
    try {
      cpas.add(new LocationCPA("sep", "sep"));
    } catch (CPAException e) {
      // for fixed values "sep", "sep" this is actually unreachable
      e.printStackTrace();
    }

    // get scope restriction automaton
    Automaton<CFAEdge> lScopeRestrictionAutomaton = AutomatonTestCases.getScopeRestrictionAutomaton(pMainFunction);
    ScopeRestrictionCPA lScopeRestrictionCPA = new ScopeRestrictionCPA(lScopeRestrictionAutomaton);
    cpas.add(lScopeRestrictionCPA);

    // initialize symbolic predicate abstraction
    ExplicitCPA lExplicitAbstractionCPA = new ExplicitCPA("sep", "sep");
    cpas.add(lExplicitAbstractionCPA);
    
    ExplicitAbstractFormulaManager lEAFManager = lExplicitAbstractionCPA.getAbstractFormulaManager();
    SymbolicFormulaManager lSFManager = lExplicitAbstractionCPA.getFormulaManager();
    
    
    // get test goal automaton
    Automaton<CFAEdge> lTestGoalAutomaton = AutomatonTestCases.getTestGoalAutomaton(pMainFunction);
    TestGoalCPA lTestGoalCPA = new TestGoalCPA(lTestGoalAutomaton);
    cpas.add(lTestGoalCPA);
    
    
    CPAAlgorithm algo = new CPAAlgorithm();

    // every final state in the test goal automaton represents a 
    // test goal, so initialize test goals with the final states
    // of the test goal automaton
    Set<Automaton<CFAEdge>.State> lTestGoals = lTestGoalAutomaton.getFinalStates();

    // the resulting set of paths
    Set<Deque<ExplicitAbstractElement>> lPaths = new HashSet<Deque<ExplicitAbstractElement>>();
    
    while (!lTestGoals.isEmpty()) {
      // TODO remove this output
      System.out.println("NEXT LOOP #####################");
      
      
      cpas.remove(mTestGoalCPAIndex);
      
      Automaton<CFAEdge> lSimplifiedAutomaton = lTestGoalCPA.getAbstractDomain().getAutomaton().getSimplifiedAutomaton();
      lTestGoals = lSimplifiedAutomaton.getFinalStates();
      
      lTestGoalCPA = new TestGoalCPA(lSimplifiedAutomaton);
      cpas.add(lTestGoalCPA);
      
      // create composite cpa
      //CompositeCPA cpa = CompositeCPA.createNewCompositeCPA(cpas, pMainFunction);
      MyCompositeCPA cpa = new MyCompositeCPA(cpas, pMainFunction);
      WrapperCPA lWrapperCPA = new WrapperCPA(cpa, lTestGoalCPA.getAbstractDomain());
      
      
      AbstractElementWithLocation lInitialElement = lWrapperCPA.getInitialElement(pMainFunction);
      Precision lInitialPrecision = lWrapperCPA.getInitialPrecision(pMainFunction);
      
      
      // TODO This is kind of a hack
      CompositePrecision lCompositePrecision = (CompositePrecision)lInitialPrecision;
      TestGoalPrecision lTestGoalPrecision = (TestGoalPrecision)lCompositePrecision.get(mTestGoalCPAIndex);
      // reset precision to test goals
      // TODO Hack
      lTestGoalPrecision.setTestGoals(lTestGoals);
      
      Collection<AbstractElementWithLocation> lReachedElements = null;
      
      try {
        lReachedElements = algo.CPA(lWrapperCPA, lInitialElement, lInitialPrecision);
        
        // TODO: Remove this output
        for (AbstractElement lElement : lReachedElements) {
          System.out.println(lElement);
        }
      } catch (CPAException e1) {
        e1.printStackTrace();
        
        // end test case generation
        break;
      }
      
      // TODO Remove this output
      System.out.print("Infeasible Test Goals: ");
      
      System.out.print("[");
      
      for (Automaton<CFAEdge>.State lState : lTestGoalPrecision.getRemainingFinalStates()) {
        System.out.print(lState);
        System.out.print(" ");
      }
      
      System.out.println("]");
      
      // Remove the infeasible test goals. If the set of remaining final states is
      // not empty this means that we have fully traversed an overapproximation
      // of the reachable state space. This shows that the remaing goals are not
      // reachable at all.
      lTestGoals.removeAll(lTestGoalPrecision.getRemainingFinalStates());
      
      for (AbstractElement lElement : lReachedElements) {
        // are there any remaining test goals to be covered?
        if (lTestGoals.isEmpty()) {
          // we are done, every test goal is reached
          break;
        }
        
        if (lWrapperCPA.getAbstractDomain().getBottomElement().equals(lElement)) {
          continue;
        }
        
        CompositeElement lCompositeElement = (CompositeElement)lElement;
          
        AbstractElement lTmpElement = lCompositeElement.get(mTestGoalCPAIndex);
        
        if (lTestGoalCPA.getAbstractDomain().getBottomElement().equals(lTmpElement)) {
          continue;
        }
        
        // TODO: Why is there a isBottomElement but not a isTopElement?
        // is isBottomElement superfluous?
        if (lTestGoalCPA.getAbstractDomain().getTopElement().equals(lTmpElement)) {
          // TODO: How to handle this, this element should never occur?
          // Should we consider it as covered every test goal?
          continue;
        }
        
        // now, we know it is an StateSetElement
        AutomatonCPADomain<CFAEdge>.StateSetElement lTestGoalCPAElement = lTestGoalCPA.getAbstractDomain().castToStateSetElement(lTmpElement);
        
        final Set<Automaton<CFAEdge>.State> lStates = lTestGoalCPAElement.getStates();
        
        boolean lHasFinalStates = false;
        
        // remove all covered test goals
        for (Automaton<CFAEdge>.State lState : lStates) {
          // is lState a remaining test goal?
          if (lState.isFinal()) {
            lHasFinalStates = true;
            
            if (lTestGoals.contains(lState)) {
              // TODO: Remove this output
              System.out.println("=> " + lElement.toString());

              Deque<ExplicitAbstractElement> lPath =
                                                     getAbstractPath((ExplicitAbstractElement) lCompositeElement
                                                         .get(mAbstractionCPAIndex));

              CounterexampleTraceInfo lInfo =
                                              lEAFManager
                                                  .buildCounterexampleTrace(
                                                      lSFManager, lPath);

              if (lInfo.isSpurious()) {
                // TODO: Remove this output
                System.out.println("Path is infeasible");

                TransferRelation lTransferRelation =
                                                     lExplicitAbstractionCPA
                                                         .getTransferRelation();

                ExplicitTransferRelation lExplicitTransferRelation =
                                                                     (ExplicitTransferRelation) lTransferRelation;

                try {
                  lExplicitTransferRelation.performRefinement(lPath, lInfo);
                } catch (RefinementNeededException e) {
                  // TODO: Remove this output
                  System.out.println("Refinement done!");
                } catch (Exception e) {
                  e.printStackTrace();

                  System.exit(1);
                }
              } else {
                // TODO: Remove this output
                System.out.println("Path is feasible");

                // remove the test goal from lTestGoals
                lTestGoals.remove(lState);

                // remove the test goal from the automaton
                lState.unsetFinal();

                // add feasible path to set of feasible paths
                lPaths.add(lPath);
              }
            }
          }
        }
        
        if (!lHasFinalStates) {
          // Because lReached is sorted according to the cardinality of final states
          // we will not see any final states in lReachedElements and thus can stop.
          break;
        }
        
      }
      
    }
    
    Map<Deque<ExplicitAbstractElement>, List<String>> lTranslations = AbstractPathToCTranslator.translatePaths(lPaths);

    for (Entry<Deque<ExplicitAbstractElement>,ThreeValuedBoolean> lVerified : CProver.checkSat(lTranslations).entrySet()) {
     switch (lVerified.getValue()) {
     case TRUE:
     {
       // test goal satisfied
       break;
     }
     case DONTKNOW:
     case FALSE:
     {
       // test goal still not matched
       break;
     }
     }
    }
    
    return lPaths;
  }
}

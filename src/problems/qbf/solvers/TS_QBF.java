package problems.qbf.solvers;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import metaheuristics.tabusearch.AbstractTS;
import problems.qbf.QBF_Inverse;
import solutions.Solution;



/**
 * Metaheuristic TS (Tabu Search) for obtaining an optimal solution to a QBF
 * (Quadractive Binary Function -- {@link #QuadracticBinaryFunction}).
 * Since by default this TS considers minimization problems, an inverse QBF
 *  function is adopted.
 * 
 * @author ccavellucci, fusberti
 */
public class TS_QBF extends AbstractTS<Integer> {
	
	private final Integer fake = new Integer(-1);
	
	private static final int INTERATIONS_TO_START_INTENSIFICATION = 800;
	
	private static final int INTERATIONS_OF_INTENSIFICATION = 400;
	
	private static final int PERCENTAGE_FIXED_ITENS = 25;
	
	private ArrayDeque<Integer> tlRemovedRandomItens;
	
	private Map<Integer, Integer> intensificationByRestartCounter;
	
	private Set<Integer> fixedVariablesIntensification;

	/**
	 * Constructor for the TS_QBF class. An inverse QBF objective function is
	 * passed as argument for the superclass constructor.
	 * 
	 * @param tenure
	 *            The Tabu tenure parameter.
	 * @param iterations
	 *            The number of iterations which the TS will be executed.
	 * @param filename
	 *            Name of the file for which the objective function parameters
	 *            should be read.
	 * @throws IOException
	 *             necessary for I/O operations.
	 */
	public TS_QBF(Integer tenure, Integer iterations, String filename) throws IOException {
		super(new QBF_Inverse(filename), tenure, iterations
		        ,INTERATIONS_TO_START_INTENSIFICATION, INTERATIONS_OF_INTENSIFICATION);
		tlRemovedRandomItens = new ArrayDeque<>();
		intensificationByRestartCounter = new HashMap<>();
		fixedVariablesIntensification = new HashSet<>();
	}

	/* (non-Javadoc)
	 * @see metaheuristics.tabusearch.AbstractTS#makeCL()
	 */
	@Override
	public ArrayList<Integer> makeCL() {

		ArrayList<Integer> _CL = new ArrayList<Integer>();
		for (int i = 0; i < ObjFunction.getDomainSize(); i++) {
			Integer cand = new Integer(i);
			_CL.add(cand);
		}

		return _CL;

	}

	/* (non-Javadoc)
	 * @see metaheuristics.tabusearch.AbstractTS#makeRCL()
	 */
	@Override
	public ArrayList<Integer> makeRCL() {

		ArrayList<Integer> _RCL = new ArrayList<Integer>();

		return _RCL;

	}
	
	/* (non-Javadoc)
	 * @see metaheuristics.tabusearch.AbstractTS#makeTL()
	 */
	@Override
	public ArrayDeque<Integer> makeTL() {

		ArrayDeque<Integer> _TS = new ArrayDeque<Integer>(2*tenure);
		for (int i=0; i<2*tenure; i++) {
			_TS.add(fake);
		}

		return _TS;

	}

	/* (non-Javadoc)
	 * @see metaheuristics.tabusearch.AbstractTS#updateCL()
	 */
	@Override
	public void updateCL() {

		// do nothing

	}

	/**
	 * {@inheritDoc}
	 * 
	 * This createEmptySol instantiates an empty solution and it attributes a
	 * zero cost, since it is known that a QBF solution with all variables set
	 * to zero has also zero cost.
	 */
	@Override
	public Solution<Integer> createEmptySol() {
		Solution<Integer> sol = new Solution<Integer>();
		sol.cost = 0.0;
		return sol;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * The local search operator developed for the QBF objective function is
	 * composed by the neighborhood moves Insertion, Removal and 2-Exchange.
	 */
	@Override
	public Solution<Integer> neighborhoodMove() {

		Double minDeltaCost;
		Integer bestCandIn = null, bestCandOut = null;

		minDeltaCost = Double.POSITIVE_INFINITY;
		updateCL();
		// Evaluate insertions
		for (Integer candIn : CL) {
			Double deltaCost = ObjFunction.evaluateInsertionCost(candIn, incumbentSol);
			if (evaluationAllowed(candIn, deltaCost)) {
				if (deltaCost < minDeltaCost) {
					minDeltaCost = deltaCost;
					bestCandIn = candIn;
					bestCandOut = null;
				}
			}
		}
		// Evaluate removals
		for (Integer candOut : incumbentSol) {
			Double deltaCost = ObjFunction.evaluateRemovalCost(candOut, incumbentSol);
			if (evaluationAllowed(candOut, deltaCost)) {
				if (deltaCost < minDeltaCost) {
					minDeltaCost = deltaCost;
					bestCandIn = null;
					bestCandOut = candOut;
				}
			}
		}
		// Evaluate exchanges
		for (Integer candIn : CL) {
			for (Integer candOut : incumbentSol) {
				Double deltaCost = ObjFunction.evaluateExchangeCost(candIn, candOut, incumbentSol);
				if (evaluationAllowed(candIn, candOut, deltaCost)) {
					if (deltaCost < minDeltaCost) {
						minDeltaCost = deltaCost;
						bestCandIn = candIn;
						bestCandOut = candOut;
					}
				}
			}
		}
		// Implement the best non-tabu move
		TL.poll();
		if (bestCandOut != null) {
			incumbentSol.remove(bestCandOut);
			CL.add(bestCandOut);
			TL.add(bestCandOut);
		} else {
			TL.add(fake);
		}
		TL.poll();
		if (bestCandIn != null) {
			incumbentSol.add(bestCandIn);
			CL.remove(bestCandIn);
			TL.add(bestCandIn);
		} else {
			TL.add(fake);
		}
		
		tlRemovedRandomItens.clear();
		repair();
		
		return null;
	}
	
	public void updateIntensificationByRestartCounter() {
	    for(Integer element : bestSol) {
	        Integer elementCount = intensificationByRestartCounter.get(element.intValue());
	        
	        if(elementCount == null) {
	            intensificationByRestartCounter.put(element.intValue(), 1);
	        } else {
	            intensificationByRestartCounter.put(element.intValue(), elementCount.intValue() + 1);
	        }
	    }
	}
	
	public void resetIntensificationByRestartCounter() {
	    this.intensificationByRestartCounter.clear();
	}
	
	public void setFixedComponentsIntensification() {
	    fixedVariablesIntensification.clear();
	    int index = 0;
	    int numberFixedElements = 0;
	    
	    Map<Integer, Integer> orderedVariablesOcurrence = intensificationByRestartCounter.entrySet()
                                        .stream()
                                        .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                                        .collect(Collectors.toMap(
                                                Map.Entry::getKey, 
                                                Map.Entry::getValue, 
                                                (e1, e2) -> e1, 
                                                LinkedHashMap::new
                                              ));
	    
	    numberFixedElements = computeNumberFixedElements();
	    Iterator<Integer> iterator = orderedVariablesOcurrence.keySet().iterator();
	    while(iterator.hasNext() && index < numberFixedElements) {
	        fixedVariablesIntensification.add(iterator.next());
	        index++;
	    }
	}
	
	private int computeNumberFixedElements() {
	    return (int) Math.round(intensificationByRestartCounter.size() * ((double)PERCENTAGE_FIXED_ITENS/100));
	}

	private boolean evaluationAllowed(Integer candidate, Double deltaCost) {
	    return !(TL.contains(candidate) || tlRemovedRandomItens.contains(candidate)); //|| incumbentSol.cost+deltaCost < bestSol.cost;
	}
	
	private boolean evaluationAllowed(Integer candIn, Integer candOut, Double deltaCost) {
        return evaluationAllowed(candIn, deltaCost) && evaluationAllowed(candOut, deltaCost);
    }
	
	private void repair() {
        randomizedSimplestRepair();
        ObjFunction.evaluate(incumbentSol);
    }
	
	private void randomizedSimplestRepair() {
        double removeCandIndexProb = 0;
        int removeCandIndex = 0;
        Solution<Integer> incumbentSolCopy = new Solution<Integer>(incumbentSol);
        sortSolution(incumbentSolCopy);
        
        /*Simplest repair: remove the right element that is incorrect*/
        for(int index = 0; index < incumbentSolCopy.size(); index++) {
            if(index < (incumbentSolCopy.size() - 1) && applyAdjacentConstraint(incumbentSolCopy, index)) {
                removeCandIndexProb = rng.nextDouble();
                removeCandIndex = Double.compare(removeCandIndexProb, 0.5) <= 0 ? index : index + 1;
                
                int indexElement = incumbentSol.indexOf(incumbentSolCopy.get(removeCandIndex));
                Integer element = incumbentSol.get(indexElement);
                CL.add(element);
                
                if(!tlRemovedRandomItens.contains(element)) {
                    tlRemovedRandomItens.add(incumbentSol.get(indexElement));
                }
                
                removeElementByValue(incumbentSol, incumbentSolCopy.get(removeCandIndex));
                incumbentSolCopy.remove(removeCandIndex);
                
                if(removeCandIndex == index) {
                    index--;
                }
            }
        }
    }
	
	private void removeElementByValue(Solution<Integer> solution, int targetValue) {
        for(int index = 0; index < solution.size(); index++) {
            if(solution.get(index).intValue() == targetValue) {
                solution.remove(index);
                break;
            }
        }
    }
	
	private boolean applyAdjacentConstraint(Solution<Integer> currentSolution, int currentIndex) {
        return currentSolution.get(currentIndex) + 1 == currentSolution.get(currentIndex+1);
    }
	
	private void sortSolution(Solution<Integer> sol) {
        sol.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer element1, Integer element2)
            {

                return  element1.compareTo(element2);
            }
        });
    }
	
	/**
	 * A main method used for testing the TS metaheuristic.
	 * 
	 */
	public static void main(String[] args) throws IOException {

		long startTime = System.currentTimeMillis();
		TS_QBF tabusearch = new TS_QBF(10, 10000, "instances/qbf100");
		Solution<Integer> bestSol = tabusearch.solve();
		System.out.println("maxVal = " + bestSol);
		long endTime   = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		System.out.println("Time = "+(double)totalTime/(double)1000+" seg");

	}

}

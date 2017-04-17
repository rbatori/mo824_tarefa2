package problems.qbf.solvers;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

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
public class TS_QBFAC_IntensificationByRestart extends AbstractTS<Integer> {
	
	private Map<Integer,Integer> Elite; // Map <element, number of consecutive iterations>
	
	private final Integer fake = new Integer(-1);

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
	 * @param eliteSize 
	 * @throws IOException
	 *             necessary for I/O operations.
	 */
	public TS_QBFAC_IntensificationByRestart(Integer tenure, Integer iterations, String filename,  Double restartFrequence, double eliteSize) throws IOException {
		super(new QBF_Inverse(filename), tenure, iterations, restartFrequence, eliteSize);
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
	
	@Override
	public void restartByIntensification() {

		if (Elite.size() < eliteSize/ObjFunction.getDomainSize()) return;
		
		Map<Integer,Integer> elite = new HashMap<Integer,Integer>(Elite);		
		incumbentSol = createEmptySol();
		
		SortedSet<Integer> values = new TreeSet<Integer>(elite.values());
		Map<Integer,Integer> Copy = new HashMap<Integer, Integer>(elite);
		for (int i = values.size() - 1; i > 0 && bestSol.size() < eliteSize/ObjFunction.getDomainSize(); i--) {
			for (Map.Entry<Integer, Integer> e : Copy.entrySet()) {
				if (e.getValue() == values.toArray()[i]) {
					incumbentSol.add(e.getKey());
					elite.remove(e.getKey());
				}
			}
		}
		ObjFunction.evaluate(incumbentSol);
		if (bestSol.cost > incumbentSol.cost) {
			
			bestSol = new Solution<Integer>(incumbentSol);		
			if (verbose)
				System.out.println("RestartSol = " + bestSol);
		}
		
		updateCL();

		for (int i = 0; i <  restartFrequence * iterations; i++) {
			neighborhoodMove();
			if (bestSol.cost > incumbentSol.cost) {
				bestSol = new Solution<Integer>(incumbentSol);
				if (verbose)
					System.out.println("(RestartSol. " + i + ") BestSol = " + bestSol);
			}
		}
			
	}
	
	@Override
	public void updateElite() {
		
		for (Integer I:bestSol) {
			if (Elite.containsKey(I))
				Elite.put(I, Elite.get(I) + 1);
			else
				Elite.put(I, 1);
		}
		
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
		
		// remove adjacent elements	
		CL = makeCL();
		for (Integer I : incumbentSol) {
			if (CL.indexOf(I + 1) > -1)
				CL.remove(CL.indexOf(I + 1));
			if (CL.indexOf(I) > -1)
				CL.remove(CL.indexOf(I));
			if (CL.indexOf(I - 1) > -1)
				CL.remove(CL.indexOf(I - 1));
		}
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
		Elite = new HashMap<Integer, Integer>();
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
			if (!TL.contains(candIn) || incumbentSol.cost+deltaCost < bestSol.cost) {
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
			if (!TL.contains(candOut) || incumbentSol.cost+deltaCost < bestSol.cost) {
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
				if ((!TL.contains(candIn) && !TL.contains(candOut)) || incumbentSol.cost+deltaCost < bestSol.cost) {
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
		ObjFunction.evaluate(incumbentSol);
		
		return null;
	}

	/**
	 * A main method used for testing the TS metaheuristic.
	 * 
	 */
	public static void main(String[] args) throws IOException {

		//080 -> best result=592 restartFrequence=0.20 eliteSize=0.10
		//010 -> best result=857
		
		long startTime = System.currentTimeMillis();
		double restartFrequence = 0.20; // restartFrequence >= 1 do not restart
		double eliteSize = 0.10; //percent of size of variables  
		TS_QBFAC_IntensificationByRestart tabusearch = new TS_QBFAC_IntensificationByRestart(20, 10000, "instances/qbf100", restartFrequence, eliteSize);
		Solution<Integer> bestSol = tabusearch.solve();
		System.out.println("maxVal = " + bestSol);
		long endTime   = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		System.out.println("Time = "+(double)totalTime/(double)1000+" seg");
 
	}

}

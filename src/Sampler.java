import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import burlap.behavior.singleagent.ApprenticeshipLearning;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.RandomStartStateGenerator;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.NameDependentStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;


public class Sampler {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MacroGridWorld gw = new MacroGridWorld(128, 128, 64, 64);
		Domain domain = gw.generateDomain();
		StateHashFactory hashingFactory = new NameDependentStateHashFactory();
		PropositionalFunction[] featureFunctions = MacroGridWorld.getPropositionalFunctions(domain);
		
		// TODO Auto-generated method stub
		Map<String, Double> rewards = MacroGridWorld.generateRandomRewards(featureFunctions, MacroGridWorld.MCELL_FILLED);
		RewardFunction randomReward = new ApprenticeshipLearning.FeatureBasedRewardFunction(featureFunctions, rewards);
		
		TerminalFunction tf = new TerminalFunction() {	
			@Override
			public boolean isTerminal(State s) {
				return false;
			}
		};
		//create and instance of planner; discount is set to 0.99; the minimum delta threshold is set to 0.001
		ValueIteration planner = new ValueIteration(domain, randomReward, tf, 0.99, hashingFactory, .01, 100);		
		
		
		State initialState = MacroGridWorld.getOneAgentState(domain);
		MacroGridWorld.setAgent(initialState, 0, 0);
		planner.planFromState(initialState);
		
		planner.runVI();
		//run planner from our initial state
		
		//create a Q-greedy policy using the Q-values that the planner computes
		Policy p = new GreedyQPolicy((QComputablePlanner)planner);
		RandomStartStateGenerator stateGenerator = new RandomStartStateGenerator((SADomain)domain, initialState);
		List<Double> rewardHistory = new ArrayList<Double>();
		
		for (int i = 0; i < 1000; i++) {
			EpisodeAnalysis ep = p.evaluateBehavior(stateGenerator.generateState(), randomReward, 1000);
			double d = ep.getDiscountedReturn(0.99);
			if (i > 0) {
				double average = (rewardHistory.get(rewardHistory.size() - 1) * rewardHistory.size() + d) / (rewardHistory.size() + 1);		
				rewardHistory.add(average);
				if (i > 1) {
				double dist = Math.abs(rewardHistory.get(rewardHistory.size() - 1) - rewardHistory.get(rewardHistory.size() - 2));
				System.out.println(i + ", " + d + ", " + rewardHistory.get(rewardHistory.size() - 1) + ", " + dist);
				}
			}
			else
			{
				rewardHistory.add(d);
			}
			
		}
	}

}
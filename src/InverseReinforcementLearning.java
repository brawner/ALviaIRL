import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.deterministic.DDPlannerPolicy;
import burlap.behavior.singleagent.planning.deterministic.DeterministicPlanner;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.UniformCostRF;


public class InverseReinforcementLearning {

	/**
	 * Calculates the Feature Expectations given one demonstration, a feature mapping and a discount factor gamma
	 * @param episodeAnalysis An EpisodeAnalysis object that contains a sequence of state-action pairs
	 * @param featureMapping Feature Mapping which maps states to features
	 * @param gamma Discount factor gamma
	 * @return The Feature Expectations generated (double array that matches the length of the featureMapping)
	 */
	public static FeatureExpectations generateFeatureExpectations(EpisodeAnalysis episodeAnalysis, FeatureMapping featureMapping, Double gamma) {
		double[] featureExpectationValues = new double[featureMapping.getFeatureSize()];
		for (int i = 0; i < episodeAnalysis.stateSequence.size(); ++i) {
			PropositionalFunction[] propositionalFunctions = featureMapping.getPropositionalFunctions();
			for (int j = 0; j < featureExpectationValues.length; ++j) {
				if (propositionalFunctions[j].isTrue(episodeAnalysis.stateSequence.get(i), new String[]{})) {
					featureExpectationValues[j] += Math.pow(gamma, i);
				}
				
			}
		}
		return new FeatureExpectations(featureExpectationValues);
	}
	
	/**
	 * Calculates the Feature Expectations given a list of expert demonstrations, a feature mapping and a discount factor gamma
	 * @param expertEpisodes List of expert demonstrations as EpisodeAnalysis objects
	 * @param featureMapping Feature Mapping which maps states to features
	 * @param gamma Discount factor for future expected reward
	 * @return The Feature Expectations generated (double array that matches the length of the featureMapping)
	 */
	public static FeatureExpectations generateExpertFeatureExpectations(List<EpisodeAnalysis> expertEpisodes, FeatureMapping featureMapping, Double gamma) {
		double[] featureExpectationValues = new double[featureMapping.getFeatureSize()];
		
		for (EpisodeAnalysis episodeAnalysis : expertEpisodes) {
			for (int i = 0; i < episodeAnalysis.stateSequence.size(); ++i) {
				PropositionalFunction[] propositionalFunctions = featureMapping.getPropositionalFunctions();
				for (int j = 0; j < featureExpectationValues.length; ++j) {
					if (propositionalFunctions[j].isTrue(episodeAnalysis.stateSequence.get(i), new String[]{})) {
						featureExpectationValues[j] += Math.pow(gamma, i);
					}
				}
			}
		}
		
		// Normalize the expert feature expectation values
		for (int i = 0; i < featureExpectationValues.length; ++i) {
			featureExpectationValues[i] /= expertEpisodes.size();
		}
		return new FeatureExpectations(featureExpectationValues);
	}
	
	/**
	 * Generates an anonymous instance of a reward function derived from a FeatureMapping 
	 * and associated feature weights
	 * Computes (w^(i))T phi from step 4 in section 3
	 * @param featureMapping The feature mapping of states to features
	 * @param featureWeights The weights given to each feature
	 * @return An anonymous instance of RewardFunction
	 */
	public static RewardFunction generateRewardFunction(FeatureMapping featureMapping, FeatureWeights featureWeights) {
		final FeatureMapping newFeatureMapping = new FeatureMapping(featureMapping);
		final FeatureWeights newFeatureWeights = new FeatureWeights(featureWeights);
		return new RewardFunction() {

			@Override
			public double reward(State state, GroundedAction a, State sprime) {
				PropositionalFunction[] propositionalFunctions = newFeatureMapping.getPropositionalFunctions();
				double[] featureWeightValues = newFeatureWeights.getWeights();
				
				double sumReward = 0;
				for (int i = 0; i < propositionalFunctions.length; ++i) {
					if (propositionalFunctions[i].isTrue(state, new String[]{})) {
						sumReward += featureWeightValues[i];
					}
				}
				return sumReward;
			}
		};
	}
	
	/**
	 * Checks and returns initial state if all expert episodes share the same starting state
	 * 
	 * @param episodes A list of expert demonstrated episodes
	 * @return The initial state shared by all policies, null if there are multiple initial states
	 */
	public static State getInitialState(List<EpisodeAnalysis> episodes) {
		if (episodes.size() > 0 && episodes.get(0).numTimeSteps() > 0) {
			State state = episodes.get(0).getState(0);
			for (int i = 1; i < episodes.size(); ++i) {
				if (episodes.get(i).numTimeSteps() > 0 && !state.equals(episodes.get(i).getState(0))) {
					return null;
				}
			}
			return state;
		}
		return null;
	}
	
	
	/**
	 * Method which implements high level algorithm provided section 3 of
	 * Abbeel, Peter and Ng, Andrew. "Apprenticeship Learning via Inverse Reinforcement Learning"
	 * @param domain Domain in which we are planning
	 * @param planner A Deterministic Planner created for the domain
	 * @param featureMapping A feature mapping which maps states to a feature vector of values
	 * @param expertEpisodes A list of expert demonstrated episodes generated from what we assume 
	 * to be following some unknown reward function
	 * @param gamma Discount factor gamma
	 * @param epsilon Iteration tolerance
	 * @param maxIterations Maximum number of iterations to iterate
	 * @return
	 */
	public static Policy generatePolicyTilde(Domain domain, OOMDPPlanner planner, FeatureMapping featureMapping, List<EpisodeAnalysis> expertEpisodes, double gamma, double epsilon, int maxIterations) {
		int maximumExpertEpisodeLength = 0;
		for (EpisodeAnalysis expertEpisode : expertEpisodes) {
			maximumExpertEpisodeLength = Math.max(maximumExpertEpisodeLength, expertEpisode.numTimeSteps());
		}
		
		TerminalFunction terminalFunction = planner.getTF();
		StateHashFactory stateHashingFactory = planner.getHashingFactory();
		
		// (1). Randomly generate policy pi^(0)
		Policy policy = new RandomPolicy(domain);
		
		List<FeatureExpectations> featureExpectationsHistory = new ArrayList<FeatureExpectations>();
		FeatureExpectations expertExpectations = InverseReinforcementLearning.generateExpertFeatureExpectations(expertEpisodes, featureMapping, gamma);
		
		State initialState = InverseReinforcementLearning.getInitialState(expertEpisodes);
		if (initialState == null) {
			return null;
		}
		
		// (1b) Compute u^(0) = u(pi^(0))
		EpisodeAnalysis episodeAnalysis = policy.evaluateBehavior(initialState, new UniformCostRF(), maximumExpertEpisodeLength);
		FeatureExpectations featureExpectations = InverseReinforcementLearning.generateFeatureExpectations(episodeAnalysis, featureMapping, gamma);
		featureExpectationsHistory.add(new FeatureExpectations(featureExpectations));
		
		for (int i = 0; i < maxIterations; ++i) {
			// (2) Compute t^(i) = max_w min_j (wT (uE - u^(j)))
			FeatureWeights featureWeights = FeatureWeights.solveFeatureWeights(expertExpectations, featureExpectationsHistory);

			// (3) if t^(i) <= epsilon, terminate
			if (featureWeights == null || Math.abs(featureWeights.getScore()) <= epsilon) {
				return policy;
			}
			
			// (4a) Calculate R = (w^(i))T * phi 
			RewardFunction rewardFunction = InverseReinforcementLearning.generateRewardFunction(featureMapping, featureWeights);
			
			// (4b) Compute optimal policy for pi^(i) give R
			planner.plannerInit(domain, rewardFunction, terminalFunction, gamma, stateHashingFactory);
			planner.planFromState(initialState);
			
			if (planner instanceof DeterministicPlanner) {
				policy = new DDPlannerPolicy((DeterministicPlanner)planner);
			}
			else if (planner instanceof QComputablePlanner) {
				policy = new GreedyQPolicy((QComputablePlanner)planner);
			}
			
			// (5) Compute u^(i) = u(pi^(i))
			episodeAnalysis = policy.evaluateBehavior(initialState, rewardFunction, maximumExpertEpisodeLength);
			featureExpectations = InverseReinforcementLearning.generateFeatureExpectations(episodeAnalysis, featureMapping, gamma);
			featureExpectationsHistory.add(new FeatureExpectations(featureExpectations));
			
			// (6) i++, go back to (2).
		}
		
		return policy;
	}
	/**
	 * Implements the "projection method" for calculating a policy-tilde with a
	 * feature expectation within epsilon of an expert's feature expectation.
	 * As described in:
	 * Abbeel, Peter and Ng, Andrew. "Apprenticeship Learning via Inverse Reinforcement Learning"
	 * 
	 * Takes in an expert's samples in some domain given some features, and returns a list of
	 * policies that can be evaluated algorithmically or manually.
	 * 
	 * Note: I've stored policy histories AND feature expectation histories
	 * 
	 * @param domain
	 * @param planner
	 * @param featureMapping
	 * @param expertEpisodes
	 * @param gamma
	 * @param epsilon
	 * @param maxIterations
	 * @return
	 */
	public static Policy projectionMethod(
										Domain domain, 
										OOMDPPlanner planner, 
										FeatureMapping featureMapping, 
										List<EpisodeAnalysis> expertEpisodes, 
										double gamma, double epsilon, int maxIterations) {
		
		
		//Max steps that the apprentice will have to learn
		int maximumExpertEpisodeLength = 0;
		for (EpisodeAnalysis expertEpisode : expertEpisodes) {
			maximumExpertEpisodeLength = Math.max(maximumExpertEpisodeLength, expertEpisode.numTimeSteps());
		}
		
		//Planning objects
		TerminalFunction terminalFunction = planner.getTF();
		StateHashFactory stateHashingFactory = planner.getHashingFactory();
		
		//(0) set up policy array; exper feature expectation
		List<Policy> policyHistory = new ArrayList<Policy>();
		List<FeatureExpectations> featureExpectationsHistory = new ArrayList<FeatureExpectations>();
		
		FeatureExpectations expertExpectations = InverseReinforcementLearning.generateExpertFeatureExpectations(expertEpisodes, featureMapping, gamma);

		State initialState = InverseReinforcementLearning.getInitialState(expertEpisodes);
		if (initialState == null) {
			return null;
		}
		
		// (1). Randomly generate policy pi^(0)
		Policy policy = new RandomPolicy(domain);
		policyHistory.add(policy);
		
		// (1b) Set up initial Feature Expectation based on policy
		EpisodeAnalysis episodeAnalysis = policy.evaluateBehavior(initialState, new UniformCostRF()	, maximumExpertEpisodeLength);
		FeatureExpectations curFE = InverseReinforcementLearning.generateFeatureExpectations(episodeAnalysis, featureMapping, gamma);
		featureExpectationsHistory.add(new FeatureExpectations(curFE));
		FeatureExpectations lastProjFE = null;
		FeatureExpectations newProjFE = null;
		
		for (int i = 0; i < maxIterations; ++i) {
			// (2) Compute weights and score using projection method
			//THIS IS THE KEY DIFFERENCE BETWEEN THE MAXIMUM MARGIN METHOD AND THE PROJECTION METHOD
			//On the first iteration, the projection is just set as the current feature expectation
			if (lastProjFE == null) { 
				newProjFE = new FeatureExpectations(curFE);
			}
			else {
				newProjFE = FeatureWeights.projectExpertFE(expertExpectations, curFE, lastProjFE);
			}
			FeatureWeights featureWeights = FeatureWeights.getWeightsProjectionMethod(expertExpectations, newProjFE);
			
			lastProjFE = newProjFE; //don't forget to set the old projection to the new one!

			// (3) if t^(i) <= epsilon, terminate
			if (featureWeights.getScore() <= epsilon) {
				return policy;
			}
			
			// (4a) Calculate R = (w^(i))T * phi 
			RewardFunction rewardFunction = InverseReinforcementLearning.generateRewardFunction(featureMapping, featureWeights);
			
			// (4b) Compute optimal policy for pi^(i) give R
			planner.plannerInit(domain, rewardFunction, terminalFunction, gamma, stateHashingFactory);
			planner.planFromState(initialState);
			if (planner instanceof DeterministicPlanner) {
				policy = new DDPlannerPolicy((DeterministicPlanner)planner);
			}
			else if (planner instanceof QComputablePlanner) {
				policy = new GreedyQPolicy((QComputablePlanner)planner);
			}
			policyHistory.add(policy);
			
			
			
			// (5) Compute u^(i) = u(pi^(i))
			episodeAnalysis = policy.evaluateBehavior(initialState, rewardFunction, maximumExpertEpisodeLength);
			curFE = InverseReinforcementLearning.generateFeatureExpectations(episodeAnalysis, featureMapping, gamma);
			featureExpectationsHistory.add(new FeatureExpectations(curFE));
			
			// (6) i++, go back to (2).
		}
		
		return policy;
	}
}

package burlap.behavior.singleagent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.deterministic.DDPlannerPolicy;
import burlap.behavior.singleagent.planning.deterministic.DeterministicPlanner;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.NameDependentStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.common.UniformCostRF;

import com.joptimizer.functions.ConvexMultivariateRealFunction;
import com.joptimizer.functions.LinearMultivariateRealFunction;
import com.joptimizer.functions.PDQuadraticMultivariateRealFunction;
import com.joptimizer.functions.PSDQuadraticMultivariateRealFunction;
import com.joptimizer.functions.QuadraticMultivariateRealFunction;
import com.joptimizer.optimizers.JOptimizer;
import com.joptimizer.optimizers.OptimizationRequest;
import com.joptimizer.optimizers.OptimizationResponse;
import com.joptimizer.util.Utils;

/**
 * 
 * @author brawner markkho
 *
 */
public class ApprenticeshipLearning {
	
	public static final int								FEATURE_EXPECTATION_SAMPLES = 10;

	/**
	 * Calculates the Feature Expectations given one demonstration, a feature mapping and a discount factor gamma
	 * @param episodeAnalysis An EpisodeAnalysis object that contains a sequence of state-action pairs
	 * @param featureMapping Feature Mapping which maps states to features
	 * @param gamma Discount factor gamma
	 * @return The Feature Expectations generated (double array that matches the length of the featureMapping)
	 */
	public static double[] estimateFeatureExpectation(
			EpisodeAnalysis episodeAnalysis, PropositionalFunction[] featureFunctions, Double gamma) {
		return ApprenticeshipLearning.estimateFeatureExpectation(
				Arrays.asList(episodeAnalysis), featureFunctions, gamma);
	}
	
	/**
	 * Calculates the Feature Expectations given a list of demonstrations, a feature mapping and a 
	 * discount factor gamma
	 * @param episodes List of expert demonstrations as EpisodeAnalysis objects
	 * @param featureMapping Feature Mapping which maps states to features
	 * @param gamma Discount factor for future expected reward
	 * @return The Feature Expectations generated (double array that matches the length of the featureMapping)
	 */
	public static double[] estimateFeatureExpectation(
			List<EpisodeAnalysis> episodes, PropositionalFunction[] featureFunctions, Double gamma) {
		double[] featureExpectations = new double[featureFunctions.length];
		
		for (EpisodeAnalysis episodeAnalysis : episodes) {
			for (int i = 0; i < episodeAnalysis.stateSequence.size(); ++i) {
				for (int j = 0; j < featureExpectations.length; ++j) {
					if (featureFunctions[j].isTrue(episodeAnalysis.stateSequence.get(i), new String[]{})) {
						featureExpectations[j] += Math.pow(gamma, i);
					}
				}
			}
		}
		
		// Normalize the feature expectation values
		for (int i = 0; i < featureExpectations.length; ++i) {
			featureExpectations[i] /= episodes.size();
		}
		return featureExpectations;
	}
	
	/**
	 * Generates an anonymous instance of a reward function derived from a FeatureMapping 
	 * and associated feature weights
	 * Computes (w^(i))T phi from step 4 in section 3
	 * @param featureMapping The feature mapping of states to features
	 * @param featureWeights The weights given to each feature
	 * @return An anonymous instance of RewardFunction
	 */
	public static RewardFunction generateRewardFunction(
			PropositionalFunction[] featureFunctions, FeatureWeights featureWeights) {
		final PropositionalFunction[] newFeatureFunctions = featureFunctions.clone();
		final FeatureWeights newFeatureWeights = new FeatureWeights(featureWeights);
		return new RewardFunction() {
			@Override
			public double reward(State state, GroundedAction a, State sprime) {
				double[] featureWeightValues = newFeatureWeights.getWeights();
				double sumReward = 0;
				for (int i = 0; i < newFeatureFunctions.length; ++i) {
					if (newFeatureFunctions[i].isTrue(state, new String[]{})) {
						sumReward += featureWeightValues[i];
					}
				}
				return sumReward;
			}
		};
	}
	
	/**
	 * OLD VERSION
	 * Checks and returns initial state if all expert episodes share the same starting state
	 * 
	 * @param episodes A list of expert demonstrated episodes
	 * @return The initial state shared by all policies, null if there are multiple initial states
	 */
	/*public static State getInitialState(List<EpisodeAnalysis> episodes) {
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
	}*/
	/**
	 * Returns the initial state of a randomly chosen episode analysis
	 * @param episodes
	 * @return
	 */
	public static State getInitialState(List<EpisodeAnalysis> episodes) {
		Random rando = new Random();
		EpisodeAnalysis randomEpisodeAnalysis = episodes.get(rando.nextInt(episodes.size()));
		return randomEpisodeAnalysis.getState(0);
	}
	

	public static Policy getLearnedPolicy(ApprenticeshipLearningRequest request) {
		if (!request.isValid()) {
			return null;
		}
		if (request.getUsingMaxMargin()) {
			return ApprenticeshipLearning.maxMarginMethod(request);
		}
		return ApprenticeshipLearning.projectionMethod(request);
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
	
	
	private static Policy maxMarginMethod(ApprenticeshipLearningRequest request) {
		
		// Need to evaluate policies with trajectory lengths equal to that of the demonstrated episodes
		int maximumExpertEpisodeLength = 0;
		List<EpisodeAnalysis> expertEpisodes = request.getExpertEpisodes();
		for (EpisodeAnalysis expertEpisode : expertEpisodes) {
			maximumExpertEpisodeLength = 
					Math.max(maximumExpertEpisodeLength, expertEpisode.numTimeSteps());
		}
		
		OOMDPPlanner planner = request.getPlanner();
		TerminalFunction terminalFunction = planner.getTF();
		StateHashFactory stateHashingFactory = planner.getHashingFactory();
		
		// (1). Randomly generate policy pi^(0)
		Domain domain = request.getDomain();
		Policy policy = new RandomPolicy(domain);
		
		PropositionalFunction[] featureFunctions = request.getFeatureFunctions();
		List<double[]> featureExpectationsHistory = new ArrayList<double[]>();
		double[] expertExpectations = 
				ApprenticeshipLearning.estimateFeatureExpectation(expertEpisodes, featureFunctions, request.getGamma());
		
		// (1b) Compute u^(0) = u(pi^(0))
		EpisodeAnalysis episodeAnalysis = 
				policy.evaluateBehavior(request.getStartStateGenerator().generateState(), new UniformCostRF(), maximumExpertEpisodeLength);
		double[] featureExpectations = 
				ApprenticeshipLearning.estimateFeatureExpectation(episodeAnalysis, featureFunctions, request.getGamma());
		featureExpectationsHistory.add(featureExpectations);
		
		int maxIterations = request.getMaxIterations();
		double[] tHistory = new double[maxIterations];
		int policyCount = request.getPolicyCount();
		for (int i = 0; i < maxIterations; ++i) {
			// (2) Compute t^(i) = max_w min_j (wT (uE - u^(j)))
			FeatureWeights featureWeights = null;
			
			int index = 0;
			while(featureWeights == null) {
				 featureWeights = featureWeights = solveFeatureWeights(expertExpectations, featureExpectationsHistory);
				 System.out.println(i + ", " + index++);
			}
			
			// (3) if t^(i) <= epsilon, terminate
			if (featureWeights == null || Math.abs(featureWeights.getScore()) <= request.getEpsilon()) {
				request.setTHistory(tHistory);
				return policy;
			}
			tHistory[i] = featureWeights.getScore();
			System.out.println(tHistory[i]);
			// (4a) Calculate R = (w^(i))T * phi 
			RewardFunction rewardFunction = 
					ApprenticeshipLearning.generateRewardFunction(featureFunctions, featureWeights);
			
			// (4b) Compute optimal policy for pi^(i) give R
			planner.plannerInit(domain, rewardFunction, terminalFunction, request.getGamma(), stateHashingFactory);
			planner.planFromState(request.getStartStateGenerator().generateState());
			
			if (planner instanceof DeterministicPlanner) {
				policy = new DDPlannerPolicy((DeterministicPlanner)planner);
			}
			else if (planner instanceof QComputablePlanner) {
				policy = new GreedyQPolicy((QComputablePlanner)planner);
			}
			
			// (5) Compute u^(i) = u(pi^(i))
			
			List<EpisodeAnalysis> evaluatedEpisodes = new ArrayList<EpisodeAnalysis>();
			for (int j = 0; j < policyCount; ++j) {
				evaluatedEpisodes.add(
						policy.evaluateBehavior(request.getStartStateGenerator().generateState(), rewardFunction, maximumExpertEpisodeLength));
			}
			featureExpectations = 
					ApprenticeshipLearning.estimateFeatureExpectation(evaluatedEpisodes, featureFunctions, request.getGamma());
			featureExpectationsHistory.add(featureExpectations);
			
			// (6) i++, go back to (2).
		}
		request.setTHistory(tHistory);
		
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
	private static Policy projectionMethod(ApprenticeshipLearningRequest request) {
		
		//Max steps that the apprentice will have to learn
		int maximumExpertEpisodeLength = 0;
		List<EpisodeAnalysis> expertEpisodes = request.getExpertEpisodes();
		for (EpisodeAnalysis expertEpisode : expertEpisodes) {
			maximumExpertEpisodeLength = Math.max(maximumExpertEpisodeLength, expertEpisode.numTimeSteps());
		}
		
		//Planning objects
		OOMDPPlanner planner = request.getPlanner();
		TerminalFunction terminalFunction = planner.getTF();
		StateHashFactory stateHashingFactory = planner.getHashingFactory();
		
		//(0) set up policy array; exper feature expectation
		List<Policy> policyHistory = new ArrayList<Policy>();
		List<double[]> featureExpectationsHistory = new ArrayList<double[]>();
		
		PropositionalFunction[] featureFunctions = request.getFeatureFunctions();
		double[] expertExpectations = 
				ApprenticeshipLearning.estimateFeatureExpectation(expertEpisodes, featureFunctions, request.getGamma());
		
		// (1). Randomly generate policy pi^(0)
		Domain domain = request.getDomain();
		Policy policy = new RandomPolicy(domain);
		policyHistory.add(policy);
	
		// (1b) Set up initial Feature Expectation based on policy
		List<EpisodeAnalysis> sampleEpisodes = new ArrayList<EpisodeAnalysis>();
		for (int j = 0; j < request.getPolicyCount(); ++j) {
			sampleEpisodes.add(
					policy.evaluateBehavior(request.getStartStateGenerator().generateState(), new UniformCostRF(), maximumExpertEpisodeLength)); 
		}
		double[] curFE = 
				ApprenticeshipLearning.estimateFeatureExpectation(sampleEpisodes, featureFunctions, request.getGamma());
		featureExpectationsHistory.add(curFE);
		double[] lastProjFE = null;
		double[] newProjFE = null;
		
		int maxIterations = request.getMaxIterations();
		double[] tHistory = new double[maxIterations];
		int policyCount = request.getPolicyCount();
		for (int i = 0; i < maxIterations; ++i) {
			// (2) Compute weights and score using projection method
			//THIS IS THE KEY DIFFERENCE BETWEEN THE MAXIMUM MARGIN METHOD AND THE PROJECTION METHOD
			//On the first iteration, the projection is just set as the current feature expectation
			if (lastProjFE == null) { 
				newProjFE = curFE.clone();
			}
			else {
				newProjFE = projectExpertFE(expertExpectations, curFE, lastProjFE);
			}
			FeatureWeights featureWeights = getWeightsProjectionMethod(expertExpectations, newProjFE);
			tHistory[i] = featureWeights.getScore();
			lastProjFE = newProjFE; //don't forget to set the old projection to the new one!

			
			// (3) if t^(i) <= epsilon, terminate
			if (featureWeights.getScore() <= request.getEpsilon()) {
				return policy;
			}
			
			// (4a) Calculate R = (w^(i))T * phi 
			RewardFunction rewardFunction = 
					ApprenticeshipLearning.generateRewardFunction(featureFunctions, featureWeights);
			
			// (4b) Compute optimal policy for pi^(i) give R
			planner.plannerInit(domain, rewardFunction, terminalFunction, request.getGamma(), stateHashingFactory);
			planner.planFromState(request.getStartStateGenerator().generateState());
			if (planner instanceof DeterministicPlanner) {
				policy = new DDPlannerPolicy((DeterministicPlanner)planner);
			}
			else if (planner instanceof QComputablePlanner) {
				policy = new GreedyQPolicy((QComputablePlanner)planner);
			}
			policyHistory.add(policy);
			
			// (5) Compute u^(i) = u(pi^(i))
			List<EpisodeAnalysis> evaluatedEpisodes = new ArrayList<EpisodeAnalysis>();
			for (int j = 0; j < policyCount; ++j) {
				evaluatedEpisodes.add(
						policy.evaluateBehavior(request.getStartStateGenerator().generateState(), rewardFunction, maximumExpertEpisodeLength));
			}
			curFE = ApprenticeshipLearning.estimateFeatureExpectation(evaluatedEpisodes, featureFunctions, request.getGamma());
			featureExpectationsHistory.add(curFE.clone());
			
			// (6) i++, go back to (2).
		}
		request.setTHistory(tHistory);
		return policy;
	}
	
	/**
	 * Static methods for estimating weights and tolerance in feature expectation space
	 */
	/**
	 * FeatureWeight factory which solves the best weights given Feature Expectations calculated from
	 * the expert demonstrations and a history of Feature Expectations.
	 * @param expertExpectations Feature Expectations calculated from the expert demonstrations
	 * @param featureExpectations Feature History of feature expectations generated from past policies
	 * @return
	 */
	private static FeatureWeights solveFeatureWeights(
			double[] expertExpectations, List<double[]> featureExpectations) {
		// We are solving a Quadratic Programming Problem here, yay!
		// Solve equation of form xT * P * x + qT * x + r
		// Let x = {w0, w1, ... , wn, t}
		int weightsSize = expertExpectations.length;
		
		// The objective is to maximize t, or minimize -t
		double[] qObjective = new double[weightsSize + 1];
		qObjective[weightsSize] = -1;
		LinearMultivariateRealFunction objectiveFunction = 
				new LinearMultivariateRealFunction( qObjective, 0);
		
		// We set the requirement that all feature expectations generated have to be less than the expert
		List<ConvexMultivariateRealFunction> expertBasedWeightConstraints = 
				new ArrayList<ConvexMultivariateRealFunction>();
		
		// (1/2)xT * Pi * x + qiT + ri <= 0
		// Equation (11) wT * uE >= wT * u(j) + t ===>  (u(j) - uE)T * w + t <= 0
		// Because x = {w0, w1, ... , wn, t}, we can set
		// qi = {u(j)_1 - uE_1, ... , u(j)_n - uE_n, 1}
		for (double[] expectations : featureExpectations) {
			double[] difference = new double[weightsSize + 1];
			for (int i = 0; i < expectations.length; ++i) {
				difference[i] = expectations[i] - expertExpectations[i];
			}
			difference[weightsSize] = 1;
			expertBasedWeightConstraints.add(new LinearMultivariateRealFunction(difference, 1));
		}
		
		// L2 norm of weights must be less than or equal to 1. So 
		// P = Identity, except for the last entry (which cancels t).
		double[][] identityMatrix = Utils.createConstantDiagonalMatrix(weightsSize + 1, 1);
		identityMatrix[weightsSize][weightsSize] = 0;
		expertBasedWeightConstraints.add(new PSDQuadraticMultivariateRealFunction(identityMatrix, null, -0.5));
		
		OptimizationRequest optimizationRequest = new OptimizationRequest();
		optimizationRequest.setF0(objectiveFunction);
		optimizationRequest.setFi(expertBasedWeightConstraints.toArray(
				new ConvexMultivariateRealFunction[expertBasedWeightConstraints.size()]));
		optimizationRequest.setCheckKKTSolutionAccuracy(false);
		optimizationRequest.setTolerance(1.E-12);
		optimizationRequest.setToleranceFeas(1.E-12);
		
		JOptimizer optimizer = new JOptimizer();
		optimizer.setOptimizationRequest(optimizationRequest);
		try {
			optimizer.optimize();
		} catch (Exception e) {
			System.out.println(e);
			return null;
		}
		OptimizationResponse optimizationResponse = optimizer.getOptimizationResponse();
		
		double[] solution = optimizationResponse.getSolution();
		double[] weights = Arrays.copyOfRange(solution, 0, weightsSize);
		double score = solution[weightsSize];
		return new FeatureWeights(weights, score);
	}
	
	/**
	 * 
	 * This projects the expert's feature expectation onto a line connecting the previous
	 * estimate of the optimal feature expectation to the previous projection. It is step 2a
	 * of the projection method.
	 * 
	 * @param expertFE - The Expert's Feature Expectations (or estimate of)
	 * @param lastFE - The last (i-1)th estimate of the optimal feature expectations
	 * @param lastProjFE - The last (i-2)th projection of the expert's Feature Expectations
	 * @return A new projection of the Expert's feature Expectation
	 */
	private static double[] projectExpertFE(double[] expertFE,
											double[] lastFE,
											double[] lastProjFE) {

		double[] newProjExp = new double[lastProjFE.length];
		
		double newProjExpCoefficient_num = 0.0;
		double newProjExpCoefficient_den = 0.0;
		//mu_bar^(i-2) + (mu^(i-1)-mu_bar^(i-2))*
			//((mu^(i-1)-mu_bar^(i-2))*(mu_E-mu_bar^(i-2)))/
			//((mu^(i-1)-mu_bar^(i-2))*(mu(i-1)-mu_bar^(i-2)))
		
		for (int i = 0; i < newProjExp.length; i++) {
			newProjExpCoefficient_num += ((lastFE[i]-lastProjFE[i])*(expertFE[i]-lastProjFE[i]));
			newProjExpCoefficient_den += ((lastFE[i]-lastProjFE[i])*(lastFE[i]-lastProjFE[i]));
		}
		
		double newProjExpCoefficient = newProjExpCoefficient_num/newProjExpCoefficient_den;
		
		for (int i = 0; i < newProjExp.length; i++) {
			newProjExp[i] = lastProjFE[i] + (lastFE[i]-lastProjFE[i])*newProjExpCoefficient;
		}
		
		return newProjExp;
	}
	
	
	
	/**
	 * Class that returns rewards based on a FeatureMapping and Map from Features to rewards
	 * Reward in a state will always be based on a combination of propositions satisfied.
	 * @author markho
	 *
	 */
	public static class FeatureBasedRewardFunction implements RewardFunction {
		private Map<String, Double> rewards;
		private PropositionalFunction[] propositionalFunctions;
		
		public FeatureBasedRewardFunction(PropositionalFunction[] functions, Map<String, Double> rewards) {
			this.propositionalFunctions = functions.clone();
			this.rewards = new HashMap<String, Double>(rewards);
		}
		@Override
		public double reward(State s, GroundedAction a, State sprime) {
			double reward = 0;
			for (PropositionalFunction function : this.propositionalFunctions) {
				if (function.isTrue(s, "")) {
					reward += this.rewards.get(function.getName());
				}
			}
			return reward;
		}
	}
	
	/**
	 * This takes the Expert's feature expectation and a projection, and calculates the weight
	 * and score. This is step 2b of the projection method.
	 * 
	 * @param expertFE
	 * @param newProjFE
	 * @return
	 */
	private static FeatureWeights getWeightsProjectionMethod(double[] expertFE, double[] newProjFE){
		
		//set the weight as the expert's feature expectation minus the new projection
		double[] weights = new double[newProjFE.length];
		for (int i = 0; i < weights.length; i++) {
			weights[i] = expertFE[i] - newProjFE[i];
		}
		
		//set the score (t) as the L2 norm of the weight
		double score = 0;
		for (double w : weights) {
			score += w*w;
			}
		
		score = Math.sqrt(score);
		return new FeatureWeights(weights, score);
	}
	
	/**
	 * Class of feature weights which contain the weight values and the associated score given to them
	 * @author brawner
	 *
	 */
	private static class FeatureWeights {
		private double[] weights;
		private double score;
		
		private FeatureWeights(double[] weights, double score) {
			this.weights = weights.clone();
			this.score = score;
		}
		
		public FeatureWeights(FeatureWeights featureWeights) {
			this.weights = featureWeights.getWeights();
			this.score = featureWeights.getScore();
		}
		
		public double[] getWeights() {
			return this.weights.clone();
		}
		
		public Double getScore() {
			return this.score;
		}
	}
	
	/**
	 * This class extends Policy, and all it does is create a randomly generated distribution of
	 * actions over all possible states. It lazily initializes because I have no idea what sorts 
	 * of states you are passing it.
	 * @author brawner
	 *
	 */
	public static class RandomPolicy extends Policy {
		Map<StateHashTuple, GroundedAction> stateActionMapping;
		List<Action> actions;
		Map<StateHashTuple, List<ActionProb>> stateActionDistributionMapping;
		StateHashFactory hashFactory;
		Random rando;
		
		/**
		 * Constructor initializes the policy, doesn't compute anything here.
		 * @param domain Domain object for which we need to plan
		 */
		private RandomPolicy(Domain domain) {
			this.stateActionMapping = new HashMap<StateHashTuple, GroundedAction>();
			this.stateActionDistributionMapping = new HashMap<StateHashTuple, List<ActionProb>>();
			this.actions = domain.getActions();
			this.rando = new Random();
			this.hashFactory = new NameDependentStateHashFactory();
		}

		public static Policy generateRandomPolicy(Domain domain) {
			return new RandomPolicy(domain);
		}
		
		/**
		 * For states which we have not yet visited, this policy needs a randomly generated distribution
		 * of actions. It queries all the grounded actions possible from this state and assigns a random
		 * probability to it. The probabilities are all normalized for happiness.
		 * @param state State for which to generate actions.
		 */
		private void addNewDistributionForState(State state) {
			StateHashTuple stateHashTuple = this.hashFactory.hashState(state);
			
			// Get all possible actions from this state
			List<GroundedAction> groundedActions = state.getAllGroundedActionsFor(this.actions);
			Double[] probabilities = new Double[groundedActions.size()];
			Double sum = 0.0;
			
			// Create a random distribution of doubles
			for (int i = 0; i < probabilities.length; ++i) {
				probabilities[i] = this.rando.nextDouble();
				sum += probabilities[i];
			}
			
			List<ActionProb> newActionDistribution = new ArrayList<ActionProb>(groundedActions.size());
			// Normalize distribution and add a new ActionProb to our list.
			for (int i = 0; i < probabilities.length; ++i) {
				ActionProb actionProb = new  ActionProb(groundedActions.get(i), probabilities[i] / sum);
				newActionDistribution.add(actionProb);
			}
			
			this.stateActionDistributionMapping.put(stateHashTuple, newActionDistribution);
		}
		
		@Override
		public GroundedAction getAction(State s) {
			StateHashTuple stateHashTuple = this.hashFactory.hashState(s);
			
			// If this state has not yet been visited, we need to compute a new distribution of actions
			if (!this.stateActionDistributionMapping.containsKey(stateHashTuple)) {
				this.addNewDistributionForState(s);
			}
			
			// Get the action probability distribution for this state
			List<ActionProb> actionDistribution = this.stateActionDistributionMapping.get(stateHashTuple);
			Double roll = this.rando.nextDouble();
			Double probabilitySum = 0.0;
			
			// Choose an action randomly from this distribution
			for (ActionProb actionProb : actionDistribution) {
				probabilitySum += actionProb.pSelection;
				if (probabilitySum >= roll) {
					return actionProb.ga;
				}
			}
			return null;
		}

		@Override
		public List<ActionProb> getActionDistributionForState(State s) {
			StateHashTuple stateHashTuple = this.hashFactory.hashState(s);
			
			// If this state has not yet been visited, we need to compute a new distribution of actions
			if (!this.stateActionDistributionMapping.containsKey(stateHashTuple)) {
				this.addNewDistributionForState(s);
			}
			return new ArrayList<ActionProb>(this.stateActionDistributionMapping.get(stateHashTuple));
		}

		@Override
		public boolean isStochastic() {
			return true;
		}
	}
}

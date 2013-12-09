import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import burlap.behavior.singleagent.ApprenticeshipLearning;
import burlap.behavior.singleagent.ApprenticeshipLearningRequest;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.RandomStartStateGenerator;
import burlap.behavior.singleagent.Policy.ActionProb;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.NameDependentStateHashFactory;
import burlap.behavior.statehashing.StateHashTuple;
import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;


public class TestConvergence extends IRLGridWorldDemo{
	
	final int			N_EXPERT_FE_SAMPLES = 30;
	final int			N_APPRENTICE_FE_SAMPLES = 5;
	final int			N_ALGO_ITERATIONS = 20;
	final int			N_VALUE_ESTIMATION_SAMPLES = 100;

	public void euclideanDistVsIterations(int maxIterations, int nRuns, String outputPath){
		//0 - generate a random reward funciton on the gridworld
		PropositionalFunction[] featureFunctions = MacroGridWorld.getPropositionalFunctions(this.domain);
		Map<String, Double> rewards = MacroGridWorld.generateRandomRewards(featureFunctions);
		RewardFunction randomReward = new ApprenticeshipLearning.FeatureBasedRewardFunction(featureFunctions, rewards);
		rf = randomReward;
		
		// 1 - Get Expert Policy, trajectories, and feature expectation estimate (via VI)
		//create and instance of planner; discount is set to 0.99; the minimum delta threshold is set to 0.001
		ValueIteration planner = new ValueIteration(domain, randomReward, tf, GAMMA, hashingFactory, .01, 100);		
		
		//run planner from our initial state
		//MacroGridWorld.setAgent(initialState, 0,0);
		planner.planFromState(MacroGridWorld.getRandomInitialState(this.domain));
		
		//create a Q-greedy policy using the Q-values that the planner computes
		Policy expertPolicy = new GreedyQPolicy((QComputablePlanner)planner);
		
		//run a sample of the computed policy and write its results to the file "VIResult.episode" in the directory outputPath
		//a '.episode' extension is automatically added by the writeToFileMethod
		List<EpisodeAnalysis> expertEpisodes = new ArrayList<EpisodeAnalysis>();
		for (int i =0; i < N_EXPERT_FE_SAMPLES; i++) {
			//MacroGridWorld.setAgent(initialState, (int)(Math.random()*MacroGridWorld.WIDTH), (int)(Math.random()*MacroGridWorld.HEIGHT));
			EpisodeAnalysis episode = expertPolicy.evaluateBehavior(MacroGridWorld.getRandomInitialState(this.domain), randomReward, tf,100);
			expertEpisodes.add(episode);
			episode.writeToFile(outputPath +"/traj/"+ "Expert_"+i, sp);
		}
		
		double[] expertFExp = ApprenticeshipLearning.estimateFeatureExpectation(expertEpisodes, featureFunctions, GAMMA);
		
		//
		//	Run experiment
		//
		List<double[]> results = new ArrayList<double[]>();
		
		long start = System.currentTimeMillis();
		
		ValueIteration apprenticePlanner = new ValueIteration(domain, null, tf, GAMMA, hashingFactory, .01, 100);		
		StateGenerator startStateGenerator = new RandomStartStateGenerator((SADomain)this.domain, this.initialState);
		ApprenticeshipLearningRequest request = 
				new ApprenticeshipLearningRequest(this.domain, apprenticePlanner, featureFunctions, expertEpisodes, startStateGenerator);
		request.setGamma(GAMMA);
		request.setEpsilon(FEXP_EPSILON);
		request.setMaxIterations(maxIterations);
		request.setPolicyCount(ApprenticeshipLearning.FEATURE_EXPECTATION_SAMPLES);
		
		for (int run = 0; run < nRuns; run++) {
			Policy projPolicy = ApprenticeshipLearning.getLearnedPolicy(request);
			double[] featureWeightScoreHistory = request.getTHistory();
			List<EpisodeAnalysis> apprenticeEpisodes = new ArrayList<EpisodeAnalysis>();
			for (int i = 0; i < maxIterations; ++i) {
				featureWeightScoreHistory[i] *= (1-GAMMA);
				}
			results.add(featureWeightScoreHistory);
			System.out.print(run+", ");
		}
		
		//Write results to file
		writeResultsToFile(results, outputPath, "performanceToIterations.csv");
		
	}
	
	public void euclideanDistVsIterationsIndependent(int maxIterations, int nRuns, String outputPath){
		//0 - generate a random reward funciton on the gridworld
		PropositionalFunction[] featureFunctions = MacroGridWorld.getPropositionalFunctions(this.domain);
		Map<String, Double> rewards = MacroGridWorld.generateRandomRewards(featureFunctions);
		RewardFunction randomReward = new ApprenticeshipLearning.FeatureBasedRewardFunction(featureFunctions, rewards);
		rf = randomReward;
		
		// 1 - Get Expert Policy, trajectories, and feature expectation estimate (via VI)
		//create and instance of planner; discount is set to 0.99; the minimum delta threshold is set to 0.001
		ValueIteration planner = new ValueIteration(domain, randomReward, tf, GAMMA, hashingFactory, .01, 100);		
		
		//run planner from our initial state
		MacroGridWorld.setAgent(initialState, 0,0);
		planner.planFromState(initialState);
		
		//create a Q-greedy policy using the Q-values that the planner computes
		Policy expertPolicy = new GreedyQPolicy((QComputablePlanner)planner);
		
		//run a sample of the computed policy and write its results to the file "VIResult.episode" in the directory outputPath
		//a '.episode' extension is automatically added by the writeToFileMethod
		List<EpisodeAnalysis> expertEpisodes = new ArrayList<EpisodeAnalysis>();
		for (int i =0; i < N_EXPERT_FE_SAMPLES; i++) {
			//MacroGridWorld.setAgent(initialState, (int)(Math.random()*MacroGridWorld.WIDTH), (int)(Math.random()*MacroGridWorld.HEIGHT));
			EpisodeAnalysis episode = expertPolicy.evaluateBehavior(MacroGridWorld.getRandomInitialState(this.domain), randomReward, tf,100);
			expertEpisodes.add(episode);
			episode.writeToFile(outputPath +"/traj/"+ "Expert_"+i, sp);
		}
		
		double[] expertFExp = ApprenticeshipLearning.estimateFeatureExpectation(expertEpisodes, featureFunctions, GAMMA);
		
		//
		//	Run experiment
		//
		List<double[]> results = new ArrayList<double[]>();
		
		long start = System.currentTimeMillis();
		
		ValueIteration apprenticePlanner = new ValueIteration(domain, null, tf, GAMMA, hashingFactory, .01, 100);		
		StateGenerator startStateGenerator = new RandomStartStateGenerator((SADomain)this.domain, this.initialState);
		ApprenticeshipLearningRequest request = 
				new ApprenticeshipLearningRequest(this.domain, apprenticePlanner, featureFunctions, expertEpisodes, startStateGenerator);
		request.setGamma(GAMMA);
		request.setEpsilon(FEXP_EPSILON);
		request.setMaxIterations(maxIterations);
		request.setPolicyCount(ApprenticeshipLearning.FEATURE_EXPECTATION_SAMPLES);
		
		for (int run = 0; run < nRuns; run++) {
			System.out.print("run: " + run + ", maxi: ");
			double[] runResults = new double[maxIterations];
			for (int maxi = 1; maxi < maxIterations; maxi++) {
				Policy projPolicy = ApprenticeshipLearning.getLearnedPolicy(request);
				double[] featureWeightScoreHistory = request.getTHistory();
				for (int i = 0; i < maxi; ++i) {
					featureWeightScoreHistory[i] *= (1-GAMMA);
				}
				if (featureWeightScoreHistory.length > 0) {
					runResults[maxi] = featureWeightScoreHistory[featureWeightScoreHistory.length-1];
				}
				System.out.print(maxi+", ");
			}
			results.add(runResults);
		}
		
		//Write results to file
		writeResultsToFile(results, outputPath, "performanceToIterations_independent.csv");
		
	}
	
	public void performanceToExpertSampleSize(int [] sampleSizes, int nRuns, String outputPath, String algorithm){
		//0 - generate a random reward funciton on the gridworld
		PropositionalFunction[] featureFunctions = MacroGridWorld.getPropositionalFunctions(this.domain);
		Map<String, Double> rewards = MacroGridWorld.generateRandomRewards(featureFunctions);
		RewardFunction randomReward = new ApprenticeshipLearning.FeatureBasedRewardFunction(featureFunctions, rewards);
		rf = randomReward;
		
		// 1 - Get Expert Policy, trajectories, and feature expectation estimate (via VI)
		//create an instance of planner; discount is set to 0.99; the minimum delta threshold is set to 0.001
		ValueIteration planner = new ValueIteration(domain, randomReward, tf, GAMMA, hashingFactory, .01, 100);		
		
		//run planner from our initial state
		MacroGridWorld.setAgent(initialState, 0,0);
		planner.planFromState(initialState);
		
		//create a Q-greedy policy using the Q-values that the planner computes
		Policy expertPolicy = new GreedyQPolicy((QComputablePlanner)planner);
		
		//run a sample of the computed policy and write its results to the file "VIResult.episode" in the directory outputPath
		//a '.episode' extension is automatically added by the writeToFileMethod
		int maxSamples = 0;
		for (int i = 0; i < sampleSizes.length; i++) {
			maxSamples = Math.max(sampleSizes[i],maxSamples);
		}
		
		//get the expert's expected value (basically measures how good the policy is)
		double expertValueEstimate = 0.0;
		for (int j =0; j < N_VALUE_ESTIMATION_SAMPLES; j++) {
			EpisodeAnalysis episode = expertPolicy.evaluateBehavior(MacroGridWorld.getRandomInitialState(this.domain), randomReward, tf,100);
			expertValueEstimate += episode.getDiscountedReturn(GAMMA);
		}
		expertValueEstimate /= N_VALUE_ESTIMATION_SAMPLES;
		
		//By Sample Size, do nRun runs
		List<double[]> results = new ArrayList<double[]>();
		
		//apprentice planner
		//RewardFunction randomReward = new ApprenticeshipLearning.FeatureBasedRewardFunction(featureFunctions, rewards);
		
		for (int sampleSize : sampleSizes) {
			System.out.print("SampleSize: "+sampleSize+ " ");
			double[] sampleResults = new double[nRuns];
			
			//generate expert samples
			List<EpisodeAnalysis> expertEpisodes = new ArrayList<EpisodeAnalysis>();
			for (int j =0; j < sampleSize; j++) {
				EpisodeAnalysis episode = expertPolicy.evaluateBehavior(MacroGridWorld.getRandomInitialState(this.domain), randomReward, tf,100);
				expertEpisodes.add(episode);
				episode.writeToFile(outputPath +"/traj/"+ "Expert_"+j, sp);
			}
			
			//get expert feature expectation
			//double[] expertFExp = ApprenticeshipLearning.estimateFeatureExpectation(
					//expertEpisodes, featureFunctions, GAMMA);
			
			Policy apprenticePolicy = null;
			//run it under this sample size a bunch of times
			for (int run = 0 ; run < nRuns; run++) {
				ValueIteration apprenticePlanner = new ValueIteration(domain, null, tf, GAMMA, hashingFactory, .01, 100);		
				apprenticePlanner.toggleDebugPrinting(false);
				
				System.out.print(run);
				StateGenerator startStateGenerator = new RandomStartStateGenerator((SADomain)this.domain, this.initialState);
				ApprenticeshipLearningRequest request = 
						new ApprenticeshipLearningRequest(this.domain, planner, featureFunctions, expertEpisodes, startStateGenerator);
				request.setUsingMaxMargin(algorithm == "maxmargin");
				apprenticePolicy = ApprenticeshipLearning.getLearnedPolicy(request);

				double valueEstimate = 0.0;
				for (int v = 0 ; v < N_VALUE_ESTIMATION_SAMPLES; v++) {
					EpisodeAnalysis ea = apprenticePolicy.evaluateBehavior(MacroGridWorld.getRandomInitialState(this.domain), rf, 100);
					valueEstimate += ea.getDiscountedReturn(GAMMA);
					ea.writeToFile(outputPath +"/traj/"+ algorithm+"_"+run+"_"+sampleSize, sp);
				}
				valueEstimate /= N_VALUE_ESTIMATION_SAMPLES;
				
				sampleResults[run] = valueEstimate/expertValueEstimate;
			}
			System.out.println();
			results.add(sampleResults);
		}
		
		//Write results to file
		writeResultsToFile(results, outputPath, "performanceToSamples.csv");
		
	}
	
	
	
	
	public void mimicToExpert(int [] sampleSizes, int nRuns, String outputPath){
		//0 - generate a random reward funciton on the gridworld
		PropositionalFunction[] featureFunctions = MacroGridWorld.getPropositionalFunctions(this.domain);
		Map<String, Double> rewards = MacroGridWorld.generateRandomRewards(featureFunctions);
		RewardFunction randomReward = new ApprenticeshipLearning.FeatureBasedRewardFunction(featureFunctions, rewards);
		rf = randomReward;
		
		// 1 - Get Expert Policy, trajectories, and feature expectation estimate (via VI)
		//create and instance of planner; discount is set to 0.99; the minimum delta threshold is set to 0.001
		ValueIteration planner = new ValueIteration(domain, randomReward, tf, GAMMA, hashingFactory, .01, 100);		
		
		//run planner from our initial state
		MacroGridWorld.setAgent(initialState, 0,0);
		planner.planFromState(initialState);
		
		//create a Q-greedy policy using the Q-values that the planner computes
		Policy expertPolicy = new GreedyQPolicy((QComputablePlanner)planner);
		
		//run a sample of the computed policy and write its results to the file "VIResult.episode" in the directory outputPath
		//a '.episode' extension is automatically added by the writeToFileMethod
		int maxSamples = 0;
		for (int i = 0; i < sampleSizes.length; i++) {
			maxSamples = Math.max(sampleSizes[i],maxSamples);
		}
		
		double expertValueEstimate = 0.0;
		for (int j =0; j < N_VALUE_ESTIMATION_SAMPLES; j++) {
			//MacroGridWorld.setAgent(initialState, (int)(Math.random()*MacroGridWorld.WIDTH), (int)(Math.random()*MacroGridWorld.HEIGHT));
			EpisodeAnalysis episode = expertPolicy.evaluateBehavior(MacroGridWorld.getRandomInitialState(this.domain), randomReward, tf,100);
			expertValueEstimate += episode.getDiscountedReturn(GAMMA);
		}
		expertValueEstimate /= N_VALUE_ESTIMATION_SAMPLES;
		
		//By Sample Size, do nRun runs
		List<double[]> results = new ArrayList<double[]>();
		
		for (int sampleSize : sampleSizes) {
			System.out.print("SampleSize: "+sampleSize+ " ");
			double[] sampleResults = new double[nRuns];
			
			//generate expert samples
			List<EpisodeAnalysis> expertEpisodes = new ArrayList<EpisodeAnalysis>();
			for (int j =0; j < sampleSize; j++) {
				//MacroGridWorld.setAgent(initialState, (int)(Math.random()*MacroGridWorld.WIDTH), (int)(Math.random()*MacroGridWorld.HEIGHT));
				EpisodeAnalysis episode = expertPolicy.evaluateBehavior(MacroGridWorld.getRandomInitialState(this.domain), randomReward, tf,100);
				expertEpisodes.add(episode);
				episode.writeToFile(outputPath +"/traj/"+ "Expert_"+j, sp);
			}
			
			//get expert feature expectation
			double[] expertFExp = ApprenticeshipLearning.estimateFeatureExpectation(expertEpisodes, featureFunctions, GAMMA);
			
			//run it under this sample size a bunch of times
			for (int run = 0 ; run < nRuns; run++) {
				System.out.print(run);
				Policy mimicPolicy = new MimicTheExpertPolicy(this.domain, expertEpisodes);
				
				
				double valueEstimate = 0.0;
				for (int v = 0 ; v < N_VALUE_ESTIMATION_SAMPLES; v++) {
					EpisodeAnalysis ea = mimicPolicy.evaluateBehavior(MacroGridWorld.getRandomInitialState(this.domain), rf, 100);
					valueEstimate += ea.getDiscountedReturn(GAMMA);
					ea.writeToFile(outputPath +"/traj/"+ "Mimic_"+run+"_"+sampleSize, sp);
				}
				valueEstimate /= N_VALUE_ESTIMATION_SAMPLES;
				
				sampleResults[run] = valueEstimate/expertValueEstimate;
			}
			System.out.println();
			results.add(sampleResults);
		}
		
		//Write results to file
		writeResultsToFile(results, outputPath, "performanceToSamples_mimic.csv");
		
	}
	
	private static void writeResultsToFile(List<double[]> results, String outputPath, String fname) {
		File f = (new File(outputPath));
		f.mkdirs();
		
		try {
			
			BufferedWriter br = new BufferedWriter(new FileWriter(outputPath + "/" + fname));
			StringBuilder sb = new StringBuilder();
			
			for (int i = 0; i < results.size(); i++) {
				double[] runResults = results.get(i);
				//sb.append("run_"+(i));
				for (double iter : runResults) {
					sb.append(iter);
					sb.append(", ");
				}
				sb.append("\n");
			}

			br.write(sb.toString());
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static class MimicTheExpertPolicy extends Policy {

		private HashMap<StateHashTuple, GroundedAction> stateActionMapping;
		private HashMap<StateHashTuple, List<ActionProb>> stateActionDistributionMapping;
		private List<Action> actions;
		private Random rando;
		private NameDependentStateHashFactory hashFactory;

		public MimicTheExpertPolicy(Domain d, List<EpisodeAnalysis> eas) {
			this.stateActionMapping = new HashMap<StateHashTuple, GroundedAction>();
			this.stateActionDistributionMapping = new HashMap<StateHashTuple, List<ActionProb>>();
			this.actions = d.getActions();
			this.rando = new Random();
			this.hashFactory = new NameDependentStateHashFactory();
			
			
			//generate state-action mapping by looking at all the state-action info in the expert's examples
			for (EpisodeAnalysis ea :eas) {
				for (int step = 0; step < ea.numTimeSteps() - 1; step++) {
					
					Object statehash = this.hashFactory.hashState(ea.getState(step));
					if (!this.stateActionMapping.containsKey(statehash)) {
						try {
							this.stateActionMapping.put((StateHashTuple) statehash, ea.getAction(step));
						}
						catch (IndexOutOfBoundsException e) {
							System.out.println(e.getMessage());
						}
					}
				}
			}
		}
		
		@Override
		public GroundedAction getAction(State s) {
			// TODO Auto-generated method stub
			
			Object statehash = this.hashFactory.hashState(s);
			//check if its in the dictionary from the expert
			if(this.stateActionMapping.containsKey(statehash)) {
				return this.stateActionMapping.get(statehash);
			}
			
			//if not, do a random action
			List<GroundedAction> allActions = s.getAllGroundedActionsFor(this.actions);
			return allActions.get(this.rando.nextInt(allActions.size()));
			
		}

		@Override
		public List<ActionProb> getActionDistributionForState(State s) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean isStochastic() {
			// TODO Auto-generated method stub
			return false;
		}
		
	}
	


	public static void main(String [] args) {
		TestConvergence tester = new TestConvergence();
		String outputPath = "results"; //directory to record results
		
		//tester.euclideanDistVsIterations(30, 5, outputPath);
		//tester.euclideanDistVsIterationsIndependent(30, 5, outputPath);
		
		int [] sampleSizes = {1,4,8,10,40,80,100};
		tester.performanceToExpertSampleSize(sampleSizes, 5, outputPath, "projection");
		//tester.mimicToExpert(sampleSizes, 5, outputPath);
		tester.visualizeEpisode(outputPath+"/traj/");
	}
}

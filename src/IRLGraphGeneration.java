import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import burlap.behavior.singleagent.ApprenticeshipLearning;
import burlap.behavior.singleagent.ApprenticeshipLearningRequest;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.RandomStartStateGenerator;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldStateParser;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.explorer.VisualExplorerRecorder;
import burlap.oomdp.visualizer.Visualizer;


public class IRLGraphGeneration {
	MacroGridWorld gridWorld;
	Domain domain;
	StateParser stateParser;
	State initialState;
	static TerminalFunction terminalFunction = new IRLGridTF();
	static DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
	
	public IRLGraphGeneration(MacroGridWorld world) {
		this.gridWorld = world;
		this.domain = world.generateDomain();
		this.stateParser = new GridWorldStateParser(domain); //for writing states to a file
		this.initialState = MacroGridWorld.getOneAgentState(domain);
		MacroGridWorld.setAgent(initialState, 0, 0);
		hashingFactory.setAttributesForClass(MacroGridWorld.CLASSAGENT, domain.getObjectClass(GridWorldDomain.CLASSAGENT).attributeList);
	}
	
	/**
	 * This will method will perform VI planning and save a sample of the policy.
	 * @param outputPath the path to the directory in which the policy sample will be saved
	 * 
	 */
	public long runALviaIRLRandomlyGeneratedEpisodes(String outputPath, int method, int episodesCount){
		//for consistency make sure the path ends with a '/'
		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}
		
		PropositionalFunction[] featureFunctions = 
				MacroGridWorld.getPropositionalFunctions(domain, this.gridWorld);
		
		Map<String, Double> rewards = MacroGridWorld.generateRandomRewards(featureFunctions, 4);
		RewardFunction randomReward = new ApprenticeshipLearning.FeatureBasedRewardFunction(featureFunctions, rewards);
		
		//create and instance of planner; discount is set to 0.99; the minimum delta threshold is set to 0.001
		ValueIteration planner = new ValueIteration(domain, randomReward, terminalFunction, 0.9, hashingFactory, .01, 100);		
		
		//run planner from our initial state
		planner.planFromState(initialState);
		
		//create a Q-greedy policy using the Q-values that the planner computes
		Policy p = new GreedyQPolicy((QComputablePlanner)planner);
		
		//run a sample of the computed policy and write its results to the file "VIResult.episode" in the directory outputPath
		//a '.episode' extension is automatically added by the writeToFileMethod
		List<EpisodeAnalysis> episodes = new ArrayList<EpisodeAnalysis>();
		for (int i =0; i < episodesCount; ++i) {
			EpisodeAnalysis episode = p.evaluateBehavior(initialState, randomReward, terminalFunction, 100);
			episodes.add(episode);
		}
		
		if (method == 0) {
			return this.runALviaIRLMaxMargin(outputPath, planner, featureFunctions, episodes, randomReward);
		}
		else {
			return this.runALviaIRLProjection(outputPath, planner, featureFunctions, episodes, randomReward);
		}
		
	}
	
	/**
	 * This will method will perform VI planning and save a sample of the policy.
	 * @param outputPath the path to the directory in which the policy sample will be saved
	 * 
	 */
	public double[] runALviaIRLRandomlyGeneratedEpisodesWithTHistory(String outputPath, int method, int episodesCount, int episodeNumber){
		//for consistency make sure the path ends with a '/'
		if(!outputPath.endsWith("/")){
		outputPath = outputPath + "/";
		}
		
		PropositionalFunction[] featureFunctions = MacroGridWorld.getPropositionalFunctions(this.domain);
		Map<String, Double> rewards = MacroGridWorld.generateRandomRewards(featureFunctions, MacroGridWorld.MCELL_FILLED);
		RewardFunction randomReward = new ApprenticeshipLearning.FeatureBasedRewardFunction(featureFunctions, rewards);

		
		//create and instance of planner; discount is set to 0.99; the minimum delta threshold is set to 0.001
		ValueIteration planner = new ValueIteration(domain, randomReward, terminalFunction, ApprenticeshipLearningRequest.DEFAULT_GAMMA, hashingFactory, .01, 100);		
		
		//run planner from our initial state
		planner.planFromState(initialState);
		
		//create a Q-greedy policy using the Q-values that the planner computes
		Policy p = new GreedyQPolicy((QComputablePlanner)planner);
		
		StateGenerator startStateGenerator = new RandomStartStateGenerator((SADomain)this.domain, this.initialState);
		
		//run a sample of the computed policy and write its results to the file "VIResult.episode" in the directory outputPath
		//a '.episode' extension is automatically added by the writeToFileMethod
		List<EpisodeAnalysis> episodes = new ArrayList<EpisodeAnalysis>();
		for (int i =0; i < 10; ++i) {
			EpisodeAnalysis episode = p.evaluateBehavior(startStateGenerator.generateState(), randomReward, terminalFunction ,100);
			episodes.add(episode);
		}
		
		ApprenticeshipLearningRequest request = 
				new ApprenticeshipLearningRequest(this.domain, planner, featureFunctions, episodes, startStateGenerator);
		request.setPolicyCount(episodeNumber);
		request.setUsingMaxMargin(method == 0);
		Policy projectionPolicy = ApprenticeshipLearning.getLearnedPolicy(request);
		long end = System.currentTimeMillis();
		EpisodeAnalysis projectionEpisode = projectionPolicy.evaluateBehavior(initialState, randomReward, terminalFunction, 100);
		projectionEpisode.writeToFile(outputPath + "Projection", stateParser);
		
		return request.getTHistory();
		
	}
	
	public long runALviaIRLMaxMargin(String outputPath, ValueIteration planner, PropositionalFunction[] featureFunctions, List<EpisodeAnalysis> expertEpisodes, RewardFunction randomReward) {
		//run a sample of the computed policy and write its results to the file "VIResult.episode" in the directory outputPath
		//a '.episode' extension is automatically added by the writeToFileMethod
		int index = 0;
		for (EpisodeAnalysis episode : expertEpisodes) {
			episode.writeToFile(outputPath + "expert" + index++, stateParser);
		}
		
		StateGenerator startStateGenerator = new RandomStartStateGenerator((SADomain)this.domain, this.initialState);
		ApprenticeshipLearningRequest request = 
				new ApprenticeshipLearningRequest(this.domain, planner, featureFunctions, expertEpisodes, startStateGenerator);
		request.setUsingMaxMargin(true);
		
		long start = System.currentTimeMillis();
		Policy policy = ApprenticeshipLearning.getLearnedPolicy(request);
		long end = System.currentTimeMillis();
		EpisodeAnalysis resultEpisode = policy.evaluateBehavior(startStateGenerator.generateState(), randomReward, terminalFunction, 100);
		resultEpisode.writeToFile(outputPath + "Result", stateParser);
		
		return end - start;
	}
	
	public long runALviaIRLProjection(String outputPath, ValueIteration planner, PropositionalFunction[] featureFunctions, List<EpisodeAnalysis> expertEpisodes, RewardFunction randomReward)
	{
		//run a sample of the computed policy and write its results to the file "VIResult.episode" in the directory outputPath
		//a '.episode' extension is automatically added by the writeToFileMethod
		int index = 0;
		for (EpisodeAnalysis episode : expertEpisodes) {
			episode.writeToFile(outputPath + "expert" + index++, stateParser);
		}
		StateGenerator startStateGenerator = new RandomStartStateGenerator((SADomain)this.domain, this.initialState);
		ApprenticeshipLearningRequest request = 
				new ApprenticeshipLearningRequest(this.domain, planner, featureFunctions, expertEpisodes, startStateGenerator);
		
		long start = System.currentTimeMillis();
		Policy projectionPolicy = ApprenticeshipLearning.getLearnedPolicy(request);
		long end = System.currentTimeMillis();
		EpisodeAnalysis projectionEpisode = projectionPolicy.evaluateBehavior(initialState, randomReward, terminalFunction, 100);
		projectionEpisode.writeToFile(outputPath + "Projection", stateParser);
		
		return end - start;
	}	
	
	public static void generateRuntimeVSFeatures(String outputPath, int maxFeatureCount, int dataPointCount, int repetitions) {
		int xSeparation = maxFeatureCount / dataPointCount;
		int count = 0;
		FileWriter writer;
		try {	
			writer = new FileWriter("results.txt");
			for (int i = 0; i < repetitions; ++i) {
				for (int j = 1; j < 4; j++) {
					int macroCellWidth = (int)Math.pow(2, j);
					for (int k = 0; k < 4; k++) {
						int macroCellHeight = (int)Math.pow(2, k);
						IRLGraphGeneration tester = new IRLGraphGeneration(new MacroGridWorld(8, 8, macroCellWidth, macroCellHeight));
						String trialOutputPath = outputPath + "/trial" + count++;
						writer.append(macroCellWidth + ", " + macroCellHeight + ", " + tester.runALviaIRLRandomlyGeneratedEpisodes(trialOutputPath,0, 1) + ", 0\n");
						writer.append(macroCellWidth + ", " + macroCellHeight + ", " + tester.runALviaIRLRandomlyGeneratedEpisodes(trialOutputPath,1, 1) + ", 1\n");
					}
				}
			}
			
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void generateTVSRuntimeForEpisodesIterations(String outputPath, int maxFeatureCount, int dataPointCount, int repetitions) {
		int xSeparation = maxFeatureCount / dataPointCount;
		int count = 0;
		IRLGraphGeneration tester = new IRLGraphGeneration(new MacroGridWorld(8, 8, 4, 4));
		FileWriter writer;
		try {	
			writer = new FileWriter("tVSRuns.txt");
			for (int i = 0; i < repetitions; ++i) {
				for (int j = 1; j < 5; j++) {
					String trialOutputPath = outputPath + "/trial" + count++;
					double[] tHistory = tester.runALviaIRLRandomlyGeneratedEpisodesWithTHistory(trialOutputPath,1, 5, j);
					for (int k = 0; k < tHistory.length; ++k) {
						writer.append(j + ", " + tHistory[k] + ", 1\n");
					}
				}
			}
			
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//IRLGraphGeneration.generateRuntimeVSFeatures("output", 64, 4, 1);
		IRLGraphGeneration.generateTVSRuntimeForEpisodesIterations("output", 64, 4, 1);
	}
	
	static class IRLGridTF implements TerminalFunction{	
		public IRLGridTF(){ }
		@Override
		public boolean isTerminal(State s) {
			return false;
		}
	}
}

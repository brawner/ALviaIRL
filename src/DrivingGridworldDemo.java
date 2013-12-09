import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import burlap.behavior.singleagent.ApprenticeshipLearning;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldStateParser;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.explorer.VisualExplorerRecorder;
import burlap.oomdp.visualizer.Visualizer;


public class DrivingGridworldDemo {
	public static List<EpisodeAnalysis> interactive(Domain domain, DrivingGridWorld gridWorld, State initialState) {
		Visualizer v = DrivingWorldVisualizer.getVisualizer(domain, gridWorld.getMap());
		VisualExplorerRecorder exp = new VisualExplorerRecorder(domain, v, initialState);
		
		exp.addKeyAction("w", MacroGridWorld.ACTIONNORTH);
		exp.addKeyAction("s", MacroGridWorld.ACTIONSOUTH);
		exp.addKeyAction("d", MacroGridWorld.ACTIONEAST);
		exp.addKeyAction("a", MacroGridWorld.ACTIONWEST);
		
		List<EpisodeAnalysis> recordedEpisodes = new ArrayList<EpisodeAnalysis>();
		exp.initGUIAndRecord(recordedEpisodes);
		return recordedEpisodes;
	}
	
	public static void runDrivingDemo(String outputPath, int width, int height, int numLanes, int laneWidth ) {
		DrivingGridWorld gridWorld = new DrivingGridWorld(width, height, numLanes, laneWidth);
		Domain gridWorldDomain = gridWorld.generateDomain();
		
		int[] xLocations = new int[numLanes];
		int startX = (width - numLanes * laneWidth) / 2 + laneWidth / 2;
		for (int i =0; i < numLanes; ++i) {
			xLocations[i] = startX + i * laneWidth;
		}
		
		State initialState = gridWorld.getOneAgentState(gridWorldDomain, xLocations, height, height);
		List<EpisodeAnalysis> expertEpisodes = DrivingGridworldDemo.interactive(gridWorldDomain, gridWorld, initialState);
		
		//for consistency make sure the path ends with a '/'
		if(!outputPath.endsWith("/")){
			outputPath = outputPath + "/";
		}
		
		PropositionalFunction[] featureFunctions = DrivingGridWorld.getFeatureFunctions(gridWorldDomain);
		Map<String, Double> rewards = DrivingGridWorld.generateRewards(featureFunctions);
		RewardFunction rewardFunction = new ApprenticeshipLearning.FeatureBasedRewardFunction(featureFunctions, rewards);
		StateHashFactory hashingFactory = new DiscreteStateHashFactory();
		
		TerminalFunction terminalFunction = new TerminalFunction() {
			@Override
			public boolean isTerminal(State s) { return false; } 
		};

		//create and instance of planner; discount is set to 0.99; the minimum delta threshold is set to 0.001
		ValueIteration planner = new ValueIteration(gridWorldDomain, rewardFunction, terminalFunction, 0.9, hashingFactory, .01, 100);		
		
		//run planner from our initial state
		planner.planFromState(initialState);
		
		//create a Q-greedy policy using the Q-values that the planner computes
		Policy p = new GreedyQPolicy((QComputablePlanner)planner);
		StateParser stateParser = new GridWorldStateParser(gridWorldDomain);
		
		int index = 0;
		for (EpisodeAnalysis episode : expertEpisodes) {
			episode.writeToFile(outputPath + "expert" + index++, stateParser);
		}

		Policy projectionPolicy = ApprenticeshipLearning.projectionMethod(gridWorldDomain, planner, featureFunctions, expertEpisodes, 0.9, 0.01, 100);
		EpisodeAnalysis projectionEpisode = projectionPolicy.evaluateBehavior(initialState, rewardFunction, terminalFunction, 100);
		projectionEpisode.writeToFile(outputPath + "Projection", stateParser);
	}
	public static void main(String[] args) {
		String outputPath = "output";
		int width = 11;
		int height = 11;
		int numLanes = 3;
		int laneWidth = 3;
		
		for (int i = 0; i < args.length; ++i) {
			String arg = args[i];
			if (arg.equalsIgnoreCase("-o")) {
				outputPath = args[i++];
			}
			else if (arg.equals("-w")) {
				try {
					width = Integer.parseInt(args[i++]);
				}
				catch (NumberFormatException e) {
					System.err.println("Argument " + args[i] + " is not an integer");
					System.exit(1);
				}
			}
			else if (arg.equals("-h")) {
				try {
					height = Integer.parseInt(args[i++]);
				}
				catch (NumberFormatException e) {
					System.err.println("Argument " + args[i] + " is not an integer");
					System.exit(1);
				}
			}
			else if (arg.equals("--lanes")) {
				try {
					numLanes = Integer.parseInt(args[i++]);
				}
				catch (NumberFormatException e) {
					System.err.println("Argument " + args[i] + " is not an integer");
					System.exit(1);
				}
			}
			else if (arg.equals("--lane-width")) {
				try {
					laneWidth = Integer.parseInt(args[i++]);
				}
				catch (NumberFormatException e) {
					System.err.println("Argument " + args[i] + " is not an integer");
					System.exit(1);
				}
			}
			else {
				System.err.println("Unrecognized argument: " + arg);
			}
		}
		
		DrivingGridworldDemo.runDrivingDemo(outputPath, width, height, numLanes, laneWidth);
	}
}

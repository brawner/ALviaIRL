import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import burlap.behavior.singleagent.ApprenticeshipLearning;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.planning.QComputablePlanner;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.singleagent.RewardFunction;


public class TestConvergence extends IRLGridWorldDemo{
	
	final int			N_EXPERT_FE_SAMPLES = 30;
	final int			N_APPRENTICE_FE_SAMPLES = 5;

	public void euclideanDistVsIterations(int maxIterations, int nRuns, String outputPath){
		//0 - generate a random reward funciton on the gridworld
		PropositionalFunction[] featureFunctions = MacroGridWorld.getPropositionalFunctions(this.domain);
		Map<String, Double> rewards = MacroGridWorld.generateRandomRewards(featureFunctions, MacroGridWorld.MCELL_FILLED);
		RewardFunction randomReward = new ApprenticeshipLearning.FeatureBasedRewardFunction(featureFunctions, rewards);
		rf = randomReward;
		
		// 1 - Get Expert Policy (via VI)
		//create and instance of planner; discount is set to 0.99; the minimum delta threshold is set to 0.001
		ValueIteration planner = new ValueIteration(domain, randomReward, tf, GAMMA, hashingFactory, .01, 100);		
		
		//run planner from our initial state
		MacroGridWorld.setAgent(initialState, 0,0);
		planner.planFromState(initialState);
		
		//create a Q-greedy policy using the Q-values that the planner computes
		Policy expertPolicy = new GreedyQPolicy((QComputablePlanner)planner);
		
		//2 - Get Expert Trajectories
		//run a sample of the computed policy and write its results to the file "VIResult.episode" in the directory outputPath
		//a '.episode' extension is automatically added by the writeToFileMethod
		List<EpisodeAnalysis> expertEpisodes = new ArrayList<EpisodeAnalysis>();
		for (int i =0; i < N_EXPERT_FE_SAMPLES; i++) {
			//MacroGridWorld.setAgent(initialState, (int)(Math.random()*MacroGridWorld.WIDTH), (int)(Math.random()*MacroGridWorld.HEIGHT));
			EpisodeAnalysis episode = expertPolicy.evaluateBehavior(initialState, randomReward, tf,100);
			expertEpisodes.add(episode);
			episode.writeToFile(outputPath +"/traj/"+ "Expert_"+i, sp);
		}
		
		//3 - calculate expert feature expectation estimate
		double[] expertFExp = ApprenticeshipLearning.estimateFeatureExpectation(expertEpisodes, featureFunctions, GAMMA);
		
		//
		//	Run experiment
		//
		
		double [] results = new double[maxIterations];
		long start = System.currentTimeMillis();
		
		for (int run = 0; run < nRuns; run++) {
			double[] featureWeightScoreHistory = new double[maxIterations];
			
			Policy projPolicy = 
					ApprenticeshipLearning.projectionMethod(
							this.domain, planner, featureFunctions, expertEpisodes, GAMMA, FEXP_EPSILON, maxIterations, 
							ApprenticeshipLearning.FEATURE_EXPECTATION_SAMPLES, featureWeightScoreHistory);
			List<EpisodeAnalysis> apprenticeEpisodes = new ArrayList<EpisodeAnalysis>();
			for (int i = 0; i < maxIterations; ++i) {
				results[i] += featureWeightScoreHistory[i]*(1-GAMMA);
				}
			System.out.print(run+", ");
		}
		for (int i = 0; i < maxIterations; ++i) {
			results[i] /= nRuns;
		}
		
		/*
		for (int maxi = 0; maxi < maxIterations; maxi++) {
			//run n i-length iterations
			System.out.print("iteration: " + maxi + ", run: ");
			
			for (int run = 0; run < nRuns; run++) {
				double[] featureWeightScoreHistory = new double[maxi];
				
				Policy projPolicy = 
						ApprenticeshipLearning.projectionMethod(
								this.domain, planner, featureFunctions, expertEpisodes, GAMMA, FEXP_EPSILON, maxi, 
								ApprenticeshipLearning.FEATURE_EXPECTATION_SAMPLES, featureWeightScoreHistory);
				List<EpisodeAnalysis> apprenticeEpisodes = new ArrayList<EpisodeAnalysis>();
				/*for (int s = 0; s < 1; s++) {
					MacroGridWorld.setAgent(initialState, (int)(Math.random()*MacroGridWorld.WIDTH), (int)(Math.random()*MacroGridWorld.HEIGHT));
					EpisodeAnalysis projectionEpisode = projPolicy.evaluateBehavior(initialState, rf, tf, 100);
					apprenticeEpisodes.add(projectionEpisode);
					
					projectionEpisode.writeToFile(outputPath +"/traj/"+ "Projection_"+maxi+"_"+run+"_"+s, sp);
				}*/
				//double[] apprenticeFExp = ApprenticeshipLearning.estimateFeatureExpectation(apprenticeEpisodes, featureFunctions, GAMMA);
				/*
				if (featureWeightScoreHistory.length > 0) {
					results[maxi][run] = featureWeightScoreHistory[featureWeightScoreHistory.length -1]*(1-GAMMA);
						//euclideanDist(apprenticeFExp, expertFExp, true);
					System.out.println(featureWeightScoreHistory.length);
					//System.out.println("nth: "+featureWeightScoreHistory.get(featureWeightScoreHistory.size() -1));
				}
				else {
					results[maxi][run] = 0;
				}
				System.out.print(run+", ");
			}
			System.out.println(" time: " +(System.currentTimeMillis()-start));
			start = System.currentTimeMillis();
		}*/
		
		//Write results to file
		File f = (new File(outputPath+"/"));
		f.mkdirs();
		
		try {
			
			BufferedWriter br = new BufferedWriter(new FileWriter(outputPath+"/"+"projection.csv"));
			StringBuilder sb = new StringBuilder();
			
			for (int i = 0; i < results.length; i++) {
				//double[] maxi = results[i];
				sb.append("maxi_"+(i));
				sb.append(", ");
				//for (double run : maxi) {
				//	sb.append(run);
				//	sb.append(",");
				//}
				sb.append(results[i]);
				sb.append("\n");
			}
			br.write(sb.toString());
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	

	private static double euclideanDist(double[] a, double[] b, boolean rescale) {
		double norm = 0.0;
		for (double aVal : a) {
			for (double bVal : b) {
				if (rescale) {
					norm += (aVal-bVal)*(1-GAMMA)*(aVal-bVal)*(1-GAMMA);
				}
				else {
					norm += (aVal-bVal)*(aVal-bVal);
				}
			}
		}
		return Math.sqrt(norm);
	}
	
	public static void main(String [] args) {
		TestConvergence tester = new TestConvergence();
		String outputPath = "distVsIterations"; //directory to record results
		tester.euclideanDistVsIterations(30, 5, outputPath);
		//tester.visualizeEpisode(outputPath+"/traj/");
	}
}

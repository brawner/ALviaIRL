import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.joptimizer.functions.ConvexMultivariateRealFunction;
import com.joptimizer.functions.LinearMultivariateRealFunction;
import com.joptimizer.functions.QuadraticMultivariateRealFunction;
import com.joptimizer.optimizers.JOptimizer;
import com.joptimizer.optimizers.OptimizationRequest;
import com.joptimizer.optimizers.OptimizationResponse;
import com.joptimizer.util.Utils;

import burlap.oomdp.core.State;

/**
 * Class of feature weights which contain the weight values and the associated score given to them
 * @author brawner
 *
 */
public class FeatureWeights {
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
	
	/**
	 * FeatureWeight factory which solves the best weights given Feature Expectations calculated from
	 * the expert demonstrations and a history of Feature Expectations.
	 * @param expertExpectations Feature Expectations calculated from the expert demonstrations
	 * @param featureExpectations Feature History of feature expectations generated from past policies
	 * @return
	 */
	public static FeatureWeights solveFeatureWeights(FeatureExpectations expertExpectations, List<FeatureExpectations> featureExpectations) {
		// We are solving a Quadratic Programming Problem here, yay!
		// Solve equation of form xT * P * x + qT * x + r
		// Let x = {w0, w1, ... , wn, t}
		
		int weightsSize = expertExpectations.getValues().length;
		
		
		// The objective is to maximize t, or minimize -t
		double[] qObjective = new double[weightsSize + 1];
		qObjective[weightsSize] = -1;
		LinearMultivariateRealFunction objectiveFunction = new LinearMultivariateRealFunction( qObjective, 0);
		
		// We set the requirement that all feature expectations generated have to be less than the expert
		List<ConvexMultivariateRealFunction> expertBasedWeightConstraints = new ArrayList<ConvexMultivariateRealFunction>();
		
		// (1/2)xT * Pi * x + qiT + ri ≤ 0
		// Equation (11) wT * uE >= wT * u(j) + t ===>  (u(j) - uE)T * w + t <= 0
		// Because x = {w0, w1, ... , wn, t}, we can set
		// qi = {u(j)_1 - uE_1, ... , u(j)_n - uE_n, 1}
		// TODO Why does http://www.joptimizer.com/qcQuadraticProgramming.html say (1/2)xT * Pi * x + qiT + ri
		// not (1/2)xT * Pi * x + qiT * x + ri
		double[] expertValues = expertExpectations.getValues();
		for (FeatureExpectations expectations : featureExpectations) {
			double[] expectationValues = expectations.getValues();
			double[] difference = new double[weightsSize + 1];
			for (int i = 0; i < difference.length; ++i) {
				difference[i] = expectationValues[i] - expertValues[i];
			}
			difference[weightsSize] = 1;
			expertBasedWeightConstraints.add(new LinearMultivariateRealFunction(difference, 1));
		}
		
		// L2 norm of weights must be less than or equal to 1. So 
		// P = Identity, except for the last entry (which cancels t).
		double[][] identityMatrix = Utils.createConstantDiagonalMatrix(weightsSize + 1, 1);
		identityMatrix[weightsSize][weightsSize] = 0;
		expertBasedWeightConstraints.add(new QuadraticMultivariateRealFunction(identityMatrix, null, -1));
		
		OptimizationRequest optimizationRequest = new OptimizationRequest();
		optimizationRequest.setF0(objectiveFunction);
		optimizationRequest.setFi(expertBasedWeightConstraints.toArray(new ConvexMultivariateRealFunction[expertBasedWeightConstraints.size()]));
		optimizationRequest.setCheckKKTSolutionAccuracy(true);
		
		optimizationRequest.setA(new double[weightsSize + 1][weightsSize + 1]);
		optimizationRequest.setB(new double[weightsSize + 1]);
		optimizationRequest.setTolerance(1.E-12);
		optimizationRequest.setToleranceFeas(1.E-12);
		
		JOptimizer optimizer = new JOptimizer();
		optimizer.setOptimizationRequest(optimizationRequest);
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
	public static FeatureExpectations projectExpertFE(FeatureExpectations expertFE,
														FeatureExpectations lastFE,
														FeatureExpectations lastProjFE) {

		//otherwise
		double[] expertExp = expertFE.getValues();
		double[] lastExp = lastFE.getValues();
		double[] lastProjExp = lastProjFE.getValues();
		double[] newProjExp = new double[lastProjExp.length];
		
		for (int i = 0; i < newProjExp.length; i++) {
			//mu_bar^(i-2) + (mu^(i-1)-mu_bar^(i-2))*
			//((mu^(i-1)-mu_bar^(i-2))*(mu_E-mu_bar^(i-2)))/
			//((mu^(i-1)-mu_bar^(i-2))*(mu(i-1)-mu_bar^(i-2)))
			newProjExp[i] = lastProjExp[i] + (lastExp[i]-lastProjExp[i])*
								((lastExp[i]-lastProjExp[i])*(expertExp[i]-lastProjExp[i]))/
								((lastExp[i]-lastProjExp[i])*(lastExp[i]-lastProjExp[i]));
		}
		
		return new FeatureExpectations(newProjExp);
	}
	
	/**
	 * This takes the Expert's feature expectation and a projection, and calculates the weight
	 * and score. This is step 2b of the projection method.
	 * 
	 * @param expertFE
	 * @param newProjFE
	 * @return
	 */
	public static FeatureWeights getWeightsProjectionMethod(FeatureExpectations expertFE, FeatureExpectations newProjFE){
		
		double[] expertExp = expertFE.getValues();
		double[] newProjExp = newProjFE.getValues();
		
		//set the weight as the expert's feature expecation minus the new projection
		double[] weights = new double[newProjExp.length];
		for (int i = 0; i < weights.length; i++) {
			weights[i] = expertExp[i] - newProjExp[i];
		}
		
		//set the score (t) as the L2 norm of the weight
		double score = 0;
		for (double w : weights) score += w*w;
		score = Math.sqrt(score);
		
		return new FeatureWeights(weights, score);
	}
}

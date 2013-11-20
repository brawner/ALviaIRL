
/** 
 * Class that wraps the values of feature expectations
 * @author brawner
 *
 */
public class FeatureExpectations {
		private double[] featureExpectations;
		public FeatureExpectations(double[] featureExpectations) {
			this.featureExpectations = featureExpectations.clone();
		}
		
		public FeatureExpectations(FeatureExpectations featureExpectations) {
			this.featureExpectations = featureExpectations.getValues();
		}
		
		public double[] getValues() {
			return this.featureExpectations.clone();
		}
	}


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

//TODO 
public class FeatureMappingRF implements RewardFunction {
	private Map<String, Double> rewards;
	private PropositionalFunction[] propositionalFunctions;
	
	public FeatureMappingRF(PropositionalFunction[] functions, Map<String, Double> rewards) {
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

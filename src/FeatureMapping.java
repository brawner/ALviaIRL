import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;

/**
 * Class for storing a mapping of states to a vector of values indicating features.
 * @author brawner
 *
 */
public class FeatureMapping {
	private PropositionalFunction[] propositionalFunctions;
	
	private FeatureMapping(PropositionalFunction[] functions)
	{
		this.propositionalFunctions = functions.clone();
	}
	
	public FeatureMapping(FeatureMapping featureMapping) {
		this.propositionalFunctions = featureMapping.getPropositionalFunctions();
	}

	/**
	 * Factory method for generating feature mappings
	 * @param stateSequence A List of states
	 * @param mappedValues The mapped values of the states
	 * @return
	 */
	public static FeatureMapping CreateFeatureMapping(PropositionalFunction[] propositionalFunctions)
	{
		if (propositionalFunctions == null || propositionalFunctions.length == 0) {
			return null;
		}
		return new FeatureMapping(propositionalFunctions);
	}	
	
	public PropositionalFunction[] getPropositionalFunctions() {
		return this.propositionalFunctions.clone();
	}
	
	public int getFeatureSize() {
		return this.propositionalFunctions.length;
	}
}

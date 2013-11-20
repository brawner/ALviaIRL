import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import burlap.oomdp.core.State;

/**
 * Class for storing a mapping of states to a vector of values indicating features.
 * @author brawner
 *
 */
public class FeatureMapping {
	private Map<State, double[]> mapping;
	
	private FeatureMapping(List<State> stateSequence, List<double[]> mappedValues)
	{
		this.mapping = new HashMap<State, double[]>(stateSequence.size());
		Iterator<State> stateIterator = null;
		Iterator<double[]> valueIterator = null;
		for (stateIterator = stateSequence.iterator(), valueIterator = mappedValues.iterator(); 
				stateIterator.hasNext() && valueIterator.hasNext();) {
			this.mapping.put((State)stateIterator.next(), (double[])valueIterator.next());
		}
	}
	
	public FeatureMapping(FeatureMapping featureMapping) {
		this.mapping = new HashMap<State, double[]>(featureMapping.mapping);
	}

	/**
	 * Factory method for generating feature mappings
	 * @param stateSequence A List of states
	 * @param mappedValues The mapped values of the states
	 * @return
	 */
	public static FeatureMapping CreateFeatureMapping(List<State> stateSequence, List<double[]> mappedValues)
	{
		if (stateSequence == null || mappedValues == null) {
			return null;
		}
		if (stateSequence.size() != mappedValues.size()) {
			return null;
		}
		return new FeatureMapping(stateSequence, mappedValues);
	}	
	
	public double[] getMappedStateValue(State state) {
		return this.mapping.get(state);
	}
	
	public int getFeatureSize() {
		if (this.mapping.size() > 0) {
			return this.mapping.values().iterator().next().length;
		}
		return 0;
	}
}

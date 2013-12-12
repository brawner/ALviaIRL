package burlap.behavior.singleagent;
import java.util.List;
import java.util.Random;

import burlap.behavior.singleagent.auxiliary.StateReachability;
import burlap.behavior.statehashing.NameDependentStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.SADomain;


public class RandomStartStateGenerator implements StateGenerator {
	private List<State> reachableStates;
	private Random 		random;
	
	public RandomStartStateGenerator(SADomain domain, State seedState) {
		StateHashFactory hashFactory = new NameDependentStateHashFactory();
		this.reachableStates = StateReachability.getReachableStates(seedState, domain, hashFactory);
		this.random = new Random();
	}

	@Override
	public State generateState() {
		return this.reachableStates.get(this.random.nextInt(this.reachableStates.size()));
	}
}

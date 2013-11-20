import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import burlap.behavior.singleagent.Policy;
import burlap.behavior.statehashing.NameDependentStateHashFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.behavior.statehashing.StateHashTuple;

/**
 * This class extends Policy, and all it does is create a randomly generated distribution of
 * actions over all possible states. It lazily initializes because I have no idea what sorts 
 * of states you are passing it.
 * @author brawner
 *
 */
public class RandomPolicy extends Policy {
	Map<StateHashTuple, GroundedAction> stateActionMapping;
	List<Action> actions;
	Map<StateHashTuple, List<ActionProb>> stateActionDistributionMapping;
	StateHashFactory hashFactory;
	Random rando;
	
	/**
	 * Constructor initializes the policy, doesn't compute anything here.
	 * @param domain Domain object for which we need to plan
	 */
	public RandomPolicy(Domain domain) {
		this.stateActionMapping = new HashMap<StateHashTuple, GroundedAction>();
		this.actions = domain.getActions();
		this.rando = new Random();
		this.hashFactory = new NameDependentStateHashFactory();
	}

	/**
	 * For states which we have not yet visited, this policy needs a randomly generated distribution
	 * of actions. It queries all the grounded actions possible from this state and assigns a random
	 * probability to it. The probabilities are all normalized for happiness.
	 * @param state State for which to generate actions.
	 */
	private void addNewDistributionForState(State state) {
		StateHashTuple stateHashTuple = this.hashFactory.hashState(state);
		
		// Get all possible actions from this state
		List<GroundedAction> groundedActions = state.getAllGroundedActionsFor(this.actions);
		Double[] probabilities = new Double[groundedActions.size()];
		Double sum = 0.0;
		
		// Create a random distribution of doubles
		for (int i = 0; i < probabilities.length; ++i) {
			probabilities[i] = this.rando.nextDouble();
			sum += probabilities[i];
		}
		
		List<ActionProb> newActionDistribution = new ArrayList<ActionProb>(groundedActions.size());
		// Normalize distribution and add a new ActionProb to our list.
		for (int i = 0; i < probabilities.length; ++i) {
			ActionProb actionProb = new  ActionProb(groundedActions.get(i), probabilities[i] / sum);
			newActionDistribution.add(actionProb);
		}
		
		this.stateActionDistributionMapping.put(stateHashTuple, newActionDistribution);
	}
	
	@Override
	public GroundedAction getAction(State s) {
		StateHashTuple stateHashTuple = this.hashFactory.hashState(s);
		
		// If this state has not yet been visited, we need to compute a new distribution of actions
		if (!this.stateActionDistributionMapping.containsKey(stateHashTuple)) {
			this.addNewDistributionForState(s);
		}
		
		// Get the action probability distribution for this state
		List<ActionProb> actionDistribution = this.stateActionDistributionMapping.get(stateHashTuple);
		Double roll = this.rando.nextDouble();
		Double probabilitySum = 0.0;
		
		// Choose an action randomly from this distribution
		for (ActionProb actionProb : actionDistribution) {
			probabilitySum += actionProb.pSelection;
			if (probabilitySum >= roll) {
				return actionProb.ga;
			}
		}
		return null;
	}

	@Override
	public List<ActionProb> getActionDistributionForState(State s) {
		StateHashTuple stateHashTuple = this.hashFactory.hashState(s);
		
		// If this state has not yet been visited, we need to compute a new distribution of actions
		if (!this.stateActionDistributionMapping.containsKey(stateHashTuple)) {
			this.addNewDistributionForState(s);
		}
		return new ArrayList<ActionProb>(this.stateActionDistributionMapping.get(stateHashTuple));
	}

	@Override
	public boolean isStochastic() {
		return true;
	}

}

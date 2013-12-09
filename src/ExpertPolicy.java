import java.util.List;

import burlap.behavior.singleagent.Policy;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;


public class ExpertPolicy extends Policy {
	private int goalX;
	private int goalY;
	private Action north;
	private Action south;
	private Action west;
	private Action east;
	
	public ExpertPolicy(int maxX, int maxY, 
			Action northAction, Action southAction, Action westAction, Action eastAction) {
		this.goalX = (int)Math.floor( maxX * 0.75);
		this.goalY = (int)Math.floor(maxY * 0.75);
		this.north = northAction;
		this.south = southAction;
		this.east = eastAction;
		this.west = westAction;
	}

	@Override
	public GroundedAction getAction(State s) {
		ObjectInstance agent = s.getObjectsOfTrueClass(MacroGridWorld.CLASSAGENT).get(0);
		int attX = agent.getDiscValForAttribute(MacroGridWorld.ATTX);
		int attY = agent.getDiscValForAttribute(MacroGridWorld.ATTY);
		Action action = this.south;
		if (attX < this.goalX){
			action = this.east;
		} else if (attX > this.goalX) {
			action = this.west;
		} else if (attY < this.goalY) {
			action = this.north;
		}
		return s.getAllGroundedActionsFor(action).get(0);
	}

	@Override
	public boolean isStochastic() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<ActionProb> getActionDistributionForState(State s) {
		// TODO Auto-generated method stub
		return null;
	}

}

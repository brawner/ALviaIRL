import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;

public class IRLGridRF implements RewardFunction {

	int nCells;
	List<Integer> mcellR;
	/**
	 * @low lower bound on rewards
	 * @high upper bound on rewards
	 * @nCells number of cells
	 * @nFilled number of cells to fill with non-zero rewards
	 * 
	 */
	public IRLGridRF(List<Integer> macroCellRewards) {
		this.nCells = macroCellRewards.size();
		this.mcellR = macroCellRewards;
	}
	
	public List<Integer> getCellRewards() {
		return this.mcellR;
	}
	
	@Override
	public double reward(State s, GroundedAction a, State sprime) {
		ObjectInstance agent = sprime.getObjectsOfTrueClass(IRLGridWorld.CLASSAGENT).get(0);
		
		//Figure out which macrocell it is in
		int ax = agent.getDiscValForAttribute(IRLGridWorld.ATTX);
		int ay = agent.getDiscValForAttribute(IRLGridWorld.ATTY);
		int amx = (ax*IRLGridWorld.MCELL_WIDTH)/IRLGridWorld.WIDTH;
		int amy = (ay*IRLGridWorld.MCELL_HEIGHT)/IRLGridWorld.HEIGHT;
		int ami = amy*IRLGridWorld.MCELL_WIDTH+amx;

		return (double)this.mcellR.get(ami);
	}

}

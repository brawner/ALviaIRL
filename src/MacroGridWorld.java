import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;


// TODO Rename to MacroCell gridworld
// TODO remove commented code
// TODO move in InMacroCellPF here
public class MacroGridWorld extends GridWorldDomain{
	
	public static final String							MCELL_INDEX = "mcelli";
	public static final String							MCELL_REWARD = "mcellreward";
	public static final String							CLASSMCELL = "mcellclass";
	public static final String							PFINMCELL = "inMacrocell";
	public static final String							PFINREWARDMCELL = "inRewardingMacrocell";
	public static final String							ATTSTEPS = "agentstepsattribute";
	public static final int								MIN_REWARD = 1;
	public static final int								MAX_REWARD = 10;
	public static final int								HEIGHT = 20;
	public static final int								WIDTH = 20;
	
	public static final int								MCELL_HEIGHT = 10;
	public static final int								MCELL_WIDTH = 10;
	public static final int								MCELL_COUNT = MCELL_HEIGHT*MCELL_WIDTH;
	public static final int								MCELL_FILLED = 2;
	

	
	public MacroGridWorld() {
		super(WIDTH, HEIGHT); //default gridworld
		
		//There are 4 actions (cardinal directions)
		// 30% chance action goes in one of the other 3
		// directions
		this.setProbSucceedTransitionDynamics(.7);
	}
	
	
	@Override
	public Domain generateDomain() {
		return super.generateDomain();
	}
	
	/**
	 * returns a state with one agent and all the macro cells set up
	 * 
	 * @param d
	 * @return
	 */
	public static State getOneAgentState(Domain d) {
		State s = new State();
		s.addObject(new ObjectInstance(d.getObjectClass(CLASSAGENT), CLASSAGENT+0));
		return s;
	}

	public static PropositionalFunction[] getPropositionalFunctions(Domain domain) {
		int width = MacroGridWorld.WIDTH / MacroGridWorld.MCELL_WIDTH;
		int height = MacroGridWorld.HEIGHT / MacroGridWorld.MCELL_HEIGHT;
		int count = MacroGridWorld.MCELL_WIDTH * MacroGridWorld.MCELL_HEIGHT;
		PropositionalFunction[] functions = new PropositionalFunction[count];
		int index = 0;
		for (int i = 0; i < MacroGridWorld.MCELL_WIDTH; ++i) {
			int x = i * width;
			for (int j = 0; j < MacroGridWorld.MCELL_HEIGHT; ++j) {
				int y = j * height;
				functions[index] = new InMacroCellPF(domain, x, y, width, height);
				index++;
			}
		}
		return functions;
	}
	
	public static Map<String, Double> generateRandomRewards(PropositionalFunction[] functions, int numberFilled) {
		
		Random rando = new Random();
		Double[] mrewards = new Double[functions.length];
		for (int i = 0; i < MCELL_COUNT; i++) {
			mrewards[i] = (i < MCELL_FILLED) ? rando.nextDouble() * (MAX_REWARD - MIN_REWARD) + MIN_REWARD : 0;
		}
	
		List<Double> rewardList = Arrays.asList(mrewards);
		Collections.shuffle(rewardList);
		
		Map<String, Double> rewards = new HashMap<String, Double>();
		for (int i = 0; i < functions.length; i++) {
			rewards.put(functions[i].getName(), rewardList.get(i));
			System.out.println(functions[i].getName() + " reward: " + rewardList.get(i));
		}
		return rewards;
	}

	public static class InMacroCellPF extends PropositionalFunction{
		private int left, right, top, bottom;
		
		public InMacroCellPF(Domain domain, int x, int y, int width, int height) {
			super("[" + x + ", " + y + "]", domain, "");
			this.left = x;
			this.right = x + width;
			this.bottom = y;
			this.top = y + width;
		}

		@Override
		public boolean isTrue(State state, String[] params) {
			List<ObjectInstance> agents = state.getObjectsOfTrueClass(MacroGridWorld.CLASSAGENT);
			if (agents.size() == 0) {
				return false;
			}
			ObjectInstance agent = agents.get(0);
			int agentX = agent.getDiscValForAttribute(MacroGridWorld.ATTX);
			int agentY = agent.getDiscValForAttribute(MacroGridWorld.ATTY);
			return (left <= agentX && agentX <= right &&
					bottom <= agentY && agentY <= top);
				
		}
	}
	

	
	
}
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldDomain.AtLocationPF;
import burlap.domain.singleagent.gridworld.GridWorldDomain.MovementAction;
import burlap.domain.singleagent.gridworld.GridWorldDomain.WallToPF;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.RewardFunction;



public class IRLGridWorld extends GridWorldDomain{
	
	public static final String							MCELL_INDEX = "mcelli";
	public static final String							MCELL_REWARD = "mcellreward";
	public static final String							CLASSMCELL = "mcellclass";
	public static final String							PFINMCELL = "inMacrocell";
	public static final String							PFINREWARDMCELL = "inRewardingMacrocell";
	public static final String							ATTSTEPS = "agentstepsattribute";
	public static final int								MIN_REWARD = 1;
	public static final int								MAX_REWARD = 10;
	public static final int								HEIGHT = 8;
	public static final int								WIDTH = 8;
	
	public static final int								MCELL_HEIGHT = 4;
	public static final int								MCELL_WIDTH = 4;
	public static final int								MCELL_COUNT = MCELL_HEIGHT*MCELL_WIDTH;
	public static final int								MCELL_FILLED = 4;
	

	
	public IRLGridWorld() {
		super(WIDTH, HEIGHT); //default gridworld
		
		//There are 4 actions (cardinal directions)
		// 30% chance action goes in one of the other 3
		// directions
		this.setProbSucceedTransitionDynamics(.7);
	}
	
	
	@Override
	public Domain generateDomain() {
		Domain DOMAIN = super.generateDomain();
		
		//Macrocell Attributes
		Attribute mcelli = new Attribute(DOMAIN, MCELL_INDEX, Attribute.AttributeType.DISC);
		mcelli.setDiscValuesForRange(0, MCELL_COUNT-1, 1);
		Attribute mcellr = new Attribute(DOMAIN, MCELL_REWARD, Attribute.AttributeType.DISC);
		mcellr.setDiscValuesForRange(MIN_REWARD, MAX_REWARD, 1);
		
		//Macrocell object
		ObjectClass macrocellClass = new ObjectClass(DOMAIN, CLASSMCELL);
		macrocellClass.addAttribute(mcelli);
		macrocellClass.addAttribute(mcellr);
		
		PropositionalFunction inMacrocell = new InMacroCell(PFINMCELL, DOMAIN, new String[]{CLASSAGENT,CLASSMCELL});
		PropositionalFunction inRewardingMacrocell = new InRewardingMacroCell(PFINREWARDMCELL, DOMAIN, new String[]{CLASSAGENT,CLASSMCELL});

		return DOMAIN;
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
		
		//generate random small values for macrocells
		List<Integer> mrewards = new ArrayList<Integer>();
		for (int i = 0; i < MCELL_COUNT; i++) {
			if (i < MCELL_FILLED) {
				mrewards.add((int)(Math.random()*((MAX_REWARD-MIN_REWARD)+1)));
			}
			else {
				mrewards.add(0);
			}
		}
		Collections.shuffle(mrewards);
		
		for (int i = 0; i < MCELL_COUNT; i++) {
			ObjectInstance o = new ObjectInstance(d.getObjectClass(CLASSMCELL), CLASSMCELL+i);
			o.setValue(MCELL_INDEX, i);
			o.setValue(MCELL_REWARD, mrewards.get(i));
			s.addObject(o);
		}
		
		return s;
	}
	
	public List<Integer> getMacroCellRewards(State s) {
		List<Integer> mrewards = new ArrayList<Integer>();
		
		List<ObjectInstance> mcells = s.getObjectsOfTrueClass(CLASSMCELL);
		for (int i = 0; i < mcells.size(); i++) {
			mrewards.add(mcells.get(i).getDiscValForAttribute(MCELL_REWARD));
		}
		return mrewards;
	}
	
	public static PropositionalFunction[] getPropositionalFunctions(Domain domain) {
		int numWidth = IRLGridWorld.WIDTH / IRLGridWorld.MCELL_WIDTH;
		int numHeight = IRLGridWorld.HEIGHT / IRLGridWorld.MCELL_HEIGHT;
		int count = numWidth * numHeight;
		PropositionalFunction[] functions = new PropositionalFunction[count];
		int index = 0;
		for (int i = 0; i < numWidth; ++i) {
			int x = i * IRLGridWorld.MCELL_WIDTH;
			for (int j = 0; j < numHeight; ++j) {
				int y = j * IRLGridWorld.MCELL_HEIGHT;
				functions[index] = new InMacroCellPF(domain, x, y, IRLGridWorld.MCELL_WIDTH, IRLGridWorld.MCELL_HEIGHT);
				index++;
			}
		}
		return functions;
	}
	
	public static Map<String, Double> generateRandomRewards(PropositionalFunction[] functions) {
		Map<String, Double> rewards = new HashMap<String, Double>();
		Random rando = new Random();
		for (PropositionalFunction function : functions) {
			double reward = rando.nextDouble();
			
			rewards.put(function.getName(), reward);
		}
		return rewards;
	}
	/**
	 * Propositional function for determining if the agent is
	 * in a particular Macrocell
	 * 
	 * @author Mark Ho
	 *
	 */
	public class InMacroCell extends PropositionalFunction{

		public InMacroCell(String name, Domain domain, String[] strings) {
			super(name, domain, strings);
		}

		@Override
		public boolean isTrue(State s, String[] params) {
			// TODO Auto-generated method stub
			ObjectInstance agent = s.getObject(params[0]);
			ObjectInstance macrocell = s.getObject(params[1]);
			
			//check if the agent's x,y coordinates are in the macrocell
			int ax = agent.getDiscValForAttribute(ATTX);
			int ay = agent.getDiscValForAttribute(ATTY);
			int amx = (ax*MCELL_WIDTH)/WIDTH;
			int amy = (ay*MCELL_HEIGHT)/HEIGHT;
			int ami = amy*MCELL_WIDTH+amx;
			
			int mi = macrocell.getDiscValForAttribute(MCELL_INDEX);
			
			if(ami == mi) {
				return true;
			}
			
			return false;
		}
	}
	
	public class InRewardingMacroCell extends PropositionalFunction{

		public InRewardingMacroCell(String name, Domain domain, String[] strings) {
			super(name, domain, strings);
		}

		@Override
		public boolean isTrue(State s, String[] params) {
			// TODO Auto-generated method stub
			ObjectInstance agent = s.getObject(params[0]);
			ObjectInstance macrocell = s.getObject(params[1]);
			
			//check if the agent's x,y coordinates are in the macrocell
			int ax = agent.getDiscValForAttribute(ATTX);
			int ay = agent.getDiscValForAttribute(ATTY);
			int amx = (ax*MCELL_WIDTH)/WIDTH;
			int amy = (ay*MCELL_HEIGHT)/HEIGHT;
			int ami = amy*MCELL_WIDTH+amx;
			
			int mi = macrocell.getDiscValForAttribute(MCELL_INDEX);
			int mr = macrocell.getDiscValForAttribute(MCELL_REWARD);
			
			if(ami == mi &&  mr > 0) {
				return true;
			}
			
			return false;
		}
	}
	
	

	
	
}

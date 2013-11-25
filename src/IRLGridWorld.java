import java.util.ArrayList;
import java.util.Arrays;
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


// TODO Rename to MacroCell gridworld
// TODO remove commented code
// TODO move in InMacroCellPF here
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
	public static final int								MCELL_FILLED = 1;
	

	
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
		//Attribute mcellr = new Attribute(DOMAIN, MCELL_REWARD, Attribute.AttributeType.DISC);
		//mcellr.setDiscValuesForRange(MIN_REWARD, MAX_REWARD, 1);
		
		//Macrocell object
		ObjectClass macrocellClass = new ObjectClass(DOMAIN, CLASSMCELL);
		macrocellClass.addAttribute(mcelli);
		//macrocellClass.addAttribute(mcellr);
		
		//PropositionalFunction inMacrocell = new InMacroCell(PFINMCELL, DOMAIN, new String[]{CLASSAGENT,CLASSMCELL});
		//PropositionalFunction inRewardingMacrocell = new InRewardingMacroCell(PFINREWARDMCELL, DOMAIN, new String[]{CLASSAGENT,CLASSMCELL});

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
		
		/*
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
		*/
		return s;
	}

	public static PropositionalFunction[] getPropositionalFunctions(Domain domain) {
		int width = IRLGridWorld.WIDTH / IRLGridWorld.MCELL_WIDTH;
		int height = IRLGridWorld.HEIGHT / IRLGridWorld.MCELL_HEIGHT;
		int count = IRLGridWorld.MCELL_WIDTH * IRLGridWorld.MCELL_HEIGHT;
		PropositionalFunction[] functions = new PropositionalFunction[count];
		int index = 0;
		for (int i = 0; i < IRLGridWorld.MCELL_WIDTH; ++i) {
			int x = i * width;
			for (int j = 0; j < IRLGridWorld.MCELL_HEIGHT; ++j) {
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

	
	

	
	
}

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.domain.singleagent.gridworld.GridWorldDomain.AtLocationPF;
import burlap.domain.singleagent.gridworld.GridWorldDomain.MovementAction;
import burlap.domain.singleagent.gridworld.GridWorldDomain.WallToPF;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer.CellPainter;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer.MapPainter;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.explorer.VisualExplorerRecorder;
import burlap.oomdp.visualizer.Visualizer;


public class DrivingGridWorld extends GridWorldDomain {
	public static final String agentClass = "agent";
	public static final String blockClass = "block";
	private int leftGrassRight;
	private int rightGrassLeft;
	public static final String ACTIONBLOCKMOVE = "moveblock";
	
	private int laneCount;
	private int laneWidth;
	
	public DrivingGridWorld(int[][] map) {
		
		super(map);
		
	}

	public DrivingGridWorld(int width, int height, int numLanes, int laneWidth) {
		super(width, height);
		this.laneWidth = laneWidth;
		if (laneWidth * numLanes > width) {
			laneWidth = width / numLanes;
		}
		if ((width - numLanes * laneWidth) % 2 != 0) {
			width++;
		}
			
		this.laneCount = numLanes;
		this.map = new int[width][height];
		int roadWidth = numLanes * laneWidth;
		this.leftGrassRight = width - roadWidth;
		this.rightGrassLeft = this.leftGrassRight + roadWidth;
		
		for (int i = 0; i < width; ++i) {
			if (i < leftGrassRight || i >= rightGrassLeft) {
				for (int j = 0; j < height; ++j) {
					this.map[i][j] = DrivingWorldVisualizer.grass;
				}
			}
			else {
				for (int j = 0; j < height; ++j) {
					this.map[i][j] = DrivingWorldVisualizer.lane;
				}
			}
		}
	}

	@Override
	public Domain generateDomain() {
		Domain domain= new SADomain();
		
		//Creates a new Attribute object
		Attribute xatt = new Attribute(domain, ATTX, Attribute.AttributeType.DISC);
		xatt.setDiscValuesForRange(0, this.width-1, 1); //-1 due to inclusivity vs exclusivity
		
		Attribute yatt = new Attribute(domain, ATTY, Attribute.AttributeType.DISC);
		yatt.setDiscValuesForRange(0, this.height-1, 1); //-1 due to inclusivity vs exclusivity
		
		ObjectClass agentClass = new ObjectClass(domain, CLASSAGENT);
		agentClass.addAttribute(xatt);
		agentClass.addAttribute(yatt);
		
		ObjectClass block = new ObjectClass(domain, DrivingGridWorld.blockClass);
		block.addAttribute(xatt);
		block.addAttribute(yatt);
		
		ObjectClass locationClass = new ObjectClass(domain, CLASSLOCATION);
		locationClass.addAttribute(xatt);
		locationClass.addAttribute(yatt);
		
		Action north = new DrivingAction(ACTIONNORTH, domain, this.transitionDynamics[0], this.height);
		Action south = new DrivingAction(ACTIONSOUTH, domain, this.transitionDynamics[1], this.height);
		Action east = new DrivingAction(ACTIONEAST, domain, this.transitionDynamics[2], this.height);
		Action west = new DrivingAction(ACTIONWEST, domain, this.transitionDynamics[3], this.height);
		
		
		PropositionalFunction atLocationPF = new AtLocationPF(PFATLOCATION, domain, new String[]{CLASSAGENT, CLASSLOCATION});
		
		PropositionalFunction wallToNorthPF = new WallToPF(PFWALLNORTH, domain, new String[]{CLASSAGENT}, 0);
		PropositionalFunction wallToSouthPF = new WallToPF(PFWALLSOUTH, domain, new String[]{CLASSAGENT}, 1);
		PropositionalFunction wallToEastPF = new WallToPF(PFWALLEAST, domain, new String[]{CLASSAGENT}, 2);
		PropositionalFunction wallToWestPF = new WallToPF(PFWALLWEST, domain, new String[]{CLASSAGENT}, 3);
		
		PropositionalFunction[] functions = DrivingGridWorld.getFeatureFunctions(domain, this);

		//new block actions
		
		Action blockmove = new MovementAction(DrivingGridWorld.ACTIONBLOCKMOVE, domain, this.transitionDynamics[0]);

		return domain;
	}
	
	public static State getOneAgentState(Domain d, int[] xLocations, int height, int blockCount) {
		State s = new State();
		ObjectInstance agent = new ObjectInstance(d.getObjectClass(agentClass), agentClass+0);
		agent.setValue(ATTX, xLocations[0]);
		agent.setValue(ATTY, 0);
		s.addObject(agent);
		Set<double[]> blockLocations = new HashSet<double[]>();
		Random random = new Random();
		
		while (blockLocations.size() < blockCount) {
			int x = xLocations[random.nextInt(xLocations.length)];
			int y = random.nextInt(height);;
			blockLocations.add(new double[]{x,y});
		}
		
		for (double[] point : blockLocations) {
			ObjectInstance block = new ObjectInstance(d.getObjectClass(blockClass), blockClass + point[0] + "_" + point[1]);
			block.setValue(ATTX, point[0]);
			block.setValue(ATTY, point[1]);
			s.addObject(block);
		}
		return s;
	}
	
	public List<EpisodeAnalysis> interactive(Domain domain, DrivingGridWorld gridWorld, State initialState){
		Visualizer v = DrivingWorldVisualizer.getVisualizer(domain, gridWorld.getMap());
		VisualExplorerRecorder exp = new VisualExplorerRecorder(domain, v, initialState);
		
		exp.addKeyAction("w", GridWorldDomain.ACTIONNORTH);
		exp.addKeyAction("s", GridWorldDomain.ACTIONSOUTH);
		exp.addKeyAction("d", GridWorldDomain.ACTIONEAST);
		exp.addKeyAction("a", GridWorldDomain.ACTIONWEST);
		
		
		List<EpisodeAnalysis> recordedEpisodes = new ArrayList<EpisodeAnalysis>();
		exp.initGUIAndRecord(recordedEpisodes);
		return recordedEpisodes;
	}
	
	public void visualizeEpisode(String outputPath, Domain domain, DrivingGridWorld gridWorld, StateParser stateParser){
		Visualizer v = DrivingWorldVisualizer.getVisualizer(domain, gridWorld.getMap());
		EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v, domain, stateParser, outputPath);
	}
	
	public static PropositionalFunction[] getFeatureFunctions(Domain domain, DrivingGridWorld driveGW) {		
		//features: near or on a block; on the grass; in a lane
		PropositionalFunction[] pfs = new PropositionalFunction[2 + driveGW.laneCount];
		
		pfs[0] = new NearBlockPF(domain);
		pfs[1] = new OnGrassPF(domain, driveGW.getMap());
		
		for (int laneNum = 0; laneNum < driveGW.laneCount; laneNum++) {
			pfs[laneNum+2] = new InLanePF(domain, laneNum, driveGW.leftGrassRight, driveGW.laneWidth);
		}
				
		return pfs;
	}
	
	/**
	 * True if you are touching or on top of a block
	 * @author markho
	 *
	 */
	public static class NearBlockPF extends PropositionalFunction {

		public NearBlockPF(Domain domain) {
			super("NearBlockPF", domain, "");
		}

		@Override
		public boolean isTrue(State s, String[] params) {
			// get blocks; calculate if near one of themh
			List<ObjectInstance> blocks = s.getObjectsOfTrueClass(blockClass);
			ObjectInstance agent = s.getFirstObjectOfClass(agentClass);
			int agentx = agent.getDiscValForAttribute(ATTX);
			int agenty = agent.getDiscValForAttribute(ATTY);
			
			for (ObjectInstance block : blocks) {
				int blockx = block.getDiscValForAttribute(ATTX);
				int blocky = block.getDiscValForAttribute(ATTY);
				
				//if touching or on
				if ((agentx-blockx)*(agentx-blockx) + (agenty-blocky)*(agenty-blocky) <= 2) {
					return true;
				}
			}
			
			return false;
		}
		
	}
	
	/**
	 * Returns true if the agent is on the grass
	 * @author markho
	 *
	 */
	public static class OnGrassPF extends PropositionalFunction {
		int[][] map;

		public OnGrassPF(Domain domain, int[][] map) {
			super("OnGrassPF", domain, "");
			this.map = map;
		}

		@Override
		public boolean isTrue(State s, String[] params) {
			ObjectInstance agent = s.getFirstObjectOfClass(agentClass);
			int x = agent.getDiscValForAttribute(ATTX);
			int y = agent.getDiscValForAttribute(ATTY);
			return this.map[x][y] == DrivingWorldVisualizer.grass;
		}
		
	}
	
	/**
	 * Return true if in a particular lane specified
	 * @author markho
	 *
	 */
	public static class InLanePF extends PropositionalFunction {

		
		private int laneMax;
		private int laneMin;

		public InLanePF(Domain domain, int laneNum, int roadLeftSide, int laneWidth) {
			super("InLanePF_"+laneNum, domain, "");
			this.laneMin = roadLeftSide + laneNum*laneWidth;
			this.laneMax = laneMin + laneWidth;
		}

		@Override
		public boolean isTrue(State s, String[] params) {
			ObjectInstance agent = s.getFirstObjectOfClass(agentClass);
			int agentx = agent.getDiscValForAttribute(ATTX);
			
			if (agentx >= laneMin && agentx < laneMax) {
				return true;
			}
			
			return false;
		}
		
	}
	
	
	
	public class DrivingAction extends MovementAction {

		private int height;
		//private Random rando;
		//private double probMove;

		public DrivingAction(String name, Domain domain, double[] directions, int height) {
			super(name, domain, directions);
			this.height = height;
			//this.rando = new Random();
			//this.probMove = probMove;
		}
		
		@Override
		protected State performActionHelper(State st, String[] params) {
			
			//update block locations
			List<ObjectInstance> blocks = st.getObjectsOfTrueClass(blockClass);
			for (ObjectInstance block : blocks) {
				//if (rando.nextDouble() > probMove) {
				//	continue;
				//}
				int y = block.getDiscValForAttribute(ATTY);
				if (y == 0) {
					block.setValue(ATTY, height);
				}
				else {
					block.setValue(ATTY, y-1);
				}
			}
			
			
			//move agent
			double roll = rand.nextDouble();
			double curSum = 0.;
			int dir = 0;
			for(int i = 0; i < directionProbs.length; i++){
				curSum += directionProbs[i];
				if(roll < curSum){
					dir = i;
					break;
				}
			}
			
			int [] dcomps = DrivingGridWorld.this.movementDirectionFromIndex(dir);
			DrivingGridWorld.this.move(st, dcomps[0], dcomps[1]);
			
			return st;
		}
		
	}

	public static Map<String, Double> generateRewards(PropositionalFunction[] featureFunctions) {
		// TODO need to generate a mapping between the feature propositional functions
		// TODO feel free to change the declaration or add different version of reward mappings
		return null;
	}
}



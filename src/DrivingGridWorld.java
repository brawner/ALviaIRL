import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.domain.singleagent.gridworld.GridWorldDomain.MovementAction;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer.CellPainter;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer.MapPainter;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.explorer.VisualExplorerRecorder;
import burlap.oomdp.visualizer.Visualizer;


public class DrivingGridWorld extends GridWorldDomain {
	public static final String agentClass = "agent";
	public static final String blockClass = "block";
	public static final String ACTIONBLOCKMOVE = "moveblock";
	
	private int laneCount;
	private int leftGrassRight;
	private int rightGrassLeft;
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
		this.laneCount = numLanes;
		this.map = new int[width][height];
		int roadWidth = numLanes * laneWidth;
		this.leftGrassRight = width - roadWidth;
		this.rightGrassLeft = this.leftGrassRight + roadWidth;
		
		for (int i = 0; i < width; ++i) {
			if (i <= leftGrassRight || i >= rightGrassLeft) {
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
		Domain domain = super.generateDomain();
		
		ObjectClass block = new ObjectClass(domain, DrivingGridWorld.blockClass);
		
		//new block actions
		
		Action blockmove = new MovementAction(DrivingGridWorld.ACTIONBLOCKMOVE, domain, this.transitionDynamics[0]);

		return domain;
	}
	
	public static State getOneAgentState(Domain d) {
		State s = new State();
		s.addObject(new ObjectInstance(d.getObjectClass(CLASSAGENT), CLASSAGENT+0));
		return s;
	}
	
	public List<EpisodeAnalysis> interactive(Domain domain, DrivingGridWorld gridWorld, State initialState){
		Visualizer v = GridWorldVisualizer.getVisualizer(domain, gridWorld.getMap());
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
		Visualizer v = GridWorldVisualizer.getVisualizer(domain, gridWorld.getMap());
		EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v, domain, stateParser, outputPath);
	}
	
	public static PropositionalFunction[] getFeatureFunctions(Domain domain, DrivingGridWorld driveGW) {		
		//features: near or on a block; on the grass; in a lane
		PropositionalFunction[] pfs = new PropositionalFunction[2 + driveGW.laneCount];
		
		pfs[0] = new NearBlockPF(domain);
		pfs[1] = new OnGrassPF(domain, driveGW.laneCount, driveGW.leftGrassRight);
		
		for (int laneNum = 0; laneNum < driveGW.laneCount; laneNum++) {
			pfs[laneNum+2] = new InLanePF(domain, laneNum, driveGW.leftGrassRight+1, driveGW.laneWidth);
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
			// get blocks; calculate if near one of them
			List<ObjectInstance> blocks = s.getObjectsOfTrueClass(blockClass);
			ObjectInstance agent = s.getFirstObjectOfClass(agentClass);
			int agentx = agent.getDiscValForAttribute(ATTX);
			int agenty = agent.getDiscValForAttribute(ATTY);
			
			for (ObjectInstance block : blocks) {
				int blockx = block.getDiscValForAttribute(ATTX);
				int blocky = block.getDiscValForAttribute(ATTY);
				
				//if touching or on
				if ((agentx-blockx)*(agentx-blockx) < 1 && (agenty-blocky)*(agenty-blocky) < 1) {
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
		private int leftBoundary;
		private int rightBoundary;

		public OnGrassPF(Domain domain, int leftBoundary, int rightBoundary) {
			super("OnGrassPF", domain, "");
			this.leftBoundary = leftBoundary;
			this.rightBoundary = rightBoundary;
		}

		@Override
		public boolean isTrue(State s, String[] params) {
			ObjectInstance agent = s.getFirstObjectOfClass(agentClass);
			int x = agent.getDiscValForAttribute(ATTX);
			if (x <= this.leftBoundary || x >= this.rightBoundary) {
				return true;
			}
			
			return false;
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
		private Random rando;
		private double probMove;

		public DrivingAction(String name, Domain domain, double[] directions, int height, double probMove) {
			super(name, domain, directions);
			this.height = height;
			this.rando = new Random();
			this.probMove = probMove;
		}
		
		@Override
		protected State performActionHelper(State st, String[] params) {
			
			//update block locations
			List<ObjectInstance> blocks = st.getObjectsOfTrueClass(blockClass);
			for (ObjectInstance block : blocks) {
				if (rando.nextDouble() > probMove) {
					continue;
				}
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



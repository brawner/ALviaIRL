import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer.CellPainter;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer.MapPainter;
import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.explorer.VisualExplorerRecorder;
import burlap.oomdp.visualizer.Visualizer;


public class DrivingGridWorld extends GridWorldDomain {
	public static final String agentClass = "agent";
	public static final String blockClass = "block";
	
	private int laneCount;
	
	public DrivingGridWorld(int[][] map) {
		super(map);
	}

	public DrivingGridWorld(int width, int height, int numLanes, int laneWidth) {
		super(width, height);
		if (laneWidth * numLanes > width) {
			laneWidth = width / numLanes;
		}
		this.laneCount = numLanes;
		this.map = new int[width][height];
		int roadWidth = numLanes * laneWidth;
		int leftGrassRight = width - roadWidth;
		int rightGrassLeft = leftGrassRight + roadWidth;
		
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
		
		//move blocks when a key is pressed
		
		List<EpisodeAnalysis> recordedEpisodes = new ArrayList<EpisodeAnalysis>();
		exp.initGUIAndRecord(recordedEpisodes);
		return recordedEpisodes;
	}
	
	public void visualizeEpisode(String outputPath, Domain domain, DrivingGridWorld gridWorld, StateParser stateParser){
		Visualizer v = GridWorldVisualizer.getVisualizer(domain, gridWorld.getMap());
		EpisodeSequenceVisualizer evis = new EpisodeSequenceVisualizer(v, domain, stateParser, outputPath);
	}
	
	public static PropositionalFunction[] getFeatureFunctions(Domain domain) {
		// TODO need to return an array of PropositionalFunctions which correspond to the
		// features available in DrivingGridWorld.
		// TODO feel free to change the declaration.
		
		//features: near a block
		return null;
	}
	
	public static Map<String, Double> generateRewards(PropositionalFunction[] featureFunctions) {
		// TODO need to generate a mapping between the feature propositional functions
		// TODO feel free to change the declaration or add different version of reward mappings
		return null;
	}
}

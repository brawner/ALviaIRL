import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import burlap.oomdp.auxiliary.StateGenerator;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;


public class DrivingWorldStateGenerator implements StateGenerator {
	private Domain domain;
	private int[] blockXLocations;
	private int blockCount;
	private int height;
	public DrivingWorldStateGenerator(Domain domain, int[] blockXLocations, int height, int blockCount) {
		this.domain = domain;
		this.blockXLocations = blockXLocations;
		this.blockCount = blockCount;
		this.height = height;
	}

	@Override
	public State generateState() {

		State s = new State();
		ObjectInstance agent = new ObjectInstance(this.domain.getObjectClass(DrivingGridWorld.agentClass), DrivingGridWorld.agentClass+0);
		agent.setValue(DrivingGridWorld.ATTX, this.blockXLocations[0]);
		agent.setValue(DrivingGridWorld.ATTY, 0);
		s.addObject(agent);
		Set<double[]> blockLocations = new HashSet<double[]>();
		Random random = new Random();
		
		while (blockLocations.size() < blockCount) {
			int x = this.blockXLocations[random.nextInt(this.blockXLocations.length)];
			int y = random.nextInt(this.height);;
			blockLocations.add(new double[]{x,y});
		}
		
		for (double[] point : blockLocations) {
			ObjectInstance block = new ObjectInstance(this.domain.getObjectClass(DrivingGridWorld.blockClass), DrivingGridWorld.blockClass + point[0] + "_" + point[1]);
			block.setValue(DrivingGridWorld.ATTX, point[0]);
			block.setValue(DrivingGridWorld.ATTY, point[1]);
			s.addObject(block);
		}
		return s;
	}

}

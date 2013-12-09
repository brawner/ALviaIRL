
import java.util.List;

import burlap.oomdp.auxiliary.StateParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;


public class DrivingWorldStateParser implements StateParser {

	protected Domain				domain;
	
	
	public DrivingWorldStateParser(int width, int height, int numLanes, int laneWidth){
		DrivingGridWorld generator = new DrivingGridWorld(width, height, numLanes, laneWidth);
		this.domain = generator.generateDomain();
	}
	
	public DrivingWorldStateParser(Domain domain){
		this.domain = domain;
	}
	
	@Override
	public String stateToString(State s) {
		
		StringBuffer sbuf = new StringBuffer(256);
		
		ObjectInstance a = s.getObjectsOfTrueClass(DrivingGridWorld.agentClass).get(0);
		List<ObjectInstance> locs = s.getObjectsOfTrueClass(DrivingGridWorld.blockClass);
		
		String xa = DrivingGridWorld.ATTX;
		String ya = DrivingGridWorld.ATTY;
		
		sbuf.append(a.getDiscValForAttribute(xa)).append(" ").append(a.getDiscValForAttribute(ya));
		for(ObjectInstance l : locs){
			sbuf.append(", ").append(l.getDiscValForAttribute(xa)).append(" ").append(l.getDiscValForAttribute(ya));
		}
		
		
		return sbuf.toString();
	}

	@Override
	public State stringToState(String str) {
		
		String [] obcomps = str.split(", ");
		
		String [] acomps = obcomps[0].split(" ");
		int ax = Integer.parseInt(acomps[0]);
		int ay = Integer.parseInt(acomps[1]);
		
		int nl = obcomps.length - 1;
		
		State s = DrivingGridWorld.getOneAgentNLocationState(domain, nl);
		DrivingGridWorld.setAgent(s, ax, ay);
		
		for(int i = 1; i < obcomps.length; i++){
			String [] lcomps = obcomps[i].split(" ");
			int lx = Integer.parseInt(lcomps[0]);
			int ly = Integer.parseInt(lcomps[1]);
			
			DrivingGridWorld.setBlockLocation(s, i-1, lx, ly);
			
		}
		
		
		return s;
	}

}

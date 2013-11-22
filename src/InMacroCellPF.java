import java.util.List;

import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;

public class InMacroCellPF extends PropositionalFunction{
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
		List<ObjectInstance> agents = state.getObjectsOfTrueClass(IRLGridWorld.CLASSAGENT);
		if (agents.size() == 0) {
			return false;
		}
		ObjectInstance agent = agents.get(0);
		int agentX = agent.getDiscValForAttribute(IRLGridWorld.ATTX);
		int agentY = agent.getDiscValForAttribute(IRLGridWorld.ATTY);
		return (left <= agentX && agentX <= right &&
				bottom <= agentY && agentY <= top);
			
	}
}
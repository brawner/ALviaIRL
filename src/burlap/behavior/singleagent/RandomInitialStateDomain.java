package burlap.behavior.singleagent;

import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;

public interface RandomInitialStateDomain {

	State getRandomInitialState(Domain d);
}

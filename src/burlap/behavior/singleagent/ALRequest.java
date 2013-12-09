package burlap.behavior.singleagent;

import java.util.List;

import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.PropositionalFunction;

public class ALRequest {
	private Domain domain;
	private OOMDPPlanner planner;
	private PropositionalFunction[] featureFunctions;
	private List<EpisodeAnalysis> expertEpisodes;
	private double gamma;
	private double epsilon;
	private int maxIterations;
	private int policyCount;
	private double[] tHistory;

	public ALRequest(Domain d, OOMDPPlanner planner, PropositionalFunction[] featureFunctions) {
		// TODO Auto-generated constructor stub
	}
	
	public void setDomain(Domain d) {
		this.domain = d;
	}
	
	public Domain getDomain() {return this.domain;}
	
	public void setPlanner(OOMDPPlanner p) {
		this.planner = p;
	}
	
	public OOMDPPLanner getPlanner() {return this.planner;}
	
	public void setFeatureFunctions(PropositionalFunction[] functions) {
		this.featureFunctions= functions.clone();
	}
	
	public PropositionalFunction[] getFeatureFunctions() {return this.featureFunctions.clone();}	

}

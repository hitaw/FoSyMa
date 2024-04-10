package eu.su.mas.dedaleEtu.mas.behaviours.custom;

import java.util.*;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.gsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;

import eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;

import jade.core.behaviours.OneShotBehaviour;
import org.graphstream.graph.Edge;

import static eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM.MaxStuck;

public class WalkStateBeha extends OneShotBehaviour {	

	private static final long serialVersionUID = 8567689731496787661L;

	/**
	 * Current knowledge of the agent regarding the environment
	 */
	private MapRepresentation myMap;

	private List<String> list_agentNames;
	private ExploreCoopAgentFSM myAgent;

	public static final int TimeoutRemovedEdge = 10;

/**
 * 
 * @param myagent reference to the agent we are adding this behavior to
 * @param agentNames name of the agents to share the map with
 */
	public WalkStateBeha(final AbstractDedaleAgent myagent, List<String> agentNames) {
		super(myagent);
		myAgent = (ExploreCoopAgentFSM) myagent;
		this.list_agentNames=agentNames;

	}

	@Override
	public void action() {
		this.myMap=myAgent.getMyMap();

		System.out.println(this.myAgent.getLocalName()+" - WalkStateBeha");

		if(this.myMap==null) {
			this.myMap = new MapRepresentation();
		}

		myAgent.ageRemovedEdges(); // On vieillit les arêtes retirées
		Edge ed = this.myAgent.getEdge(); // Un effectue cette action une fois par itération donc, et on ne peut enlever qu'une arête par itération donc une seule peut avoir dépassé le temps limite
		if (ed != null) {
			System.out.println("Agent "+this.myAgent.getLocalName()+" -- adding edge "+ed.getId());
			this.myMap.addEdge(ed.getSourceNode().getId(), ed.getTargetNode().getId(), ed.getId(), this.myAgent.getLocalName());
		}

		//0) Retrieve the current position
		Location myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();

		if (myPosition!=null){
			//List of observable from the agent's current position
			List<Couple<Location,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
//			System.out.println(this.myAgent.getLocalName()+" - Observations: "+lobs);
			/**
			 * Just added here to let you see what the agent is doing, otherwise he will be too quick
			 */
			try {
				this.myAgent.doWait(100);
			} catch (Exception e) {
				e.printStackTrace();
			}

			//1) remove the current node from openlist and add it to closedNodes.
			Couple<Location, List<Couple<Observation, Integer>>> obs = lobs.get(0);
			this.myMap.addNode(myPosition.getLocationId(), MapAttribute.closed, !obs.getRight().isEmpty() && (obs.getRight().get(0).getLeft().toString().equals("Stench")));

			//2) get the surrounding nodes and, if not in closedNodes, add them to open nodes.
			String nextNodeId=null;
			Iterator<Couple<Location, List<Couple<Observation, Integer>>>> iter=lobs.iterator();
			while(iter.hasNext()){
				obs = iter.next();
				Location accessibleNode=obs.getLeft();
				boolean stench = !obs.getRight().isEmpty() && (obs.getRight().get(0).getLeft().toString().equals("Stench"));
				boolean isNewNode = this.myMap.addNewNode(accessibleNode.getLocationId(), stench); // this also updates the stench date on already existing nodes
				//the node may exist, but not necessarily the edge
				if (myPosition.getLocationId() != accessibleNode.getLocationId()) {
					if (myAgent.isRemovedEdge(myPosition.getLocationId(), accessibleNode.getLocationId())) {
						continue;
					}
					this.myMap.addEdge(myPosition.getLocationId(), accessibleNode.getLocationId());
					if (nextNodeId==null && isNewNode) nextNodeId=accessibleNode.getLocationId();
				}
			}
			//3) while openNodes is not empty, continues.
			if (!this.myMap.hasOpenNode()){
				//Explo finished
				System.out.println(this.myAgent.getLocalName()+" - Exploration successufully done, behaviour removed.");
				myAgent.addAllEdges();
				myAgent.clearRemovedEdges();
				myAgent.setHunting(true);
			} else {
				if (myAgent.getStuck() > MaxStuck) {
					myAgent.setStuck(0);
					Edge e = myMap.removeEdge(myPosition.getLocationId(), this.myMap.getShortestPathToClosestOpenNode(myPosition.getLocationId()).get(0));
					myAgent.addRemovedEdge(e);
					myMap.clearPlannedItinerary(); //The plan that was calculated is no longer valid, we have to calculate a new one
				}
				//4) select next move.

				// 4.0 If the movement plan has already been calculated, and nothing has changed (new map info or stuck made a change), go for it
				if (myMap.getNextNodePlan() != null) {
					nextNodeId = myMap.getNextNodePlan();
				}

				//4.1 If there exist one open node directly reachable, go for it,
				//	 otherwise choose one from the openNode list, compute the shortestPath and go for it
				if (nextNodeId==null){
					//no directly accessible openNode
					//chose one, compute the path and take the first step.
					List<String> path = this.myMap.getShortestPathToClosestOpenNode(myPosition.getLocationId());
					if (path != null) {
						nextNodeId=path.get(0);//getShortestPath(myPosition,this.openNodes.get(0)).get(0);
						myMap.setPlannedItinerary(path);
					} else {
						nextNodeId = myPosition.getLocationId();
					}
					//System.out.println(this.myAgent.getLocalName()+"-- list= "+this.myMap.getOpenNodes()+"| nextNode: "+nextNode);
				} else {
					//System.out.println("nextNode notNUll - "+this.myAgent.getLocalName()+"-- list= "+this.myMap.getOpenNodes()+"\n -- nextNode: "+nextNode);
				}
				if (((AbstractDedaleAgent)this.myAgent).moveTo(new gsLocation(nextNodeId))) {
					myMap.advancePlan();
					myAgent.setStuck(0);
				} else {
					myAgent.setStuck(myAgent.getStuck() + 1);
				}
			}

		}
		myAgent.setMyMap(myMap);
	}
	@Override
	public int onEnd() {
		if (myMap.hasOpenNode()) {
			return 1;
		}
		return 0;
	}
}

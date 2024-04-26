package eu.su.mas.dedaleEtu.mas.behaviours.custom;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.gsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import org.graphstream.graph.Edge;

import java.util.*;

import static eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM.MaxStuck;
import static eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM.WaitTime;


public class CaptainStrategyStateBeha extends OneShotBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;
	private MapRepresentation myMap;
	private ExploreCoopAgentFSM myAgent;
	private List<String> team;
	Map<String, Integer> possibleNodes;
	String objectifGolem = null;
	String posGolem;
	int iteration;
	boolean teamReady = false;
	boolean freeze = false;


	/**
 *
 * @param myagent reference to the agent we are adding this behavior to
 */
	public CaptainStrategyStateBeha(final AbstractDedaleAgent myagent) {
		super(myagent);
		myAgent = (ExploreCoopAgentFSM) myagent;
		team = myAgent.getTeam();
		iteration = myAgent.getIteration();
	}

	private void calculatePlan() {
		List<String> line;
		List<String> nextLine;
		myMap = myAgent.getMyMap();

		// The golem is considered to be at the stinkiest node if we don't have a position
		posGolem = myAgent.getGolemPos() == null ? myMap.getStinkiestNode() : myAgent.getGolemPos();
		if (posGolem == null) { // no stench detected, select a random node
			posGolem = myMap.getRandomNode(); // TODO just démerdez vous vers là bas
		}
		// calcul des noeuds proches sur lesquels on a assez d'agents pour bloquer
		myAgent.addAllEdges(); // We need to add all edges to make sure the arity of nodes is not broken
		myAgent.clearRemovedEdges();
		possibleNodes = myMap.getCloseNodesMaxArity(team.size(), ExploreCoopAgentFSM.MaxDistanceGolem, posGolem);

		// Décide de la stratégie
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setSender(this.myAgent.getAID());
		msg.setProtocol("PLAN");

		if (!possibleNodes.isEmpty()) {
			// Hypothèse : le golem est sur le noeud posGolem
			// On choisit le noeud qui nous intéresse pour bloquer le golem et on fait un ligne derrière lui
			objectifGolem = choiceNode();
			line = myMap.neighborLine(posGolem, objectifGolem);
			List<String> golemPath = myMap.getShortestPath(posGolem, objectifGolem); // Hope is sweet
			String nextGolemStep = golemPath.isEmpty() ? posGolem : golemPath.get(0);
			nextLine = myMap.neighborLine(nextGolemStep, objectifGolem);
			myAgent.setLine(line);
			myAgent.setNextLine(nextLine);

			List<String> lineShort = myAgent.getLine();
			if (!lineShort.isEmpty()) lineShort.remove(0);
			List<String> nextLineShort = myAgent.getNextLine();
			if (!nextLineShort.isEmpty())nextLineShort.remove(0);
			msg.setContent(this.iteration + ":"+ lineShort +";" + ++this.iteration + ":" + nextLineShort + ";" + objectifGolem);
		}
		else { // There is no interesting nodes for our team, we need more agents
			msg.setContent("null");
		}
		for (String agentName : team) {
			if (agentName == this.myAgent.getLocalName()) continue;
			msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
		}
		((AbstractDedaleAgent)this.myAgent).sendMessage(msg);

		System.out.println("Chef " + this.myAgent.getLocalName()+ "-- possible nodes -- " + possibleNodes + "-- chosen node : " + objectifGolem);
		System.out.println("Chef " + this.myAgent.getLocalName()+ "--- line : " + msg.getContent());
	}

	private static List<String> parseList(String list){
		list = list.replaceAll("\\[", "").replaceAll("\\]","").replaceAll(" ", "");
		System.out.println(list.length());
		if (list.length() == 0)
			return null;
		return Arrays.asList(list.split(","));
	}

	private void updatePlan() {
		// calculate the next line for the team
		myAgent.addAllEdges();
		myAgent.clearRemovedEdges();

		List<String> golemPath = myMap.getShortestPath(posGolem, objectifGolem); // Hope is sweet
		posGolem = golemPath.isEmpty() ? posGolem : golemPath.get(0);
		if (!golemPath.isEmpty()) golemPath.remove(0);
		myAgent.setNextLine(myMap.neighborLine(posGolem, objectifGolem));

		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setSender(this.myAgent.getAID());
		msg.setProtocol("PLAN");
		List<String> lineShort = myAgent.getLine();
		if (!lineShort.isEmpty()) lineShort.remove(0);
		List<String> nextLineShort = myAgent.getNextLine();
		if (!nextLineShort.isEmpty())nextLineShort.remove(0);
		msg.setContent(this.iteration + ":"+ lineShort +";" + ++this.iteration + ":" + nextLineShort + ";" + objectifGolem);
		for (String agentName : team) {
			if (agentName == this.myAgent.getLocalName()) continue;
			msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
		}
		((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
	}

	private String choiceNode() {
		// chose the node with the smallest heuristic in possible_nodes
		String node = null;
		int min = Integer.MAX_VALUE;
		for (Map.Entry<String, Integer> entry : possibleNodes.entrySet()) {
			if (entry.getValue() < min) {
				min = entry.getValue();
				node = entry.getKey();
			}
		}
		return node;
	}

	private void calculateItinerary(List<String> line, Location myPosition) {
        List<String> path = myMap.getShortestPath(myPosition.getLocationId(), line.get(0));

//		int length = Integer.MAX_VALUE;
//		for (String s : line) {
//			List<String> p = myMap.getShortestPath(myPosition.getLocationId(), s);
//			if (p != null && p.size() < length) {
//                length = p.size();
//				path = p;
//			}
//		}
		myMap.setPlannedItinerary(path);
	}

	@Override
	public void action() {

		try {
			this.myAgent.doWait(WaitTime);
		} catch (Exception e) {
			e.printStackTrace();
		}

		boolean blocking = false;
		teamReady = false;
		this.myMap = myAgent.getMyMap();
		this.iteration = myAgent.getIteration();

		System.out.println(this.myAgent.getLocalName()+" --- CaptainStrategyState");

		if (this.myMap == null) {
			this.myMap = new MapRepresentation();
		}
		myAgent.ageRemovedEdges(); // On vieillit les arêtes retirées
		Edge ed = this.myAgent.getEdge(); // Un effectue cette action une fois par itération, et on ne peut enlever qu'une arête par itération donc une seule peut avoir dépassé le temps limite
		if (ed != null) {
			System.out.println("Agent "+this.myAgent.getLocalName()+" -- adding edge "+ed.getId());
			this.myMap.addEdge(ed.getSourceNode().getId(), ed.getTargetNode().getId(), ed.getId(), this.myAgent.getLocalName());
		}

		//0) Retrieve the current position
		Location myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();

		if (myPosition!=null){

			if (myAgent.getStuck() > MaxStuck) {
				myAgent.setStuck(0);
				Edge e = myMap.removeEdge(myPosition.getLocationId(), this.myMap.getNextNodePlan());
				myAgent.addRemovedEdge(e);
				myMap.clearPlannedItinerary(); //The plan that was calculated is no longer valid, we have to calculate a new one
			}

			//List of observable from the agent's current position
			List<Couple<Location,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
//			System.out.println(this.myAgent.getLocalName()+" - Observations: "+lobs);

			Couple<Location, List<Couple<Observation, Integer>>> obs = lobs.get(0);
			boolean stinks = !obs.getRight().isEmpty() && (obs.getRight().get(0).getLeft().toString().equals("Stench"));

			//2) get the surrounding nodes and update stench
			String nextNodeId=null;
			Iterator<Couple<Location, List<Couple<Observation, Integer>>>> iter=lobs.iterator();
			while(iter.hasNext()){
				obs = iter.next();
				Location accessibleNode=obs.getLeft();
				boolean stench = !obs.getRight().isEmpty() && (obs.getRight().get(0).getLeft().toString().equals("Stench"));
				this.myMap.addNewNode(accessibleNode.getLocationId(), stench); // this also updates the stench date on already existing nodes
			}

			List<String> line = myAgent.getLine();
			// The captain calculate the lines, the others expect a message from the team captain
			if (line == null) {
				calculatePlan();
				line = myAgent.getLine();
			}

			if (line == null) { // First iteration or after fail
				System.out.println(this.myAgent.getLocalName() + " --- No good destination, gather more people ");
				// Go to the captain's last known destination (a way to stay together)
				nextNodeId = myMap.getNextNodePlan() != null ? myMap.getNextNodePlan() : myPosition.getLocationId();
			}
			else {
				calculateItinerary(line, myPosition);

				if (myAgent.getNextLine() == null) {
					updatePlan();
					calculateItinerary(myAgent.getLine(), myPosition);
				}

				nextNodeId = myMap.getNextNodePlan() != null ? myMap.getNextNodePlan() : myPosition.getLocationId();

				// If we are on the line and line == nextLine, we are blocking. Let's check that the golem is indeed where we think by going there
//				if ( myAgent.getLine().equals(myAgent.getNextLine()) && line.contains(myPosition.getLocationId())) {
				if ( myAgent.getLine().equals(myAgent.getNextLine()) && myPosition.getLocationId().equals(line.get(0))) {
					nextNodeId = objectifGolem;
					blocking = true;
				}
			}
			if (this.myAgent.moveTo(new gsLocation(nextNodeId))) {
				myMap.advancePlan();
				myAgent.setStuck(0);
				if (myMap.getNextNodePlan() == null) { // We are at our next destination
					if (myAgent.isReady()) {
						myAgent.setLine(myAgent.getNextLine());
						myAgent.setNextLine(null);
						myAgent.increaseIteration();
					}
					if (!stinks) {
						myAgent.removeGolemPos();
						myMap.addNode(objectifGolem, MapAttribute.closed, false);
						myAgent.setLine(null);
						myAgent.setNextLine(null);
					}
					if (blocking) {
						System.out.println("Agent " + this.myAgent.getLocalName() + " --- We are not blocking the golem");
						myAgent.removeGolemPos();
						myMap.addNode(objectifGolem, MapAttribute.closed, false);
						myAgent.setLine(null);
						myAgent.setNextLine(null);
					}
				}
			} else {
				boolean isGolem = myAgent.diagnostic(nextNodeId, true);
				if (blocking && isGolem) {
					System.out.println("Agent " + this.myAgent.getLocalName() + " --- We are blocking the golem");
					if (myAgent.isReady()) {
						// tell the useful agents to freeze
						ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
						msg.setSender(this.myAgent.getAID());
						msg.setProtocol("FREEZE");
						msg.setContent(objectifGolem);
						for (int i = 0; i < line.size(); i++) { // line forcément plus petit que le nb d'agents de la team par définition du blocage
							msg.addReceiver(new AID(team.get(i), AID.ISLOCALNAME));
							System.out.println("Send freeze to " + team.get(i));
						}
						((AbstractDedaleAgent)this.myAgent).sendMessage(msg);

						// warn useless agents that they are free to go hunt other golems
						ACLMessage msgFree = new ACLMessage(ACLMessage.INFORM);
						msgFree.setSender(this.myAgent.getAID());
						msgFree.setProtocol("FREE");
						for (int i = line.size(); i < team.size(); i ++) {
							msgFree.addReceiver(new AID(team.get(i), AID.ISLOCALNAME));
						}
						((AbstractDedaleAgent)this.myAgent).sendMessage(msgFree);
						freeze = true;
					}
				} else if (isGolem) {
					myAgent.setLine(null);
					myAgent.setNextLine(null);
				}
				myAgent.setStuck(myAgent.getStuck() + 1); // TODO if stuck !=0 on considère qu'on est face au golem ?
			}
		}
		myAgent.setMyMap(myMap);
	}
	@Override
	public int onEnd() {
		if (freeze) return 1;
		return 0;
	}
}

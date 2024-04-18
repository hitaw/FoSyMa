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


public class CaptainStrategyState extends OneShotBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;

	private MapRepresentation myMap;
	private ExploreCoopAgentFSM myAgent;
	private List<String> team;
	private int teamMember = 0;
	Map<String, Integer> possibleNodes;
	String objectifGolem = null;
	String posGolem;
	int iteration = 0;


	/**
 *
 * @param myagent reference to the agent we are adding this behavior to
 */
	public CaptainStrategyState(final AbstractDedaleAgent myagent) {
		super(myagent);
		myAgent = (ExploreCoopAgentFSM) myagent;
		team = myAgent.getTeam();
	}

	private void calculatePlan() {
		List<String> line;
		List<String> nextLine;
		myMap = myAgent.getMyMap();

		// Find out if the agent is the chief and if not, their rank in the team
		teamMember = 0;

		// The golem is considered to be at the stinkiest node if we don't have a position
		posGolem = myAgent.getGolemPos() == null ? myMap.getStinkiestNode() : myAgent.getGolemPos();
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
			msg.setContent(this.iteration + ":"+ line.toString() +";" + ++this.iteration + ":" + nextLine.toString()+ ";" + objectifGolem);
			myAgent.setLine(line);
			myAgent.setNextLine(nextLine);
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

	private List<String> parseList(String list){
		list = list.replaceAll("\\[", "").replaceAll("\\]","").replaceAll(" ", "");
		return Arrays.asList(list.split(","));
	}

	private void updatePlan() {
		// calculate the next line for the team
		myAgent.addAllEdges();
		myAgent.clearRemovedEdges();
		List<String> golemPath = myMap.getShortestPath(posGolem, objectifGolem); // Hope is sweet
		posGolem = golemPath.isEmpty() ? posGolem : golemPath.get(0);
		if (!golemPath.isEmpty()) golemPath.remove(0);
		String nextPosGolem = golemPath.isEmpty() ? posGolem : golemPath.get(0);
		myAgent.setNextLine(myMap.neighborLine(nextPosGolem, objectifGolem));

		// send the new plan to the team
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setSender(this.myAgent.getAID());
		msg.setProtocol("PLAN");
		msg.setContent(this.iteration + ":"+ myAgent.getLine().toString() +";" + ++this.iteration + ":" + myAgent.getNextLine().toString()+ ";" + objectifGolem);
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
		String nextDestination = line.size() > teamMember ? line.get(teamMember) : null;
		if (nextDestination != null) {
			List<String> path = myMap.getShortestPath(myPosition.getLocationId(), nextDestination);
			if ((path != null) && (!path.isEmpty())) {
				myMap.setPlannedItinerary(path);
				System.out.println("Agent " + this.myAgent.getLocalName() + "--- path : " + path);
			}
		}
	}

	@Override
	public void action() {
		this.myMap = myAgent.getMyMap();

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
			/**
			 * Just added here to let you see what the agent is doing, otherwise he will be too quick
			 */
			try {
				this.myAgent.doWait(WaitTime);
			} catch (Exception e) {
				e.printStackTrace();
			}

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

			Date expiration = new Date(new Date().getTime() + WaitTime);
			List<String> line = myAgent.getLine();
			// The captain calculate the lines, the others expect a message from the team captain
			while ((line == null) && (new Date().before(expiration))) {
				calculatePlan();
				line = myAgent.getLine();
			}

			if (line == null) {
				System.out.println(this.myAgent.getLocalName() + " --- Didn't get a destination ");
				// Go to the captain's last known destination (a way to stay together)

			}
			else { // TODO need optimization, no need to calculate for each iteration
				calculateItinerary(line, myPosition);
				expiration = new Date(new Date().getTime() + WaitTime);

				while ((myAgent.getNextLine() == null) && (new Date().before(expiration))){ // We are at the line node
					updatePlan();
					calculateItinerary(myAgent.getLine(), myPosition);
				}
			}

			nextNodeId = myMap.getNextNodePlan() != null ? myMap.getNextNodePlan() : myPosition.getLocationId();
			// If we are not to move and line == nextLine, we are blocking. Let's check that the golem is indeed where we think by going there
			boolean blocking = false;
			if (line.equals(myAgent.getNextLine()) && myPosition.getLocationId().equals(nextNodeId)) {
				nextNodeId = objectifGolem;
				blocking = true;
			}
			if (this.myAgent.moveTo(new gsLocation(nextNodeId))) { // Si ça marche pas on a rencontré le golem ?
				myMap.advancePlan();
				myAgent.setStuck(0);
				if (myMap.getNextNodePlan() == null) { // We are at our next destination
					myAgent.setLine(myAgent.getNextLine());
					myAgent.setNextLine(null);
					if (blocking == true) {
						myAgent.removeGolemPos();
						myMap.addNode(objectifGolem, MapAttribute.closed, false);
						myAgent.setLine(null);
						myAgent.setNextLine(null);
					}
				}
			} else {
				myAgent.setStuck(myAgent.getStuck() + 1); // TODO if stuck !=0 on considère qu'on est face au golem ?
				boolean golem = myAgent.diagnostic();
			}

		}
		myAgent.setMyMap(myMap);
	}
	@Override
	public int onEnd() {
		return 0;
	}
}

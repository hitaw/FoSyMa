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
import jade.lang.acl.MessageTemplate;
import org.graphstream.graph.Edge;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM.MaxStuck;


public class TeamStrategyState extends OneShotBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;

	private MapRepresentation myMap;
	private ExploreCoopAgentFSM myAgent;
	private List<String> team;
	private boolean chef;
	private int teamMember = 0;
	Map<String, Integer> possibleNodes;
	List<List<String>> plan;
	String objectifGolem = null;
	String posGolem;
	int iteration = 0;
	List<String> line;
	List<String> nextLine;


	/**
 *
 * @param myagent reference to the agent we are adding this behavior to
 */
	public TeamStrategyState(final AbstractDedaleAgent myagent) {
		super(myagent);
		myAgent = (ExploreCoopAgentFSM) myagent;
		team = myAgent.getTeam();
		chef = true;
		for (String agent : team ) {
			if (myAgent.getLocalName().compareToIgnoreCase(agent) > 0) {
				teamMember++;
				chef = false;
				}
		}
	}

	private void calculatePlan() {
		myMap = myAgent.getMyMap();
		teamMember = 0;
		chef = true;
		for (String agent : team ) {
			if (myAgent.getLocalName().compareToIgnoreCase(agent) > 0) {
				chef = false;
				teamMember++;
			}
		}

		if (chef){
			posGolem = myMap.getLastNodePlan() != null ? myMap.getLastNodePlan() : myAgent.getCurrentPosition().getLocationId();
			possibleNodes = myMap.getCloseNodesMaxArity(team.size(), ExploreCoopAgentFSM.MaxDistanceGolem, posGolem);
			// Décide de la stratégie
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setSender(this.myAgent.getAID());
			msg.setProtocol("PLAN");
			if (!possibleNodes.isEmpty()) {

				// Hypothèse : le golem est sur le noeud posGolem
				// On choisit le noeud qui nous intéresse pour bloquer le golem et on fait un ligne derrière lui
				objectifGolem = choiceNode(); //TODO
				line = myMap.neighborLine(posGolem, objectifGolem);
				List<String> golemPath = myMap.getShortestPath(posGolem, objectifGolem); // Hope is sweet
				String nextGolemStep = golemPath.isEmpty() ? posGolem : golemPath.get(0);
				nextLine = myMap.neighborLine(nextGolemStep, objectifGolem);
				msg.setContent(this.iteration + ":"+ line.toString() +";" +this.iteration++ + ":" + nextLine.toString()+ ";" + objectifGolem);
//				iteration++;
			}
			else { // There is no interesting nodes for our team, we need more agents
				msg.setContent("null");
			}
			for (String agentName : team) {
				msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
			}
			((AbstractDedaleAgent)this.myAgent).sendMessage(msg);

			System.out.println("Chef " + this.myAgent.getLocalName()+ "-- possible nodes -- " + possibleNodes + "-- chosen node : " + objectifGolem);
			System.out.println("Chef " + this.myAgent.getLocalName()+ "--- line : " + msg.getContent());
		}
		else {
			MessageTemplate msgTemplate = MessageTemplate.and(
					MessageTemplate.MatchProtocol("PLAN"),
					MessageTemplate.MatchPerformative(ACLMessage.INFORM));

			ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
			while (msgReceived != null) { // TODO wait until we have a plan
				String content = msgReceived.getContent();
				System.out.println(this.myAgent.getLocalName() + "-- received plan from " + msgReceived.getSender().getLocalName());
				if (content.equals("null")) {
					// We need more agents
					// TODO Gathering more agents
				}
				else{
					String[] info = content.split(";");
					int it = Integer.parseInt(info[0].split(":")[0]);
					if (it >= this.iteration) { // We might receive messages that are outdated, we check with the iteration number
						this.iteration = it;
						line = parseListInt(info[0].split(":")[1]);
						nextLine = parseListInt(info[1].split(":")[1]);
						objectifGolem = info[2];
						System.out.println(this.myAgent.getLocalName() + "-- received plan -- " + line + " -- " + nextLine);
					}
				}
				msgReceived = this.myAgent.receive(msgTemplate);
				}
			}
	}

	private List<String> parseListInt(String list){
		List<String> res = new ArrayList<String>();
		Pattern p = Pattern.compile("\\d+");
		Matcher m = p.matcher(list);
		while (m.find()) {
			res.add(m.group());
		}
		return res;
	}

	private void updatePlan() {

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

	@Override
	public void action() {
		this.myMap = myAgent.getMyMap();

		System.out.println(this.myAgent.getLocalName()+" - TeamStrategyState");

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
			//List of observable from the agent's current position
			List<Couple<Location,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
//			System.out.println(this.myAgent.getLocalName()+" - Observations: "+lobs);
			/**
			 * Just added here to let you see what the agent is doing, otherwise he will be too quick
			 */
			try {
				this.myAgent.doWait(500);
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

			while (line == null) {
				calculatePlan();
			}
			if (myMap.getNextNodePlan() != null) {
				nextNodeId = myMap.getNextNodePlan();
			} else {
				String nextDestination = line.size() > teamMember ? line.get(teamMember) : null;
				if (nextDestination != null) {
					List<String> path = myMap.getShortestPath(myPosition.getLocationId(), nextDestination);
					if ((path != null) && (!path.isEmpty())) {
						nextNodeId = path.get(0);
						myMap.setPlannedItinerary(path);
					} else {
						nextNodeId = myPosition.getLocationId();
					}
				} else {
					nextNodeId = myPosition.getLocationId(); // En l'abscence d'information on ne bouge pas pour l'instant, après on essaiera de rester à portée du chef
				}
			}
			if (this.myAgent.moveTo(new gsLocation(nextNodeId))) { // Si ça marche pas on a rencontré le golem ?
				myMap.advancePlan();
				myAgent.setStuck(0);
				line = nextLine;
				nextLine = null;
			} else {
				myAgent.setStuck(myAgent.getStuck() + 1); // TODO if stuck !=0 on considère qu'on est face au golem ?
			}

		}
		myAgent.setMyMap(myMap);
	}
	@Override
	public int onEnd() {
		return 0;
	}
}

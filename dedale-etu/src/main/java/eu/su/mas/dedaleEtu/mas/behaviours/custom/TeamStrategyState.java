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

import java.util.*;

import static eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM.MaxStuck;
import static eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM.WaitTime;


public class TeamStrategyState extends OneShotBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;

	private MapRepresentation myMap;
	private ExploreCoopAgentFSM myAgent;
	private List<String> team;
	private int teamMember = 0;
	String objectifGolem = null;
	String posGolem;
	int iteration = 0;


	/**
 *
 * @param myagent reference to the agent we are adding this behavior to
 */
	public TeamStrategyState(final AbstractDedaleAgent myagent) {
		super(myagent);
		myAgent = (ExploreCoopAgentFSM) myagent;
		team = myAgent.getTeam();
		for (String agent : team ) {
			if (myAgent.getLocalName().compareToIgnoreCase(agent) > 0) {
				teamMember++;
				}
		}
	}

	private void calculatePlan() {
		List<String> line;
		List<String> nextLine;
		myMap = myAgent.getMyMap();

		// Find out if the agent is the chief and if not, their rank in the team
		teamMember = 0;
		for (String agent : team ) {
			if (myAgent.getLocalName().compareToIgnoreCase(agent) > 0) {
				teamMember++;
			}
		}

        MessageTemplate msgTemplate = MessageTemplate.and(
                MessageTemplate.MatchProtocol("PLAN"),
                MessageTemplate.MatchPerformative(ACLMessage.INFORM));

        ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
        while (msgReceived != null) {
            String content = msgReceived.getContent();
//            System.out.println(this.myAgent.getLocalName() + "-- received plan from " + msgReceived.getSender().getLocalName());
            if (content.equals("null")) {
                // We need more agents
                // TODO Gathering more agents
                // Don't move ? Or follow Golem, idk
                System.out.println("Plan empty, need more agents");
            }
            else{
                String[] info = content.split(";");
                int it = Integer.parseInt(info[0].split(":")[0]);
                if (it >= this.iteration) { // We might receive messages that are outdated, we check with the iteration number
                    this.iteration = it;
                    line = parseList(info[0].split(":")[1]);
                    nextLine = parseList(info[1].split(":")[1]);
                    objectifGolem = info[2];
                    System.out.println(this.myAgent.getLocalName() + "-- received plan -- " + line + " -- " + nextLine);
                    myAgent.setLine(line);
                    myAgent.setNextLine(nextLine);
                }
            }
            msgReceived = this.myAgent.receive(msgTemplate);
            }

	}

	private List<String> parseList(String list){
		list = list.replaceAll("\\[", "").replaceAll("\\]","").replaceAll(" ", "");
		return Arrays.asList(list.split(","));
	}

	private void updatePlan() {
        // receive the instructions for the next step
        MessageTemplate msgTemplate = MessageTemplate.and(
                MessageTemplate.MatchProtocol("PLAN"),
                MessageTemplate.MatchPerformative(ACLMessage.INFORM));

        ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
        while (msgReceived != null) {
            String content = msgReceived.getContent();
//            System.out.println(this.myAgent.getLocalName() + "-- received plan from " + msgReceived.getSender().getLocalName());
            String[] info = content.split(";");
            int it = Integer.parseInt(info[0].split(":")[0]);
            if (it >= this.iteration) { // We might receive messages that are outdated, we check with the iteration number
                this.iteration = it;
                myAgent.setLine(parseList(info[0].split(":")[1]));
                myAgent.setNextLine(parseList(info[1].split(":")[1]));
				System.out.println(this.myAgent.getLocalName() + "-- received plan -- " + myAgent.getLine() + " -- " + myAgent.getNextLine());

			}
            msgReceived = this.myAgent.receive(msgTemplate);
		}

	}


	private void calculateItinerary(List<String> line, Location myPosition) {
		String nextDestination = line.size() > teamMember ? line.get(teamMember) : null;
		if (nextDestination != null) {
			List<String> path = myMap.getShortestPath(myPosition.getLocationId(), nextDestination);
			if ((path != null) && (!path.isEmpty())) {
				myMap.setPlannedItinerary(path);
				System.out.println("Agent " + this.myAgent.getLocalName() + "--- path to line: " + path + "it = " +iteration);
			}
		}
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
				String captain = myAgent.getChefName();
				if (this.myAgent.getLocalName().compareTo(captain) != 0) {
					String captainDestination = myAgent.getAgentDestination(captain);
					myMap.setPlannedItinerary(myMap.getShortestPath(myPosition.getLocationId(), captainDestination));
				}
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

			if (this.myAgent.moveTo(new gsLocation(nextNodeId))) { // Si ça marche pas on a rencontré le golem ?
				myMap.advancePlan();
				myAgent.setStuck(0);
				if (myMap.getNextNodePlan() == null) { // We are at our next destination
					myAgent.setLine(myAgent.getNextLine());
					myAgent.setNextLine(null);
				}
			} else {
				myAgent.setStuck(myAgent.getStuck() + 1); // TODO if stuck !=0 on considère qu'on est face au golem ?
				boolean golem = myAgent.diagnostic(nextNodeId);
			}

		}
		myAgent.setMyMap(myMap);
	}
	@Override
	public int onEnd() {
		return 0;
	}
}

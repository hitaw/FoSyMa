package eu.su.mas.dedaleEtu.mas.behaviours.custom;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.gsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.graphstream.graph.Edge;

import java.util.*;

import static eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM.MaxStuck;
import static eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM.WaitTime;


public class TeamStrategyStateBeha extends OneShotBehaviour {

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
	public TeamStrategyStateBeha(final AbstractDedaleAgent myagent) {
		super(myagent);
		myAgent = (ExploreCoopAgentFSM) myagent;
		team = myAgent.getTeam();
		teamMember = team.indexOf(myAgent.getLocalName());
	}

	private void calculatePlan() {
		List<String> line = null;
		List<String> nextLine = null;
		myMap = myAgent.getMyMap();
		teamMember = team.indexOf(myAgent.getLocalName());

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
				myAgent.setLine(new ArrayList<>());
            }
            else{
                String[] info = content.split(";");
                int it = Integer.parseInt(info[0].split(":")[0]);
                if (it >= this.iteration) { // We might receive messages that are outdated, we check with the iteration number
                    this.iteration = it;
					myAgent.setIteration(it);
					if (info[0].compareTo("") != 0) line = parseList(info[0].split(":")[1]);
                    if (info[1].compareTo("") != 0) nextLine = parseList(info[1].split(":")[1]);
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
            int it = Integer.parseInt(info[0].split(":")[0]); // TODO check the strings sent
            if (it >= this.iteration) { // We might receive messages that are outdated, we check with the iteration number
				myAgent.setGoToNext(true);
                this.iteration = it;
				myAgent.setIteration(it);
                myAgent.setLine(parseList(info[0].split(":")[1]));
                myAgent.setNextLine(parseList(info[1].split(":")[1]));
				System.out.println(this.myAgent.getLocalName() + "-- received plan -- " + myAgent.getLine() + " -- " + myAgent.getNextLine());

			}
            msgReceived = this.myAgent.receive(msgTemplate);
		}

	}


//	private void calculateItinerary(List<String> line, Location myPosition) {
//		nextDestination = line.size() > teamMember ? line.get(teamMember) : null;
//		if (myMap.getLastNodePlan()!=null && nextDestination != null && nextDestination.compareTo(myMap.getLastNodePlan()) == 0) {
//			return;
//		}
//		if (nextDestination != null) {
//			List<String> path = myMap.getShortestPath(myPosition.getLocationId(), nextDestination);
//			if ((path != null) && (!path.isEmpty())) {
//				myMap.setPlannedItinerary(path);
//				System.out.println("Agent " + this.myAgent.getLocalName() + "--- path to line: " + path + "it = " +iteration);
//			}
//		}
//	}

	private void calculateItinerary(List<String> line, Location myPosition) {
		List<String> path = myMap.getShortestPath(myPosition.getLocationId(), line.get(0));

		int length = Integer.MAX_VALUE;
		for (String s : line) {
			path = myMap.getShortestPath(myPosition.getLocationId(), s);
			if (path != null && path.size() < length) {
				length = path.size();
			}
		}
		myMap.setPlannedItinerary(path);
	}

	@Override
	public void action() {

		try {
			this.myAgent.doWait(WaitTime);
		} catch (Exception e) {
			e.printStackTrace();
		}

		this.myMap = myAgent.getMyMap();

		System.out.println(this.myAgent.getLocalName()+" - TeamStrategyState");
		posGolem = myAgent.getGolemPos();

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

			if (line == null || line.isEmpty()) {
				System.out.println(this.myAgent.getLocalName() + " --- Didn't get a destination ");
				// Go to the captain's last known destination (a way to stay together)
				if (posGolem != null) {
					myMap.setPlannedItinerary(myMap.getShortestPath(myPosition.getLocationId(), posGolem));
				} else {
					String captain = myAgent.getChefName();
					String captainDestination = myAgent.getAgentDestination(captain) == null ? myPosition.getLocationId() : myAgent.getAgentDestination(captain);
					myMap.setPlannedItinerary(myMap.getShortestPath(myPosition.getLocationId(), captainDestination)); //TODO if stuck et pas de chemin vers la destination
				}
			}
			else {
				calculateItinerary(line, myPosition);
				expiration = new Date(new Date().getTime() + WaitTime);

				while ((myAgent.getNextLine() == null) && (new Date().before(expiration))){ // We are at the line node
					// just slow the calculation
					try {
						this.myAgent.doWait(WaitTime/10);
					} catch (Exception e) {
						e.printStackTrace();
					}
					updatePlan();  // TODO need to check if no new plan sent
					calculateItinerary(myAgent.getLine(), myPosition);
				}
			}

			nextNodeId = myMap.getNextNodePlan() != null ? myMap.getNextNodePlan() : myPosition.getLocationId();

			if (this.myAgent.moveTo(new gsLocation(nextNodeId))) {
				myMap.advancePlan();
				myAgent.setStuck(0);
				updatePlan(); // TODO not clean here
				if ((myMap.getNextNodePlan() == null) && myAgent.getGoToNext()) { // We are at our next destination
					myAgent.setLine(myAgent.getNextLine());
					myAgent.setNextLine(null);
					myAgent.setGoToNext(false);
				}
			} else {
				updatePlan();
				myAgent.setStuck(myAgent.getStuck() + 1);
				myAgent.diagnostic(nextNodeId);
			}

		}
		myAgent.setMyMap(myMap);
	}
	@Override
	public int onEnd() {
		return 0;
	}
}

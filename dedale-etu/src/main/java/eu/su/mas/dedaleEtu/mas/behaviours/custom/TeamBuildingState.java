package eu.su.mas.dedaleEtu.mas.behaviours.custom;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Date;
import java.util.List;

import static eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM.MaxTeamDistance;
import static eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM.WaitTime;

public class TeamBuildingState extends OneShotBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;

	private ExploreCoopAgentFSM myAgent;
	private List<String> team;
	private MapRepresentation myMap;
	private boolean end = false;

	public TeamBuildingState(final AbstractDedaleAgent myagent) {
		super(myagent);
		this.myAgent = (ExploreCoopAgentFSM) myagent;
		team = myAgent.getTeam();
	}

	@Override
	public void action() {
		end = false;
		team = myAgent.getTeam();
		Date expiration = myAgent.getExpiration();

		if (expiration.before(new Date())) {
			System.out.println("Agent "+this.myAgent.getLocalName()+" -- " + team);
			System.out.println("Agent "+this.myAgent.getLocalName()+" -- team building expiration date reached");
			end = true;
		}

		myMap = myAgent.getMyMap();
		if(myMap ==null) {
			this.myAgent.setMyMap(new MapRepresentation());
			myMap = myAgent.getMyMap();
		}

		// Recevoir les positions des autres agents et leur proposer de rejoindre notre team si on chasse le même golem
		MessageTemplate msgTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol("POSITION"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));

		ACLMessage msgReceived = this.myAgent.receive(msgTemplate);

		ACLMessage offer = new ACLMessage(ACLMessage.PROPOSE);
		offer.setSender(this.myAgent.getAID());
		offer.setProtocol("JOIN");
		offer.setContent(team.toString());

		while (msgReceived != null) {
			String sender = msgReceived.getSender().getLocalName();
			String content = msgReceived.getContent();
			String[] info = content.split(";");
			String location = info[0];
			String destination = info[1];

			System.out.println("Agent "+this.myAgent.getLocalName()+" -- received inform from "+msgReceived.getSender().getLocalName() + " : " + location + " destination : " + destination);
			myAgent.updateAgentPosition(sender, location, destination);
			String myDestination = myMap.getLastNodePlan() != null ? myMap.getLastNodePlan() : myAgent.getCurrentPosition().getLocationId();
			List<String> shortestPath = myMap.getShortestPath(myDestination, destination);
			if ((shortestPath != null) && (shortestPath.size() < MaxTeamDistance)) {
				// offer to join team if we hunt the same golem, and we are not yet in the same team ofc
				if (!team.contains(sender)) {
					offer.addReceiver(new AID(sender, AID.ISLOCALNAME));
					System.out.println("Agent "+this.myAgent.getLocalName()+" -- send join to "+sender);
					// If we are joining a team, we go to the same destination to keep together
					// only change if the other agent is to be our chef, otherwise they will change their destination
					if (sender.compareToIgnoreCase(myAgent.getLocalName()) < 0) myMap.setPlannedItinerary(myMap.getShortestPath(myAgent.getCurrentPosition().getLocationId(), destination));
					//TODO not sure about this
				}
			}
			msgReceived = this.myAgent.receive(msgTemplate);
		}
		((AbstractDedaleAgent)this.myAgent).sendMessage(offer);
	}

	@Override
	public int onEnd() {
		if (end) {
			myAgent.setExpiration(new Date(new Date().getTime() + WaitTime));
			return 1;
		}
		return 0;
	}
}

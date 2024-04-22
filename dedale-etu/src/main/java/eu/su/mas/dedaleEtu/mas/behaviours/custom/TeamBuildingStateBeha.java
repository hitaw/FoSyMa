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

import static eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM.WaitTime;

public class TeamBuildingStateBeha extends OneShotBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;

	private ExploreCoopAgentFSM myAgent;
	private List<String> team;
	private MapRepresentation myMap;
	private boolean end = false;

	public TeamBuildingStateBeha(final AbstractDedaleAgent myagent) {
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

			if (team.contains(sender)) {
				// If we are in the same team, we need to update the team's position
				List<String> line = myAgent.getLine();
				int i = team.indexOf(sender);
				// Si un agent de notre équipe n'est pas à la même itération que nous, on lui renvoie le plan. Risque de harceler un agent avec le plan
				// Permet de relayer le plan à un agent qui n'a pas reçu le plan
				if (myAgent.getIteration()-1 > Integer.parseInt(info[2])) {
					myAgent.sendStrategy(sender);
				}
			}
			// offer to join team if we are not yet in the same team
			else{
				offer.addReceiver(new AID(sender, AID.ISLOCALNAME));
				System.out.println("Agent "+this.myAgent.getLocalName()+" -- send join to "+sender);
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

package eu.su.mas.dedaleEtu.mas.behaviours.custom;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.ACLMessage;
import eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM;


public class WaitAnswerStateBeha extends OneShotBehaviour {
	
	private static final long serialVersionUID = 8567689731496787661L;
	
	private List<String> listReceiver = new ArrayList<>();
	int nbCartesAttendues = 0;
	private ExploreCoopAgentFSM myAgent;
	Boolean end;


	public WaitAnswerStateBeha (final AbstractDedaleAgent myagent) {
		super(myagent);
		myAgent = (ExploreCoopAgentFSM) myagent;
	}

	@Override
	public void action() {
		end = false;
		listReceiver  = new ArrayList<String>();
		nbCartesAttendues = 0;
	//Reception de ping + envoi du yes
		Date expiration = myAgent.getExpiration();
	
		MessageTemplate pingTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol("PING"),
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

		MessageTemplate msgTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol("YES"),
				MessageTemplate.MatchPerformative(ACLMessage.AGREE));

		if (expiration.before(new Date())) {
//			System.out.println("Agent "+this.myAgent.getLocalName()+" -- expiration date reached");
			end = true;
		}

		//check if ping received

		ACLMessage pingReceived = this.myAgent.receive(pingTemplate);
		ACLMessage yes = new ACLMessage(ACLMessage.AGREE);
		yes.setProtocol("YES");
		yes.setSender(this.myAgent.getAID());
//		System.out.println("Agent " +this.myAgent.getLocalName() + "-- is looking for ping");
		while (pingReceived != null) {
			System.out.println("Agent "+this.myAgent.getLocalName()+" -- received ping from "+pingReceived.getSender().getLocalName());
			yes.addReceiver(pingReceived.getSender());
			listReceiver.add(pingReceived.getSender().getLocalName());
			nbCartesAttendues += 1;
			pingReceived = this.myAgent.receive(pingTemplate);
		}
		if (yes.getAllReceiver().hasNext()) {
			((AbstractDedaleAgent)this.myAgent).sendMessage(yes);
		}

		//check if YES received
//		System.out.println("Agent " +this.myAgent.getLocalName() + "-- is looking for yes");

		ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
		while (msgReceived != null) {
			System.out.println("Agent "+this.myAgent.getLocalName()+" -- received yes from "+msgReceived.getSender().getLocalName());
			if (!Objects.equals(msgReceived.getContent(), "Explo done")){
				listReceiver.add(msgReceived.getSender().getLocalName()); // We won't send a map to an agent done with exploration
			} else {
				System.out.println(this.myAgent.getLocalName() + " --- I will receive a full map");
			}
			nbCartesAttendues += 1;
			msgReceived = this.myAgent.receive(msgTemplate);
		}
		myAgent.setVoisins(listReceiver, nbCartesAttendues);
	}

	@Override
	public int onEnd() {
		if (nbCartesAttendues != 0) {
			return 1;
		}
		if (end) {
			return 2;
		}
		return 0;
	}


}

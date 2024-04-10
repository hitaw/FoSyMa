package eu.su.mas.dedaleEtu.mas.behaviours.custom;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;


public class WaitAnswerHuntStateBeha extends OneShotBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;

	private List<String> listReceiver;
	private ExploreCoopAgentFSM myAgent;
	Boolean end = false;


	public WaitAnswerHuntStateBeha(final AbstractDedaleAgent myagent) {
		super(myagent);
		myAgent = (ExploreCoopAgentFSM) myagent;
	}

	@Override
	public void action() {
		listReceiver  = new ArrayList<String>();
	//Reception de ping + envoi du yes
		Date expiration = myAgent.getExpiration();
	
		MessageTemplate pingTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol("PING"),
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

//		MessageTemplate msgTemplate = MessageTemplate.and(
//				MessageTemplate.MatchProtocol("YES"),
//				MessageTemplate.MatchPerformative(ACLMessage.AGREE));

		if (expiration.before(new Date())) {
			System.out.println("Agent "+this.myAgent.getLocalName()+" -- expiration date reached");
			end = true;
		}

		//check if ping received

		ACLMessage pingReceived = this.myAgent.receive(pingTemplate);
		ACLMessage yes = new ACLMessage(ACLMessage.AGREE);
		yes.setProtocol("YES");
		yes.setSender(this.myAgent.getAID());
//		System.out.println("Agent " +this.myAgent.getLocalName() + "-- is looking for ping");
		while (pingReceived != null) {
//			System.out.println("Agent "+this.myAgent.getLocalName()+" -- received ping from "+pingReceived.getSender().getLocalName());
			// TODO differencier les différents pings qu'on aura
			yes.addReceiver(pingReceived.getSender());
			yes.setContent("Explo done");
			System.out.println(this.myAgent.getLocalName()+ "-- in hunt received ping from "+pingReceived.getSender().getLocalName());
			listReceiver.add(pingReceived.getSender().getLocalName());
			pingReceived = this.myAgent.receive(pingTemplate);
		}
		if (yes.getAllReceiver().hasNext()) {
			((AbstractDedaleAgent)this.myAgent).sendMessage(yes);
		}

		//check if YES received
//		System.out.println("Agent " +this.myAgent.getLocalName() + "-- is looking for yes");

//		ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
//		while (msgReceived != null) {
////			System.out.println("Agent "+this.myAgent.getLocalName()+" -- received yes from "+msgReceived.getSender().getName());
//			if (!Objects.equals(msgReceived.getContent(), "Explo done")) listReceiver.add(msgReceived.getSender().getLocalName()); // We won't send a map to an agent done with exploration
//			msgReceived = this.myAgent.receive(msgTemplate);
//		}
		myAgent.setVoisins(listReceiver, 0);
	}

	@Override
	public int onEnd() {
		if (!listReceiver.isEmpty()) {
			return 1;
		}
		if (end) {
			return 2;
		}
		return 0;
	}


}

package eu.su.mas.dedaleEtu.mas.behaviours.custom;

import java.util.List;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.ACLMessage;


public class WaitAnswerStateBeha extends OneShotBehaviour {
	
	private List<String> listReceiver;

	@Override
	public void action() {
	//Reception de ping + envoi du yes
	
		MessageTemplate pingTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol("PING"),
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
		
		for(int i = 0; i < 5; i++) {
			
			//check if ping received
			
			ACLMessage pingReceived = this.myAgent.receive(pingTemplate);
			ACLMessage yes = new ACLMessage(ACLMessage.AGREE);
			yes.setProtocol("YES");
			yes.setSender(this.myAgent.getAID());
			
			while (pingReceived != null) {
				pingReceived = this.myAgent.receive(pingTemplate);
				
				yes.addReceiver(pingReceived.getSender());
				listReceiver.add(pingReceived.getSender().getName());
			}
			if (yes.getAllReceiver().hasNext()) {
				((AbstractDedaleAgent)this.myAgent).sendMessage(yes);				
			}
		}
	}
// TODO transmettre la liste des destinataires et onDone 
}

package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.List;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class PingBehaviour extends SimpleBehaviour{
	
	private static final long serialVersionUID = 8567689731496787661L;

	private boolean finished = false;

	private List<String> receivers;
	
	public PingBehaviour (final AbstractDedaleAgent myagent, List<String> agentNames) {
		super(myagent);
		this.receivers = agentNames;
	}

	@Override
	public void action() {
		
		//Envoi du ping
		
		ACLMessage ping = new ACLMessage(ACLMessage.REQUEST);
		ping.setSender(this.myAgent.getAID());
		ping.setProtocol("PING");
		
		for (String agentName : receivers) {
			ping.addReceiver(new AID(agentName, AID.ISLOCALNAME));
		}
		((AbstractDedaleAgent)this.myAgent).sendMessage(ping);
		
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
			}
			if (yes.getAllReceiver().hasNext()) {
				((AbstractDedaleAgent)this.myAgent).sendMessage(yes);				
			}
		}
	}

	@Override
	public boolean done() {
		return finished;
	}
}

package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

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
		
		for (String agentName : receivers) {
			ping.addReceiver(new AID(agentName, AID.ISLOCALNAME));
		}
		((AbstractDedaleAgent)this.myAgent).sendMessage(ping);
		
		//Definir une date d'autorisation de move	
		
	}

	@Override
	public boolean done() {
		return finished;
	}
}

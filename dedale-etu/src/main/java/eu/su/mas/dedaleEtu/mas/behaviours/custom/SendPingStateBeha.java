package eu.su.mas.dedaleEtu.mas.behaviours.custom;

import java.util.Date;
import java.util.List;

import eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM;
import jade.core.behaviours.OneShotBehaviour;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;

public class SendPingStateBeha extends OneShotBehaviour {
	
	private static final long serialVersionUID = 8567689731496787661L;

	private List<String> receivers;
	private ExploreCoopAgentFSM myAgent;

	public SendPingStateBeha(final AbstractDedaleAgent myagent, List<String> agentNames) {
		super(myagent);
		this.myAgent = (ExploreCoopAgentFSM) myagent;
		this.receivers = agentNames;
	}

	@Override
	public void action() {
		
		//Envoi du ping
		
		ACLMessage ping = new ACLMessage(ACLMessage.REQUEST);
		System.out.println("Agent "+this.myAgent.getLocalName()+" -- send ping to "+receivers);
		ping.setSender(this.myAgent.getAID());
		ping.setProtocol("PING");
		
		for (String agentName : receivers) {
			ping.addReceiver(new AID(agentName, AID.ISLOCALNAME));
		}
		((AbstractDedaleAgent)this.myAgent).sendMessage(ping);

		// Date set à now + 1s
		Date exp = new Date();
		exp.setTime(exp.getTime() + 1);
		myAgent.setExpiration(exp);
	}

}

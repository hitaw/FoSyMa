package eu.su.mas.dedaleEtu.mas.behaviours.custom;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.Date;
import java.util.List;

import static eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM.WaitTime;

public class SendPingPosStateBeha extends OneShotBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;

	private List<String> receivers;
	private ExploreCoopAgentFSM myAgent;

	public SendPingPosStateBeha(final AbstractDedaleAgent myagent, List<String> agentNames) {
		super(myagent);
		this.myAgent = (ExploreCoopAgentFSM) myagent;
		this.receivers = agentNames;
	}

	@Override
	public void action() {
		System.out.println(this.myAgent.getLocalName() + "SendPingPosState");
		//Envoi du ping
		MapRepresentation myMap = myAgent.getMyMap();
		if(myMap ==null) {
			this.myAgent.setMyMap(new MapRepresentation());
			myMap = myAgent.getMyMap();
		}
//		teamSize = myAgent.getTeamSize();

		ACLMessage ping = new ACLMessage(ACLMessage.INFORM);
//		System.out.println("Agent "+this.myAgent.getLocalName()+" -- send position to "+receivers);
		ping.setSender(this.myAgent.getAID());
		ping.setProtocol("POSITION");
		String destination = myMap.getLastNodePlan();
		if (destination == null) {
			destination = myAgent.getCurrentPosition().getLocationId();
		}

		ping.setContent(myAgent.getCurrentPosition().getLocationId() + ";" + destination + ";" + myAgent.getIteration());
		// Ajout des receivers
		for (String agentName : receivers) {
			ping.addReceiver(new AID(agentName, AID.ISLOCALNAME));
		}
		((AbstractDedaleAgent)this.myAgent).sendMessage(ping);

		// Date set à now + 0.1s
		Date exp = new Date();
		exp.setTime(exp.getTime() + WaitTime);
		myAgent.setExpiration(exp);
	}

}

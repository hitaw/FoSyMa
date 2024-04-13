package eu.su.mas.dedaleEtu.mas.behaviours.custom;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class DiagnoticStateBeha extends OneShotBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;

	private List<String> receivers;
	private ExploreCoopAgentFSM myAgent;
	private MapRepresentation myMap;
	public DiagnoticStateBeha(final AbstractDedaleAgent myagent, List<String> agentNames) {
		super(myagent);
		this.myAgent = (ExploreCoopAgentFSM) myagent;
		this.receivers = agentNames;
	}

	@Override
	public void action() {
		myMap = myAgent.getMyMap();

//		ACLMessage diag = new ACLMessage(ACLMessage.REQUEST);
//		System.out.println("Agent "+this.myAgent.getLocalName()+" -- send diag to "+receivers);
//		diag.setSender(this.myAgent.getAID());
//		diag.setProtocol("PING");
//
//		// Ajout des receivers
//		//si on a parlé à un agent il y a pas longtemps, on ne le ping pas
//		Set<String> recents = myAgent.getRecents();
//		for (String agentName : receivers) {
//			if (recents.contains(agentName)) {
//				continue;
//			}
//			diag.addReceiver(new AID(agentName, AID.ISLOCALNAME));
//		}
//		((AbstractDedaleAgent)this.myAgent).sendMessage(diag);
//
//		// Date set à now + 1s
//		Date exp = new Date();
//		exp.setTime(exp.getTime() + 100);
//		myAgent.setExpiration(exp);

		if (!myAgent.isAgentOn(myMap.getNextNodePlan())) {
			String golem = myMap.getNextNodePlan();
		}
	}

}

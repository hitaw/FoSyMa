package eu.su.mas.dedaleEtu.mas.behaviours.custom;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import static eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM.WaitTime;

public class FrozenStateBeha extends OneShotBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;

	private ExploreCoopAgentFSM myAgent;

	public FrozenStateBeha(final AbstractDedaleAgent myagent) {
		super(myagent);
		this.myAgent = (ExploreCoopAgentFSM) myagent;
	}

	@Override
	public void action() {
		System.out.println(this.myAgent.getLocalName() + " -- I'm frozen");

		// just slow the calculation
		try {
			this.myAgent.doWait(WaitTime/10);
		} catch (Exception e) {
			e.printStackTrace();
		}

		MessageTemplate diagTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol("DIAGNOSTIC"),
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

		ACLMessage diagReceived = this.myAgent.receive(diagTemplate);
		while (diagReceived != null) {
			String pos = diagReceived.getContent();
			if (pos.compareTo(((AbstractDedaleAgent)this.myAgent).getCurrentPosition().getLocationId()) == 0) {
				System.out.println(this.myAgent.getLocalName()+ "-- received diagnostic from "+diagReceived.getSender().getLocalName());
				ACLMessage diagAnswer = new ACLMessage(ACLMessage.CONFIRM);
				diagAnswer.setSender(this.myAgent.getAID());
				diagAnswer.setContent(pos);
				diagAnswer.addReceiver(diagReceived.getSender());
				diagAnswer.setProtocol("DIAGNOSTIC");
				((AbstractDedaleAgent)this.myAgent).sendMessage(diagAnswer);
			}
			diagReceived = this.myAgent.receive(diagTemplate);
		}
	}
}

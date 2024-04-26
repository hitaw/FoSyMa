package eu.su.mas.dedaleEtu.mas.behaviours.custom;

import eu.su.mas.dedale.env.gs.gsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import static eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM.WaitTime;

public class FrozenStateBeha extends OneShotBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;

	private ExploreCoopAgentFSM myAgent;
	boolean error = false;

	public FrozenStateBeha(final AbstractDedaleAgent myagent) {
		super(myagent);
		this.myAgent = (ExploreCoopAgentFSM) myagent;
	}

	@Override
	public void action() {
		error = false;
		System.out.println(this.myAgent.getLocalName() + " -- I'm frozen");

		// just slow the calculation
		try {
			this.myAgent.doWait(WaitTime/10);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (myAgent.getMyMap().isNeighbor(myAgent.getCurrentPosition().getLocationId(), myAgent.getGolemPos())){
			error = !myAgent.diagnostic(myAgent.getGolemPos(), false);
			if (this.myAgent.moveTo(new gsLocation(myAgent.getGolemPos()))) {
				error = true;
			}
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

	@Override
	public int onEnd() {
		if (error) {
			return 1;
		}
		return 0;
	}
}

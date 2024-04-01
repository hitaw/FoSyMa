package eu.su.mas.dedaleEtu.mas.behaviours.custom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM;


public class SendMapStateBeha extends OneShotBehaviour{
	
	private static final long serialVersionUID = 8567689731496787661L;

	private int cptAgents = 0;
	private List<String> agentNames;
	private ExploreCoopAgentFSM myAgent;

	
	public SendMapStateBeha (final AbstractDedaleAgent myagent) {
		super(myagent);
		this.myAgent = (ExploreCoopAgentFSM) myagent;
	}

	@Override
	public void action() {
		this.agentNames = this.myAgent.getVoisins();
		//envoi de la map

		for (String s : this.agentNames) {
			ACLMessage map = new ACLMessage(ACLMessage.INFORM);
			map.setProtocol("SHARE-TOPO");
			map.setSender(this.myAgent.getAID());

			map.addReceiver(new AID(s,AID.ISLOCALNAME));
			SerializableSimpleGraph<String, MapAttribute> sg = this.myAgent.getDiffAgentMap(s);
			if (sg != null) {
				this.myAgent.updateAgentMap(s, sg);
				try {
					map.setContentObject(sg);
				} catch (IOException e) {
					e.printStackTrace();
				}
				((AbstractDedaleAgent)this.myAgent).sendMessage(map);
				System.out.println("Agent "+this.myAgent.getLocalName()+" -- map sent to "+this.agentNames);
			} else {
				System.out.println("Agent "+this.myAgent.getLocalName()+" -- no map to send to "+this.agentNames);
			}
		}
	}
}

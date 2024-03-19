package eu.su.mas.dedaleEtu.mas.behaviours.custom;

import java.io.IOException;
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
	private MapRepresentation myMap;
	private ExploreCoopAgentFSM myAgent;

	
	public SendMapStateBeha (final AbstractDedaleAgent myagent) {
		super(myagent);
		this.myAgent = (ExploreCoopAgentFSM) myagent;
		this.myMap = myAgent.getMyMap();
		this.agentNames = this.myAgent.getVoisins();
	}

	@Override
	public void action() {
		//Reception du yes + envoi de la map
		// wait
		this.myAgent.doWait(100);
		
		MessageTemplate msgTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol("YES"),
				MessageTemplate.MatchPerformative(ACLMessage.AGREE));
		
		ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
		
		ACLMessage map = new ACLMessage(ACLMessage.INFORM);
		map.setProtocol("SHARE-TOPO");
		map.setSender(this.myAgent.getAID());
		
		while (msgReceived != null) {
		
			cptAgents++;
			map.addReceiver(msgReceived.getSender());
			msgReceived = this.myAgent.receive(msgTemplate);
		}
		
		for (String s : this.agentNames) {
			map.addReceiver(new AID(s,AID.ISLOCALNAME));
		}
		
		SerializableSimpleGraph<String, MapAttribute> sg=this.myMap.getSerializableGraph();
		try {					
			map.setContentObject(sg);
		} catch (IOException e) {
			e.printStackTrace();
		}
		((AbstractDedaleAgent)this.myAgent).sendMessage(map);
		
	}
}

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

public class ReceiveMapBehaviour extends SimpleBehaviour{
	
	private static final long serialVersionUID = 8567689731496787661L;

	private boolean finished = false;
	
	public ReceiveMapBehaviour (final AbstractDedaleAgent myagent, List<String> agentNames) {
		super(myagent);
	}

	@Override
	public void action() {
		
		// Reception de la map
		
		MessageTemplate shareTemplate=MessageTemplate.and(
		MessageTemplate.MatchProtocol("SHARE-TOPO"),
		MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage shareReceived=this.myAgent.receive(msgTemplate);
		if (shareReceived!=null) {
			SerializableSimpleGraph<String, MapAttribute> sgreceived=null;
			try {
				sgreceived = (SerializableSimpleGraph<String, MapAttribute>)shareReceived.getContentObject();
			} catch (UnreadableException e) {
				e.printStackTrace();
			}
			this.myMap.mergeMap(sgreceived);
		}	
	}

	@Override
	public boolean done() {
		return finished;
	}
}

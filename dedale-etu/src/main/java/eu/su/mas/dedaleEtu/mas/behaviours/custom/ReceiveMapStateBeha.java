package eu.su.mas.dedaleEtu.mas.behaviours.custom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import static eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM.WaitTime;

public class ReceiveMapStateBeha extends OneShotBehaviour{
	
	private static final long serialVersionUID = 8567689731496787661L;
	private ExploreCoopAgentFSM myAgent;

	public ReceiveMapStateBeha (final AbstractDedaleAgent myagent) {
		super(myagent);
		myAgent = (ExploreCoopAgentFSM) myagent;
	}

	@Override
	public void action() {
		Date exp = new Date();
		exp.setTime(exp.getTime() + WaitTime * 3); // We wait for a rather long time because we want to be sure to receive all the maps

		// Reception de la map
		int nbCartesAttendues = myAgent.getNbCartesAttendues();
		System.out.println(this.myAgent.getLocalName() + "--- I expect " + nbCartesAttendues + " maps ");
		int cptAgents = 0;

		MessageTemplate msgTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol("SHARE-TOPO"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));

		while ((cptAgents < nbCartesAttendues) && (new Date().before(exp)) ){
			ACLMessage shareReceived=this.myAgent.receive(msgTemplate);
			if (shareReceived!=null) {
				SerializableSimpleGraph<String, Couple<MapAttribute, Couple<Date, Integer>>> sgreceived=null;
				try {
					sgreceived = (SerializableSimpleGraph<String, Couple<MapAttribute, Couple<Date, Integer>>>)shareReceived.getContentObject();
					System.out.println("Agent "+this.myAgent.getLocalName()+" -- received map from "+shareReceived.getSender().getLocalName());
					myAgent.addRecent(shareReceived.getSender().getLocalName());
				} catch (UnreadableException e) {
					e.printStackTrace();
				}
				this.myAgent.mergeMap(sgreceived);
				cptAgents++;
				myAgent.updateAgentMap(shareReceived.getSender().getLocalName(), sgreceived);
			}
		}
		this.myAgent.setVoisins(new ArrayList<String>(), 0);
	}
}

package eu.su.mas.dedaleEtu.mas.agents.custom;

import java.util.ArrayList;
import java.util.List;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.*;

import eu.su.mas.dedaleEtu.mas.behaviours.ExploCoopBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.custom.SendMapStateBeha;
import eu.su.mas.dedaleEtu.mas.behaviours.custom.ReceiveMapStateBeha;
import eu.su.mas.dedaleEtu.mas.behaviours.custom.SendPingStateBeha;
import eu.su.mas.dedaleEtu.mas.behaviours.custom.WalkStateBeha;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;

import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;

/**
 * <pre>
 * ExploreCoop agent FSM. 
 * Basic example of how to "collaboratively" explore the map
 *  - It explore the map using a DFS algorithm and blindly tries to share the topology with the agents within reach.
 *  - The shortestPath computation is not optimized
 *  - Agents do not coordinate themselves on the node(s) to visit, thus progressively creating a single file. It's bad.
 *  - The agent sends all its map, periodically, forever. Its bad x3.
 *  - You should give him the list of agents'name to send its map to in parameter when creating the agent.
 *   Object [] entityParameters={"Name1","Name2};
 *   ag=createNewDedaleAgent(c, agentName, ExploreCoopAgent.class.getName(), entityParameters);
 *  
 * It stops when all nodes have been visited.
 * 
 * 
 *  </pre>
 *  
 * @author hc
 *
 */


public class ExploreCoopAgentFSM extends AbstractDedaleAgent {

	private static final long serialVersionUID = -7969469610241668140L;
	private MapRepresentation myMap;
	
	// State names 
	private static final String A = "SendPing";
	private static final String B = "WaitAnswer";
	private static final String C = "Walk";
	private static final String D = "ReceiveMap";
	private static final String E = "SendMap";
	
	
	/**
	 * This method is automatically called when "agent".start() is executed.
	 * Consider that Agent is launched for the first time. 
	 * 			1) set the agent attributes 
	 *	 		2) add the behaviours
	 *          
	 */
	protected void setup(){		

		super.setup();
		
		//get the parameters added to the agent at creation (if any)
		final Object[] args = getArguments();
		
		List<String> list_agentNames=new ArrayList<String>();
		
		if(args.length==0){
			System.err.println("Error while creating the agent, names of agent to contact expected");
			System.exit(-1);
		}else{
			int i=2;// WARNING YOU SHOULD ALWAYS START AT 2. This will be corrected in the next release.
			while (i<args.length) {
				list_agentNames.add((String)args[i]);
				i++;
			}
		}

		List<Behaviour> lb=new ArrayList<Behaviour>();
		
		/************************************************
		 * 
		 * ADD the behaviors of the Dummy Moving Agent
		 * 
		 ************************************************/
		
		FSMBehaviour fsm = new FSMBehaviour(this);
		
		// Define the different states and behaviour
		fsm.registerFirstState(new SendPingStateBeha(this, list_agentNames), A); // TODO Behaviour
		fsm.registerState(new StateBeha(), B);
		fsm.registerState(new SendMapStateBeha(this, TODO, myMap), E);
		fsm.registerState(new ReceiveMapStateBeha(this, TODO), D); // Trouver comment transférer la liste des voisins
		fsm.registerLastState(new WalkStateBeha(this, myMap, list_agentNames), C);
		
		// Register the transitions
		fsm.registerDefaultTransition(A, B);
		fsm.registerDefaultTransition(B, B);
		fsm.registerTransition(B, C, 2);
		fsm.registerTransition(B, E, 1);
		fsm.registerTransition(C, A, 1);
		fsm.registerDefaultTransition(D, B);
		fsm.registerDefaultTransition(E, D);

		lb.add(fsm);
		
		
		/***
		 * MANDATORY TO ALLOW YOUR AGENT TO BE DEPLOYED CORRECTLY
		 */
		
		
		addBehaviour(new startMyBehaviours(this,lb));
		
		System.out.println("the  agent "+this.getLocalName()+ " is started");

	}
	
	
	
}
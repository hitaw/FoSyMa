package eu.su.mas.dedaleEtu.mas.agents.custom;

import java.util.*;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.*;

import eu.su.mas.dedaleEtu.mas.behaviours.custom.*;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;

import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;
import org.graphstream.graph.Edge;

/**
 * <pre>
 * ExploreCoop agent FSM. 
 * Basic example of how to "collaboratively" explore the map
 *  - It explore the map using a DFS algorithm and blindly tries to share the topology with the agents within reach.
 *  - The shortestPath computation is not optimized
 *  - Agents do not coordinate themselves on the node(s) to visit, thus progressively creating a single file. It's bad.
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
	public static final int ExchangeTimeout = 3;
	public static final int MaxDistanceGolem = 3; //This determines the distance maximum that a team is going to try to move a golem in order to block it.
	public static final int MaxTeamDistance = 3; // change this to change the distance max for which two agents will consider that they are hunting the same golem
	public static final int WaitTime = 100;
	private boolean hunting = false;
	private MapRepresentation myMap;
	private List<String> team = new ArrayList<String>();
	
	// State names 
	private static final String A = "SendPing";
	private static final String B = "WaitAnswer";
	private static final String C = "Walk";
	private static final String D = "ReceiveMap";
	private static final String E = "SendMap";
	private static final String F = "Gathering";
	private static final String G = "Diagnostic";
	private static final String H = "WaitAnswerHunt";
	private static final String I = "SendPingPosState";
	private static final String J = "TeamBuilding";
	public static final int MaxStuck = 2;

	private List<String> voisins = new ArrayList<String>();
	private int nbCartesAttendues = 0;
	private Map<String,Integer> recents = new HashMap<String,Integer>();
	private Map<String, MapRepresentation> receivedMaps = new HashMap<String, MapRepresentation>();
	private Map<String, String> agentsPositions = new HashMap<String, String>();
	private Date expiration = new Date();

	// the number of times the agent tried to go to the same node and failed
	private int stuck = 0;
	private Map<Edge, Integer> edgesRemoved = new HashMap<Edge, Integer>();
	
	
	/**
	 * This method is automatically called when "agent".start() is executed.
	 * Consider that Agent is launched for the first time. 
	 * 			1) set the agent attributes 
	 *	 		2) add the behaviours
	 *          
	 */
	protected void setup(){
		super.setup();
		this.hunting = false;
		this.team.add(this.getLocalName());
		
		//get the parameters added to the agent at creation (if any)
		final Object[] args = getArguments();
		
		List<String> list_agentNames = new ArrayList<String>();

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

		//Initialize the maps of agents to null
		for (String agentName : list_agentNames) {
			receivedMaps.put(agentName, null);
		}

		List<Behaviour> lb=new ArrayList<Behaviour>();
		
		FSMBehaviour fsm = new FSMBehaviour(this);
		
		// Define the different states and behaviour
		fsm.registerFirstState(new SendPingStateBeha(this, list_agentNames), A);
		fsm.registerState(new WaitAnswerStateBeha(this), B);
		fsm.registerState(new SendMapStateBeha(this), E);
		fsm.registerState(new ReceiveMapStateBeha(this), D);
		fsm.registerState(new WalkStateBeha(this), C);
		fsm.registerState(new GatheringStateBeha(this, list_agentNames.size()), F);
		fsm.registerState(new DiagnoticStateBeha(this, list_agentNames), G);
		fsm.registerState(new WaitAnswerHuntStateBeha(this), H);
		fsm.registerState(new SendPingPosState(this, list_agentNames), I);
		fsm.registerState(new TeamBuildingState(this), J);
		
		// Register the transitions
		fsm.registerDefaultTransition(A, B);
		fsm.registerTransition(B, C, 2);
		fsm.registerTransition(B, E, 1);
		fsm.registerTransition(B, B, 0);
		fsm.registerTransition(C, A, 1);
		fsm.registerTransition(C, F, 0);
		fsm.registerDefaultTransition(D, B);
		fsm.registerTransition(E, D, 0);
		fsm.registerTransition(E, F, 1);
		fsm.registerTransition(F,H, 1);
		fsm.registerTransition(F,I,0);
		fsm.registerDefaultTransition(I,J);
		fsm.registerTransition(H,H,0);
		fsm.registerTransition(H, E,1);
		fsm.registerTransition(H, F,2);
		fsm.registerTransition(J, H, 1);
		fsm.registerTransition(J, J, 0);


		lb.add(fsm);
		
		
		/***
		 * MANDATORY TO ALLOW YOUR AGENT TO BE DEPLOYED CORRECTLY
		 */
		
		
		addBehaviour(new startMyBehaviours(this,lb));
		
		System.out.println("the  agent "+this.getLocalName()+ " is started");

	}

	public List<String> getVoisins() {
		Set<String> set = new HashSet<>(voisins);
		voisins.clear();
		voisins.addAll(set);
		return voisins;
	}
    public void setVoisins(List<String> v, int nbCartesAttendues) {
        voisins = v;
		this.nbCartesAttendues = nbCartesAttendues;
    }

	public int getNbCartesAttendues() {
		return nbCartesAttendues;
	}

    public void addVoisin(String s, Integer i) {
        voisins.add(s);
    }

    public void removeVoisin(String s) {
        voisins.remove(s);
    }

	public void mergeMap(SerializableSimpleGraph<String, Couple<MapRepresentation.MapAttribute, Couple<Date, Integer>>> sgreceived) {
		this.myMap.mergeMap(sgreceived);
		for (Edge e : edgesRemoved.keySet()) {
			this.myMap.removeEdge(e.getSourceNode().getId(), e.getTargetNode().getId());
		}
	}

	public Date getExpiration() {
		return expiration;
	}

	public void setExpiration(Date expiration) {
		this.expiration = expiration;
	}

	public MapRepresentation getMyMap() {
		return this.myMap;
	}

	public void setMyMap(MapRepresentation myMap) {
		this.myMap = myMap;
	}

	public Set<String> getRecents() {
		return recents.keySet();
	}

	public void setRecents(Map<String, Integer> recents) {
		this.recents = recents;
	}
	public void addRecent(String s) {
		recents.put(s, 0);
	}

	public void ageRecent() {
		System.out.println(this.getLocalName() + "---" + recents);
		List<String> toRemove = new ArrayList<>();
		for (String key : recents.keySet()) {
			recents.put(key, recents.get(key) + 1);
			if (recents.get(key) > ExchangeTimeout) {
				toRemove.add(key);
			}
		}
		for (String s : toRemove) {
			recents.remove(s);
		}
	}

	public int getStuck() {
		return stuck;
	}

	public void setStuck(int stuck) {
		if (stuck!=this.stuck) System.out.println("Agent "+this.getLocalName()+" -- stuck for "+stuck+ " steps");
		this.stuck = stuck;
	}
	public Edge getEdge() {
		Edge e = null;
		for (Edge key : edgesRemoved.keySet()) {
			if (edgesRemoved.get(key) > WalkStateBeha.TimeoutRemovedEdge){
				e = key;
				edgesRemoved.remove(key);
				System.out.println("Agent "+this.getLocalName()+" -- added edge "+e.getId()+"\n");
				break;
			}
		}
		return e;
	}
	public boolean isRemovedEdge(String from, String to) {
		for (Edge key : edgesRemoved.keySet()) {
			if (key.getSourceNode().getId().equals(from) && key.getTargetNode().getId().equals(to)) {
				return true;
			}
		}
		return false;
	}
	public void addRemovedEdge(Edge e) {
		if (e != null) {
			System.out.println("Agent "+this.getLocalName()+" -- removed edge "+e.getId());
			edgesRemoved.put(e, 0);
		}
	}

	public void ageRemovedEdges() {
		for (Edge key : edgesRemoved.keySet()) {
			edgesRemoved.put(key, edgesRemoved.get(key) + 1);
		}
	}

	public void addAllEdges() {
		for (Edge key : edgesRemoved.keySet()) {
			this.myMap.addEdge(key.getSourceNode().getId(), key.getTargetNode().getId(), key.getId(), this.getLocalName());
		}
	}

	public void clearRemovedEdges() {
		this.edgesRemoved = new HashMap<Edge, Integer>();
	}

	public MapRepresentation getAgentMap(String agentName) {
		return receivedMaps.get(agentName);
	}
	public SerializableSimpleGraph<String, Couple<MapRepresentation.MapAttribute, Couple<Date, Integer>>> getDiffAgentMap(String agentName) {
		MapRepresentation m = receivedMaps.get(agentName);
		if (m == null) {
			return myMap.getSerializableGraph();
		} else {
			return myMap.getDiff(m);
		}
	}

	public void updateAgentMap(String agentName, SerializableSimpleGraph<String, Couple<MapRepresentation.MapAttribute, Couple<Date, Integer>>> map) {
		MapRepresentation m = receivedMaps.get(agentName);
		if (m == null) {
			m = new MapRepresentation(false);
        }
        m.mergeMap(map);
        receivedMaps.put(agentName, m);
    }

	public void updateAgentPosition(String agent, String locationId) {
		agentsPositions.put(agent, locationId);
	}

	public boolean isHunting() {
		return hunting;
	}

	public void setHunting(boolean hunting) {
		this.hunting = hunting;
	}

	public boolean isAgentOn(String location) {
		return agentsPositions.containsValue(location);
	}

	public List<String> getTeam() {
		return team;
	}
	public boolean isTeamMember(String agent) {
		return team.contains(agent);
	}
	public boolean addTeamMember(String agent) {
		return team.add(agent);
	}
	public boolean removeTeamMember(String agent) {
		return team.remove(agent);
	}
}
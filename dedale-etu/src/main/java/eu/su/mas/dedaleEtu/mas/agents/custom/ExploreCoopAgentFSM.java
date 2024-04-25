package eu.su.mas.dedaleEtu.mas.agents.custom;

import java.util.*;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.*;

import eu.su.mas.dedaleEtu.mas.behaviours.custom.*;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.graphstream.graph.Edge;

public class ExploreCoopAgentFSM extends AbstractDedaleAgent {

	private static final long serialVersionUID = -7969469610241668140L;

	// State names
	private static final String A = "SendPing";
	private static final String B = "WaitAnswer";
	private static final String C = "Walk";
	private static final String D = "ReceiveMap";
	private static final String E = "SendMap";
	private static final String F = "Gathering";
	private static final String H = "WaitAnswerHunt";
	private static final String I = "SendPingPosState";
	private static final String J = "TeamBuilding";
	private static final String K = "TeamStrategy";
	private static final String L = "CaptainStrategy";
	private static final String M = "Frozen";

	// General variables
	List<String> list_agentNames = new ArrayList<String>();
	public static final int WaitTime = 200;
	private MapRepresentation myMap;
	private Date expiration = new Date();
	private Map<Edge, Integer> edgesRemoved = new HashMap<Edge, Integer>();
	public static final int MaxStuck = 2;
	// the number of times the agent tried to go to the same node and failed
	private int stuck = 0;
	// Exploration variables
	private List<String> voisins = new ArrayList<String>();
	private Map<String,Integer> recents = new HashMap<String,Integer>();
	private Map<String, MapRepresentation> receivedMaps = new HashMap<String, MapRepresentation>();
	public static final int ExchangeTimeout = 3;
	private int nbCartesAttendues = 0;
	// Hunting variables
	private boolean hunting = false;
	private List<String> team = new ArrayList<String>();
	public static final int MaxDistanceGolem = 3; //This determines the distance maximum that a team is going to try to move a golem in order to block it.
	private Map<String, Couple<String,String>> agentsPositions = new HashMap<String, Couple<String, String>>();
	private List<String> line;
	private List<String> nextLine;
	private Couple<String, Date> golemPos = new Couple<>(null, null);
	private int iteration = 0;
	private boolean goToNext = false;

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
		fsm.registerState(new WaitAnswerHuntStateBeha(this), H);
		fsm.registerState(new SendPingPosStateBeha(this, list_agentNames), I);
		fsm.registerState(new TeamBuildingStateBeha(this), J);
		fsm.registerState(new TeamStrategyStateBeha(this), K);
		fsm.registerState(new CaptainStrategyStateBeha(this), L);
		fsm.registerState(new FrozenStateBeha(this), M);
		
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
		fsm.registerTransition(H, H,0);
		fsm.registerTransition(H, E,1);
		fsm.registerTransition(H, F,2);
		fsm.registerTransition(J, H, 1);
		fsm.registerTransition(J, J, 0);
		fsm.registerTransition(H, K, 3);
		fsm.registerTransition(H, L,4);
		fsm.registerTransition(L, I, 0);
		fsm.registerDefaultTransition(K,I);
		fsm.registerTransition(H, M, 5);
		fsm.registerDefaultTransition(M, M);
		fsm.registerTransition(L, M, 1);

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

	public void updateAgentPosition(String agent, String locationId, String destinationId) {
		if (locationId == null) {
			locationId = agentsPositions.containsKey(agent) ? agentsPositions.get(agent).getLeft() : null;
		}
		if (destinationId == null) {
			destinationId = agentsPositions.containsKey(agent) ? agentsPositions.get(agent).getRight() : null;
		}
		agentsPositions.put(agent, new Couple<>(locationId, destinationId));
	}
	public String getAgentPosition(String agent){ return agentsPositions.get(agent) != null ? agentsPositions.get(agent).getLeft(): null;}
	public String getAgentDestination(String agent) {return agentsPositions.get(agent) != null ? agentsPositions.get(agent).getRight(): null;}

	public boolean isHunting() {
		return hunting;
	}

	public void setHunting(boolean hunting) {
		this.hunting = hunting;
	}

	public boolean isAgentOn(String location) {
		for (String agent : agentsPositions.keySet()) {
			String l = agentsPositions.get(agent).getLeft();
			if (l != null && l.compareTo(location) == 0) {
				return true;
			}
		}
		return false;
	}

	public List<String> getTeam() {
		return team;
	}
	public boolean isTeamMember(String agent) {
		return team.contains(agent);
	}
	public boolean addTeamMember(String agent) {
		boolean res = team.add(agent);
		Collections.sort(team);
		return res;
	}
	public boolean removeTeamMember(String agent) {
		return team.remove(agent);
	}
	public String getChefName() {
		String chef = team.get(0);
		for (String agent : team) {
			if (agent.compareToIgnoreCase(chef) < 0) {
				chef = agent;
			}
		}
		return chef;
	}

	public void resetTeam() {
		team = new ArrayList<String>();
		team.add(this.getLocalName());
	}

	public List<String> getLine() {
		return line;
	}

	public void setLine(List<String> line) {
		this.line = line;
	}

	public List<String> getNextLine() {
		return nextLine;
	}

	public void setNextLine(List<String> nextLine) {
		this.nextLine = nextLine;
	}
	public void setGolemPos(String pos, Date date) {this.golemPos = new Couple<>(pos, date);}
	public String getGolemPos() {
		return golemPos.getLeft();
	}
	public Date getGolemDate() {
		return golemPos.getRight();
	}
	public void removeGolemPos() {this.golemPos = new Couple<>(null, null);}

	/**
	 * a diagnostic funtion to be called when moveTo fails
	 * Warning, if called on an empty stinky node, will return true
	 * @return true if there is a golem in front of the agent, false if it is an agent
	 */
	public boolean diagnostic(String nextNodeId) {
		// If we know that there is an agent in front, no need to do anything
//		myMap = this.getMyMap();
//		String golem = myMap.getNextNodePlan();
		String golem = nextNodeId;
		if (this.isAgentOn(golem)) {
			return false;
		}
		// If the node has no stench, it cannot be a golem
		List<Couple<Location,List<Couple<Observation,Integer>>>> lobs=this.observe();//myPosition
		boolean stinks = true;
		Couple<Location, List<Couple<Observation, Integer>>> obs;
		for (int i = 0; i < lobs.size(); i++) {
			obs = lobs.get(i);
			if (obs.getLeft().getLocationId().compareTo(golem) == 0) {
				stinks = !obs.getRight().isEmpty() && (obs.getRight().get(0).getLeft().toString().equals("Stench"));
				break;
			}
		}
		if (!stinks) {
			return false;
		}

		ACLMessage diag = new ACLMessage(ACLMessage.REQUEST);
		System.out.println("Agent "+this.getLocalName()+" -- send diag to "+list_agentNames);
		diag.setSender(this.getAID());
		diag.setProtocol("DIAGNOSTIC");
		diag.setContent(golem);
		for (String agentName : list_agentNames) {
			if (agentName.compareTo(this.getLocalName())==0) continue;
			diag.addReceiver(new AID(agentName, AID.ISLOCALNAME));
		}
		this.sendMessage(diag);

		Date expiration = new Date(new Date().getTime()+WaitTime*2);
		MessageTemplate repTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol("DIAGNOSTIC"),
				MessageTemplate.MatchPerformative(ACLMessage.CONFIRM));
		boolean isAgent = false;

		ACLMessage rep = this.receive(repTemplate);
		while ((!isAgent) && (new Date().before(expiration))) { // we set a date after which we decide that we are facing a golem
			while (rep != null) {
				String content = rep.getContent();
				String sender = rep.getSender().getLocalName();
				if (content.compareTo(golem) == 0) {
					isAgent = true;
					System.out.println(this.getLocalName() + "just stuck on "+ sender);
					this.updateAgentPosition(sender, content, content);
					break;
				}
				rep = this.receive(repTemplate);
			}
		}
		if (!isAgent) {
			System.out.println(this.getLocalName() + " -- golem detected at "+golem);
			Date date = new Date();
			this.setGolemPos(golem, date);
			this.setLine(null);
			this.setNextLine(null);
			ACLMessage spotted = new ACLMessage(ACLMessage.INFORM);
			spotted.setSender(this.getAID());
			spotted.setProtocol("GOLEM");
			spotted.setContent(golem+ ";"+date.getTime());
			for (String agent : list_agentNames) {
				if (agent.compareTo(this.getLocalName()) == 0) continue;
				spotted.addReceiver(new AID(agent, AID.ISLOCALNAME));
			}
			this.sendMessage(spotted);
		}
		return !isAgent;
	}

	public int getIteration() {
		return iteration;
	}

	public void setIteration(int iteration) {
		this.iteration = iteration;
	}

	public void increaseIteration() {
		this.iteration++;
	}

	public boolean isReady() {
		System.out.println("Call is team ready ?");
//		for (int i = 1; i < team.size(); i++) { // start at 1 because index 0 is the captain calling is method
//			String agent = team.get(i);
//			if (line.size() > i && agentsPositions.get(agent).getLeft().compareTo(line.get(i)) != 0) return false;
//		}
		int cpt = 1;
		for (int i = 1; i < team.size(); i++) {
			String agent = team.get(i);
			if (line.contains(getAgentPosition(agent))) cpt ++;
		}
		return (cpt == team.size() || cpt == line.size());
	}

	public boolean getGoToNext() {
		return goToNext;
	}

	public void setGoToNext(boolean goToNext) {
		this.goToNext = goToNext;
	}

	public void restartStrategy() {
		setLine(null);
		setNextLine(null);
		setGoToNext(false);
	}

	public void sendStrategy(String agent) {
		System.out.println(this.getLocalName() + " -- send strategy to "+agent);
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setSender(getAID());
		msg.setProtocol("PLAN");
		int i = iteration-1;
		if (line != null && nextLine != null)
		msg.setContent(i + ":"+ line +";" + iteration + ":" + nextLine + ";0");
		else msg.setContent("null");
		msg.addReceiver(new AID(agent, AID.ISLOCALNAME));
		sendMessage(msg);
	}
}
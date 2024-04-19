package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.gsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.behaviours.SimpleBehaviour;

import static eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM.WaitTime;


/**
 * This behaviour allows an agent to explore the environment and learn the associated topological map.
 * The algorithm is a pseudo - DFS computationally consuming because its not optimised at all.
 * <p>
 * 
 * When all the nodes around him are visited, the agent randomly select an open node and go there to restart its dfs.<p>
 * This (non optimal) behaviour is done until all nodes are explored. <p> 
 * 
 * Warning, this behaviour does not save the content of visited nodes, only the topology.<p> 
 * Warning, this behaviour is a solo exploration and does not take into account the presence of other agents (or well) and indefinitely tries to reach its target node
 * @author hc
 *
 */
public class ExploSoloBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;

	private boolean finished = false;

	/**
	 * Current knowledge of the agent regarding the environment
	 */
	private MapRepresentation myMap;

	public ExploSoloBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap) {
		super(myagent);
		this.myMap=myMap;
		
	}

	@Override
	public void action() {

		if(this.myMap==null)
			this.myMap= new MapRepresentation();
		
		//0) Retrieve the current position
		Location myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
	
		if (myPosition!=null){
			//List of observable from the agent's current position
			List<Couple<Location,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition

			/**
			 * Just added here to let you see what the agent is doing, otherwise he will be too quick
			 */
			try {
				this.myAgent.doWait(WaitTime);
			} catch (Exception e) {
				e.printStackTrace();
			}

			//1) remove the current node from openlist and add it to closedNodes.
			this.myMap.addNode(myPosition.getLocationId(),MapAttribute.closed);

			//2) get the surrounding nodes and, if not in closedNodes, add them to open nodes.
			String nextNodeId=null;
			Iterator<Couple<Location, List<Couple<Observation, Integer>>>> iter=lobs.iterator();
			while(iter.hasNext()){
				Location accessibleNode=iter.next().getLeft();
				boolean isNewNode=this.myMap.addNewNode(accessibleNode.getLocationId(), false);
				//the node may exist, but not necessarily the edge
				if (myPosition.getLocationId()!=accessibleNode.getLocationId()) {
					this.myMap.addEdge(myPosition.getLocationId(), accessibleNode.getLocationId());
					if (nextNodeId==null && isNewNode) nextNodeId=accessibleNode.getLocationId();
				}
			}

			//3) while openNodes is not empty, continues.
			if (!this.myMap.hasOpenNode()){
				//Explo finished
				finished=true;
				System.out.println(this.myAgent.getLocalName()+" - Exploration successufully done, behaviour removed.");
			}else{
				//4) select next move.
				//4.1 If there exist one open node directly reachable, go for it,
				//	 otherwise choose one from the openNode list, compute the shortestPath and go for it
				if (nextNodeId==null){
					//no directly accessible openNode
					//chose one, compute the path and take the first step.
					nextNodeId=this.myMap.getShortestPathToClosestOpenNode(myPosition.getLocationId()).get(0);//getShortestPath(myPosition,this.openNodes.get(0)).get(0);
					//System.out.println(this.myAgent.getLocalName()+"-- list= "+this.myMap.getOpenNodes()+"| nextNode: "+nextNode);
				}else {
					//System.out.println("nextNode notNUll - "+this.myAgent.getLocalName()+"-- list= "+this.myMap.getOpenNodes()+"\n -- nextNode: "+nextNode);
				}
				
				
				
				/***************************************************
				** 		ADDING the API CALL to illustrate their use **
				*****************************************************/

				//list of observations associated to the currentPosition
				List<Couple<Observation,Integer>> lObservations= lobs.get(0).getRight();
				System.out.println(this.myAgent.getLocalName()+" - State of the observations : "+lobs);
				
				//example related to the use of the backpack for the treasure hunt
				Boolean b=false;
				for(Couple<Observation,Integer> o:lObservations){
					switch (o.getLeft()) {
					case DIAMOND:case GOLD:

						System.out.println(this.myAgent.getLocalName()+" - My treasure type is : "+((AbstractDedaleAgent) this.myAgent).getMyTreasureType());
						System.out.println(this.myAgent.getLocalName()+" - My current backpack capacity is:"+ ((AbstractDedaleAgent) this.myAgent).getBackPackFreeSpace());
						System.out.println(this.myAgent.getLocalName()+" - My expertise is: "+((AbstractDedaleAgent) this.myAgent).getMyExpertise());
						System.out.println(this.myAgent.getLocalName()+" - I try to open the safe: "+((AbstractDedaleAgent) this.myAgent).openLock(Observation.GOLD));
						System.out.println(this.myAgent.getLocalName()+" - Value of the treasure on the current position: "+o.getLeft() +": "+ o.getRight());
						System.out.println(this.myAgent.getLocalName()+" - The agent grabbed : "+((AbstractDedaleAgent) this.myAgent).pick());
						System.out.println(this.myAgent.getLocalName()+" - the remaining backpack capacity is: "+ ((AbstractDedaleAgent) this.myAgent).getBackPackFreeSpace());
						b=true;
						break;
					default:
						break;
					}
				}

				//If the agent picked (part of) the treasure
				if (b){
					List<Couple<Location,List<Couple<Observation,Integer>>>> lobs2=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
					System.out.println(this.myAgent.getLocalName()+" - State of the observations after picking "+lobs2);
					
					//Trying to store everything in the tanker
					System.out.println(this.myAgent.getLocalName()+" - My current backpack capacity is:"+ ((AbstractDedaleAgent)this.myAgent).getBackPackFreeSpace());
					System.out.println(this.myAgent.getLocalName()+" - The agent tries to transfer is load into the Silo (if reachable); succes ? : "+((AbstractDedaleAgent)this.myAgent).emptyMyBackPack("Silo"));
					System.out.println(this.myAgent.getLocalName()+" - My current backpack capacity is:"+ ((AbstractDedaleAgent)this.myAgent).getBackPackFreeSpace());
					
				}
				
				//Trying to store everything in the tanker
				//System.out.println(this.myAgent.getLocalName()+" - My current backpack capacity is:"+ ((AbstractDedaleAgent)this.myAgent).getBackPackFreeSpace());
				//System.out.println(this.myAgent.getLocalName()+" - The agent tries to transfer is load into the Silo (if reachable); succes ? : "+((AbstractDedaleAgent)this.myAgent).emptyMyBackPack("Silo"));
				//System.out.println(this.myAgent.getLocalName()+" - My current backpack capacity is:"+ ((AbstractDedaleAgent)this.myAgent).getBackPackFreeSpace());


				/************************************************
				 * 				END API CALL ILUSTRATION
				 *************************************************/
				((AbstractDedaleAgent)this.myAgent).moveTo(new gsLocation(nextNodeId));
			}

		}
	}

	@Override
	public boolean done() {
		return finished;
	}

}

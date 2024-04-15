package eu.su.mas.dedaleEtu.mas.behaviours.custom;

import java.util.*;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.gsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.behaviours.OneShotBehaviour;
import org.graphstream.graph.Edge;

import static eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM.MaxStuck;

public class GatheringStateBeha extends OneShotBehaviour {

        private static final long serialVersionUID = 8567689731496787661L;

        private MapRepresentation myMap;
        private int nbAgents;
        private ExploreCoopAgentFSM myAgent;
        private boolean talk;


        public GatheringStateBeha (AbstractDedaleAgent myagent, int nbAgents) {
            super(myagent);
            myAgent = (ExploreCoopAgentFSM) myagent;
            this.nbAgents = nbAgents;
        }
        @Override
        public void action() {
            talk = false;
            System.out.println(this.myAgent.getLocalName() +  " -- GatheringStateBeha");
            this.myMap=myAgent.getMyMap();

            if(this.myMap==null) { // Should never happen but who knows
                this.myMap = new MapRepresentation();
            }

            //0) Retrieve the current position
            Location myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();

            if (myPosition!=null) {

                /**
                 * Just added here to let you see what the agent is doing, otherwise he will be too quick
                 */
                try {
                    this.myAgent.doWait(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Updates stenches observed
                List<Couple<Location,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
                Iterator<Couple<Location, List<Couple<Observation, Integer>>>> iter=lobs.iterator();
                Couple<Location, List<Couple<Observation, Integer>>> obs;
                Location accessibleNode = myAgent.getCurrentPosition();
                while(iter.hasNext()) {
                    obs = iter.next();
                    accessibleNode = obs.getLeft();
                    boolean stench = !obs.getRight().isEmpty() && (obs.getRight().get(0).getLeft().toString().equals("Stench"));
                    this.myMap.addNewNode(accessibleNode.getLocationId(), stench); // this also updates the stench date on already existing nodes
                }

                String nextNodeId = null;

                if (myAgent.getStuck() > MaxStuck) {
                    myAgent.setStuck(0);
                    Edge e = myMap.removeEdge(myPosition.getLocationId(), myMap.getNextNodePlan());
                    myAgent.addRemovedEdge(e);
                    myMap.clearPlannedItinerary(); //The plan that was calculated is no longer valid, we have to calculate a new one
                }

                if (myMap.getNextNodePlan() != null) {
                    if (myMap.destinationInRange(nbAgents)) { //change the argument here to decide when the agents start creating teams
                        talk = true;
                    }
                    nextNodeId = myMap.getNextNodePlan();
                } else {
                    // Walk to node with the highest stench count
                    String dest = myMap.getStinkiestNode();
                    System.out.println(this.myAgent.getLocalName() + " -- GatheringStateBeha: going to " + dest);

                    // Calculate path to destination
                    List<String> path = myMap.getShortestPath(myPosition.getLocationId(), dest);
                    if (path != null) {
                        if (!path.isEmpty()) {
                            nextNodeId = path.get(0);
                            myMap.setPlannedItinerary(path);
                        } else {
                            //TODO aller au hasard à côté
                            nextNodeId = accessibleNode.getLocationId();
                        }
                    } else {
                        nextNodeId = myPosition.getLocationId();
                    }
                }
                if (((AbstractDedaleAgent)this.myAgent).moveTo(new gsLocation(nextNodeId))) {
                    System.out.println(this.myAgent.getLocalName() + " -- GatheringStateBeha: moving to " + nextNodeId);
                    myMap.advancePlan();
                    myAgent.setStuck(0);
                } else {
                    myAgent.setStuck(myAgent.getStuck() + 1);
                }
            }
            this.myAgent.setMyMap(myMap);
        }
        @Override
        public int onEnd() {
            if (talk) {
                return 1;
            }
            return 0;
        }
}



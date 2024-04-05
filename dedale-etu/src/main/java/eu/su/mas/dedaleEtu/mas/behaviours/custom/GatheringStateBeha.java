package eu.su.mas.dedaleEtu.mas.behaviours.custom;

import java.util.*;

import eu.su.mas.dedale.env.Location;
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
        private List<String> list_agentNames;
        private ExploreCoopAgentFSM myAgent;


        public GatheringStateBeha (AbstractDedaleAgent myagent, List<String> agentNames) {
            super(myagent);
            myAgent = (ExploreCoopAgentFSM) myagent;
            this.list_agentNames=agentNames;
        }
        @Override
        public void action() {
            System.out.println("GatheringStateBeha");
            this.myMap=myAgent.getMyMap();

            if(this.myMap==null) { // Should never happen but who knows
                this.myMap = new MapRepresentation();
            }

            //0) Retrieve the current position
            Location myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();

            if (myPosition!=null) {
                String nextNodeId=null;

                if (myAgent.getStuck() > MaxStuck) {
                    myAgent.setStuck(0);
                    Edge e = myMap.removeEdge(myPosition.getLocationId(), this.myMap.getShortestPathToClosestOpenNode(myPosition.getLocationId()).get(0));
                    myAgent.addRemovedEdge(e);
                    myMap.clearPlannedItinerary(); //The plan that was calculated is no longer valid, we have to calculate a new one
                }
                if (myMap.getNextNodePlan() != null) {
                    nextNodeId = myMap.getNextNodePlan();
                } else {
                    // Walk to node with the highest stench count
                    String dest = myMap.getStinkiestNode();

                    // Calculate path to destination
                    List<String> path = myMap.getShortestPath(myPosition.getLocationId(), dest);
                    if (path != null) {
                        nextNodeId=path.get(0);
                        myMap.setPlannedItinerary(path);
                    } else {
                        nextNodeId = myPosition.getLocationId();
                    }
                }
                if (((AbstractDedaleAgent)this.myAgent).moveTo(new gsLocation(nextNodeId))) {
                    myMap.advancePlan();
                    myAgent.setStuck(0);
                } else {
                    myAgent.setStuck(myAgent.getStuck() + 1);
                }
            }

            myAgent.setMyMap(myMap);
        }
}

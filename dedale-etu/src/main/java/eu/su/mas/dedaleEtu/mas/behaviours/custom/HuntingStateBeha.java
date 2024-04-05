package eu.su.mas.dedaleEtu.mas.behaviours.custom;

import java.util.*;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.behaviours.OneShotBehaviour;

public class HuntingStateBeha extends OneShotBehaviour {

        private static final long serialVersionUID = 8567689731496787661L;

        private MapRepresentation myMap;
        private List<String> list_agentNames;
        private ExploreCoopAgentFSM myAgent;


        public HuntingStateBeha (AbstractDedaleAgent myagent, List<String> agentNames) {
            super(myagent);
            myAgent = (ExploreCoopAgentFSM) myagent;
            this.list_agentNames=agentNames;
        }
        @Override
        public void action() {
            System.out.println("HuntingStateBeha");
            this.myMap=myAgent.getMyMap();
            if(this.myMap==null) { // Should never happen but who knows
                this.myMap = new MapRepresentation();
            }

            // Walk to node with highest stench count TODO

        }
}

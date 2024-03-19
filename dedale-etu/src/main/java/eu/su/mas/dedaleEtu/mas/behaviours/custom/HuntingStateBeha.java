package eu.su.mas.dedaleEtu.mas.behaviours.custom;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.behaviours.OneShotBehaviour;

public class HuntingStateBeha extends OneShotBehaviour {

        private static final long serialVersionUID = 8567689731496787661L;

        public HuntingStateBeha (AbstractDedaleAgent myagent) {
            super(myagent);
        }
        @Override
        public void action() {
            System.out.println("HuntingStateBeha");
        }
}

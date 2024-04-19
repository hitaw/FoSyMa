package eu.su.mas.dedaleEtu.mas.behaviours.custom;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static eu.su.mas.dedaleEtu.mas.agents.custom.ExploreCoopAgentFSM.MaxTeamDistance;


public class WaitAnswerHuntStateBeha extends OneShotBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;

	private List<String> listReceiver;
	private ExploreCoopAgentFSM myAgent;
	private List<String> team = new ArrayList<String>();
	boolean end = false;

	public WaitAnswerHuntStateBeha(final AbstractDedaleAgent myagent) {
		super(myagent);
		myAgent = (ExploreCoopAgentFSM) myagent;
	}

	@Override
	public void action() {
		/*--------------------------answer diagnostics ------------------------------------*/
		MessageTemplate diagTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol("DIAGNOSTIC"),
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

		ACLMessage diagReceived = this.myAgent.receive(diagTemplate);
		while (diagReceived != null) {
			String pos = diagReceived.getContent();
			if (pos.compareTo(((AbstractDedaleAgent)this.myAgent).getCurrentPosition().getLocationId()) == 0) {
				System.out.println(this.myAgent.getLocalName()+ "-- received diagnostic from "+diagReceived.getSender().getLocalName());
				ACLMessage diagAnswer = new ACLMessage(ACLMessage.CONFIRM);
				diagAnswer.setSender(this.myAgent.getAID());
				diagAnswer.setContent(pos);
				diagAnswer.addReceiver(diagReceived.getSender());
				diagAnswer.setProtocol("DIAGNOSTIC");
				((AbstractDedaleAgent)this.myAgent).sendMessage(diagAnswer);
			}
			diagReceived = this.myAgent.receive(diagTemplate);
		}
		/*-------------------store golemPos-----------------------*/
		MessageTemplate golemFound = MessageTemplate.and(
				MessageTemplate.MatchProtocol("GOLEM"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));

		ACLMessage golemReceived = this.myAgent.receive(golemFound);
		while (golemReceived != null) {
			System.out.println(this.myAgent.getLocalName()+ "-- received golem from "+golemReceived.getSender().getLocalName());
			String golemPos = golemReceived.getContent().split(";")[0];
			Date date = new Date(Long.parseLong(golemReceived.getContent().split(";")[1]));
			if ((myAgent.getGolemDate() == null) || (myAgent.getGolemDate().before(date))) {
				myAgent.setGolemPos(golemPos, date);
				myAgent.setLine(null);
				myAgent.setNextLine(null);
			}
			golemReceived = this.myAgent.receive(golemFound);
		}

		end = false;
		Date expiration = myAgent.getExpiration();

		/*------------------------- map sharing with the other agents -----------------------------*/
		// Deal with agents that didn't finish exploration first
		listReceiver  = new ArrayList<String>();
		//Reception de ping + envoi du yes

		MessageTemplate pingTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol("PING"),
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

		if (expiration.before(new Date())) {
//			System.out.println("Agent "+this.myAgent.getLocalName()+" -- expiration date reached");
			end = true;
		}

		//check if ping received

		ACLMessage pingReceived = this.myAgent.receive(pingTemplate);
		ACLMessage yes = new ACLMessage(ACLMessage.AGREE);
		yes.setProtocol("YES");
		yes.setSender(this.myAgent.getAID());
//		System.out.println("Agent " +this.myAgent.getLocalName() + "-- is looking for ping");
		while (pingReceived != null) {
//			System.out.println("Agent "+this.myAgent.getLocalName()+" -- received ping from "+pingReceived.getSender().getLocalName());
			yes.addReceiver(pingReceived.getSender());
			yes.setContent("Explo done");
			System.out.println(this.myAgent.getLocalName()+ "-- in hunt received ping from "+pingReceived.getSender().getLocalName());
			listReceiver.add(pingReceived.getSender().getLocalName());
			pingReceived = this.myAgent.receive(pingTemplate);
		}
		if (yes.getAllReceiver().hasNext()) {
			((AbstractDedaleAgent)this.myAgent).sendMessage(yes);
		}
		myAgent.setVoisins(listReceiver, 0);

		/*---------------------------------------------- team building -------------------------------------*/
		Pattern p = Pattern.compile("[a-zA-Z]+");
		Matcher m;
		//check if PROPOSE received
		MessageTemplate offerTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol("JOIN"),
				MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));

		// Message to warn our team of new member(s)
		ACLMessage updateTeam = new ACLMessage(ACLMessage.INFORM);
		updateTeam.setProtocol("TEAM");
		updateTeam.setSender(this.myAgent.getAID());

		String destination = null;
		for (String agent : myAgent.getTeam() ){
			destination = (myAgent.getAgentDestination(agent) != null) ? myAgent.getAgentDestination(agent) :
			myAgent.getMyMap().getLastNodePlan();
		}

		boolean update = false; // If there is no offer, we won't send an update to our team members

//		System.out.println("Agent " +this.myAgent.getLocalName() + "-- is looking for offer");
		ACLMessage offerReceived = this.myAgent.receive(offerTemplate);
		while (offerReceived != null) {
			String sender = offerReceived.getSender().getLocalName();
			String content = offerReceived.getContent();

			System.out.println(this.myAgent.getLocalName()+ "-- received offer from "+sender);
			team = myAgent.getTeam();
			// We parse the string received (a list.toString) to extract the agents in the team
			m = p.matcher(content);
			// This should at least contain the sender's name
			while (m.find()) {
				String agent = m.group();
				if (!team.contains(agent)) {
					if (myAgent.getAgentDestination(agent) == null) myAgent.updateAgentPosition(agent,null, destination);
					update = true; // We added an agent to our team
					myAgent.addTeamMember(agent);
					team = myAgent.getTeam();
				}
			}
			offerReceived = this.myAgent.receive(offerTemplate);
		}
		//check if update received
		MessageTemplate updateTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol("TEAM"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));

		ACLMessage updateReceived = this.myAgent.receive(updateTemplate);
		while (updateReceived != null) {
			String content = updateReceived.getContent();
			System.out.println(this.myAgent.getLocalName()+ "-- received update from "+updateReceived.getSender().getLocalName());
			team = myAgent.getTeam();
			// We parse the string received (a list.toString) to extract the agents in the team
			m = p.matcher(content);
			// This should at least contain the sender's name
			while (m.find()) {
				String agent = m.group();
				if (!team.contains(agent)) {
					update = true;
					myAgent.addTeamMember(agent);
					team = myAgent.getTeam();
				}
			}
			updateReceived = this.myAgent.receive(updateTemplate);
		}

		if (update) { // If we added agent(s) to our team, we warn the team, including the agent(s) we just added to warn we accepted their proposal
			for (String agent : team) {
				System.out.println(this.myAgent.getLocalName()+ "-- add receiver to update team -- " + agent);
				if (Objects.equals(agent, this.myAgent.getLocalName())) {
					System.out.println("That's me");
					continue;
				}
				updateTeam.addReceiver(new AID(agent,AID.ISLOCALNAME));
			}
			updateTeam.setContent(team.toString());
			System.out.println(this.myAgent.getLocalName()+ "-- send update to team -- " + team.toString());
			((AbstractDedaleAgent)this.myAgent).sendMessage(updateTeam);
		}
	}

	@Override
	public int onEnd() {
		if (!listReceiver.isEmpty()) {
			return 1;
		}
		if (end) {
			if (team.size() > 1) {
                if (myAgent.getChefName().compareTo(myAgent.getLocalName()) != 0)
					return 3;
                else
					return 4;
            }
			return 2;
		}
		return 0;
	}

}

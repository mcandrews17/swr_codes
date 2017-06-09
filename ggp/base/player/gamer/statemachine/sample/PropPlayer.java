package org.ggp.base.player.gamer.statemachine.sample;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.apps.player.detail.SimpleDetailPanel;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.PropNetStateMachine;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public final class PropPlayer extends SampleGamer
{

	public int gLimit;
	public double bestScoreForDepth;
	public int depthCharges;

	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
//		double start = (double) System.currentTimeMillis();
//		double timer = 0.75 * (double) getMatch().getStartClock() * 1000; //converting sec to millisec
//		double endtime = start + timer;
//
//		MachineState state = getCurrentState();
//
//		limit = bfs(state, endtime) - 1;
//		gLimit = 0;
//		depthCharges = 0;
	}

	public int bfs(MachineState state, double endtime) throws MoveDefinitionException, TransitionDefinitionException
	{
		// citation here
		Queue<MachineState> queue = new LinkedList<MachineState>();
		Set<MachineState> visited = new HashSet<MachineState>();
		queue.add(state);
		visited.add(state);
		StateMachine machine = getStateMachine();
		int depth = 0;
		int newStatesAdded = 1;
		while(!queue.isEmpty()) {
			if ((double) System.currentTimeMillis() > endtime) {
				break;
			}
			Set<MachineState> newStates = new HashSet<MachineState>();
			for (int i = 0; i < newStatesAdded; i++) {
				MachineState front = queue.remove(); //look through all states from this level in this iteration of while loop
				Role currRole = getRole();
				List<Move> actions = machine.getLegalMoves(front, currRole);
				for (Move a : actions) {
					if ((double) System.currentTimeMillis() > endtime) {
						break;
					}
					List<List<Move>> possibleJoint = machine.getLegalJointMoves(front, currRole, a);
					for (List<Move> l : possibleJoint) {
						newStates.add(machine.getNextState(front, l));
					}
				}
			}
			newStatesAdded = 0;
			for (MachineState child : newStates) {
				if (!visited.contains(child)) {
					visited.add(child);
					queue.add(child);
					newStatesAdded++;
				}
			}
			depth++;
		}
		double actualVal = (double) depth / machine.getRoles().size();
		return (int) java.lang.Math.floor(actualVal); //is this right?
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		gLimit = 0;
		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();
		Role role = getRole();
		//List<Move> moves = machine.getLegalMoves(state,  role);
		// Legal player
		//return moves.get(0);

		// Random player
		//int rand = new Random().nextInt(moves.size());
		//return moves.get(rand);

		double timer = ((double) getMatch().getPlayClock() * 1000) - 2500; //converting sec to millisec
		double endtime = System.currentTimeMillis() + timer;
		Move bestMove = null;
//		double overallBestScore = 0;
		double currMaxUtility = 0;
		ArrayList<GameNode> eArray = new ArrayList<GameNode>();
		GameNode currNode = new GameNode(0, 0, state, null, null, eArray, true);
		while (System.currentTimeMillis() <= endtime) {
			List<Move> actions = machine.getLegalMoves(state, role);
			if (actions.size() == 1) {
				bestMove = actions.get(0);
				break;
			}

			if (machine.getRoles().size() == 1) {
				// Compulsive deliberation
				//return cdBestMove(role, state, machine);
				//bestMove = mmabBestMove(role, state, machine, "ab", gLimit, endtime); //used 8 for hunter

				GameNode selectedNode = select(currNode, machine, role, endtime);
				if (!machine.isTerminal(selectedNode.getState())) {
//					System.out.println("not terminal");
					expand(currNode, machine, role, endtime);
				}
				if (selectedNode.getUtility() > currMaxUtility && selectedNode.getAction() != null) {
					currMaxUtility = selectedNode.getUtility();
					bestMove = selectedNode.getAction();
					System.out.print("node utility: ");
					System.out.println(selectedNode.getUtility());
					System.out.print("curr best move: ");
					System.out.println(bestMove);
				}

//				if (bestScoreForDepth >= overallBestScore) {
//					bestMove = newMove;
//					overallBestScore = bestScoreForDepth;
//				}
//				bestScoreForDepth = 0;
			} else {
				// Minimax
				//return mmabBestMove(role, state, machine, "mm", 0);

				// Alpha-Beta
//				Move newMove = mmabBestMove(role, state, machine, "ab", gLimit, endtime);
//				if (bestScoreForDepth >= overallBestScore) {
//					bestMove = newMove;
//					overallBestScore = bestScoreForDepth;
//				}
//				bestScoreForDepth = 0;

				GameNode selectedNode = select(currNode, machine, role, endtime);
				if (!machine.isTerminal(selectedNode.getState())) {
					expand(selectedNode, machine, role, endtime);
				}
				if (selectedNode.getUtility() > currMaxUtility && selectedNode.getAction() != null && selectedNode.isMax()) {
					currMaxUtility = selectedNode.getUtility();
					bestMove = selectedNode.getAction();
				}

				// Fixed-Depth
	//			return mmabBestMove(role, state, machine, "fd", limit);
			}
			if ((double) System.currentTimeMillis() > endtime) {
				break;
			}
			gLimit++;
		}
		if (bestMove == null) {
			List<Move> moves = machine.getLegalMoves(state,  role);
			int rand = new Random().nextInt(moves.size());
			bestMove = moves.get(rand);
		}
//		System.out.print("New move best score: ");
//		System.out.println(currMaxUtility);
//		System.out.println(bestMove);
		System.out.println("Depth charges:");
		System.out.println(depthCharges);
		return bestMove;
	}

	public GameNode select(GameNode currNode, StateMachine machine, Role role, double endtime) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		if (currNode.getVisits() == 0) { return currNode; }
		for (GameNode child : currNode.getChildren()) {
			if (child.getVisits() == 0) { return child; }
		}
		if ((double) System.currentTimeMillis() > endtime) {
			return currNode;
		}
		double score = -999999999;
		GameNode result = currNode;
		for (GameNode child : currNode.getChildren()) {
			double newscore = 0;
			if (child.isMax()) {
				newscore = selectfn(child);
			} else {
				newscore = -1 * selectfn(child);
			}
			if (newscore > score) {
				score = newscore;
				result = child;
			}
		}
		if (!result.isMax()) {
			return select(result, machine, role, endtime);
		} else {
			return result;
		}
	}

	public double selectfn(GameNode currNode) {
		double exploitation = currNode.getUtility()/currNode.getVisits();
		double exploration = 1.5 * Math.sqrt(2*Math.log(currNode.getParent().getVisits())/currNode.getVisits());
		return exploitation + exploration;
	}

	public boolean expand(GameNode selectedNode, StateMachine machine, Role role, double endtime) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		List<Move> actions = machine.getLegalMoves(selectedNode.getState(), role);
		ArrayList<GameNode> eArray = new ArrayList<GameNode>();
		for (int i = 0; i < actions.size(); i++) {
			GameNode oppNode = new GameNode(0, 0, selectedNode.getState(), null, selectedNode, eArray, false);
			List<List<Move>> jointMoves = machine.getLegalJointMoves(selectedNode.getState(), role, actions.get(i));
			for (int j = 0; j < jointMoves.size(); j++) {
				if ((double) System.currentTimeMillis() > endtime) {
					break;
				}
				MachineState newState = machine.getNextState(selectedNode.getState(), jointMoves.get(j));
				if (machine.getRoles().size() == 1) {
					GameNode childNode = new GameNode(0, 0, newState, actions.get(i), selectedNode, eArray, true);
					selectedNode.getChildren().add(childNode);
					double MCresult = montecarlo(role, childNode.getState(), machine, 2, endtime); //increase the # as possible
					backprop(childNode, MCresult);
				} else {
					selectedNode.getChildren().add(oppNode);
					GameNode grandchildNode = new GameNode(0, 0, newState, actions.get(i), oppNode, eArray, true);
					oppNode.getChildren().add(grandchildNode);
					double MCresult = montecarlo(role, grandchildNode.getState(), machine, 3, endtime); //increase the # as possible
					backprop(grandchildNode, MCresult);
					//need to find a way to choose from grandchild nodes, not opp nodes which have null as prev action
				}
			}
		}
		return true;
	}

	public boolean backprop(GameNode currNode, double MCresult) {
		currNode.increaseVisits();
		currNode.setUtility(MCresult);
		if (currNode.getParent() != null) {
			backprop(currNode.getParent(), MCresult);
		}
		return true;
	}

	public Move mmabBestMove(Role role, MachineState state, StateMachine machine, String abbrev, int limit, double endtime) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		List<Move> actions = machine.getLegalMoves(state, role);
		Move action = actions.get(0);
		double score = 0;
		for (int i = 0; i < actions.size(); i++) {
			if ((double) System.currentTimeMillis() > endtime) {
				return action;
			}
			double result = 0;
			if (abbrev.equals("mm")) {
				result = mmMinScore(role, actions.get(i), state, machine);
			} else if (abbrev.equals("ab")){
				result = abMinScore(role, actions.get(i), state, machine, 0.0, 100.0, 0, limit, endtime);
			} else {
				result = fdMinScore(role, actions.get(i), state, machine, 0, limit);
			}
			if (result > score) {
				score = result;
				bestScoreForDepth = result;
				action = actions.get(i);
			}
		}
		return action;
	}

	public double montecarlo(Role role, MachineState state, StateMachine machine, int count, double endtime) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		double total = 0.0;
		for (int i = 0; i < count; i++) {
			depthCharges++;
			total = total + depthcharge(role, state, machine, endtime);
		}
		double ans = total / count;
		if (ans == 100) {
			ans = 99;
		}
		return ans;
	}

	public int depthcharge(Role role, MachineState state, StateMachine machine, double endtime) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		if (machine.isTerminal(state)) {
			return machine.getGoal(state, role);
		}
		if ((double) System.currentTimeMillis() > endtime) {
			return machine.getGoal(state, role);
		}
		List<Move> moves = new ArrayList<Move>();
		List<Role> roles = machine.getRoles();
		for (int i = 0; i < roles.size(); i++) {
			List<Move> options = machine.getLegalMoves(state, roles.get(i));
			int rand = new Random().nextInt(options.size());
			moves.add(options.get(rand));
		}
		MachineState nextState = machine.getNextState(state, moves);
		return depthcharge(role, nextState, machine, endtime);
	}

	public double abMaxScore(Role role, MachineState state, StateMachine machine, double alpha, double beta, int level, int limit, double endtime) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		if (machine.isTerminal(state)) {
			double res = machine.getGoal(state, role);
			return res;
		}
		if (level >= limit) {
			return montecarlo(role, state, machine, 3, endtime);
		}
		if ((double) System.currentTimeMillis() > endtime) {
			return alpha;
		}
		List<Move> actions = machine.getLegalMoves(state, role);
		for (int i = 0; i < actions.size(); i++) {
			double result = abMinScore(role, actions.get(i), state, machine, alpha, beta,level,limit, endtime);
			if (result == 100) {return 100;}
			alpha = Math.max(alpha, result);
			if (alpha >= beta) {return beta;}
			if ((double) System.currentTimeMillis() > endtime) {
				return alpha;
			}
		}
		return alpha;
	}
	public double abMinScore(Role role, Move action, MachineState state, StateMachine machine, double alpha, double beta, int level, int limit, double endtime) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		if ((double) System.currentTimeMillis() > endtime) {
			return beta;
		}
		List<List<Move>> actions = machine.getLegalJointMoves(state, role, action);
		for (int i = 0; i < actions.size(); i++) {
			if ((double) System.currentTimeMillis() > endtime) {
				return beta;
			}
			MachineState newState = machine.getNextState(state, actions.get(i));
			double result = abMaxScore(role, newState, machine, alpha, beta, level + 1, limit, endtime);
			if (result == 0) {return 0;}
			beta = Math.min(beta, result);
			if (beta <= alpha) {return alpha;}
		}
		return beta;
	}
	public Move cdBestMove(Role role, MachineState state, StateMachine machine) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		List<Move> actions = machine.getLegalMoves(state, role);
		Move action = actions.get(0);
		int score = 0;
		for (int i = 0; i < actions.size(); i++) {
			List<Move> currAction = new ArrayList<Move>();
			currAction.add(actions.get(i));
			int result = cdMaxScore(role, machine.getNextState(state, currAction), machine);
			if (result == 100) {return actions.get(i);}
			if (result > score) {
				score = result;
				action = actions.get(i);
			}
		}
		return action;
	}
	public int cdMaxScore(Role role, MachineState state, StateMachine machine) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		if (machine.isTerminal(state)) {
			return machine.getGoal(state, role);
		}
		List<Move> actions = machine.getLegalMoves(state, role);
		int score = 0;
		for (int i = 0; i < actions.size(); i++) {
			List<Move> currAction = new ArrayList<Move>();
			currAction.add(actions.get(i));
			int result = cdMaxScore(role, machine.getNextState(state, currAction), machine);
			if (result > score) {score = result;}
		}
		return score;
	}
	public int mmMaxScore(Role role, MachineState state, StateMachine machine) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		if (machine.isTerminal(state)) {
			return machine.getGoal(state, role);
		}
		List<Move> actions = machine.getLegalMoves(state, role);
		int score = 0;
		for (int i = 0; i < actions.size(); i++) {
			int result = mmMinScore(role, actions.get(i), state, machine);
			if (result > score) {
				score = result;
			}
		}
		return score;
	}
	public int mmMinScore(Role role, Move action, MachineState state, StateMachine machine) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		List<List<Move>> actions = machine.getLegalJointMoves(state, role, action);
		int score = 100;
		for (int i = 0; i < actions.size(); i++) {
			MachineState newState = machine.getNextState(state, actions.get(i));
			int result = mmMaxScore(role, newState, machine);
			if (result < score) {
				score = result;
			}
		}
		return score;
	}
	public int oldabMaxScore(Role role, MachineState state, StateMachine machine, int alpha, int beta) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		if (machine.isTerminal(state)) {
			return machine.getGoal(state, role);
		}
		List<Move> actions = machine.getLegalMoves(state, role);
		for (int i = 0; i < actions.size(); i++) {
			int result = oldabMinScore(role, actions.get(i), state, machine, alpha, beta);
			alpha = Math.max(alpha, result);
			if (alpha >= beta) {return beta;}
		}
		return alpha;
	}
	public int oldabMinScore(Role role, Move action, MachineState state, StateMachine machine, int alpha, int beta) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		List<List<Move>> actions = machine.getLegalJointMoves(state, role, action);
		for (int i = 0; i < actions.size(); i++) {
			MachineState newState = machine.getNextState(state, actions.get(i));
			int result = oldabMaxScore(role, newState, machine, alpha, beta);
			beta = Math.min(beta, result);
			if (beta <= alpha) {return alpha;}
		}
		return beta;
	}
	public double fdMaxScore(Role role, MachineState state, StateMachine machine, int level, double limit) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		if (machine.isTerminal(state)) {
			return machine.getGoal(state, role);
		}
		double score = 0;
		if (level >= limit) { return proximityFn(role, state, machine); }
		List<Move> actions = machine.getLegalMoves(state, role);
		for (int i = 0; i < actions.size(); i++) {
			double result = fdMinScore(role, actions.get(i), state, machine, level, limit);
			if (result == 100) {return 100;}
			if (result > score) {score = result;}
		}
		return score;
	}
	public double fdMinScore(Role role, Move action, MachineState state, StateMachine machine, int level, double limit) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		List<List<Move>> actions = machine.getLegalJointMoves(state, role, action);
		double score = 100;
		for (int i = 0; i < actions.size(); i++) {
			MachineState newState = machine.getNextState(state, actions.get(i));
			double result = fdMaxScore(role, newState, machine, level + 1, limit);
			if (result == 0) {return 0;}
			if (result < score) {score = result;}
		}
		return score;
	}
	public double mobilityFn(Role role, MachineState state, StateMachine machine) throws MoveDefinitionException, TransitionDefinitionException {
		List<Move> actions = machine.getLegalMoves(state, role);
		HashSet<MachineState> possibleStates = new HashSet<MachineState>();
		for (Move a : actions) {
			List<List<Move>> possibleJoint = machine.getLegalJointMoves(state, role, a);
			for (List<Move> l : possibleJoint) {
				possibleStates.add(machine.getNextState(state, l));
			}
		}
		System.out.println(possibleStates.size());
		return possibleStates.size();
	}

	public double proximityFn(Role role, MachineState state, StateMachine machine) throws GoalDefinitionException {
		return machine.getGoal(state, role);
	}

	//need to pass in opponent not role
	public double opponentFn(Role role, MachineState state, StateMachine machine) throws MoveDefinitionException, TransitionDefinitionException {
		if (!role.equals(getRole())) {
			return 0;
		}
		List<Move> actions = machine.getLegalMoves(state, role);
		int maxTotalActions = 0;
		int minTotalActions = 9999999;
		for (Move a : actions) {
			List<List<Move>> possibleJoint = machine.getLegalJointMoves(state, role, a);
			MachineState nextState = machine.getNextState(state, possibleJoint.get(0));
			List<Role> opponents = machine.getRoles();
			int totalOpponentActions = 0;
			for (Role opponent : opponents) {
				if (!opponent.equals(role)) {
					totalOpponentActions += machine.getLegalMoves(nextState, opponent).size();
				}
			}
			if (totalOpponentActions < minTotalActions) {
				minTotalActions = totalOpponentActions;
			}
			if (totalOpponentActions > maxTotalActions) {
				maxTotalActions = totalOpponentActions;
			}
		}
		System.out.println("minTotal here:");
		System.out.println(minTotalActions);
		System.out.println("totalActions here:");
		System.out.println(maxTotalActions);
		System.out.println(100.0 - ((double) minTotalActions / maxTotalActions * 100.0));
		return 100.0 - ((double) minTotalActions / maxTotalActions * 100.0);
	}

	public double weightedFn(Role role, MachineState state, StateMachine machine) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		double wM = 0.4;
		double wP = 0.9;
		double wO = 0.1;
		return wM*mobilityFn(role,state,machine) + wP*proximityFn(role, state, machine) + wO*opponentFn(role, state,machine);
	}

	/** This will currently return "SampleGamer"
	 * If you are working on : public abstract class MyGamer extends SampleGamer
	 * Then this function would return "MyGamer"
	 */
	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	// This is the default State Machine
	@Override
	public StateMachine getInitialStateMachine() {
		return new PropNetStateMachine(); //and change all the method names above to the ones in PNSM.java
	}

	// This is the default Sample Panel
	@Override
	public DetailPanel getDetailPanel() {
		return new SimpleDetailPanel();
	}



	@Override
	public void stateMachineStop() {
		// Sample gamers do no special cleanup when the match ends normally.
	}

	@Override
	public void stateMachineAbort() {
		// Sample gamers do no special cleanup when the match ends abruptly.
	}

	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {
		// Sample gamers do no game previewing.
	}
}
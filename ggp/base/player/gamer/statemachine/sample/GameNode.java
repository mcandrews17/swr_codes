package org.ggp.base.player.gamer.statemachine.sample;

import java.util.ArrayList;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;

public class GameNode {
	private double utility;
	private int visits;
	private MachineState state;
	private Move action;
	private GameNode parent;
	private ArrayList<GameNode> children;
	private boolean isMax;

	public GameNode(double utility, int visits, MachineState state, Move action, GameNode parent, ArrayList<GameNode> children, boolean isMax){
		this.utility = utility;
		this.visits = visits;
		this.state = state;
		this.action = action;
		this.parent = parent;
		this.children = children;
		this.isMax = isMax;
	}

	public void setUtility(double addition){
		this.utility = this.utility + addition;
	}

	public void increaseVisits(){
		this.visits++;
	}

	public void setChildren(GameNode child){
		this.children.add(child);
	}

	public double getUtility(){
		return utility;
	}

	public int getVisits(){
		return visits;
	}

	public MachineState getState(){
		return state;
	}

	public Move getAction() {
		return action;
	}

	public GameNode getParent(){
		return parent;
	}

	public ArrayList<GameNode> getChildren() {
		return children;
	}

	public boolean isMax() {
		return isMax;
	}
}

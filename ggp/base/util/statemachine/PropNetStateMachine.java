package org.ggp.base.util.statemachine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.And;
import org.ggp.base.util.propnet.architecture.components.Constant;
import org.ggp.base.util.propnet.architecture.components.Not;
import org.ggp.base.util.propnet.architecture.components.Or;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.architecture.components.Transition;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;



public class PropNetStateMachine extends StateMachine {
    /** The underlying proposition network  */
    private PropNet propNet;
    /** The topological ordering of the propositions */
    private List<Proposition> ordering;
    /** The player roles */
    private List<Role> roles;

    /**
     * Initializes the PropNetStateMachine. You should compute the topological
     * ordering here. Additionally you may compute the initial state here, at
     * your discretion.
     */
    @Override
    public void initialize(List<Gdl> description) {
        try {
			propNet = OptimizingPropNetFactory.create(description);
	        roles = propNet.getRoles();
	        ordering = getOrdering();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
        propNet.renderToFile("twayne.dot");
        System.out.println("rendered");
    }

    public boolean markBases(MachineState currState) {
    	Set<GdlSentence> contents = currState.getContents();
    	Map<GdlSentence, Proposition> props = propNet.getBasePropositions();
    	for (GdlSentence p : contents) {
    		if (props.keySet().contains(p)) {
    			props.get(p).setValue(true);
    		} else if (p.equals(propNet.getInitProposition().getName())) {
    			propNet.getInitProposition().setValue(true);
    		}
    	}
    	return true;
    }

    public boolean markActions(List<Move> actions) {
    	List<GdlSentence> actionsinGdl = toDoes(actions);
    	Map<GdlSentence, Proposition> props = propNet.getInputPropositions();
    	for (GdlSentence s : actionsinGdl) {
    		props.get(s).setValue(true);
    	}
    	return true;
    }

    public boolean clearPropNet() {
    	propNet.getInitProposition().setValue(false);
    	Map<GdlSentence, Proposition> bases = propNet.getBasePropositions();
    	for (GdlSentence b : bases.keySet()) {
    		bases.get(b).setValue(false);;
    	}
    	Map<GdlSentence, Proposition> inputs = propNet.getInputPropositions();
    	for (GdlSentence i : inputs.keySet()) {
    		inputs.get(i).setValue(false);
    	}
    	return true;
    }

    public boolean propmarkp(Component c) {
    	Map<GdlSentence, Proposition> baseProps = propNet.getBasePropositions();
    	Map<GdlSentence, Proposition> inputProps = propNet.getInputPropositions();
    	Set<Proposition> allProps = propNet.getPropositions();

    	if (baseProps.values().contains(c) || inputProps.values().contains(c)){
    		return c.getValue();

    	} else if (c.equals(propNet.getInitProposition())) {
    		return propNet.getInitProposition().getValue();

    	} else if (c instanceof And) {
    		return propmarkconjunction(c);
    	} else if (c instanceof Or) {
    		return propmarkdisjunction(c);
    	} else if (c instanceof Not) {
    		return propmarknegation(c);
    	} else if (c instanceof Constant) {
    		return c.getValue();
    	} else if (c instanceof Transition) {
    		return propmarkp(c.getSingleInput());

    	} else if (allProps.contains(c)) {
    		return propmarkp(c.getSingleInput());
    	}
    	return false;
    }

    public boolean propmarknegation(Component c) {
    	return !propmarkp(c.getSingleInput());
    }

    public boolean propmarkconjunction(Component c) {
    	Set<Component> sources = c.getInputs();
    	for (Component s: sources) {
    		if (!propmarkp(s)) { return false; }
    	}
    	return true;
    }

    public boolean propmarkdisjunction(Component c) {
    	Set<Component> sources = c.getInputs();
    	for (Component s: sources) {
    		if (propmarkp(s)) { return true; }
    	}
    	return false;
    }

	/**
	 * Computes if the state is terminal. Should return the value
	 * of the terminal proposition for the state.
	 */
	@Override
	public boolean isTerminal(MachineState state) {
		clearPropNet();
		markBases(state);
		return propmarkp(propNet.getTerminalProposition());
	}

	/**
	 * Computes the goal for a role in the current state.
	 * Should return the value of the goal proposition that
	 * is true for that role. If there is not exactly one goal
	 * proposition true for that role, then you should throw a
	 * GoalDefinitionException because the goal is ill-defined.
	 */

	//propmarkp from goal nodes
	@Override
	public int getGoal(MachineState state, Role role)
	throws GoalDefinitionException {
		clearPropNet();
		markBases(state);
//		propNet.renderToFile("hunter");
		Set<Proposition> rewards = propNet.getGoalPropositions().get(role);
		for (Proposition reward : rewards) {
			if (propmarkp(reward)) {
//				System.out.println(reward);
//				System.out.print("here is goal value: ");
//				System.out.println(getGoalValue(reward));
				return getGoalValue(reward);
			}
		}
		return 0;
	}

	/**
	 * Returns the initial state. The initial state can be computed
	 * by only setting the truth value of the INIT proposition to true,
	 * and then computing the resulting state.
	 */
	@Override
	public MachineState getInitialState() {
		clearPropNet();

		propNet.getInitProposition().setValue(true);
		Set<GdlSentence> initState = new HashSet<GdlSentence>();

		Map<GdlSentence, Proposition> baseProps = propNet.getBasePropositions();
		for (GdlSentence sent : baseProps.keySet()) {
			if (propmarkp(baseProps.get(sent).getSingleInput())) {
				initState.add(sent);
			}
		}

		propNet.getInitProposition().setValue(false);
		return new MachineState(initState);
	}

	/**
	 * Computes the legal moves for role in state.
	 */
	@Override
	public List<Move> getLegalMoves(MachineState state, Role role)
	throws MoveDefinitionException {
		clearPropNet();
		markBases(state);
		List<Role> roles = propNet.getRoles();
		Set<Proposition> legals = new HashSet<Proposition>();
		for (int i = 0; i < roles.size(); i++) {
			if (role.equals(roles.get(i))) {
				legals = propNet.getLegalPropositions().get(role);
				break;
			}
		}
		List<Move> actions = new ArrayList<Move>();
		for (Proposition l : legals) {
			boolean testhere = propmarkp(l);
			if (testhere) {
				actions.add(getMoveFromProposition(l));
			}
		}
		return actions;
	}

	/**
	 * Computes the next state given state and the list of moves.
	 */
	@Override
	public MachineState getNextState(MachineState state, List<Move> moves)
	throws TransitionDefinitionException {
		clearPropNet();
		markActions(moves);
		markBases(state);
		Map<GdlSentence, Proposition> bases = propNet.getBasePropositions();
		//get from move to legal proposition
		List<GdlSentence> chosenLegals = toDoes(moves);
		Set<GdlSentence> nexts = new HashSet<GdlSentence>();
		Map<Proposition, Proposition> limap = propNet.getLegalInputMap();
		for (GdlSentence s : bases.keySet()) {
			if (propmarkp(bases.get(s).getSingleInput().getSingleInput())) { //you only deal with true props here
				nexts.add(bases.get(s).getName());
			}
		}
		for (GdlSentence s : chosenLegals) {
			for (Set<Proposition> roleOptions : propNet.getLegalPropositions().values()) {
				for (Proposition option : roleOptions) {
					if (option.getName().equals(s)) {
						System.out.println("HERE");
						Proposition inputProp = limap.get(option);
						nexts.add(inputProp.getName());
						break;
					}
				}
			}
		}
		return new MachineState(nexts);
	}

	/**
	 * This should compute the topological ordering of propositions.
	 * Each component is either a proposition, logical gate, or transition.
	 * Logical gates and transitions only have propositions as inputs.
	 *
	 * The base propositions and input propositions should always be exempt
	 * from this ordering.
	 *
	 * The base propositions values are set from the MachineState that
	 * operations are performed on and the input propositions are set from
	 * the Moves that operations are performed on as well (if any).
	 *
	 * @return The order in which the truth values of propositions need to be set.
	 */
	public List<Proposition> getOrdering()
	{
	    // List to contain the topological ordering.
	    List<Proposition> order = new LinkedList<Proposition>();

		// All of the components in the PropNet
		List<Component> components = new ArrayList<Component>(propNet.getComponents());

		// All of the propositions in the PropNet.
		List<Proposition> propositions = new ArrayList<Proposition>(propNet.getPropositions());

	    // TODO: Compute the topological ordering.

		return order;
	}

	/* Already implemented for you */
	@Override
	public List<Role> getRoles() {
		return roles;
	}

	/* Helper methods */

	/**
	 * The Input propositions are indexed by (does ?player ?action).
	 *
	 * This translates a list of Moves (backed by a sentence that is simply ?action)
	 * into GdlSentences that can be used to get Propositions from inputPropositions.
	 * and accordingly set their values etc.  This is a naive implementation when coupled with
	 * setting input values, feel free to change this for a more efficient implementation.
	 *
	 * @param moves
	 * @return
	 */
	private List<GdlSentence> toDoes(List<Move> moves)
	{
		List<GdlSentence> doeses = new ArrayList<GdlSentence>(moves.size());
		Map<Role, Integer> roleIndices = getRoleIndices();

		for (int i = 0; i < roles.size(); i++)
		{
			int index = roleIndices.get(roles.get(i));
			doeses.add(ProverQueryBuilder.toDoes(roles.get(i), moves.get(index)));
		}
		return doeses;
	}

	/**
	 * Takes in a Legal Proposition and returns the appropriate corresponding Move
	 * @param p
	 * @return a PropNetMove
	 */
	public static Move getMoveFromProposition(Proposition p)
	{
		return new Move(p.getName().get(1));
	}

	/**
	 * Helper method for parsing the value of a goal proposition
	 * @param goalProposition
	 * @return the integer value of the goal proposition
	 */
    private int getGoalValue(Proposition goalProposition)
	{
		GdlRelation relation = (GdlRelation) goalProposition.getName();
		GdlConstant constant = (GdlConstant) relation.get(1);
		return Integer.parseInt(constant.toString());
	}

	/**
	 * A Naive implementation that computes a PropNetMachineState
	 * from the true BasePropositions.  This is correct but slower than more advanced implementations
	 * You need not use this method!
	 * @return PropNetMachineState
	 */
	public MachineState getStateFromBase()
	{
		Set<GdlSentence> contents = new HashSet<GdlSentence>();
		for (Proposition p : propNet.getBasePropositions().values())
		{
			p.setValue(p.getSingleInput().getValue());
			if (p.getValue())
			{
				contents.add(p.getName());
			}

		}
		return new MachineState(contents);
	}
}
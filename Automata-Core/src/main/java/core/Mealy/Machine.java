package core.Mealy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Machine {
	private List<State> states;
	private String name;
	private Set<Character> iAlphabet, oAlphabet;
	private State currState;

	public Machine(String name) {
		this.states = new ArrayList<State>();
		this.name = name;
		this.currState = null;
	}

	public Machine(String name, Set<Character> iAlphabet, Set<Character> oAlphabet) throws MachineExpection {
		this.name = name;
		this.states = new ArrayList<State>();
		this.currState = null;
		this.init(iAlphabet, oAlphabet);
	}

	public List<State> getStates() {
		return states;
	}

	public State getCurrState() {
		return currState;
	}

	public void addState() {
		this.states.add(new State());
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setiAlphabet(Set<Character> iAlphabet) {
		this.iAlphabet = iAlphabet;
	}

	public void setoAlphabet(Set<Character> oAlphabet) {
		this.oAlphabet = oAlphabet;
	}

	public void setCurrState(State currState) {
		this.currState = currState;
	}

	public Set<Character> getiAlphabet() {
		return iAlphabet;
	}

	public Set<Character> getoAlphabet() {
		return oAlphabet;
	}

	public void init(Set<Character> iAlphabet, Set<Character> oAlphabet) throws MachineExpection {
		if (iAlphabet.size() != oAlphabet.size())
			throw new MachineExpection("Input and Output Alphabets must contain the same amount of letters!");
		this.iAlphabet = iAlphabet;
		this.oAlphabet = oAlphabet;

		List<Character> inputAlphabet = new ArrayList<Character>(iAlphabet);
		List<Character> outputAlphabet = new ArrayList<Character>(oAlphabet);
		List<Integer> range = IntStream.range(0, iAlphabet.size()).boxed().collect(Collectors.toList());

		for (int i = 0; i < iAlphabet.size(); i++)
			this.addState();

		for (State currState : this.states) {
			Collections.shuffle(outputAlphabet);
			Collections.shuffle(range);

			for (int i = 0; i < iAlphabet.size(); i++) {
				currState.addTranslation(
						new Translation(inputAlphabet.get(i), outputAlphabet.get(i), this.states.get(range.get(i))));
			}
		}

		this.currState = this.states.get(0);

	}

	public boolean isValid() {
		if (this.iAlphabet.size() != this.oAlphabet.size() || this.currState == null)
			return false;

		Set<Integer> compareRange = IntStream.range(0, this.iAlphabet.size()).boxed().collect(Collectors.toSet());

		Set<Character> checkIAlphabet = new HashSet<Character>();
		Set<Character> checkOAlphabet = new HashSet<Character>();
		Set<Integer> checkRange = new HashSet<Integer>();

		for (State currState : this.states) {
			if (currState.getTranslations().size() != this.states.size())
				return false;

			checkIAlphabet.clear();
			checkOAlphabet.clear();
			checkRange.clear();

			for (Translation currTranslation : currState.getTranslations()) {
				checkIAlphabet.add(currTranslation.getInput());
				checkOAlphabet.add(currTranslation.getOutput());
				checkRange.add(this.states.indexOf(currTranslation.getTarget()));
			}

			if (!checkIAlphabet.equals(this.iAlphabet) || !checkOAlphabet.equals(this.oAlphabet)
					|| !checkRange.equals(compareRange))
				return false;

		}

		return true;
	}

	public Character step(Character input, boolean encoding) {
		if (encoding) {
			for (Translation currTranslation : this.currState.getTranslations()) {
				if (currTranslation.getInput().equals(input)) {
					this.currState = currTranslation.getTarget();
					return currTranslation.getOutput();
				}
			}
		} else {
			for (Translation currTranslation : this.currState.getTranslations()) {
				if (currTranslation.getOutput().equals(input)) {
					this.currState = currTranslation.getTarget();
					return currTranslation.getInput();
				}
			}
		}
		return null;
	}

	public String encode(String input) {
		String output = new String();
		for (int i = 0; i < input.length(); i++) {
			output += this.step(input.charAt(i), true);
		}

		this.currState = this.states.get(0);

		return output;
	}

	public String decode(String input) {
		String output = new String();
		for (int i = 0; i < input.length(); i++) {
			output += this.step(input.charAt(i), false);
		}

		this.currState = this.states.get(0);

		return output;

	}

	public void processData(String data) throws MachineExpection {
		Set<Character> base = new HashSet<Character>();
		for (int i = 0; i < data.length(); i++) {
			base.add(data.charAt(i));
		}
		this.init(base, base);
	}

	public core.Moore.Machine toMoore() {
		core.Moore.Machine m = new core.Moore.Machine(this.name);
		m.setiAlphabet(new HashSet<Character>(this.iAlphabet));
		m.setoAlphabet(new HashSet<Character>(this.oAlphabet));

		Map<State, Set<Character>> stateDistributor = new HashMap<State, Set<Character>>();
		for (State currState : this.states) {
			stateDistributor.put(currState, new HashSet<Character>());
		}

		for (State currState : this.states) {
			for (Translation currTranslation : currState.getTranslations()) {
				stateDistributor.get(currTranslation.getTarget()).add(currTranslation.getOutput());
			}
		}

		Map<State, List<core.Moore.State>> symbolDistributor = new HashMap<State, List<core.Moore.State>>();

		for (State currState : stateDistributor.keySet()) {
			symbolDistributor.put(currState, new ArrayList<core.Moore.State>());
			for (Character currChar : stateDistributor.get(currState)) {
				m.addState(currChar);
				symbolDistributor.get(currState).add(m.getStates().get(m.getStates().size() - 1));
			}
		}

		for (State currState : this.states) {
			for (Translation currTranslation : currState.getTranslations()) {
				core.Moore.State target = null;
				for (core.Moore.State currMooreState : symbolDistributor.get(currTranslation.getTarget())) {
					if (currMooreState.getOutput().equals(currTranslation.getOutput())) {
						target = currMooreState;
						break;
					}
				}
				for (core.Moore.State currMooreState : symbolDistributor.get(currState)) {
					currMooreState.addTranslation(new core.Moore.Translation(currTranslation.getInput(), target));
				}
			}
		}

		// Map<State, Map<Character, core.Moore.State>> translationDistributor =
		// new HashMap<State, Map<Character, core.Moore.State>>();

		return m;
	}

	@Override
	public String toString() {
		String output = new String();
		output += "Machine: " + this.name + "\n";
		for (State currState : this.states) {
			output += "---State " + this.states.indexOf(currState) + "---\n";
			for (Translation currTranslation : currState.getTranslations()) {
				output += "[ " + currTranslation.getInput() + " ---> " + currTranslation.getOutput() + " / q"
						+ this.states.indexOf(currTranslation.getTarget()) + " ]\n";
			}
		}
		return output;
	}

}

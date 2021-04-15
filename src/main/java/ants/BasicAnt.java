package ants;

import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.MessageContents;
import com.github.rinde.rinsim.core.model.time.TimeLapse;

import agents.AGVAgent;
import agents.BasicAgent;

public abstract class BasicAnt implements MessageContents{

	public final int id;
	public final AGVAgent agent;
	public boolean isReturning;
	protected static int IDCounter = 1;
	public BasicAgent sendBy;
	public boolean sendDependingAnts = true;

	public BasicAnt(AGVAgent agent) {
		this.id = getNextId();
		this.agent = agent;
		this.sendBy = agent;
	}

	public BasicAnt(int pid, AGVAgent agent) {
		this.id = pid;
		this.sendBy = agent;
		this.agent = agent;
	}

	public abstract BasicAnt copy();

	public abstract boolean canContinue();
	
	private int getNextId() {
		return IDCounter++;
	}

	public CommUser getAgent() {
		return this.agent;
	}

	public abstract void handleAnt(BasicAgent cm, TimeLapse timeLapse);


	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (!(o instanceof BasicAnt))
			return false;
		if (((BasicAnt) o).id == this.id)
			return true;
		return false;
	}
	
}

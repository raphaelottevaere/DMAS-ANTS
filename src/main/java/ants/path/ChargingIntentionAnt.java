package ants.path;

import java.util.List;

import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;

import agents.AGVAgent;
import agents.BasicAgent;
import reservations.ChargingOptions;
import reservations.RouteReservation;

public class ChargingIntentionAnt extends IntentionAnt {
	
	private ChargingOptions chargingOptions;

	//An intention ant describes a basic intention of the agent
	//Here the intention if to find a chargingStation
	public ChargingIntentionAnt(List<Point> path, AGVAgent agent, Point endpoint, List<RouteReservation> r, ChargingOptions co) {
		super(path, agent,endpoint, r);
		this.chargingOptions=co;
	}

	public ChargingIntentionAnt(List<Point> pathCopy,AGVAgent agent,  Point endPoint,
			List<RouteReservation> reservationsCopy, ChargingOptions pchargingOptions, int id) {
		super(pathCopy, agent, endPoint, reservationsCopy,id);
		this.chargingOptions=pchargingOptions;
	}

	@Override
	public PathAnt copy() {
		ChargingIntentionAnt ant = new ChargingIntentionAnt(pathCopy(),agent,endPoint,reservationsCopy(),chargingOptions, id);
		ant.isReturning=this.isReturning;
		ant.sendBy=this.sendBy;
		return ant;
	}
	
	public boolean getAccepted() {
		return accepted;
	}
	
	public void setAccepted(boolean b) {
		this.accepted=b;
	}
	 
	public ChargingOptions getchargingOptions() {
		return this.chargingOptions;
	}

	@Override
	public void handleAnt(BasicAgent cm, TimeLapse timeLapse) {
		cm.handleChargingIntentionAnt(this, timeLapse);
	}
	
}

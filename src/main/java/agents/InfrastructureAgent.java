package agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math3.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;

import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.Message;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.rand.RandomProvider;
import com.github.rinde.rinsim.core.model.road.DynamicGraphRoadModelImpl;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import ants.*;
import ants.path.ChargingExplorationAnt;
import ants.path.ChargingIntentionAnt;
import ants.path.PathAnt;
import ants.path.PathExplorationAnt;
import ants.path.PathIntentionAnt;
import ants.path.TaskExplorationAnt;
import ants.path.TaskIntentionAnt;
import comperators.ReservationComparator;
import comperators.RouteComperator;
import graphs.AStar;
import models.DARModel;
import reservations.DeadLockRouteReservation;
import reservations.RouteReservation;
import simulator.SimulationSettings;

import simulator.SimulationException;

/**
 * A basic infrastructure agent Main job is ANT handling and correctly
 * processing the incoming AGV Agents
 * 
 * @author rapha
 *
 */
public class InfrastructureAgent implements BasicAgent, RoadUser {

	protected CommDevice commDevice;
	public Point position;
	protected RoadModel roadModel;
	protected RandomGenerator rng;
	protected ListenableGraph<LengthData> staticGraph;
	protected PDPModel pdpModel;
	protected DARModel darModel;
	public List<RouteReservation> routeReservations = new LinkedList<RouteReservation>();

	public List<PathAnt> outGoingAnts = new ArrayList<PathAnt>();
	protected List<PathAnt> incomingAnts = new ArrayList<PathAnt>();
	protected Map<Integer, PathAnt> dependantAnts = new HashMap<Integer, PathAnt>();
	// ID followed by time to just send the ant back
	protected Map<Integer, Integer> sendTimeDependantAnts = new HashMap<Integer, Integer>();

	// Do i want to keep track of neighbours?
	protected Set<BasicAgent> neighbours = new HashSet<BasicAgent>();
	protected long robotTimePerHop;
	protected int intentionReservationLifetime;
	protected long tickLength;
	public boolean verbose;
	protected boolean trie = true;
	private Collection<Point> outgoingNodes;
	protected int waitingForDependant = (int) SimulationSettings.WAITING_ON_DEPENDANT_ANTS / 2;

	public InfrastructureAgent(Point position, int nodeDistance, double agvSpeed, long tickLength, boolean verbose,
			ListenableGraph<LengthData> staticGraph, DARModel darModel, RandomGenerator rng) {
		this.position = position;
		this.intentionReservationLifetime = SimulationSettings.INTENTION_RESERVATION_LIFETIME;
		// distance in m, speed in m/s, travel time between two nodes = (distance
		// /speed)
		// Maybe I can force this is some way so that agv just consrantly jumps from
		// node to node with a wait time
		this.robotTimePerHop = (long) ((nodeDistance / SimulationSettings.AGV_SPEED)
				+ SimulationSettings.Extra_time_per_HOP) * tickLength;
		this.tickLength = tickLength;
		this.staticGraph = staticGraph;
		this.verbose = verbose;
		this.darModel = darModel;
		this.outgoingNodes = staticGraph.getOutgoingConnections(position);
		this.rng = rng;
	}

	/*******************
	 * Main functions *
	 *******************/

	final public void tick(@NotNull TimeLapse timeLapse) {
		if (trie) {
			try {
				InfrastructureAgent ia;
				for (Point temp : staticGraph.getIncomingConnections(this.position)) {
					ia = darModel.getAgentAt(temp);
					this.commDevice.send(Messages.NICE_TO_MEET_YOU, ia);
					neighbours.add(ia);
				}

				for (Point temp : staticGraph.getOutgoingConnections(this.position)) {
					ia = darModel.getAgentAt(temp);
					this.commDevice.send(Messages.NICE_TO_MEET_YOU, ia);
					neighbours.add(ia);
				}
				trie = false;
				return;
			} catch (Exception e) {
				e.printStackTrace();
				new SimulationException("Failed to send NICE_TO_MEET_YOU").printStackTrace();
			}
		}
		this.readMessages(timeLapse);
		this.checkDependantAnts();
		this.sendOutgoingAnts(timeLapse);
	}

	protected void readMessages(TimeLapse timeLapse) {
		ImmutableList<Message> incoming = commDevice.getUnreadMessages();
		if(incoming.size()>100) {
			System.out.print("");
		}
		for (Message m : incoming) {
			if (m.getContents() instanceof BasicAnt) {
				BasicAnt ba = (BasicAnt) m.getContents();
				ba.handleAnt(this, timeLapse);
			} else if (m.getContents() == Messages.NICE_TO_MEET_YOU) {
				if (!neighbours.contains((BasicAgent) m.getSender())) {
					neighbours.add((BasicAgent) m.getSender());
				}
			}
		}
	}

	public void afterTick(@NotNull TimeLapse timeLapse) {
		// Remove all routeReservations that are older then the current time
		// Remove all routeReservations where the pheromones ran out
		List<RouteReservation> toRemove = new ArrayList<RouteReservation>();
		try {
			if(routeReservations.size()>100)
				System.out.print("");
			for (RouteReservation rr : routeReservations) {
				if (rr.getEndTime() < timeLapse.getStartTime()) {
					if (verbose)
						System.err.println("[REMOVED RESERVATION INFO] Removed reservations: " + rr
								+ ": Due to endTime is past now, " + timeLapse.getTime());

					toRemove.add(rr);

				} else if (rr.evaporates < 0) {
					if (!rr.agv.getPosition().get().equals(this.position)) {
						if (verbose)
						System.err
								.println("[REMOVED RESERVATION] Removed reservations: " + rr + ": Due to evaportation");
						toRemove.add(rr);
					} else
						rr.resetEvaporation();
				} else
					rr.evaporates -= 1;
			}

			for (RouteReservation rr : toRemove) {
				if (rr.agv.getPosition().get().equals(this.position)) {
					this.extendReservation(timeLapse, 10, rr.agv);
					rr.agv.resetReservation = true;
				} else
					routeReservations.remove(rr);
			}
			toRemove.clear();
		} catch (ConcurrentModificationException e) {
			e.printStackTrace();
		}
	}

	public void firstReservation(AGVAgent agv, long currentTime) {
		routeReservations.add(new RouteReservation(agv, this.position, currentTime,
				SimulationSettings.WAITING_ON_DEPENDANT_ANTS * SimulationSettings.TICK_LENGTH, null, this));
		routeReservations.sort(new ReservationComparator());
	}

	public void extendReservationTo(TimeLapse time, long endTime, AGVAgent agv) {
		if (routeReservations.size() == 0) {
			if (verbose)
				new SimulationException("[ERROR] trying to extend non-existing reservation").printStackTrace();

			// AFTER AGV EXTENDS A NON EXCISTING RESERVATION IT GETS COMPLETLY STUCK
			// NEXT
			routeReservations.add(new RouteReservation(agv, position, time.getTime(), endTime, position, this));
		}

		RouteReservation rr = routeReservations.get(0);
		if (rr.agv == agv) {
			// needs to modify all reservations that are later then this
			// Needs to send delay reservations to the next stop of all modified
			// RouteReservation
			long tempTime = endTime;
			rr.resetEvaporation();

			if (!(tempTime > rr.endTime)) {
				return;
			}
			rr.endTime = tempTime;
			if (routeReservations.size() == 1)
				return;

			List<RouteReservation> toRemove = new ArrayList<RouteReservation>();
			for (RouteReservation reserv : routeReservations) {
				if (reserv.getBeginTime() < rr.endTime && !(reserv.equals(rr))) {
					toRemove.add(reserv);
				}
			}
			if (!toRemove.isEmpty()) {
				routeReservations.removeAll(toRemove);
				routeReservations.sort(new RouteComperator());

				for (RouteReservation RR : toRemove) {
					if (!RR.agv.equals(agv)) {
						RR.agv.reservationExtendedReSearchNeeded(time);
					}
				}

			}
		}
	}

	public void extendReservationTime(@NotNull TimeLapse time, long timeExtended, AGVAgent agv) {
		if (routeReservations.size() == 0) {
			if (verbose)
				new SimulationException("[ERROR] trying to extend non-existing reservation").printStackTrace();

			// BUGG
			// AFTER AGV EXTENDS A NON EXCISTING RESERVATION IT GETS COMPLETLY STUCK
			// NEXT
			routeReservations.add(
					new RouteReservation(agv, position, time.getTime(), time.getTime() + timeExtended, position, this));
		}

		RouteReservation rr = routeReservations.get(0);
		if (rr.agv == agv) {
			// needs to modify all reservations that are later then this
			// Needs to send delay reservations to the next stop of all modified
			// RouteReservation
			long tempTime = time.getTime() + timeExtended;
			rr.resetEvaporation();

			if (!(tempTime > rr.endTime)) {
				return;
			}
			rr.endTime = tempTime;
			
			if(agv.ID%1000 == 18)
				System.out.print("");

			// for some reasons some reservations are added twice
			if (routeReservations.size() > 1) {
				List<RouteReservation> toRemove = new ArrayList<RouteReservation>();
				for (RouteReservation reserv : routeReservations) {
					if (reserv.getBeginTime() < rr.endTime && !(reserv.equals(rr))) {
						toRemove.add(reserv);
					}
				}
				if (!toRemove.isEmpty()) {
					routeReservations.removeAll(toRemove);
					routeReservations.sort(new RouteComperator());

					Set<Integer > ids = new HashSet<Integer>();
					for (RouteReservation RR : toRemove) {
						if (!RR.agv.equals(agv) && !ids.contains(RR.agv.ID)) {
							ids.add(RR.agv.ID);
							RR.agv.reservationExtendedReSearchNeeded(time);                                                      
						}
					}
				}
			}
		}
	}

	public void extendReservation(@NotNull TimeLapse time, long timeExtended, AGVAgent agv) {
		long tempTime = timeExtended * robotTimePerHop;
		extendReservationTime(time, tempTime, agv);
	}

	// included maximum endtime When a agv needs to be moved
	// BV doing this ants can be send right back to the sender instead of over the
	// network
	// VOLGENDE Het gaat hier mis bij meerdere AGVS
	protected RouteReservation findNextPotentialRouteReservation(PathAnt ba, TimeLapse timeLapse) {
		RouteReservation rr = ba.getLastReservation();
		try {
			if (rr == null) {
				// Something terrible happened here
				if (ba.reservations.size() != 0) {
					new SimulationException(
							"A reservation couldnt be found somewhere, Infrastucture agent has no reservation of agent in ant: "
									+ ba).printStackTrace();
					return null;
				}

				Point exit = ba.getNextPoint(this.position);
				if (routeReservations.isEmpty()) {
					System.err.println("This agent" + ba.getAgent()
							+ " was expected to have a routeReservation, but does not have one, so a new one is made for the AGV");
					RouteReservation errorRR = new RouteReservation(ba.agent, position, timeLapse.getTime(),
							timeLapse.getTime() + robotTimePerHop * 2, exit, this);
					this.routeReservations.add(errorRR);
					((AGVAgent) ba.getAgent()).reservationExtendedReSearchNeeded(timeLapse);
					return errorRR;
				}
				// First routeReservations need to be added
				if (routeReservations.get(0).agv.equals(ba.agent)) {
					return routeReservations.get(0);
				} else {
					if (verbose) {
						new SimulationException(
								"Couldnt make a new RouteReservation for the given Agent as the Agent should have already reserved his spot where he is located")
										.printStackTrace();
					}
					rr = new RouteReservation((AGVAgent) ba.agent, position, timeLapse.getTime(), robotTimePerHop, exit,
							this);
					// add maximum end time
					routeReservations.add(rr);
					return rr;
				}
			} else {
				long arrivalTime = rr.getEndTime();
				if (routeReservations.size() == 0)
					return new RouteReservation((AGVAgent) ba.agent, this.position, arrivalTime,
							arrivalTime + robotTimePerHop, ba.getNextPoint(this.position), this);

				for (int i = 0; i < routeReservations.size(); i++) {
					RouteReservation reservation = routeReservations.get(i);
					// if i is the first we only need to look at the first in the group (only taking
					// into account the first BeginTime of the first)
					if (i == 0) {
						if (arrivalTime + robotTimePerHop < reservation.getBeginTime()) {
							return new RouteReservation((AGVAgent) ba.agent, this.position, arrivalTime,
									arrivalTime + robotTimePerHop, ba.getNextPoint(this.position), this);
							// add maximum end time
						} else if (arrivalTime >= reservation.getEndTime()) {
							return new RouteReservation((AGVAgent) ba.agent, this.position, arrivalTime,
									arrivalTime + robotTimePerHop, ba.getNextPoint(this.position), this);
						}
					}
					// Now we need to take into account the start of the previous one and the
					// beginning of the following one
					// We do want to find the first available one
					else {
						// Check if this reservation beginning is after the end of the previous
						// reservation of the ant
						RouteReservation prevReservation = routeReservations.get(i - 1);
						if (arrivalTime + 1 > prevReservation.endTime
								&& arrivalTime + robotTimePerHop < reservation.getBeginTime()
								&& arrivalTime < rr.maximumEndTime) {
							return new RouteReservation((AGVAgent) ba.agent, this.position, rr.getEndTime(),
									rr.getEndTime() + robotTimePerHop, ba.getNextPoint(this.position), this,
									reservation.getBeginTime());
							// add maximum end time
						}
					}
				}
				// If we haven't found a time slot yet this means there is no available time
				// slot and it should be the last one passing through here
				RouteReservation reservation = routeReservations.get(routeReservations.size() - 1);
				return new RouteReservation((AGVAgent) ba.agent, this.position, reservation.getEndTime(),
						reservation.getEndTime() + robotTimePerHop, ba.getNextPoint(this.position), this);
				// add maximum end time
			}
		} catch (

		SimulationException e) {
			e.printStackTrace();
			return null;
		}
	}

	public boolean agvHasReservation(int iD, long time) {
		if (routeReservations.isEmpty())
			return false;

		return (routeReservations.get(0).isIn(time) && routeReservations.get(0).agv.ID == iD);
	}

	/****************
	 * Ant Handlers *
	 ****************/
	@Override
	public void handleRestingPlaceAnt(findRestingPlaceAnt rp, TimeLapse timeLapse) {
		if (rp.hasVisitedNode(this.position) && rp.canContinue())
			return;

		if (rp.canContinue()) {
			rp.addVisitedNode(this.position);
			for (BasicAgent b : this.neighbours) {
				Point p = b.getPosition().get();
				if (this.outgoingNodes.contains(p) && rp.canContinue(p) && !rp.hasVisitedNode(p)) {
					commDevice.send(rp, b);
				}
			}
		}
	}

	@Override
	public void handleDeadLockDetectionAnt(DeadLockDetectionAnt dlAnt, TimeLapse time) {
		if (this.routeReservations.isEmpty()) {
			// If there is no currentReservation there is no reason for checking for
			// deadlocks
			// AGVS should be able to enter here cause no reservation present
			// Searching for deadlocks ends
			if (verbose)
				System.out.println(" there was no current reservation");
		} else if (dlAnt.addNode(this.position)) {
			// we have not visited this node yet, so no possible deadlocks
			// send the ant to the AGV that is on this node

			// commDevice.send(dlAnt, this.routeReservations.get(0).agv);
			if (this.routeReservations.get(0).agv.getPosition().get().equals(this.position)) {
				dlAnt.handleAnt(this.routeReservations.get(0).agv, time);
			}
		} else {
			dlAnt.setLastNode(this.position, time);
			System.err.println(" A CIRCLE HAS BEEN DETECTED");
			// WIP
			// we have visited this node so we have a circle
			// We have found a possible deadlock
		}
	}

	public void handleChargingIntentionAnt(ChargingIntentionAnt chargingIntentionAnt, TimeLapse timeLapse) {
		if (chargingIntentionAnt.isReturning) {
			handleReturningAnt(chargingIntentionAnt, timeLapse);
		} else {
			// addMeToAntNonRouteBinding(chargingIntentionAnt, timeLapse);
			if (chargingIntentionAnt.canContinue())
				sendAllongPath(chargingIntentionAnt, timeLapse);
		}
	}

	// I think something goes wrong here, the routeReservations are not completly
	// right
	// with multiple routeReservations on the same node shoudl be after each other
	// but im not sure this happens
	public void handlePathIntentionAnt(PathIntentionAnt pathIntentionAnt, TimeLapse timeLapse) {
		if (pathIntentionAnt.isReturning) {
			handleReturningAnt(pathIntentionAnt, timeLapse);
		} else {
			addMeToAntRouteBinding(pathIntentionAnt, timeLapse);
			if (pathIntentionAnt.checkGoal(this.position)) {
				pathIntentionAnt.isReturning = true;
				pathIntentionAnt.handleAnt(pathIntentionAnt.sendBy, timeLapse);
			} else {
				if (pathIntentionAnt.canContinue())
					sendAllongPath(pathIntentionAnt, timeLapse);
			}
		}
	}

	// The ants also should not come back node by node but send directly making this
	// method (normally never be called)
	@SuppressWarnings("unused")
	@Deprecated
	private boolean endTimeRouteReservationCheck(PathIntentionAnt ant, TimeLapse timeLapse) {
		RouteReservation reservation = findRouteReservation(ant, timeLapse);
		if (reservation == null)
			return false;

		Long newEndTime = null;
		for (int i = 0; i < ant.reservations.size(); i++) {
			RouteReservation rr = ant.reservations.get(i);
			if (rr.node.equals(this.position)) {
				if (ant.reservations.size() > i + 1) {
					newEndTime = ant.reservations.get(i + 1).getBeginTime();
					break;
				}
			}
		}

		if (newEndTime == null)
			return true;

		for (int i = 0; i < routeReservations.size(); i++) {
			RouteReservation rr = routeReservations.get(i);
			if (rr.equals(reservation)) {
				if (i + 1 < routeReservations.size()) {
					RouteReservation nextReservation = routeReservations.get(i + 1);
					if (newEndTime > nextReservation.startTime)
						return false;
					else {
						reservation.endTime = newEndTime;
						return true;
					}
				} else
					return true;
			}
		}

		return true;
	}

	private RouteReservation findRouteReservation(PathAnt ant, TimeLapse timeLapse) {
		// This needs to check if there is a routeReservation that is already present
		// for the AGV at the same time
		// If there is extend the pheremones
		// If there is not return null
		RouteReservation rr = null;

		try {
			for (int i = 0; i < ant.reservations.size(); i++) {
				rr = ant.reservations.get(i);
				if (rr.node.equals(this.position)) {
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (rr == null) {
			if (ant.agent.getPosition().get().equals(this.position)) {
				rr = this.getCurrentReservation(timeLapse);
				if (rr == null)
					return null;
				ant.addRouteReservation(rr);
				rr.resetEvaporation();
				return rr;
			} else
				return null;
		}

		for (RouteReservation rrThis : routeReservations) {
			if (rrThis.equals(rr)) {
				rrThis.resetEvaporation();
				return rrThis;
			}
		}
		return null;
	}

	public void handleTaskIntentionAnt(TaskIntentionAnt taskIntentionAnt, TimeLapse timeLapse) {
		if (taskIntentionAnt.isReturning) {
			handleReturningAnt(taskIntentionAnt, timeLapse);
		} else {
			// addMeToAntNonRouteBinding(taskIntentionAnt, timeLapse);
			if (taskIntentionAnt.canContinue())
				sendAllongPath(taskIntentionAnt, timeLapse);
		}
	}

	public void handleChargingExplorationAnt(ChargingExplorationAnt explorationAnt, TimeLapse timeLapse) {
		if (verbose)
			System.out.println(
					"Handling ChargingExplorationAnt " + this.position + ";  " + explorationAnt.sendBy.toString());

		if (explorationAnt.isReturning) {
			handleReturningAnt(explorationAnt, timeLapse);
		} else {
			addMeToAntNonRouteBinding(explorationAnt, timeLapse);
			if (explorationAnt.canContinue())
				sendAllongPath(explorationAnt, timeLapse);
		}
	}

	public void handlePathExplorationAnt(PathExplorationAnt explorationAnt, TimeLapse timeLapse) {
		if (verbose)
			System.out.println("[INFO] Handling PathExplorationAnt " + this.position + "; for "
					+ explorationAnt.sendBy.toString());

		if (explorationAnt.isReturning) {
			handleReturningAnt(explorationAnt, timeLapse);
		} else {
			addMeToAntNonRouteBinding(explorationAnt, timeLapse);
			if (explorationAnt.checkGoal(this.position)) {
				if (explorationAnt.sendDependingAnts) {
					sendChargingExplorationAnts(explorationAnt, timeLapse);
				} else {
					explorationAnt.isReturning = true;
					handleReturningAnt(explorationAnt, timeLapse);
				}
			} else {
				if (explorationAnt.canContinue())
					sendAllongPath(explorationAnt, timeLapse);
			}
		}
	}

	private void sendChargingExplorationAnts(PathExplorationAnt explorationAnt, TimeLapse time) {
		ChargingStation cs = this.getClosestChargingStation();

		if (cs == null) {
			// no chargingstations were in range so we just resend the ant
			explorationAnt.isReturning = true;
			handleReturningAnt(explorationAnt, time);
		}

		List<List<Point>> paths = AStar.getInstance().getAlternativePaths(1, this.position, cs.position, rng,
				staticGraph);
		for (List<Point> path : paths) {
			PathAnt ba = new ChargingExplorationAnt(path, explorationAnt.agent);
			ba.addRouteReservation(explorationAnt.getLastReservation());
			ba.sendBy = this;
			sendTimeDependantAnts.put(ba.id, 0);
			dependantAnts.put(ba.id, explorationAnt);
			outGoingAnts.add(ba);
		}
	}

	protected ChargingStation getClosestChargingStation() {
		Collection<ChargingStation> chargingStations = darModel.getChargingStations();
		if (chargingStations.isEmpty()) {
			return null;
		}
		long distance = Long.MAX_VALUE;
		ChargingStation csa = null;
		for (ChargingStation cs : chargingStations) {
			// If the distance is to big we should accept that the chargingStation is not
			// worth it to look fof
			List<List<Point>> paths = AStar.getInstance().getAlternativePaths(1, this.position, cs.position, rng,
					staticGraph);
			long tempDistance = 0;
			for (List<Point> path : paths) {
				tempDistance = AStar.calculateDistanceFromPath(path);
			}

			if (tempDistance < distance) {
				csa = cs;
				distance = tempDistance;
			}
		}
		return csa;
	}

	public void handleTaskExplorationAnt(TaskExplorationAnt explorationAnt, TimeLapse timeLapse) {
		if (verbose)
			System.out.println("[INFO] Handling TaskExplorationAnt " + this.position + "; for "
					+ explorationAnt.sendBy.toString());

		if (explorationAnt.isReturning) {
			handleReturningAnt(explorationAnt, timeLapse);
		} else {
			addMeToAntNonRouteBinding(explorationAnt, timeLapse);
			if (explorationAnt.canContinue())
				sendAllongPath(explorationAnt, timeLapse);
		}

	}

	// Returning ants can be send directly back to the source that send them
	protected void handleReturningAnt(PathAnt ba, TimeLapse time) {
		Point nextPoint = null;
		try {
			nextPoint = ba.getNextPoint(this.position);
		} catch (SimulationException e) {
			//e.printStackTrace();
		}
		if (nextPoint == null) {
			if (ba.sendBy == this)
				incomingAnts.add(ba);
			else
				commDevice.send(ba, ba.agent);
		} else if (ba.sendBy != null)
			ba.handleAnt(ba.sendBy, time);
		else
			sendAntToNeighbour(ba, nextPoint, time);
	}

	protected void addMeToAntNonRouteBinding(PathAnt ba, TimeLapse timeLapse) {
		RouteReservation newRR = findRouteReservation(ba, timeLapse);
		if (newRR == null)
			newRR = findNextPotentialRouteReservation(ba, timeLapse);
		if (newRR == null)
			return;
		if (deadLockDetection(newRR, ba.getLastReservation()))
			ba.addRouteReservation(new DeadLockRouteReservation(newRR));
		else
			ba.addRouteReservation(newRR);
	}

	protected void addMeToAntRouteBinding(PathAnt ba, TimeLapse timeLapse) {
		RouteReservation RR = findRouteReservation(ba, timeLapse);
		RouteReservation temp;
		if (RR == null) {
			temp = findNextPotentialRouteReservation(ba, timeLapse);
			// afterward we update the previous routeReservation so that the endtime equals
			// the time the move should happen

			RouteReservation rr1 = null;
			for (RouteReservation r : ba.reservations) {
				if (r.node.equals(this.position)) {
					rr1 = r;
					break;
				}
			}
			if (rr1 != null) {
				ba.reservations.remove(rr1);
			}

			RouteReservation rr2 = null;
			for (int i = 0; i < ba.reservations.size(); i++) {
				if (ba.reservations.get(i).node.equals(this.position)) {
					if (i != 0) {
						rr2 = ba.reservations.get(i - 1);
						break;
					}
					break;
				}
			}
			if (rr2 != null) {
				if (rr2.maximumEndTime < temp.getBeginTime()) {
					rr2.endTime = temp.getBeginTime();
				} else {
					ba.accepted = false;
				}
			}

			if (deadLockDetection(temp, ba.getLastReservation()))
				ba.addRouteReservation(new DeadLockRouteReservation(temp));
			else {
				if (temp == null) {
					new SimulationException("");
				}
				routeReservations.add(temp);
				ba.addRouteReservation(temp);
			}
			routeReservations.sort(new RouteComperator());

		} else {
			RR.resetEvaporation();
		}
	}

	protected boolean deadLockDetection(RouteReservation newRR, RouteReservation lastReservation) {
		if (!SimulationSettings.DeadlockPreventionOn) {
			return false;
		}

		if (lastReservation == null)
			return false;
		Point incomingNode = lastReservation.node;
		long timeIn = newRR.getBeginTime();

		for (RouteReservation rr : routeReservations) {
			if (rr.getEndTime() == timeIn) {
				if (rr.exit != null && rr.exit.equals(incomingNode))
					return true;
			}
		}
		return false;

	}

	protected void checkDependantAnts() {
		for (PathAnt ba : incomingAnts) {
			PathAnt dependant = dependantAnts.get(ba.id);
			if (dependant != null) {
				// System.err.println("FOUND DEPENDING ANT");
				dependant.setDependentAnt(ba);
				// System.out.println(dependant);
				if (!(dependant instanceof TaskExplorationAnt)
				// || (dependant instanceof TaskExplorationAnt && ba instanceof
				// ChargingExplorationAnt)
				) {
					dependant.isReturning = true;
					outGoingAnts.add(dependant);
					dependantAnts.remove(ba.id);
					sendTimeDependantAnts.remove(ba.id);
				} else {
					System.out.print("");
				}
				// antToSend.isReturning = true;
				// outGoingAnts.add(antToSend);
				// System.err.println(antToSend.reservations);
			}
		}
		incomingAnts.clear();

		ArrayList<Integer> toRemove = new ArrayList<Integer>();
		// Here we update all the timeDependant ants time (how long they waited for an
		// answer)
		// IF it takes to long to get an answer we send the ant back without the
		// dependant ant
		// The agents that receive this should just assume the dependant ant failed
		// getting there
		Map<Integer, Integer> tempMap = new HashMap<Integer, Integer>();
		for (Entry<Integer, Integer> entry : sendTimeDependantAnts.entrySet()) {
			int i = entry.getValue();
			if (i < this.waitingForDependant) {
				i++;
				tempMap.put(entry.getKey(), i);
			} else {

				int ID = entry.getKey();
				PathAnt tempAnt = dependantAnts.get(ID);
				tempAnt.isReturning = true;

				if (!tempAnt.hasDependentAnt()) {
					// we didnt find a path to the endpoint of the taskExploration W
					// We should not consider this ant in that case
					System.err.print("");
					// System.err.println(tempAnt + " Has no dependant ants " + this);
				}

				outGoingAnts.add(tempAnt);
				toRemove.add(ID);
			}
		}

		for (int ID : toRemove) {
			sendTimeDependantAnts.remove(ID);
		}
		sendTimeDependantAnts = tempMap;

	}

	/****************
	 * Sending Ants *
	 ****************/
	protected void sendOutgoingAnts(TimeLapse timeLapse) {
		for (PathAnt ba : outGoingAnts) {
			// addMeToAntNonRouteBinding(ba, timeLapse);
			if (ba.isReturning) {
				handleReturningAnt(ba, timeLapse);
			} else
				try {
					Point nextPoint;
					nextPoint = ba.getNextPoint(this.position);
					if (nextPoint == null)
						return;
					for (CommUser cu : neighbours) {
						if (cu.getPosition().get().equals(nextPoint)) {
							commDevice.send(ba, cu);
							break;
						}
					}
					throw new SimulationException("CAN'T FIND A NEIGHBOUR I SHOULD HAVE " + this + " to " + nextPoint);
				} catch (SimulationException e) {
					//e.printStackTrace();
				}
		}
		outGoingAnts.clear();
	}

	protected void sendAntToNeighbour(BasicAnt ant, Point nextPoint, TimeLapse time) {
		if (nextPoint.equals(this.position)) {
			if (verbose)
				new SimulationException("Trying to send message to myself").printStackTrace();
		} else {

			for (CommUser bs : neighbours) {
				if (bs.getPosition().get().equals(nextPoint)) {
					if (verbose)
						System.out.println("InfrastructureAgent " + this + " send " + ant + " to " + bs);
					ant.handleAnt(((InfrastructureAgent) bs), time);
					return;
				}
			}

			new SimulationException("Couldnt find neighbour " + nextPoint + " for node " + this.position)
					.printStackTrace();
		}
	}

	// relook at this
	// Not happy with the giant stacks that the direct handle ant invokes
	// But the sending of the ants needs to happen at a brisk pace
	// Ant i think it is to slow doing it with the sending using comm?

	// Has to be done like this as otherwist it takes to long to actually find a new
	// path before just moving to a new node
	protected void sendAllongPath(PathAnt ant, TimeLapse time) {
		if (verbose)
			System.out.println("Agent " + this + " send " + ant);

		try {
			Point nextPoint;
			nextPoint = ant.getNextPoint(this.position);
			if (nextPoint == null)
				return;
			for (CommUser ba : neighbours) {
				if (ba.getPosition().get().equals(nextPoint)) {
					// commDevice.send(ant, ba);
					ant.handleAnt(((InfrastructureAgent) ba), time);
					return;
				}
			}
			throw new SimulationException("CAN'T FIND A NEIGHBOUR I SHOULD HAVE " + this + " to " + nextPoint);
		} catch (SimulationException e) {
			//e.printStackTrace();
		}

	}

	public Set<BasicAgent> getNeighbors() {
		return this.neighbours;
	}

	/***********************
	 * Methods for init
	 ***********************/

	@SuppressWarnings("unchecked")
	public void initRoadPDP(RoadModel roadModel, PDPModel pdpModel) {
		this.pdpModel = pdpModel;
		this.roadModel = roadModel;
		this.staticGraph = (ListenableGraph<LengthData>) ((DynamicGraphRoadModelImpl) roadModel).getGraph();
	}

	public void setCommDevice(@NotNull CommDeviceBuilder builder) {
		builder.setMaxRange(2 * SimulationSettings.AGV_Length);
		commDevice = builder.build();
	}

	public void setRandomGenerator(RandomProvider provider) {
		this.rng = provider.newInstance();
	}

	public Optional<Point> getPosition() {
		return Optional.of(this.position);
	}

	public RouteReservation getCurrentReservation(TimeLapse time) {
		if (this.routeReservations.isEmpty()) {
			return null;
		}
		if (routeReservations.get(0) != null && routeReservations.get(0).isIn(time.getTime()))
			return routeReservations.get(0);
		return null;
	}

	public boolean refreshCurrentReservation(TimeLapse time) {
		if (this.routeReservations.isEmpty())
			return false;
		if (this.routeReservations.get(0).isIn(time.getTime())) {
			this.routeReservations.get(0).resetEvaporation();
			return true;
		}
		return false;
	}

	@Override
	public void initRoadUser(RoadModel model) {
		model.addObjectAt(this, this.position);
	}

	public boolean canRestHere(AGVAgent agv) {
		return false;
	}

	@Override
	public String toString() {
		return "Infrastructure: Position " + this.position;

	}

	public boolean canSearchHere(AGVAgent agv) {
		return false;
	}

	public boolean containsReservationForMe(AGVAgent agvAgent) {
		for (RouteReservation rr : routeReservations) {
			if (rr.agv.equals(agvAgent))
				return true;
		}
		return false;
	}

	public long nonBlockedCheck(Point point, TimeLapse time, AGVAgent agv) {
		return (long) 0d;
	}

	public long endCurrentReservation(TimeLapse time) {
		if (!this.routeReservations.isEmpty() && this.routeReservations.get(0).isIn(time.getTime())) {
			return this.routeReservations.get(0).endTime;
		}
		return 0;
	}

	public long endCurrentReservationMotAgv(TimeLapse time, AGVAgent agv) {
		if (!this.routeReservations.isEmpty() && this.routeReservations.get(0).isIn(time.getTime())
				&& !this.routeReservations.get(0).agv.equals(agv)) {
			return this.routeReservations.get(0).endTime;
		}
		return 0;
	}

	public void removeMyReservation(AGVAgent agv) {
		ArrayList<RouteReservation> toRemove = new ArrayList<RouteReservation>();
		for (RouteReservation rr : this.routeReservations) {
			if (rr.agv.equals(agv)) {
				toRemove.add(rr);
			}
		}
		routeReservations.removeAll(toRemove);
	}

	public long beginTimeReservationForMe(AGVAgent agv) {
		for (RouteReservation rr : this.routeReservations) {
			if (rr.agv.equals(agv)) {
				return rr.getBeginTime();
			}
		}
		return (long) 0d;
	}

	public RouteReservation nextReservation() {
		if (routeReservations.isEmpty())
			return null;
		return routeReservations.get(0);
	}

	/*
	 * public boolean equals(Object o) { if (o == null) return false; if (!(o
	 * instanceof InfrastructureAgent)) return false; if (((InfrastructureAgent)
	 * o).position.equals(this.position)) return true; return false; }
	 */
}

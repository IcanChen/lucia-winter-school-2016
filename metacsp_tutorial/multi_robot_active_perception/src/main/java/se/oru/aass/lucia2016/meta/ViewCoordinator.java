package se.oru.aass.lucia2016.meta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import org.hamcrest.core.IsInstanceOf;
import org.metacsp.framework.Constraint;
import org.metacsp.framework.ConstraintNetwork;
import org.metacsp.framework.ConstraintSolver;
import org.metacsp.framework.Variable;
import org.metacsp.framework.meta.MetaConstraint;
import org.metacsp.framework.meta.MetaConstraintSolver;
import org.metacsp.framework.meta.MetaVariable;
import org.metacsp.meta.spatioTemporal.paths.Map;
import org.metacsp.meta.spatioTemporal.paths.TrajectoryEnvelopeScheduler;
import org.metacsp.multi.allenInterval.AllenIntervalConstraint;
import org.metacsp.multi.spatial.DE9IM.DE9IMRelation;
import org.metacsp.multi.spatial.DE9IM.GeometricShapeDomain;
import org.metacsp.multi.spatial.DE9IM.GeometricShapeVariable;
import org.metacsp.multi.spatioTemporal.paths.PoseSteering;
import org.metacsp.multi.spatioTemporal.paths.Trajectory;
import org.metacsp.multi.spatioTemporal.paths.TrajectoryEnvelope;
import org.metacsp.multi.spatioTemporal.paths.TrajectoryEnvelopeSolver;
import org.metacsp.time.Bounds;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

import se.oru.aass.lucia2016.multi.RobotConstraint;
import se.oru.aass.lucia2016.multi.ViewConstraint;
import se.oru.aass.lucia2016.multi.ViewConstraintSolver;
import se.oru.aass.lucia2016.multi.ViewVariable;

public class ViewCoordinator extends MetaConstraintSolver{

	
	private HashMap<TrajectoryEnvelope,ArrayList<TrajectoryEnvelope>> refinedWith = new HashMap<TrajectoryEnvelope, ArrayList<TrajectoryEnvelope>>();
	private static final int MINIMUM_SIZE = 5;
	
	protected ViewCoordinator(Class<?>[] constraintTypes, long animationTime,
			ConstraintSolver[] internalSolvers) {
		super(constraintTypes, animationTime, internalSolvers);
		// TODO Auto-generated constructor stub
	}

	
	/**
	 * Create a {@link ViewCoordinator} with a given origin and temporal horizon.
	 * @param origin The origin of time.
	 * @param horizon The temporal horizon.
	 * @param maxTrajectories The maximum number of {@link TrajectoryEnvelope}s that can be created with this solver.
	 */
	public ViewCoordinator(long origin, long horizon, int maxTrajectories) {
		super(new Class[] {AllenIntervalConstraint.class, DE9IMRelation.class, ViewConstraint.class, RobotConstraint.class}, 0, new ViewConstraintSolver(origin, horizon, maxTrajectories));
	}
	
	@Override
	public void preBacktrack() {
		// TODO Auto-generated method stub
	}

	@Override
	public void postBacktrack(MetaVariable metaVariable) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void retractResolverSub(ConstraintNetwork metaVariable,
			ConstraintNetwork metaValue) {
		ViewConstraintSolver solver = (ViewConstraintSolver)this.getConstraintSolvers()[0];
		Constraint[] cons = metaValue.getConstraints();
		for (int i = 0; i < cons.length; i++) {
			if(cons[i] instanceof RobotConstraint){
				Constraint[] tcons = solver.getTrajectoryEnvelopeSolver().getConstraints();
				for (int j = 0; j < tcons.length; j++) {
					AllenIntervalConstraint tc = (AllenIntervalConstraint)tcons[j];
					if(tc.getTypes()[0].equals(AllenIntervalConstraint.Type.Meets)){
						TrajectoryEnvelope from = (TrajectoryEnvelope)tc.getFrom();
						Constraint during = solver.getTrajectoryEnvelopeSolver().getConstraintNetwork().getConstraints(from, from)[0];
						solver.getTrajectoryEnvelopeSolver().removeConstraint(tc);
						solver.getTrajectoryEnvelopeSolver().removeConstraint(during);
						solver.getTrajectoryEnvelopeSolver().removeVariable(from);
					}
						
				}
			}
		}
		
		//remove setUsage
		
	}

	@Override
	protected boolean addResolverSub(ConstraintNetwork metaVariable,
			ConstraintNetwork metaValue) {
		
		//for test now
		HashMap<Integer, String> robotToPath = new HashMap<Integer, String>();
//		robotToPath.put(1, "paths/path1.path");
//		robotToPath.put(2, "paths/path2.path");
//		robotToPath.put(3, "paths/path3.path");

		robotToPath.put(1, "paths/rid1_task0.path");
		robotToPath.put(2, "paths/rid1_task3.path");
		robotToPath.put(3, "paths/rid1_task5.path");

		ViewSchedulingMetaConstraint ViewSchedulingMC = null; 
		for (int i = 0; i < this.getMetaConstraints().length; i++) {
			if(this.getMetaConstraints()[i] instanceof ViewSchedulingMetaConstraint){
				ViewSchedulingMC = (ViewSchedulingMetaConstraint)this.getMetaConstraints()[i];
			}
		}
		
		
		//If it is robotConstraint, set the robot ID and create the trajectoryEnvelope for the path
		ViewConstraintSolver solver = (ViewConstraintSolver)this.getConstraintSolvers()[0];
		Constraint[] cons = metaValue.getConstraints();
		Variable[] vars =  metaValue.getVariables();
		for (int i = 0; i < cons.length; i++) {
			if(cons[i] instanceof RobotConstraint){
				RobotConstraint rc = (RobotConstraint)cons[i];
				ViewVariable vv = (ViewVariable)rc.getFrom();
				vv.getTrajectoryEnvelope().setRobotID(rc.getRobotId());
				vv.getTrajectoryEnvelope().getSymbolicVariableActivity().setComponent("Robot" + rc.getRobotId());

				//create the trajectoryEnvelope
				TrajectoryEnvelope moveinTE = (TrajectoryEnvelope)solver.getTrajectoryEnvelopeSolver().createVariable("Robot" + rc.getRobotId());
				Trajectory trajRobot1 = new Trajectory(robotToPath.get(rc.getRobotId()));				
				moveinTE.setFootprint(vv.getTrajectoryEnvelope().getFootprint());
				moveinTE.setTrajectory(trajRobot1);
				moveinTE.setRobotID(rc.getRobotId());
				moveinTE.setMarking("path");
				ViewSchedulingMC.setUsage(moveinTE);
				moveinTE.getSymbolicVariableActivity().setSymbolicDomain("MoveIn");
				
				//create a temporal constraint the trajectory and the parkingPolygon (viewVariable)
				AllenIntervalConstraint tcon = new AllenIntervalConstraint(AllenIntervalConstraint.Type.Meets);
				tcon.setFrom(moveinTE);
				tcon.setTo(vv.getTrajectoryEnvelope());
				solver.getTrajectoryEnvelopeSolver().addConstraint(tcon);
				
				TrajectoryEnvelope moveOutTE = creatMoveBaseTrajectoryEnvelope(vv);
				moveOutTE.getSymbolicVariableActivity().setSymbolicDomain("MoveOut");
				AllenIntervalConstraint meetsMoveOut =  new AllenIntervalConstraint(AllenIntervalConstraint.Type.Meets);
				meetsMoveOut.setFrom(vv.getTrajectoryEnvelope());
				meetsMoveOut.setTo(moveOutTE);
				solver.getTrajectoryEnvelopeSolver().addConstraint(meetsMoveOut);
				
//				//chop te with respect to others paths
				for (int j = 0; j < vars.length; j++) {
					if(!((ViewVariable)vars[j]).equals(vv)){
						refineTrajectoryEnvelopes(moveinTE,((ViewVariable)vars[j]).getFoV());
						refineTrajectoryEnvelopes(moveOutTE,((ViewVariable)vars[j]).getFoV());
//						System.out.println(vv);
					}
				}


				
			}
		}
		//make a trajectory envelope from the robot starting point to the end
		
		
		return true;
	}
	
	private TrajectoryEnvelope creatMoveBaseTrajectoryEnvelope(ViewVariable vv) {
		ViewConstraintSolver viewSolver= (ViewConstraintSolver)this.getConstraintSolvers()[0];
		TrajectoryEnvelope moveOutTE = (TrajectoryEnvelope)viewSolver.getTrajectoryEnvelopeSolver().createVariable("Robot" + vv.getTrajectoryEnvelope().getRobotID());
		Trajectory moveOutTrajectory = new Trajectory(getTrajectory(vv.getTrajectoryEnvelope().getRobotID()));				
		moveOutTE.setFootprint(vv.getTrajectoryEnvelope().getFootprint());
		moveOutTE.setTrajectory(moveOutTrajectory);
		moveOutTE.setRobotID(vv.getTrajectoryEnvelope().getRobotID());
		return moveOutTE;
	}

	private String getTrajectory(int robotID) {
		if(robotID == 1) 
			return "paths/rid1_task1.path";
		else if(robotID == 2)
			return "paths/rid1_task4.path";
		else
			return "paths/rid1_task6.path";
	}
	
	
	@Override
	protected double getUpperBound() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected void setUpperBound() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected double getLowerBound() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected void setLowerBound() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected boolean hasConflictClause(ConstraintNetwork metaValue) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void resetFalseClause() {
		// TODO Auto-generated method stub
		
	}
	
	private ConstraintNetwork refineTrajectoryEnvelopes(TrajectoryEnvelope var1, GeometricShapeVariable geometricShapeVariable) {
		ViewConstraintSolver viewSolver= ((ViewConstraintSolver)this.getConstraintSolvers()[0]);
		//ViewConstraintSolver viewSolver= (ViewConstraintSolver)this.getGroundSolver();
		TrajectoryEnvelopeSolver solver = viewSolver.getTrajectoryEnvelopeSolver();
		ConstraintNetwork toReturn = new ConstraintNetwork(null);
		GeometryFactory gf = new GeometryFactory();
		Geometry se1 = ((GeometricShapeDomain)var1.getEnvelopeVariable().getDomain()).getGeometry();
		Geometry se2 = ((GeometricShapeDomain)geometricShapeVariable.getDomain()).getGeometry();
		Geometry intersectionse1se2 = se1.intersection(se2);
		
		boolean useDefaultEnvelopeChunks = false;
		
		if (!intersectionse1se2.isValid()) {
			intersectionse1se2 = intersectionse1se2.symDifference(intersectionse1se2.getBoundary());
			logger.info("Intersection " + var1 + " with " + geometricShapeVariable + " invalid - fixing");
		}

		if (intersectionse1se2 instanceof MultiPolygon) {
			logger.info("Intersection " + var1 + " with " + geometricShapeVariable + " too complex - skipping");
			useDefaultEnvelopeChunks = true;
			//return toReturn;								
		}
		
		boolean in  = false;
		int countIn = 0;
		for (int i = 0; i < var1.getPathLength(); i++) {
			Coordinate coord = var1.getTrajectory().getPositions()[i];
			Point point = gf.createPoint(coord);
			if (intersectionse1se2.contains(point) && !in) {
				in = true;
				if (++countIn > 1) {
					logger.info("Reference path of " + var1 + " enters intersection with " + geometricShapeVariable + " multiple times - skipping");
					useDefaultEnvelopeChunks = true;
					break;
					//return toReturn;					
				}
			}
			if (!intersectionse1se2.contains(point)) {
				in = false;
			}
		}

		double areaDifference = intersectionse1se2.symDifference(intersectionse1se2.getBoundary()).union(se1).getArea()-se1.getArea();
		if (areaDifference > 0.001) {
			logger.info("Intersection " + var1 + " with " + geometricShapeVariable + " seems corrupt (area increased by " + areaDifference + ") - skipping ");
			useDefaultEnvelopeChunks = true;
			//return toReturn;											
		}

		// IRAN: UNOCMMENT THIS IF YOU HAVE PROBLEMS WITH SCHEDULING
//		if (!intersectionse1se2.coveredBy(se1)) {
//			logger.info("Intersection " + var1 + " with " + var2 + " is corrupted - skipping");
//			return toReturn;											
//		}

//		logger.info("Refining " + var1 + " with " + var2);

		ArrayList<PoseSteering> var1sec1 = new ArrayList<PoseSteering>();
		ArrayList<PoseSteering> var1sec2 = new ArrayList<PoseSteering>();
		ArrayList<PoseSteering> var1sec3 = new ArrayList<PoseSteering>();

		boolean skipSec1 = false;
		boolean skipSec3 = false;

		if (useDefaultEnvelopeChunks) {
			float percentageChunckOne = 0.30f;
			float percentageChunckTwo = 0.40f;
			for (int i = 0; i < var1.getPathLength(); i++) {
				PoseSteering ps = var1.getTrajectory().getPoseSteering()[i];
				if (i < var1.getPathLength()*percentageChunckOne) var1sec1.add(ps);
				else if (i < var1.getPathLength()*(percentageChunckOne+percentageChunckTwo)) var1sec2.add(ps);
				else var1sec3.add(ps);
			}
			logger.info("Using default chunk sizes " + var1sec1.size() + " / " + var1sec2.size() + " / " + var1sec3.size());
		}
		else {	
			for (int i = 0; i < var1.getPathLength(); i++) {
				Coordinate coord = var1.getTrajectory().getPositions()[i];
				PoseSteering ps = var1.getTrajectory().getPoseSteering()[i];
				Point point = gf.createPoint(coord);
				Geometry fp = var1.makeFootprint(ps);
				if (!intersectionse1se2.intersects(fp) && var1sec2.isEmpty()) {
					var1sec1.add(ps);
				}
				else if (intersectionse1se2.intersects(fp)) {
					var1sec2.add(ps);
				}
				else if (!intersectionse1se2.intersects(fp) && !var1sec2.isEmpty()) {
					var1sec3.add(ps);
				}
	//			if (!intersectionse1se2.contains(point) && var1sec2.isEmpty()) {
	//				var1sec1.add(ps);
	//			}
	//			else if (intersectionse1se2.contains(point)) {
	//				var1sec2.add(ps);
	//			}
	//			else if (!intersectionse1se2.contains(point) && !var1sec2.isEmpty()) {
	//				var1sec3.add(ps);
	//			}
			}
				
			//Add to start
			boolean done = false;
			while (!done) {
				try {
					Geometry lastPolySec1 = var1.makeFootprint(var1sec1.get(var1sec1.size()-1));
					if (lastPolySec1.disjoint(se2)) done = true;
					else {
						var1sec2.add(0,var1sec1.get(var1sec1.size()-1));
						var1sec1.remove(var1sec1.size()-1);
						logger.info("Added to start... (1)");
					}
				} catch (IndexOutOfBoundsException e) 
				{ skipSec1 = true; done = true; }
			}
			//If sec1 emptied, remove it
			if (var1sec1.size() < MINIMUM_SIZE) {
				while (var1sec1.size() > 0) {
					var1sec2.add(0,var1sec1.get(var1sec1.size()-1));
					var1sec1.remove(var1sec1.size()-1);
				}
				skipSec1 = true;
			}
	
			//Add to end
			done = false;
			while (!done) {
				try {
					Geometry firstPolySec3 = var1.makeFootprint(var1sec3.get(0));
					if (firstPolySec3.disjoint(se2)) done = true;
					else {
						var1sec2.add(var1sec3.get(0));
						var1sec3.remove(0);
	//					logger.info("Added to end... (1)");
					}
				} catch (IndexOutOfBoundsException e) { skipSec3 = true; done = true; }
			}
			//If sec3 emptied, remove it
			if (var1sec3.size() < MINIMUM_SIZE) {
				while (var1sec3.size() > 0) {
					var1sec2.add(var1sec3.get(0));
					var1sec3.remove(0);
				}
				skipSec3 = true;
			}
			
			if (var1sec2.size() < MINIMUM_SIZE) {
				if (var1sec1.size() > MINIMUM_SIZE) {
					var1sec2.add(0,var1sec1.get(var1sec1.size()-1));
					var1sec1.remove(var1sec1.size()-1);
	//				logger.info("Added to start... (2)");
				}
				else if (var1sec3.size() > MINIMUM_SIZE) {
					var1sec2.add(var1sec3.get(0));
					var1sec3.remove(0);				
	//				logger.info("Added to end... (2)");
				}
			}
	
				if ((skipSec1 && skipSec3) || (!skipSec1 && var1sec1.size() < MINIMUM_SIZE) || (!skipSec3 && var1sec3.size() < MINIMUM_SIZE) || var1sec2.size() < MINIMUM_SIZE) {
					logger.fine("Intersection " + var1 + " with " + geometricShapeVariable + " too small - skipping");
					return toReturn;
				}
		
		}

		var1.setRefinable(false);
		ArrayList<Trajectory> newTrajectories = new ArrayList<Trajectory>();
		ArrayList<TrajectoryEnvelope> newTrajectoryEnvelopes = new ArrayList<TrajectoryEnvelope>();
				
		if (!skipSec1) {
			newTrajectories.add(new Trajectory(var1sec1.toArray(new PoseSteering[var1sec1.size()]),var1.getTrajectory().getDts(0, var1sec1.size())));
			newTrajectories.add(new Trajectory(var1sec2.toArray(new PoseSteering[var1sec2.size()]),var1.getTrajectory().getDts(var1sec1.size(), var1sec1.size()+var1sec2.size())));
			if (!skipSec3) {
				newTrajectories.add(new Trajectory(var1sec3.toArray(new PoseSteering[var1sec3.size()]),var1.getTrajectory().getDts(var1sec1.size()+var1sec2.size(),var1.getTrajectory().getPoseSteering().length)));
			}
		}
		else {
			newTrajectories.add(new Trajectory(var1sec2.toArray(new PoseSteering[var1sec2.size()]),var1.getTrajectory().getDts(0, var1sec2.size())));
			if (!skipSec3) {
				newTrajectories.add(new Trajectory(var1sec3.toArray(new PoseSteering[var1sec3.size()]),var1.getTrajectory().getDts(var1sec2.size(),var1.getTrajectory().getPoseSteering().length)));
			}			
		}

		Variable[] newVars = solver.createVariables(newTrajectories.size());
		for (int i = 0; i < newVars.length; i++) {
			TrajectoryEnvelope te = (TrajectoryEnvelope)newVars[i];
			//te.setFootprint(var1.getWidth(), var1.getLength(), var1.getDeltaW(), var1.getDeltaL());
			te.setFootprint(var1.getFootprint());
			//Only for second!
//			if ((!skipSec1 && i == 1) || (skipSec1 && i == 0)) {
//				te.setRefinable(false);
//				refinedWith.get(geometricShapeVariable).add(te);
//			}
//			System.out.println("doing i = " + i + " skipsec1: " + skipSec1 + " skipsec3: " + skipSec3);
			te.setTrajectory(newTrajectories.get(i));
			te.setSuperEnvelope(var1);
			te.setRobotID(var1.getRobotID());
			var1.addSubEnvelope(te);
			newTrajectoryEnvelopes.add(te);			
		}

		AllenIntervalConstraint starts = new AllenIntervalConstraint(AllenIntervalConstraint.Type.Starts);
		starts.setFrom(newTrajectoryEnvelopes.get(0));
		starts.setTo(var1);
		toReturn.addConstraint(starts);

		AllenIntervalConstraint finishes = new AllenIntervalConstraint(AllenIntervalConstraint.Type.Finishes);
		finishes.setFrom(newTrajectoryEnvelopes.get(newTrajectoryEnvelopes.size()-1));
		finishes.setTo(var1);
		toReturn.addConstraint(finishes);

		double minTTT12 = 0.0;
		
		if (!skipSec1) minTTT12 = var1.getTrajectory().getDTs()[var1sec1.size()];
		else minTTT12 = var1.getTrajectory().getDTs()[var1sec2.size()];
		long minTimeToTransition12 = (long)(TrajectoryEnvelope.RESOLUTION*minTTT12);
		AllenIntervalConstraint before1 = new AllenIntervalConstraint(AllenIntervalConstraint.Type.Before, new Bounds(minTimeToTransition12,minTimeToTransition12));
		before1.setFrom(newTrajectoryEnvelopes.get(0));
		before1.setTo(newTrajectoryEnvelopes.get(1));
		toReturn.addConstraint(before1);
	
		if (newTrajectoryEnvelopes.size() > 2) {
			double minTTT23 = var1.getTrajectory().getDTs()[var1sec1.size()+var1sec2.size()];
			long minTimeToTransition23 = (long)(TrajectoryEnvelope.RESOLUTION*minTTT23);
			AllenIntervalConstraint before2 = new AllenIntervalConstraint(AllenIntervalConstraint.Type.Before, new Bounds(minTimeToTransition23,minTimeToTransition23));
			before2.setFrom(newTrajectoryEnvelopes.get(1));
			before2.setTo(newTrajectoryEnvelopes.get(2));
			toReturn.addConstraint(before2);
		}

//		System.out.println("var1sec1 (" + skipSec1 + "): " + var1sec1);
//		System.out.println("var1sec2: " + var1sec2);
//		System.out.println("var1sec3 (" + skipSec3 + "): " + var1sec3);
//		System.out.println("DTs of var1sec2: " + Arrays.toString(var1.getTrajectory().getDts( var1sec2.size(),var1.getTrajectory().getDTs().length-1 )));
		solver.addConstraints(toReturn.getConstraints());
		
		return toReturn;
	}

}
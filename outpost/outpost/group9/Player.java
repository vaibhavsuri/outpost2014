package outpost.group9;

import java.util.*;

import outpost.sim.Pair;
import outpost.sim.Point;
import outpost.sim.movePair;

public class Player extends outpost.sim.Player {
	final static int SIDE_SIZE = 100;

	boolean playerInitialized;
	int RADIUS;
	int L_PARAM;
	int W_PARAM;
	int MAX_TICKS;
	
	// utility stuff
	Point[] grid = new Point[SIDE_SIZE * SIDE_SIZE];
	List<ArrayList<Pair>> playersOutposts;
	List<Pair> playersBase = new ArrayList<>(Arrays.asList(new Pair(0,0), new Pair(99, 0), new Pair(99, 99), new Pair(0,99)));
	HashMap<Point, Integer> pointToPlayer = new HashMap<Point, Integer>();
	int tickCounter = 0;
	ArrayList<Pair> myOutposts;
	Point myBase;
	
	// duo strategy stuff
	ArrayList<Integer> outpostsForDuoStrategy = new ArrayList<Integer>();
	ArrayList<Duo> allDuos = new ArrayList<Duo>();
	Set<Point> alreadySelectedDuosTargets = new HashSet<Point>();
	Set<Duo> duosAlreadyWithTarget = new HashSet<Duo>();
	Set<Point> duosPointsOnEnemyBase = new HashSet<Point>();
	Set<Point> waitingToMove = new HashSet<Point>();
	
	// resource strategy stuff
	ArrayList<Pair> next_moves;
	int my_land, my_water;
	ArrayList<Cell> board_scored;
	Resource totalResourceNeeded;
	Resource totalResourceGuaranteed;


	
	public Player(int id_in) {super(id_in);}

	public void init() {}

	public int delete(ArrayList<ArrayList<Pair>> king_outpostlist, Point[] gridin) {
		return king_outpostlist.get(this.id).size()-1;
	}

	public ArrayList<movePair> move(ArrayList<ArrayList<Pair>> king_outpostlist, Point[] gridin, int r, int L, int W, int T) {
		if (!playerInitialized) {
			for (int i = 0; i < gridin.length; i++) {
				grid[i] = new Point(gridin[i]);
			}

			RADIUS = r;
			L_PARAM = L;
			W_PARAM = W;
			MAX_TICKS = T;
			
			myBase = getGridPoint(playersBase.get(id));
			
			//on the first tick, evaluate all the cells the board and store the scores in 
			//board_scored
			board_scored = evaluateBoard(r);
			
			playerInitialized = true;
		}
		playersOutposts = king_outpostlist;
		myOutposts = king_outpostlist.get(this.id);
		tickCounter++;
		for (int i = 0; i < SIDE_SIZE * SIDE_SIZE; i++) {
			grid[i].ownerlist.clear();
		}
		// init pointToPlayer
		pointToPlayer.clear();
		for (int i = 0; i < 4; i++) {
			ArrayList<Pair> outposts =  king_outpostlist.get(i);
			for (int j = 0; j < outposts.size(); j++) {
				pointToPlayer.put(getGridPoint(outposts.get(j)), new Integer(j));
			}
		}
		// init ownerlist with enemy points
		for (int i = 0; i < 4; i++) {
			if (i == id) {
				continue;
			}
			ArrayList<Pair> outposts =  king_outpostlist.get(i);
			for (int j = 0; j < outposts.size(); j++) {
				markFieldInRadius(getGridPoint(outposts.get(j)));
			}
		}
		System.out.printf("---- New tick -----\n");
		ArrayList<movePair> movelist = new ArrayList<movePair>();
		
		//preparation code for the resource strategy
		next_moves = new ArrayList<Pair>();
		totalResourceNeeded = new Resource((myOutposts.size())*W_PARAM,(myOutposts.size())*L_PARAM);
		totalResourceGuaranteed = new Resource(0,0);
		
		// preparation code for the duo strategy
		alreadySelectedDuosTargets.clear();
		duosAlreadyWithTarget.clear();
		allDuos.clear();
		outpostsForDuoStrategy.clear();
		duosPointsOnEnemyBase.clear();
		// update waitingToMove by removing outposts that moved
		Iterator<Point> it = waitingToMove.iterator();
		waitingToMoveWhileLoop:
		while(it.hasNext()) {
			Point p = it.next();
			for(int i = 0; i < 4; i++) {
				if (i == this.id) {
					continue;
				}
				
				for (Pair pr : playersOutposts.get(i)) {
					if (getGridPoint(pr).equals(p)) {
						continue waitingToMoveWhileLoop;
					}
				}
			}
			it.remove();
		}
		// update duosPointsOnEnemyBase
		for (int i = 0; i < 4; i++) {
			if (i == id) {
				continue;
			}
			Point enemyBase = getGridPoint(playersBase.get(i));
			int counter = 0;
			int leader = 0;
			int follower = 0;
			for (int outpostId = 0; outpostId < myOutposts.size(); outpostId++) {
				Point p = getGridPoint(myOutposts.get(outpostId));
				int dist = distance(p, enemyBase);
				if (dist == 1) {
					leader = outpostId;
					counter++;
				} else if (dist == 2) {
					follower = outpostId;
					counter++;
				}
				if (counter == 2) {
					duosPointsOnEnemyBase.add(getGridPoint(myOutposts.get(leader)));
					duosPointsOnEnemyBase.add(getGridPoint(myOutposts.get(follower)));
					alreadySelectedDuosTargets.add(enemyBase);
					System.out.printf("Outposts %d %d dominates base %s\n", leader, follower, pointToString(enemyBase));
					break;
				}
			}
		}


//		int outpostId = 0;
//		for (Pair thisOutpost : myOutposts) {
//			System.out.printf("Outpost %d: %d,%d\n", outpostId, thisOutpost.x, thisOutpost.y);
//			outpostId++;
//		}
		
		// decide strategy
		for (int currentOutpostId = myOutposts.size() - 1; currentOutpostId >= 0; currentOutpostId--) {
			Point outpost = getGridPoint(myOutposts.get(currentOutpostId));
			if (duosPointsOnEnemyBase.contains(outpost)) {
				continue;
			}

			

			if (totalResourceGuaranteed.isMoreThan(totalResourceNeeded)) {
				// Duo strategy
				outpostsForDuoStrategy.add(currentOutpostId);
			} else {	
				// TODO: be defensive if enemies too close
//				SortedSet<Point> enemiesByDistToOutpost = getEnemiesCloserThanDist(outpost, 10);
//				if (enemiesByDistToOutpost.size() != 0 && buildPath(outpost, enemiesByDistToOutpost.first()).size() < 10) {
//					// Duo strategy
//					outpostsInDanger.add(currentOutpostId);
//				}
				
				// Resource strategy
				boolean success = addResourceOutpostToMovelist(movelist, currentOutpostId);
				if (!success) {
					System.out.printf("Resource outpost %d failed\n", currentOutpostId);
				}
			}
		}
		
		// Duo strategy code:
		// Specify the partners
		Collections.reverse(outpostsForDuoStrategy); //reverse so the new outposts in the duo strategy does not cause all duos to change
		for (int i = 0; i < outpostsForDuoStrategy.size(); i+=2) {
			int outpostId1 = outpostsForDuoStrategy.get(i);
			if(i+1 >= outpostsForDuoStrategy.size()) {
				addResourceOutpostToMovelist(movelist, outpostId1);
				continue;
			}
			
			int outpostId2 = outpostsForDuoStrategy.get(i+1);
			
			System.out.printf("Duo %d %d\n", outpostId1, outpostId2);
			allDuos.add(new Duo(outpostId1, outpostId2));
		}
		
		SortedSet<Point> enemiesByDistToMyBase = getEnemiesByDistanceToPoint(myBase);
		for (Point enemy : enemiesByDistToMyBase) {
			SortedSet<Duo> duos = getDuosByDistanceToPoint(getGridPoint(enemy));
			if (duos.size() == 0) {
				break;
			}
			for (Duo duo : duos) {
				boolean tooFar = distance(myOutposts.get(duo.p1), enemy) > 30;
				boolean moreEnemiesThanDuos = duos.size() < enemiesByDistToMyBase.size();
				if (!(tooFar && moreEnemiesThanDuos)) {
					addDuoToMovelist(movelist, duo, getGridPoint(enemy));
					break;
				}
			}
		}

		// Second pass: send to remaining targets or to collect resources.
		for(Duo duo : allDuos) {
			if (duosAlreadyWithTarget.contains(duo)) {
				continue;
			}
			
			SortedSet<Point> enemiesByDistToDuo = getEnemiesByDistanceToPoint(getGridPoint(myOutposts.get(duo.p1)));
			if (enemiesByDistToDuo.size() != 0) {
				addDuoToMovelist(movelist, duo, getGridPoint(enemiesByDistToDuo.first()));
				continue;
			} else {
				addResourceOutpostToMovelist(movelist, duo.p1);
				addResourceOutpostToMovelist(movelist, duo.p2);
				System.out.printf("Duo %d %d will gather resource instead\n", duo.p1, duo.p2);
			}
		}
		
		return movelist;
	}
	
	Resource markFieldInRadius(Point outpost) {
		Resource newResource = new Resource(0,0);
		for (int i = Math.max(0, outpost.x - RADIUS); i < Math.min(SIDE_SIZE, outpost.x + RADIUS + 1); i++) {
			for (int j = Math.max(0, outpost.y - RADIUS); j < Math.min(SIDE_SIZE, outpost.y + RADIUS + 1); j++) {
				Point p = getGridPoint(i, j);
				int dist = distance(outpost, p);
				if (dist > RADIUS) {
					continue;
				}
				
				boolean isTheClosest = true;
				boolean isMyFirstOutpost = true;
				for (Pair pr : p.ownerlist) {
					int distPr = distance(pr, p);
					if (distPr < dist) {
						isTheClosest = false;
					}
					Integer playerId = pointToPlayer.get(getGridPoint(pr));
					if (playerId == id) {
						isMyFirstOutpost = false;
					}
				}
				
				if (isTheClosest && isMyFirstOutpost) {
					if (p.water) {
						newResource.water++;
					} else {
						newResource.land++;
					}
				}
				
				p.ownerlist.add(pointToPair(outpost));
			}
		}
		return newResource;
	}
	
	void addDuoToMovelist(ArrayList<movePair> movelist, Duo designatedDuo, Point target) {
		System.out.printf("Designated duo %s. Target %s\n", designatedDuo, pointToString(target));
		
		duosAlreadyWithTarget.add(designatedDuo);
		alreadySelectedDuosTargets.add(target);
		
		//decide who is the leader
		int dist1 = buildPath(getGridPoint(myOutposts.get(designatedDuo.p1)), target).size();
		int dist2 = buildPath(getGridPoint(myOutposts.get(designatedDuo.p2)), target).size();
		int leaderId = designatedDuo.p1;
		int followerId = designatedDuo.p2;
		if (dist1 > dist2) {
			leaderId = designatedDuo.p2;
			followerId = designatedDuo.p1;
		}
		Point leader = getGridPoint(myOutposts.get(leaderId));
		Point follower = getGridPoint(myOutposts.get(followerId));
		
		Point myBase = getGridPoint(playersBase.get(id));
		int distLeaderToMyBase = buildPath(myBase, leader).size();
		int distFollowerToMyBase = buildPath(myBase, follower).size();
		int distTargetToMyBase = buildPath(myBase, target).size();
		if (playersBase.contains(target) && distance(leader, target) <= 2 && distance(follower, target) <= 2) {
			return;
		} else if (distance(follower, leader) > 1) {
			// get together
			Point leaderNextPosition = nextPositionToGetToPosition(leader, follower);
			movePair next = new movePair(leaderId, pointToPair(leaderNextPosition));
			movelist.add(next);
			Point followerNextPosition = nextPositionToGetToPosition(follower, leader);
			if (leaderNextPosition.equals(followerNextPosition)) {
				followerNextPosition = follower;
			}
			movePair next2 = new movePair(followerId, pointToPair(followerNextPosition));
			movelist.add(next2);
		} else if (!(distFollowerToMyBase < distTargetToMyBase || distLeaderToMyBase < distTargetToMyBase)){
			// not safe to attack, go back base
			// TODO add supplyline logic here
			Point leaderNextPosition = nextPositionToGetToPosition(leader, getGridPoint(playersBase.get(id)));
			movePair next = new movePair(leaderId, pointToPair(leaderNextPosition));
			movelist.add(next);
			Point followerNextPosition = nextPositionToGetToPosition(follower, leader);
			if (leaderNextPosition.equals(follower)) {
				followerNextPosition = nextPositionToGetToPosition(follower, getGridPoint(playersBase.get(id)));
			}
			movePair next2 = new movePair(followerId, pointToPair(followerNextPosition));
			movelist.add(next2);
			alreadySelectedDuosTargets.remove(target);
		} else if (distance(target, leader) != 1) {
			// safe to attack, but far, go close
			//TODO might not be safe, we need to calculate possible supplylines to be sure
			Point leaderNextPosition = nextPositionToGetToPosition(leader, target);
			movePair next = new movePair(leaderId, pointToPair(leaderNextPosition));
			movelist.add(next);
			Point followerNextPosition = nextPositionToGetToPosition(follower, leader);
			movePair next2 = new movePair(followerId, pointToPair(followerNextPosition));
			movelist.add(next2);
		} else {
			//safe to stay put, waiting for enemy to go to leader cell
			//TODO might not be safe, we need to calculate possible supplylines to be sure
			
			if (distance(target, leader) != 0 && waitingToMove.contains(target)) {
				// risk moving into enemy cell
				Point leaderNextPosition = nextPositionToGetToPosition(leader, target);
				movePair next = new movePair(leaderId, pointToPair(leaderNextPosition));
				movelist.add(next);
				Point followerNextPosition = nextPositionToGetToPosition(follower, leader);
				movePair next2 = new movePair(followerId, pointToPair(followerNextPosition));
				movelist.add(next2);
			}
			waitingToMove.add(target);
		}
	}
	
	SortedSet<Duo> getDuosByDistanceToPoint(final Point p) {
		SortedSet<Duo> duos = new TreeSet<Duo>(new Comparator<Duo>() {
            @Override
            public int compare(Duo o1, Duo o2) {
            	Pair p1 = playersOutposts.get(id).get(o1.p1);
            	Pair p2 = playersOutposts.get(id).get(o2.p1);
            	int dist1 = distance(p1, p);
            	int dist2 = distance(p2, p);
                int diff = (dist1 - dist2);
                if (diff > 0) {
                	return 1;
                } else if (diff == 0) {
                	return (p1.x - p2.x) + 100*(p1.y - p2.y);
                } else {
                	return -1;
                }
            }
        });
		
		for(Duo duo : allDuos) {
			if (duosAlreadyWithTarget.contains(duo)) {
				continue;
			}
			
			duos.add(duo);
		}
		return duos;
	}
	
	SortedSet<Duo> getDuosById() {
		SortedSet<Duo> duos = new TreeSet<Duo>(new Comparator<Duo>() {
            @Override
            public int compare(Duo o1, Duo o2) {
            	Pair p1 = playersOutposts.get(id).get(o1.p1);
            	Pair p2 = playersOutposts.get(id).get(o2.p1);
                int diff = (o1.p1 + o1.p2 - (o2.p1 + o2.p2));
                if (diff > 0) {
                	return 1;
                } else if (diff == 0) {
                	return (p1.x - p2.x) + 100*(p1.y - p2.y);
                } else {
                	return -1;
                }
            }
        });
		
		for(Duo duo : allDuos) {
			if (duosAlreadyWithTarget.contains(duo)) {
				continue;
			}
			
			duos.add(duo);
		}
		return duos;
	}
	
	SortedSet<Point> getEnemiesByDistanceToPoint(final Point p) {
		SortedSet<Point> enemies = new TreeSet<Point>(new Comparator<Point>() {
            @Override
            public int compare(Point o1, Point o2) {
            	int distToMyBase1 = distance(o1 , p);
            	int distToMyBase2 = distance(o2, p);
                int diff = (distToMyBase1 - distToMyBase2);
                if (diff > 0) {
                	return 1;
                } else if (diff == 0) {
                	return (o1.x - o2.x) + 100*(o1.y - o2.y);
                } else {
                	return -1;
                }
            }
        });
		
		for(int i = 0; i < 4; i++) {
			if (i == this.id) {
				continue;
			}
			
			for (Pair pr : playersOutposts.get(i)) {
				Point enemy = getGridPoint(pr);
				if (alreadySelectedDuosTargets.contains(enemy)) {
					continue;
				}
				enemies.add(enemy);
			}
			
			Point enemyBase = getGridPoint(playersBase.get(i));
			if (alreadySelectedDuosTargets.contains(enemyBase)) {
				continue;
			}
			enemies.add(enemyBase);
		}
		return enemies;
	}
	
	SortedSet<Point> getEnemiesCloserThanDist(final Point outpost, int dist) {
		SortedSet<Point> enemies = new TreeSet<Point>(new Comparator<Point>() {
            @Override
            public int compare(Point o1, Point o2) {
            	int distToMyBase1 = distance(o1 , outpost);
            	int distToMyBase2 = distance(o2, outpost);
                int diff = (distToMyBase1 - distToMyBase2);
                if (diff > 0) {
                	return 1;
                } else if (diff == 0) {
                	return (o1.x - o2.x) + 100*(o1.y - o2.y);
                } else {
                	return -1;
                }
            }
        });
		
		for (int i = Math.max(0, outpost.x - dist); i < Math.min(SIDE_SIZE, outpost.x + dist + 1); i++) {
			for (int j = Math.max(0, outpost.y - dist); j < Math.min(SIDE_SIZE, outpost.y + dist + 1); j++) {
				Point p = getGridPoint(i, j);
				Integer playerId = pointToPlayer.get(p);
				if (playerId != null && playerId != id) {
					enemies.add(p);
				}
			}
		}
		return enemies;
	}
	
	Point nextPositionToGetToPosition(Point source, Point destination) {
		source = getGridPoint(source);
		destination = getGridPoint(destination);
		if (source.equals(destination)) {
			return destination;
		}
		
		ArrayList<Point> path = buildPath(source, destination);
		
//		System.out.printf("From %s to %s: move to %s\n", pointToString(source), pointToString(destination), pointToString(path.get(1)));
//		System.out.println(path.get(1).water);
//		System.out.println(getGridPoint(path.get(1)).water);
		return path.get(1);
	}
	
	public ArrayList<Point> buildPath(Point source, Point destination) {
		source = getGridPoint(source);
		destination = getGridPoint(destination);
		
		HashMap<Point, Point> parent = new HashMap<Point, Point>();
		ArrayList<Point> discover = new ArrayList<Point>();
		Set<Point> visited = new HashSet<Point>();
		discover.add(source);

		while(true)
		{
			if(discover.size()!=0)
			{
				Point current = discover.remove(0);
				
//				System.out.println(this.id+" analyzing: "+current.x+" "+current.y);
				visited.add(current);
				
				if (equal(current, destination))
				{
//					System.out.println("Found destination");
					break;
				}
				
				ArrayList<Point> validNeighbors = neighborPoints(current);
				Collections.shuffle(validNeighbors);
				
				// Move on the coordinate with highest difference first
//				final Point destinationFinal = destination;
//				Collections.sort(validNeighbors, new Comparator<Point>() {
//		            @Override
//		            public int compare(Point o1, Point o2) {
//		            	int p1 = Math.max(Math.abs(o1.x - destinationFinal.x), Math.abs(o1.y - destinationFinal.y));
//		            	int p2 = Math.max(Math.abs(o2.x - destinationFinal.x), Math.abs(o2.y - destinationFinal.y));
//		                int diff = p1 - p2;
//		                if (diff > 0) {
//		                	return 1;
//		                } else if (diff == 0) {
//		                	return (o1.x - o2.x) + 100*(o1.y - o2.y);
//		                } else {
//		                	return -1;
//		                }
//		            }
//		        });
	        
				for (Point p: validNeighbors)
				{
					if (p.water) {
						continue;
					}
					if (visited.contains(p)) {
						continue;
					}
					if (discover.contains(p)) {
						continue;
					}
					
//					if(p.ownerlist.size() == 0 || p.ownerlist.get(0).x==this.id) 
//					{
//						continue;
//					}
					
					discover.add(p);
					parent.put(p, current);
				}
			}
			else 
			{
				System.out.printf("No Path from %s to %s\n", pointToString(source), pointToString(destination));
				return null;			
			}
		}
		
		ArrayList<Point> path = new ArrayList<Point>();
		Point p = destination;
		while(true) {
			path.add(p);
			if (p.equals(source)) {
				break;
			}
			p = parent.get(p);
		}
		Collections.reverse(path);
		
//		for (Point p2 : path) {
//			System.out.println(pointToString(p2));
//		}
		
		return path;
	}
	
	
	//Method to find the best resource cell an outpost can move to 
	public boolean addResourceOutpostToMovelist(ArrayList<movePair> movelist, int outpostId)
	{
		Pair chosen_move = null;
		//first find if we can move to an exclusive cell with most access to water
		chosen_move = getExclusiveBestCellAroundWater(outpostId);
	
		if (chosen_move==null) //if no such exclusive cell is found, we search for alternates
		{
			System.out.println("Could not find exclusive water");

			chosen_move = getAlternateResource(outpostId);
		}
		
		if (chosen_move == null) {
			System.out.println("chosen_move is null");
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {}
			return false;
		}
		next_moves.add(chosen_move);

		
		try { //because sometimes throws null exception
			Point nextPosition = nextPositionToGetToPosition(getGridPoint(myOutposts.get(outpostId)), new Point(chosen_move.x,chosen_move.y,false));
			movelist.add(new movePair(outpostId, pointToPair(nextPosition)));
			Resource newResource = markFieldInRadius(nextPosition);
			totalResourceGuaranteed.water += newResource.water;
			totalResourceGuaranteed.land += newResource.land;
		} catch(Exception E){}
		
		return true;
	}
	
	//Finding an exclusive cell which has the best water resource accessibility but also satisfies the land requirements
	public Pair getExclusiveBestCellAroundWater(int index)
	{
		Pair closest_best = null;
		int max_water = 0;
		int min_dist = Integer.MAX_VALUE;
		for (Cell check: board_scored)
		{
			if(getGridPoint(check.cell.x, check.cell.y).water)
				continue;
			if(check.land >= L_PARAM)  //Ensuring that we go to a cell with enough land cells around for generating new outposts
			{
				if (check.water > max_water || (check.water == max_water &&  distance(new Pair(check.cell.x, check.cell.y), myOutposts.get(index)) < min_dist))
				{
					if(tooCloseToOtherOutpost(index, new Pair(check.cell.x, check.cell.y)))
						continue;
					min_dist = distance(new Pair(check.cell.x, check.cell.y), myOutposts.get(index));
					max_water = check.water;
					closest_best = new Pair(check.cell.x, check.cell.y);
				}
			}
		}
		return closest_best;
	}
	
	//without worrying about the land requirements, find a cell with best water accessibility - NOT BEING USED RIGHT NOW
	public Pair getLastResortBestCellAroundWater(int index)
	{
		Pair closest_best = null;
		int max_water = 0;
		int min_dist = Integer.MAX_VALUE;
		for (Cell check: board_scored)
		{
			if(getGridPoint(check.cell.x, check.cell.y).water)
				continue;
			if (check.water > max_water || (check.water == max_water &&  distance(new Pair(check.cell.x, check.cell.y), myOutposts.get(index)) < min_dist))
			{
				if(tooCloseToOtherOutpost(index, new Pair(check.cell.x, check.cell.y)))
					continue;
				min_dist = distance(new Pair(check.cell.x, check.cell.y), myOutposts.get(index));
				max_water = check.water;
				closest_best = new Pair(check.cell.x, check.cell.y);
			}
		}
		return closest_best;
	}
	
	//Find an alternate resource cell if we cant find an exclusive resource cell
	public Pair getAlternateResource(int index)
	{
		Pair chosen_move = null;
		
		//check if there is water reachable by the end of this season
		if (!waterWithinLimit(index))
		{
			//if not, go to the cell with the best water score which is closest to the outpost
			chosen_move = getClosestBestCellAroundWater(index);
			return chosen_move;
		}
		
		//if we have access to water within this season
		
		//get the best ratio cell which has exclusive access
		chosen_move = getSeasonBestRatioCellForOutpostId(index);
		
		if (chosen_move == null)
		{
			chosen_move = getClosestBestCellAroundWater(index); //get the best cell around water without worrying about exclusivity
		}
		
		 if (chosen_move == myOutposts.get(index))
		 {
			if (clustered(index)) //if there is a chance of clustering, move towards the an unoccupied resource cell
				chosen_move = getUnoccupiedResourceForOutpostId(index);
		 }
		 
		 if (chosen_move == null) //if couldn't find unoccupied resource cell, move to the farthest outpost from base
			 chosen_move = farthestOutpost(myOutposts);
		 
		 //if the water access from present location is better than if we move to our new target
		 if (waterSurround(myOutposts.get(index)) > waterSurround(chosen_move))
		 {
			 if (!tooCloseToOtherOutpost(index, myOutposts.get(index))) //if we are not too close to others
				 chosen_move = myOutposts.get(index); //we don't move to new target
		 }

		return chosen_move;
	}
	
	//find the closest cell which has the best water access, without worrying about exclusivity
	public Pair getClosestBestCellAroundWater(int index)
	{
		Pair closest_best = null;
		int max_water = 0;
		int min_dist = Integer.MAX_VALUE;
		for (Cell check: board_scored)
		{
			if(getGridPoint(check.cell.x, check.cell.y).water)
				continue;

			if (check.water > max_water || (check.water == max_water &&  distance(new Pair(check.cell.x, check.cell.y), myOutposts.get(index)) < min_dist))
			{
					min_dist = distance(new Pair(check.cell.x, check.cell.y), myOutposts.get(index));
					max_water = check.water;
			}
		}
		return closest_best;
	}
	
	
	//returns the "best" closest cell BASED ON RATIO to get to within this season
	public Pair getSeasonBestRatioCellForOutpostId(int index)
	{
		Pair p = myOutposts.get(index);
		Pair best_cell = null;
		double req_ratio = L_PARAM/W_PARAM; //this is our required Land to Water ratio
		double best_ratio = -1;
		int best_land=0;
		int best_water=0;
		double limit = 10 - (tickCounter%10); //the number of ticks left until the season ends - this decides how many steps we can move before the season ends

		for (int b = 0; b<board_scored.size(); b++) //loop through the scored cells
		{
			Cell k = board_scored.get(b);
			if (getGridPoint(k.cell.x, k.cell.y).water) {
                continue;
			}
			//the "too_close" boolean is for determining if this cell would be close to either one
			//of the next moves of other outposts or close to other outposts
			boolean too_close = false;
			
			//checking with the next decided targets of other outposts (if any)
			if(next_moves.size()>0){
			for (int i=0; i < next_moves.size(); i++)
				if(distance(new Pair(k.cell.x, k.cell.y), next_moves.get(i)) < RADIUS)
				{
					too_close=true;
					break;
				}
			}
			
			if(too_close)
				continue;
			
			too_close = tooCloseToOtherOutpost(index, new Pair(k.cell.x, k.cell.y));
			
			if(too_close)
				continue;
			
			//check if the cell can be reached within the end of this season
			if (distance(new Pair(k.cell.x, k.cell.y), p) < limit)
			{
				if(k.water!=0) //to avoid Math errors
				if ((Math.abs(req_ratio - (k.land/k.water)) < Math.abs(req_ratio - best_ratio)) || (k.land >= best_land && k.water >= best_water)) //the second condition is for edge cases
				{
					best_ratio = k.land/(k.water);
					best_cell = new Pair(k.cell.x, k.cell.y);
					best_land = k.land;
					best_water = k.water;
				}
			}
		}

		return best_cell;
	}
	
	//get one resource cell which has NOT been occupied by some other outpost on the board
	public Pair getUnoccupiedResourceForOutpostId(int index)
	{
		ArrayList<Pair> myOutposts = playersOutposts.get(this.id);
		Pair p = myOutposts.get(index);
		Pair best_cell = new Pair(-1, -1);
		double limit = 10 - (tickCounter%10); //the number of ticks left until the season ends - this decides how many steps we can move before the season ends

		for (int b = 0; b<board_scored.size(); b++) //loop through the scored cells
		{
			Cell k = board_scored.get(b);
			if (getGridPoint(k.cell.x, k.cell.y).water)
				continue;
			
			//the "too_close" boolean is for determining if this cell would be close to either one
			//of the next moves of other outposts or close to other outposts
			boolean too_close = false;
			
			//checking with the next decided targets of other outposts (if any)
			if(next_moves.size()>0){
			for (int i=0; i<next_moves.size(); i++)
				if(distance(new Pair(k.cell.x, k.cell.y), next_moves.get(i)) < 2*RADIUS)
				{
					too_close=true;
					break;
				}
			}
			
			if(too_close)
				continue;
			
			too_close = tooCloseToOtherOutpost(index, new Pair(k.cell.x, k.cell.y));

			if(too_close)
				continue;
			
			//check if the cell can be reached within the end of this season
			if (distance(new Pair(k.cell.x, k.cell.y), p) < limit)
			{
				best_cell = new Pair(k.cell.x, k.cell.y);
				break;
			}
		}
		return best_cell;
	}
	
	//find the closest water cell to a given outpost - NOT BEING USED RIGHT NOW
	public Pair findClosestWaterCell(Pair p)
	{
		double min_dist = Integer.MAX_VALUE;
		Pair closestWater = new Pair();
		for(int i=0; i<100; i++)
		{
			for(int j=0; j<100; j++)
			{
				if(getGridPoint(i,j).water && (distance(new Pair(i,j), p)<min_dist))
				{
					min_dist = distance(new Pair(i,j), p);
					closestWater = new Pair(i,j);
				}
			}
		}
		return closestWater;
	}
	
	//find the farthest outpost to our base
	public Pair farthestOutpost(ArrayList<Pair> myOutposts)
	{
		double max_dist = Double.NEGATIVE_INFINITY;
		Pair farthest = new Pair();
		for (Pair p: myOutposts)
		{
			if (distance(p, playersBase.get(id)) > max_dist)
			{
				max_dist = distance(p, playersBase.get(id));
				farthest = new Pair(p.x, p.y);
			}
		}
		return farthest;
	}
	
	//HELPER FUNCTIONS FOR RESOURCE STRATEGY
	
	//Get how much water (in ideal situation) would an outpost have if it is on 'p'
	public int waterSurround(Pair p)
	{
		for(Cell i: board_scored)
		{
			if ((i.cell.x==p.x) && (i.cell.y==p.y))
				return i.water;
		}
		return 0;
	}
	
	//find if there is any water within the season limit of an outpost
	public boolean waterWithinLimit(int index)
	{
		boolean found_water = false;
		int a = myOutposts.get(index).x-10;
		int b = myOutposts.get(index).y-10;
		int x = a+20;
		int y = b+20;
		for (int i = a; i<x; i++)
		{
			for (int j = b; j<y; j++)
			{
				if ((i>=0) && (i<100) && (j>=0) && (j<100))
				{
					if (getGridPoint(i,j).water)
					{
						found_water=true;
						break;
					}
				}
			}
		}
		return found_water;
	}
	
	//check if an outpost may get clustered with some other outpost of ours
	public boolean clustered(int index)
	{
		boolean clustered = false;
		for(int other=0; other<myOutposts.size(); other++)
		{
			if (index == other)
				continue;
		    if (distance(new Pair(myOutposts.get(index).x, myOutposts.get(index).y),myOutposts.get(other)) < RADIUS)
				{
		    		clustered = true;
					break;
				}
		}
		return clustered;
	}
	
	//check if an outpost will get too close to any other outpost on the board
	public boolean tooCloseToOtherOutpost(int index, Pair p)
	{
		boolean too_close = false;
		for(int t=0; t < playersOutposts.size(); t++)
		{
				for (int i=0; i < playersOutposts.get(t).size(); i++)
				{
					if ((i==index) && (this.id==t))
						continue;
					
					if(distance(p, playersOutposts.get(t).get(i)) < RADIUS)
					{
						too_close=true;
						break;
					}
				}
		}
		return too_close;
	}
	

	//evaluate each cell on the board - used for later stuff in the code to find
	//which cell is more "attractive" for the outposts
	public ArrayList<Cell> evaluateBoard(int r)
	{
		ArrayList<Cell> board_eval = new ArrayList<Cell>();
		for(int i=0; i<100; i++)
		{
			for(int j=0; j<100; j++)
			{
				Point eval_cell = getGridPoint(i, j);
				board_eval.add(new Cell(eval_cell));
				for(int k=0; k<100; k++)
				{
					for(int l=0; l<100; l++)
					{
						Point test_cell = getGridPoint(k, l);
						if (distance(eval_cell, test_cell) < r)
						{
							if (test_cell.water)
							{
								board_eval.get(board_eval.size()-1).water++;
							}
							else
							{
								board_eval.get(board_eval.size()-1).land++;
							}
						}
					}
				}
			}
		}
		return board_eval;
	}

	ArrayList<Point> neighborPoints(Point start) {
		ArrayList<Point> prlist = new ArrayList<Point>();
		Point p = new Point(start);
		
		p.x = start.x - 1;
		p.y = start.y;
		if (isPointInsideGrid(p)) {
			prlist.add(getGridPoint(p));
		}
		
		p.x = start.x + 1;
		p.y = start.y;
		if (isPointInsideGrid(p)) {
			prlist.add(getGridPoint(p));
		}
		
		p.x = start.x;
		p.y = start.y - 1;
		if (isPointInsideGrid(p)) {
			prlist.add(getGridPoint(p));
		}
		
		p.x = start.x;
		p.y = start.y + 1;
		if (isPointInsideGrid(p)) {
			prlist.add(getGridPoint(p));
		}
		return prlist;
	}
	
	boolean isPointInsideGrid(Point p) {
		if (p.x < 0 || p.x >= SIDE_SIZE) {
			return false;
		}
		if (p.y < 0 || p.y >= SIDE_SIZE) {
			return false;
		}
		return true;
	}

	Point getGridPoint(int x, int y) { return grid[x * SIDE_SIZE + y]; }
	Point getGridPoint(Pair pr) { return grid[pr.x * SIDE_SIZE + pr.y]; }
	Point getGridPoint(Point p) { return grid[p.x * SIDE_SIZE + p.y]; }

	Pair pointToPair(Point pt) { return new Pair(pt.x, pt.y); }
	
	int distance(Point a, Point b) {	return Math.abs(a.x-b.x)+Math.abs(a.y-b.y); }
	int distance(Point a, Pair b) {	return Math.abs(a.x-b.x)+Math.abs(a.y-b.y); }
	int distance(Pair a, Point b) {	return Math.abs(a.x-b.x)+Math.abs(a.y-b.y); }
	int distance(Pair a, Pair b) {	return Math.abs(a.x-b.x)+Math.abs(a.y-b.y); }
	
	double euclidianDistance(Point a, Point b) {	return Math.sqrt((a.x-b.x) * (a.x-b.x) + (a.y-b.y) * (a.y-b.y)); }
	double euclidianDistance(Point a, Pair b) {	return Math.sqrt((a.x-b.x) * (a.x-b.x) + (a.y-b.y) * (a.y-b.y)); }
	double euclidianDistance(Pair a, Point b) {	return Math.sqrt((a.x-b.x) * (a.x-b.x) + (a.y-b.y) * (a.y-b.y)); }
	double euclidianDistance(Pair a, Pair b) {	return Math.sqrt((a.x-b.x) * (a.x-b.x) + (a.y-b.y) * (a.y-b.y)); }
	
	boolean equal(Pair a, Point b) { return a.x == b.x && a.y==b.y; }
	boolean equal(Pair a, Pair b) { return a.x == b.x && a.y==b.y; }
	boolean equal(Point a, Pair b) { return a.x == b.x && a.y==b.y; }
	boolean equal(Point a, Point b) { return a.x == b.x && a.y==b.y; }
	
	String pointToString(Point p) { return "" + p.x + ", " + p.y; }
	
	class Resource {
		int water;
		int land;
		
		public Resource(int w, int l) {
			this.water = w;
			this.land = l;
		}
		
		public boolean isMoreThan(Resource needed) {
			if (this.water > needed.water) {
				if (this.land > needed.land) {
					return true;
				}
			}
			return false;
		}
	}
	
	class Duo {
		int p1, p2;
		
		public Duo(int leaderId, int followerId) {
			this.p1 = leaderId;
			this.p2 = followerId;
		}
		
		public String toString() {
			return "["+p1+","+p2+"]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + p2;
			result = prime * result + p1;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Duo other = (Duo) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (p2 != other.p2)
				return false;
			if (p1 != other.p1)
				return false;
			return true;
		}

		private Player getOuterType() {
			return Player.this;
		}
	}
}

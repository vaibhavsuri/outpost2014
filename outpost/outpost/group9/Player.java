package outpost.group9;

import java.util.*;

import outpost.sim.Pair;
import outpost.sim.Point;
import outpost.sim.movePair;

public class Player extends outpost.sim.Player {
	final static int SIDE_SIZE = 100;
	final static int TOO_CLOSE = 30;

	boolean playerInitialized;
	int RADIUS;
	int L_PARAM;
	int W_PARAM;
	int MAX_TICKS;
	
	// utility stuff
	Point[] grid = new Point[SIDE_SIZE * SIDE_SIZE];
	List<ArrayList<Point>> playersOutposts = new ArrayList<ArrayList<Point>>();
	HashMap<Point, OutpostId> ownerGrid = new HashMap<Point, OutpostId>();
	List<Point> playersBase;
	List<Point> playersSecondBase;
	List<Point> playersLeftTopCorner;
	ArrayList<Point> myOutposts;
	Point myBase;
	HashMap<Point, ArrayList<OutpostId>> pointToOutposts = new HashMap<Point, ArrayList<OutpostId>>();
	
	int tickCounter = 0;
	
	// duo strategy stuff
	ArrayList<Integer> outpostsForDuoStrategy = new ArrayList<Integer>();
	ArrayList<Duo> allDuos = new ArrayList<Duo>();
	Set<Point> alreadySelectedDuosTargets = new HashSet<Point>();
	Set<Duo> duosAlreadyWithTarget = new HashSet<Duo>();
	Set<Point> duosPointsOnEnemyBase = new HashSet<Point>();
	Set<Point> waitingToMove = new HashSet<Point>();
	
	// resource strategy stuff
	ArrayList<Point> next_moves;
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
				pointToOutposts.put(grid[i], new ArrayList<OutpostId>());
			}

			RADIUS = r;
			L_PARAM = L;
			W_PARAM = W;
			MAX_TICKS = T;
			
			playersBase = Arrays.asList(getGridPoint(0, 0), getGridPoint(99, 0), getGridPoint(99, 99), getGridPoint(0, 99));
			playersSecondBase = Arrays.asList(getGridPoint(4, 4), getGridPoint(94, 4), getGridPoint(94, 94), getGridPoint(4, 94));
			playersLeftTopCorner = Arrays.asList(getGridPoint(0, 0), getGridPoint(100 - TOO_CLOSE, 0), getGridPoint(100 - TOO_CLOSE, 100 - TOO_CLOSE), getGridPoint(0, 100 - TOO_CLOSE));
			myBase = getGridPoint(playersBase.get(id));
			for (int i = 0; i < 4; i++) {
				playersOutposts.add(new ArrayList<Point>());
			}
			
			//on the first tick, evaluate all the cells the board and store the scores in 
			//board_scored
			board_scored = evaluateBoard(r);
			
			playerInitialized = true;
		}
		for (int i = 0; i < 4; i++) {
			playersOutposts.get(i).clear();
			for(Pair pr : king_outpostlist.get(i)) {
				playersOutposts.get(i).add(getGridPoint(pr));
			}
		}
		myOutposts = playersOutposts.get(this.id);
		tickCounter++;
		// clear grid and poinToOutposts
		for (int i = 0; i < SIDE_SIZE * SIDE_SIZE; i++) {
			grid[i].ownerlist.clear();
			pointToOutposts.get(grid[i]).clear();
		}
		// init pointToOutposts
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < playersOutposts.get(i).size(); j++) {
				pointToOutposts.get(playersOutposts.get(i).get(j)).add(new OutpostId(playersOutposts.get(i).get(j), j, i));
			}
		}
		// init ownerlist and ownerGrid with enemy points
		ownerGrid.clear();
		for (int i = 0; i < 4; i++) {
			if (i == id) {
				continue;
			}
			for (int j = 0; j < playersOutposts.get(i).size(); j++) {
				markFieldInRadius(playersOutposts.get(i).get(j), j, i);
			}
		}
		Iterator<Point> it = waitingToMove.iterator();
		while(it.hasNext()) {
			Point p = it.next();
			if (pointHasEnemyOutpost(p)) {
				continue;
			}
			it.remove();
		}
		
		
		System.out.printf("---- New tick -----\n");
		ArrayList<movePair> movelist = new ArrayList<movePair>();
		
		//preparation code for the resource strategy
		next_moves = new ArrayList<Point>();
		totalResourceNeeded = new Resource((myOutposts.size())*W_PARAM,(myOutposts.size())*L_PARAM);
		totalResourceGuaranteed = new Resource(0,0);
		
		// preparation code for the duo strategy
		alreadySelectedDuosTargets.clear();
		duosAlreadyWithTarget.clear();
		allDuos.clear();
		outpostsForDuoStrategy.clear();
		duosPointsOnEnemyBase.clear();
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
					
					List<List<Point>> formation1 = Arrays.asList(Arrays.asList(getGridPoint(1, 0), getGridPoint(1, 1)), Arrays.asList(getGridPoint(98, 0), getGridPoint(98, 1)), Arrays.asList(getGridPoint(98, 99), getGridPoint(98, 98)), Arrays.asList(getGridPoint(1, 99), getGridPoint(1, 98)));
					List<List<Point>> formation2 = Arrays.asList(Arrays.asList(getGridPoint(0, 1), getGridPoint(1, 1)), Arrays.asList(getGridPoint(99, 1), getGridPoint(98, 1)), Arrays.asList(getGridPoint(99, 98), getGridPoint(98, 98)), Arrays.asList(getGridPoint(0, 98), getGridPoint(1, 98)));
					
					if(myOutposts.get(follower).equals(formation1.get(i).get(1))) {
						//follower is in pivot position
						if(myOutposts.get(leader).equals(formation1.get(i).get(0)) && pointHasEnemyOutpost(myOutposts.get(leader))) {
							movelist.add(new movePair(leader, pointToPair(formation2.get(i).get(1))));
							movelist.add(new movePair(follower, pointToPair(formation2.get(i).get(0))));
						} else {
							movelist.add(new movePair(leader, pointToPair(formation1.get(i).get(1))));
							movelist.add(new movePair(follower, pointToPair(formation1.get(i).get(0))));
						}
					} else {
						//follower is not in pivot position
						movelist.add(new movePair(leader, pointToPair(formation1.get(i).get(1))));
						movelist.add(new movePair(follower, pointToPair(myOutposts.get(leader))));
					}
					
					break;
				}
			}
		}


//		int outpostId = 0;
//		for (Pair thisOutpost : myOutposts) {
//			System.out.printf("Outpost %d: %d,%d\n", outpostId, thisOutpost.x, thisOutpost.y);
//			outpostId++;
//		}
		
		ArrayList<Point> enemiesInMySide = new ArrayList<Point>();
		for (int x = playersLeftTopCorner.get(id).x; x < playersLeftTopCorner.get(id).x + TOO_CLOSE; x++) {
			for (int y = playersLeftTopCorner.get(id).y; y < playersLeftTopCorner.get(id).y + TOO_CLOSE; y++) {
				Point p = getGridPoint(x, y);
				if (pointHasEnemyOutpost(p)) {
					enemiesInMySide.add(p);
				}
			}
		}
		Collections.sort(enemiesInMySide, new Comparator<Point>() {
            @Override
            public int compare(Point o1, Point o2) {
            	int distToMyBase1 = distance(o1 , playersBase.get(id));
            	int distToMyBase2 = distance(o2, playersBase.get(id));
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
		
		
		
		// decide strategy
		for (int currentOutpostId = myOutposts.size() - 1; currentOutpostId >= 0; currentOutpostId--) {
			Point outpost = getGridPoint(myOutposts.get(currentOutpostId));
			if (duosPointsOnEnemyBase.contains(outpost)) {
				continue;
			}
			
			if (enemiesInMySide.size() != 0 && currentOutpostId -2 >= 0) {
				List<List<Point>> formation1 = Arrays.asList(Arrays.asList(getGridPoint(1, 0), getGridPoint(0, 1)), Arrays.asList(getGridPoint(98, 0), getGridPoint(99, 1)), Arrays.asList(getGridPoint(98, 99), getGridPoint(99, 98)), Arrays.asList(getGridPoint(1, 99), getGridPoint(0, 98)));
				
				movelist.add(new movePair(currentOutpostId, pointToPair(nextPositionToGetToPosition(myOutposts.get(currentOutpostId), myBase))));
				movelist.add(new movePair(currentOutpostId-1, pointToPair(nextPositionToGetToPosition(myOutposts.get(currentOutpostId-1), formation1.get(id).get(0)))));
				movelist.add(new movePair(currentOutpostId-2, pointToPair(nextPositionToGetToPosition(myOutposts.get(currentOutpostId-2), formation1.get(id).get(1)))));
				enemiesInMySide.clear();
				currentOutpostId -= 2;
			} else if (totalResourceGuaranteed.isMoreThan(totalResourceNeeded)) {
				// Duo strategy
				outpostsForDuoStrategy.add(currentOutpostId);
			} else {	
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
		
//		PlayerStatistics.printStats();
		
		return movelist;
	}
	
	Resource markFieldInRadius(Point outpost, int outpostListId, int playerId) {
		Resource newResource = new Resource(0,0);
		for (int i = Math.max(0, outpost.x - RADIUS); i < Math.min(SIDE_SIZE, outpost.x + RADIUS + 1); i++) {
			for (int j = Math.max(0, outpost.y - RADIUS); j < Math.min(SIDE_SIZE, outpost.y + RADIUS + 1); j++) {
				Point p = getGridPoint(i, j);
				int dist = distance(outpost, p);
				if (dist > RADIUS) {
					continue;
				}
				
				p.ownerlist.add(pointToPair(outpost));
				
				OutpostId outpostId = ownerGrid.get(p);
				// update ownerGrid
				if (outpostId == null) {
					ownerGrid.put(p, new OutpostId(outpost, outpostListId, playerId));
				} else {
					if (dist < distance(outpostId.p, p)) {
						ownerGrid.put(p, new OutpostId(outpost, outpostListId, playerId));
					} else if (outpostId.ownerId != playerId && dist == distance(outpostId.p, p)) {
						ownerGrid.remove(p);
//						System.out.printf("Point %s is undecided when considering %s old %s newDist %d\n", pointToString(p), pointToString(outpost), outpostId.toString(), dist);
					}
				}
				
				// add new resource
				if (outpostId == null || (outpostId.ownerId != id && dist < distance(outpostId.p, p))) {
					if (p.water) {
						newResource.water++;
					} else {
						newResource.land++;
					}
				}
				
			}
		}
		return newResource;
	}
	
	Point getClosestOwnerInOwnerlist(Point p) {
		Point closest = null;
		int closestDist = Integer.MAX_VALUE;
		for (Pair pr : p.ownerlist) {
			int dist = distance(pr, p);
			if (closestDist <= dist) {
				closestDist = dist;
				closest = getGridPoint(pr);
			}
		}
		return closest;
	}
	
	void addDuoToMovelist(ArrayList<movePair> movelist, Duo designatedDuo, Point target) {
		duosAlreadyWithTarget.add(designatedDuo);

		Point outpost1 = getGridPoint(myOutposts.get(designatedDuo.p1));
		Point outpost2 = getGridPoint(myOutposts.get(designatedDuo.p2));
		if (distance(outpost1, outpost2) > 1) {
			// get together
			Point outpost1NextPosition = nextPositionToGetToPositionForDuos(outpost1, outpost2);
			movePair next = new movePair(designatedDuo.p1, pointToPair(outpost1NextPosition));
			movelist.add(next);
			Point outpost2NextPosition = nextPositionToGetToPositionForDuos(outpost2, outpost1);
			if (outpost1NextPosition.equals(outpost2NextPosition)) {
				outpost2NextPosition = outpost2;
			}
			movePair next2 = new movePair(designatedDuo.p2, pointToPair(outpost2NextPosition));
			movelist.add(next2);
			return;
		}
		
		System.out.printf("Designated duo %s (%s,%s). Target %s\n", designatedDuo, pointToString(outpost1), pointToString(outpost2), pointToString(target));
		alreadySelectedDuosTargets.add(target);

		//decide who is the leader
		int dist1 = buildPath(outpost1, target).size();
		int dist2 = buildPath(outpost2, target).size();
		int leaderId = designatedDuo.p1;
		int followerId = designatedDuo.p2;
		if (dist1 > dist2) {
			leaderId = designatedDuo.p2;
			followerId = designatedDuo.p1;
		}
		Point leader = getGridPoint(myOutposts.get(leaderId));
		Point follower = getGridPoint(myOutposts.get(followerId));

		Point leaderNextPosition = nextPositionToGetToPositionForDuos(leader, target);
		Point followerNextPosition = leader;
		
		HashMap<Point, OutpostId> copyOwnerGrid = new HashMap<Point, OutpostId>(ownerGrid);
		markFieldInRadius(leaderNextPosition, leaderId, id);
		markFieldInRadius(followerNextPosition, followerId, id);
		
		if (distance(leader, myBase) > distance(target, myBase)) {
			alreadySelectedDuosTargets.remove(target);
		}
		
		recoverdowhile:
		do {
			if (hasWeakSupplyLine(leaderNextPosition, id)) {
				if (waitingToMove.contains(target)) {
					break;
				}
				waitingToMove.add(target);
//				System.out.printf("Was about to move from %s to %s\n", pointToString(leader), pointToString(leaderNextPosition));
//				OutpostId targetPoint = ownerGrid.get(target);
//				if (targetPoint == null) {
//					System.out.printf("null\n");
//				} else {
//					System.out.printf("%s\n", targetPoint.toString());
//				}
//				System.out.println(isPointSafe(target, id));
				
				
				alreadySelectedDuosTargets.remove(target);
				
				leaderNextPosition = leader;
				followerNextPosition = follower;
				ownerGrid = copyOwnerGrid;
				markFieldInRadius(leaderNextPosition, leaderId, id);
				markFieldInRadius(followerNextPosition, followerId, id);
				
				if (!hasWeakSupplyLine(leaderNextPosition, id)) {
//					System.out.printf("Success by staying in place\n");
					PlayerStatistics.STAY_IN_PLACE.counter++;
					break recoverdowhile;
				}
					
					
				ArrayList<Point> validNeighbors = neighborPoints(follower);
				for (Point p: validNeighbors){
					if (p.water) {
						continue;
					}

					leaderNextPosition = follower;
					followerNextPosition = p;
					ownerGrid = copyOwnerGrid;
					markFieldInRadius(leaderNextPosition, leaderId, id);
					markFieldInRadius(followerNextPosition, followerId, id);
					
					if (!hasWeakSupplyLine(leaderNextPosition, id)) {
						break;
					}
				}
				if (!hasWeakSupplyLine(leaderNextPosition, id)) {
//					System.out.printf("Success by checking follower's neighbors\n");
					PlayerStatistics.FOLLOWERS_NEIGHBORS.counter++;
					break recoverdowhile;
				}
				
				
				validNeighbors = neighborPoints(leader);
				for (Point p: validNeighbors){
					if (p.water) {
						continue;
					}

					leaderNextPosition = p;
					followerNextPosition = leader;
					ownerGrid = copyOwnerGrid;
					markFieldInRadius(leaderNextPosition, leaderId, id);
					markFieldInRadius(followerNextPosition, followerId, id);
					
					if (!hasWeakSupplyLine(leaderNextPosition, id)) {
						break;
					}
				}
				if (!hasWeakSupplyLine(leaderNextPosition, id)) {
//					System.out.printf("Success by checking leader's neighbors\n");
					PlayerStatistics.LEADER_NEIGHBORS.counter++;
					break recoverdowhile;
				}
				
				if (distance(leader, myBase) > distance(follower, myBase)) {
					leaderNextPosition = follower;
					followerNextPosition = nextPositionToGetToPosition(follower, myBase);
				} else {
					leaderNextPosition = nextPositionToGetToPosition(leader, myBase);
					followerNextPosition = leader;
				}
				if (!hasWeakSupplyLine(leaderNextPosition, id)) {
//					System.out.printf("Success by going to base\n");
					PlayerStatistics.BASE.counter++;
					break recoverdowhile;
				}
//				System.out.printf("Good luck!\n");
				PlayerStatistics.LUCK.counter++;
			}
		} while(false);
		
//		int radius = 1;
//		if(pointAlreadyHasMineOutpost(leaderNextPosition)) {
//			for (int i = Math.max(0, target.x - radius); i < Math.min(SIDE_SIZE, target.x + radius + 1); i++) {
//				for (int j = Math.max(0, target.y - radius); j < Math.min(SIDE_SIZE, target.y + radius + 1); j++) {
//					Point p = getGridPoint(i, j);
//					if (p.water) {
//						continue;
//					}
//					if (pointToOutposts.get(p).size()!=0) {
//						continue;
//					}
//
//	
//					leaderNextPosition = nextPositionToGetToPositionForDuos(leader, p);
//					followerNextPosition = nextPositionToGetToPositionForDuos(follower, leader);
//				}
//			}
//			radius++;
//		}
		
		if(!leaderNextPosition.equals(leader) && !leaderNextPosition.equals(follower)) {
			if (pointAlreadyHasMineOutpost(leaderNextPosition)) {
				if (distance(leader, playersSecondBase.get(id)) > distance(follower, playersSecondBase.get(id))) {
					leaderNextPosition = follower;
					followerNextPosition = nextPositionToGetToPosition(follower, playersSecondBase.get(id));
				} else {
					leaderNextPosition = nextPositionToGetToPosition(leader, playersSecondBase.get(id));
					followerNextPosition = leader;
				}
			}
		}
		
		ownerGrid = copyOwnerGrid;
		markFieldInRadius(leaderNextPosition, leaderId, id);
		markFieldInRadius(followerNextPosition, followerId, id);
		movelist.add(new movePair(leaderId, pointToPair(leaderNextPosition)));
		movelist.add(new movePair(followerId, pointToPair(followerNextPosition)));
	}
	
	public boolean pointAlreadyHasMineOutpost(Point p)	{
		ArrayList<OutpostId> outpostInP = pointToOutposts.get(p);
		for (OutpostId outpostIdLoop : outpostInP) {
			if (outpostIdLoop.ownerId == id) {
				return true;
			}
		}
		return false;
	}
	
	public boolean pointHasEnemyOutpost(Point p)	{
		ArrayList<OutpostId> outpostInP = pointToOutposts.get(p);
		for (OutpostId outpostIdLoop : outpostInP) {
			if (outpostIdLoop.ownerId != id) {
				return true;
			}
		}
		return false;
	}
	
	SortedSet<Duo> getDuosByDistanceToPoint(final Point p) {
		SortedSet<Duo> duos = new TreeSet<Duo>(new Comparator<Duo>() {
            @Override
            public int compare(Duo o1, Duo o2) {
            	Point p1 = playersOutposts.get(id).get(o1.p1);
            	Point p2 = playersOutposts.get(id).get(o2.p1);
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
            	Point p1 = playersOutposts.get(id).get(o1.p1);
            	Point p2 = playersOutposts.get(id).get(o2.p1);
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
			
			for (Point p2 : playersOutposts.get(i)) {
				Point enemy = getGridPoint(p2);
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
				if (pointToOutposts.get(p).size() != 0 && !pointAlreadyHasMineOutpost(p)) {
					enemies.add(p);
				}
			}
		}
		return enemies;
	}

	
	public boolean isPointSafe(Point p, int playerId) {
		OutpostId closest = null;
		int closestDist = Integer.MAX_VALUE;
		boolean tie = false;
		for(Pair pr : p.ownerlist) {
			Point prPoint = getGridPoint(pr);
			ArrayList<OutpostId> prPointOwner = pointToOutposts.get(prPoint);
			for (OutpostId candidate : prPointOwner) {
				int candidateDist = distance(candidate.p, p);
				if(candidate.ownerId != playerId) {
					candidateDist = Math.max(0, candidateDist-1);
				}
				if (candidateDist < closestDist) {
					closest = candidate;
					closestDist = candidateDist;
					tie = false;
				}
				if (candidateDist == closestDist && closest.ownerId != playerId) {
					tie = true;
				}
			}
		}
		if(closest != null && closest.ownerId != playerId) {
			return false;
		}
		if(tie) {
			return false;
		}
		return true;
	}
	
	public boolean hasWeakSupplyLine(Point source, int playerId) {
		source = getGridPoint(source);
		Point destination = playersBase.get(playerId);
//		HashMap<Point, Point> parent = new HashMap<Point, Point>();
		ArrayList<Point> discover = new ArrayList<Point>();
		Set<Point> visited = new HashSet<Point>();
		discover.add(source);

		while(true)
		{
			if(discover.size()!=0)
			{
				Point current = discover.remove(0);
				visited.add(current);
//				System.out.println(this.id+" analyzing: "+current.x+" "+current.y);
				
				if (equal(current, destination)) {
//					System.out.println("Found destination");
					break;
				}
				
				ArrayList<Point> validNeighbors = neighborPoints(current);
				Collections.shuffle(validNeighbors);
				for (Point p: validNeighbors){
					if (p.water) {
						continue;
					}
					if (visited.contains(p)) {
						continue;
					}
					if (discover.contains(p)) {
						continue;
					}
					
					
					if (!isPointSafe(p, playerId)) {
						continue;
					}
					
//					OutpostId outpostId = ownerGrid.get(p);
//					if (outpostId == null && p.ownerlist.size() != 0) {
//						continue;
//					}
//					if (outpostId != null && outpostId.ownerId != playerId) {
//						continue;
//					}
					
					discover.add(p);
//					parent.put(p, current);
				}
			}
			else {
//				System.out.printf("No Path from %s to %s\n", pointToString(source), pointToString(destination));
				return true;			
			}
		}
		
		return false;
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
				visited.add(current);
//				System.out.println(this.id+" analyzing: "+current.x+" "+current.y);
				
				if (equal(current, destination)) {
//					System.out.println("Found destination");
					break;
				}
				
				ArrayList<Point> validNeighbors = neighborPoints(current);
				Collections.shuffle(validNeighbors);
				for (Point p: validNeighbors){
					if (p.water) {
						continue;
					}
					if (visited.contains(p)) {
						continue;
					}
					if (discover.contains(p)) {
						continue;
					}
					
					discover.add(p);
					parent.put(p, current);
				}
			}
			else {
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
	
	Point nextPositionToGetToPositionForDuos(Point source, Point destination) {
		source = getGridPoint(source);
		destination = getGridPoint(destination);
		if (source.equals(destination)) {
			return destination;
		}
		
		ArrayList<Point> path = buildPathForDuos(source, destination);
		if (path== null) {
			path = buildPath(source, destination);
		}
		
//		System.out.printf("From %s to %s: move to %s\n", pointToString(source), pointToString(destination), pointToString(path.get(1)));
//		System.out.println(path.get(1).water);
//		System.out.println(getGridPoint(path.get(1)).water);
		return path.get(1);
	}
	
	public ArrayList<Point> buildPathForDuos(Point source, Point destination) {
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
				visited.add(current);
//				System.out.println(this.id+" analyzing: "+current.x+" "+current.y);
				
				if (equal(current, destination)) {
//					System.out.println("Found destination");
					break;
				}
				
				ArrayList<Point> validNeighbors = neighborPoints(current);
				Collections.shuffle(validNeighbors);
				for (Point p: validNeighbors){
					if (p.water) {
						continue;
					}
					if (visited.contains(p)) {
						continue;
					}
					if (discover.contains(p)) {
						continue;
					}
					
					if(!p.equals(destination) && pointToOutposts.get(p).size() != 0) {
						continue;
					}
					
					discover.add(p);
					parent.put(p, current);
				}
			}
			else {
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
		Point chosen_move = null;
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
			Resource newResource = markFieldInRadius(nextPosition, outpostId, id);
			totalResourceGuaranteed.water += newResource.water;
			totalResourceGuaranteed.land += newResource.land;
		} catch(Exception E){
			E.printStackTrace();
		}
		
		return true;
	}
	
	//Finding an exclusive cell which has the best water resource accessibility but also satisfies the land requirements
	public Point getExclusiveBestCellAroundWater(int index)
	{
		Point closest_best = null;
		int max_water = 0;
		int min_dist = Integer.MAX_VALUE;
		for (Cell check: board_scored)
		{
			if(getGridPoint(check.cell.x, check.cell.y).water)
				continue;
			if(check.land >= L_PARAM)  //Ensuring that we go to a cell with enough land cells around for generating new outposts
			{
				if (check.water > max_water || (check.water == max_water &&  distance(getGridPoint(check.cell.x, check.cell.y), myOutposts.get(index)) < min_dist))
				{
					if(tooCloseToOtherOutpost(index, getGridPoint(check.cell)))
						continue;
					min_dist = distance(getGridPoint(check.cell), myOutposts.get(index));
					max_water = check.water;
					closest_best = getGridPoint(check.cell.x, check.cell.y);
				}
			}
		}
		return closest_best;
	}
	
	//without worrying about the land requirements, find a cell with best water accessibility - NOT BEING USED RIGHT NOW
	public Point getLastResortBestCellAroundWater(int index)
	{
		Point closest_best = null;
		int max_water = 0;
		int min_dist = Integer.MAX_VALUE;
		for (Cell check: board_scored)
		{
			if(getGridPoint(check.cell.x, check.cell.y).water)
				continue;
			if (check.water > max_water || (check.water == max_water &&  distance(getGridPoint(check.cell.x, check.cell.y), myOutposts.get(index)) < min_dist))
			{
				if(tooCloseToOtherOutpost(index, getGridPoint(check.cell.x, check.cell.y)))
					continue;
				min_dist = distance(getGridPoint(check.cell.x, check.cell.y), myOutposts.get(index));
				max_water = check.water;
				closest_best = getGridPoint(check.cell.x, check.cell.y);
			}
		}
		return closest_best;
	}
	
	//Find an alternate resource cell if we cant find an exclusive resource cell
	public Point getAlternateResource(int index)
	{
		Point chosen_move = null;
		
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
		
		 if (chosen_move.equals(myOutposts.get(index))) {
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
	public Point getClosestBestCellAroundWater(int index)
	{
		Point closest_best = null;
		int max_water = 0;
		int min_dist = Integer.MAX_VALUE;
		for (Cell check: board_scored)
		{
			if(getGridPoint(check.cell.x, check.cell.y).water)
				continue;

			if (check.water > max_water || (check.water == max_water &&  distance(getGridPoint(check.cell), myOutposts.get(index)) < min_dist))
			{
					min_dist = distance(getGridPoint(check.cell), myOutposts.get(index));
					max_water = check.water;
			}
		}
		return closest_best;
	}
	
	
	//returns the "best" closest cell BASED ON RATIO to get to within this season
	public Point getSeasonBestRatioCellForOutpostId(int index)
	{
		Point p = myOutposts.get(index);
		Point best_cell = null;
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
				if(distance(getGridPoint(k.cell.x, k.cell.y), next_moves.get(i)) < RADIUS)
				{
					too_close=true;
					break;
				}
			}
			
			if(too_close)
				continue;
			
			too_close = tooCloseToOtherOutpost(index, getGridPoint(k.cell.x, k.cell.y));
			
			if(too_close)
				continue;
			
			//check if the cell can be reached within the end of this season
			if (distance(getGridPoint(k.cell.x, k.cell.y), p) < limit)
			{
				if(k.water!=0) //to avoid Math errors
				if ((Math.abs(req_ratio - (k.land/k.water)) < Math.abs(req_ratio - best_ratio)) || (k.land >= best_land && k.water >= best_water)) //the second condition is for edge cases
				{
					best_ratio = k.land/(k.water);
					best_cell = getGridPoint(k.cell.x, k.cell.y);
					best_land = k.land;
					best_water = k.water;
				}
			}
		}

		return best_cell;
	}
	
	//get one resource cell which has NOT been occupied by some other outpost on the board
	public Point getUnoccupiedResourceForOutpostId(int index)
	{
		Point p = myOutposts.get(index);
		Point best_cell = null;
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
				if(distance(getGridPoint(k.cell.x, k.cell.y), next_moves.get(i)) < 2*RADIUS)
				{
					too_close=true;
					break;
				}
			}
			
			if(too_close)
				continue;
			
			too_close = tooCloseToOtherOutpost(index, getGridPoint(k.cell.x, k.cell.y));

			if(too_close)
				continue;
			
			//check if the cell can be reached within the end of this season
			if (distance(getGridPoint(k.cell.x, k.cell.y), p) < limit)
			{
				best_cell = getGridPoint(k.cell.x, k.cell.y);
				break;
			}
		}
		return best_cell;
	}
	
	//find the closest water cell to a given outpost - NOT BEING USED RIGHT NOW
	public Point findClosestWaterCell(Pair p)
	{
		double min_dist = Integer.MAX_VALUE;
		Point closestWater = null;
		for(int i=0; i<100; i++)
		{
			for(int j=0; j<100; j++)
			{
				if(getGridPoint(i,j).water && (distance(getGridPoint(i,j), p)<min_dist))
				{
					min_dist = distance(getGridPoint(i,j), p);
					closestWater = getGridPoint(i,j);
				}
			}
		}
		return closestWater;
	}
	
	//find the farthest outpost to our base
	public Point farthestOutpost(ArrayList<Point> myOutposts)
	{
		double max_dist = Double.NEGATIVE_INFINITY;
		Point farthest = new Point();
		for (Point p: myOutposts)
		{
			if (distance(p, playersBase.get(id)) > max_dist)
			{
				max_dist = distance(p, playersBase.get(id));
				farthest = getGridPoint(p.x, p.y);
			}
		}
		return farthest;
	}
	
	//HELPER FUNCTIONS FOR RESOURCE STRATEGY
	
	//Get how much water (in ideal situation) would an outpost have if it is on 'p'
	public int waterSurround(Point p)
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
		    if (distance(getGridPoint(myOutposts.get(index).x, myOutposts.get(index).y),myOutposts.get(other)) < RADIUS)
				{
		    		clustered = true;
					break;
				}
		}
		return clustered;
	}
	
	//check if an outpost will get too close to any other outpost on the board
	public boolean tooCloseToOtherOutpost(int index, Point p)
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
	Point getGridPoint(Cell p) { return grid[p.cell.x * SIDE_SIZE + p.cell.y]; }

	Pair pointToPair(Point pt) { return new Pair(pt.x, pt.y); }
	
	int distance(Point a, Point b) {	return Math.abs(a.x-b.x)+Math.abs(a.y-b.y); }
	int distance(Point a, Pair b) {	return Math.abs(a.x-b.x)+Math.abs(a.y-b.y); }
	int distance(Pair a, Point b) {	return Math.abs(a.x-b.x)+Math.abs(a.y-b.y); }
	int distance(Pair a, Pair b) {	return Math.abs(a.x-b.x)+Math.abs(a.y-b.y); }
	int distance(OutpostId a, OutpostId b) {	return Math.abs(a.p.x-b.p.x)+Math.abs(a.p.y-b.p.y); }
	
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
	
	class OutpostId {
		Point p;
		int outpostId;
		int ownerId;
		public OutpostId(Point p, int outpostId, int ownerId) {
			super();
			this.p = p;
			this.outpostId = outpostId;
			this.ownerId = ownerId;
		}
		@Override
		public String toString() {
			return "OutpostId [p=" + pointToString(p) + ", outpostId=" + outpostId
					+ ", ownerId=" + ownerId + "]";
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + outpostId;
			result = prime * result + ownerId;
			result = prime * result + ((p == null) ? 0 : p.hashCode());
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
			OutpostId other = (OutpostId) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (outpostId != other.outpostId)
				return false;
			if (ownerId != other.ownerId)
				return false;
			if (p == null) {
				if (other.p != null)
					return false;
			} else if (!p.equals(other.p))
				return false;
			return true;
		}
		private Player getOuterType() {
			return Player.this;
		}
		

	}
	public enum PlayerStatistics {
		STAY_IN_PLACE,
		FOLLOWERS_NEIGHBORS,
		LEADER_NEIGHBORS,
		BASE,
		LUCK;
		
		public int counter = 0;
		public static void printStats() {
			for(PlayerStatistics s : PlayerStatistics.values()) {
				System.out.printf("%s: %d,", s, s.counter);
			}
			System.out.printf("\n");
		}
	}
}

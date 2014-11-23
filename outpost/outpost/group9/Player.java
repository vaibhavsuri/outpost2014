package outpost.group9;

import java.util.*;

import outpost.sim.Pair;
import outpost.sim.Point;
import outpost.sim.movePair;

public class Player extends outpost.sim.Player {
	final static int SIDE_SIZE = 100;

	boolean playerInitialized;
	Point[] grid = new Point[SIDE_SIZE * SIDE_SIZE];
	List<ArrayList<Pair>> playersOutposts;
	List<Pair> playersBase = new ArrayList<>(Arrays.asList(new Pair(0,0), new Pair(99, 0), new Pair(99, 99), new Pair(0,99)));;
	int RADIUS;
	int L_PARAM;
	int W_PARAM;
	int MAX_TICKS;
	
	Random random = new Random();
	int tickCounter = 0;
	
	Set<Point> currentTargets = new HashSet<Point>();
	Set<Duo> busyDuos = new HashSet<Duo>();
	
	ArrayList<Duo> allDuos = new ArrayList<Duo>();
	
	public Player(int id_in) {super(id_in);}

	public void init() {}

	public int delete(ArrayList<ArrayList<Pair>> king_outpostlist, Point[] gridin) {
//		int del = random.nextInt(king_outpostlist.get(id).size());
		return king_outpostlist.get(id).size();
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
			
			playerInitialized = true;
		}

		tickCounter++;
		currentTargets.clear();
		busyDuos.clear();
		
		playersOutposts = king_outpostlist;
		for (int i = 0; i < SIDE_SIZE * SIDE_SIZE; i++) {
			grid[i].ownerlist.clear();
		}
		
		ArrayList<Pair> myOutposts = king_outpostlist.get(this.id);
//		int id = 0;
//		for (Pair thisOutpost : myOutposts) {
//			System.out.printf("Outpost %d: %d,%d\n", id, thisOutpost.x, thisOutpost.y);
//			id++;
//		}
		
		ArrayList<movePair> movelist = new ArrayList<movePair>();
		
		//init duos
		allDuos.clear();
		// Make every outpost part of a partner
		for (int j = 0; j < myOutposts.size(); j+= 2) {
			if(j+1 >= myOutposts.size()) {
				// wait for follower
				continue;
			}
			
			int leaderId = j;
			int followerId = j+1;
			allDuos.add(new Duo(leaderId, followerId));
		}
		
		SortedSet<Pair> enemiesByDistToMyGate = getEnemiesByDistanceToPoint(getGridPoint(playersBase.get(id)));
		for (final Pair enemy : enemiesByDistToMyGate) {
			SortedSet<Duo> duos = getDuosByDistanceToPoint(getGridPoint(enemy));
			if (duos.size() == 0) {
				break;
			}
			Duo designatedDuo = duos.first();
			
			System.out.printf("duo %s\n", designatedDuo);
			int leaderId = designatedDuo.leaderId;
			int followerId = designatedDuo.followerId;
			
			Point leader = getGridPoint(myOutposts.get(leaderId));
			Point follower = getGridPoint(myOutposts.get(followerId));
//			Point closestEnemy = getClosestUntargetedEnemy(getGridPoint(playersBase.get(id)));
			Point nextTarget = getGridPoint(enemy);
			if(nextTarget == null) {
				System.out.printf("Duo %s has nothing to do.\n", designatedDuo);
				continue;
			}
			
			
			// don't move past the enemy
			if (distance(nextTarget, leader) != 1) {
				Point leaderNextPosition = nextPositionToGetToPosition(leader, nextTarget);
				movePair next = new movePair(leaderId, pointToPair(leaderNextPosition));
				movelist.add(next);
				
				// if leader is going to follower location
				Point followerNextPosition = nextPositionToGetToPosition(follower, leader);
				if (leaderNextPosition.equals(follower)) {
					Point tempPoint = new Point(follower.x - (leader.x - follower.x), follower.y - (leader.y - follower.y), false);
					if (isPointInsideGrid(tempPoint)) {
						Point positionAwayFromLeader = getGridPoint(tempPoint);
						if (!positionAwayFromLeader.water) {
							followerNextPosition = positionAwayFromLeader;
						}
					}
				}
				movePair next2 = new movePair(followerId, pointToPair(followerNextPosition));
				movelist.add(next2);
			}
			
			busyDuos.add(designatedDuo);
			currentTargets.add(getGridPoint(enemy));
		}
		
		return movelist;
	}
	
	SortedSet<Duo> getDuosByDistanceToPoint(final Point p) {
		SortedSet<Duo> duos = new TreeSet<Duo>(new Comparator<Duo>() {
            @Override
            public int compare(Duo o1, Duo o2) {
            	Pair p1 = playersOutposts.get(id).get(o1.leaderId);
            	Pair p2 = playersOutposts.get(id).get(o2.leaderId);
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
			if (busyDuos.contains(duo)) {
				continue;
			}
			
			duos.add(duo);
		}
		return duos;
	}
	
	SortedSet<Pair> getEnemiesByDistanceToPoint(final Point p) {
		SortedSet<Pair> enemies = new TreeSet<Pair>(new Comparator<Pair>() {
            @Override
            public int compare(Pair o1, Pair o2) {
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
				if (currentTargets.contains(getGridPoint(pr))) {
					continue;
				}
				enemies.add(pr);
			}
			
			Pair pr = playersBase.get(i);
			if (currentTargets.contains(getGridPoint(pr))) {
				continue;
			}
			enemies.add(pr);
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
		
		System.out.printf("From %s to %s: move to %s\n", pointToString(source), pointToString(destination), pointToString(path.get(1)));
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
				
				// Move on the coordinate with highest difference first
				final Point destinationFinal = destination;
				Collections.sort(validNeighbors, new Comparator<Point>() {
		            @Override
		            public int compare(Point o1, Point o2) {
		            	int p1 = Math.max(Math.abs(o1.x - destinationFinal.x), Math.abs(o1.y - destinationFinal.y));
		            	int p2 = Math.max(Math.abs(o2.x - destinationFinal.x), Math.abs(o2.y - destinationFinal.y));
		                int diff = p1 - p2;
		                if (diff > 0) {
		                	return 1;
		                } else if (diff == 0) {
		                	return (o1.x - o2.x) + 100*(o1.y - o2.y);
		                } else {
		                	return -1;
		                }
		            }
		        });
	        
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
	}
	
	class Duo {
		int leaderId, followerId;
		
		public Duo(int leaderId, int followerId) {
			this.leaderId = leaderId;
			this.followerId = followerId;
		}
		
		public String toString() {
			return "["+leaderId+","+followerId+"]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + followerId;
			result = prime * result + leaderId;
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
			if (followerId != other.followerId)
				return false;
			if (leaderId != other.leaderId)
				return false;
			return true;
		}

		private Player getOuterType() {
			return Player.this;
		}
	}
}

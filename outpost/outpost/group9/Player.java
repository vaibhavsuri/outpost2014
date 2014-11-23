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
	List<Pair> playersBase = new ArrayList<>(Arrays.asList(new Pair(0,0), new Pair(99, 0), new Pair(99, 99), new Pair(0,99)));
	List<Pair> playersSecondBase = new ArrayList<>(Arrays.asList(new Pair(5,5), new Pair(94, 5), new Pair(94, 94), new Pair(5,94)));;
	int RADIUS;
	int L_PARAM;
	int W_PARAM;
	int MAX_TICKS;
	
	int tickCounter = 0;
	ArrayList<Pair> myOutposts;
	
	ArrayList<Duo> allDuos = new ArrayList<Duo>();
	Set<Point> currentDuosTargets = new HashSet<Point>();
	Set<Duo> busyDuos = new HashSet<Duo>();
	Set<Point> waitingToMove = new HashSet<Point>();
	
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
		currentDuosTargets.clear();
		busyDuos.clear();
		allDuos.clear();
		
		playersOutposts = king_outpostlist;
		for (int i = 0; i < SIDE_SIZE * SIDE_SIZE; i++) {
			grid[i].ownerlist.clear();
		}
		myOutposts = king_outpostlist.get(this.id);
		
//		int id = 0;
//		for (Pair thisOutpost : myOutposts) {
//			System.out.printf("Outpost %d: %d,%d\n", id, thisOutpost.x, thisOutpost.y);
//			id++;
//		}
		
		ArrayList<movePair> movelist = new ArrayList<movePair>();
		
		// update waitingToMove by removing outposts that moved
		Iterator<Point> it = waitingToMove.iterator();
		whileloop:
		while(it.hasNext()) {
			Point p = it.next();
			for(int i = 0; i < 4; i++) {
				if (i == this.id) {
					continue;
				}
				
				for (Pair pr : playersOutposts.get(i)) {
					if (getGridPoint(pr).equals(p)) {
						continue whileloop;
					}
				}
			}
			it.remove();
		}
		
		
		//init duos
		allDuos.clear();
		currentDuosTargets.clear();
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
		for (Duo duo : allDuos) {
			for(int i = 0; i < 4; i++) {
				Point enemyBase = getGridPoint(playersBase.get(i));
				if (i == id) {
					continue;
				}
				if (currentDuosTargets.contains(enemyBase)) {
					continue;
				}
				
				Pair p1 = myOutposts.get(duo.p1);
				Pair p2 = myOutposts.get(duo.p2);
				if (distance(p1, enemyBase) <= 1 || distance(p2, enemyBase) <= 1) {
					// if a duo is inside an enemy base, keep it
					currentDuosTargets.add(enemyBase);
					busyDuos.add(duo);
				}
			}
		}
		
		// Duos code
		SortedSet<Pair> enemiesByDistToMyBase = getEnemiesByDistanceToPoint(getGridPoint(playersBase.get(id)));
		for (final Pair enemy : enemiesByDistToMyBase) {
			SortedSet<Duo> duos = getDuosByDistanceToPoint(getGridPoint(enemy));
			if (duos.size() == 0) {
				break;
			}
			Duo designatedDuo = duos.first();
			Point target = getGridPoint(enemy);
			if(target == null) {
				System.out.printf("Duo %s has nothing to do.\n", designatedDuo);
				continue;
			}
			addMovePairForDuo(movelist, designatedDuo, target);
		}
		
		return movelist;
	}
	
	void addMovePairForDuo(ArrayList<movePair> movelist, Duo designatedDuo, Point target) {
		System.out.printf("Designated duo %s. Target %s\n", designatedDuo, pointToString(target));
		
		busyDuos.add(designatedDuo);
		currentDuosTargets.add(target);
		
		//decide who is the leader
		int dist1 = distance(myOutposts.get(designatedDuo.p1), target);
		int dist2 = distance(myOutposts.get(designatedDuo.p2), target);
		int leaderId = designatedDuo.p1;
		int followerId = designatedDuo.p2;
		if (dist1 > dist2) {
			leaderId = designatedDuo.p2;
			followerId = designatedDuo.p1;
		}
		Point leader = getGridPoint(myOutposts.get(leaderId));
		Point follower = getGridPoint(myOutposts.get(followerId));
		
		
		int distLeaderToMyBase = distance(playersBase.get(id), leader);
		int distFollowerToMyBase = distance(playersBase.get(id), follower);
		int distTargetToMyBase = distance(playersBase.get(id), target);
		System.out.printf("%d %d %d\n", distLeaderToMyBase, distFollowerToMyBase, distTargetToMyBase);
		if (distance(follower, leader) > 1) {
			// get together
			Point leaderNextPosition = nextPositionToGetToPosition(leader, follower);
			movePair next = new movePair(leaderId, pointToPair(leaderNextPosition));
			movelist.add(next);
			Point followerNextPosition = nextPositionToGetToPosition(follower, leader);
			if (leaderNextPosition.equals(follower)) {
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
			currentDuosTargets.remove(target);
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
			if (waitingToMove.contains(target)) {
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
			if (busyDuos.contains(duo)) {
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
				if (currentDuosTargets.contains(getGridPoint(pr))) {
					continue;
				}
				enemies.add(pr);
			}
			
			Pair pr = playersBase.get(i);
			if (currentDuosTargets.contains(getGridPoint(pr))) {
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

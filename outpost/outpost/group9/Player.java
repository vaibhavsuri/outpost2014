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
	int[] theta = new int[100];
	int tickCounter = 0;
	
	Set<Integer> waterSafers = new HashSet<Integer>();
	int safeWaterSupply = 0;

	int nextNewTheta = 5;


	public Player(int id_in) {
		super(id_in);
	}

	public void init() {
		for (int i = 0; i < 100; i++) {
			theta[i] = random.nextInt(4);
		}
	}

	public int delete(ArrayList<ArrayList<Pair>> king_outpostlist, Point[] gridin) {
		for (int i = 0; i < king_outpostlist.size(); i++) {
			if (!waterSafers.contains(new Integer(i))) {
				return i;
			}
		}
		int del = random.nextInt(king_outpostlist.get(id).size());
		return del;
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
		if (tickCounter % nextNewTheta == 0) {
			for (int i = 0; i < 100; i++) {
				theta[i] = random.nextInt(4);
			}
			nextNewTheta = random.nextInt(50)+1;
		}
		
		playersOutposts = king_outpostlist;
		for (int i = 0; i < SIDE_SIZE * SIDE_SIZE; i++) {
			grid[i].ownerlist.clear();
		}
		
		ArrayList<Pair> myOutposts = king_outpostlist.get(this.id);
		int id = 0;
		for (Pair thisOutpost : myOutposts) {
			System.out.printf("Outpost %d: %d,%d\n", id, thisOutpost.x, thisOutpost.y);
			id++;
		}
		
		ArrayList<movePair> movelist = new ArrayList<movePair>();

		for (int j = 0; j < myOutposts.size(); j+= 2) {
			if(j+1 >= myOutposts.size()) {
				// wait for follower
				continue;
			}
			Point leader = getGridPoint(myOutposts.get(j));
			Point follower = getGridPoint(myOutposts.get(j+1));
			Point closestEnemy = getClosestEnemy(leader);
			if (distance(closestEnemy, leader) != 1) {
				movePair next = new movePair(j, pointToPair(nextPositionToGetToPosition(leader, closestEnemy)));
				movelist.add(next);
				movePair next2 = new movePair(j+1, pointToPair(nextPositionToGetToPosition(follower, leader)));
				movelist.add(next2);
			}
		}
		
		return movelist;
	}
	
	Point getClosestEnemy(Point p) {
		int minDist = Integer.MAX_VALUE;
		Pair minPair = null;
		for(int i = 0; i < 4; i++) {
			if (i == this.id) {
				continue;
			}
			
			for (Pair pr : playersOutposts.get(i)) {
				int dist = distance(pr,p);
				if (dist < minDist) {
					minDist = dist;
					minPair = pr;
				}
			}
			
			Pair pr = playersBase.get(i);
			int dist = distance(pr,p);
			if (dist < minDist) {
				minDist = dist;
				minPair = pr;
			}
		}
		
		
		return getGridPoint(minPair);
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
				
				ArrayList<Point> validNeighbors = surrounds(current);
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
	

	ArrayList<Point> surrounds(Point start) {
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
}

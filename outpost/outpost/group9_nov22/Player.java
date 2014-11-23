package outpost.group9_nov22;

import java.util.*;

import outpost.sim.Pair;
import outpost.sim.Point;
import outpost.sim.movePair;

public class Player extends outpost.sim.Player {
	final static int SIDE_SIZE = 100;

	boolean playerInitialized;
	Point[] grid = new Point[SIDE_SIZE * SIDE_SIZE];
	ArrayList<ArrayList<Pair>> playersOutposts;
	int RADIUS;
	int L_PARAM;
	int W_PARAM;
	int MAX_TICKS;
	
	Random random = new Random();
	int[] theta = new int[100];
	int seasonCounter = 0;
	
	int nextIdToBeFixed = 0;


	public Player(int id_in) {
		super(id_in);
	}

	public void init() {
		for (int i = 0; i < 100; i++) {
			theta[i] = random.nextInt(4);
		}
	}

	public int delete(ArrayList<ArrayList<Pair>> king_outpostlist,
			Point[] gridin) {
		int del = random.nextInt(king_outpostlist.get(id).size());
		return del;
	}

	public ArrayList<movePair> move(ArrayList<ArrayList<Pair>> king_outpostlist, Point[] gridin, int r, int L, int W, int T) {
		if (!playerInitialized) {
			for (int i = 0; i < gridin.length; i++) {
				grid[i] = new Point(gridin[i]);
				RADIUS = r;
				L_PARAM = L;
				W_PARAM = W;
				MAX_TICKS = T;
			}

			playerInitialized = true;
		}

		seasonCounter++;
		if (seasonCounter % 10 == 0) {
			for (int i = 0; i < 100; i++) {
				theta[i] = random.nextInt(4);
			}
		}
		
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

		for (int j = nextIdToBeFixed; j < myOutposts.size(); j++) {
			ArrayList<Pair> positions = new ArrayList<Pair>();
			positions = possibleFuturePositions(myOutposts.get(j));
			boolean gotit = false;
			while (!gotit) {
				if (theta[j] < positions.size()) {
					if (isPairValidPosition(positions.get(theta[j]))) {
						movePair next = new movePair(j, positions.get(theta[j]));
						movelist.add(next);
						gotit = true;
						break;
					}
				}
				theta[j] = random.nextInt(positions.size());
			}
		}

		return movelist;
	}
	
	ArrayList<Resource> computeResources() {
		return null;
	}
	
	Pair nextPositionToGetToPosition(Pair curr, Pair target) {
		//TODO FIX will hit water
		Pair nextPosition = null;
		int xDiff = target.x - curr.x;
		int yDiff = target.y - curr.y;
		if (Math.abs(xDiff) > Math.abs(yDiff)) {
			if (target.x > curr.x) {
				nextPosition = new Pair(curr.x + 1, curr.y);
			} else if (target.x < curr.x) {
				nextPosition = new Pair(curr.x - 1, curr.y);
			} else {
				if (target.y > curr.y) {
					nextPosition = new Pair(curr.x, curr.y + 1);
				} else if (target.y < curr.y) {
					nextPosition = new Pair(curr.x, curr.y - 1);
				} else {
					nextPosition = new Pair(curr.x, curr.y);
				}
			}
		} else {
			if (target.y > curr.y) {
				nextPosition = new Pair(curr.x, curr.y + 1);
			} else if (target.y < curr.y) {
				nextPosition = new Pair(curr.x, curr.y - 1);
			} else {
				if (target.x > curr.x) {
					nextPosition = new Pair(curr.x + 1, curr.y);
				} else if (target.x < curr.x) {
					nextPosition = new Pair(curr.x - 1, curr.y);
				} else {
					nextPosition = new Pair(curr.x, curr.y);
				}
			}
		}
		
		System.out.printf("nextPosition %d,%d from %d,%d to %d,%d\n",nextPosition.x, nextPosition.y, curr.x,curr.y,target.x,target.y);
		return nextPosition;
	}
	

	ArrayList<Pair> possibleFuturePositions(Pair start) {
		ArrayList<Pair> prlist = new ArrayList<Pair>();
		prlist.add(new Pair(start.x - 1, start.y));
		prlist.add(new Pair(start.x + 1, start.y));
		prlist.add(new Pair(start.x, start.y - 1));
		prlist.add(new Pair(start.x, start.y + 1));
		return prlist;
	}
	
	boolean isPairValidPosition(Pair pr) {
		if (pr.x < 0 || pr.x >= SIDE_SIZE) {
			return false;
		}
		if (pr.y < 0 || pr.y >= SIDE_SIZE) {
			return false;
		}
		if (getGridPoint(pr).water) {
			return false;
		}
		return true;
	}

	Point getGridPoint(int x, int y) { return grid[x * SIDE_SIZE + y]; }
	Point getGridPoint(Pair pr) { return grid[pr.x * SIDE_SIZE + pr.y]; }

	Pair pointToPair(Point pt) {
		return new Pair(pt.x, pt.y);
	}
	
	// compute Euclidean distance between two points
	double distance(Point a, Point b) {	return Math.sqrt((a.x-b.x) * (a.x-b.x) + (a.y-b.y) * (a.y-b.y)); }
	double distance(Point a, Pair b) {	return Math.sqrt((a.x-b.x) * (a.x-b.x) + (a.y-b.y) * (a.y-b.y)); }
	double distance(Pair a, Point b) {	return Math.sqrt((a.x-b.x) * (a.x-b.x) + (a.y-b.y) * (a.y-b.y)); }
	double distance(Pair a, Pair b) {	return Math.sqrt((a.x-b.x) * (a.x-b.x) + (a.y-b.y) * (a.y-b.y)); }
	
	class Resource {
		int water;
		int land;
		
		public Resource(int w, int l) {
			this.water = w;
			this.land = l;
		}
	}
}

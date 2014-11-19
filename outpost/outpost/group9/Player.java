package outpost.group9;

import java.util.*;

import outpost.sim.Pair;
import outpost.sim.Point;
import outpost.sim.movePair;

public class Player extends outpost.sim.Player {
	final static int SIDE_SIZE = 100;

	boolean playerInitialized;
	Point[] grid = new Point[SIDE_SIZE * SIDE_SIZE];
	Random random = new Random();
	int[] theta = new int[100];
	int seasonCounter = 0;

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
			}

			playerInitialized = true;
		}

		seasonCounter++;
		if (seasonCounter % 10 == 0) {
			for (int i = 0; i < 100; i++) {
				theta[i] = random.nextInt(4);
			}
		}
		
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


		for (int j = 0; j < myOutposts.size() - 1; j++) {
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

		ArrayList<Pair> positions = new ArrayList<Pair>();
		positions = possibleFuturePositions(myOutposts.get(myOutposts.size() - 1));
		boolean gotit = false;
		while (!gotit) {
			if (theta[0] < positions.size()) {
				if (isPairValidPosition(positions.get(theta[0]))) {
					movePair next = new movePair(myOutposts.size() - 1, positions.get(theta[0]));
					movelist.add(next);
					gotit = true;
					break;
				}
			}
			theta[0] = random.nextInt(positions.size());
		}

		return movelist;

	}

	static ArrayList<Pair> possibleFuturePositions(Pair start) {
		ArrayList<Pair> prlist = new ArrayList<Pair>();
		prlist.add(new Pair(start.x - 1, start.y));
		prlist.add(new Pair(start.x + 1, start.y));
		prlist.add(new Pair(start.x, start.y - 1));
		prlist.add(new Pair(start.x, start.y + 1));
		return prlist;
	}
	
	boolean isPairValidPosition(Pair pr) {
		if (pr.x < 0 || pr.x > SIDE_SIZE) {
			return false;
		}
		if (pr.y < 0 || pr.y > SIDE_SIZE) {
			return false;
		}
		if (getGridPointFromPair(pr).water) {
			return false;
		}
		return true;
	}

	Point getGridPointFromPair(Pair pr) {
		return grid[pr.x * SIDE_SIZE + pr.y];
	}

	Pair pointToPair(Point pt) {
		return new Pair(pt.x, pt.y);
	}
}

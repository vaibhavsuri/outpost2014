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

	public ArrayList<movePair> move(ArrayList<ArrayList<Pair>> king_outpostlist, Point[] gridin) {
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
			positions = surround(myOutposts.get(j));
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
				// System.out.println("we need to change the direction???");
				theta[j] = random.nextInt(positions.size());
			}
		}

		/*
		 * if (prarr.size()>noutpost) { movePair mpr = new
		 * movePair(prarr.size()-1, new Pair(0,0)); nextlist.add(mpr);
		 * //mpr.printmovePair(); }
		 */
		// else {
		ArrayList<Pair> positions = new ArrayList<Pair>();
		positions = surround(myOutposts.get(myOutposts.size() - 1));
		boolean gotit = false;
		while (!gotit) {
			// Random random = new Random();
			// int theta = random.nextInt(positions.size());
			// System.out.println("we are here!!!");
			if (theta[0] < positions.size()) {
				if (isPairValidPosition(positions.get(theta[0]))) {
					movePair next = new movePair(myOutposts.size() - 1, positions.get(theta[0]));
					movelist.add(next);
					// next.printmovePair();
					gotit = true;
					break;
				}
			}
			// System.out.println("outpost 0 need to change the direction???");
			theta[0] = random.nextInt(positions.size());
		}

		// }

		return movelist;

	}

	static ArrayList<Pair> surround(Pair start) {
		// System.out.printf("start is (%d, %d)", start.x, start.y);
		ArrayList<Pair> prlist = new ArrayList<Pair>();
		for (int i = 0; i < 4; i++) {
			Pair tmp0 = new Pair(start);
			Pair tmp;
			if (i == 0) {
				// if (start.x>0) {
				tmp = new Pair(tmp0.x - 1, tmp0.y);
				// if (!PairtoPoint(tmp).water)
				prlist.add(tmp);
				// }
			}
			if (i == 1) {
				// if (start.x<size-1) {
				tmp = new Pair(tmp0.x + 1, tmp0.y);
				// if (!PairtoPoint(tmp).water)
				prlist.add(tmp);
				// }
			}
			if (i == 2) {
				// if (start.y>0) {
				tmp = new Pair(tmp0.x, tmp0.y - 1);
				// if (!PairtoPoint(tmp).water)
				prlist.add(tmp);
				// }
			}
			if (i == 3) {
				// if (start.y<size-1) {
				tmp = new Pair(tmp0.x, tmp0.y + 1);
				// if (!PairtoPoint(tmp).water)
				prlist.add(tmp);
				// }
			}

		}

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

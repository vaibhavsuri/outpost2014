// TODO: HashMap to cache dst and path, save reduncant path finding

package outpost.group5_3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import outpost.sim.Pair;
import outpost.sim.Point;
import outpost.sim.movePair;

public class Player extends outpost.sim.Player {
	static int size = 100;
	static Point[][] grid = new Point[size][size];
	static Random random = new Random();
	static int[] theta = new int[100];
	static int counter = 0;
	boolean initFlag = false;
	ArrayList<Pair> ourOutpostLst = new ArrayList<Pair>();
	Set<Pair> opponentOutpostSet = new HashSet<Pair>();
	ArrayList<Point> opponentBaseLst = new ArrayList<Point>();
	ArrayList<ArrayList<Point>> radianLsts = new ArrayList<ArrayList<Point>>();
	ArrayList<ArrayList<Point>> waterBodies = new ArrayList<ArrayList<Point>>();
	boolean firstReachedWater = false;

	static int numWater = 0, numLand = 0;
	Point ourBasePosition;
	int L, W, r, t;

	public Player(int id_in) {
		super(id_in);
		initFlag = true;
	}

	public ArrayList<movePair> move(ArrayList<ArrayList<Pair>> outpostLsts, Point[] gridin, int r, int L, int W, int t) {
		// cell : Point (int x, int y, boolean water)
		// outpost : Pair (int x, int y)
		// move : movePair (int id, Pair pr)

		if (initFlag) {
			initFlag = false;
			this.L = L;
			this.W = W;
			this.r = r;
			this.t = t;
			parseGrid(gridin);
			populateRadianLsts(r);
			getNumberOfWaterLandInQuarter();
			getWaterBodies();
			System.out.println("gridin size = " + gridin.length);
			int totalWater = 0;
			for (int i = 0; i < waterBodies.size(); i++) {
				totalWater += waterBodies.get(i).size();
				System.out.print("size = " + waterBodies.get(i).size() + ", ");
			}
			System.out.println("\nnumWater = " + numWater + ", totalWater = " + totalWater);
		}

		ArrayList<movePair> returnLst = new ArrayList<movePair>();
		updateOpponentOutpost(outpostLsts);
		ourOutpostLst = outpostLsts.get(this.id);

		// if there is not an ideal ratio of land to water in this region, then
		// make sure outposts will be able to sustain themselves
		/*
		 * if (numLand / numWater < L / W) {
		 * System.out.println("There is not enough water"); Point
		 * closestWaterPoint = getOptimalWaterPoint(r); Pair closestPair =
		 * realPair(new Pair(closestWaterPoint.x, closestWaterPoint.y));
		 * closestWaterPoint = new Point(closestPair.x, closestPair.y, true);
		 * 
		 * if (firstReachedWater == false) { movePair move = moveTo(0,
		 * closestWaterPoint); returnLst.add(move); }
		 * 
		 * // move to sustain/increase outpost number } // if there is enough
		 * water in the region to not have to worry about // land to water ratio
		 * else {
		 */
		// System.out.println("There is enough water");
		// returnLst = radianOutreach(ourOutpostLst);
		returnLst.add(moveTo(0, getOptimalWaterPoint(r)));
		if (ourOutpostLst.size() >= 4) {
			ArrayList<Integer> team = new ArrayList<Integer>();
			team.add(1);
			team.add(2);
			// team.add(2);
			// team.add(3);
			returnLst = expendablesMove(team);
			returnLst.addAll(radianOutreach(ourOutpostLst, 3));

		} else {
			returnLst.addAll(radianOutreach(ourOutpostLst, 1));
		}

		/*
		 * failed suffocate logic harverster outpost returnLst.add(moveTo(0,
		 * grid[50][50])); killer outpost
		 * returnLst.addAll(suffocate(ourOutpostLst));
		 */
		// }

    /*
		for (movePair mp : returnLst) {
			if (mp.id == 0) {
				System.out.println("water seeker is moving to (" + mp.pr.x + "," + mp.pr.y + ")");
			}
		}
    */
		return returnLst;
	}

	private void getWaterBodies() {
		ArrayList<Point> waterLocs = new ArrayList<Point>();
		boolean[][] visitedMap = new boolean[grid.length][grid.length];
		for (int i = 0; i < grid.length; ++i) {
			for (int j = 0; j < grid.length; ++j) {
				if (visitedMap[i][j] || !grid[i][j].water) {
					continue;
				}
				waterBodies.add(floodHelper(visitedMap, i, j));
			}
		}
    /*
		Collections.sort(waterBodies, new Comparator<ArrayList>() {
			public int compare(ArrayList a1, ArrayList a2) {
				if (a2.size == a1.size()) {
					double a1Dist = getClosestDistance(a1);
					double a2Dist = getClosestDistance(a2);
					return (int) (a1Dist - a2Dist); // assumes you want the
													// smallest
					// distance over the larger distance
				} else
					return a2.size() - a1.size(); // assumes you want biggest to
													// smallest
			}
		});
    */
		System.out.println("===== waters =====");
		for (ArrayList<Point> p : waterBodies) {
			System.out.println(p.size());
		}
	}
  /*
	private double getClosestDistance(ArrayList a) {
		double minDist = Double.MAX_VALUE;
		Point minPoint;
		for (int i = 0; i < a.size(); i++) {
			double tmpDist = distance(a.get(i), ourBasePosition);
			if (tmpDist < minDist) {
				minDist = tmpDist;
				minPoint = a.get(i);
			}
		}
		return minDist;
	}
  */
	ArrayList<Point> floodHelper(boolean[][] visitedMap, int i, int j) {
		ArrayList<Point> res = new ArrayList<Point>();
		if (i < 0 || i > grid.length || j < 0 || j > grid.length || visitedMap[i][j] || !grid[i][j].water) {
			return res;
		}
		res.add(grid[i][j]);
		visitedMap[i][j] = true;
		res.addAll(floodHelper(visitedMap, i - 1, j));
		res.addAll(floodHelper(visitedMap, i + 1, j));
		res.addAll(floodHelper(visitedMap, i, j - 1));
		res.addAll(floodHelper(visitedMap, i, j + 1));
		return res;
	}

	private ArrayList<Point> getWaterBody(Point point, ArrayList<Point> waterLocs) {
		ArrayList<Point> waterBody = new ArrayList<Point>();

		ArrayList<Point> queue = new ArrayList<Point>();
		queue.add(point);

		while (queue.size() > 0) {
			Point node = queue.get(0);
			if (waterLocs.contains(grid[node.x - 1][node.y])) {
				queue.add(grid[node.x - 1][node.y]);
				waterBody.add(grid[node.x - 1][node.y]);
			}
			if (waterLocs.contains(grid[node.x + 1][node.y])) {
				queue.add(grid[node.x + 1][node.y]);
				waterBody.add(grid[node.x + 1][node.y]);
			}
			if (waterLocs.contains(grid[node.x][node.y - 1])) {
				queue.add(grid[node.x][node.y - 1]);
				waterBody.add(grid[node.x][node.y - 1]);
			}
			if (waterLocs.contains(grid[node.x][node.y + 1])) {
				queue.add(grid[node.x][node.y + 1]);
				waterBody.add(grid[node.x][node.y + 1]);
			}
		}

		return waterBody;
	}

	private boolean isNextTo(Point p1, Point p2) {
		if ((p1.x - 1 == p2.x) || (p1.x + 1 == p2.x) || (p1.y - 1 == p2.y) || (p1.y + 1 == p2.y))
			return true;
		return false;
	}

	public Point getOptimalWaterPoint(int r) {
		int minDist = 2 * size;
		int waterMaxSize = 0;
		ArrayList<Point> waterMinDstLst = null;
		for (ArrayList<Point> waterCluster : waterBodies) {
			if (waterCluster.size() > W && waterCluster.get(0).x + waterCluster.get(0).y < minDist) {
				minDist = waterCluster.get(0).x + waterCluster.get(0).y;
				waterMinDstLst = waterCluster;
			}
		}
		if (waterMinDstLst != null) {
			return waterMinDstLst.get(waterMinDstLst.size() / 2);
		}
		return grid[size / 2][size / 2];
	}

	public static int numberOfWater(Point point, int r) {
		int water = 0;
		for (int i = 0; i < (size / 2); i++) {
			for (int j = 0; j < (size / 2); j++) {
				if (distance(point, new Point(i, j, false)) <= r) {
					if (grid[i][j].water == true)
						water++;
				}
			}
		}
		return water;
	}

	public void getNumberOfWaterLandInQuarter() {
		for (int i = 0; i < size / 2; i++) {
			for (int j = 0; j < size / 2; j++) {
				if (grid[i][j].water == true)
					numWater++;
				else
					numLand++;
			}
		}
	}

	public static Point findFurthestWaterInQuarter() {
		int i = 50;
		int j = 50;
		for (int k = 0; k < ((size / 2) * (size / 2)); k++) {
			if (grid[i - 1][j - 1].water == true) {
				return grid[i - 1][j - 1];
			}
			if ((i + j) % 2 == 0) {
				if (j > 0)
					j--;
				else
					i -= 2;
				if (i < 50)
					i++;
			} else {
				if (i > 0)
					i--;
				else
					j -= 2;
				if (j < 50)
					j++;
			}
		}
		return null;
	}

	public static Point findClosestWater() {
		int i = 1;
		int j = 1;
		for (int k = 0; k < ((size / 2) * (size / 2)); k++) {
			if (grid[i - 1][j - 1].water == true) {
				return grid[i - 1][j - 1];
			}
			if ((i + j) % 2 == 0) {
				if (j < (size / 4))
					j++;
				else
					i += 2;
				if (i > 1)
					i--;
			} else {
				if (i < (size / 4))
					i++;
				else
					j += 2;
				if (j > 1)
					j--;
			}
		}
		return null;
	}

	public static Point findClosestWaterNotControlled(ArrayList<Pair> ourOutposts, int r) {
		int i = 1;
		int j = 1;
		for (int k = 0; k < ((size / 2) * (size / 2)); k++) {
			if (grid[i - 1][j - 1].water == true && !isControlled(grid[i - 1][j - 1], r, ourOutposts)) {
				return grid[i - 1][j - 1];
			}
			if ((i + j) % 2 == 0) {
				if (j < (size / 2))
					j++;
				else
					i += 2;
				if (i > 1)
					i--;
			} else {
				if (i < (size / 2))
					i++;
				else
					j += 2;
				if (j > 1)
					j--;
			}
		}
		return null;
	}

	public static boolean isControlled(Point point, int r, ArrayList<Pair> ourOutposts) {
		for (Pair outpost : ourOutposts) {
			if (distance(point, new Point(outpost.x, outpost.y, false)) < r)
				return true;
		}
		return false;
	}

	public Point getClosestNearWaterPoint() {
		int minDistance = Integer.MAX_VALUE;
		int minRow = 0, minCol = 0;
		for (int row = 0; row < grid.length; row++) {
			for (int col = 0; col < grid[0].length; col++) {
				int tempDistance = distance(grid[row][col], ourBasePosition);
				if (grid[row][col].water == false && nextToWater(grid[row][col]) == true && tempDistance < minDistance) {
					minDistance = tempDistance;
					minRow = row;
					minCol = col;
				}
			}
		}
		return grid[minRow][minCol];
	}

	public boolean nextToWater(Point point) {
		int x = point.x;
		int y = point.y;
		if (x + 1 < grid[0].length && grid[x + 1][y].water == true)
			return true;
		else if (x - 1 > 0 && grid[x - 1][y].water == true)
			return true;
		else if (y + 1 < grid.length && grid[x][y + 1].water == true)
			return true;
		else if (y - 1 > 0 && grid[x][y - 1].water == true)
			return true;

		return false;
	}

	// =========================================================
	//
	// Utility logic
	//
	// =========================================================

	public Pair realPair(Pair p) {
		switch (id) {
		case 0:
			return new Pair(p.x, p.y);
		case 1:
			return new Pair(size - p.x - 1, p.y);
		case 2:
			return new Pair(p.x, size - p.y - 1);
		case 3:
			return new Pair(size - p.x - 1, size - p.y - 1);
		}
		System.out.println("something is wroong in realPair");
		return new Pair(0, 0);
	}

	public void init() {
		// update opponent base position for suffocation muahaha
		System.out.println("size of the list: " + opponentBaseLst.size());
		opponentBaseLst.add(new Point(0, 0, false));
		opponentBaseLst.add(new Point(size - 1, 0, false));
		opponentBaseLst.add(new Point(size - 1, size - 1, false));
		opponentBaseLst.add(new Point(0, size - 1, false));
		// cache our base's location
		ourBasePosition = opponentBaseLst.get(id);

		opponentBaseLst.remove(id);
		System.out.println("-----");
		for (Point p : opponentBaseLst) {
			System.out.println(p.x + ":" + p.y);
		}
		System.out.println("-----");
	}

	static int distance(Point a, Point b) {
		// return Math.sqrt((a.x-b.x) * (a.x-b.x) +
		// (a.y-b.y) * (a.y-b.y));
		return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
	}

	int distToBase(Pair p) {
		return distance(new Point(p.x, p.y, false), ourBasePosition);
	}

	// logic for remove over populated outpost
	// kill the youngest one
	public int delete(ArrayList<ArrayList<Pair>> outpostLsts, Point[] gridin) {
		return outpostLsts.get(id).size() - 1;
	}

	// convert grid into 2-D plane
	void parseGrid(Point[] gridin) {
		for (int i = 0; i < size; ++i) {
			for (int j = 0; j < size; ++j) {
				grid[i][j] = gridin[i * size + j];
				// System.out.println("(" + i + ":" + j + ")  ;  (" +
				// grid[i][j].x + ":" + grid[i][j].y + ")");
			}
		}
		System.out.println("Grid parsing finished");
	}

	public int getPointOwnerShip(int i, int j,ArrayList<ArrayList<Pair>> outpostLsts){
		Point point=grid[i][j];
		ArrayList<Pair> point_list=point.ownerlist;
		if(point_list.size()==0)
			return 0;
		if(point_list.size()>1)
			return 5;
		
		Pair owner=point_list.get(0);

		for(int k=0; k<outpostLsts.size(); k++){
			ArrayList<Pair> player_list=outpostLsts.get(k);
			for(Pair p: player_list){
				if(p.x==owner.x && p.y==owner.y){
					return k;
				}
			}
		}
		
		return 6;	
	}

	// setup radian mesh
	// TODO: supporting line formation
	void populateRadianLsts(int r) {
		// increase r to diameter
		// r *= 2;
		int boundary = size / 2;
		for (int i = boundary; i >= 0.1 * boundary; i -= r / 2) {
			ArrayList<Point> newLst = new ArrayList<Point>();
			for (int j = 0; j < i; j += r / 2) {
				int i1 = 0, i2 = 0, j1 = 0, j2 = 0;
				switch (id) {
				case 0:
					i1 = i;
					j1 = j;
					i2 = j;
					j2 = i;
					break;
				case 1:
					i1 = size - i - 1;
					j1 = j;
					i2 = size - j - 1;
					j2 = i;
					break;
				case 2:
					i1 = i;
					j1 = size - j - 1;
					i2 = j;
					j2 = size - i - 1;
					break;
				case 3:
					i1 = size - i - 1;
					j1 = size - j - 1;
					i2 = size - j - 1;
					j2 = size - i - 1;
					break;
				}
				if (!grid[i1][j1].water)
					newLst.add(new Point(i1, j1, false));
				if (!grid[i2][j2].water)
					newLst.add(new Point(i2, j2, false));
			}
			radianLsts.add(newLst);
		}
	}

	void updateOpponentOutpost(ArrayList<ArrayList<Pair>> outpostLsts) {
		opponentOutpostSet.clear();
		for (int i = 0; i < outpostLsts.size(); ++i) {
			if (i == id)
				continue;
			ArrayList<Pair> tempLst = outpostLsts.get(i);
			for (Pair p : tempLst)
				opponentOutpostSet.add(p);
		}
		System.out.println("OpponentSet updated, total hostile outpost : " + opponentOutpostSet.size());
	}

	// basic path finding logic
	// TODO: implement priority queue to switch from BFS to A*
	movePair moveTo(int index, Point dst) {
		System.out.println("id:" + id);
		Pair src = ourOutpostLst.get(index);

		if (src.x == dst.x && src.y == dst.y) {
			System.out.println("dst reached");
			return new movePair(index, src);
		}
		// BFS for rechability finding
		int[][] visitedMap = new int[size][size];
		Queue<Pair> q = new LinkedList<Pair>();
		q.offer(src);
		visitedMap[src.x][src.y] = 1;
		boolean reachableFlag = false;
		Pair nearestLand = null;
		int nearestLandDst = 2 * size;
		while (q.size() > 0) {
			Pair curP = q.poll();
			ArrayList<Pair> nextHop = nextHopLst(curP);
			for (Pair p : nextHop) {
				if (!grid[p.x][p.y].water && visitedMap[p.x][p.y] == 0) {
					q.offer(p);
					visitedMap[p.x][p.y] = visitedMap[curP.x][curP.y] + 1;
					if (distance(new Point(p.x, p.y, false), dst) < nearestLandDst) {
						nearestLandDst = distance(new Point(p.x, p.y, false), dst);
						nearestLand = p;
					}
				}
				if (p.equals(new Pair(dst.x, dst.y))) {
					reachableFlag = true;
					break;
				}
			}
		}
		// reverse searching for path formation
		if (reachableFlag) {
			System.out.printf("a path is found for src: (%d, %d); dst: (%d, %d)\n", src.x, src.y, dst.x, dst.y);
			Pair prevP = new Pair(dst.x, dst.y);
			while (true) {
				ArrayList<Pair> prevHop = nextHopLst(prevP);
				for (Pair p : prevHop) {
					// System.out.print("(" + prevP.x + "," + prevP.y + ")");
					// reach src
					if (visitedMap[p.x][p.y] == 1) {
						System.out.println("returning from moveto");
						return new movePair(index, prevP);
					}
					if (visitedMap[p.x][p.y] == visitedMap[prevP.x][prevP.y] - 1) {
						prevP = p;
						break;
					}
				}
			}
		} else {
			System.out.printf("no path is found for src: (%d, %d); dst: (%d, %d)\n", src.x, src.y, dst.x, dst.y);
			if (nearestLand != null) {
				Pair prevP = new Pair(nearestLand.x, nearestLand.y);
				System.out.println("nearestLand x : " + nearestLand.x + "nearestland y: " + nearestLand.y);
				while (true) {
					ArrayList<Pair> prevHop = nextHopLst(prevP);
					for (Pair p : prevHop) {
						// reach nearestLand
						if (visitedMap[p.x][p.y] == 1) {
							return new movePair(index, prevP);
						}
						if (visitedMap[p.x][p.y] == visitedMap[prevP.x][prevP.y] - 1) {
							prevP = p;
							break;
						}
					}
				}
			}
		}
		System.out.printf(
				"no path is found for src: (%d, %d); dst: (%d, %d) and there is no nearest land, something is wrong\n",
				src.x, src.y, dst.x, dst.y);
		System.out.println("returning from moveto");
		return new movePair(index, src);
	}

	// get adjacent cells
	ArrayList<Pair> nextHopLst(Pair start) {
		ArrayList<Pair> prLst = new ArrayList<Pair>();
		for (int i = 0; i < 4; ++i) {
			Pair tmp0 = new Pair(start);
			Pair tmp = null;
			switch (i) {
			case 0:
				tmp = new Pair(tmp0.x - 1, tmp0.y);
				break;
			case 1:
				tmp = new Pair(tmp0.x + 1, tmp0.y);
				break;
			case 2:
				tmp = new Pair(tmp0.x, tmp0.y - 1);
				break;
			case 3:
				tmp = new Pair(tmp0.x, tmp0.y + 1);
				break;
			}
			if (tmp.x >= 0 && tmp.x < size && tmp.y >= 0 && tmp.y < size && !grid[tmp.x][tmp.y].water) {
				prLst.add(tmp);
			}
		}
		return prLst;
	}

	static Pair PointtoPair(Point pt) {
		return new Pair(pt.x, pt.y);
	}

	// =========================================================
	//
	// Game Logic
	//
	// =========================================================

	ArrayList<movePair> suffocate(ArrayList<Pair> outpostLst) {
		ArrayList<movePair> res = new ArrayList<movePair>();
		for (int i = 1; i < outpostLst.size(); ++i) {
			switch (i % 3) {
			case 0:
				res.add(moveTo(i, opponentBaseLst.get(0)));
				break;
			case 1:
				res.add(moveTo(i, opponentBaseLst.get(1)));
				break;
			case 2:
				res.add(moveTo(i, opponentBaseLst.get(2)));
				break;
			}
		}

		System.out.println("killer list size: " + res.size());
		return res;
	}

	ArrayList<movePair> radianOutreach(ArrayList<Pair> outpostLst, int startIdx) {
		ArrayList<movePair> res = new ArrayList<movePair>();
		int counter = startIdx;
		for (ArrayList<Point> arrayLst : radianLsts) {
			for (Point p : arrayLst) {
				if (counter >= outpostLst.size()) {
					return res;
				}
				res.add(moveTo(counter++, p));
			}
		}
		while (counter < outpostLst.size()) {
			res.add(moveTo(counter++, opponentBaseLst.get(counter % 3)));
		}
		return res;
	}

	// team consists of 4 members in hope to make a surrounding of the left
	// along outpost from opponent
	// *
	// like this: *X*
	// *
	ArrayList<movePair> expendablesMove(List<Integer> team) {
		ArrayList<movePair> res = new ArrayList<movePair>();
		if (team.size() < 4) {
			return res;
		}
		int minDst = 2 * size;
		Pair minPair = null;
		for (Pair p : opponentOutpostSet) {
			if (distToBase(p) < minDst) {
				minPair = p;
				minDst = distToBase(p);
			}
		}
		if (minPair == null) {
			return res;
		}
		System.out.println("adding first point");
		res.add(moveTo(team.get(0), new Point(minPair.x - 1, minPair.y, false)));
		System.out.println("adding second point");
		res.add(moveTo(team.get(1), new Point(minPair.x + 1, minPair.y, false)));
		System.out.println("adding third point");
		res.add(moveTo(team.get(2), new Point(minPair.x, minPair.y - 1, false)));
		System.out.println("adding fourth point");
		res.add(moveTo(team.get(3), new Point(minPair.x, minPair.y + 1, false)));
		return res;
	}

}

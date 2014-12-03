package outpost.group8_3;

import java.util.*;

import outpost.sim.Pair;
import outpost.sim.Point;
import outpost.sim.movePair;

public class Player extends outpost.sim.Player {
	static int size = 100;
	static Point[] grid = new Point[size*size];
	static Location[][] locGrid;
	static Random random = new Random();
	static int[] theta = new int[100];
	static int counter = 0;
	boolean flag = true;
	static int count = 0 ;
	static int upperRandom = 3;
	
	static HashSet<Location> waterPoints;
	static HashSet<Location> shorePoints;
	// key = outpost id (integer, counting up from 0). assumes outposts will be in the same
	// order always (fragile)
	HashMap<Integer, Location> targets = new HashMap<Integer, Location>();
	HashSet<Integer[]> targetHistory = new HashSet<Integer[]>();
	ArrayList<Location> openShore = new ArrayList<Location>();

	// Helper classes
	static Global globalHelper = new Global();
	static MapAnalysis mapHelper = new MapAnalysis();
	
	public static boolean initGlobal = false;
	
    public Player(int id_in) {
		super(id_in);
		// TODO Auto-generated constructor stub
	}

	public void init() {
    	for (int i=0; i<100; i++) {
    		theta[i]=random.nextInt(4);
    	}
    }
    
    static double distance(Point a, Point b) {
        return Math.sqrt((a.x-b.x) * (a.x-b.x) +
                         (a.y-b.y) * (a.y-b.y));
    }
    
    public int delete(ArrayList<ArrayList<Pair>> king_outpostlist, Point[] gridin) {
    	return PlayerUtil.furthestOutpost(king_outpostlist.get(id), this.id, size);
    }
    
	public ArrayList<movePair> move(
			ArrayList<ArrayList<Pair>> king_outpostlist,
			Point[] gridin,
			int r,
			int L,
			int W,
			int t){
		
		if(!initGlobal){
			locGrid = Global.initGlobal(gridin);
			Global.initGlobal(r, id);
			initGlobal = true;
		}
		
		// Do map analysis
		if (shorePoints == null || waterPoints == null) {
			waterPoints = MapAnalysis.findWater(locGrid);
			shorePoints = MapAnalysis.findShores(waterPoints);
		}
		
		ArrayList<movePair> returnlist = null;
    	List<Pair> myOutPosts = king_outpostlist.get(this.id);
    	count++;
    	
    	// Keep track of the shoreline that we occupy.
    	if (openShore.isEmpty()) {
    		openShore = MapAnalysis.updateOpenShore(myOutPosts, shorePoints);
    	}
    	Location followLocation =null;
    	
    	
    	// Target water resources when there are less than 3 Outposts.
    	Random rand = new Random();
    	int ran = rand.nextInt(2);

    	int applyExpansion = rand.nextInt(7);
    	
    	if (applyExpansion == 5 && count < 980 ) {
    		returnlist = Strategy.angularExpansion(myOutPosts, shorePoints, r);	
    	} else if ((ran == 0 || applyExpansion == 3)&& count < 980) {
    		// Lenient attack to waters
    		returnlist = Strategy.attackWater(myOutPosts, shorePoints);
    		
    	} 
    	else if (ran == 1 && count < 980 ) {
    		// Strict attack to waters with less space in between
    		// Samara's water attack 
    		returnlist = Strategy.targetWaterResources(myOutPosts, targets, openShore, targetHistory);	
    		//returnlist = Strategy.protectHome(myOutPosts, shorePoints, r);
    	} else {
    		 // Lenient attack to waters
    	    	returnlist = Strategy.attackWater(myOutPosts, shorePoints);	
    	}

    	/*
		for (movePair mp : returnlist) {
			mp.printmovePair();
			System.out.println();
		}
		*/
    	return returnlist;
    }
	
	static boolean targetHistoryContains(Integer x, Integer y, HashSet<Integer[]> targetHist) {
		for (Integer[] arr : targetHist) {
            // System.out.printf("CHECKING %d %d, SO FAR: %d %d\n", x.intValue(), y.intValue(), arr[0].intValue(), arr[1].intValue());
			if (arr[0].intValue() == x.intValue() && arr[1].intValue() == y.intValue()) {
				return true;
			}
		}
		return false;
	}

	static Integer[] arrayify (Integer x, Integer y) {
		Integer[] target = new Integer[2];
		target[0] = x;
		target[1] = y;
		return target;
	}

    static Point PairtoPoint(Pair pr) {
    	return grid[pr.x*size+pr.y];
    }
    static Pair PointtoPair(Point pt) {
    	return new Pair(pt.x, pt.y);
    }
}

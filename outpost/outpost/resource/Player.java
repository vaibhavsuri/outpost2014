package outpost.resource;

import java.util.*;

import outpost.sim.Pair;
import outpost.sim.Point;
import outpost.sim.movePair;

public class Player extends outpost.sim.Player {
	final static int SIDE_SIZE = 100;

	boolean playerInitialized;
	Point[] grid = new Point[SIDE_SIZE * SIDE_SIZE];
	ArrayList<ArrayList<Pair>> playersOutposts;
	ArrayList<Pair> next_moves;
	int RADIUS;
	int L_PARAM;
	int W_PARAM;
	int MAX_TICKS;
	
	Random random = new Random();
	int[] theta = new int[100];
	int tickCounter = 0;
	
	int my_land, my_water;
	ArrayList<Cell> board_scored;
	
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

		
		if (tickCounter == 0)
		{
			//on the first tick, evaluate all the cells the board and store the scores in 
			//board_scored
			board_scored = evaluateBoard(r);
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
//		int id = 0;
//		for (Pair thisOutpost : myOutposts) {
//			System.out.printf("Outpost %d: %d,%d\n", id, thisOutpost.x, thisOutpost.y);
//			id++;
//		}
		

		ArrayList<movePair> movelist = new ArrayList<movePair>();
		
		//instantiating a list of all next moves of outposts
		next_moves = new ArrayList<Pair>();
		for (int j = myOutposts.size()-1; j >=0 ; j--) { //the order is reversed so as to make the earlier born outposts to move further rather than block newer ones
			Pair best_resource = new Pair();
			best_resource = getSeasonBestResource(king_outpostlist, j, L, W, r); //get the best resource cell to move to within this season
			if (best_resource.x==0 && best_resource.y==0)
			{
				Pair closestWater=findClosestWaterCell(myOutposts.get(j));
				ArrayList<Point> surround_cells = surrounds(getGridPoint(closestWater));
				for (Point check_cell: surround_cells)
				{
					if(!check_cell.water)
					{
						best_resource = new Pair(check_cell.x, check_cell.y);
					}
				}
			}
			next_moves.add(best_resource);
			movePair next = new movePair(j, myOutposts.get(j));
			try{ //because sometimes throws null exception
			 next = new movePair(j, pointToPair(nextPositionToGetToPosition(getGridPoint(myOutposts.get(j)), new Point(best_resource.x,best_resource.y,false))));
			}
			catch(Exception E){}
			movelist.add(next);
		}
		
		return movelist;
	}
	
	Point nextPositionToGetToPosition(Point source, Point destination) {
		source = getGridPoint(source);
		destination = getGridPoint(destination);
		if (source.equals(destination)) {
			return destination;
		}
		
		ArrayList<Point> path = buildPath(source, destination);
		
		//System.out.printf("From %s to %s: move to %s\n", pointToString(source), pointToString(destination), pointToString(path.get(1)));
		return path.get(1);
	}

	//returns the "best" closest cell to get to within this season
	public Pair getSeasonBestResource(ArrayList<ArrayList<Pair>> king_outpostlist, int index, int L, int W, int r)
	{
		ArrayList<Pair> myOutposts = king_outpostlist.get(this.id);
		Pair p = myOutposts.get(index);
		Pair best_cell = new Pair();
		double req_ratio = L/W; //this is our required Land to Water ratio
		double best_ratio = -1;
		int best_land=0;
		int best_water=0;
		double limit = 10 - (tickCounter%10); //the number of ticks left until the season ends - this decides how many steps we can move before the season ends

		for (int b = 0; b<board_scored.size(); b++) //loop through the scored cells
		{
			Cell k = board_scored.get(b);
			//the "too_close" boolean is for determining if this cell would be close to either one
			//of the next moves of other outposts or close to other outposts
			boolean too_close = false;
			
			//checking with the next decided targets of other outposts (if any)
			if(next_moves.size()>0){
			for (int i=0; i<next_moves.size(); i++)
				if(M_distance(new Pair(k.cell.x, k.cell.y), next_moves.get(i)) < 2*r)
				{
					too_close=true;
					break;
				}
			}
			
			if(too_close)
				continue;
			
			//checking with all the other outposts on the board
			for (int t=0; t<4; t++)
			{
					ArrayList<Pair> teamOutposts = king_outpostlist.get(t);
					for (int i=0; i< teamOutposts.size(); i++)
					{
						double separation;
						if ((i==index) && (this.id==t))
							continue;
						
						if (t==this.id)
							 separation = r; 
						else
							 separation = 2*r;//trying to set target farther from other teams' outposts
						
						if(M_distance(new Pair(k.cell.x, k.cell.y), teamOutposts.get(i)) < separation)
						{
							too_close=true;
							break;
						}
					}
			}
			
			if(too_close)
				continue;
			
			//check if the cell can be reached within the end of this season
			if (M_distance(new Pair(k.cell.x, k.cell.y), p) < limit)
			{
				if(k.water!=0) //to avoid Math errors
				if ((Math.abs(req_ratio - (k.land/k.water)) < Math.abs(req_ratio - best_ratio)) || (k.land >= best_land && k.water >= best_water)) //the second condition is for edge cases
				{
					best_ratio = k.land/k.water;
					best_cell = new Pair(k.cell.x, k.cell.y);
					best_land = k.land;
					best_water = k.water;
				}
				
			}
		}

		return best_cell;
	}
	
	public Pair findClosestWaterCell(Pair p)
	{
		double min_dist = Integer.MAX_VALUE;
		Pair closestWater = new Pair();
		for(int i=0; i<100; i++)
		{
			for(int j=0; j<100; j++)
			{
				if(getGridPoint(i,j).water && (M_distance(new Pair(i,j), p)<min_dist))
				{
					min_dist = M_distance(new Pair(i,j), p);
					closestWater = new Pair(i,j);
				}
			}
		}
		return closestWater;
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
						if (M_distance(eval_cell, test_cell) < r)
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
	
	public static double E_distance(Pair a, Pair b)
	{
		return Math.abs((a.x-b.x)^2+(a.y-b.y)^2);
	}

	public static double M_distance(Point a, Point b) {
		return Math.abs(a.x-b.x)+Math.abs(a.y-b.y);
	}
	
	public static double M_distance(Pair a, Pair b) {
		return Math.abs(a.x-b.x)+Math.abs(a.y-b.y);
	}
	
	public static int M_distance(int a, int b, int x, int y) {
		return Math.abs(a-x)+Math.abs(b-y);
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
	
	double distance(Point a, Point b) {	return Math.abs(a.x-b.x)+Math.abs(a.y-b.y); }
	double distance(Point a, Pair b) {	return Math.abs(a.x-b.x)+Math.abs(a.y-b.y); }
	double distance(Pair a, Point b) {	return Math.abs(a.x-b.x)+Math.abs(a.y-b.y); }
	double distance(Pair a, Pair b) {	return Math.abs(a.x-b.x)+Math.abs(a.y-b.y); }
	
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

package outpost.buildpath;

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
	static int size = 100;
	
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

		
		
		buildPath(new Pair(0,2), new Pair(77, 8), gridin);
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
	
	class PathTrack
	{
		public Cell parent, child;
		
		public PathTrack(Pair a, Pair b) 
		{
			/*
	        parent.x = a.x;
	        parent.y = a.y;
	        child.x = b.x;
	        child.y = b.y;
	        */
			parent = new Cell(a.x, a.y);
			child = new Cell(b.x, b.y);
	    }
	}
	
	public void buildPath(Pair source, Pair destination, Point[] gridin)
	{
		HashMap<Pair, Pair> parent = new HashMap<Pair, Pair>();
		ArrayList<PathTrack> backtrace = new ArrayList<PathTrack>();
	//	Queue<Pair> discover = new LinkedList<Pair>();
		ArrayList<Pair> discover = new ArrayList<Pair>();
		ArrayList<Pair> visited = new ArrayList<Pair>();
		discover.add(source);

		while(true)
		{
			if(discover.size()!=0)
			{
				Pair current = discover.get(0);
				discover.remove(0);
				visited.add(current);
				System.out.println(this.id+" analyzing: "+current.x+" "+current.y);
				if (current.x == destination.x && current.y==destination.y)
				{
					System.out.println("Found destination");
					break;
				}
				ArrayList<Pair> surround = surround(current);
				for (Pair p: surround)
				{
					boolean flag=false;
					for (Pair already: visited)
					{
						if (p.x==already.x && p.y==already.y)
						{
							flag=true;
							break;
						}
					}
					if (flag)
							continue;
					for (Pair already: discover)
					{
						if (p.x==already.x && p.y==already.y)
						{
							flag=true;
							break;
						}
					}
					if (flag)
						continue;
					if(/*(!visited.contains(p)) &&*/ (!gridin[p.x*size+p.y].water) && ((PairtoPoint(p, gridin).ownerlist.size() == 0) || (PairtoPoint(p, gridin).ownerlist.get(0).x==this.id))) 
					{
						discover.add(p);
						backtrace.add(new PathTrack(current, p));
						parent.put(p, current);
					}
				}
				
			}
			else 
			{
				System.out.println("No Path");
				return;			
			}
		}
		
//		System.out.println("Hashmap size : "+parent.size());
//		Pair child_node = new Pair(destination.x, destination.y);
//		Pair parent_node = new Pair(destination.x, destination.y);
//		while(true)
//		{
//			if((parent_node.x==source.x) && (parent_node.y==source.y))
//				break;
//			child_node = parent_node;
//			parent_node = parent.get(child_node);
//			if(parent_node == null)
//				System.out.println("Null hashmap parent");
//		}
//		System.out.println("Next step: "+child_node.x+" "+child_node.y);

		Cell child_node = new Cell(destination.x, destination.y);
		Cell parent_node = new Cell(destination.x, destination.y);
		while(true)
		{
			System.out.println("Parent: "+parent_node.x+" "+parent_node.y);
			if((parent_node.x==source.x) && (parent_node.y==source.y))
				break;
			child_node = parent_node;
			for(int i=0; i <backtrace.size(); i++)
			{
				//System.out.println("Child: "+backtrace.get(i).child.x+" "+backtrace.get(i).child.y);

				if ((backtrace.get(i).child.x == child_node.x) && (backtrace.get(i).child.y == child_node.y)) 
				{
					parent_node = backtrace.get(i).parent;
					break;
				}
			}
		}
		System.out.println("Next step: "+child_node.x+" "+child_node.y);
	}
	

	static ArrayList<Pair> surround(Pair start) {
		// 	System.out.printf("start is (%d, %d)", start.x, start.y);
		ArrayList<Pair> prlist = new ArrayList<Pair>();
		for (int i=0; i<4; i++) {
			Pair tmp0 = new Pair(start);
			Pair tmp;
			if (i==0) {
				if (start.x>0) {
					tmp = new Pair(tmp0.x-1,tmp0.y);
					prlist.add(tmp);
				}
			}
			if (i==1) {
				if (start.x<size-1) {
					tmp = new Pair(tmp0.x+1,tmp0.y);
					prlist.add(tmp);
				}
			}
			if (i==2) {
				if (start.y>0) {
					tmp = new Pair(tmp0.x, tmp0.y-1);
					prlist.add(tmp);
				}
			}
			if (i==3) {
				if (start.y<size-1) {
					tmp = new Pair(tmp0.x, tmp0.y+1);
					prlist.add(tmp);
				}
			}

		}

		return prlist;
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
	
	static Point PairtoPoint(Pair pr, Point[] gridin) {
		return gridin[pr.x*size+pr.y];
	}
}

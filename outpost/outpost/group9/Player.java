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
	
	// duo strategy stuff
	ArrayList<Duo> allDuos = new ArrayList<Duo>();
	Set<Point> currentDuosTargets = new HashSet<Point>();
	Set<Duo> busyDuos = new HashSet<Duo>();
	Set<Point> waitingToMove = new HashSet<Point>();
	
	// resource strategy stuff
	ArrayList<Pair> next_moves;
	int my_land, my_water;
	ArrayList<Cell> board_scored;
	
	Pair base;
	
	public Player(int id_in) {super(id_in);}

	public void init() {}

	public int delete(ArrayList<ArrayList<Pair>> king_outpostlist, Point[] gridin) {
//		int del = random.nextInt(king_outpostlist.get(id).size());
		return king_outpostlist.get(id).size() - 1;
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
			
			//on the first tick, evaluate all the cells the board and store the scores in 
			//board_scored
			board_scored = evaluateBoard(r);
			set_base_location();
			playerInitialized = true;
		}
		playersOutposts = king_outpostlist;
		myOutposts = king_outpostlist.get(this.id);
		tickCounter++;
		for (int i = 0; i < SIDE_SIZE * SIDE_SIZE; i++) {
			grid[i].ownerlist.clear();
		}
		
		
		// preparation code for the duo strategy
		currentDuosTargets.clear();
		busyDuos.clear();
		allDuos.clear();
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
		
		System.out.printf("New tick\n");
		int outpostId = 0;
		for (Pair thisOutpost : myOutposts) {
			System.out.printf("Outpost %d: %d,%d\n", outpostId, thisOutpost.x, thisOutpost.y);
			outpostId++;
		}
		
		// Begin movelist code
		ArrayList<movePair> movelist = new ArrayList<movePair>();
		
		
		//instantiating a list of all next moves of outposts
		next_moves = new ArrayList<Pair>();
		for (int j = myOutposts.size()-1; j >=0 ; j--) { //the order is reversed so as to make the earlier born outposts to move further rather than block newer ones
			Pair best_resource = new Pair();
			best_resource = getSeasonBestResource(king_outpostlist, j, L, W, r); //get the best resource cell to move to within this season
			if (best_resource.x==0 && best_resource.y==0)
			{
					boolean already_occupied = false;
					Pair closestWater=findClosestWaterCell(myOutposts.get(j));
					ArrayList<Point> surround_cells = neighborPoints(getGridPoint(closestWater));
					for (Point check_cell: surround_cells)
					{
						for(int other=0; other<myOutposts.size(); other++)
						{
							if (distance(new Pair(check_cell.x, check_cell.y),myOutposts.get(other)) < r)
							{
								already_occupied=true;
								break;
							}
						}
						if (already_occupied)
							break;
						if(!check_cell.water)
						{
							best_resource = new Pair(check_cell.x, check_cell.y);
							break;
						}
						
					}
					
					if(already_occupied)
					{
						best_resource = getUnoccupiedResource(king_outpostlist, j, L, W, r);
						if (best_resource.x==-1 && best_resource.y==-1)
							best_resource = farthestOutpost(myOutposts);
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
		

		
		// Specify the partners
//		for (int j = 0; j < myOutposts.size(); j+= 2) {
//			if(j+1 >= myOutposts.size()) {
//				// wait for follower
//				continue;
//			}
//			
//			int leaderId = j;
//			int followerId = j+1;
//			allDuos.add(new Duo(leaderId, followerId));
//		}
		
		// Priority number 1: If we have an enemy base, don't leave it
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
		
		// Priority 2: enemies closest to my base
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
	

	//returns the "best" closest cell to get to within this season
	public Pair getSeasonBestResource(ArrayList<ArrayList<Pair>> king_outpostlist, int index, int L, int W, int r)
	{
		ArrayList<Pair> myOutposts = king_outpostlist.get(this.id);
		Pair p = myOutposts.get(index);
		Pair best_cell = new Pair(0, 0);
		double req_ratio = L/W; //this is our required Land to Water ratio
		double best_ratio = -1;
		int best_land=0;
		int best_water=0;
		double limit = 10 - (tickCounter%10); //the number of ticks left until the season ends - this decides how many steps we can move before the season ends

		for (int b = 0; b<board_scored.size(); b++) //loop through the scored cells
		{
			Cell k = board_scored.get(b);
			if (getGridPoint(k.cell.x, k.cell.y).water) {
                continue;
			}
			//the "too_close" boolean is for determining if this cell would be close to either one
			//of the next moves of other outposts or close to other outposts
			boolean too_close = false;
			
			//checking with the next decided targets of other outposts (if any)
			if(next_moves.size()>0){
			for (int i=0; i<next_moves.size(); i++)
				if(distance(new Pair(k.cell.x, k.cell.y), next_moves.get(i)) < 2*r)
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
						
						if(distance(new Pair(k.cell.x, k.cell.y), teamOutposts.get(i)) < separation)
						{
							too_close=true;
							break;
						}
					}
					if (too_close)
						break;
			}
			
			if(too_close)
				continue;
			
			//check if the cell can be reached within the end of this season
			if (distance(new Pair(k.cell.x, k.cell.y), p) < limit)
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
	
	
	public Pair getUnoccupiedResource(ArrayList<ArrayList<Pair>> king_outpostlist, int index, int L, int W, int r)
	{
		ArrayList<Pair> myOutposts = king_outpostlist.get(this.id);
		Pair p = myOutposts.get(index);
		Pair best_cell = new Pair(-1, -1);
		double limit = 10 - (tickCounter%10); //the number of ticks left until the season ends - this decides how many steps we can move before the season ends

		for (int b = 0; b<board_scored.size(); b++) //loop through the scored cells
		{
			Cell k = board_scored.get(b);
			if (getGridPoint(k.cell.x, k.cell.y).water)
				continue;
			
			//the "too_close" boolean is for determining if this cell would be close to either one
			//of the next moves of other outposts or close to other outposts
			boolean too_close = false;
			
			//checking with the next decided targets of other outposts (if any)
			if(next_moves.size()>0){
			for (int i=0; i<next_moves.size(); i++)
				if(distance(new Pair(k.cell.x, k.cell.y), next_moves.get(i)) < 2*r)
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
							 separation = 3*r;//trying to set target farther from other teams' outposts
						
						if(distance(new Pair(k.cell.x, k.cell.y), teamOutposts.get(i)) < separation)
						{
							too_close=true;
							break;
						}
					}
					if (too_close)
						break;
			}
			
			if(too_close)
				continue;
			
			//check if the cell can be reached within the end of this season
			if (distance(new Pair(k.cell.x, k.cell.y), p) < limit)
			{
				best_cell = new Pair(k.cell.x, k.cell.y);
				break;
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
				if(getGridPoint(i,j).water && (distance(new Pair(i,j), p)<min_dist))
				{
					min_dist = distance(new Pair(i,j), p);
					closestWater = new Pair(i,j);
				}
			}
		}
		return closestWater;
	}
	
	public Pair farthestOutpost(ArrayList<Pair> myOutposts)
	{
		double max_dist = Double.NEGATIVE_INFINITY;
		Pair farthest = new Pair();
		for (Pair p: myOutposts)
		{
			if (distance(p, base) > max_dist)
			{
				max_dist = distance(p, base);
				farthest = new Pair(p.x, p.y);
			}
		}
		return farthest;
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
						if (distance(eval_cell, test_cell) < r)
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
	
	public void set_base_location()
	{
		if (this.id==0)
			base=new Pair(0,0);
		else if (this.id==1)
			base=new Pair(99,0);
		else if (this.id==2)
			base=new Pair(99,99);
		else
			base=new Pair(0,99);
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

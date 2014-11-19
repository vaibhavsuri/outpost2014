package outpost.findwater;

import java.util.*;

import outpost.sim.Pair;
import outpost.sim.Point;
import outpost.sim.movePair;

public class Player extends outpost.sim.Player {
	 static int size =100;
	static Point[] grid = new Point[size*size];
	static Random random = new Random();
	static int[] theta = new int[100];
	static int counter = 0;
	
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
    static double distance(Pair a, Point b) {
        return Math.sqrt((a.x-b.x) * (a.x-b.x) +
                         (a.y-b.y) * (a.y-b.y));
    }
    
    // Return: the next position
    // my position: dogs[id-1]

    
    public int delete(ArrayList<ArrayList<Pair>> king_outpostlist, Point[] gridin) {
    	//System.out.printf("haha, we are trying to delete a outpost for player %d\n", this.id);
    	int del = random.nextInt(king_outpostlist.get(id).size());
    	return del;
    }
    
	//public movePair move(ArrayList<ArrayList<Pair>> king_outpostlist, int noutpost, Point[] grid) {
    public ArrayList<movePair> move(ArrayList<ArrayList<Pair>> king_outpostlist, Point[] gridin, int r, int L, int W, int T){
    	
    	ArrayList<movePair> moves = new ArrayList<movePair>();
    	ArrayList<Pair> move_path;
    	
    	counter = counter+1;
    	if (counter % 10 == 0) {
    		for (int i=0; i<100; i++) {
        		theta[i]=random.nextInt(4);
        	}
    	}

		System.out.printf("outpost of %d is %d,%d\n", this.id, king_outpostlist.get(this.id).get(0).x, king_outpostlist.get(this.id).get(0).y);
		ArrayList<movePair> nextlist = new ArrayList<movePair>();
    	//System.out.printf("Player %d\n", this.id);
    	for (int i=0; i<gridin.length; i++) {
    		grid[i]=new Point(gridin[i]);
//    		if(grid[i].water)
//    			System.out.println("There is water on the map");
    	}
    	ArrayList<Pair> prarr = new ArrayList<Pair>();
    	prarr = king_outpostlist.get(this.id);
    	System.out.println("Size: "+prarr.size());
    	for (int j = 0 ; j < prarr.size(); j++) {
    		double min_dist = Double.POSITIVE_INFINITY;
    		int target=0;
    		for (int q=0; q<gridin.length; q++) 
    		{
    			for (int s = 0 ; s < prarr.size(); s++) 
    			{
	    			if((gridin[q].x-1 == prarr.get(s).x) && (gridin[q].y == prarr.get(s).y))
	    			{
	    				System.out.println("already someone there");
	    				continue;
	    			}
				}
    			if(gridin[q].water)
    			{
    				//System.out.println("Found water on the loop yay");
    				double dist = distance(prarr.get(j),grid[q]);
    				if (dist<min_dist)
    				{
    					min_dist = dist;
    					target = q;
    				}
    			}

    		}
    		
    		

    		if ((grid[target].x-1 == prarr.get(j).x) && (grid[target].y == prarr.get(j).y))
    		{
    			continue;
    		}
    		System.out.println("Building path");
    		move_path = buildPath(prarr.get(j), new Pair(grid[target].x-1, grid[target].y));
    		
    		System.out.println("Target: "+grid[target].x+" "+grid[target].y);

    		System.out.println("Next step: "+move_path.get(0).x+" "+move_path.get(0).y);
    		moves.add(new movePair(prarr.size()-1, move_path.get(0)));
//    		for (int k = 0; k<prarr.size()-1; k++)
//    		{
//    			if(k!=j)
//    				moves.add(new movePair(k, prarr.get(k)));
//    			else
//    				moves.add(new movePair(k, move_path.get(1)));
//    		}
    		break;
    	}

    	return moves;
    
    }
    
   public static ArrayList<Pair> buildPath(Pair source, Pair destination)
   {
	   ArrayList<Pair> move_path = new ArrayList<Pair>();
	   System.out.println("Buliding path function. Source "+source.x+" "+source.y+" Destination "+destination.x+" "+destination.y);
	   System.out.println("Moving to the x1");
	   if (source.x < destination.x)
	   {
		   int move_x=source.x;
		   while (move_x!=destination.x)
		   {
			   if(move_x == source.x)
				   move_x++;
			   move_path.add(new Pair(move_x, source.y));
			   if (move_x!=destination.x)
				   move_x++;
		   }
	   }
	   System.out.println("Moving to the x2");
	   if (source.x > destination.x)
	   {
		   int move_x=source.x;
		   while (move_x!=destination.x)
		   {
			   if(move_x == source.x)
				   move_x--;
			   move_path.add(new Pair(move_x, source.y));
			   if(move_x!=destination.x)
				   move_x--;
		   }
	   }
	   System.out.println("Moving to the y1");
	   if (source.y < destination.y)
	   {
		   int move_y=source.y;
		   while (move_y!=destination.y)
		   {
			   if(move_y == source.y)
				   move_y++;
			   move_path.add(new Pair(source.x, move_y));
			  if((move_y!=destination.y))
				  move_y++;
		   }
	   }
	   System.out.println("Moving to the y2");
	   if (source.y > destination.y)
	   {
		   int move_y=source.y;
		   while (move_y!=destination.y)
		   {
			   if(move_y == source.y)
				   move_y--;
			   move_path.add(new Pair(source.x, move_y));
			   if((move_y!=destination.y))
				   move_y--;
		   }
	   }
	   System.out.println("Path built");

	   return move_path;
	  
   }
    
    
    static ArrayList<Pair> surround(Pair start) {
   // 	System.out.printf("start is (%d, %d)", start.x, start.y);
    	ArrayList<Pair> prlist = new ArrayList<Pair>();
    	for (int i=0; i<4; i++) {
    		Pair tmp0 = new Pair(start);
    		Pair tmp;
    		if (i==0) {
    			//if (start.x>0) {
    			tmp = new Pair(tmp0.x-1,tmp0.y);
    	//		if (!PairtoPoint(tmp).water)
    			prlist.add(tmp);
    		//	}
    		}
    		if (i==1) {
    			//if (start.x<size-1) {
    			tmp = new Pair(tmp0.x+1,tmp0.y);
    		//	if (!PairtoPoint(tmp).water)
    			prlist.add(tmp);
    			//}
    		}
    		if (i==2) {
    			//if (start.y>0) {
    			tmp = new Pair(tmp0.x, tmp0.y-1);
    			//if (!PairtoPoint(tmp).water)
    			prlist.add(tmp);
    			//}
    		}
    		if (i==3) {
    			//if (start.y<size-1) {
    			tmp = new Pair(tmp0.x, tmp0.y+1);
    			//if (!PairtoPoint(tmp).water)
    			prlist.add(tmp);
    			//}
    		}
    		
    	}
    	
    	return prlist;
    }
    
    static Point PairtoPoint(Pair pr) {
    	return grid[pr.x*size+pr.y];
    }
    static Pair PointtoPair(Point pt) {
    	return new Pair(pt.x, pt.y);
    }
}

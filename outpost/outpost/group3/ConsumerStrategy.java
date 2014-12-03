package outpost.group3;

import java.util.ArrayList;

import outpost.group3.Outpost;

public class ConsumerStrategy extends outpost.group3.Strategy {
  enum State { ASSIGN, BUILD, ATTACK };

  final int SIZE = 4;
  
  // These are the 4 outposts composing a consumer
  final int UL = 0;
  final int UR = 1;
  final int BL = 2;
  final int BR = 3;

  private class Consumer {
    public ArrayList<Outpost> members;
    public Loc targetCenter;
    public Loc myCenter;
    public State state;
    public int area;
    
    Consumer(){
      members = new ArrayList<Outpost>();
      targetCenter = null;
      myCenter = null;
      state = State.ASSIGN;
      area = 0;
    }
  }

  public static ArrayList<Consumer> consumers = new ArrayList<Consumer>();

  private boolean isInConsumer(Outpost outpost){
    for (Consumer consumer : consumers){
      if (consumer.members.contains(outpost))
        return true;
    }
    return false;
  }


  ConsumerStrategy() {}

  public void run(Board board, ArrayList<Outpost> outposts) {
    if (outposts.size() >= SIZE){
      for (Consumer consumer : consumers) {
        ArrayList<Outpost> originalMembers = (ArrayList<Outpost>) consumer.members.clone();
        for (Outpost member : originalMembers) {
          if (!outposts.contains(member))
            consumer.members.remove(member);
        }
      }

      // Get the outposts that can be added to an existing or new consumer
      ArrayList<Outpost> availableOutposts = new ArrayList<Outpost>();
      for (Outpost outpost : outposts) {
        if (!isInConsumer(outpost))
          availableOutposts.add(outpost);
      }

      // Fill up existing consumers that have too few members, if possible
      for (Consumer consumer : consumers) {
        while (consumer.members.size() > 0 && consumer.members.size() < SIZE && availableOutposts.size() > 0)
          consumer.members.add(availableOutposts.remove(availableOutposts.size() - 1));
      }

      // Assign remaining outposts to new consumers and add fully formed consumers to global list
      while(availableOutposts.size() > 0){
        Consumer consumer = new Consumer();
        while (consumer.members.size() >= 0 && consumer.members.size() < SIZE && availableOutposts.size() > 0){
          consumer.members.add(availableOutposts.remove(0));
        }
        
        int[] area = new int[2];
        for (Consumer existing : consumers)
        	area[existing.area]++;
        
        if (area[0] < area[1])
        	consumer.area = 0;
        else
        	consumer.area = 1;
        
        consumers.add(consumer);
      }

      // For any consumer that still doesn't have enough members, destroy it
      ArrayList<Consumer> builtThisTurn = (ArrayList<Consumer>)consumers.clone();
      for (Consumer consumer : builtThisTurn) {
        if (consumer.members.size() < SIZE) {
          for (Outpost outpost : consumer.members)
            outpost.setStrategy(null);
          consumers.remove(consumer);   // Be careful about doing this inside a loop
        }
      }

      //Run strategy for each consumer
      for (Consumer consumer : consumers) {
        runConsumer(consumer,board);
      }
    }
    else{
      for (Outpost outpost : outposts)
        outpost.setStrategy(null);
    }
  }

  private void runConsumer(Consumer consumer, Board board){
    //Check if any outposts haven't been assigned a role
    ArrayList<String> filledRoles = new ArrayList<String>();
    for (Outpost outpost : consumer.members){
      if (outpost.memory.containsKey("role"))
        filledRoles.add((String)outpost.memory.get("role"));
    }
    if (filledRoles.size() < SIZE){
      //Go back to original assignment point to reform the formation
      consumer.state = State.ASSIGN;
      for (Outpost outpost : consumer.members){
        if (outpost.memory.containsKey("role"))
          filledRoles.remove((String)outpost.memory.get("role"));
      }
      //Assign unassigned roles to new outposts
      for (Outpost outpost : consumer.members){
        if(!outpost.memory.containsKey("role") && !filledRoles.isEmpty()){
          outpost.memory.put("role",filledRoles.remove(0));
        }
      }
    }

    switch (consumer.state){

      case ASSIGN:
        //Pick a location where we'll form the consumer that's close to all outposts
        double avg_x = 0.0;
        double avg_y = 0.0;
        for (Outpost outpost : consumer.members){
          Loc l = outpost.getCurrentLoc();
          avg_x += l.x;
          avg_y += l.y;
        }
        avg_x /= consumer.members.size();
        avg_y /= consumer.members.size();
        consumer.myCenter = board.nearestLand(new Loc((int)avg_x,(int)avg_y));
  
        //Assign each outpost its position
        consumer.members.get(UL).memory.put("role","UL");
        consumer.members.get(BL).memory.put("role","BL");
        consumer.members.get(BR).memory.put("role","BR");
        consumer.members.get(UR).memory.put("role","UR");

        //Set state to BUILD and fall through
        consumer.state = State.BUILD;

      case BUILD: 
        buildMove(consumer,board);
        break;

      case ATTACK: 
        attackMove(consumer,board);
        break;

    }

  }

  //Build a formation centered at consCenter
  private void buildMove(Consumer consumer, Board board){
    int numInPosition = 0;

    //set the target locations
    setFormationLocs(consumer, board);

    //Are we in formation?
    for (Outpost outpost : consumer.members){
      if (outpost.getCurrentLoc().equals(outpost.getTargetLoc()))
        numInPosition++;
    }

    if(numInPosition == SIZE){
      //We have fully formed a consumer, let's attack!
      consumer.state = State.ATTACK;
      for (Outpost outpost : consumer.members)
        outpost.memory.put("expectedSpot",outpost.getExpectedLoc());
      attackMove(consumer,board);
    }
  }

  //Can we rebuild without water being in the way?
  private boolean formationIsClear(Consumer consumer,Board board){
	Loc c = consumer.myCenter;
    if (board.getCell(c).isWater())
      return false;
    c.x++;
    if (board.getCell(c).isWater())
      return false;
    c.y++;
    if (board.getCell(c).isWater())
      return false;
    c.x--;
    if (board.getCell(c).isWater())
      return false;
    c.y--;
    return true;
  }

  private Consumer nearestConsumerToHomeCell(int id, Board board) {
	  Consumer nearest = null;
	  int minDist = Integer.MAX_VALUE;
	  
	  for (Consumer consumer : consumers) {
		  if (consumer.myCenter == null)
			  continue;
		  
		  int dist = board.getCell(consumer.myCenter).getPathDistanceToHome(id);
		  
		  if (dist < minDist) {
			  minDist = dist;
			  nearest = consumer;
		  }
	  }
	  
	  return nearest;
  }
  
  //Set the next move for our consumer formation to attack the closest enemy
  private void attackMove(Consumer consumer, Board board){

    int enemyDist = Integer.MAX_VALUE;

    ArrayList<Loc> enemyOutposts = new ArrayList<Loc>();
    ArrayList<Outpost> outOfFormation = new ArrayList<Outpost>();

    for (Outpost outpost : consumer.members){
      Loc spot = (Loc)outpost.memory.get("expectedSpot");
      Loc realSpot = outpost.getExpectedLoc();
      if(spot.x != realSpot.x || spot.y != realSpot.y){
        outOfFormation.add(outpost);
      }
    }

    //If either coordinate of an outpost's location are incorrect
    //with respect to its formation assignment, and we can rebuild on land cells, we need to rebuild
    if (!outOfFormation.isEmpty() && formationIsClear(consumer,board)){
      //System.out.println("rebuild");
      consumer.state = State.BUILD;
      buildMove(consumer,board);
    }
    else{
      // Pick an enemy to target
      ArrayList<Loc> path;
      
      for (int i = 0; i < 4; i ++){
    	if (i == board.playerId)
    		continue;
    	
    	enemyOutposts = board.theirOutposts(i);
        
    	if (enemyOutposts.size() <= 1 && consumer == nearestConsumerToHomeCell(i, board)) {
    		consumer.targetCenter = board.getHomeKillCell(i);
    	} else {
	        for (Loc outpost : enemyOutposts){
	          if (((consumer.area == 0 && outpost.x >= outpost.y) || (consumer.area == 1 && outpost.x < outpost.y)) && outpost.x + outpost.y > board.dimension / 2) 
	        	  continue;
	          
	          // Quick hack to avoid targeting enemy outposts that are too "strong", i.e., have 3+ adjacent
	          if (board.adjacentOutposts(i, outpost).size() >= 3)
	        	  continue;
	        
	          //int tempDist = board.getCell(outpost).getPathDistanceToHome(board.playerId) - board.getCell(outpost).getPathDistanceToHome(i);
	          int tempDist = board.getCell(outpost).getPathDistanceToHome(board.playerId);
	          path = board.findPathUL(new Loc(consumer.myCenter), new Loc(outpost));
	          
	          if (path == null || path.size() <= 1)
	        	  continue;
	          
	          tempDist += path.size() - 1;
	          
	          boolean targeted = false;
	          for (Consumer c : consumers){
	            if (c.targetCenter != null && c.targetCenter.equals(outpost))
	              targeted = true;
	          }
	          if (tempDist < enemyDist && !targeted){
	            enemyDist = tempDist;
	            consumer.targetCenter = outpost;
	          }
	        }
    	}
      }
      //System.out.println("Formation " + formationNum + " going for enemy " + targets.get(formationNum));
      
      if (consumer.targetCenter == null)
    	  consumer.targetCenter = new Loc(consumer.myCenter);
      
      path = board.findPathUL(consumer.myCenter, consumer.targetCenter);
      
      Loc newCenter = new Loc();
      if (path == null || path.size() == 0 || path.size() == 1) {
        newCenter.x = consumer.myCenter.x;
        newCenter.y = consumer.myCenter.y;
      } else {
        newCenter.x = path.get(1).x;
        newCenter.y = path.get(1).y;
      }

      //Set new formation center
      consumer.myCenter = newCenter;

      setFormationLocs(consumer,board);
    }
  }

  //Given the center of a formation, assign the consumer outposts to their associated positions
  private void setFormationLocs(Consumer consumer, Board board){
    Loc formationCenter = consumer.myCenter;
    for (Outpost outpost : consumer.members){    	
      if(outpost.memory.get("role").equals("UL"))
        outpost.setTargetLoc(board.nearestLand(new Loc(formationCenter.x,formationCenter.y)));
      else if(outpost.memory.get("role").equals("UR"))
        outpost.setTargetLoc(board.nearestLand(new Loc(formationCenter.x + 1,formationCenter.y)));
      else if(outpost.memory.get("role").equals("BL"))
        outpost.setTargetLoc(board.nearestLand(new Loc(formationCenter.x,formationCenter.y + 1)));
      else if(outpost.memory.get("role").equals("BR"))
        outpost.setTargetLoc(board.nearestLand(new Loc(formationCenter.x + 1,formationCenter.y + 1)));
    	
      outpost.memory.put("expectedSpot",outpost.getTargetLoc());
    }
  }
}

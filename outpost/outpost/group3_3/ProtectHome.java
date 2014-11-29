package outpost.group3_3;

import java.util.ArrayList;

import outpost.group3_3.Outpost;

public class ProtectHome extends outpost.group3_3.Strategy{
	ProtectHome() {}
	
	public void run(Board board, ArrayList<Outpost> outposts) {
		if (outposts.size() != 3) {
			for (Outpost outpost : outposts)
				outpost.setStrategy(null);
			
			return;
		}
		
		outposts.get(0).setTargetLoc(board.nearestLand(new Loc(0, 0)));
		outposts.get(1).setTargetLoc(board.nearestLand(new Loc(0, 1)));
		outposts.get(2).setTargetLoc(board.nearestLand(new Loc(1, 0)));
		
		for (int i = 0; i < outposts.size(); i++)
			if (i >= 3)
				outposts.get(i).setStrategy(null);
	}
}

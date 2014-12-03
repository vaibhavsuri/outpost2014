package outpost.group4_3;

import java.util.*;

public class MaxTerritoryHeuristic extends BoardHeuristic {

    private AvailableTerritoryGridSquareFilter filter;

    public MaxTerritoryHeuristic(Board board) {
        super(board);

        filter = new AvailableTerritoryGridSquareFilter();
    }

    public double score(GridSquare square) {
        return (double) board.filteredSquaresWithinRadius(square, filter).size();
    }
}

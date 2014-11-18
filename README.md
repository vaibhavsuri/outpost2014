outpost2014
===========

 Project 4: Outpost  Four empires are competing for territory in a landscape modeled as a 100x100 grid. Each of the four empires has a home cell in a different corner or the grid. The goal of each empire is to grow, and to control as much territory as possible after a large number T of turns. You will write code to control the moves of each empire. Four players will be playing in each game, interacting with a simulator that will administer the rules as described below.  The landscape is symmetric: a 50x50 region will be replicated four times with rotational symmetry to cover the entire 100x100 grid. All players will therefore have the same view of the landscape. The 50x50 region will be read from a file by the game simulator. Some initial maps will be supplied; groups will be asked to submit additional maps. The dynamics of the game are likely to depend significantly on the map, as well as several game parameters mentioned below.  Cells in the grid can be either land or water. There will be four cells of land for each cell of water. Water cells are likely to be somewhat contiguous, forming lakes and rivers, but water cells do not partition the 50x50 region. In other words, on any valid map it will always be possible to reach a land cell from any other land cell using orthogonal moves over land cells (no diagonal moves in this game). The corners of the 50x50 region must be land cells. A map is specified simply by a sequence of x and y coordinate pairs (one pair per line, $0\leq x,y\leq 49$) of the 500 water cells.  At any point in the game each empire will have a number of outposts located at various points in the grid. At the start of the game, each empire has a single outpost located on its home cell. On any turn, an empire can move each outpost orthogonally by one cell in any direction that contains a land cell. Every 10 turns (a ``season'') each empire determines the total area controlled by its outposts. The number of outposts that the empire can afford depends on this area. If the empire can afford additional outposts, it gets to build one new outpost on the home cell. (At most one outpost can be built each season, even if the area controlled justifies more than one additional outpost.) If the empire loses territory and cannot afford its current outposts, then the empire must disband one outpost of its choice. Disbanded outposts are immediately removed from the map. This disbandment may lead to a further reduction in what the empire can afford, and a subsequent disbandment in the next season, etc.  Outposts have a radius of influence r, which is a parameter that we will vary. Since we're using orthogonal moves, distances are computed using the Manhattan metric, and so the area within the radius of r looks like a diamond, and covers exactly 2r2+2r+1 cells. Cells may be controlled by empires if they are within the radius of influence of an outpost. In particular, for each cell C, let d be the distance from C to the closest outpost(s).      If d>r, then C is neutral.     If $d \leq r$ and multiple empires have outposts at distance d from C, then C is disputed.     If $d \leq r$ and a single empire E has outposts at distance d from C, then C is controlled by E.  To be able to afford n outposts, an empire must control at least (n-1)L land cells, and at least (n-1)W water cells, where L and W are parameters that we shall vary along with r. Because of the (n-1) term, every empire can always afford at least one outpost. If L:W is something other than 4:1, then one type of cell is more valuable than the other given its frequency in the landscape.  Outposts for empire E must always maintain a supply line to E's home cell. What that means is that every outpost must have a path (orthogonal, traversing land cells only) to the home cell that passes through cells that are either neutral or controlled by E. If after a given turn an outpost loses its supply line, the simulator will automatically disband the outpost.  Multiple outposts can occupy the same cell. While there is no long-term advantage for an empire to co-locate outposts (they control less area than if they were separated) co-location may be transiently helpful as outposts move within the landscape. Outposts from different empires that occupy the same cell will create disputed cells in their immediate vicinity. Under most conditions these disputed cells would break supply lines for both outposts, and they would both be disbanded. (When would mutual disbandment not happen?)  All players have complete information about the map and the locations of all players' outposts.  At the end of the project we'll run tournaments for a variety of maps with various values for the game parameters. Some of the tournaments will use previously unseen maps. 

package outpost.group4;

import java.util.*;

public class AggressiveDefensiveStrategy implements Strategy {

    ArrayList<Post> posts;

    public ArrayList<Post> move(ArrayList<ArrayList<Post>> otherPlayerPosts, ArrayList<Post> posts, boolean newSeason) {
      this.posts = posts;

      ArrayList<Post> newPosts = new ArrayList<Post>();
      GridSquare[][] gridSquares = Player.board.getGridSquares();

      for (Post p : posts) {
        Post newPost = p;

        if (p.id % 2 == 0) {
          newPost = aggressiveMove(p);
        } else {
          newPost = defensiveMove(p);
        }

        newPosts.add(newPost);
      }

      return newPosts;
    }

    public Post aggressiveMove(Post p) {
      Post newPost = null;

      ArrayList<Post> nearPosts = p.postsUnderInfluence(posts);
      if (nearPosts.size() > 0) {
        Post nearestPost = p.nearestPost(nearPosts);
        if (nearestPost.equals(p)) {
          newPost = p.preferredAdjacency();
        } else {
          newPost = p.moveMaximizingDistanceFrom(nearestPost);
        }
      }
      else {
        GridSquare water = p.furthestWater();
        newPost = p.moveMinimizingDistanceFrom(water);
      }

      if (newPost == null) newPost = p;
      return newPost;
    }

    public Post defensiveMove(Post p) {
      Post myAgressivePartner = (p.id > 0)? posts.get(p.id - 1) : posts.get(p.id + 1);
      GridSquare water = myAgressivePartner.nearestWater();
      Post newPost = p.moveMinimizingDistanceFrom(water);
      if (newPost == null) newPost = p;
      return newPost;
    }

}

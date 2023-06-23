import java.util.*;
// interface allowing specification of movement rules
interface MoveRule {
    // issues a move command to the given mover based on a rule
    // returns true if it actually moved, false if it couldn't/didn't
	public String getId();
	public Point getTarget();
    boolean move(Mover m);
}

// rule where no movement is ever done
class StandStill implements MoveRule {
	
	public String getId() {
		return "still";
	}
	public Point getTarget() {
		return null;
	}
    public boolean move(Mover m) {
        return false;
    };
}

// rule to move in random direction at all times
class RandomMovement implements MoveRule {
	public String getId() {
		return "random";
	}
    public boolean move(Mover m) {
        double angle = Math.random() * Math.PI * 2;
        
        return m.moveTo(m.x + Math.cos(angle) * m.speed,
                        m.y + Math.sin(angle) * m.speed);
        
    }
    public Point getTarget() {
		return null;
	}
}

// rule to move towards a Point (which could be moving itself)
class MoveTo implements MoveRule {
    Point target;
    public String getId() {
		return "moveTo";
	}
    MoveTo(Point target) {
        this.target = target;
    }
    public Point getTarget() {
		return target;
	}
    
    public boolean move(Mover m) {
    	
        return m.moveTo(target);
        
    }
}

class RandomEdge implements MoveRule{
	Point target;
	Point player;
	WallSet wsCurr;
	public String getId() {
		return "randomEdge";
	}
	
	public Point getTarget() {
		return target;
	}
	
	
	RandomEdge(WallSet wsCurr, Point player) {
		this.wsCurr = wsCurr;
		this.player = player;
	}

	public boolean move(Mover m) {
		if((target == player && m.distTo(target) ==0)) return false;
		if(m.distTo(player) == 0) return false;
		if(target == null || m.distTo(target) == 0 || wsCurr.isClearPath(m, target) == false) {
			target = m.getRandomNeighbor();
			//System.out.println(target);
		}
		if(!(target == null)) {
			return m.moveTo(target);
		}
		return false;
		
		
		
	}
	
}

class Bfs implements MoveRule{
	Point target;
	Point player;
	WallSet wsCurr;
	public String getId() {
		return "bfs";
	}
	Bfs(WallSet wsCurr, Point player){
		this.wsCurr = wsCurr;
		this.player = player;
	}
	
	public boolean move(Mover m) {
		
		if(target == player && m.distTo(target) ==0) {
			return false;
		}
		else if(target == null || m.distTo(target) == 0 || wsCurr.isClearPath(m, target) == false) {
			target = m.bfsPoint(player);

		}
		if(!(target == null)) {
			return m.moveTo(target);
		}
		return false;
	}
	public Point getTarget() {
		return target;
	}
}

class Dijkstra implements MoveRule{
	Point target;
	Point player;
	WallSet wsCurr;
	public String getId() {
		return "dijkstra";
	}
	
	Dijkstra(WallSet wsCurr, Point player){
		this.wsCurr = wsCurr;
		this.player = player;
	}
	public boolean move(Mover m) {
		if(target == player && m.distTo(target) == 0) {
			return false;
		}
		else if(target == null || m.distTo(target) == 0 || wsCurr.isClearPath(m, target) == false) {
			target = m.dijkstraPoint(player);

		}
		if(!(target == null)) {
			return m.moveTo(target);
		}
		return false;
	}
	public Point getTarget() {
		return target;
	}
}

class Dfs implements MoveRule{
	Point target;
	Point player;
	WallSet wsCurr;
	int index = 0;
	public String getId() {
		return "dfs";
	}
	List<Point> route = new ArrayList<>();
	Dfs(WallSet wsCurr, Point player){
		this.wsCurr = wsCurr;
		this.player = player;
	}
	
	public boolean move(Mover m) {
		

		
		
		
		//if we are close to player, dont have move rule
		if(m.distTo(player) < 3) return false;
		
		
		
		//the next 2 if statements have same action, but i seperated them for clarity
		
		
		//dont have route, get route and reset index
		if(route.isEmpty()) {
			route = m.dfsPath(player, m);			
			index = 0;
			target = route.get(index);
			//System.out.println(target +"route is empty");
			System.out.println(route);
		}
		//if the target is player and the path is not clear, reset route
		if(target == player && wsCurr.isClearPath(m, target) == false) {
			route = m.dfsPath(player, m);			
			index = 0;
			target = route.get(index);	
			//System.out.println(target +"player target is not good");
			System.out.println(route);
		}
		
		
		//if we are at the target, increase the target index for new target
		if((m.distTo(target) == 0 || wsCurr.isClearPath(m, target)== false) && route.size()!=1){
			index++;
			target = route.get(index);
			//System.out.println(target +"we at target so new one");
			
		}
		
		
		// move to the target
		if(!(target == null)) {
			return m.moveTo(target);
			
		}
		
		return false;
	}
	public Point getTarget() {
		return target;
	}
}








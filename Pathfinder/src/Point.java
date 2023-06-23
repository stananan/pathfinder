import java.util.*;


import processing.core.PApplet;

// simple class representing a point in 2d space, which
//   may or may not be part of a Wall
class Point {
    float x, y;
    Wall wall;
    int id;
    
    //myCode
    Set<Point> neighbors = new HashSet<>();
    
    static int nextId = 0;
    
    static void resetIds(int next) {
        nextId = next;
    }

    // random point
    Point(PApplet pa) {
        x = pa.random(0, pa.width);
        y = pa.random(0, pa.height);
        id = nextId++;
    }

    // specific point
    Point(float x, float y) {
        this.x = x;
        this.y = y;
        id = nextId++;
    }

    // copy constructor
    Point(Point other) {
        this.x = other.x;
        this.y = other.y;
        id = nextId++;
    }

    // default display (bright red)
    void display(PApplet pa) {
        display(pa, 0, 100, 100);
    }
   
    
    Point getRandomNeighbor() {


    	Random random = new Random();
    	if(this.neighbors.size() <= 0) return null;
    	int randomIndex = random.nextInt(this.neighbors.size());
    	
    	int c = 0;
    	Point randomPoint = null;
    	for(Point rand : this.neighbors) {
    		randomPoint = rand;
    		if(c == randomIndex) return rand;
    		c++;
    	}
    	return randomPoint;
    	
    }
    
    Point bfsPoint(Point player) {
    	if(this == player) {
    		return this;
    	}
    	Queue<Point> queue = new LinkedList<>();
    	Set<Point> visited = new HashSet<>();
    	Map<Point, Point> parentMap = new HashMap<>();
    	
    	queue.add(this);
    	visited.add(this);
    	parentMap.put(this, null);
    	while(!queue.isEmpty()) {
    		Point currentPoint = queue.poll();
    		for(Point neighbor : currentPoint.neighbors) {
    			if(!visited.contains(neighbor)) {
    				queue.add(neighbor);
    				visited.add(neighbor);
    				parentMap.put(neighbor, currentPoint);
    				
    				if(neighbor == player) {
    					
    					return getFirstPointOfPath(parentMap, neighbor);
    					
    				}
    			}
    		}
    	}
    	
    	return null;
    }
    Point dijkstraPoint(Point player) {
    	PriorityQueue<Step> pq = new PriorityQueue<>(
			(a,b) -> {
				if(a.totalPathDist < b.totalPathDist) return -1;
				if(a.totalPathDist > b.totalPathDist) return 1;
				return 0;
				
			}
		);
    	Set<Point> visited = new HashSet<>();
    	Map<Point, Point> parentMap = new HashMap<>();
    	pq.add(new Step(this, null));
    	visited.add(this);
    	parentMap.put(this, null);
    	while(!pq.isEmpty()){
    		Step s = pq.poll();
    		for(Point neighbor : s.to.neighbors) {
    			if(!visited.contains(neighbor)) {
    				pq.add(new Step(neighbor, s));
    				visited.add(neighbor);
    				parentMap.put(neighbor, s.to);
    				if(neighbor == player) {
    					return getFirstPointOfPath(parentMap, neighbor);
    				}
    			}
    		}
    	}
    	
    	return null;
    }
    private Point getFirstPointOfPath(Map<Point, Point> parentMap, Point neighbor) {
		Point currentPoint = neighbor;
		while(parentMap.containsKey(currentPoint)) {
			Point parentPoint = parentMap.get(currentPoint);
			if(parentPoint == this) {

				return currentPoint;
				
			}
			currentPoint = parentPoint;
			
		}
		return null;
	}
   

    List<Point> dfsPath(Point player, Mover m){
    	
    	Set<Point> visited = new LinkedHashSet<>();
    	dfs(m, visited, player);
    	
    	List<Point> path = new LinkedList<Point>(visited);
    	return path;
    }
    void dfs(Point n, Set<Point> visited, Point player){
    	
    	if(visited.contains(n)) return;
    	if(visited.contains(player)) return;
    	visited.add(n);
    	if(n == player) return;
    	
	    for(Point neighbor: n.neighbors) {
	    		
	    	
	    	dfs(neighbor, visited, player);
	    }
	    	
    }
	
	// display with custom color
    void display(PApplet pa, int h, int s, int b) {
        pa.stroke(h,s,b);
        pa.strokeWeight(3);  // medium
        pa.noFill();
        pa.ellipse(x, y, 10, 10);
    }
    
    String getLabel() {
        // determine label string by id in style of
        // A,B,..Z,AA,AB,..AZ,BA,BB,..BZ,...ZA,ZB,..ZZ,AAA....
        int id2 = id;
        String txt = "";
        while (id2 > 0) {
            char c = (char) ('A' + id2 % 26);
            id2 /= 26;
            
            txt = c + txt;
        }
        
        return txt;
    }
    
    void displayLabel(PApplet pa) {
        String txt = getLabel();
        
        // determine position
        float lblX = x + 10;  // down/right of main pt by default
        float lblY = y + 10;
        
        if (wall != null) {
            Point wo = wOpposite();
            float dx = this.x - wo.x;
            float dy = this.y - wo.y;
            float d = (float) Math.sqrt(dx*dx + dy*dy);
            
            if (d == 0) {
                // this should never happen, but if it does, one end
                //   of the wall is labeled the other way
                lblX = x - 10;
                lblY = y + 10;
            }
            else {
                // position label 10 px further away from the opposite endpoint
                dx *= 10 / d;
                dy *= 10 / d;
                lblX = x + dx;
                lblY = y + dy;
            }
        }
        
        pa.fill(0);
        pa.textSize(18);
        pa.textAlign(PApplet.CENTER, PApplet.CENTER);
        pa.text(txt, lblX, lblY);
    }
    
    // opposite wall endpoint from this one 
    Point wOpposite() {
        if (wall == null) return null;
        return wall.opposite(this);
    }

    // distance to other point
    double distTo(Point other) {
        float dx = other.x - this.x;
        float dy = other.y - this.y;
        return Math.sqrt(dx*dx + dy*dy);
    }

    // finds intersection of line a1-a2 with b1-b2
    //   or null if there is none
    static Point intersection(Point a1, Point a2, Point b1, Point b2) {
        // if line b is vertical, swap it with line a, so "a is vertical"
        //   code will apply just as easily to the opposite case
        if (b1.x == b2.x) {
            Point c1 = a1;
            Point c2 = a2;
            a1 = b1;
            a2 = b2;
            b1 = c1;
            b2 = c2;
        }
        // line a is vertical
        if (a1.x == a2.x) {
            // both are vertical -> parallel -> no intersection
            if (b1.x == b2.x) return null;

            // line b is left or right of line a
            if (Math.min(b1.x, b2.x) < a1.x ||
                    Math.max(b1.x, b2.x) > a1.x) return null;

            // calculate intercept
            float yIntercept = b1.y + (b1.y-b2.y)/(b1.x-b2.x)*(a1.x - b1.x);

            // line b is above or below line a
            if (yIntercept < Math.min(a1.y, a2.y) ||
                    yIntercept > Math.max(a1.y, a2.y)) return null;

            // line b does intercept a at its x coordinate
            return new Point(a1.x, yIntercept);
        }

        // we now know neither line is vertical
        // we wish to find a point where ya = yb, xa = xb
        //   point-slope form -> slope-intercept
        //     y - y1 = m(x-x1)
        //     y = mx + (y1 - mx1)
        //   set y's equal to solve the system of equations
        //     ma*x + (y1a - ma*x1a) = mb*x + (y1b - mb*x1b)
        //     x*(ma - mb) = y1b - mb*x1b - y1a + ma*x1a
        //     x = (y1b - mb*x1b - y1a + ma*x1a) / (ma - mb)

        float aSlope = (a1.y - a2.y) / (a1.x - a2.x);
        float bSlope = (b1.y - b2.y) / (b1.x - b2.x);

        // parallel lines don't intersect
        if (aSlope == bSlope) return null;

        // use solution above to find where xs intersect
        float xIntercept = (b1.y - bSlope*b1.x - a1.y + aSlope*a1.x)
                / (aSlope - bSlope);

        // check if x is outside of bounds of either line
        if (xIntercept < Math.min(a1.x, a2.x) ||
            xIntercept > Math.max(a1.x, a2.x) ||
            xIntercept < Math.min(b1.x, b2.x) ||
            xIntercept > Math.max(b1.x, b2.x)) return null;

        // lines do truly intersect; calculate y and return
        float yIntercept = a1.y + (xIntercept - a1.x) * aSlope;
        return new Point(xIntercept, yIntercept);
    }

    public String toString() {
        return getLabel() + "(" + x + "," + y + ")";
    }

    // equal if two Points have same coordinates
    public boolean equals(Object other) {
        if (!(other instanceof Point)) return false;

        Point pOther = (Point) other;
        return this.x == pOther.x && this.y == pOther.y;
    }
}
class Step{
	Point to, from;
	float totalPathDist;
	
	Step(Point t, Step prev){
		if(prev == null) {
			to = t;
		}
		else {
			to = t;
			from = prev.to;
			totalPathDist = prev.totalPathDist + (float) to.distTo(from);
		}
	}
}
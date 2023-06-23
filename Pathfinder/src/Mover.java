import java.util.List;

import processing.core.PApplet;
import processing.core.PImage;

// defines a moving point, aka player or enemy
public class Mover extends Point {
    // rule that will be followed when point moves
    MoveRule rule;
    float speed = 2.0f;  // top speed allowed for point
    float hue;
    PImage image;
    
    // per-frame tracking of whether this has moved
    boolean moved;

    // Mover must remember its Pathfinder instance so it can
    //   move without crashing into walls
    Pathfinder pf;

    // default mover doesn't actually move
    Mover(Pathfinder pf) {
        this(pf, new StandStill());
    }

    // mover with specific movement system/pattern
    Mover(Pathfinder pf, MoveRule rule) {
        super(pf);
        this.pf = pf;
        this.rule = rule;
        hue = (float) (Math.random() * 360);
    }
    
    void resetFrame() {
        moved = false;
    }

    // make a single move based on current rule
    // return whether a move actually happened
    boolean move() {
        return rule.move(this);
    }

    // attempt to move in direction of another point
    boolean moveTo(Point p) {
        return moveTo(p.x, p.y);
    }

    // attempt to move toward coordinates (x2, y2)
    boolean moveTo(double x2, double y2) {
        return moveTo((float)x2, (float)y2);
    }

    boolean moveTo(float x2, float y2) {
        // block double-moves
        if (moved) {
            System.err.println(this + " - Attempting to move twice in one frame!");
            return false;
        }
        
        float dx = x2 - x;
        float dy = y2 - y;

        double d = Math.sqrt(dx*dx + dy*dy);

        // don't move
        if (d == 0) return false;

        // scale down movement if necessary
        if (d > speed) {
            dx *= speed / d;
            dy *= speed / d;
        }

        Point target = new Point(x + dx, y + dy);
        Point crashPoint = crashCheck(target);

        // move to target if nothing blocks it
        if (crashPoint == null) {
            x += dx;
            y += dy;
            moved = true;
            return true;
        }
        // move halfway to blockage, unless within 0.5px already
        else if (this.distTo(crashPoint) >= 0.5) {
            x = (x + crashPoint.x) / 2;
            y = (y + crashPoint.y) / 2;
            moved = true;
            return true;
        }
        
        return false;
    }

    // returns closest Point that would be crashed into by
    //   moving to target, or null if movement is unblocked
    Point crashCheck(Point target) {
        // special case: crash at the current point if trying to go
        //   to the end of a wall and currently at the other end
        Point targetWO = target.wOpposite();
        if (this.equals(targetWO)) return targetWO;
        
        Point closest = null;
        double closestD = Double.MAX_VALUE;

        List<Point> crashes = pf.wsCurr.intersections(this, target);

        // get closest among all potential crashes
        for (Point crash : crashes) {

            double crashD = this.distTo(crash);

            // get closest result
            if (crashD < closestD) {
                closest = crash;
                closestD = crashD;
            }
        }
        return closest;
    }

    
	@Override
    void display(PApplet pa) {
        pa.noStroke();
        pa.fill(hue, 100, 100);
        pa.ellipse(x, y, 10, 10);
        pa.fill(0);
        pa.textAlign(PApplet.CENTER, PApplet.CENTER);
        pa.textSize(12);
        pa.text(rule.getId(), x, y+5);
    }
}

// special Mover which is displayed differently and
//   always follows the mouse
class Player extends Mover {
	
    Player(Pathfinder pf) {
        super(pf, new MoveTo(pf.mouse));
    }

    @Override
    void display(PApplet pa) {
        pa.noStroke();
        pa.fill(hue, 100, 100); // light
        pa.ellipse(x, y, 20, 20);
        pa.fill(hue, 100, 50);  // darker
        pa.ellipse(x, y, 3, 3);
    }
}
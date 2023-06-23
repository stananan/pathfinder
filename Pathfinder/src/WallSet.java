import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import processing.core.PApplet;

// class to manage sets of walls / game "maps"
public class WallSet {
    final static int STACK_LIMIT = 200; // max size of undo/redo stack
    final static String PATH_PREFIX = "wallsets/";

    String name = null; // name of save file
    int mods = 0;       // # of modifications since last save

    // actual map data
    Set<Wall> walls = new HashSet<>();
    Set<Point> points = new HashSet<>();

    // NOTE: stacks of events representing what can be undone
    Deque<Event> undoStack = new ArrayDeque<>();
    Deque<Event> redoStack = new ArrayDeque<>();
    
    int nextPtId;

    // draw all walls in this set
    void display(PApplet pa) {
        for (Wall w : walls) w.display(pa);
    }
    
    void displayPtLabels(PApplet pa) {
        for (Point p : points) p.displayLabel(pa);
    }

    // USE THIS METHOD!
    // checks to see if there is a clear path from a->b (no intersection
    //   points with any walls)
    boolean isClearPath(Point a, Point b) {
    	if(a == null|| b == null) return false;
        for (Wall w : walls) {
            // crashing into any wall means no clear path
            if (w.intersection(a, b) != null) return false;
        }

        return true; // clear path if it hit no walls
    }

    // finds all points of intersection with walls along path from a->b
    List<Point> intersections(Point a, Point b) {
        List<Point> results = new ArrayList<>();

        for (Wall w : walls) {
            Point crash = w.intersection(a, b);
            if (crash != null) results.add(crash);
        }

        return results;
    }

    // displayable name + status
    String getName() {
        if (name == null) return "(untitled wall set)";

        return name + (mods == 0 ? "" : " (modified)");
    }

    // adds wall to this set, tracking the event
    void add(Wall w) {
        WallEvent we = new WallEvent(w, true);
        we.doEvent();

        mods++;
        undoStack.push(we);
        redoStack.clear();
    }

    // removes wall from this set, tracking the event
    void rem(Wall w) {
        WallEvent we = new WallEvent(w, false);
        we.doEvent();

        mods++;
        undoStack.push(we);
        redoStack.clear();
    }

    // lock in movement for point p to its current position,
    //   from an old/previous position, tracking the event
    void finishMove(Point p, Point old) {
        if (!points.contains(p)) return;

        MoveEvent me = new MoveEvent(p, old);

        mods++;
        undoStack.push(me);
        redoStack.clear();
    }

    // reverts to saved data from file, tracking the event
    void revert() {
        if (name == null) return;

        Set<Wall> backupWalls = walls;
        Set<Point> backupPoints = points;

        WallSet fromFile = fromFile(name);
        if (fromFile == null) return;

        walls = fromFile.walls;
        points = fromFile.points;

        RevertEvent re = new RevertEvent(backupWalls, backupPoints);

        mods = 0;
        undoStack.push(re);
        redoStack.clear();
    }

    // message about what would be undone
    String undoPeek() {
        if (undoStack.isEmpty()) return "nothing to undo";
        else                     return "undo " + undoStack.peek();
    }

    // message about what would be redone
    String redoPeek() {
        if (redoStack.isEmpty()) return "nothing to redo";
        else                     return "redo " + redoStack.peek();
    }

    // reverses last action to "undo" it, transferring it to redo stack
    boolean undo() {
        if (undoStack.isEmpty()) return false;

        Event ev = undoStack.pop();
        System.out.println("undoing " + ev);
        ev.undoEvent();
        redoStack.push(ev);
        if (redoStack.size() > STACK_LIMIT) redoStack.removeLast();

        mods--;
        return true;
    }

    // redoes last action undone
    boolean redo() {
        if (redoStack.isEmpty()) return false;

        Event ev = redoStack.pop();
        ev.doEvent();
        undoStack.push(ev);
        if (undoStack.size() > STACK_LIMIT) undoStack.removeLast();

        mods++;
        return true;
    }

    // saves wall data in JSON format to file with stored name, if any
    boolean save() {
        if (name == null) return false;

        String path = PATH_PREFIX + name;

        // NOTE: "try using" block allows assignment to any AutoCloseable
        //   object, which will automatically be cleaned up at end of
        //   try/catch/finally block to ensure all resources are closed
        try ( PrintWriter out = new PrintWriter(new File(path)) ) {
            out.println("[");
            for (Wall w : walls) {
                out.println("  " + w.toJson() + ",");
            }
            out.println("]");
        }
        catch (FileNotFoundException fnfe) {
            System.err.println("Failed to save " + path);
            System.err.println("  " + fnfe.getMessage());
            return false;
        }

        mods = 0;
        System.out.println("Saved " + path);
        return true;
    }

    // produces WallSet from file in correct JSON format
    //   (2d array of wall coordinates)
    static WallSet fromFile(String name) {
        String path = PATH_PREFIX + name;
        return fromFile(new File(path));
    }

    // produces WallSet from file in correct JSON format
    //   (2d array of wall coordinates)
    static WallSet fromFile(File f) {

        StringBuilder jsonSB = new StringBuilder();
        try ( Scanner in = new Scanner(f) ) {
            while (in.hasNextLine()) {
                // NOTE: remove all spacing from file with regex help
                jsonSB.append(in.nextLine().replaceAll("\\s+", ""));
            }
        }
        catch (FileNotFoundException fnfe) {
            System.err.println("Could not read " + f.getName());
            System.err.println("  " + fnfe.getMessage());
            return null;
        }
        String json = jsonSB.toString();

        // NOTE: regexes!!!

        String wallPatt = "\\[(\\d+\\.\\d+),(\\d+\\.\\d+),(\\d+\\.\\d+),(\\d+\\.\\d+)]";
        // wall pattern: 4 comma separated decimals in square brackets
        String filePatt = "\\[("+wallPatt+",)*("+wallPatt+")?]";
        // file pattern: any number of comma-separated repetitions of the wall pattern,
        //   optionally ending with one wall pattern without a comma, in square brackets

        if (!json.matches(filePatt)) {
            System.err.println("Invalid file pattern in " + f.getName());
            return null;
        }

        WallSet result = new WallSet();
        result.name = f.getName();
        
        Point.resetIds(0);

        // NOTE: regex to extract # parts of each wall, then parsing
        Matcher m = Pattern.compile(wallPatt).matcher(json);
        while (m.find()) {
            float x1 = Float.parseFloat(m.group(1));
            float y1 = Float.parseFloat(m.group(2));
            float x2 = Float.parseFloat(m.group(3));
            float y2 = Float.parseFloat(m.group(4));

            Wall w = new Wall(x1, y1, x2, y2);
            result.walls.add(w);
            result.points.add(w.p1);
            result.points.add(w.p2);
        }
        
        result.nextPtId = Point.nextId;

        System.out.println("Loaded walls from " + f.getName());
        return result;
    }

    // allows all events to be done or undone
    interface Event {
        void doEvent();
        void undoEvent();
        // NOTE: "contract" of interface
        //   Events may generally assume that when doEvent() is called,
        //   it is called on an Event in the redo stack, and will then
        //   be moved to the undo stack; and vice versa
    }

    // addition or removal of wall
    class WallEvent implements Event {
        Wall w;
        boolean adding;  // false: wall was removed, not added

        WallEvent(Wall ww, boolean aa) {
            w = ww; adding = aa;
        }

        public void doEvent() { handleEvent(false); }
        public void undoEvent() { handleEvent(true); }

        void handleEvent(boolean undo) {
            // "xor" operation: redoing an add and undoing a remove are the same
            if (adding && !undo || !adding && undo) {
                walls.add(w);
                points.add(w.p1);
                points.add(w.p2);
            }
            else {
                walls.remove(w);
                points.remove(w.p1);
                points.remove(w.p2);
            }
        }

        public String toString() {
            return (adding ? "add" : "rem") + w.p1 + "-" + w.p2;
        }
    } // end class WallEvent

    // movement of a point
    class MoveEvent implements Event {
        Point p;
        // point that is moving
        Point other;
        // "other end" of the movement
        //  - previous position, if in undo stack
        //  - position after movement, if in redo stack

        MoveEvent(Point p, Point other) {
            this.p = p;
            this.other = other;
        }

        // do and undo are the same: swap point with its alternate
        //   position
        // this event MUST be moved from undo stack to redo stack
        //   or vice versa when triggered, or else discarded;
        //   otherwise the "other" point will not correspond to
        //   desired behavior
        public void doEvent() {
            float temp = p.x;
            p.x = other.x;
            other.x = temp;

            temp = p.y;
            p.y = other.y;
            other.y = temp;
        }
        public void undoEvent() { doEvent(); }

        public String toString() {
            return "move" + other;
        }
    } // end class MoveEvent

    class RevertEvent implements Event {
        Set<Wall> otherWalls;
        Set<Point> otherPoints;
        // "other end" of the reversion
        //  - if in undo stack, the state before reverting
        //  - if in redo stack, the state after reverting
        //      (which may no longer actually be the same as what you
        //       would get from the file if you reverted again)

        // stores walls from just before reversion
        RevertEvent(Set<Wall> otherWalls, Set<Point> otherPoints) {
            this.otherWalls = otherWalls;
            this.otherPoints = otherPoints;
        }

        // do and undo are the same: swap wall/point data with
        //   its alternate data
        // this event MUST be moved from undo stack to redo stack
        //   or vice versa when triggered, or else discarded;
        //   otherwise the "other" state will not correspond to
        //   desired behavior
        public void doEvent() {
            Set<Wall> tempWalls = otherWalls;
            otherWalls = walls;
            walls = tempWalls;

            Set<Point> tempPoints = otherPoints;
            otherPoints = points;
            points = tempPoints;
        }
        public void undoEvent() { doEvent(); }

        public String toString() {
            return "revert";
        }
    }
}

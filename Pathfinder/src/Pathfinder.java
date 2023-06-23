import java.io.File;
import java.util.*;
import processing.core.*;

public class Pathfinder extends PApplet {
    
    //==== PROCESSING 3 SETUP STUFF ====//
    public static void main(String[] args) {
        new Pathfinder().runSketch();
    }
    
    public void settings() {
        size(1000, 800);
    }
    //==== END PROCESSING 3 STUFF ====//

    // used for Serializable interface inherited via PApplet
    private static final long serialVersionUID = 1L;

    // ====================== //
    // ==== GENERAL CODE ==== //    (shared between modes)
    // ====================== //

    // WallSets are primary containers for the "map" that's edited
    //   and played on
    // wsCurr is always updated to refer to the current WallSet
    ArrayList<WallSet> wsList = new ArrayList<>();
    int wsIndex = 0;
    WallSet wsCurr;

    // Points primarily represent ends of walls but can also
    //   represent the mouse, moving objects in a game, etc
    Point mouse = new Point(0,0);

    // mode control variables
    Mode[] modes = new Mode[]{new BuildMode(), new PlayMode()};
    final int BUILD_MODE = 0;
    final int PLAY_MODE = 1;
    int modeIndex = BUILD_MODE;

    // modifier keys tracked here
    boolean ctrlHold, shiftHold;

    public void setup() {
        size(1000, 800);
        colorMode(HSB, 360, 100, 100, 100);
        // hue, saturation, brightness
        // 0-360   0-100      0-100
        // red-red

        loadWallSets();
        wsCurr = wsList.get(0);
        Point.resetIds(wsCurr.nextPtId);
    }

    // loads all valid wallsets in folder
    void loadWallSets() {
        File folder = new File("wallsets/");

        // check valid folder structure
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files.length == 0) {
                System.out.println("No files in wallsets folder.");
            }

            System.out.println("Loading from wallsets folder:");
            for (File wsFile : folder.listFiles()) {
                if (!wsFile.isDirectory()) {
                    WallSet ws = WallSet.fromFile(wsFile);
                    if (ws != null) wsList.add(ws);
                }
            }
            if (wsList.isEmpty()) {
                System.out.println("No valid files in wallsets folder.");
            }
        }
        else {
            System.err.println("Couldn't find wallsets folder! Should be at " +
                    folder.getAbsolutePath());
        }

        if (wsList.isEmpty()) wsList.add(new WallSet());
    }

    public void draw() {
        // keep mouse Point updated
        mouse.x = mouseX;
        mouse.y = mouseY;

        // draw based on mode
        if (0 <= modeIndex && modeIndex < modes.length) {
            modes[modeIndex].draw();
        }
        else {
            background(0); // black
            fill(0, 10, 100); // 10% red
            textAlign(CENTER, CENTER);
            textSize(48);
            text("Invalid mode!", width/2, height/2);
        }
    }

    public void keyPressed() {
        if (keyCode == CONTROL) ctrlHold = true;
        if (keyCode == SHIFT) shiftHold = true;

        // most things should only work on a valid mode
        if (0 <= modeIndex && modeIndex < modes.length) {
            // space will switch modes, so clean up the old one
            if (key == ' ') {
                modes[modeIndex].cleanup();
            }
            // send the key press to the mode
            else {
                modes[modeIndex].keyPressed();
            }
        }
        // space cycles to new mode and starts it up
        if (key == ' ') {
            modeIndex = (modeIndex + 1) % modes.length;
            modes[modeIndex].init();
        }
    }

    // most controls just get passed to the correct mode
    public void keyReleased() {
        if (keyCode == CONTROL) ctrlHold = false;
        if (keyCode == SHIFT) shiftHold = false;

        if (0 <= modeIndex && modeIndex < modes.length) {
            modes[modeIndex].keyReleased();
        }
    }

    public void mousePressed() {
        if (0 <= modeIndex && modeIndex < modes.length) {
            modes[modeIndex].mousePressed();
        }
    }

    public void mouseDragged() {
        if (0 <= modeIndex && modeIndex < modes.length) {
            modes[modeIndex].mouseDragged();
        }
    }

    public void mouseReleased() {
        if (0 <= modeIndex && modeIndex < modes.length) {
            modes[modeIndex].mouseReleased();
        }
    }

// Indentation note: We are breaking the usual indentation format here
//   for the inner interface and inner classes (the Mode definitions).
//   This way code in the methods in these classes have more room to work with.
    
// interface for different modes with different behaviors
interface Mode {
    void draw();

    // default: allows interfaces to provide simple implementations,
    //   allowing subclasses to treat those methods as optional
    
    // all these methods do nothing in a given mode unless they're
    //   overridden to do something
    default void init() {}
    default void cleanup() {}

    default void keyPressed() {}
    default void keyReleased() {}

    default void mousePressed() {}
    default void mouseDragged() {}
    default void mouseReleased() {}
}

// ==================== //
// ==== BUILD MODE ==== //
// ==================== //

// this mode allows display, editing, loading, saving, etc,
//   of WallSets
class BuildMode implements Mode {
    Point lastPoint = null;       // last point created/dragged
    Point lastPointOrigin = null; // point where dragging began

    String typed = null;     // used for typed input
    boolean saving = false;  // sub-mode for typing save filename

    public void draw() {
        background(120, 50, 100);  // green

        // NOTE: this class is BuildMode, so "this" would refer to the
        //     instance of BuildMode
        //   "Pathfinder.this" is a "qualified this" allowing access to the
        //     instance of the Pathfinder / PApplet object representing
        //     representing the program
        //   (as an inner class, members of BuildMode are part of a
        //     BuildMode instance AND part of a Pathfinder instance)
        wsCurr.display(Pathfinder.this);

        // draw last point and line connecting it to mouse
        if (lastPoint != null) {
            drawCrossingLine(lastPoint, mouse);
            lastPoint.display(Pathfinder.this);
        }

        showInstructions();

        if (saving) showSavePrompt();
    }

    void showSavePrompt() {
        fill(100, 0, 0);
        textAlign(CENTER, BOTTOM);
        textSize(24);
        float tSize = textAscent() + textDescent();

        text("Enter file name:", width/2, height - tSize - 5);
        text(typed, width/2, height - 5);
    }

    void showInstructions() {
        fill(0, 40);  // black, 40% opacity
        textAlign(LEFT, BOTTOM);
        textSize(12);
        float tSize = textAscent() + textDescent();
        float y = 0;

        String label = "BUILD MODE - #" + wsIndex + " - " + wsCurr.getName();
        text(label,                                      5, y += tSize);
        text("space: switch mode (works in both modes)", 5, y += tSize);

        y += tSize;
        int maxWall = Math.min(9, wsList.size()-1);
        text("any #: load wallset (0-" + maxWall + ")", 5, y += tSize);

        // unicode 2191 & 2193: up & down arrows
        text("\u2191\u2193: browse wall sets",    5, y += tSize);
        text("n: new wall set",                   5, y+= tSize);
        text("s: save current walls",             5, y += tSize);
        text("r: revert to saved walls",          5, y += tSize);

        y += tSize;
        text("click: create endpoint",            5, y += tSize);
        text("right-click endpoint: remove wall", 5, y += tSize);
        text("click and drag: move endpoint",     5, y += tSize);
        text("w: Add random wall.",               5, y += tSize);

        y += tSize;
        text("ctrl-z: " + wsCurr.undoPeek(), 5, y += tSize);
        text("shift-ctrl-z: " + wsCurr.redoPeek(), 5, y += tSize);
    }

    // draws line from a->b, with intersections with walls
    void drawCrossingLine(Point a, Point b) {
        List<Point> intersects = wsCurr.intersections(a, b);

        strokeWeight(3);
        if (intersects.isEmpty()) {
            stroke(120, 100, 50); // green - no crossings
            line(a.x, a.y, b.x, b.y);
        }
        else {
            stroke(0, 100, 100); // red - crosses other lines
            line(a.x, a.y, b.x, b.y);

            // display each intersection point
            for (Point intersect : intersects) {
                intersect.display(Pathfinder.this);
            }
        }
    }

    public void keyPressed() {
        // typing a file name as part of save operation
        if (saving) {
            // typable ASCII values get typed
            if (key >= 32 && key < 127) {
                typed += key;
            }
            // backspace backspaces
            if (key == BACKSPACE && typed.length() > 0) {
                typed = typed.substring(0, typed.length()-1);
            }
            // enter completes save operation
            if (key == ENTER) {
                finishSave();
            }
        }
        // normal controls when not in mid-save
        else {
            if (key == 'w') {
                wsCurr.add(new Wall(Pathfinder.this));
                wsCurr.nextPtId = Point.nextId;
            }
            if (key == 'r') wsCurr.revert();
            if ('0' <= key && key <= '9') loadWalls(key - '0');
            if (key == 's') {
                // begin typing name to save to, if none exists
                if (wsCurr.name == null) {
                    saving = true;
                    typed = "";
                }
                // or just save
                else {
                    wsCurr.save();
                }
            }
            // ctrl-Z with or without shift for undo/redo
            // checks keyCode not key because weird stuff happens when
            //   holding control
            if ((keyCode == 'z' || keyCode == 'Z') && ctrlHold) {
                if (shiftHold) wsCurr.redo();
                else           wsCurr.undo();
            }
            // secret support for ctrl-Y
            if (keyCode == 'y' && ctrlHold) wsCurr.redo();
            if (keyCode == UP) {
                loadWalls( (wsIndex + 1) % wsList.size() );
                Point.resetIds(wsCurr.nextPtId);
            }
            if (keyCode == DOWN) {
                loadWalls( (wsIndex + wsList.size() - 1) % wsList.size());
                Point.resetIds(wsCurr.nextPtId);
            }
            if (key == 'n') {
                lastPoint = null;  // deselect point

                wsCurr = new WallSet(); // brand new WallSet at end of list
                wsIndex = wsList.size();
                wsList.add(wsCurr);
                Point.resetIds(0);
            }
        }
    }

    // attempts to name and save file based on typed name
    void finishSave() {
        // entering nothing cancels save
        if (typed.isEmpty()) return;

        // add file extension if none was provided
        if (!typed.contains(".")) typed += ".walls";

        // lock in name
        wsCurr.name = typed;

        // try to save, but don't keep name if it fails
        //   (it's probably an invalid name if that happens)
        if (!wsCurr.save()) wsCurr.name = null;

        // reset typing vars and exit saving operation
        typed = null;
        saving = false;
    }

    // load a set of walls by number
    void loadWalls(int index) {
        if (index >= wsList.size()) {
            System.err.println("Only " + wsList.size() + " wall sets.");
        }
        else {
            lastPoint = null;  // deselect points when loading

            wsIndex = index;
            wsCurr = wsList.get(index);
            Point.resetIds(wsCurr.nextPtId);
        }
    }

    public void cleanup() {
        lastPoint = null;  // deselect
        
    }

    public void mousePressed() {
        Point clicked = findPoint(mouseX, mouseY);
        if (mouseButton == LEFT) {
            // create new Point if one was not already there
            if (clicked == null) {
                clicked = new Point(mouseX, mouseY);
                wsCurr.nextPtId = Point.nextId;

                // create new wall if there was another point before
                if (lastPoint != null) {
                    wsCurr.add(new Wall(clicked, lastPoint));
                }
                // ... or just start forming a wall at that point
                else {
                    lastPoint = clicked;
                }
            }
            // start moving point if there was already a point there
            else {
                lastPointOrigin = new Point(clicked);
                wsCurr.nextPtId = Point.nextId;
            }

            // whether point is new or pre-existing, select it for
            //   potential dragging/moving
            lastPoint = clicked;
        }
        else if (mouseButton == RIGHT) {
            // remove Point or Wall
            if (clicked != null) {
                if (clicked.wall == null) {
                    if (clicked == lastPoint) lastPoint = null;
                }
                else {
                    wsCurr.rem(clicked.wall);
                    // reset lastPoint if part of this wall
                    if (lastPoint != null && lastPoint.wall == clicked.wall) {
                        lastPoint = null;
                    }
                }
            }
            // clicked on nothing; deselect lastPoint
            else if (lastPoint != null) lastPoint = null;
        }
    }

    public void mouseDragged() {
        // drag point if one is selected
        if (lastPoint != null) {
            lastPoint.x = mouseX;
            lastPoint.y = mouseY;
        }
    }

    public void mouseReleased() {
        // "let go" of any point that's already part of a wall, after
        //   dragging or placing it
        if (lastPoint != null && lastPoint.wall != null) {
            // also lock in movement if it was moved
            if (lastPointOrigin != null &&
                    !lastPoint.equals(lastPointOrigin)) {
                wsCurr.finishMove(lastPoint, lastPointOrigin);
                lastPointOrigin = null;
            }
            lastPoint = null;
        }
    }

    // finds Point with 5 px of a given point
    Point findPoint(float x, float y) {
        if (lastPoint != null &&
                dist(lastPoint.x, lastPoint.y, x, y) <= 5) {
            return lastPoint;
        }

        for (Point p : wsCurr.points) {
            if (dist(p.x, p.y, x, y) <= 5) return p;
        }

        return null;
    }
} // END OF BUILD MODE

// ===================
// ==== PLAY MODE ====
// ===================
class PlayMode implements Mode {
    boolean playPaused = false;
    boolean labelPts = false;
    boolean drawGraph = false;
    boolean drawTarget = false;

    Player player;
    Mover[] ghosts;
    
    int start = millis();

    // TODO: graph settings?

    // controls display and movement of game
    public void draw() {
        // movers must get frame update before any movement is possible
        player.resetFrame();
        for (Mover m : ghosts) {
            m.resetFrame();
        }
        
        // TODO: update movement graph based on Mover positions
        for(Point current : wsCurr.points) {
        	for(Mover ghost : ghosts) {
        		if(wsCurr.isClearPath(ghost, current) == true) {
            		current.neighbors.add(ghost);
            		ghost.neighbors.add(current);
        			
            	}
        		if(wsCurr.isClearPath(ghost, current) == false) {
            		current.neighbors.remove(ghost);
            		ghost.neighbors.remove(current);
        			
            	}
        		
        	}
        	if(wsCurr.isClearPath(player, current) == true) {
        		current.neighbors.add(player);
        		player.neighbors.add(current);
        	}
        	if(wsCurr.isClearPath(player, current) == false) {
        		current.neighbors.remove(player);
        		player.neighbors.remove(current);
        	}
        	
        	
        	//graph.add(current);
        }
        for(Mover ghost : ghosts) {
    		if(wsCurr.isClearPath(ghost, player) == true) {
        		ghost.neighbors.add(player);
        		player.neighbors.add(ghost);
    			
        	}
    		if(wsCurr.isClearPath(ghost, player) == false) {
        		ghost.neighbors.remove(player);
        		player.neighbors.remove(ghost);
        	}
    		//should go be neighbors to ghosts?????
//    		for(Mover ghostNeighbor: ghosts) {
//    			if(ghost.equals(ghostNeighbor)==false) {
//    				if(wsCurr.isClearPath(ghost, ghostNeighbor) == true) {
//    					ghost.neighbors.add(ghostNeighbor);
//    				}
//    				if(wsCurr.isClearPath(ghost, ghostNeighbor) == false) {
//    					ghost.neighbors.remove(ghostNeighbor);
//    				}
//    			}
//    		}
    		
    		
    	}

        background(0, 0, 100);  // white

        if (!playPaused) player.move();

        // half the time display player first; half the time last
        if (frameCount % 2 != 0) player.display(Pathfinder.this);

        // handle all ghosts
        for (Mover m : ghosts) {
            if (!playPaused) m.move();
            m.display(Pathfinder.this);

            // TODO: display movement paths?
        }

        // half the time display player last; half the time first
        if (frameCount % 2 == 0) player.display(Pathfinder.this);

        wsCurr.display(Pathfinder.this);
        if (labelPts) wsCurr.displayPtLabels(Pathfinder.this);

        int elapsed = millis() - start;
        if (elapsed < 10000) {
            // in 1st 10 seconds of play mode, display instructions
            //   fading from 40 to 0 opacity in seconds 5-10,
            //   also keeping it no more than 40 in seconds 0-5
            fill(0, min(40, map(elapsed, 5000, 10000, 40, 0)));
            showInstructions();
        }
        //System.out.println(elapsed);
        fill(0,0,0);
        textAlign(CENTER, BOTTOM);
        textSize(42);
        int time = elapsed;
//        for(Mover ghost : ghosts) {
//        	if(ghost.distTo(player)==0) {
//        		time = -1;
//        	}else {
//        		text(time/1000 + " seconds",          width/2, 60);
//        	}
//        }
        text(time, width/2, 60);
        
        
       
        // TODO: display graph (perhaps based on settings)
        if(drawGraph) {
	        for(Point current : wsCurr.points) {
	        	
	        	for(Point neighbor : current.neighbors) {
	        		stroke(100,100,50);
	            	strokeWeight(3);
	            	strokeWeight(1);
	        		line(current.x, current.y, neighbor.x, neighbor.y);
	        		
	        	}
	        	
	        	
	        }
//	        for(Point neighbor : player.neighbors) {
//	        	stroke(255,0,0);
//            	strokeWeight(3);
//            	strokeWeight(1);
//        		line(player.x, player.y, neighbor.x, neighbor.y);
//	        }
	        for(Mover ghost : ghosts) {
	        	for(Point neighbor : ghost.neighbors) {
	        		stroke(100,100,50);
	            	strokeWeight(3);
	            	strokeWeight(1);
	        		line(ghost.x, ghost.y, neighbor.x, neighbor.y);
	        	}
	        	
	        	
	        	
	        	
	        }
	        
	        
        }
        if(drawTarget) {
        	
        	for(Mover ghost: ghosts) {
        		if(ghost.rule.getTarget() != null) {
	        		stroke(0, 100, 50);
	        		strokeWeight(3);
	            	strokeWeight(1);
	        		line(ghost.x, ghost.y, ghost.rule.getTarget().x, ghost.rule.getTarget().y);
	        	}
        	}
        }
    }

    void showInstructions() {
        textAlign(LEFT, BOTTOM);
        textSize(12);
        float tSize = textAscent() + textDescent();
        float y = 0;

        text("PLAY MODE",          5, y += tSize);
        text("space: switch mode", 5, y += tSize);
        text("p: pause/unpause",   5, y += tSize);
        text("r: reset game",      5, y += tSize);
        text("l: label points",    5, y += tSize);
        text("d: draw the Graph",  5, y += tSize);
        text("t: draw the Target of the points",  5, y += tSize);
    }

    public void keyPressed() {
        if (key == 'p') playPaused = !playPaused;
        if (key == 'r') resetPlayers();
        if (key == 'l') labelPts = !labelPts;
        if (key == 'd') drawGraph = !drawGraph;
        if (key == 't') drawTarget = !drawTarget;

        // TODO: control graph settings?
    }
    
    public void cleanup() {
    	//graph.clear();
    	for(Point current : wsCurr.points) {
    		current.neighbors.clear();
    	}
    	for(Mover ghost: ghosts) {
    		ghost.neighbors.clear();
    	}
    	player.neighbors.clear();
    }

    // entering and exiting play mode
    public void init() {
        

        // TODO: create graph among Points
        
     
        
//        System.out.println(wsCurr.walls);
//        System.out.println(wsCurr.points);
        
        //for every point in set and PLAYER, we find neighbors that dont intersect and make them neighbors.
        //bfs find shortest path to player
        for(Point current : wsCurr.points) {
        	
        	for(Point neighbor : wsCurr.points) {
        		if(current.equals(neighbor) == false) {
        			if(wsCurr.isClearPath(current, neighbor) == true) {
        				current.neighbors.add(neighbor);
        			}
        		}
        	}
        	//graph.add(current);
        	//System.out.println("Current neig"+current+ ":" + current.neighbors);
        	
        }
        
        
        //iterate all neighbors from player and make neighbors neighbors to the player (grammer is correct)
        
        
        resetPlayers();
        
    }

    // reset player/enemy positions
    void resetPlayers() {
        player = new Player(Pathfinder.this);
        System.out.println(player);
        ghosts = new Mover[]{
            new Mover(Pathfinder.this, new StandStill()),
            new Mover(Pathfinder.this, new MoveTo(player)),
            new Mover(Pathfinder.this, new MoveTo(mouse)),
            
            new Mover(Pathfinder.this, new RandomEdge(wsCurr, player)),
            new Mover(Pathfinder.this, new Dfs(wsCurr, player)),
            new Mover(Pathfinder.this, new Bfs(wsCurr, player)),
        	new Mover(Pathfinder.this, new Dijkstra(wsCurr, player))
            
        };
        //ghosts = new Mover[] {new Mover(Pathfinder.this, new RandomEdge(wsCurr, player))};
        //ghosts = new Mover[] {new Mover(Pathfinder.this, new Dijkstra(wsCurr, player)),new Mover(Pathfinder.this, new Bfs(wsCurr, player))};
        //ghosts = new Mover[] {new Mover(Pathfinder.this, new Bfs(wsCurr, player))};
        start = millis();
    }
} // END OF PLAY MODE

}


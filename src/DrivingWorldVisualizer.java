import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer.CellPainter;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer.MapPainter;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.visualizer.ObjectPainter;
import burlap.oomdp.visualizer.StaticPainter;
import burlap.oomdp.visualizer.Visualizer;


public class DrivingWorldVisualizer extends GridWorldVisualizer {
	public static final int grass = 0;
	public static final int lane = -1;
	
	
	
	public DrivingWorldVisualizer() {
		// TODO Auto-generated constructor stub
	}

public static Visualizer getVisualizer(Domain d, int [][] map, int leftGrassRight, int rightGrassLeft, int numLanes, int laneWidth){
		
		Visualizer v = new Visualizer();
		
		v.addStaticPainter(new MapPainter(d, map));
		v.addObjectClassPainter(DrivingGridWorld.blockClass, new GridWorldVisualizer.CellPainter(Color.blue, map));
		v.addObjectClassPainter(GridWorldDomain.CLASSAGENT, new GridWorldVisualizer.CellPainter(Color.red, map));
		
		return v;
	}
	
	public static class MapPainter implements StaticPainter{

		protected int 				dwidth;
		protected int 				dheight;
		protected int [][] 			map;
		int							numLanes;
		int							laneWidth;
		int							leftGrassRight;
		int							rightGrassLeft;
		
		
		public MapPainter(Domain domain, int [][] map, int leftGrassRight, int rightGrassLeft, int numLanes, int laneWidth) {
			this.dwidth = map.length;
			this.dheight = map[0].length;
			this.map = map;
		}

		@Override
		public void paint(Graphics2D g2, State s, float cWidth, float cHeight) {
			
			//draw the walls; make them black
			g2.setColor(Color.black);
			
			float domainXScale = this.dwidth;
			float domainYScale = this.dheight;
			
			//determine then normalized width
			float width = (1.0f / domainXScale) * cWidth;
			float height = (1.0f / domainYScale) * cHeight;
			
			for (int i = 0; i < numLanes; ++i) {
				float rx = i*laneWidth;
				float ryTop = cHeight - height - this.dheight*height;
				float ryBottom = cHeight - height;
			
			}
			
			//pass through each cell of the map and if it is a wall, draw it
			for(int i = 0; i < this.dwidth; i++){
				for(int j = 0; j < this.dheight; j++){
					
					if(this.map[i][j] == DrivingWorldVisualizer.lane){
						g2.setColor(Color.gray);

						float rx = i*width;
						float ry = cHeight - height - j*height;
					
						g2.fill(new Rectangle2D.Float(rx, ry, width, height));	
					}
					else if (this.map[i][j] == DrivingWorldVisualizer.grass) {
						g2.setColor(Color.green);

						float rx = i*width;
						float ry = cHeight - height - j*height;
					
						g2.fill(new Rectangle2D.Float(rx, ry, width, height));	
					}
				}
			}	
		}	
	}	
}

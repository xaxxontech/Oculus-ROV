package oculus;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.servlet.*;
import javax.servlet.http.*;

import org.red5.io.amf3.ByteArray;

public class FrameGrabHTTP extends HttpServlet {
	
	private static Application app = null;
	public static byte[] img  = null;
	private State state = State.getReference();
	
	public static void setApp(Application a) {
		if(app != null) return;
		app = a;
	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		doPost(req,res);
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		//frameGrab(req,res);
		radarGrab(req,res);
	}
	
	private void frameGrab(HttpServletRequest req, HttpServletResponse res) 
		throws ServletException, IOException {
		
		res.setContentType("image/jpeg");
		OutputStream out = res.getOutputStream();

			System.out.println("OCULUS: frame grabbing servlet start");
			img = null;
			if (app.frameGrab()) {
				
				int n = 0;
				while (state.getBoolean(State.framegrabbusy)) {
//				while (img == null) {
					try {
						Thread.sleep(5);
					} catch (InterruptedException e) {
						e.printStackTrace();
					} 
					n++;
					if (n> 2000) {  // give up after 10 seconds 
						state.set(State.framegrabbusy, false);
						break;
					}
				}
//				System.out.println("OCULUS: frame byte size: "+img.length());
				System.out.println("OCULUS: frame grabbing done in "+n*5+" ms");
				
				if (img != null) {
					for (int i=0; i<img.length; i++) {
						out.write(img[i]);
					}
				}
			    out.close();
			}
	}
	
	private void radarGrab(HttpServletRequest req, HttpServletResponse res) 
		throws ServletException, IOException {

		int w = 240;
		int h = 320;
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		
		if (app.openNIRead.depthCamReading == true) { 		
			//Util.log("getting frame depth info", this);
			int[] xdepth = app.openNIRead.readHorizDepth(120);
			//Util.log("depth map width = "+Integer.toString(horizDepthDistances.length));
			int maxDepthInMM = 3500;
			
			int voff = 0;
			double angle = 0.392699082; // 22.5 deg in radians from ctr, or half included view angle
			WritableRaster raster = image.getRaster();
			Graphics2D g2d = image.createGraphics();
			
			//render background
			g2d.setColor(new Color(10,10,10)); // was 10 10 10 
			g2d.fill(new Rectangle2D.Double(0, 0, w, h));
	
			// render pixel data and shadows
			int[] dataRGB = {0,255,0}; // sensor data pixel colour
			g2d.setColor(new Color(0,70,0)); // shadow colour
			int xdctr = xdepth.length/2;
			for (int xd=0; xd < xdepth.length; xd++) {
				int y = (int) ((float)xdepth[xd]/(float)maxDepthInMM*(float)h);
				// x(opposite) = tan(angle)*y(adjacent)
				double xdratio = (double)(xd - xdctr)/ (double) xdctr;
	//			Util.log(Double.toString(xdratio),this);
				int x = (w/2) - ((int) (Math.tan(angle)*(double) y * xdratio));
				int xend = (w/2) - ((int) (Math.tan(angle)*(double) (h-1) * xdratio)); // for shadow fill past point
				if (y<h-voff && y>0+voff && x>=0 && x<w) {
					y = h-y-1+voff; //flip vertically
					g2d.drawLine(x, y, xend, 0);  //fill area behind with line
					raster.setPixel(x,y,dataRGB);
					raster.setPixel(x,y+1,dataRGB);
				}
			}
			
			// dist scale arcs
			g2d.setColor(new Color(100,100,100));
			int r = 100;
			g2d.draw(new Ellipse2D.Double( w/2-r, h-1-r*0.95+voff, r*2, r*2*0.95));
			r = 200;
			g2d.draw(new Ellipse2D.Double( w/2-r, h-1-r*0.95+voff, r*2, r*2*0.95));
			r = 300;
			g2d.draw(new Ellipse2D.Double( w/2-r, h-1-r*0.95+voff, r*2, r*2*0.95));
			
			// outside cone colour fill
			g2d.setColor(new Color(23,25,0)); 
			for (int y= 0-voff; y<h+voff; y++) {
				int x = (int) (Math.tan(angle)*(double)(h-y-1));
				if (x>=0) {
					g2d.drawLine(0, y, (w/2)-x, y);  
					g2d.drawLine(w-1, y, (w/2)+x,y);
				}
	  
			}
			
			// cone perim lines
			g2d.setColor(new Color(100,100,100));
			int x = (int) (Math.tan(angle)*(double)(319));
			g2d.drawLine(w/2, 319, (w/2)-x, 0);
			g2d.drawLine(w/2, 319, (w/2)+x, 0);
			
			// send image
			res.setContentType("image/gif");
			OutputStream out = res.getOutputStream();
			ImageIO.write(image, "GIF", out);
		}
	}
	
}

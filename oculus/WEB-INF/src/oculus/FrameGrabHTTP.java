package oculus;

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
		//Util.log("getting frame depth info", this);
		int[] xdepth = app.openNIRead.readHorizDepth();
		//Util.log("depth map width = "+Integer.toString(horizDepthDistances.length));
		int maxDepthInMM = 3500;
		
		int w = 240;
		int h = 320;
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		WritableRaster raster = image.getRaster();
		int[] rgb = {0,255,0};

		int x;
		int y;
		int xdctr = xdepth.length/2;
		double xdratio;
		double angle = 0.392699082; // 22.5 deg in radians from ctr, or half included view angle
		for (int xd=0; xd < xdepth.length; xd++) {
			y = (int) ((float)xdepth[xd]/(float)maxDepthInMM*(float)h);
			// x(opposite) = tan(angle)*y(adjacent)
			xdratio = (double)(xd - xdctr)/ (double) xdctr;
//			Util.log(Double.toString(xdratio),this);
			x = (w/2) - ((int) (Math.tan(angle)*(double) y * xdratio));
			if (y<h && y>=0 && x>=0 && x<w) {
				y = h-y-1; //flip vertically
				raster.setPixel(x,y,rgb);
			}
		}

		res.setContentType("image/gif");
		OutputStream out = res.getOutputStream();
		ImageIO.write(image, "GIF", out);
	}
	
}

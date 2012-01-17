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
		int[] horizDepthDistances = app.openNIRead.readHorizDepth();
		//Util.log("depth map width = "+Integer.toString(horizDepthDistances.length));
		int maxDepthInMM = 3500;
		
		int w = horizDepthDistances.length;
		int h = horizDepthDistances.length*3/2;
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		WritableRaster raster = image.getRaster();
/*		
		int hdx = 0;
		int[] rgb = {0,0,0};
		for (int x =0; x<w; x++) {
			for (int y=0; y<h; y++) {
				if (horizDepthDistances[hdx]/maxDepthInMM*h == y) { // map depth to y
					rgb[1]=255;
				}
				else { rgb[1]= 0; }
				raster.setPixel(x,y,rgb);
			}
			Util.log(Integer.toString(hdx)+" "+ Integer.toString(horizDepthDistances[hdx]));
			hdx ++;
		}
*/

		int hd = 0;
		int y;
		int[] rgb = {0,255,0};
		for (int x=0; x<horizDepthDistances.length; x++) {
			y = horizDepthDistances[hd]/maxDepthInMM*h;
			raster.setPixel(x,y,rgb);
			hd++;
		}

		res.setContentType("image/gif");
		OutputStream out = res.getOutputStream();
		ImageIO.write(image, "GIF", out);
	}
	
}

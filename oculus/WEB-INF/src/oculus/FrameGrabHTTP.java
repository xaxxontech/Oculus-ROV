package oculus;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.*;
import javax.servlet.http.*;

import org.red5.io.amf3.ByteArray;

public class FrameGrabHTTP extends HttpServlet {
	
	private static Application app = null;
	public static ByteArray img  = null;
	
	public static void setApp(Application a) {
		System.out.println("OCULUS: calling setApp FrameGrabHTTP ");
		if(app != null) return;
		app = a;
	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		doPost(req,res);
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		
		res.setContentType("image/jpeg");
		OutputStream out = res.getOutputStream();

			System.out.println("OCULUS: frame grabbing servlet start");
			img = null;
			if (app.frameGrab()) {
				
				int n = 0;
				while (img == null) {
					try {
						Thread.sleep(5);
					} catch (InterruptedException e) {
						e.printStackTrace();
					} 
					n++;
				}
				System.out.println("OCULUS: frame grabbing done in "+n*5+" ms");
				
				for (int i=0; i<img.length(); i++) {
					out.write(img.readByte());
				}
			    out.close();
			}

	}
	
}

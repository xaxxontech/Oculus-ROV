package oculus;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.*;
import javax.servlet.http.*;

import org.red5.io.amf3.ByteArray;

public class FrameGrabHTTP extends HttpServlet {
	
	private static Application app = null;
	public static ByteArray img  = null;
//	private OutputStream out = null;
//	private HttpServletResponse response = null;
	
//	public FrameGrabHTTP(Application a) {
//		System.out.println("OCULUS: calling constructor FrameGrabHTTP ");
//		if(app != null) return;
//		app = a;
//	}
	
	public static void setApp(Application a) {
		System.out.println("OCULUS: calling setApp FrameGrabHTTP ");
		if(app != null) return;
		app = a;
	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		doPost(req,res);
		
//		PrintWriter out = response.getWriter();
//		String fulladdress = request.getRequestURL().toString();
//		String address = request.getRemoteHost()+":"+request.getServerPort();
//		System.out.println("OCULUS: getRemoteHost = "+address);
//		out.print("<img src='http://"+address+"/oculus/images/framegrab.jpg' alt=''>");
//		out.close();
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
//		response = res;
//		response.setContentType("image/jpeg");
//		out = response.getOutputStream();
		
		res.setContentType("image/jpeg");
		OutputStream out = res.getOutputStream();

//		new Thread(new Runnable() {
//			public void run() {
				try {
					System.out.println("OCULUS: frame grabbing servlet start");
					img = null;
					app.frameGrab();
					
					int n = 0;
					while (img == null) {
						Thread.sleep(5); 
						n++;
					}
					System.out.println("OCULUS: frame grabbing done in "+n*5+" ms");
					
					for (int i=0; i<img.length(); i++) {
						out.write(img.readByte());
					}
				    out.close();
				}
				catch (Exception e) { e.printStackTrace(); }
//			}
//		}).start();
	}
	
}

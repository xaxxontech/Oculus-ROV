package oculus;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.*;
import javax.servlet.http.*;

public class RtmpPortRequest extends HttpServlet {
	private Settings settings = new Settings();
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.print(settings.readRed5Setting("rtmp.port"));
		out.close();
//		System.out.println("xmlhttphandler");
	}
}

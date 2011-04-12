package oculus;

import java.io.*;
import java.net.*;

import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

public class Downloader {
	
	private static Logger log = Red5LoggerFactory.getLogger(Downloader.class, "oculus");

	/**
	 * 
	 * Down load a given URL to the local disk. Will delete existing file first, and create directory if required. 
	 *
	 * @param fileAddress is the full http://url of the remote file
	 * @param localFileName the file name to use on the host
	 * @param destinationDir the folder name to put this down load into 
	 * @return true if the file is down loaded, false on any error. 
	 * 
	 */
	public static boolean FileDownload(final String fileAddress,
			final String localFileName, final String destinationDir) {

		InputStream is = null;
		OutputStream os = null;
		URLConnection URLConn = null;

		// create path to local file
		final String path = destinationDir + System.getProperty("file.separator") + localFileName;

		// create target directory
		new File(destinationDir).mkdirs();

		// delete target first
		new File(path).delete();

		// test is really gone
		if (new File(path).exists()) {
			log.error("can't delete existing file: " + path);
			return false;
		}

		try {

			int ByteRead, ByteWritten = 0;
			os = new BufferedOutputStream(new FileOutputStream(path));

			URLConn = new URL(fileAddress).openConnection();
			is = URLConn.getInputStream();
			byte[] buf = new byte[1024];

			// pull in the bytes
			while ((ByteRead = is.read(buf)) != -1) {
				os.write(buf, 0, ByteRead);
				ByteWritten += ByteRead;
			}

			// System.out.println("local file: " + path + " bytes: " + ByteWritten);

		} catch (Exception e) {
			log.error(e.getMessage());
			return false;
		} finally {
			try {
				is.close();
				os.close();
			} catch (IOException e) {
				log.error(e.getMessage());
				return false;
			}
		}

		// all good
		return true;
	}

	/** test driver */
	public static void main(String[] args) {

		boolean result = FileDownload(
				"http://oculus.googlecode.com/svn/trunk/sketchbook/oculusDC_id/oculusDC_id.pde",
				"oculusDC.pde", "foo/bar/test");

		if (!result)
			System.out.println("fail");

	}
}

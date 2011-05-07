package oculus;

import java.util.*;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;

import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

/**
 * Send yourself an email from your gmail account
 * 
 * @author brad.zdanivsk@gmail.com
 */
public class SendMail {

	private static Logger log = Red5LoggerFactory.getLogger(SendMail.class, "oculus");

	// TODO: take this from properties on startup if we want any smtp server
	private static int SMTP_HOST_PORT = 587;
	private static final String SMTP_HOST_NAME = "smtp.gmail.com";

	private Settings settings = new Settings();
	private final String user = settings.readSetting("email");
	private final String pass = settings.readSetting("email_password");
	private final boolean debug = settings.getBoolean("developer");

	private String subject = null;
	private String body = null;
	private String fileName = null;
	
	// if set, send error messages to user screen 
	private Application application = null;

	/** */
	SendMail(final String sub, final String text, final String file) {

		subject = sub;
		body = text;
		fileName = file;

		new Thread(new Runnable() {
			public void run() {
				sendAttachment();
			}
		}).start();
	}

	/**	*/
	SendMail(final String sub, final String text) {

		subject = sub;
		body = text;

		new Thread(new Runnable() {
			public void run() {
				sendMessage();
			}
		}).start();
	}


	/** send messages to user */
	SendMail(final String sub, final String text, final String file, Application app) {
		
		subject = sub;
		body = text;
		fileName = file;
		application = app;
		
		new Thread(new Runnable() {
			public void run() {
				sendAttachment();
			}
		}).start();
	}

	/** send messages to user */
	SendMail(final String sub, final String text, Application app) {
		
		subject = sub;
		body = text;
		application = app;
		
		new Thread(new Runnable() {
			public void run() {
				sendMessage();
			}
		}).start();
	}
	

	/** blocking send */
	SendMail(final String sub, final String text, final String file, boolean block) {

		subject = sub;
		body = text;
		fileName = file;
			
		sendAttachment();	
	}

	/**	blocking send */
	SendMail(final String sub, final String text, boolean block) {

		subject = sub;
		body = text;

		sendMessage();
	}
	
	/** */
	private void sendMessage() {

		if (user == null || pass == null) {
			log.error("no email and password found in settings");
			if(debug) System.out.println("no email and password found in settings");
			return;
		}
		
		try {

			Properties props = new Properties();
			props.put("mail.smtps.host", SMTP_HOST_NAME);
			props.put("mail.smtps.auth", "true");
			props.put("mail.smtp.starttls.enable", "true");

			Session mailSession = Session.getDefaultInstance(props);
			Transport transport = mailSession.getTransport("smtp");

			if (debug) mailSession.setDebug(true);

			MimeMessage message = new MimeMessage(mailSession);
			message.setSubject(subject);
			message.setContent(body, "text/plain");
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(user));

			transport.connect(SMTP_HOST_NAME, SMTP_HOST_PORT, user, pass);
			transport.sendMessage(message, message.getRecipients(Message.RecipientType.TO));
			transport.close();

			if (debug) System.out.println("... email sent");
			if(application!=null) application.message("email has been sent", null, null);

		} catch (Exception e) {
			log.error(e.getMessage());
			if(debug) System.out.println("error sending email, check settings");
			if(application!=null) application.message("error sending email", null, null);
		}
	}

	
	
	/** */
	private void sendAttachment() {

		if (user == null || pass == null) {
			log.error("no email and password found in settings");
			if(debug) System.out.println("no email and password found in settings");
			return;
		}
		
		try {

			if (debug) System.out.println("sending email..");
			
			Properties props = new Properties();
			props.put("mail.smtps.host", SMTP_HOST_NAME);
			props.put("mail.smtps.auth", "true");
			props.put("mail.smtp.starttls.enable", "true");

			Session mailSession = Session.getDefaultInstance(props);
			Transport transport = mailSession.getTransport("smtp");

			if (debug) mailSession.setDebug(true);

			MimeMessage message = new MimeMessage(mailSession);
			message.setSubject(subject);
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(user));

			BodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setText(body);
			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(messageBodyPart);

			messageBodyPart = new MimeBodyPart();
			DataSource source = new FileDataSource(fileName);
			messageBodyPart.setDataHandler(new DataHandler(source));
			messageBodyPart.setFileName(fileName);
			multipart.addBodyPart(messageBodyPart);
			message.setContent(multipart);

			transport.connect(SMTP_HOST_NAME, SMTP_HOST_PORT, user, pass);
			transport.sendMessage(message, message.getRecipients(Message.RecipientType.TO));
			transport.close();

			if (debug) System.out.println("... email sent");
			if(application!=null) application.message("email has been sent", null, null);

		} catch (Exception e) {
			log.error(e.getMessage());
			if(debug) System.out.println("error sending email, check settings");
			if(application!=null) application.message("error sending email", null, null);
		}
	}
}
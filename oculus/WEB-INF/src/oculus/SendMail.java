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

	// take this from properties on startup if we want any smtp server
	private static int SMTP_HOST_PORT = 587;
	private static final String SMTP_HOST_NAME = "smtp.gmail.com";

	// send result back 
	private boolean result = true;
	private String user = null;
	private String pass = null;
	
	/**
	 * Check props for email auth info 
	 */
	SendMail(){

		Settings settings = new Settings();
		// TODO: take smtp info from props?
		// private static final String SMTP_HOST_NAME = "smtp.gmail.com";
		user = settings.readSetting("email");
		pass = settings.readSetting("email_password");
	}
	
	/**
	 * 
	 * Send yourself an error message from the robot. This method requires a
	 * Gmail user account.
	 * 
	 * @param text
	 *            is the message body to form the email body
	 * @return True if mail was sent successfully
	 */
	public boolean sendMessage(final String sub, final String text) {
		
		if( user == null || pass == null ) return false;
			
		new Thread(new Runnable() {
			public void run() {
				try {

					Properties props = new Properties();
					props.put("mail.smtps.host", SMTP_HOST_NAME);
					props.put("mail.smtps.auth", "true");
					props.put("mail.smtp.starttls.enable", "true");

					Session mailSession = Session.getDefaultInstance(props);
					Transport transport = mailSession.getTransport("smtp");

					/* turn off on deply */
					mailSession.setDebug(true);

					MimeMessage message = new MimeMessage(mailSession);
					message.setSubject(sub);
					message.setContent(text, "text/plain");
					message.addRecipient(Message.RecipientType.TO,
							new InternetAddress(user));

					transport.connect(SMTP_HOST_NAME, SMTP_HOST_PORT, user, pass);
					transport.sendMessage(message,
							message.getRecipients(Message.RecipientType.TO));
					transport.close();
				} catch (Exception e) {
					log.error(e.getMessage());
					result = false;
				}
			}
		}).start();

		// all good
		return result;
	}

	/**
	 * Send yourself an error message from the robot. This method requires a
	 * Gmail user account.
	 * 
	 * @param sub
	 *            is the subject text of the email
	 * @param text
	 *            is the body of the email
	 * @param path
	 *            is the path to the file to attach to the email
	 * 
	 */
	public boolean sendMessage(final String sub, final String text, final String path) {
		
		if( user == null || pass == null ) return false;
		
		new Thread(new Runnable() {
			public void run() {
				try {
				
					Properties props = new Properties();
					props.put("mail.smtps.host", SMTP_HOST_NAME);
					props.put("mail.smtps.auth", "true");
					props.put("mail.smtp.starttls.enable", "true");

					Session mailSession = Session.getDefaultInstance(props);
					Transport transport = mailSession.getTransport("smtp");

					// debug flag in props
					mailSession.setDebug(true);

					MimeMessage message = new MimeMessage(mailSession);
					message.setSubject(sub);
					message.addRecipient(Message.RecipientType.TO,
							new InternetAddress(user));

					BodyPart messageBodyPart = new MimeBodyPart();
					messageBodyPart.setText(text);
					Multipart multipart = new MimeMultipart();
					multipart.addBodyPart(messageBodyPart);

					messageBodyPart = new MimeBodyPart();
					DataSource source = new FileDataSource(path);
					messageBodyPart.setDataHandler(new DataHandler(source));
					messageBodyPart.setFileName(path);
					multipart.addBodyPart(messageBodyPart);
					message.setContent(multipart);

					transport.connect(SMTP_HOST_NAME, SMTP_HOST_PORT, user,pass);
					transport.sendMessage(message,
							message.getRecipients(Message.RecipientType.TO));
					transport.close();

				} catch (Exception e) {
					log.error(e.getMessage());
					result = false;
				}
			}
		}).start();
		return result;
	}
}
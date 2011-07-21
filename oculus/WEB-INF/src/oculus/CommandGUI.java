package oculus;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Vector;

/** */
public class CommandGUI {
	
	public static final String oculus = "oculus";
	public static final String function = "function";
	public static final String argument = "argument";

	/** framework configuration */
	// static ZephyrOpen constants = ZephyrOpen.getReference();
	static final String title = "command line utility";

	/** framework configuration */
	public static final int WIDTH = 300;
	public static final int HEIGHT = 80;

	/** keep all outgoing commands */  
	private Vector<String> history = new Vector<String>();
	private int ptr = 0;

	/** */
	private JFrame frame = new JFrame(title);
	private JTextField user = new JTextField();

	/** create menu */ 
	private JMenu deviceMenue = new JMenu("commands");
	private JMenuItem closeItem = new JMenuItem("close");
	private JMenuItem dockItem = new JMenuItem("autodock");
	private JMenuItem undockItem = new JMenuItem("un-dock");
	private JMenuItem scriptItem = new JMenuItem("run script file");
	
	/** re-use same command object */ 
	private Command command = new Command(oculus);
	
	/** lock out default settings */
	public static void main(String[] args) {
		// constants.init();
		// ApiFactory.getReference().remove(ZephyrOpen.zephyropen);
		// constants.put(ZephyrOpen.frameworkDebug, false);
		// constants.lock();
		new CommandGUI();
	}
	
	/** send to group */
	private void sendCommand(String input){
		String fn = null;
		String ar = null;
		int space = input.indexOf(' ');
		if(space==-1){
			fn = input;
		} else {
			fn = input.substring(0, space);
			ar = input.substring(input.indexOf(' ')+1, input.length());
		}
			
		// create command 
		command.flush();
		if(fn!=null) command.add(function, fn);
		if(ar!=null) command.add(argument, ar);
		command.send();
	}
	
	/** */
	private void replayHistory(String input){
	
		if(history.size() <= 0) return; 
	
		frame.setTitle("execute script");
		System.out.println("start script");
		
		int delay = 0;
		int n = 0;
		
		try {
			String[] args = input.split(" ");
			n = Integer.parseInt(args[1]);
			if(args.length >= 3)
				delay = Integer.parseInt(args[2]);
		} catch (NumberFormatException e1) {
			return;
		}
	
		if( n < 0 ) n = history.size();
		if( n > history.size()) n = history.size();
		for(int i = 0 ; i < n ;i++){
			
			// user.setText(i + " " + history.get((history.size()-1)-i));
			System.out.println(i + " " + ((history.size()-1)-i) + " " + history.get((history.size()-1)-i));
			sendCommand(history.get((history.size()-1)-i));
			Util.delay(delay);
			
		}			
		
		frame.setTitle("done script");
	}

	/** parse input from text area */
	public class UserInput implements KeyListener {
		@Override
		public void keyTyped(KeyEvent e) {
			final char c = e.getKeyChar();
			if (c == '\n' || c == '\r') {

				final String input = user.getText().trim();
				if (input.length() > 2) {

					// clear input screen 
					user.setText("");
					frame.setTitle(title + " (" + ptr + "/" + history.size() + ")");
					
					// script commands 
					if(input.startsWith("/r")) replayHistory(input);
					
					// only if not caught above 
					else {

						if ( ! history.contains(input)) {
							history.add(input);
							ptr = history.size() - 1;
						} else {
							ptr = history.indexOf(input);
						}

						sendCommand(input);
					}		
				}
			}
		}

		@Override
		public void keyPressed(KeyEvent e) {
			if(e.getKeyCode() == KeyEvent.VK_UP){
				if (history.isEmpty()) {
					user.setText("");
					return;
				}

				user.setText(history.get(ptr));
				//user.se
				//setCursor(history.get(ptr).length());
				//user.setCaretPosition(user.getDocument().getLength()); // user.getText().length()-2);
				//user.repaint();
				
				if (history.size() > 0) ptr--;
				if (ptr < 0) ptr = history.size() - 1;
				
				// limit history ??
				// if (history.size() > 10) history.remove(0);
				
				frame.setTitle(title + " (" + ptr + "/" + history.size() + ") " +  user.getCaretPosition());
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {}
	}

	/** */
	private ActionListener listener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent event) {
			Object source = event.getSource();
			if (source.equals(dockItem)) {
				new Thread() {
					public void run() {
						history.add("autodock go");
						command.add(function, "autodock");
						command.add(argument, "go");
						command.send();
					}
				}.start();
			}
			
			if (source.equals(undockItem)) {
				new Thread() {
					public void run() {
						history.add("dock undock");
						command.add(function, "dock");
						command.add(argument, "undock");
						command.send();
					}
				}.start();
			}
		}
	};

	/** */
	public CommandGUI() {

		/** Resister listener */
		scriptItem.addActionListener(listener);
		undockItem.addActionListener(listener);
		dockItem.addActionListener(listener);
		closeItem.addActionListener(listener);
		user.addKeyListener(new UserInput());

		/** Add to menu */
		deviceMenue.add(closeItem);
		deviceMenue.add(undockItem);
		deviceMenue.add(dockItem);
		deviceMenue.add(dockItem);
		deviceMenue.add(scriptItem);
		
		/** Create the menu bar */
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(deviceMenue);

		/** Create frame */
		frame.setJMenuBar(menuBar);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(user);
		frame.setSize(WIDTH, HEIGHT);
		frame.setResizable(false);
		frame.setAlwaysOnTop(true);
		frame.setVisible(true);

		/** register shutdown hook
		Runtime.getRuntime().addShutdownHook(
				new Thread() {
					public void run() {
						System.out.println("shutdown");
					}
				}
				);
				*/
	}
}
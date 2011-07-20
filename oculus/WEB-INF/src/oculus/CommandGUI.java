package oculus;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Vector;

/** */
public class CommandGUI {
	
	static String oculus = "oculus";
	private static String function = "function";
	private static String argument = "argument";

	/** framework configuration */
	static final String title = "command line utility v0.1";

	/** framework configuration */
	public static final int WIDTH = 300;
	public static final int HEIGHT = 80;

	private Vector<String> history = new Vector<String>();
	private int ptr = 0;

	JFrame frame = new JFrame(title);
	JTextField user = new JTextField();
	JMenuItem closeItem = new JMenuItem("close");
	JMenuItem dockItem = new JMenuItem("autodock");
	JMenuItem undockItem = new JMenuItem("un-dock");
	JMenuItem scriptItem = new JMenuItem("run script file");
	JMenu deviceMenue = new JMenu("Commands");

	/** */
	public static void main(String[] args) {
		//constants.init();
		//ApiFactory.getReference().remove(ZephyrOpen.zephyropen);
		//constants.put(ZephyrOpen.frameworkDebug, false);
		//constants.lock();
		new CommandGUI();
	}

	/** */
	public class UserInput implements KeyListener {
		@Override
		public void keyTyped(KeyEvent e) {
			char c = e.getKeyChar();
			String input = user.getText().trim();
			if (c == '\n' || c == '\r') {
				String s = input;
				if (s.length() > 1) {
					if (!history.contains(s)) {
						history.add(s);
						ptr = history.size() - 1;
					} else {
						ptr = history.indexOf(s);
					}	
				}

				frame.setTitle(title + " (" + ptr + " of " + history.size() + ")");

				// parse input string 
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
				Command command = new Command(oculus);
				if(fn!=null) command.add(function, fn);
				if(ar!=null) command.add(argument, ar);
				command.send();
				System.out.println("out: " + command);

				// clear it
				user.setText("");
			}
		}

		@Override
		public void keyPressed(KeyEvent e) {
			int keyCode = e.getKeyCode();
			switch (keyCode) {
			
			case KeyEvent.VK_UP:
			
				if (history.isEmpty()) {
					user.setText("");
					return;
				}

				user.setText(history.get(ptr));
				frame.setTitle(title + " (" + ptr + " of " + history.size() + ")");

				if (history.size() > 0)
					ptr--;
				if (ptr < 0)
					ptr = history.size() - 1;
				if (history.size() > 15)
					history.remove(0);

				break;
			
			case KeyEvent.VK_DOWN:
				break;	
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

						Command command = new Command(oculus);
						command.add(function, "autodock");
						command.add(argument, "go");
						command.send();
						System.out.println(command.toString());

					}
				}.start();
			}
		
			
			if (source.equals(undockItem)) {
				new Thread() {
					public void run() {
						
						Command command = new Command(oculus);
						command.add(function, "dock");
						command.add(argument, "undock");
						command.send();
						System.out.println(command.toString());

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
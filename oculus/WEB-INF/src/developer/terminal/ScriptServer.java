package developer.terminal;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;

import oculus.Util;

public class ScriptServer extends AbstractTerminal {
	
	public String scriptFile = null;

	public ScriptServer(String ip, String port, final String user, final String pass, String filename) 
		throws NumberFormatException, UnknownHostException, IOException {
		super(ip, port, user, pass);
		
		scriptFile = filename;
		execute();
	}

	/** loop through given file */
	@Override
	public void execute(){
		
		System.out.println("running file:" + scriptFile);
			
		try{
		
			FileInputStream filein = new FileInputStream(scriptFile);
			BufferedReader reader = new BufferedReader(new InputStreamReader(filein));
			String cmd = null;
			while(true){
				
				cmd = reader.readLine();
				if(cmd==null) break;
				
				else {
					
					cmd = cmd.trim();
					final String[] str = cmd.split(" ");
					if(cmd.startsWith("delay")){
						System.out.println("doing delay " + str[1]);
						Util.delay(Integer.parseInt(str[1]));
					} else { 
						// send to robot 
						System.out.println("sending to bot: " + cmd);
						out.println(cmd);
					}	
				}
			}
	
			filein.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// log out 
		out.println("bye");
	}
	
	/** parameters: ip, port, user name, password, script file */ 
	public static void main(String args[]) throws IOException {
		new ScriptServer(args[0], args[1], args[2], args[3], args[4]);
				
		///"/Users/brad/Documents/workspace/Oculus/test.txt"); 
	}
}
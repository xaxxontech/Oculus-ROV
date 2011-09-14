package oculus;

import java.util.Date;
import java.util.Vector;

public class LoginRecords {
	
	// private static final String filename = System.getenv("RED5_HOME") + "\\log\\login.txt";
	private Vector<LoginRecord> loginRecord = null;
	
	public LoginRecords(){
		 loginRecord = new Vector<LoginRecord>();
		 System.out.println("ready to login..");
	}
	
	public void login(String usr){
		System.out.println("log in: " + usr);
		loginRecord.add(new LoginRecord(usr));
		System.out.println("size now = " + loginRecord.size());
	}

	public void logout(String usr){
		System.out.println("log out: " + usr);
		//int i = getIndex(usr);
		//loginRecord.get(i).logout();
	}
	
	public LoginRecord get(int i){
		return loginRecord.get(i);
	}
	
	public int getIndex(String usr){
		for(int i = 0 ; i < loginRecord.size() ; i++)
			if(loginRecord.get(i).getUser().equals(usr))
				return i;
		
		return -1;
	}
	
	public int size(){
		return loginRecord.size();
	}
	
	public void save(){
		
	}

	@Override
	public String toString(){
		
		if(loginRecord.isEmpty()) return null;
		
		String str = "";
		for(int i = 0 ; i < loginRecord.size() ; i++)
			str+= loginRecord.get(i).getUser().toString() + "\r\n";
		
		return str;
	}
	
	/**
	 * 
	 */
	private class LoginRecord {

		private long timein = System.currentTimeMillis();
		private long timeout = 0;
		private String user = null;

		LoginRecord(String usr) {
			this.user = usr;
			System.out.println("ceated login: " + toString());
		}

		public String getUser() {
			return user;
		}

		public long inTime() {
			return timein;
		}

		public long outTime() {
			return timeout;
		}
		
		@Override
		public String toString() {
			return new Date(timein).toString() + " " + user;
		}

		public void logout() {
			timeout = System.currentTimeMillis();
			System.out.println("logged out : " + toString());
		}
	}

}

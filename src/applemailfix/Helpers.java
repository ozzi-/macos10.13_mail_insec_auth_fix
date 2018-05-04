package applemailfix;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ResourceBundle;

import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;

public class Helpers {

	static void osCheck(ResourceBundle messages) {
		if(!System.getProperty("os.version").startsWith("10.13") && !System.getProperty("os.version").startsWith("4.13.0-16-generic")) {
			JOptionPane.showMessageDialog(null,
					messages.getString("osnotification"),
					messages.getString("notificationl"), JOptionPane.INFORMATION_MESSAGE);
			System.exit(1);
		}
	}
	
	static void log(String msg, PrintWriter writer) {
		if(writer!=null) {
			writer.println(msg);
		}
		System.out.println(msg);
	}
	
	static void killMail() {
		Process p;
		try {
			p = Runtime.getRuntime().exec("pkill Mail");
			p.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			@SuppressWarnings("unused")
			String line = "";
			while ((line = reader.readLine()) != null) {
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	static void backupFiles(ResourceBundle messages, String accountsPath, String accounts4SQL, String accounts4SQLshm, String accounts4SQLwal) {
		String backupEnding = "."+(System.currentTimeMillis() / 1000L)+".backup";
		try {
			FileUtils.copyFile(new File(accountsPath + accounts4SQL),
				new File(accountsPath + accounts4SQL + backupEnding));
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null,
				messages.getString("errorfound")+"\n( " + accountsPath + accounts4SQL + " )",
				"Error", JOptionPane.ERROR_MESSAGE);
				System.exit(1);
		}
		try {
			FileUtils.copyFile(new File(accountsPath + accounts4SQLshm),
				new File(accountsPath + accounts4SQLshm + backupEnding));
		} catch (IOException e) {}
		try {
			FileUtils.copyFile(new File(accountsPath + accounts4SQLwal),
				new File(accountsPath + accounts4SQLwal + backupEnding));
		} catch (IOException e) {}
	}

	static void insertZACCOUNTPROPERTY(Connection conn, byte[] blob_bool, byte[] blob_smtp, byte[] blob_imap, byte[] blob_pop3, Obj obj, PrintWriter writer) throws SQLException {
		String insertPort = "INSERT INTO ZACCOUNTPROPERTY (Z_ENT,Z_OPT,ZOWNER,ZKEY,ZVALUE) VALUES "
				+ "('3','1','"+ obj.getZ_PK() + "','PortNumber',?)";
		String insertDynConf = "INSERT INTO ZACCOUNTPROPERTY (Z_ENT,Z_OPT,ZOWNER,ZKEY,ZVALUE) VALUES "
				+ "('3','1','"+ obj.getZ_PK() + "','DisableDynamicConfiguration',?)";
		String insertInsecAuth = "INSERT INTO ZACCOUNTPROPERTY (Z_ENT,Z_OPT,ZOWNER,ZKEY,ZVALUE) VALUES "
				+ "('3','1','"+ obj.getZ_PK() + "','AllowsInsecureAuthentication',?)";
		PreparedStatement stmntIP = conn.prepareStatement(insertPort);
		PreparedStatement stmntID = conn.prepareStatement(insertDynConf);
		PreparedStatement stmntII = conn.prepareStatement(insertInsecAuth);
		if (obj.getZ_ACCOUNT_TYPE().equals("IMAP")) {
			stmntIP.setBytes(1, blob_imap);
			Helpers.log("-> INCOMING IMAP",writer);
		} else if (obj.getZ_ACCOUNT_TYPE().equals("POP")) {
			stmntIP.setBytes(1, blob_pop3);
			Helpers.log("-> INCOMING POP",writer);
		} else if (obj.getZ_ACCOUNT_TYPE().equals("SMTP")) {
			stmntIP.setBytes(1, blob_smtp);
			Helpers.log("-> OUTGOING SMTP",writer);
		}else {
			Helpers.log("-> UNKNOWN ACCOUNT TYPE! ",writer);
		}
		stmntID.setBytes(1, blob_bool);
		stmntII.setBytes(1, blob_bool);
		int ip = stmntIP.executeUpdate();
		int id = stmntID.executeUpdate();
		int ii = stmntII.executeUpdate();
		Helpers.log("Result: " + obj.getZ_ACCOUNT_DESCRIPTION(),writer);
		Helpers.log(ip + "-" + id + "-" + ii,writer);

		stmntID.close();
		stmntII.close();
		stmntIP.close();
	}

	static void deleteZACCOUNTPROPERTY(Connection conn, Obj obj, PrintWriter writer) throws SQLException {
		String deletePorts = "DELETE FROM ZACCOUNTPROPERTY WHERE ZKEY='PortNumber' AND ZOWNER = '"
				+ obj.getZ_PK() + "';";
		String deleteDynConf = "DELETE FROM ZACCOUNTPROPERTY WHERE ZKEY='DisableDynamicConfiguration' AND ZOWNER = '"
				+ obj.getZ_PK() + "';";
		String deleteInsecAuth = "DELETE FROM ZACCOUNTPROPERTY WHERE ZKEY='AllowsInsecureAuthentication' AND ZOWNER = '"
				+ obj.getZ_PK() + "';";
		Statement stmtnDP = conn.createStatement();
		Statement stmtnDD = conn.createStatement();
		Statement stmtnDI = conn.createStatement();
		int rsDP = stmtnDP.executeUpdate(deletePorts);
		int rsDD = stmtnDD.executeUpdate(deleteDynConf);
		int rsDI = stmtnDI.executeUpdate(deleteInsecAuth);
		Helpers.log("Result: " + rsDP + "-" + rsDD + "-" + rsDI,writer);

		stmtnDP.close();
		stmtnDD.close();
		stmtnDI.close();
	}
}

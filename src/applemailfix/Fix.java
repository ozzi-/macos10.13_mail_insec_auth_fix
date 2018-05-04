package applemailfix;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.oxbow.swingbits.dialog.task.CommandLink;
import org.oxbow.swingbits.dialog.task.TaskDialogs;

public class Fix {
	public static void main(String[] args) {
		Locale deLocale = new Locale("de","DE");
		Locale.setDefault(deLocale);

		JFrame jf = new JFrame("");
		String lang =  System.getProperty("user.language");
		String langChs="";
		
		if(lang.equals("de") || lang.equals("fr")) {
			langChs=lang.toUpperCase();
		}else {
			int choice = TaskDialogs.choice(jf,
					"", 
					"", 
					1, 
					new CommandLink("Français", ""),
					new CommandLink("Deutsch", "")
					);
			if(choice<0) {
				System.exit(1);
			}
			langChs = choice==1?"DE":"FR";	
		}
		System.out.println(langChs);
		deLocale = new Locale(langChs.toLowerCase(),langChs);
		ResourceBundle messages = ResourceBundle.getBundle("MessagesBundle",deLocale);
		Locale.setDefault(deLocale);
		Helpers.osCheck(messages);		
		
		String accountsPath;
		if(args.length!=0) {
			accountsPath = args[0].endsWith("/")?args[0]:args[0]+"/";
			System.out.println("Using supplied path :"+accountsPath);			
		}else {
			accountsPath = "/Users/" + System.getProperty("user.name") + "/Library/Accounts/";
		}
		
		PrintWriter writer = null;
		try {
			writer = new PrintWriter("/Users/" + System.getProperty("user.name")+"/amf_log.txt", "UTF-8");
		} catch (FileNotFoundException | UnsupportedEncodingException e1) {}
		
		Helpers.log("Path used "+accountsPath,writer);

		String accounts4SQL = "Accounts4.sqlite";
		String accounts4SQLshm = "Accounts4.sqlite-shm";
		String accounts4SQLwal = "Accounts4.sqlite-wal";

		Helpers.killMail();
		Helpers.backupFiles(messages, accountsPath, accounts4SQL, accounts4SQLshm, accounts4SQLwal);
		Helpers.log("Killed mail, backup done",writer);
		
		try {
			Class.forName("org.sqlite.JDBC");
			File f = new File(accountsPath + accounts4SQL);
			if (!f.exists()) {
				throw new FileNotFoundException("DB File not found");
			}
			Connection conn = DriverManager.getConnection("jdbc:sqlite:" + accountsPath + accounts4SQL);
			String selectZAccounts = "SELECT * FROM ZACCOUNT";
			Statement stmtn = conn.createStatement();
			ResultSet rs = stmtn.executeQuery(selectZAccounts);
			Set<String> accounts = new LinkedHashSet<String>();
			ArrayList<Obj> ol = new ArrayList<Obj>();
			String prefilled = "";
			Helpers.log("Accounts found: ",writer);
			while (rs.next()) {
				Obj obj = new Obj();
				obj.setZ_PK(rs.getInt("Z_PK"));
				String accDesc = rs.getString("ZACCOUNTDESCRIPTION")==null?"-":rs.getString("ZACCOUNTDESCRIPTION");
				String usrName= rs.getString("ZUSERNAME")==null?"-":rs.getString("ZUSERNAME");
				Helpers.log(accDesc+" -> "+usrName,writer);
				obj.setZ_ACCOUNT_DESCRIPTION(accDesc);
				obj.setZ_ACCOUNT_TYPE_ID(rs.getInt("ZACCOUNTTYPE"));
				String selectAccountType = "SELECT * FROM ZACCOUNTTYPE WHERE Z_PK = " + obj.getZ_ACCOUNT_TYPE_ID();
				Statement stmtnAT = conn.createStatement();
				ResultSet rsAT = stmtnAT.executeQuery(selectAccountType);
				while (rsAT.next()) {
					obj.setZ_ACCOUNT_TYPE(rsAT.getString("ZACCOUNTTYPEDESCRIPTION"));
				}
				ol.add(obj);
				String accGUILabel = accDesc+(usrName.equals("-")?"":" ("+usrName+")");
				if(accGUILabel.startsWith(messages.getString("prefilledaccount"))) {
					prefilled = accGUILabel;
				}
				obj.setZ_ACCOUNT_DESCRIPTION_LABEL(accGUILabel);
				accounts.add(accGUILabel);
			}
			Helpers.log("Done Parsing",writer);

			if(ol.size()==0) {
				JOptionPane.showMessageDialog(null,
						messages.getString("noaccounts"),
						"Error", JOptionPane.ERROR_MESSAGE);
				if(writer!=null) {
					writer.println("no accounts err");
					writer.close();			
				}
				System.exit(1);
			}
			
			List<String> list = new ArrayList<String>();
			list.addAll(accounts);
			Collection<String> result = TaskDialogs
				.build(jf, messages.getString("chooseaccount"), null)
				.checkChoice(list, Arrays.asList(prefilled));
			if(result==null) {
				if(writer!=null) {
					writer.println("no result from choose dialog, exiting");
					writer.close();			
				}
				System.exit(0);
			}
			
			BlobLoader blobloader = new BlobLoader();
			byte[] blob_bool = blobloader.loadBlobByteArr("blob_bool_true.txt");
			byte[] blob_smtp = blobloader.loadBlobByteArr("port_blob_5018_smtp.txt");
			byte[] blob_pop3 = blobloader.loadBlobByteArr("port_blob_5019_pop3.txt");
			byte[] blob_imap = blobloader.loadBlobByteArr("port_blob_5020_imap.txt");
			
			Helpers.log("Starting DB manipulation",writer);

			for (String doitfor : result) {
				for (Obj obj : ol) {
					if (obj.getZ_ACCOUNT_DESCRIPTION_LABEL().equals(doitfor)) {
						Helpers.log("-> " + obj.getZ_ACCOUNT_DESCRIPTION() + " " + obj.getZ_ACCOUNT_TYPE(),writer);
						Helpers.deleteZACCOUNTPROPERTY(conn, obj, writer);
						Helpers.insertZACCOUNTPROPERTY(conn, blob_bool, blob_smtp, blob_imap, blob_pop3, obj, writer);
						Helpers.log("Done for "+obj.getZ_ACCOUNT_DESCRIPTION()+" "+obj.getZ_ACCOUNT_TYPE(),writer);
					}
				}
			}
			Helpers.log("Done ",writer);
			writer.close();
			JOptionPane.showMessageDialog(null,
					messages.getString("success"),
				"", JOptionPane.PLAIN_MESSAGE);
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null,
				messages.getString("errorload")+"\n( " + e.getMessage() + " )",
				"Error", JOptionPane.ERROR_MESSAGE);
			if(writer!=null) {
				writer.println("exception "+e.getMessage());
				writer.close();			
			}
			System.exit(1);
		}
	}
}

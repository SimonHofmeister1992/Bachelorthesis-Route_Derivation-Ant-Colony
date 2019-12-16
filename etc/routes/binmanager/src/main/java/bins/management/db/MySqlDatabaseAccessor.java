package bins.management.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import bins.management.Config;

public class MySqlDatabaseAccessor {

	private final String DB_HOST;
	private final String DB_NAME;
	private final String DB_USER;
	private final String DB_PWD;
	
	private static String mysql_url;
	private Connection conn;
	
	public MySqlDatabaseAccessor() {
		Config config = new Config();
		DB_HOST = config.getProperty("mDbHost");
		DB_NAME = config.getProperty("mDbName");
		DB_USER = config.getProperty("mDbUser");
		DB_PWD = config.getProperty("mDbPwd");
		
		mysql_url = "jdbc:mysql://" + DB_HOST + "/" + DB_NAME + "?useSSL=false" +
				  "&useUnicode=true&useJDBCCompliantTimezoneShift=true" + "" +
	            "&useLegacyDatetimeCode=false" +
	            "&serverTimezone=UTC";

	}

	public Connection getConnection() {
		if(this.conn==null) {
			try {
				this.conn=DriverManager.getConnection(mysql_url, DB_USER, DB_PWD);
			}
			catch(SQLException e) {
				e.printStackTrace();
			}
		}
		return this.conn;
	}
	
	
    public ResultSet fireQuery(Connection conn, String query, StatementType type) {
    	ResultSet rs = null;
    	try {
    			Statement stmt = conn.createStatement();
    			if(type != StatementType.SELECT) stmt.executeUpdate(query);
    			else rs = stmt.executeQuery(query);
    	} catch (SQLException e) {
				e.printStackTrace();
			}
		return rs;
	}
	
    public ResultSet fireQuery(Connection conn, PreparedStatement query, StatementType type) {
    	ResultSet rs = null;
    	try {
    			if(type != StatementType.SELECT) query.executeUpdate();
    			else rs = query.executeQuery();
    	} catch (SQLException e) {
				e.printStackTrace();
			}
		return rs;
	}
}

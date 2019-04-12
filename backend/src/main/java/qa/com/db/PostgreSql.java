
package qa.com.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import qa.com.classDefinition.User;
import qa.com.classDefinition.UserInfo;
import qa.com.ssoException.CannotAcquireDataException;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author li.jie updated by avbee
 */

@SpringBootApplication
//@Configuration
@Component
public class PostgreSql {

	/**
	 * @method getConn() 获取数据库的连接
	 * @return Connection
	 */
	@Autowired
	private retryDBConnection retryConnection;

	public Connection getConn(String url, String username, String password)
			throws SQLException, ClassNotFoundException, IOException, CannotAcquireDataException {
		String driver = "org.postgresql.Driver";
		Connection conn = null;
		try {
			Class.forName(driver); // classLoader,加载对应驱动
			conn = (Connection) DriverManager.getConnection(url, username, password);

		} catch (ClassNotFoundException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();

		} catch (SQLException e) {
			conn = retryConnection.getConnect(driver, url, username, password);

		}

		return conn;
	}

	public void insertUser(String url, String dbusername, String dbpassword, String username) {

		try {
			Connection conn = getConn(url, dbusername, dbpassword);
			Statement stmt = conn.createStatement();
			// long time = new Date().getTime()/1000;

			String groupName = "g_sample";
			String schemaName = "sample";
			String sql = String.format("create schema if not exists %s;" + "ALTER SCHEMA %s OWNER TO %s;"
					+ "create table if not exists %s.sso_user(id serial primary key not null, username text not null);"
					+ "ALTER TABLE %s.sso_user OWNER TO %s;" + "GRANT ALL ON ALL TABLES IN SCHEMA %s TO %s;"
					+ "GRANT ALL ON ALL SEQUENCES IN SCHEMA %s TO %s;", schemaName, schemaName, groupName, schemaName,
					schemaName, groupName, schemaName, groupName, schemaName, groupName);
			System.out.print(sql);
			stmt.execute(sql);
			sql = String.format("select * from %s.sso_user where username = '%s'", schemaName, username);
			System.out.print(sql);
			ResultSet rs = stmt.executeQuery(sql);
			if (rs.next()) {

			} else {
				sql = String.format("insert into %s.sso_user (username) values ('%s');", schemaName, username);
				System.out.print(sql);
				stmt.execute(sql);
			}
			rs.close();
			stmt.close();
			conn.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());

		}
	}

	public void deleteUser(String url, String dbusername, String dbpassword, String username) {
		try {
			String schemaName = "sample";
			Connection conn = getConn(url, dbusername, dbpassword);
			Statement stmt = conn.createStatement();
			// long time = new Date().getTime()/1000;
			String sql = String.format("delete from %s.sso_user where username = '%s'", schemaName, username);
			stmt.execute(sql);
			stmt.close();
			conn.close();
		} catch (Exception e) {

		}
	}

	public User getUserList(String url, String dbusername, String dbpassword) {
		User users = new User();
		try {
			String schemaName = "sample";
			Connection conn = getConn(url, dbusername, dbpassword);
			Statement stmt = conn.createStatement();
			String sql = String.format("select * from %s.sso_user", schemaName);
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				UserInfo userInfo = new UserInfo();
				userInfo.setUserName(rs.getString("username"));
				users.UserList.add(userInfo);
				users.setTotalCount(users.getTotalCount() + 1);
			}
			rs.close();
			stmt.close();
			conn.close();
		} catch (Exception e) {
			users.setErrorDescription(e.getMessage());
		}
		return users;
	}

}

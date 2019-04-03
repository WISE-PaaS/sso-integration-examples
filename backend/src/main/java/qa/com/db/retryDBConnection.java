/*
 * avbee 040219
*/
package qa.com.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class retryDBConnection {

	/*
	 * CONSTANT VARIABLE FOR @RETRYABLE
	 */

	final long startDelay = 1000;
	final int maxAttempts = 10000;
	final long maxDelay = 1000;
	final double multiplier = 2;

	/*
	 * CONSTANT VARIABLE JDBC ERROR CODE
	 */
	final int CANNOT_ACQUIRE_DATA_SOURCE = 7060;
		
			
	int i = 1;

	private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(retryDBConnection.class);

	@Retryable(value = {
			SQLException.class }, maxAttempts = maxAttempts, backoff = @Backoff(delay = startDelay, maxDelay = maxDelay, multiplier = multiplier))
	public Connection getConnect(String driver, String url, String username, String password)
			throws SQLException, ClassNotFoundException {

		Class.forName(driver);
		Connection conn = null;
		try {
			LOGGER.info("Connection Attempt = " + i++);
			conn = (Connection) DriverManager.getConnection(url, username, password);

			LOGGER.info("Connection Established!");

		} catch (SQLException e) {
		
			switch (e.getErrorCode()) {
			case CANNOT_ACQUIRE_DATA_SOURCE:
				getRecoveryStatus();

			default:
				throw e;

			}

		}
		return conn;
	}

	@Recover
	public static String getRecoveryStatus() {
		LOGGER.info("SSO_dbConnection.recover");
		return "Recovered from NullPointer Exception. Can't re-establish connection to DB";
	}
}
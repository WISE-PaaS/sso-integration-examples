/*
 * avbee 040219
*/
package qa.com.db;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import qa.com.ssoException.CannotAcquireDataException;

@Component
public class retryDBConnection {

	/*
	 * CONSTANT VARIABLE FOR @RETRYABLE
	 */
	final int milliseconds = 1000;

	final long startDelay = 2 * milliseconds;
	final int maxAttempts = 3;
	final long maxDelay = 60 * 60 * milliseconds;
	final double multiplier = 2.0;

	/*
	 * CONSTANT VARIABLE JDBC ERROR CODE
	 */

	final int CANNOT_ACQUIRE_DATA_SOURCE = 7060;

	int i = 1;

	private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(retryDBConnection.class);

	@Retryable(value = {
			SQLException.class }, maxAttempts = maxAttempts, backoff = @Backoff(delay = startDelay, maxDelay = maxDelay, multiplier = multiplier))
	public Connection getConnect(HttpServletRequest req, String driver, String url, String username, String password)
			throws SQLException, ClassNotFoundException, IOException, CannotAcquireDataException {

		Class.forName(driver);
		Connection conn = null;
		try {
			LOGGER.info("Conneqfrggrgection Established!");
			System.out.printf("is commited: " + req.getRequestURL());
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
	public static String getRecoveryStatus() throws CannotAcquireDataException {
		LOGGER.info("SSO_dbConnection.recover");
		throw new CannotAcquireDataException(
				"Recovered from CannotAcquireDataException.class Can't re-establish connection to DB");

	}
}
//
//@Component
//@Configuration
//public class retryDBConnection {
//
//	private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(retryDBConnection.class);
//
//	@Bean
//	public List<RetryListener> retryListeners() {
//		return Collections.singletonList(new RetryListener() {
//
//			@Override
//			public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
//				// TODO Auto-generated method stub
//				Field labelField = ReflectionUtils.findField(callback.getClass(), "val$label");
//				ReflectionUtils.makeAccessible(labelField);
//				String label = (String) ReflectionUtils.getField(labelField, callback);
//				LOGGER.warn("Starting retryable method {}", label);
//				return true;
//			}
//
//			@Override
//			public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback,
//					Throwable throwable) {
//
//				LOGGER.warn("Connection attempt {} exception {}", context.getRetryCount(), throwable.toString());
//
//				return;
//
//			}
//
//			@Override
//			public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback,
//					Throwable throwable) {
//				LOGGER.trace("Finished retryable method");
//
//			}
//		});
//
//	}
//
//	@Bean
//	public Services service() {
//		return new Services();
//	}
//}
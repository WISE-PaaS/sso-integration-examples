package qa.com.ssoException;

public class CannotAcquireDataException extends Exception {

	private static final long serialVersionUID = 4234807164568983813L;

	public CannotAcquireDataException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public CannotAcquireDataException(String message) {
		super(message);
	}
}

package at.bitfire.davdroid.webdav;

import org.apache.http.HttpException;

public class PermanentlyMovedException extends HttpException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1655002285399629289L;
	public PermanentlyMovedException(String reason) {
		super(reason);
	}

}

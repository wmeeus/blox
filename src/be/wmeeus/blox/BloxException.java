package be.wmeeus.blox;

/**
 * An exception for the Blox framework.
 * @author Wim Meeus
 *
 */

@SuppressWarnings("serial")
public class BloxException extends Exception {
	public BloxException() {
		super();
	}
	public BloxException(String s) {
		super(s);
	}
}

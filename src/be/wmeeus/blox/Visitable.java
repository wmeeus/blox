package be.wmeeus.blox;

/**
 * Interface Visitable is part of the Visitor design pattern.
 * @author Wim Meeus
 *
 */
public interface Visitable {
	public void accept(Visitor visitor);
}

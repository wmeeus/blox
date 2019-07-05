package be.wmeeus.blox;

/**
 * Interface Visitor is part of the Visitor design pattern.
 * @author Wim Meeus
 *
 */
public interface Visitor {
	public void visit(Bloxnode b);
	public void visit(Bloxinst b);
	public void visit(Bloxport b);
}

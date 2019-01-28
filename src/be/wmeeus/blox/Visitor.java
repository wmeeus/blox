package be.wmeeus.blox;

public interface Visitor {
	public void visit(Bloxnode b);
	public void visit(Bloxinst b);
	public void visit(Bloxport b);
}

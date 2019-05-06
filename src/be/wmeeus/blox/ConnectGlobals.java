package be.wmeeus.blox;

public class ConnectGlobals implements Visitor {

	public void visit(Bloxnode b) {
		b.connectGlobals();
	}

	public void visit(Bloxinst b) {
		b.connectGlobals();
	}

	public void visit(Bloxport b) {
		// Nothing to do
	}

}

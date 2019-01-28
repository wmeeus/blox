package be.wmeeus.blox;

public class ConnectNodes implements Visitor {

	public void visit(Bloxnode b) {
		b.connectNodes();
	}

	public void visit(Bloxinst b) {
		// Nothing to do
	}

	public void visit(Bloxport b) {
		// Nothing to do
	}

}

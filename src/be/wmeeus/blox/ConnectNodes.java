package be.wmeeus.blox;

/**
 * Class ConnectNodes implements the construction of connections using the 
 * Visitor design pattern
 * @author Wim Meeus
 *
 */
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

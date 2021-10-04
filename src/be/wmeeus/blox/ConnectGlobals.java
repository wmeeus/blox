package be.wmeeus.blox;

/**
 * Class ConnectGlobals implements the construction of global connections using the 
 * Visitor design pattern
 * @author Wim Meeus
 *
 */
public class ConnectGlobals implements Visitor {

	public void visit(Bloxnode b) {
		b.connectGlobals();
	}

	public void visit(Bloxinstance b) {
		b.connectGlobals();
	}

	public void visit(Bloxport b) {
		// Nothing to do
	}

}

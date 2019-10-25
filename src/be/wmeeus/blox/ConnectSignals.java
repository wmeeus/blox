package be.wmeeus.blox;

public class ConnectSignals implements Visitor {

	@Override
	public void visit(Bloxnode b) {
		b.connectSignals();
	}

	@Override
	public void visit(Bloxinst b) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Bloxport b) {
		// TODO Auto-generated method stub
		
	}

}

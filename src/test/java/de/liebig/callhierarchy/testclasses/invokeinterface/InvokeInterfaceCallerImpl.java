package de.liebig.callhierarchy.testclasses.invokeinterface;

public class InvokeInterfaceCallerImpl implements InvokeInterfaceCaller {

    private InvokeInterfaceCallee callee;
    public InvokeInterfaceCallerImpl(InvokeInterfaceCallee callee) {
        this.callee = callee;
    }

    @Override
    public void invoke() {
        callee.printMessage(System.out);
    }

    public void setCallee(InvokeInterfaceCallee newCallee) {
        this.callee = newCallee;
    }
}

package de.liebig.callhierarchy.testclasses.simpleinvokes;

public class SimpleInvokeCaller {

    private SimpleInvokeCallee callee;

    public SimpleInvokeCaller(SimpleInvokeCallee callee) {
        this.callee = callee;
    }

    public void invokeCalleePrintMessage() {
        this.callee.printMessage(System.out);
    }

    public void setCallee(SimpleInvokeCallee callee) {
        this.callee = callee;
    }
}

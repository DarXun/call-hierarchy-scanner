package de.liebig.callhierarchy.testclasses.simpleinvokes;

public class SimpleInvokeMain {

    public static void main(String[] args) {
        SimpleInvokeCallee callee1 = new SimpleInvokeCallee("A message from callee1");

        SimpleInvokeCaller simpleInvokeCaller = new SimpleInvokeCaller(callee1);
        simpleInvokeCaller.invokeCalleePrintMessage();

        SimpleInvokeCallee callee2 = new SimpleInvokeCallee("callee2 says hello");
        setNewCallee(simpleInvokeCaller, callee2);

        simpleInvokeCaller.invokeCalleePrintMessage();
    }

    private static void setNewCallee(SimpleInvokeCaller caller, SimpleInvokeCallee callee) {
        caller.setCallee(callee);
    }
}

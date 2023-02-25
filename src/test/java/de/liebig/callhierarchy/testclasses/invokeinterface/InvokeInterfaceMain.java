package de.liebig.callhierarchy.testclasses.invokeinterface;

import java.io.PrintStream;

public class InvokeInterfaceMain {

    public static void main(String[] args) {
        InvokeInterfaceCallee callee1 = new InvokeInterfaceCalleeImpl1("Message from callee1");
        InvokeInterfaceCaller caller = new InvokeInterfaceCallerImpl(callee1);
        caller.invoke();
        ((InvokeInterfaceCallerImpl) caller).setCallee(new InvokeInterfaceCalleeImpl2("This is callee2"));
        caller.invoke();
    }

    private static class InvokeInterfaceCalleeImpl2 implements InvokeInterfaceCallee {
        private String message;
        public InvokeInterfaceCalleeImpl2(String message) {
            this.message = message;
        }

        @Override
        public void printMessage(PrintStream ps) {
            ps.println(message);
        }
    }
}

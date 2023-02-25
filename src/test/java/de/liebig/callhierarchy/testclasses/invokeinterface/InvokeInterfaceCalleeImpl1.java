package de.liebig.callhierarchy.testclasses.invokeinterface;

import java.io.PrintStream;

public class InvokeInterfaceCalleeImpl1 implements InvokeInterfaceCallee {

    private String message;
    public InvokeInterfaceCalleeImpl1(String messageFromCallee1) {
        this.message = message;
    }

    @Override
    public void printMessage(PrintStream ps) {
        ps.println(message);
    }
}

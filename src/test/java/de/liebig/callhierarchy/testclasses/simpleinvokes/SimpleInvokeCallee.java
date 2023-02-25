package de.liebig.callhierarchy.testclasses.simpleinvokes;

import java.io.PrintStream;

public class SimpleInvokeCallee {

    private final String message;

    public SimpleInvokeCallee(String message) {
        this.message = message;
    }

    public void printMessage(PrintStream psOut) {
        psOut.println(message);
    }
}

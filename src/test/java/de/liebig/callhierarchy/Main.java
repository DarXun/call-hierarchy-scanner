package de.liebig.callhierarchy;

import de.liebig.callhierarchy.model.ChMethodInvocation;
import de.liebig.callhierarchy.model.ChMethodNode;
import de.liebig.callhierarchy.util.PrintUtils;
import de.liebig.callhierarchy.util.PrintUtils2;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

public class Main {
    @Test
    @Disabled
    void testSimpleInvoke() {
        String methodName = "main";
        String description = "([Ljava/lang/String;)V";
        String className = "de.liebig.callhierarchy.testclasses.simpleinvokes.SimpleInvokeMain";

        ChMethodNode cmn = new ClassScanner().scanMethod(methodName, description, className);

        PrintUtils2.printCallTree(cmn, 8);
    }

    @Test
    void testInvokeInterface() {
        String methodName = "main";
        String description = "([Ljava/lang/String;)V";
        String className = "de.liebig.callhierarchy.testclasses.invokeinterface.InvokeInterfaceMain";

        ChMethodNode cmn = new ClassScanner().scanMethod(methodName, description, className);

        PrintUtils2.printCallTree(cmn, 8);
    }
}
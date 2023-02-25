package de.liebig.callhierarchy;

import de.liebig.callhierarchy.model.ChMethodInvocation;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class ConstantPoolResolver {

    private static final AtomicReference<ConstantPoolResolver> INSTANCE = new AtomicReference<>(new ConstantPoolResolver());

    private Map<String, Integer> classCountMap;
    private Deque<ConstantPoolRefInvocation> refInvocations;

    private ConstantPoolResolver() {
        classCountMap = new HashMap<>();
        refInvocations = new ArrayDeque<>();
    }

    public static ConstantPoolResolver getInstance() {
        return INSTANCE.get();
    }

    public Map<String, Integer> getClassCountMap() {
        return classCountMap;
    }

    public void addReference(String className, ChMethodInvocation invocation) {
        refInvocations.add(new ConstantPoolRefInvocation(className, invocation));
    }

    public Map<String, Integer> resolveClassesUntil(ChMethodInvocation invocation) {
        while (true) {
            ConstantPoolRefInvocation storedInvocation = refInvocations.peekLast();
            if (storedInvocation == null) {
                break;
            }
            refInvocations.removeLast();

            // Increase usage count if present
            classCountMap.compute(storedInvocation.className, (key, value) -> {
                if (value == null) {
                    return Integer.valueOf(0);
                }
                return Integer.valueOf(value.intValue() + 1);
            });

            if (invocation == storedInvocation.methodInvocation) {
                break;
            }
        }

        return classCountMap;
    }

    class ConstantPoolRefInvocation {
        String className;
        ChMethodInvocation methodInvocation;

        public ConstantPoolRefInvocation(String className, ChMethodInvocation methodInvocation) {
            this.className = className;
            this.methodInvocation = methodInvocation;
        }
    }
}

package de.liebig.callhierarchy.model;

import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ChMethodNode {

    private MethodNode method;

    private ChClassNode owner;

    private final String name;
    private final String desc;

    private final String ownerName;

    private List<ChMethodInvocation> invocations;

    private Map<ChMethodInvocation, ChMethodNode> methodForInvocation;

    public ChMethodNode(final String name, final String desc, final String ownerName) {
        this.name = name;
        this.desc = desc;
        this.ownerName = ownerName;

        invocations = new ArrayList<>();
        methodForInvocation = new HashMap<>();
    }

    public ChMethodNode(final MethodNode method, final ChClassNode chClassNode) {
        this(method.name, method.desc, chClassNode.getClassName());

        this.owner = chClassNode;
        this.method = method;
    }

    public void addCallee(ChMethodInvocation cmi, ChMethodNode cmn) {
        if (invocations.contains(cmi)) {
            methodForInvocation.put(cmi, cmn);
        } else {
            throw new IllegalArgumentException(String.format("The invocation %s is unknown for this node.", cmi.toString()));
        }
    }

    public ChMethodNode getCallee(ChMethodInvocation cmi) {
        return methodForInvocation.get(cmi);
    }

    public void addInvocation(final ChMethodInvocation invocation) {
        invocations.add(invocation);
    }

    public void addInvocation(final String name, final String desc, final String owner, final Supplier<List<String>> instanceTypeResolver) {
        invocations.add(new ChMethodInvocation(name, desc, owner, this, instanceTypeResolver));
    }

    public void addInvocation(final String name, final String desc, final String owner, final String instanceType) {
        invocations.add(new ChMethodInvocation(name, desc, owner, this, instanceType));
    }

    public List<ChMethodInvocation> getInvocations() {
        return invocations;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public String getOwnerName() { return ownerName; }

    @Override
    public String toString() {
        return "ChMethodNode{" +
                "name='" + name + '\'' +
                ", desc='" + desc + '\'' +
                ", ownerName='" + ownerName + '\'' +
                '}';
    }
}

package de.liebig.callhierarchy.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class ChMethodInvocation {
    private final String name;
    private final String desc;
    private final String owner;

    private List<String> instanceTypes;

    private Supplier<List<String>> instanceTypeCandidateResolver;

    // TODO instance this was invoked on

    private final ChMethodNode caller;

    public ChMethodInvocation(final String name, final String desc, final String owner, final ChMethodNode caller) {
        this.name = name;
        this.desc = desc;
        this.owner = owner;
        this.caller = caller;
    }

    public ChMethodInvocation(final String name, final String desc, final String owner, final ChMethodNode caller, final String instanceType) {
        this.name = name;
        this.desc = desc;
        this.owner = owner;
        this.caller = caller;
        this.instanceTypes = new ArrayList<>();
        this.instanceTypes.add(instanceType);
    }

    public ChMethodInvocation(String name, String desc, String owner, ChMethodNode caller, Supplier<List<String>> instanceTypeCandidateResolver) {
        this.name = name;
        this.desc = desc;
        this.owner = owner;
        this.caller = caller;
        this.instanceTypeCandidateResolver = instanceTypeCandidateResolver;
    }

    public List<String> getInstanceTypes() {
        if (instanceTypes == null) {
            instanceTypes = instanceTypeCandidateResolver.get();
        }

        return instanceTypes;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public String getOwner() {
        return owner;
    }

    @Override
    public String toString() {
        return "ChMethodInvocation{" +
                "name='" + name + '\'' +
                ", desc='" + desc + '\'' +
                ", owner='" + owner + '\'' +
                '}';
    }

    public void setInstanceTypeResolver(Supplier<List<String>> instanceTypeResolver) {
        this.instanceTypeCandidateResolver = instanceTypeResolver;
    }
}
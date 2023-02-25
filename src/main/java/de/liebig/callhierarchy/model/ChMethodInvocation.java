package de.liebig.callhierarchy.model;

public class ChMethodInvocation {
    private final String name;
    private final String desc;
    private final String owner;

    // TODO instance this was invoked on

    private final ChMethodNode caller;

    public ChMethodInvocation(final String name, final String desc, final String owner, final ChMethodNode caller) {
        this.name = name;
        this.desc = desc;
        this.owner = owner;

        this.caller = caller;
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
}
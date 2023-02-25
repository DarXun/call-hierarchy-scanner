package de.liebig.callhierarchy.model;

import de.liebig.callhierarchy.util.MethodUtil;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;

public class ChClassNode {
    private final Map<String, MethodNode> methods;

    private final List<ChMethodNode> chMethodNodes;

    private ClassNode classNode;

    private final String className;

    private final String ressourceName;

    public ChClassNode(final String className) {
        methods = new HashMap<>();
        chMethodNodes = new ArrayList<>();

        this.className = className;
        ressourceName = '/' + className.replace('.', '/') + ".class";
    }

    public String getClassName() {
        return className;
    }

    public String getRessourceName() {
        return ressourceName;
    }

    /**
     * Looks for a method with the given name and description in this ChClassNodes known methods and returns it.
     * @param name
     * @param desc
     * @return
     */
    public Optional<MethodNode> findMethod(final String name, final String desc) {
        return Optional.ofNullable(methods.get(MethodUtil.buildMethodKey(name, desc)));
    }

    public void addMethod(MethodNode mn) {
        methods.put(MethodUtil.buildMethodKey(mn.name, mn.desc), mn);
    }

    /**
     * Looks for a ChMethodNode with the given name and description in this ChClassNode and returns it.
     * @param name
     * @param desc
     * @return
     */
    public Optional<ChMethodNode> getChMethodNode(String name, String desc) {
        return chMethodNodes.stream().filter(cmn -> name.equals(cmn.getName()) && desc.equals(cmn.getDesc())).findFirst();
    }

    public ClassNode getClassNode() {
        return classNode;
    }

    public void setClassNode(ClassNode classNode) {
        this.classNode = classNode;
    }

    public void addChMethodNode(final ChMethodNode chMethodNode) {
        this.chMethodNodes.add(chMethodNode);
    }

    public List<ChMethodNode> getChMethodNodes() {
        return chMethodNodes;
    }

}

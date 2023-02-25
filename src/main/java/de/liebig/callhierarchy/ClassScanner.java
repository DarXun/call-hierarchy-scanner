package de.liebig.callhierarchy;

import de.liebig.callhierarchy.model.ChClassNode;
import de.liebig.callhierarchy.model.ChMethodInvocation;
import de.liebig.callhierarchy.model.ChMethodNode;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import javax.swing.text.html.Option;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ClassScanner {

    private static final Map<String, ChClassNode> CLASS_CACHE = new HashMap<>();

    public ChMethodNode scanMethod(final String methodName, final String className) {
        return scanMethod(methodName, null, className);
    }

    public ChMethodNode scanMethod(final String methodName, final String description, final String className) {
        ChMethodNode chMethodNode;

        // TODO introduce parametrization to let the user decide on boundaries (e.g. list of predicates)
        if (className.startsWith("java/")) {
            ChClassNode dummyClassNode;
            chMethodNode = new ChMethodNode(methodName, description, className);
            try {
                dummyClassNode = loadClass(className);
            } catch (ClassNotFoundException e) {
                dummyClassNode = new ChClassNode(className);
            }
            dummyClassNode.addChMethodNode(chMethodNode);
            return chMethodNode;
        }

        final ChClassNode chClassNode;

        try {
            chClassNode = loadClass(className);
            chMethodNode = chClassNode.findMethod(methodName, description).map(mn -> createChMethodNode(chClassNode, mn)).orElseGet(() -> {
                final Optional<ChClassNode> owner = findOwner(chClassNode, methodName, description);
                if (!owner.isPresent()) {
                    return createDummyMethod(methodName, description, className);
                }

                // TODO introduce scanMethod with ChClassNode instead of className String?
                return scanMethod(methodName, description, owner.get().getClassName());
            });
        } catch (ClassNotFoundException e) {
            chMethodNode = createDummyMethod(methodName, description, className);
        }

        // the chMethodNode knows all its invocations which we'll now resolve (so we know the chMethodNode behind these invocations)
        resolveInvocations(chMethodNode);

        return chMethodNode;
    }

    private void resolveInvocations(final ChMethodNode chMethodNode) {
        for (final ChMethodInvocation invocation : chMethodNode.getInvocations()) {
            ChMethodNode cmn = null;

            for (String owner : invocation.getInstanceTypes()) {
                // the invocation's got an owner which we may already have analyzed
                if (CLASS_CACHE.containsKey(owner)) {
                    ChClassNode chClassNode = CLASS_CACHE.get(owner);

                    // was the ChMethodNode for this invocation already created?
                    Optional<ChMethodNode> mnForInvocation = chClassNode.getChMethodNode(invocation.getName(), invocation.getDesc());
                    if (mnForInvocation.isPresent()) {
                        cmn = mnForInvocation.get();
                    } else {
                        // otherwise we will now resolve it
                        Optional<MethodNode> mn = chClassNode.findMethod(invocation.getName(), invocation.getDesc());
                        if (mn.isPresent()) {
                            cmn = createChMethodNode(chClassNode, mn.get());
                            resolveInvocations(cmn);
                        }
                    }
                }

                if (cmn == null) {
                    System.out.println(String.format("Scanning %s %s %s next", invocation.getName(), invocation.getDesc(), owner));
                    cmn = scanMethod(invocation.getName(), invocation.getDesc(), owner);
                }

                chMethodNode.addCallee(invocation, cmn);
            }
        }

    }

    /**
     * Creates a dummy ChMethodNode.
     * May be used if the real method could not be resolved because of class-loading errors
     *
     * @param methodName
     * @param description
     * @param className
     * @return
     */
    private static ChMethodNode createDummyMethod(final String methodName, final String description, final String className) {
        return new ChMethodNode(methodName, description, String.format("OwnerUnknown$%s", className));
    }


    private ChMethodNode createChMethodNode(final ChClassNode chClassNode, final MethodNode method) {
        final ChMethodNode chMethodNode = new ChMethodNode(method, chClassNode);
        chClassNode.addChMethodNode(chMethodNode);

        String storedClassName = null;

        InsnList instructions = method.instructions;
        for (int i = 0; i < instructions.size(); i++) {
            AbstractInsnNode insn = instructions.get(i);

            switch (insn.getOpcode()) {
                case Opcodes.NEW:
                    TypeInsnNode tin = (TypeInsnNode) insn;
                    storedClassName = tin.desc;
                    System.out.println("NEW " + storedClassName);
                    break;

                case Opcodes.INVOKEVIRTUAL:
                case Opcodes.INVOKESPECIAL:
                case Opcodes.INVOKESTATIC:
                    MethodInsnNode min = (MethodInsnNode) insn;
                    chMethodNode.addInvocation(min.name, min.desc, min.owner, min.owner);

                    if (storedClassName != null) {
                        List<ChMethodInvocation> invocations = chMethodNode.getInvocations();
                        ConstantPoolResolver.getInstance().addReference(storedClassName, invocations.get(invocations.size() - 1));
                        storedClassName = null;
                    }

                    break;

                case Opcodes.INVOKEINTERFACE:
                    min = (MethodInsnNode) insn;

                    // We need to find all implementations for min.owner that have been used up until now
                    // This is done by collecting all "Class" constant-pool-references in the method-hierarchy
                    // if it's a local variable we should be able to track it though and be 100% sure of the implementation

                    // INVOKEINTERFACE operates on an instance, so we search for the previous ALOAD to find it
                    VarInsnNode aloadInsn = (VarInsnNode) findInsn(min, Opcodes.ALOAD).orElseThrow(() -> new IllegalStateException("No ALOAD found for " + min));
                    // maybe this is a local var, we should find an ASTORE then
                    Optional<AbstractInsnNode> optAstoreInsn = findInsn(aloadInsn, Opcodes.ASTORE);
                    if (optAstoreInsn.isPresent()) {
                        // FIXME must match the same var as aloadInsn!
                        MethodInsnNode invSpecialInsn = (MethodInsnNode) findInsn(optAstoreInsn.get(), Opcodes.INVOKESPECIAL).orElseThrow(() -> new IllegalStateException("No INVOKESPECIAL found for " + min));
                        chMethodNode.addInvocation(min.name, min.desc, min.owner, invSpecialInsn.owner);
                    } else {
                        // we can't resolve the owner right now.
                        // let's assume it's a candidate of one of the constant-pool-refs up until now (traverse up the hierarchy and get all subtypes of min.owner)
                        ChMethodInvocation addInvocation = new ChMethodInvocation(min.name, min.desc, min.owner, chMethodNode);

                        Supplier<List<String>> instanceTypeResolver = () -> {
                            List<String> instanceTypeCandidates = new ArrayList<String>();
                            // FIXME add all the constant-pool-refs for subtypes of min.owner!
                            // where do i get them from? maybe a global queue like ds (saves every constant-pool ref for a specific invocation and then we traverse it until we find that invocation)
//                            instanceTypeCandidates.add(min.owner);
                            Map<String, Integer> refClasses = ConstantPoolResolver.getInstance().resolveClassesUntil(addInvocation);
                            instanceTypeCandidates.addAll(refClasses.keySet());
                            return instanceTypeCandidates;
                        };

                        addInvocation.setInstanceTypeResolver(instanceTypeResolver);
                        chMethodNode.addInvocation(addInvocation);
                    }

                    break;

                case Opcodes.INVOKEDYNAMIC:
                    final InvokeDynamicInsnNode idin = (InvokeDynamicInsnNode) insn;
                    final Handle bsm = idin.bsm;

                    if ("java/lang/invoke/LambdaMetafactory".equals(bsm.getOwner()) && "metafactory".equals(bsm.getName()) && "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;".equals(bsm.getDesc())) {
                        final Handle lambdaHandle = (Handle) idin.bsmArgs[1];

                        chMethodNode.addInvocation(lambdaHandle.getName(), lambdaHandle.getDesc(), lambdaHandle.getOwner(), lambdaHandle.getOwner());
                    } else {
                        throw new IllegalArgumentException(String.format("Unrecognized INVOKEDYNAMIC-Instruction: %s", idin.toString()));
                    }

                    break;
            }
        }

        return chMethodNode;
    }

    private Optional<AbstractInsnNode> findInsn(AbstractInsnNode insn, int opcodeToFind) {
        if (insn == null) {
            throw new IllegalArgumentException("insn may not be null");
        }

        AbstractInsnNode prev = insn;
        while (prev.getPrevious() != null) {
            prev = prev.getPrevious();

            if (prev.getOpcode() == opcodeToFind) {
                return Optional.of(prev);
            }
        }

        return Optional.empty();
    }

    public Optional<ChClassNode> findOwner(final ChClassNode chClassNode, final String name, final String desc) {
        if (chClassNode.getClassNode() == null) {
            return Optional.empty();
        }

        // look in this class first
//        List<ChMethodNode> matches = chClassNode.getChMethodNodes().stream().filter(cmn -> desc != null ? name.equals(cmn.getName()) && desc.equals(cmn.getDesc()) : name.equals(cmn.getName())).collect(Collectors.toList());
//        if (matches.size() > 1) {
//            throw new IllegalArgumentException("Method could not be identified. There are multiple matches.");
//        } else if (matches.size() == 1) {
//            // TODO the ChMethodNode could just know it's owner...
//            try {
//                Optional.of(loadClass(matches.get(0).getOwnerName()));
//            } catch (ClassNotFoundException e) {
//                throw new RuntimeException(e);
//            }
//        }
        if (chClassNode.findMethod(name, desc).isPresent()) {
            return Optional.of(chClassNode);
        }

        Optional<ChClassNode> owner = Optional.empty();

        // look in super-class next
        final ClassNode classNode = chClassNode.getClassNode();
        final String superClsName = classNode.superName;

        ChClassNode chSuperClsNode = null;
        if (superClsName != null) {
            try {
                chSuperClsNode = loadClass(superClsName);

                if (chSuperClsNode.findMethod(name, desc).isPresent()) {
                    return Optional.of(chSuperClsNode);
                }
            } catch (ClassNotFoundException e) {
                // TODO Super-Class isn't on the classpath
                // FIXME maybe abort or let this be decided by the user
            }
        }

        // if it's not in the direct super-class, look into the interfaces
        final List<String> interfaces = chClassNode.getClassNode().interfaces;
        // save ChClassNodes for interfaces for later recursion-step
        final List<ChClassNode> chInterfaceClsNodes = new ArrayList<>(interfaces.size());

        for (final String interfaceName : interfaces) {
            try {
                final ChClassNode chInterfaceClsNode = loadClass(interfaceName);
                chInterfaceClsNodes.add(chInterfaceClsNode);

                if (chInterfaceClsNode.findMethod(name, desc).isPresent()) {
                    return Optional.of(chInterfaceClsNode);
                }
            } catch (ClassNotFoundException e) {
                // TODO Interface could not be loaded
                // FIXME maybe abort or let this be decided by the user
            }
        }

        // if it's neither in the direct super-class or the direct interfaces, we'll search via recursion
        // super-class first
        if (chSuperClsNode != null) {
            owner = findOwner(chSuperClsNode, name, desc);
        }
        if (owner.isPresent()) {
            return owner;
        }
        // then interfaces
        for (final ChClassNode interfaceClsNode : chInterfaceClsNodes) {
            owner = findOwner(interfaceClsNode, name, desc);
            if (owner.isPresent()) {
                return owner;
            }
        }

        return Optional.empty();
    }

    private boolean isOfType(ChClassNode chClassNode, final String clazzName) {
        if (clazzName.equals(chClassNode.getClassName())) {
            return true;
        }

        boolean isOfType = false;

        final ClassNode classNode = chClassNode.getClassNode();
        // this may happen if a class could not be loaded
        if (classNode == null) {
            return false;
        }

        // look into the super-class
        ChClassNode chSuperClsNode;
        boolean hasSuperCls = classNode.superName != null;
        if (hasSuperCls) {
            if (clazzName.equals(classNode.superName)) {
                return true;
            }

            // recursively look into the super-class
            try {
                chSuperClsNode = loadClass(classNode.superName);
                isOfType = isOfType(chSuperClsNode, clazzName);
                if (isOfType) {
                    return true;
                }
            } catch (ClassNotFoundException e) {
                // FIXME maybe abort or let this be decided by the user
                chSuperClsNode = null;
            }
        }

        // look into the interfaces
        for (int i = 0; i < classNode.interfaces.size() && !isOfType; i++) {
            String interfaceName = classNode.interfaces.get(i);

            ChClassNode interfaceClsNode;

            try {
                interfaceClsNode = loadClass(interfaceName);
            } catch (ClassNotFoundException e) {
                // Interface could not be loaded so we just continue
                // FIXME maybe abort or let this be decided by the user
                continue;
            }

            if (interfaceClsNode != null) {
                isOfType = isOfType(interfaceClsNode, clazzName);
            }
        }

        return isOfType;
    }

    private ChClassNode loadClass(String superClsName) throws ClassNotFoundException {
        if (CLASS_CACHE.containsKey(superClsName)) {
            return CLASS_CACHE.get(superClsName);
        }

        final ChClassNode chClassNode = new ChClassNode(superClsName);

        // TODO classes shouldn't need to be on the classpath
        try (final InputStream is = ClassScanner.class.getResourceAsStream(chClassNode.getRessourceName())) {
            final ClassReader classReader = new ClassReader(is);
            final ClassNode classNode = new ClassNode();
            classReader.accept(classNode, 0);
            chClassNode.setClassNode(classNode);

            // index all methods
            classNode.methods.forEach(chClassNode::addMethod);
        } catch (IOException e) {
            throw new ClassNotFoundException(String.format("Class %s could not be found.", chClassNode.getClassName()), e);
        }

        // TODO Next up: Dependency Injection (step 1: attribute call hierarchy, step 2: spring)
        /*
        if (isOfType(chClassNode, "some/package/MyClassName")) {
            // do something
        }
         */

        CLASS_CACHE.put(chClassNode.getClassName(), chClassNode);

        return chClassNode;
    }
}

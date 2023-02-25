package de.liebig.callhierarchy.util;

import de.liebig.callhierarchy.model.ChMethodInvocation;
import de.liebig.callhierarchy.model.ChMethodNode;

import java.util.List;

public class PrintUtils2 {

    public static void printCallTree(final ChMethodNode cmn, int maxDepth) {
        printCallTree(cmn, 0, maxDepth);
    }

    private static void printCallTree(final ChMethodNode cmn, int curDepth, int maxDepth) {
        List<ChMethodInvocation> invocations = cmn.getInvocations();

        System.out.println(indentToDepth(String.format("%s %s", cmnToString(cmn), invocations.size() > 0 ? "calls: " : "- no calls found"), curDepth));
        curDepth++;

        for (ChMethodInvocation cmi : invocations) {
            if (!cmi.getOwner().startsWith("java/")) {
                ChMethodNode callee = cmn.getCallee(cmi);
                if (callee != null && curDepth <= maxDepth) {
                    printCallTree(callee, curDepth, maxDepth);
                } else if (curDepth > maxDepth) {
                    System.err.println(indentToDepth("Maximum depth reached", curDepth));
                    return;
                }
            } else {
                System.out.println(indentToDepth(cmiToString(cmi), curDepth));
            }
        }
    }

    private static String cmnToString(ChMethodNode cmn) {
        return cmn.getName() + " from " + shortenClsRessourceName(cmn.getOwnerName());
    }

    private static String cmiToString(ChMethodInvocation cmi) {
        return cmi.getName() + " from " + shortenClsRessourceName(cmi.getOwner());
    }

    private static String shortenClsRessourceName(final String clsRessourceName) {
        String[] split = clsRessourceName.split("/");
        StringBuilder sb = new StringBuilder(clsRessourceName.length());
        for (int i = 0; i < split.length - 1; i++) {
            sb.append(split[i].length() <= 3? split[i] : split[i].substring(0, 3));
            sb.append(".");
        }
        sb.append(split[split.length-1]);

        return sb.toString();
    }

    private static String indentToDepth(final String str, int depth) {
        final StringBuilder sb = new StringBuilder(200);
        for (int i = 0; i < depth; i++) {
            sb.append('\t');
        }
        sb.append(str);
        return sb.toString();
    }

}

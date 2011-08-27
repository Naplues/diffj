package org.incava.diffj;

import java.io.*;
import java.util.*;
import net.sourceforge.pmd.ast.*;
import org.incava.analysis.*;
import org.incava.java.*;
import org.incava.ijdk.lang.*;
import org.incava.ijdk.util.*;
import org.incava.pmd.*;


public abstract class AbstractTypeItemDiff<Type extends SimpleNode> extends DiffComparator {

    private final Class<Type> cls;

    public AbstractTypeItemDiff(Collection<FileDiff> differences, Class<Type> cls) {
        super(differences);

        this.cls = cls;
    }

    @SuppressWarnings("unchecked")
    public static <Type extends SimpleNode> List<Type> getDeclarationsOfClass(ASTClassOrInterfaceBodyDeclaration[] decls, Class<Type> cls) {
        List<Type> declList = new ArrayList<Type>();

        for (ASTClassOrInterfaceBodyDeclaration decl : decls) {
            SimpleNode dec = TypeDeclarationUtil.getDeclaration(decl, cls);

            if (dec != null) {
                declList.add((Type)dec);
            }   
        }
        
        return declList;
    }

    public void compare(ASTClassOrInterfaceDeclaration aNode, ASTClassOrInterfaceDeclaration bNode) {
        ASTClassOrInterfaceBodyDeclaration[] aDecls = TypeDeclarationUtil.getDeclarations(aNode);
        ASTClassOrInterfaceBodyDeclaration[] bDecls = TypeDeclarationUtil.getDeclarations(bNode);

        List<Type> amds = getDeclarationsOfClass(aDecls, cls);
        List<Type> bmds = getDeclarationsOfClass(bDecls, cls);

        MultiMap<Double, Pair<Type, Type>> matches = new MultiMap<Double, Pair<Type, Type>>();

        for (Type amd : amds) {
            for (Type bmd : bmds) {
                double score = getScore(amd, bmd);
                if (score > 0.0) {
                    matches.put(score, new Pair<Type, Type>(amd, bmd));
                }
            }
        }

        List<Double> descendingScores = new ArrayList<Double>(new TreeSet<Double>(matches.keySet()));
        
        Collections.reverse(descendingScores);

        // go through best scores

        List<Type> unprocA = new ArrayList<Type>(amds);        
        List<Type> unprocB = new ArrayList<Type>(bmds);

        Collection<FileDiff> diffs = getFileDiffs();

        for (Double score : descendingScores) {
            // don't repeat comparisons ...

            List<Type> procA = new ArrayList<Type>();
            List<Type> procB = new ArrayList<Type>();

            for (Pair<Type, Type> declPair : matches.get(score)) {
                Type amd = declPair.getFirst();
                Type bmd = declPair.getSecond();

                if (!unprocA.contains(amd) || !unprocB.contains(bmd)) {
                    continue;
                }

                doCompare(amd, bmd);

                procA.add(amd);
                procB.add(bmd);
            }

            unprocA.removeAll(procA);
            unprocB.removeAll(procB);
        }

        addRemoved(unprocA, bNode);        
        addAdded(unprocB, aNode);
    }

    public abstract double getScore(Type amd, Type bmd);

    public abstract void doCompare(Type amd, Type bmd);

    public void addAdded(List<Type> bs, ASTClassOrInterfaceDeclaration aNode) {
        for (Type b : bs) {
            String name = getName(b);
            added(aNode, b, getAddedMessage(b), name);
        }
    }

    public void addRemoved(List<Type> as, ASTClassOrInterfaceDeclaration bNode) {
        for (Type a : as) {
            String name = getName(a);
            deleted(a, bNode, getRemovedMessage(a), name);
        }
    }

    public abstract String getName(Type t);

    public abstract String getAddedMessage(Type t);

    public abstract String getRemovedMessage(Type t);
}
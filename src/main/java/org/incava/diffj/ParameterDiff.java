package org.incava.diffj;

import java.util.Iterator;
import java.util.List;
import net.sourceforge.pmd.ast.ASTFormalParameter;
import net.sourceforge.pmd.ast.ASTFormalParameters;
import net.sourceforge.pmd.ast.Token;
import org.incava.analysis.FileDiff;
import org.incava.analysis.FileDiffs;
import org.incava.pmdx.ParameterUtil;

public class ParameterDiff extends DiffComparator {
    public static final String PARAMETER_REMOVED = "parameter removed: {0}";
    public static final String PARAMETER_ADDED = "parameter added: {0}";
    public static final String PARAMETER_REORDERED = "parameter {0} reordered from argument {1} to {2}";
    public static final String PARAMETER_TYPE_CHANGED = "parameter type changed from {0} to {1}";
    public static final String PARAMETER_NAME_CHANGED = "parameter name changed from {0} to {1}";
    public static final String PARAMETER_REORDERED_AND_RENAMED = "parameter {0} reordered from argument {1} to {2} and renamed {3}";

    public ParameterDiff(FileDiffs differences) {
        super(differences);
    }

    public void compareParameters(ASTFormalParameters fromFormalParams, ASTFormalParameters toFormalParams) {
        List<String> fromParamTypes = ParameterUtil.getParameterTypes(fromFormalParams);
        List<String> toParamTypes = ParameterUtil.getParameterTypes(toFormalParams);

        int fromSize = fromParamTypes.size();
        int toSize = toParamTypes.size();

        if (fromSize > 0) {
            if (toSize > 0) {
                compareEachParameter(fromFormalParams, toFormalParams, fromSize);
            }
            else {
                markParametersRemoved(fromFormalParams, toFormalParams);
            }
        }
        else if (toSize > 0) {
            markParametersAdded(fromFormalParams, toFormalParams);
        }
    }

    protected void markParametersAdded(ASTFormalParameters fromFormalParams, ASTFormalParameters toFormalParams) {
        List<Token> names = ParameterUtil.getParameterNames(toFormalParams);
        for (Token name : names) {
            changed(fromFormalParams, name, PARAMETER_ADDED, name.image);
        }
    }

    protected void markParametersRemoved(ASTFormalParameters fromFormalParams, ASTFormalParameters toFormalParams) {
        List<Token> names = ParameterUtil.getParameterNames(fromFormalParams);
        for (Token name : names) {
            changed(name, toFormalParams, PARAMETER_REMOVED, name.image);
        }
    }

    protected void markParameterTypeChanged(ASTFormalParameter fromParam, ASTFormalParameters toFormalParams, int idx) {
        ASTFormalParameter toParam = ParameterUtil.getParameter(toFormalParams, idx);
        String toType = ParameterUtil.getParameterType(toParam);
        changed(fromParam, toParam, PARAMETER_TYPE_CHANGED, ParameterUtil.getParameterType(fromParam), toType);
    }

    protected void markParameterNameChanged(ASTFormalParameter fromParam, ASTFormalParameters toFormalParams, int idx) {
        Token fromNameTk = ParameterUtil.getParameterName(fromParam);
        Token toNameTk = ParameterUtil.getParameterName(toFormalParams, idx);
        changed(fromNameTk, toNameTk, PARAMETER_NAME_CHANGED, fromNameTk.image, toNameTk.image);
    }

    protected void checkForReorder(ASTFormalParameter fromParam, int fromIdx, ASTFormalParameters toFormalParams, int toIdx) {
        Token fromNameTk = ParameterUtil.getParameterName(fromParam);
        Token toNameTk = ParameterUtil.getParameterName(toFormalParams, toIdx);
        if (fromNameTk.image.equals(toNameTk.image)) {
            changed(fromNameTk, toNameTk, PARAMETER_REORDERED, fromNameTk.image, fromIdx, toIdx);
        }
        else {
            changed(fromNameTk, toNameTk, PARAMETER_REORDERED_AND_RENAMED, fromNameTk.image, fromIdx, toIdx, toNameTk.image);
        }
    }

    protected void markReordered(ASTFormalParameter fromParam, int fromIdx, ASTFormalParameters toParams, int toIdx) {
        Token fromNameTk = ParameterUtil.getParameterName(fromParam);
        ASTFormalParameter toParam = ParameterUtil.getParameter(toParams, toIdx);
        changed(fromParam, toParam, PARAMETER_REORDERED, fromNameTk.image, fromIdx, toIdx);
    }

    protected void markRemoved(ASTFormalParameter fromParam, ASTFormalParameters toParams) {
        Token fromNameTk = ParameterUtil.getParameterName(fromParam);
        changed(fromParam, toParams, PARAMETER_REMOVED, fromNameTk.image);
    }

    /**
     * Compares each parameter. Assumes that the lists are the same size.
     */
    public void compareEachParameter(ASTFormalParameters fromFormalParams, ASTFormalParameters toFormalParams, int size) {
        List<ASTFormalParameter> fromFormalParamList = ParameterUtil.getParameters(fromFormalParams);
        List<ASTFormalParameter> toFormalParamList = ParameterUtil.getParameters(toFormalParams);

        for (int idx = 0; idx < size; ++idx) {
            ASTFormalParameter fromFormalParam = fromFormalParamList.get(idx);
            Integer[] paramMatch = ParameterUtil.getMatch(fromFormalParamList, idx, toFormalParamList);

            if (paramMatch[0] == idx && paramMatch[1] == idx) {
                continue;
            }
            else if (paramMatch[0] == idx) {
                markParameterNameChanged(fromFormalParam, toFormalParams, idx);
            }
            else if (paramMatch[1] == idx) {
                markParameterTypeChanged(fromFormalParam, toFormalParams, idx);
            }
            else if (paramMatch[0] >= 0) {
                checkForReorder(fromFormalParam, idx, toFormalParams, paramMatch[0]);
            }
            else if (paramMatch[1] >= 0) {
                markReordered(fromFormalParam, idx, toFormalParams, paramMatch[1]);
            }
            else {
                markRemoved(fromFormalParam, toFormalParams);
            }
        }

        Iterator<ASTFormalParameter> toIt = toFormalParamList.iterator();
        for (int toIdx = 0; toIt.hasNext(); ++toIdx) {
            ASTFormalParameter toParam = toIt.next();
            if (toParam != null) {
                ASTFormalParameter toFormalParam = ParameterUtil.getParameter(toFormalParams, toIdx);
                Token toName = ParameterUtil.getParameterName(toFormalParam);
                changed(fromFormalParams, toFormalParam, PARAMETER_ADDED, toName.image);
            }
        }
    }
}
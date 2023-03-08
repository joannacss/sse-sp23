package viz;

import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.slicer.*;
import com.ibm.wala.ssa.SymbolTable;

/**
 * @author Joanna C. S. Santos (jds5109@rit.edu)
 */
public class StatementNodeLabeller implements GraphVisualizer.NodeLabeller<Statement> {

    private final SDG<InstanceKey> sdg;

    public StatementNodeLabeller(SDG<InstanceKey> sdg) {
        this.sdg = sdg;
    }

    @Override
    public String getLabel(Statement s) {

        switch (s.getKind()) {
            case NORMAL:
                return /* sdg.getNumber(s) + "_" + */ ((StatementWithInstructionIndex) s).getInstruction().toString(s.getNode().getIR().getSymbolTable());
            case PHI:
                return "PHI: " + ((PhiStatement) s).getPhi().toString(s.getNode().getIR().getSymbolTable());
            case PARAM_CALLEE:
                return /* sdg.getNumber(s) + "_" + */"PARAM_CALLEE: " + ((ParamCallee) s).getValueNumber() + " " + s.getNode().getMethod().getSignature();
            case PARAM_CALLER:
                return /* sdg.getNumber(s) + "_" + */"PARAM_CALLER: " + ((ParamCaller) s).getValueNumber() + " " + ((ParamCaller) s).getInstruction();
            case HEAP_PARAM_CALLER:
                return /* sdg.getNumber(s) + "_" + */"HEAP_PARAM_CALLER: " + ((HeapStatement.HeapParamCaller) s).getCall().getDeclaredTarget().getSignature();
            case HEAP_PARAM_CALLEE:
                return /* sdg.getNumber(s) + "_" + */"HEAP_PARAM_CALLEE: " + ((HeapStatement.HeapParamCallee) s).getLocation();
            case METHOD_ENTRY:
                return /* sdg.getNumber(s) + "_" + */"METHOD_ENTRY: " + s.getNode().getMethod().getSignature();
            case METHOD_EXIT:
                return /* sdg.getNumber(s) + "_" + */"METHOD_EXIT: " + s.getNode().getMethod().getSignature();
            case HEAP_RET_CALLEE:
                return /* sdg.getNumber(s) + "_" + */"HEAP_RET_CALLEE: " + ((HeapStatement.HeapReturnCallee) s).getLocation();
            case HEAP_RET_CALLER:
                return /* sdg.getNumber(s) + "_" + */"HEAP_RET_CALLER: " + ((HeapStatement.HeapReturnCaller) s).getCall().getDeclaredTarget().getSignature();
            case EXC_RET_CALLEE:
                return /* sdg.getNumber(s) + "_" + */"EXC_RET_CALLEE: " + s.getNode().getMethod().getSignature();
            case EXC_RET_CALLER:
                return /* sdg.getNumber(s) + "_" + */"EXC_RET_CALLER: " + s.getNode().getMethod().getSignature();

        }
        return s.toString();
    }
}

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.graph.traverse.BFSPathFinder;
import com.ibm.wala.util.io.FileProvider;

import java.io.File;
import java.net.URL;
import java.util.*;

public class LiveExampleL15 {
    /**
     * True if the IClass is under the application-scope ({@code ClassLoaderReference.Application}).
     *
     * @param iClass
     * @return
     */
    public static boolean isApplicationScope(IClass iClass) {
        return iClass != null && iClass.getClassLoader().getReference().equals(ClassLoaderReference.Application);
    }

    public static void main(String[] args) throws Exception {
        // build the analysis scope
        File exFile = new FileProvider().getFile("Java60RegressionExclusions.txt");
        URL resource = SDGExample.class.getResource("Example4.jar");
        AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(resource.getPath(), exFile);
        String runtimeClasses = SDGExample.class.getResource("jdk-17.0.1/rt.jar").getPath();
        AnalysisScopeReader.addClassPathToScope(runtimeClasses, scope, ClassLoaderReference.Primordial);

        // Compute a 1-CFA call graph
        IClassHierarchy classHierarchy = ClassHierarchyFactory.make(scope);
        AnalysisOptions options = new AnalysisOptions();
        options.setEntrypoints(Util.makeMainEntrypoints(scope, classHierarchy));
        SSAPropagationCallGraphBuilder builder = Util.makeNCFABuilder(1, options, new AnalysisCacheImpl(), classHierarchy, scope);
        CallGraph callGraph = builder.makeCallGraph(options);


        // TODO: compute the program's SDG
        SDG sdg = new SDG(callGraph, builder.getPointerAnalysis(), Slicer.DataDependenceOptions.NO_BASE_NO_HEAP, Slicer.ControlDependenceOptions.FULL);
        //If you want to visualize, un-comment line below
        // new WalaViewer(callGraph, builder.getPointerAnalysis());


        // TODO: find sources and sinks
        Collection<Statement> sources = findSources(sdg);
        Collection<Statement> sinks = findSinks(sdg);

        // TODO: slice the SDG
        Set<Statement> slice = new HashSet<>(Slicer.computeBackwardSlice(sdg, sinks));
        Graph slicedSdg = GraphSlicer.prune(sdg, s -> slice.contains(s));

        // TODO: find vulnerable paths
        Set<List<Statement>> vulnerablePaths = getVulnerablePaths(slicedSdg, new HashSet<>(sources), new HashSet<>(sinks));

        for (List<Statement> vulnerablePath : vulnerablePaths) {
            System.out.println("PATH");
            for (Statement statement : vulnerablePath) {
                System.out.println("\t"+statement);
                System.out.println("\t\tSTATEMENT IN LINE " + getLineNumber(statement));
            }

        }

    }

    public static int getLineNumber(Statement s) {
        if (s.getKind() == Statement.Kind.NORMAL) { // ignore special kinds of statements
            int bcIndex, instructionIndex = ((NormalStatement) s).getInstructionIndex();
            try {
                bcIndex = ((ShrikeBTMethod) s.getNode().getMethod()).getBytecodeIndex(instructionIndex);
                try {
                    int src_line_number = s.getNode().getMethod().getLineNumber(bcIndex);
                    return src_line_number;
                } catch (Exception e) {
                    System.err.println("Bytecode index no good");
                    System.err.println(e.getMessage());
                }
            } catch (Exception e) {
                System.err.println("it's probably not a BT method (e.g. it's a fakeroot method)");
                System.err.println(e.getMessage());
            }
        }
        return -1;
    }

    public static Set<Statement> findSources(SDG<InstanceKey> sdg) {

        Set<Statement> result = new HashSet<>();
        // TODO: find all statements that load program arguments
        Collection<CGNode> entrypointNodes = sdg.getCallGraph().getEntrypointNodes();

        for (CGNode entrypointNode : entrypointNodes) {
            IR ir = entrypointNode.getIR();
            Iterator<SSAInstruction> iterator = ir.iterateAllInstructions();
            while (iterator.hasNext()) {
                SSAInstruction instruction = iterator.next();
                if (instruction instanceof SSAArrayLoadInstruction) {
                    int usedVar = ((SSAArrayLoadInstruction) instruction).getUse(0);
                    if (usedVar == 1) // program args?
                        result.add(new NormalStatement(entrypointNode, instruction.iIndex()));
                }
            }
        }

        return result;
    }


    public static Set<Statement> findSinks(SDG<InstanceKey> sdg) {
        Set<Statement> result = new HashSet<>();
        // TODO: find all statements that invoke Runtime.exec()
        for (Statement statement : sdg) {
            // is statements in application scope?
            if (statement.getNode().getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application)) {
                if (statement.getKind().equals(Statement.Kind.NORMAL)) {
                    SSAInstruction instruction = ((NormalStatement) statement).getInstruction();
                    if (instruction instanceof SSAAbstractInvokeInstruction) {
                        MethodReference declaredTarget = ((SSAAbstractInvokeInstruction) instruction).getDeclaredTarget();
                        String signature = declaredTarget.getSignature();
                        if (signature.equals("java.lang.Runtime.exec(Ljava/lang/String;)Ljava/lang/Process;"))
                            result.add(statement);
                    }
                }

            }

        }
        return result;
    }


    public static Set<List<Statement>> getVulnerablePaths(Graph<Statement> g, Set<Statement> sources, Set<Statement> sinks) {
        Set<List<Statement>> result = HashSetFactory.make();
        // TODO: use BFS finder to compute vulnerable paths

        for (Statement statement : g) {
            if (sources.contains(statement)) {
                for (Statement sink : sinks) {
                    BFSPathFinder<Statement> pathFinder = new BFSPathFinder<>(g, statement, sink);
                    List<Statement> path;
                    while ((path = pathFinder.find()) != null)
                        result.add(path);
                }
            }
        }
        return result;
    }
}

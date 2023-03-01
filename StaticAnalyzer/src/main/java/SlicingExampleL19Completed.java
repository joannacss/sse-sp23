import com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.classLoader.ShrikeCTMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.*;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.graph.traverse.BFSPathFinder;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.Atom;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.ibm.wala.types.TypeReference.findOrCreate;

public class SlicingExampleL19Completed {

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
        File exFile = new FileProvider().getFile("Java60RegressionExclusions.txt");

        URL resource = LiveExampleL19Completed.class.getResource("Example4.jar");
        AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(resource.getPath(), exFile);
        String runtimeClasses = LiveExampleL19Completed.class.getResource("jdk-17.0.1/rt.jar").getPath();
        AnalysisScopeReader.addClassPathToScope(runtimeClasses, scope, ClassLoaderReference.Primordial);

        IClassHierarchy classHierarchy = ClassHierarchyFactory.make(scope);

        AnalysisOptions options = new AnalysisOptions();
        options.setEntrypoints(Util.makeMainEntrypoints(scope, classHierarchy));
        SSAPropagationCallGraphBuilder builder = Util.makeNCFABuilder(1, options, new AnalysisCacheImpl(), classHierarchy, scope);
        CallGraph callGraph = builder.makeCallGraph(options);

        PointerAnalysis<InstanceKey> pa = builder.getPointerAnalysis();


        SDG<InstanceKey> sdg = new SDG(callGraph, pa, Slicer.DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS, Slicer.ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
        Set<Statement> sinks = findSinks(sdg);
        Set<Statement> sources = findSources(sdg);

        Set<List<Statement>> vulnerablePaths = getVulnerablePaths(sdg, sources, sinks);

        for (List<Statement> path : vulnerablePaths) {
            System.out.println("VULNERABLE PATH");
            for (Statement statement : path) {

                if (statement.getKind() == Statement.Kind.NORMAL) {
                    System.out.println("\t" + ((NormalStatement) statement).getInstruction());
                    int instructionIndex = ((NormalStatement) statement).getInstructionIndex();
                    int lineNum = ((ShrikeCTMethod) statement.getNode().getMethod()).getLineNumber(instructionIndex);
                    System.out.println("Source line number = " + lineNum );
                }
            }
            System.out.println("------------------------------");
        }


    }

    public int getLineNumber(Statement s){
        if (s.getKind() == Statement.Kind.NORMAL) { // ignore special kinds of statements
            int bcIndex, instructionIndex = ((NormalStatement) s).getInstructionIndex();
            try {
                bcIndex = ((ShrikeBTMethod) s.getNode().getMethod()).getBytecodeIndex(instructionIndex);
                try {
                    int src_line_number = s.getNode().getMethod().getLineNumber(bcIndex);
                    System.err.println ( "Source line number = " + src_line_number );
                } catch (Exception e) {
                    System.err.println("Bytecode index no good");
                    System.err.println(e.getMessage());
                }
            } catch (Exception e ) {
                System.err.println("it's probably not a BT method (e.g. it's a fakeroot method)");
                System.err.println(e.getMessage());
            }
        }
        return -1;
    }

    private static Set<Statement> findSources(SDG<InstanceKey> sdg) {

        Set<Statement> result = new HashSet<>();
        for (Statement s : sdg) {
            if (s.getKind().equals(Statement.Kind.NORMAL) && isApplicationScope(s.getNode().getMethod().getDeclaringClass())) {
                SSAInstruction instruction = ((NormalStatement) s).getInstruction();
                if (instruction instanceof SSAArrayLoadInstruction) {
                    int varNo = instruction.getUse(0);
                    String method = s.getNode().getMethod().getSelector().toString();
                    if (varNo == 1 && method.equals("main([Ljava/lang/String;)V"))
                        result.add(s);
                }
            }
        }
        return result;
    }


    public static Set<Statement> findSinks(SDG<InstanceKey> sdg) {
        TypeReference JavaLangRuntime =
                findOrCreate(ClassLoaderReference.Application, TypeName.string2TypeName("Ljava/lang/Runtime"));
        MethodReference sinkReference =
                MethodReference.findOrCreate(JavaLangRuntime,
                        Atom.findOrCreateUnicodeAtom("exec"),
                        Descriptor.findOrCreateUTF8("(Ljava/lang/String;)Ljava/lang/Process;"));

        Set<Statement> result = new HashSet<>();
        for (Statement s : sdg) {
            if (s.getKind().equals(Statement.Kind.NORMAL) && isApplicationScope(s.getNode().getMethod().getDeclaringClass())) {
                SSAInstruction instruction = ((NormalStatement) s).getInstruction();
                if (instruction instanceof SSAAbstractInvokeInstruction) {
                    if (((SSAAbstractInvokeInstruction) instruction).getDeclaredTarget().equals(sinkReference))
                        result.add(s);
                }
            }
        }
        return result;
    }


    public static Set<List<Statement>> getVulnerablePaths(SDG<? extends InstanceKey> G, Set<Statement> sources, Set<Statement> sinks) {
        Set<List<Statement>> result = HashSetFactory.make();
        for (Statement src : G) {
            if (sources.contains(src)) {
                for (Statement dst : G) {
                    if (sinks.contains(dst)) {
                        BFSPathFinder<Statement> paths = new BFSPathFinder<>(G, src, dst);
                        List<Statement> path = paths.find();
                        if (path != null) {
                            result.add(path);
                        }
                    }
                }
            }
        }
        return result;
    }
}

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.viz.DotUtil;
import com.ibm.wala.viz.viewer.WalaViewer;

import java.io.File;
import java.net.URL;

import static com.ibm.wala.ipa.slicer.Statement.Kind.NORMAL;

public class SDGExample {

    /**
     * True if the IClass is under the application-scope ({@code ClassLoaderReference.Application}).
     *
     * @param iClass
     * @return
     */
    public static boolean isApplicationScope(IClass iClass) {
        return iClass != null && iClass.getClassLoader().getReference().equals(ClassLoaderReference.Application);
    }

    public static void printAppOnlyClasses(IClassHierarchy cha) {
        for (IClass iClass : cha) {
            if (isApplicationScope(iClass)) System.out.println(iClass);
        }
    }

    /**
     * Return a view of an {@link IClassHierarchy} as a {@link Graph}, with edges from classes to immediate subtypes
     */
    public static Graph<IClass> typeHierarchy2Graph(IClassHierarchy cha) throws WalaException {
        Graph<IClass> result = SlowSparseNumberedGraph.make();
        for (IClass c : cha) {
            if (isApplicationScope(c))
                result.addNode(c);
        }
        for (IClass c : cha) {
            for (IClass x : cha.getImmediateSubclasses(c)) {
                if (isApplicationScope(x) && isApplicationScope(c))
                    result.addEdge(c, x);
            }
            if (c.isInterface()) {
                for (IClass x : cha.getImplementors(c.getReference())) {
                    if (isApplicationScope(x) && isApplicationScope(c))
                        result.addEdge(c, x);
                }
            }
        }
        return result;
    }


    public static void plotClassHierarchy(IClassHierarchy cha) throws WalaException {
        Graph<IClass> chaGraph = typeHierarchy2Graph(cha);
        StringBuilder output = DotUtil.dotOutput(chaGraph, iClass -> iClass.toString(), "Application-only class hierarchy");
        System.out.println(output);
    }


    public static void main(String[] args) throws Exception {
        File exFile = new FileProvider().getFile("Java60RegressionExclusions.txt");

        URL resource = SDGExample.class.getResource("Example1.jar");
        AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(resource.getPath(), exFile);
        String runtimeClasses = SDGExample.class.getResource("jdk-17.0.1/rt.jar").getPath();
        AnalysisScopeReader.addClassPathToScope(runtimeClasses, scope, ClassLoaderReference.Primordial);

        // Class Hierarchy DEMO
        IClassHierarchy classHierarchy = ClassHierarchyFactory.make(scope);
        printAppOnlyClasses(classHierarchy);
        plotClassHierarchy(classHierarchy);

        // Call Graph (1-CFA) DEMO
        AnalysisOptions options = new AnalysisOptions();
        options.setEntrypoints(Util.makeMainEntrypoints(scope, classHierarchy));
        SSAPropagationCallGraphBuilder builder = Util.makeNCFABuilder(1, options, new AnalysisCacheImpl(), classHierarchy, scope);
        CallGraph callGraph = builder.makeCallGraph(options);

        System.out.println("# Nodes " + callGraph.getNumberOfNodes());
        for (CGNode cgNode : callGraph) {
            if(isApplicationScope(cgNode.getMethod().getDeclaringClass()))
                System.out.println(cgNode);
        }
        PointerAnalysis<InstanceKey> pa = builder.getPointerAnalysis();
        new WalaViewer(callGraph, pa);

        // SDG DEMO
        SDG<InstanceKey> sdg = new SDG(callGraph, pa, Slicer.DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS, Slicer.ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
        System.out.println(sdg.getNumberOfNodes());
        for (Statement statement : sdg) {
            if(statement.getKind() == NORMAL && isApplicationScope(statement.getNode().getMethod().getDeclaringClass()))
                System.out.println(statement);
        }



    }
}

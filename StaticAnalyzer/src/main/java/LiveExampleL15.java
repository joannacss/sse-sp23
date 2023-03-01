import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;

import java.io.File;
import java.net.URL;

public class LiveExampleL15 {
    public static void main(String[] args) throws Exception {
        File exFile = new FileProvider().getFile("Java60RegressionExclusions.txt");

        URL resource = LiveExampleL15.class.getResource("Example1.jar");
        AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(resource.getPath(),exFile );
        String runtimeClasses = LiveExampleL15.class.getResource("jdk-17.0.1/rt.jar").getPath();
        AnalysisScopeReader.addClassPathToScope(runtimeClasses, scope, ClassLoaderReference.Primordial);

        IClassHierarchy classHierarchy = ClassHierarchyFactory.make(scope);

        // DEMO: n-CFA
        AnalysisOptions options = new AnalysisOptions();
        options.setEntrypoints(Util.makeMainEntrypoints(scope, classHierarchy));
        SSAPropagationCallGraphBuilder builder = Util.makeNCFABuilder(1, options, new AnalysisCacheImpl(), classHierarchy, scope);
        CallGraph callGraph = builder.makeCallGraph(options);

        System.out.println("# Nodes " + callGraph.getNumberOfNodes());
        PointerAnalysis<InstanceKey> pa = builder.getPointerAnalysis();
        SDG sdg = new SDG(callGraph, pa, Slicer.DataDependenceOptions.NO_BASE_NO_HEAP, Slicer.ControlDependenceOptions.FULL);

        System.out.println(sdg.getNumberOfNodes());

    }
}

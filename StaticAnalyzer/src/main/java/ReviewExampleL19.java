import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.viz.viewer.WalaViewer;

import java.io.File;
import java.net.URL;

public class ReviewExampleL19 {

    public static void main(String[] args) throws Exception {
        File exFile = new FileProvider().getFile("Java60RegressionExclusions.txt");
        URL resource = ReviewExampleL19.class.getResource("Example1.jar");
        AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(resource.getPath(),exFile );
        String runtimeClasses = ReviewExampleL19.class.getResource("jdk-17.0.1/rt.jar").getPath();
        AnalysisScopeReader.addClassPathToScope(runtimeClasses, scope, ClassLoaderReference.Primordial);
        IClassHierarchy classHierarchy = ClassHierarchyFactory.make(scope);
        System.out.println(classHierarchy.getNumberOfClasses());
        for (IClass iClass : classHierarchy) {
            if(iClass.getClassLoader().getReference().equals(ClassLoaderReference.Application))
            System.out.println(iClass);
        }

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
//        new WalaViewer(callGraph, pa);

        SDG sdg = new SDG(callGraph, pa, Slicer.DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS, Slicer.ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);

    }

    private static boolean isApplicationScope(IClass declaringClass) {
        return declaringClass.getClassLoader().getReference().equals(ClassLoaderReference.Application);
    }
}

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;

import java.io.File;
import java.util.Collection;
import java.util.jar.JarFile;
/**
 * Example coded in class - Lecture 14.
 * Walked through  how to create RTA and n_CFA call graphs.
 * Also demonstrate how to create an SDG.
 *
 * @author Joanna C. S. Santos
 */
public class LiveExampleL14 {
    public static void main(String[] args) throws Exception {
        String cp = LiveExampleL14.class.getResource("Example1.jar").getPath();
        String exFilePath = LiveExampleL14.class.getResource("Java60RegressionExclusions.txt").getPath();
        File exFile = new FileProvider().getFile(exFilePath);
        AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(cp, exFile);
        JarFile jarFile = new JarFile(LiveExampleL14.class.getResource("jdk-17.0.1/rt.jar").getPath());
        scope.addToScope(ClassLoaderReference.Primordial, jarFile);

        IClassHierarchy ch = ClassHierarchyFactory.make(scope);
        Iterable<Entrypoint> entrypoints = Util.makeMainEntrypoints(scope, ch);


        // CHA Call Graph
        CHACallGraph chaCG = new CHACallGraph(ch, false);
        chaCG.init(entrypoints);

        // TODO:
        // 1. build RTA call graph
        AnalysisOptions options = new AnalysisOptions(scope, entrypoints);
        IAnalysisCacheView cache = new AnalysisCacheImpl();
        CallGraphBuilder<InstanceKey> rtaBuilder = Util.makeRTABuilder(options, cache, ch, scope);
        CallGraph rtaCG = rtaBuilder.makeCallGraph(options, null);

        // 2. print the IR for the main method
        Collection<CGNode> entrypointNodes = rtaCG.getEntrypointNodes();
        CGNode mainNode = entrypointNodes.iterator().next();
        TypeReference typeRef = TypeReference.findOrCreate(ClassLoaderReference.Application, "LExample1");
        IClass mainClass = ch.lookupClass(typeRef);
        IMethod iMethod = mainClass.getMethod(Selector.make("main([Ljava/lang/String;)V"));
        CGNode node = rtaCG.getNode(iMethod, Everywhere.EVERYWHERE);

        IR ir = node.getIR();
        System.out.println(ir);


        // 3. build 1-CFA call graph
        SSAPropagationCallGraphBuilder builder = Util.makeNCFABuilder(1, options, cache, ch, scope);
        CallGraph oneCfaCG = builder.makeCallGraph(options, null);

        // 4. print the number of nodes and edges and compare with CHA, RTA call graph
        System.out.println("1-CFA\n" + CallGraphStats.getStats(oneCfaCG));

        // 5. build an SDG from 1-CFA call graph
        SDG sdg = new SDG(oneCfaCG, builder.getPointerAnalysis(), Slicer.DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS, Slicer.ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
        System.out.println(sdg.getNumberOfNodes());
    }
}

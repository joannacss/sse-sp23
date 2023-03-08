import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.PDG;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;
import viz.GraphVisualizer;
import viz.StatementEdgeHighlighter;
import viz.StatementNodeHighlighter;
import viz.StatementNodeLabeller;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CallGraphExample {

    /**
     * @param projectJar JAR of the project to be analyzed
     * @return the {@link AnalysisScope} object indicating what classes are available in the class path
     * @throws URISyntaxException
     */
    public static AnalysisScope createScope(String projectJar) throws URISyntaxException, IOException {
        URL jreUrl = CallGraphExample.class.getResource("jdk-17.0.1/rt.jar");
        File exFile = new FileProvider().getFile("Java60RegressionExclusions.txt");

        AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(projectJar, exFile);
        AnalysisScopeReader.addClassPathToScope(jreUrl.getPath(), scope, ClassLoaderReference.Primordial);
        return scope;
    }

    public CallGraph buildChaCallGraph(AnalysisScope scope, IClassHierarchy classHierarchy) throws CancelException {
        CHACallGraph cg = new CHACallGraph(classHierarchy, true);
        Iterable<Entrypoint> entrypoints = Util.makeMainEntrypoints(scope, classHierarchy);
        cg.init(entrypoints);
        return cg;
    }

    public static CallGraph buildRtaCallGraph(AnalysisScope scope, IClassHierarchy classHierarchy) throws CallGraphBuilderCancelException {
        AnalysisOptions options = new AnalysisOptions();
        Iterable<Entrypoint> entrypoints = Util.makeMainEntrypoints(scope, classHierarchy);
        options.setEntrypoints(entrypoints);
        AnalysisCache analysisCache = new AnalysisCacheImpl();
        return Util.makeRTABuilder(options, analysisCache, classHierarchy, scope).makeCallGraph(options, null);
    }

    public static CallGraph buildNCfaCallGraph(AnalysisScope scope, IClassHierarchy classHierarchy, int n) throws CallGraphBuilderCancelException {
        AnalysisOptions options = new AnalysisOptions();
        Iterable<Entrypoint> entrypoints = Util.makeMainEntrypoints(scope, classHierarchy);
        options.setEntrypoints(entrypoints);
        AnalysisCache analysisCache = new AnalysisCacheImpl();
        return Util.makeNCFABuilder(n, options, analysisCache, classHierarchy, scope).makeCallGraph(options, null);
    }


    public static void main(String[] args) throws IOException, WalaException, URISyntaxException, CancelException {
        // creates an analysis scope
        String jarFilename = "Example5.jar";
        AnalysisScope scope = createScope(CallGraphExample.class.getResource(jarFilename).getPath());
        // build the class hierarchy
        IClassHierarchy cha = ClassHierarchyFactory.make(scope);

        CallGraph cg = buildNCfaCallGraph(scope, cha, 1);
        AnalysisOptions options = new AnalysisOptions();
        Iterable<Entrypoint> entrypoints = Util.makeMainEntrypoints(scope, cha);
        options.setEntrypoints(entrypoints);
        AnalysisCache analysisCache = new AnalysisCacheImpl();
        SSAPropagationCallGraphBuilder builder = Util.makeNCFABuilder(1, options, analysisCache, cha, scope);
        builder.makeCallGraph(options, null);
        PointerAnalysis<InstanceKey> pa = builder.getPointerAnalysis();
        SDG sdg = new SDG(cg, pa, Slicer.DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS, Slicer.ControlDependenceOptions.FULL);

        // compute the PDG of the main method, and store the visualization in DOT format
        CGNode mainNode = cg.getEntrypointNodes().iterator().next();
        PDG pdg = sdg.getPDG(mainNode);
        String dotFilename = String.format("pdg-%s-%s.dot", jarFilename.replace(".jar", ""), mainNode.getMethod().getName());

        Set<Statement> sinks = TaintAnalysisExample.findSinks(sdg);

        Collection<Statement> slice = Slicer.computeBackwardSlice(sdg, sinks);
        GraphVisualizer<Statement> visualizer = new GraphVisualizer<>(dotFilename, new StatementNodeLabeller(sdg), new StatementNodeHighlighter(new HashSet<>(slice)), new StatementEdgeHighlighter(sdg), null);
        visualizer.generateVisualGraph(pdg, new File(dotFilename));
    }
}



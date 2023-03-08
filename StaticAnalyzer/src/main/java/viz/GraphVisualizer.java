package viz;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SyntheticMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallString;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallStringContext;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallStringContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallerSiteContext;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.graph.labeled.LabeledGraph;
import com.ibm.wala.util.graph.traverse.DFS;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;

/**
 * This is a utility class for visualizing graphs as a DOT file or PDF file.
 * <p>
 * It requires that the machine has the DOT utility available in the PATH.
 * </p>
 *
 * @param <T> the type of the nodes in the graph
 * @author Joanna C. S. Santos (jds5109@rit.edu)
 */
public class GraphVisualizer<T> {

    private static final String DOT_EXE = "dot";
    private static final String COMMAND_FORMAT = DOT_EXE + " -T%s -o%s %s";

    // graph'to configuration
    private final String title;
    private final NodeHighlighter nodeHighliter;
    private final NodeRemover nodeRemover;
    private final EdgeHighlighter edgeHighliter;
    private final NodeLabeller nodeLabeller;

    //    private static final char LABEL_CHAR_BEGIN = '"';
//    private static final char LABEL_CHAR_END = '"';
    private static final char LABEL_CHAR_BEGIN = '<';
    private static final char LABEL_CHAR_END = '>';

    /**
     * @param title         the title of the DOT graph
     * @param nodeLabeller  used for giving labels to nodes
     * @param nodeHighliter provides styling for nodes (i.e., it populates the attributes for the node)
     * @param edgeHighliter provides styling for edges (i.e., it populates the attributes for the edges)
     * @param nodeRemover   used for commenting out some nodes
     */
    public GraphVisualizer(String title, NodeLabeller nodeLabeller, NodeHighlighter nodeHighliter, EdgeHighlighter edgeHighliter, NodeRemover nodeRemover) {
        if (nodeLabeller == null) {
            throw new IllegalArgumentException("Parameter " + NodeLabeller.class.getName() + " can't be null");
        }
        this.title = title;
        this.nodeHighliter = nodeHighliter;
        this.edgeHighliter = edgeHighliter;
        this.nodeLabeller = nodeLabeller;
        this.nodeRemover = nodeRemover;
    }

    /**
     * @param nodeLabeller used for giving labels to nodes
     */
    public GraphVisualizer(NodeLabeller nodeLabeller) {
        this(null, nodeLabeller, null, null, null);
    }

    /**
     * Generates a DOT file of the graph visualization of the Graph AS IS.
     *
     * @param graph
     * @param dotFile path indicating where to save the dot file
     */
    public void generateVisualGraph(Graph<T> graph, File dotFile) throws IllegalArgumentException {
        try (FileWriter fw = new FileWriter(dotFile)) {
            fw.write(graph2Dot(graph));
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }


    public String graph2Dot(Graph<T> g) {
        NumberedGraph nGraph = (NumberedGraph) g;
        StringBuilder dotStringBuffer = new StringBuilder();
        dotStringBuffer.append("digraph G {\n");
        dotStringBuffer.append("\trankdir=LR\n");
        dotStringBuffer.append("\tgraph[label=\"").append(title != null ? title : "").append("\"];\n");
        dotStringBuffer.append("\tnode[style=filled,fillcolor =\"white\",shape=box,margin=0.02,width=0,height=0];\n");

        // prints nodes labels first
        for (T node : g) {
            if (nodeRemover == null || !nodeRemover.isIrrelevantNode(node)) {
//                dotStringBuffer.append("\t\"").append(nodeLabeller.getLabel(node)).append("\"");
                dotStringBuffer.append("\tN").append(nGraph.getNumber(node))
                        .append("[label=\"").append(nodeLabeller.getLabel(node)).append("\"");
                dotStringBuffer.append(nodeHighliter != null ? "," + nodeHighliter.getAttributes(node).substring(1) : "");
                dotStringBuffer.append(";\n");
            }
        }

        // prints edges
        for (T from : g) {
            g.getSuccNodes(from).forEachRemaining(to -> {
                int numEdges = 1;
                if (g instanceof LabeledGraph) {
                    LabeledGraph labeledG = (LabeledGraph) g;
                    numEdges = labeledG.getEdgeLabels(from, to).size();
                }
                for (int i = 0; i < numEdges; i++) {
//                    dotStringBuffer.append(nodeRemover != null && (nodeRemover.isIrrelevantNode(from) || nodeRemover.isIrrelevantNode(to)) ? "//\t" : "\t");
                    if (nodeRemover == null || (!nodeRemover.isIrrelevantNode(from) && !nodeRemover.isIrrelevantNode(to))) {
                        dotStringBuffer.append("\tN").append(nGraph.getNumber(from));
                        dotStringBuffer.append(" -> ");
                        dotStringBuffer.append("N").append(nGraph.getNumber(to));
                        dotStringBuffer.append(edgeHighliter != null ? "[" + edgeHighliter.getAttributes(from, to) + "]" : "");
                        dotStringBuffer.append(";\n");
                    }
                }
            });
        }
        dotStringBuffer.append("}\n");
        return dotStringBuffer.toString();
    }


    private static void runCmd(String cmd) throws IOException, InterruptedException {
        Process exec = Runtime.getRuntime().exec(cmd);
        exec.waitFor();
        InputStream errorStream = exec.getErrorStream();
        StringBuilder error = new StringBuilder();
        while (errorStream.available() > 0) {
            error.append((char) errorStream.read());
        }
        if (error.length() > 0) {
            throw new IOException(error.toString());
        }
    }

    /**
     * Converts a dot file into PDF by using the underlying dot command
     *
     * @param dotFile the path to the dot file that shall be converted
     * @throws IOException                    in case of issues in the conversion
     * @throws java.lang.InterruptedException
     */
    public static void saveDotAsPDF(String dotFile) throws IOException, InterruptedException {
        String pdfFile = dotFile.replace(".dot", ".pdf");
        String cmd = String.format(COMMAND_FORMAT, "pdf", pdfFile, dotFile);
        runCmd(cmd);
        System.out.println("Saved as PDF at " + pdfFile);
    }

    /**
     * Converts a dot file into PNG by using the underlying dot command
     *
     * @param dotFile the path to the dot file that shall be converted
     * @throws IOException                    in case of issues in the conversion
     * @throws java.lang.InterruptedException
     */
    public static void saveDotAsPNG(String dotFile) throws IOException, InterruptedException {
        String pngFileName = dotFile.replace(".dot", ".png");
        String cmd = String.format(COMMAND_FORMAT, "png", pngFileName, dotFile);
        runCmd(cmd);
        System.out.println("Saved as PNG at " + pngFileName);
    }

    /**
     * Interface to be implemented to provide a series of attributes to statement nodes.
     *
     * @param <T> node type
     */
    public interface NodeHighlighter<T> {
        String getAttributes(T s);
    }

    /**
     * Interface to be implemented to provide attributes to edges.
     *
     * @param <T> node type
     */
    public interface EdgeHighlighter<T> {
        String getAttributes(T from, T to);
    }

    /**
     * Interface to be implemented to provide attributes to edges.
     *
     * @param <T> node type
     */
    public interface NodeLabeller<T> {
        String getLabel(T s);
    }

    /**
     * Used to comment-out some nodes from the generated DOT
     *
     * @param <T> node type
     */
    public interface NodeRemover<T> {

        /**
         * Checks whether a statement should be commented out from the generated DOT.
         *
         * @param s statement to be verified.
         * @return true if the statement should be commented out from the generated DOT.
         */
        public boolean isIrrelevantNode(T s);
    }

    public static NodeHighlighter<CGNode> getDefaultCgNodeHighlighter() {
        return (NodeHighlighter) n
                -> ((CGNode) n).getMethod() instanceof SyntheticMethod ?
                "[fillcolor=none,color=gray,shape=rectangle,style=dashed]" :
                isPrimordialScope((CGNode) n) ? "[fillcolor=peachpuff,color=salmon2]" // primordial
                        : (isApplicationScope((CGNode) n) ? "[fillcolor=palegreen,color=darkseagreen]" // application
                        : "[fillcolor=grey93,color=grey33]" // extension
                );
    }

    public static NodeLabeller<CGNode> getDefaultCgNodeLabeller() {
        return (NodeLabeller) cgNode ->
                String.format(
                        "[N%d] %s\\n<FONT POINT-SIZE=\"10pt\">%s</FONT>",
                        ((CGNode) cgNode).getGraphNodeId(),
                        methodToString(((CGNode) cgNode).getMethod().getReference()),
                        contextToString(((CGNode) cgNode).getContext())
                );
    }


    public static NodeRemover<CGNode> getDefaultCgNodeRemover(CallGraph cg) {
        Set<CGNode> reachableNodes = DFS.getReachableNodes(cg, cg.getEntrypointNodes());
        Set<Integer> primordialSuccessors = new HashSet<>();

        reachableNodes.forEach(cgNode -> {
            if (isApplicationScope(cgNode)) {
                cg.getSuccNodes(cgNode).forEachRemaining(succNode -> {
                    if (isPrimordialScope(succNode)) {
                        primordialSuccessors.add(succNode.getGraphNodeId());
                    }
                });
            }
        });
        // includes primordial nodes only if they'e predecessed by application-scope nodes
        return (NodeRemover) n
                -> isPrimordialScope((CGNode) n)
                && !primordialSuccessors.contains(((CGNode) n).getGraphNodeId()
        );
    }

    private static boolean isPrimordialScope(CGNode n) {
        return n.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Primordial);
    }

    private static boolean isApplicationScope(CGNode n) {
        return n.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application);
    }

    /**
     * Converts a MethodReference to a more concise string.
     *
     * @param declaredTarget method reference for a target method call
     * @return a concise (cleaner) string for it
     */
    public static String methodToString(MethodReference declaredTarget) {
        StringBuilder parameters = new StringBuilder();
        for (int i = 0; i < declaredTarget.getNumberOfParameters(); i++) {
            TypeReference parameterType = declaredTarget.getParameterType(i);
            if (parameterType.isPrimitiveType()) {
                if (parameterType.equals(TypeReference.Int))
                    parameters.append("int");
                else if (parameterType.equals(TypeReference.Float))
                    parameters.append("float");
                else if (parameterType.equals(TypeReference.Boolean))
                    parameters.append("bool");
                else if (parameterType.equals(TypeReference.Short))
                    parameters.append("short");
                else if (parameterType.equals(TypeReference.Void))
                    parameters.append("void");
                else if (parameterType.equals(TypeReference.Double))
                    parameters.append("double");
                else if (parameterType.equals(TypeReference.Char))
                    parameters.append("char");
                else parameters.append(parameterType.getName().getClassName());
            } else if (parameterType.isClassType())
                parameters.append(parameterType.getName().getClassName());
            else if (parameterType.isArrayType())
                parameters.append(parameterType.getInnermostElementType().getName().getClassName())
                        .append(StringUtils.repeat("[]", parameterType.getDimensionality()));
            if (i < declaredTarget.getNumberOfParameters() - 1)
                parameters.append(",");
        }

        String classname = declaredTarget.getDeclaringClass().getName().getClassName().toString();
        String methodName = declaredTarget.getName().toString();

        return format("%s.%s(%s)", classname, methodName, parameters);
    }

    public static String contextToString(Context context) {
        if (context instanceof Everywhere) return "Context: Ø";
        if (context instanceof CallerSiteContext) {
            CallerSiteContext callerSiteContext = (CallerSiteContext) context;
            return "CallSiteContext: N" + callerSiteContext.getCaller().getGraphNodeId() + " @ " + callerSiteContext.getCallSite().getProgramCounter();
        }
        if (context instanceof CallStringContext) {
            CallString callString = (CallString) ((CallStringContext) context).get(CallStringContextSelector.CALL_STRING);
            StringBuilder str = new StringBuilder("[");
            CallSiteReference[] sites = callString.getCallSiteRefs();
            IMethod[] methods = callString.getMethods();
            for (int i = 0; i < sites.length; i++) {
                str.append(' ')
                        .append(methodToString(methods[i].getReference()))
                        .append('@')
                        .append(sites[i].getProgramCounter());
            }
            str.append(" ]");
            return "CallStringContext: " + str;
        }

        return context.toString();
    }


}

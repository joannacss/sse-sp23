// javac TaintAnalysisExample1.java --release 8 && jar cvf TaintAnalysisExample1.jar *.class && rm *.class
public class TaintAnalysisExample1 {
    public static void main(String[] args) {
        Triangle t = new Triangle();
        double a = Double.parseDouble(args[0]);
        double b = Double.parseDouble(args[1]);

        double c = t.area(a, b);
        double d = t.area(3, 4);
    }
}

interface OrthogonalShape {
    double area(double a, double b);
}

class Rectangle implements OrthogonalShape {
    public double area(double a, double b) {
        return a * b;
    }
}

class Triangle implements OrthogonalShape {
    public double area(double a, double b) {
        return (a * b) / 2;
    }
}

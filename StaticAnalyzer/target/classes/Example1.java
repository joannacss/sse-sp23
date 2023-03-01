
// javac Example1.java --release 8 && jar cvf Example1.jar *.class && rm *.class
public class Example1 {
    public static void main(String[] args) {
        Shape s;
        // is even?
        if (Integer.parseInt(args[0]) % 2 == 0) {
            s = new Circle();
        } else {
            s = new Rectangle();
        }
        s.draw();
    }
}

interface Shape {
    void draw();
}
class Circle implements Shape{
    public void draw(){
        System.out.println("Draw Circle");
    }
}
class Rectangle implements Shape{
    public void draw(){
        System.out.println("Draw Rectangle");
    }
}

class Triangle implements Shape{
    public void draw(){
        System.out.println("Draw Triangle");
    }
}

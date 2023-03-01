// javac Example3.java --release 8 && jar cvf Example3.jar *.class && rm *.class
class Main{
    public static void main(String[] args) {
        int i = 1;
        while(i < 11){
            i = increment(i);
        }
    }
    public static  int increment(int a){
        return a + 1;
    }
}

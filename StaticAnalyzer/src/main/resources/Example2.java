// javac Example2.java --release 8 && jar cvf Example2.jar *.class && rm *.class
class Main1{
    public static void main(String[] args) {
        int i = 1;
        while(i < 11){
            i++;
        }
    }
}

class Main2{
    public static void main(String[] args) {
        int i = 1;
        int sum = 0;
        while(i < 11){
            i++;
            sum += 1;
        }
    }
}
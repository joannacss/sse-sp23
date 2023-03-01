import java.util.Random;
// javac Example4.java --release 8 && jar cvf Example4.jar *.class && rm *.class
class Example4{
    public static void main(String[] args) throws Exception{
        int i = 1;
        while(i < 11){
            i++;
        }
        System.out.println(i);
        Random r = new Random();

        int randomNumber = r.nextInt();
        String cmd = "ls";
        if(randomNumber % 2 == 0)
            cmd = args[1];

        Runtime rt = Runtime.getRuntime();
        rt.exec(cmd);
    }
}
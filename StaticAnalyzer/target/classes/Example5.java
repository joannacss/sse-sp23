import java.util.Random;
// javac Example5.java --release 8 && jar cvf Example5.jar *.class && rm *.class
class Example5{
    public static void main(String[] args) throws Exception{
//        int i = 1;
//        while(i < 11){
//            i++;
//        }
        String cmd = "ls";
        if(args.length == 1)
            cmd = args[0];
        Runtime rt = Runtime.getRuntime();
        rt.exec(cmd);
    }
}

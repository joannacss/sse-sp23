/*
@description This abstract class is the base for all non-Servlet
AbstractTestCase classes.

*/


public abstract class AbstractTestCaseBase {
    public abstract void runTest(String className);

    /* from a static method like main(), there is not an easy way to get the current
     * classes's name.  We do a trick here to make it work so that we don't have
     * to edit the main for each test case or use a string member to contain the class
     * name
     */
    public static void mainFromParent(String myClassName, AbstractTestCaseBase myObject)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        myObject.runTest(myClassName);
    }
}

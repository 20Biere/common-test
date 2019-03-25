package be.itlive.test.logging;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * This class can be extended in each unit test; it will log the execution of each test like this :
 * [full classpath] : starting test: <methodName>.
 * @author vbiertho
 *
 */
public abstract class AbstractTestLogger {

    @Rule
    public TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(final Description description) {
            System.out.println("[" + description.getClassName() + "]" + " : starting test: " + description.getMethodName());
        }
    };

    /**
     * Init the logger.
     */
    @BeforeClass
    public static void init() {
        org.apache.log4j.BasicConfigurator.configure();
    }
}

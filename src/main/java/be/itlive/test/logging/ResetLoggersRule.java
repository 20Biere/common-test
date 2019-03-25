package be.itlive.test.logging;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

/**
 * @author vbiertho
 */
public class ResetLoggersRule implements MethodRule {

    private SpyLoggersRule classRule;

    /**
     * @param classRule the SpyLogger Class rule.
     */
    public ResetLoggersRule(final SpyLoggersRule classRule) {
        this.classRule = classRule;

    }

    @Override
    public Statement apply(final Statement base, final FrameworkMethod method, final Object target) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                classRule.resetSpies();
                base.evaluate();
            }
        };
    }

}

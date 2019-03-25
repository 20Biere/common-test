package be.itlive.test.persistence;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.junit.rules.TestRule;
import org.junit.runner.Description;

import com.google.common.annotations.Beta;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

/**
 * This rule create mock for UserTransaction and TransactionManager for use with Hibernate.<br/>
 * This allow to start Persistence framework (EntityManagerFactory) with a persistence.xml file configured for JPA.<br/>
 *
 * Typical use is
 * <pre>
 * {@literal @}Rule
 * public {@link MockJTARule} jtaRule = new {@link MockJTARule}();
 *
 * ...
 * {@literal @}Test
 * public void testSomething() {
 *
 *   EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("test", jtaRule.{@link MockJTARule#getEntityManagerParameters() getEntityManagerParameters}());
 * </pre>
 *
 *
 * @author vbiertho
 *
 */
@Beta
public class MockJTARule implements TestRule {

    private UserTransaction userTransactionMock = mock(UserTransaction.class);

    private TransactionManager transactionManagerMock = mock(TransactionManager.class);

    private Transaction transactionMock = mock(Transaction.class);

    static final ThreadLocal<MockJTARule> CONTEXTUAL = new ThreadLocal<>();

    private static final String TRANSACTION_MANAGER_LOOKUP;
    static {
        String transactionManagerLookupClassName = null;
        try {
            // Hibernate 3.X
            ClassPool pool = ClassPool.getDefault();
            pool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
            CtClass ctTransactionManagerLookup = pool.get("org.hibernate.transaction.TransactionManagerLookup");
            CtClass ctTransactionManager = pool.get("javax.transaction.TransactionManager");
            CtClass ctProperties = pool.get("java.util.Properties");
            CtClass ctString = pool.get("java.lang.String");

            CtClass ctMockTransactionManagerLookup = pool.makeClass("be.itlive.test.persistence.MockTransactionManagerLookup");
            ctMockTransactionManagerLookup.addInterface(ctTransactionManagerLookup);

            CtMethod getTransactionManager = new CtMethod(ctTransactionManager, "getTransactionManager", new CtClass[] {ctProperties},
                    ctMockTransactionManagerLookup);
            getTransactionManager.setBody("{ Object o = be.itlive.test.persistence.MockJTARule.CONTEXTUAL.get();"
                    + "return ((be.itlive.test.persistence.MockJTARule) o).getTransactionManagerMock(); }");
            ctMockTransactionManagerLookup.addMethod(getTransactionManager);

            CtMethod getUserTransactionName = new CtMethod(ctString, "getUserTransactionName", new CtClass[] {}, ctMockTransactionManagerLookup);
            getUserTransactionName.setBody("return \"java:comp/UserTransaction\";");
            ctMockTransactionManagerLookup.addMethod(getUserTransactionName);
            transactionManagerLookupClassName = ctMockTransactionManagerLookup.toClass().getCanonicalName();
        } catch (CannotCompileException | NotFoundException e) {
            // Ignore
        }
        TRANSACTION_MANAGER_LOOKUP = transactionManagerLookupClassName;
    }

    public Class<?> firstForNames(final String... classNames) throws ClassNotFoundException {
        for (String className : classNames) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                // Ignore
            }
        }
        throw new ClassNotFoundException(Arrays.toString(classNames));
    }

    /**
     * Provide a Map which can be used to initialize an EntityManagerFactory.
     *
     * @return map with one key "hibernate.transaction.jta.platform" and a {@link org.hibernate.service.jta.platform.internal.AbstractJtaPlatform} instance which return mocks.
     */
    public Map<String, ? extends Object> getEntityManagerParameters()
            throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Map<String, Object> map = new HashMap<>();
        try {
            // Hibernate 4.X
            // Use reflection to not introduce Hibernate dependency into the common-test library.

            Class<?> abstractJtaPlatformClass = firstForNames("org.hibernate.service.jta.platform.internal.AbstractJtaPlatform",
                    "org.hibernate.engine.transaction.jta.platform.internal.AbstractJtaPlatform");
            Method locateUserTransactionMethod = abstractJtaPlatformClass.getDeclaredMethod("locateUserTransaction");
            Method locateTransactionManagerMethod = abstractJtaPlatformClass.getDeclaredMethod("locateTransactionManager");
            locateUserTransactionMethod.setAccessible(true);
            locateTransactionManagerMethod.setAccessible(true);

            Object jtaPlatform = spy(abstractJtaPlatformClass);
            when(locateUserTransactionMethod.invoke(jtaPlatform)).thenReturn(userTransactionMock);
            when(locateTransactionManagerMethod.invoke(jtaPlatform)).thenReturn(transactionManagerMock);

            map.put("hibernate.transaction.jta.platform", jtaPlatform);
        } catch (ClassNotFoundException e) {
            // Ignore
        }

        if (TRANSACTION_MANAGER_LOOKUP != null) {
            // Hibernate 3.X
            map.put("hibernate.transaction.manager_lookup_class", TRANSACTION_MANAGER_LOOKUP);
        }

        return Collections.unmodifiableMap(map);
    }

    @Override
    public org.junit.runners.model.Statement apply(final org.junit.runners.model.Statement base, final Description description) {
        return new org.junit.runners.model.Statement() {

            @Override
            public void evaluate() throws Throwable {
                try {
                    CONTEXTUAL.set(MockJTARule.this);
                    when(transactionManagerMock.getTransaction()).thenReturn(transactionMock);
                    base.evaluate();
                } finally {
                    reset(transactionManagerMock, transactionMock, userTransactionMock);
                    CONTEXTUAL.remove();
                }
            }

        };
    }

    public Transaction getTransactionMock() {
        return transactionMock;
    }

    public TransactionManager getTransactionManagerMock() {
        return transactionManagerMock;
    }

    public UserTransaction getUserTransactionMock() {
        return userTransactionMock;
    }

}

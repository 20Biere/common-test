package be.itlive.test.logging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.internal.util.MockUtil.isSpy;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerFactory;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.RepositorySelector;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/** @author vbiertho */
public class SpyLoggersRule implements TestRule {

  private RepositorySelector selector = mock(RepositorySelector.class);

  private Map<String, Logger> spies = new HashMap<>();

  /**
   * @param name name of the logger
   * @return the spy or a new mock if not null;
   */
  public Logger getSpyLogger(final String name) {
    if (spies.containsKey(name)) {
      return spies.get(name);
    } else {
      return spy(new Logger(name) {});
    }
  }

  /** */
  public void resetSpies() {
    for (Logger spy : spies.values()) {
      reset(spy);
    }
  }

  @Override
  public Statement apply(final Statement base, final Description description) {
    LoggerRepository original = LogManager.getLoggerRepository();
    LoggerRepository repository;
    if (isSpy(original)) {
      Mockito.reset(original);
      repository = original;
    } else {
      repository = spy(original);
    }
    doReturn(repository).when(selector).getLoggerRepository();
    doAnswer(replaceLoggerBySpy())
        .when(repository)
        .getLogger(anyString(), any(LoggerFactory.class));
    LogManager.setRepositorySelector(selector, null);
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        base.evaluate();
      }
    };
  }

  /** @return answer wich spy the result. */
  private Answer<Logger> replaceLoggerBySpy() {
    return new Answer<Logger>() {

      @Override
      public Logger answer(final InvocationOnMock invocation) throws Throwable {
        String category = invocation.getArgument(0);
        if (spies.containsKey(category)) {
          return spies.get(category);
        } else {
          Logger logger = (Logger) invocation.callRealMethod();
          logger = spy(logger);
          spies.put(category, logger);
          return logger;
        }
      }
    };
  }
}

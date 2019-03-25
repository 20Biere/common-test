package be.itlive.test.rest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.net.InetAddress;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import be.itlive.test.logging.AbstractTestLogger;

//@PrepareForTest(AbstractWsTest.class)
//disabled : Power don't work very well, but is the only way to mock Static method.
//Warning: can't make this test work both locally and on Bamboo.
public class AbstractWsTestTest extends AbstractTestLogger {
    private AbstractWsTest implWsTest;

    @Mock
    private InetAddress other;

    @Rule
    public ExpectedException throwed = ExpectedException.none();

    /*@Rule
    public PowerMockRule rule = new PowerMockRule();
    */
    @Before
    public void createImpl() {
        implWsTest = new AbstractWsTest() {

            @Override
            public String getTestDomain() {
                return "Other";
            }
        };
    }

    /*  @Test
    public void testGetDomainOther() throws Exception {
        // Given
        PowerMockito.mockStatic(InetAddress.class);
    
        // When
        when(InetAddress.getLocalHost().getHostName()).thenReturn("SL01501");
    
        //Then
        assertThat(implWsTest.getDomain(), equalTo("Other"));
    
    }
    
    @Test
    public void testGetDomainUnknown() throws UnknownHostException {
        // Given
        PowerMockito.mockStatic(InetAddress.class);
    
        when(InetAddress.getLocalHost()).thenThrow(new UnknownHostException("testing"));
        //    Expect
        throwed.expect(UnknownHostException.class);
        // When
        assertThat(implWsTest.getDomain(), equalTo("Other"));
    }
    */
    /*
    @Test
    public void testGetDomainLocalHost() throws Exception {
        assertThat(implWsTest.getDomain(), equalTo("localhost:8080"));
    }
    */
}

package be.itlive.test.maven;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.net.URL;

import org.hamcrest.core.IsEqual;
import org.junit.Before;
import org.junit.Test;

import be.itlive.common.enums.Env;

public class PomTest {
    private Pom pom;

    @Before
    public void setup() {
        pom = new Pom("./src/test/resources/test-pom.xml");
    }

    @Test
    public void testUrl() throws MalformedURLException {
        Pom remotePom = new Pom(new URL(
                "https://search.maven.org/remotecontent?filepath=org/apache/maven/apache-maven/3.6.0/apache-maven-3.6.0.pom"));
        assertNotNull(remotePom.getModel());

        try {
            new Pom(new URL("https://search.maven.org"));
            fail("should throw RuntimeException : File Not Found");
        } catch (RuntimeException e) {

        }
        System.out.println(remotePom.toString());
    }

    @Test
    public void testPomString() throws Exception {
        try {
            new Pom();
            fail("should throw RuntimeException : File Not Found");
        } catch (RuntimeException e) {

        }
        assertNotNull(pom.getModel());
        assertNotNull(pom.toString());
    }

    @Test
    public void testGetPropertyString() throws Exception {
        assertNull(pom.getProperty("create-distribution-in-dir", "encoding"));
        //assertEquals(pom.getProperty("create-distribution-in-dir", "key"), "value");
    }

    @Test
    public void testGetModel() throws Exception {
        try {
            new Pom("./src/test/resources/bad-test-pom.xml");
            fail("should throw RuntimeException : Bad Xml");
        } catch (RuntimeException e) {
            System.out.println(e);
        }
        assertEquals(pom.getModel().getArtifactId(), "common-test");

    }

    @Test
    public void testGetProperty() throws Exception {
        assertNull(pom.getProperty("XXXX"));
        assertEquals(pom.getProperty("encoding"), "UTF-8");
    }

    @Test
    public void testGetDependency() throws Exception {
        Pom remotePom = new Pom(new URL(
        		"https://search.maven.org/remotecontent?filepath=org/apache/maven/apache-maven/3.6.0/apache-maven-3.6.0.pom"));

        assertEquals(remotePom.getDependency("maven-embedder").getGroupId(), "org.apache.maven");
        assertNull(remotePom.getDependency("dfsdfsqdfqs"));

    }

    /*    @Test
    public void testBuildUrl() throws Exception {
        assertThat(Pom.buildUrl("be.biertho", "common", "1.0.0-SNAPSHOT").toString(),
                IsEqual.equalTo(Pom.REPO + "be/biertho/common/3.0.17/common-3.0.17.pom"));
    }
*/
    @Test
    public void testGAV() throws Exception {
        Pom remotePom = new Pom("org.apache.commons", "commons-lang3", "3.8");
        assertThat(remotePom.getProperty("maven.compiler.source"), is("1.7"));
        try {
            new Pom("@@%%", "null", "}");
            fail("should throw RuntimeException : Bad URL");
        } catch (RuntimeException e) {
            System.out.println(e);
        }

    }

}

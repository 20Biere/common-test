package be.itlive.test.maven;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang3.Validate;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Profile;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import be.itlive.common.enums.Env;

/**
 *
 * A utility to quickly access pom.xml file in unit test (essentially).<br/>
 * By default, the parent pom.xml file is used during initialization.<br/>
 * This is <b>not</b> a full pom resolver. No property replacement is done. Inherited parent pom is not read!<br/>
 * <p>Code examples:
 * <pre>
 * new Pom().getProperty(Env.DEV, "key");
 * new Pom("./../subFolder/custom-pom.xml").getProperty("key");
 * new Pom(URL).getDependency("junit").getVersion();
 * new Pom("be.biertho", "common", "3.0.17")
 * </pre>
 * @author vbiertho
 */
public class Pom implements java.io.Serializable {

    /**
     * Maven Central public group.
     */
    public static final String REPO = "https://repo.maven.apache.org/maven2/";

    private static final long serialVersionUID = 1L;

    private Model model;

    private String pomPath = "./../pom.xml"; //default parent pom.xml for standard ear project.

    /**
     * Default constructor look for parent pom.xml.
     */
    public Pom() {
        loadModel();
    }

    /**
     * @param pomPath target pom to read.
     */
    public Pom(final String pomPath) {
        this.pomPath = pomPath;
        loadModel();
    }

    /**
     * Get the pom.xml file from Nexus using GAV params.
     * @param groupId GroupId
     * @param artifactId ArtifactId
     * @param version Version
     */
    public Pom(final String groupId, final String artifactId, final String version) {
        this(buildUrl(groupId, artifactId, version));

    }

    /**
     * @param url URL.
     */
    public Pom(final URL url) {
        this.pomPath = url.toString();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream()));) {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            this.model = reader.read(r);
        } catch (final IOException | XmlPullParserException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * @param groupId GroupId
     * @param artifactId ArtifactId
     * @param version Version
     * @return a nexus url.
     */
    public static URL buildUrl(final String groupId, final String artifactId, final String version) {
        try {
            return new URL(REPO + groupId.replace(".", "/") + '/' + artifactId + '/' + version + '/' + artifactId + '-' + version + ".pom");
        } catch (final MalformedURLException e) {
            //This can only happen if the protocol is wrong. Here the protocol is in the REPO Static.
            throw new RuntimeException(e);
        }

    }

    /**
     * @param artifactId to get.
     * @return Dependency if found.
     */
    public Dependency getDependency(final String artifactId) {
        Validate.notBlank(artifactId);
        for (Dependency d : model.getDependencies()) {
            if (artifactId.equals(d.getArtifactId())) {
                return d;
            }
        }
        return null;
    }

    /**
     * @param env environment.
     * @return target environment profile or null.
     */
    public Profile getEnvProfile(final Env env) {
        return getProfile(env.getShortName());
    }

    /**
     * @return the model
     */
    public Model getModel() {
        return model;
    }

    /**
     * @param id the profile ID.
     * @return a Profile or Null.
     */
    public Profile getProfile(final String id) {
        Validate.notBlank(id);
        for (Profile p : model.getProfiles()) {
            if (id.equals(p.getId())) {
                return p;
            }
        }
        return null;
    }

    /**
     * @param env environment
     * @param key  property key
     * @return property String value;
     */
    public String getProperty(final Env env, final String key) {
        String result = null;
        ModelBase base = null;
        if (env == null) {
            base = this.model;
        } else {
            base = getEnvProfile(env);
        }
        if (base != null) {
            Object prop = base.getProperties().get(key);
            result = prop != null ? prop.toString() : null;
        }
        return result;

    }
    /**
    * @param env environment String
    * @param key  property key
    * @return property String value;
    */
   public String getProperty(final String profile, final String key) {
       String result = null;
       ModelBase base = null;
       if (profile == null) {
           base = this.model;
       } else {
           base = getProfile(profile);
       }
       if (base != null) {
           Object prop = base.getProperties().get(key);
           result = prop != null ? prop.toString() : null;
       }
       return result;

   }

    
    /**
     * @param key property key
     * @return property String value;
     */
    public String getProperty(final String key) {
        return getProperty((String)null, key);
    }

    /**
     * Load the pom.xml file located under pomPath.
     */
    protected void loadModel() {
        try (Reader r = new FileReader(this.pomPath)) {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            this.model = reader.read(r);
        } catch (final IOException | XmlPullParserException e) {
            throw new RuntimeException(e);
        }
    }

    /** {@inheritDoc}
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Pom [model=" + model + ", pomPath=" + pomPath + "]";
    }

}

package be.itlive.test;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.google.common.collect.Lists;

import be.itlive.test.logging.AbstractTestLogger;

public class FillerUtilTest extends AbstractTestLogger {

    @Test
    public void testGetRandomValue() throws Exception {

        List<Class<?>> check = Lists.newArrayList(int.class, Integer.class, String.class, Date.class, Boolean.class, boolean.class, Double.class,
                double.class, Long.class, long.class, Short.class, short.class, Character.class, char.class, Float.class, float.class, Byte.class,
                byte.class, BigDecimal.class, BigInteger.class, Number.class, TimeUnit.class, Array.class);

        for (Class<?> clazz : check) {
            Object value = FillerUtil.getRandomValue(clazz);
            assertNotNull(value);
            assertTrue("Class " + clazz + " is not of type " + value.getClass(), value.getClass().isAssignableFrom(value.getClass()));
        }
        assertNull(FillerUtil.getRandomValue(null));

        //unsupported type
        assertNull(FillerUtil.getRandomValue(Serializable.class));
        assertNull(FillerUtil.getRandomValue(Deprecated.class));
    }

    @Test
		    public void testFillDepth0() throws Exception {
		        Data data = FillerUtil.fill(Data.class);
		        assertNotNull(data.aInt);
		        assertNotNull(data.aSet);
		        assertNotNull(data.aList);
		        assertNotNull(data.aMap);
		        assertNotNull(data.aString);
		        assertThat(data.aSet, is(empty()));
		    }

    @Test
		    public void testFillNull() throws Exception {
		        assertNull(null);
		    }

    @Test
    public void testExceptionCall() {
        //when

    }

    @Test
		    public void testFillDepth1() throws Exception {
		        Data data = FillerUtil.fill(Data.class, true, 1);
		        assertNotNull(data.aInt);
		        assertNotNull(data.aSet);
		        assertNotNull(data.aList);
		        assertNotNull(data.aMap);
		        assertNotNull(data.aString);
		        assertThat(data.aSet, is(not(empty())));
		        assertThat(data.aList.get(0).aString, is(notNullValue(String.class)));
		    }

    public class Data {
        public int aInt;

        public long aLong;

        public String aString;

        public List<Data> aList;

        public Set<Data> aSet;

        public Map<Integer, Data> aMap;
    }

}

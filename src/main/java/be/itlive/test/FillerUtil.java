package be.itlive.test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.itlive.common.utils.GenericUtils;
import be.itlive.common.utils.ReflectionUtils;

/**
 *
 * {@link FillerUtil} can be used to get an instance of any class you want with the primary fields (int, double,
 * BigInteger, String, Date, ...) fully initialized.
 *
 * @author vbiertho
 */
public final class FillerUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(FillerUtil.class);

	/**
	 * Prevent instantiation.
	 */
	private FillerUtil() {
		super();
	}

	/**
	 * Take a class in parameter and use all the visible setters to fill the Object with a random value depending on the
	 * field's type. The types that are not supported are filled with <code>null</code>. Types supported are the primitives
	 * (int, double, ...), java.math (BigDecimal,...) and java.lang (String, Float, ...) + java.util.Date.
	 * 
	 * @param         <T> The class to fill.
	 * @param inClass The class of the needed object.
	 * @return An instance fully filled with random values.
	 */
	public static <T> T fill(final Class<T> inClass) {
		return fill(inClass, true, 0);
	}

	/**
	 * Take a class in parameter and use all the visible setters to fill the Object with a random value depending on the
	 * field's type. The types that are not supported are filled with <code>null</code>. Types supported are the primitives
	 * (int, double, ...), java.math (BigDecimal,...) and java.lang (String, Float, ...) + java.util.Date.
	 * 
	 * @param                    <T> The class to fill.
	 * @param inClass            The class of the needed object.
	 * @param fieldsNameToIgnore If some fields should not be processed you can exclude them by giving their names.
	 * @return An instance fully filled with random values.
	 */
	public static <T> T fill(final Class<T> inClass, final String... fieldsNameToIgnore) {
		return fill(inClass, true, 0, fieldsNameToIgnore);
	}

	/**
	 * Take a class in parameter and use all the visible setters to fill the Object with a random value depending on the
	 * field's type. The types that are not supported are filled with <code>null</code>. Types supported are the primitives
	 * (int, double, ...), java.math (BigDecimal,...) and java.lang (String, Float, ...) + java.util.Date.
	 * 
	 * @param                    <T> The class to fill.
	 * @param inClass            The class of the needed object.
	 * @param fieldsNameToIgnore If some fields should not be processed you can exclude them by giving their names.
	 * @param setSuperFields     If true the methods of the super class are taken into account.
	 * @param maxDepth           until what depth fields should be instantiate (default=0).
	 * @return An instance fully filled with random values.
	 */
	public static <T> T fill(final Class<T> inClass, final boolean setSuperFields, final int maxDepth, final String... fieldsNameToIgnore) {
		return fill(inClass, setSuperFields, maxDepth, 0, fieldsNameToIgnore);
	}

	/**
	 * Inner private method to control depth.
	 * 
	 * @param                    <T> The class to fill.
	 * @param inClass            The class of the needed object.
	 * @param fieldsNameToIgnore If some fields should not be processed you can exclude them by giving their names.
	 * @param setSuperFields     If true the methods of the super class are taken into account.
	 * @param maxDepth           to what depth field should be instantiate (default=0).
	 * @param currentDepth       current depth (so start =0).
	 * @return An instance fully filled with random values.
	 */
	private static <T> T fill(final Class<T> inClass, final boolean setSuperFields, final int maxDepth, final int currentDepth,
			final String... fieldsNameToIgnore) {
		if (inClass == null) {
			return null;
		}
		T object;
		try {
			object = ReflectionUtils.newInstance(inClass);
		} catch (final InstantiationException e) {
			LOGGER.error("Given class {} can't be properly instantiated", inClass, e);
			throw new RuntimeException(e);
		}
		List<String> ignoredField = Arrays.asList(ArrayUtils.nullToEmpty(fieldsNameToIgnore));

		List<Field> fields = new ArrayList<>();
		for (Field field : ReflectionUtils.collectFields(inClass, Modifier.STATIC + Modifier.PRIVATE, Modifier.STATIC + Modifier.FINAL)) {

			if (!ignoredField.contains(field.getName()) && !field.getName().startsWith("this$")) { // ignore this$ field, the implicit
				// reference created by javac.
				fields.add(field);
			}
		}

		fillFields(object, fields, maxDepth, currentDepth);

		return object;
	}

	/**
	 * Fill a object with random value (see also {@link #getRandomValue(Class)}).
	 * 
	 * @param target       the target object to fill with random value.
	 * @param fields       list of field to handle
	 * @param maxDepth     to what depth field should be instantiate (default=0).
	 * @param currentDepth current depth (so start =0).
	 * @param              <T> object Type.
	 */
	private static <T> void fillFields(final T target, final List<Field> fields, final int maxDepth, final int currentDepth) {
		for (Field field : fields) {
			Class<?> propertyType = field.getType();
			try {
				if (Collection.class.isAssignableFrom(propertyType) || Map.class.isAssignableFrom(propertyType)) {
					// Collection type : Map, Set, List
					Object value = FieldUtils.readField(field, target, true);
					if (value == null || CollectionUtils.sizeIsEmpty(value)) { // skip non empty/null value where type can't be determined.

						if (Map.class.isAssignableFrom(propertyType)) {
							value = getRandomMap(field, maxDepth, currentDepth);
						} else {
							value = getRandomCollection(field, maxDepth, currentDepth);
						}
						FieldUtils.writeField(field, target, value, true);
					}
				} else {
					// basic type.
					Object value = getRandomValue(propertyType);
					if (value != null) {
						FieldUtils.writeField(field, target, value, true);
					}
				}
			} catch (final IllegalAccessException | InstantiationException e) {
				LOGGER.warn("### {}", e.getMessage());
				// process next field.
			}
		}

	}

	/**
	 * Get a random value for a given class. Supported class and range :
	 * <ul>
	 * <li><b>String</b> : random ASCII (between ASCII 32 and ASCII 126) with length 25.</li>
	 * <li><b>java.util.Date</b> : use RandomUtils.nextLong to initialize the millis of the date.</li>
	 * <li><b>boolean</b> and <b>Boolean</b> : true or false</li>
	 * <li><b>int</b> and <b>Integer</b> : between 0 (inclusive) and 2<sup>31</sup>-1</li>
	 * <li><b>double</b> and <b>Double</b> : between 0.0 (inclusive) and 999999.999999</li>
	 * <li><b>short</b> and <b>Short</b> : between 0 (inclusive) and 2<sup>15</sup>-1</li>
	 * <li><b>long</b> and <b>Long</b> : between 0 (inclusive) and nextLong</li>
	 * <li><b>float</b> and <b>Float</b> : between 0.0 (inclusive) and 999999.999999</li>
	 * <li><b>byte</b> and <b>Byte</b> : between 0 (inclusive) and 2<sup>7</sup>-1</li>
	 * <li><b>BigInteger</b> : between 0 (inclusive) and 2<sup>31</sup>-1</li>
	 * <li><b>BigDecimal</b> : between 0.0 (inclusive) and 999999.999999</li>
	 * <li><b>Number</b> : between 0.0 (inclusive) and 2<sup>7</sup>-1</li>
	 * <li><b>char</b> and <b>Character</b> : between ASCII 32 and ASCII 126</li>
	 * <li><b>Enum</b> : one of the Enum type.</li>
	 * </ul>
	 * 
	 * @param inClass The class to fill.
	 * @param         <T> object Type.
	 * @return A random value or null if the class provided is not handled by the method.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getRandomValue(final Class<T> inClass) {
		if (inClass == null) {
			return null;
		}
		Object value = null;

		if (String.class.equals(inClass)) {
			value = RandomStringUtils.randomAscii(25);
		} else if (Date.class.equals(inClass)) {
			value = new Date(RandomUtils.nextLong());
		} else if (Boolean.class.equals(inClass) || boolean.class.equals(inClass)) {
			value = RandomUtils.nextBoolean();
		} else if (Double.class.equals(inClass) || double.class.equals(inClass)) {
			value = RandomUtils.nextDouble() * RandomUtils.nextInt(999999);
		} else if (Integer.class.equals(inClass) || int.class.equals(inClass)) {
			value = RandomUtils.nextInt(Integer.MAX_VALUE);
		} else if (Long.class.equals(inClass) || long.class.equals(inClass)) {
			value = RandomUtils.nextLong();
		} else if (Short.class.equals(inClass) || short.class.equals(inClass)) {
			value = Short.valueOf((short) RandomUtils.nextInt(Short.MAX_VALUE));
		} else if (Character.class.equals(inClass) || char.class.equals(inClass)) {
			value = RandomStringUtils.randomAscii(1).charAt(0);
		} else if (Float.class.equals(inClass) || float.class.equals(inClass)) {
			value = Double.valueOf(RandomUtils.nextDouble() * RandomUtils.nextInt(999999)).floatValue();
		} else if (Byte.class.equals(inClass) || byte.class.equals(inClass)) {
			value = RandomUtils.nextInt(Byte.MAX_VALUE);
		} else if (BigDecimal.class.equals(inClass)) {
			value = BigDecimal.valueOf(RandomUtils.nextDouble() * RandomUtils.nextInt(999999));
		} else if (BigInteger.class.equals(inClass)) {
			value = BigInteger.valueOf(RandomUtils.nextInt(Integer.MAX_VALUE));
		} else if (Number.class.equals(inClass)) {
			value = RandomUtils.nextInt(Byte.MAX_VALUE);
		} else if (Enum.class.isAssignableFrom(inClass)) {
			T[] enumValues = inClass.getEnumConstants();
			value = enumValues[RandomUtils.nextInt(enumValues.length)];
		} else if (inClass.isAnnotation() || inClass.isInterface()) {
			LOGGER.warn("No random value possible for : {}", inClass.toString());
		} else {
			value = fill(inClass, false, 0);

			// empty value
		}
		return (T) value;
	}

	/**
	 * Using the field type, create and fill a collection with random data (see {@link #getRandomValue(Class)})
	 * 
	 * @param field        the collection type field.
	 * @param maxDepth     to what depth field should be instantiate (default=0).
	 * @param currentDepth current depth (so start =0).
	 * @return a new collection
	 * @throws InstantiationException when the field can't be instantiate.
	 */
	@SuppressWarnings("unchecked")
	private static Collection<?> getRandomCollection(final Field field, final int maxDepth, final int currentDepth)
			throws InstantiationException {
		if (!Collection.class.isAssignableFrom(field.getType())) {
			return null;
		}

		Collection<Object> coll = (Collection<Object>) ReflectionUtils.newInstance(field.getType());
		Class<?> genericType = GenericUtils.getGenericCollectionType(field);
		if (genericType != null && currentDepth < maxDepth) {
			for (int i = 0; i < 10; i++) {
				Object instance = fill(genericType, true, maxDepth, currentDepth + 1);
				coll.add(instance);
			}
		}
		return coll;
	}

	/**
	 * Create and initialize a new Map (HashMap)
	 * 
	 * @param field        the map field
	 * @param maxDepth     to what depth field should be instantiate (default=0).
	 * @param currentDepth current depth (so start =0).
	 * @return a new Map
	 * @throws InstantiationException when the field can't be instantiate.
	 */
	private static Map<?, ?> getRandomMap(final Field field, final int maxDepth, final int currentDepth) throws InstantiationException {
		if (!Map.class.isAssignableFrom(field.getType())) {
			return null;
		}

		Map<?, ?> map = (Map<?, ?>) ReflectionUtils.newInstance(field.getType());
		// TODO (hard): find key,value type and instantiate them.
		return map;
	}

}

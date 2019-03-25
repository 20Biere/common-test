package be.itlive.test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.itlive.common.exceptions.ServiceException;
import be.itlive.common.utils.ReflectionUtils;

/**
 *
 * With an instance of {@link AccessorsUtil} you can test if the setters and getters of a class are standard.<br/>
 * In detail : for each primary field of the provided class (int, Double, String - for complete list see
 * {@link FillerUtil#getRandomValue(Class)}) a random value is set and get to determine if the value has not been
 * modified.
 *
 * @since 2.0.1
 * @author vbiertho
 *
 */
public final class AccessorsUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(AccessorsUtil.class);

	/**
	 * Prevent instanciation.
	 */
	private AccessorsUtil() {
		super();
	}

	/**
	 * Get all the fields of the object and compare them with the value set.
	 * 
	 * @param inObject           The object.
	 * @param fields             The object's fields.
	 * @param methods            The object's methods.
	 * @param compareValueLogger If true, we log the values compared.
	 * @param parameters
	 * @throws ServiceException
	 */
	private static void getAllFields(final Object inObject, final List<Field> fields, final Method[] methods,
			final boolean compareValueLogger, final Map<String, Object> parameters) throws ServiceException {
		try {
			for (Field field : fields) {
				String getterName = StringUtils.join("get", StringUtils.capitalize(field.getName()));
				if (field.getType().equals(boolean.class)) {
					getterName = StringUtils.join("is", StringUtils.capitalize(field.getName()));
				}
				for (Method m : methods) {
					if (getterName.equalsIgnoreCase(m.getName())) {
						Object expected = parameters.remove(field.getName());
						Object objectFound = m.invoke(inObject);
						if (compareValueLogger) {
							LOGGER.debug("METHOD <{}> : VALUE SET: {} - VALUE GET : {}", m.getName(), expected, objectFound);
						}
						if (!Objects.equals(expected, objectFound)) {
							throw new ServiceException("For method " + m.getName() + " -- expected <" + expected + "> but was <" + objectFound + ">");
						}
						break;
					}
				}
			}
		} catch (final InvocationTargetException e) {
			throw new ServiceException(e);
		} catch (final IllegalAccessException e) {
			throw new ServiceException(e);
		}
	}

	/**
	 * Set all the fields of the object with random values.
	 * 
	 * @param inObject   The object.
	 * @param fields     The object's fields.
	 * @param methods    The object's methods.
	 * @param parameters
	 * @throws ServiceException
	 */
	private static void setAllFields(final Object inObject, final List<Field> fields, final Method[] methods,
			final Map<String, Object> parameters) throws ServiceException {
		try {
			for (Field field : fields) {
				String setterName = StringUtils.join("set", StringUtils.capitalize(field.getName()));
				for (Method m : methods) {
					if (setterName.equalsIgnoreCase(m.getName())) {
						Class<?>[] classOfParameter = m.getParameterTypes();
						Object parameter = null;
						if (ArrayUtils.getLength(classOfParameter) == 1) {
							parameter = FillerUtil.getRandomValue(classOfParameter[0]);
							parameters.put(field.getName(), parameter);
						}
						m.invoke(inObject, parameter);
						break;
					}
				}
			}
		} catch (final InvocationTargetException e) {
			throw new ServiceException(e);
		} catch (final IllegalAccessException e) {
			throw new ServiceException(e);
		}
	}

	/**
	 * Set all the fields of a given object with random values (null for non trivial type) and get them. If a value that it
	 * get is different that the value set, the method throw a {@link ServiceException}.
	 * 
	 * @param inClass Class to test.
	 * @return True if getters return well the values set.
	 * @throws ServiceException If something goes wrong.
	 */
	public static <T> boolean testAccessors(final Class<T> inClass) throws ServiceException {
		try {
			return testAccessors(inClass.newInstance(), null, false);
		} catch (final InstantiationException | IllegalAccessException e) {
			throw new ServiceException(e);
		}
	}

	/**
	 * Set all the fields of a given object with random values (null for non trivial type) and get them. If a value that it
	 * get is different that the value set, the method throw a {@link ServiceException}.
	 * 
	 * @param inObject Instance of the class to test.
	 * @return True if getters return well the values set.
	 * @throws ServiceException
	 */
	public static boolean testAccessors(final Object inObject) throws ServiceException {
		return testAccessors(inObject, null, false);
	}

	/**
	 * Set all the fields of a given object with random values (null for non trivial type) and get them. If a value that it
	 * get is different that the value set, the method throw a {@link ServiceException}.
	 * 
	 * @param inObject           Instance of the class to test.
	 * @param fieldsNameToIgnore List of fields name to skip (no getter or setter, non significant field, ...). If a field
	 *                           has neither getter nor setter there is no need to specify it.
	 * @return True if getters return well the values set.
	 * @throws ServiceException
	 */
	public static boolean testAccessors(final Object inObject, final List<String> fieldsNameToIgnore) throws ServiceException {
		return testAccessors(inObject, fieldsNameToIgnore, false);
	}

	/**
	 * Set all the fields that have getters & setters of a given object with random values (null for non trivial type) and
	 * get them. If a value that it get is different that the value set, the method throw a {@link ServiceException}.
	 * 
	 * @param inObject           Instance of the class to test.
	 * @param fieldsNameToIgnore List of fields name to skip (no getter or setter, non significant field, ...). If a field
	 *                           has neither getter nor setter there is no need to specify it.
	 * @param compareValueLogger If true, we log the values compared.
	 * @return True if getters return well the values set.
	 * @throws ServiceException
	 */
	public static boolean testAccessors(final Object inObject, final List<String> fieldsNameToIgnore, final boolean compareValueLogger)
			throws ServiceException {
		if (inObject != null) {
			Map<String, Object> parameters = new HashMap<>();
			List<Field> fields = new ArrayList<>();
			for (Field field : ReflectionUtils.collectFields(inObject.getClass())) {
				if ("serialVersionUID".equals(field.getName())) {
					continue;
				}
				if (fieldsNameToIgnore == null || !fieldsNameToIgnore.contains(field.getName())) {
					fields.add(field);
				}
			}
			Method[] methods = inObject.getClass().getDeclaredMethods();
			setAllFields(inObject, fields, methods, parameters);
			getAllFields(inObject, fields, methods, compareValueLogger, parameters);
		}
		return true;
	}
}

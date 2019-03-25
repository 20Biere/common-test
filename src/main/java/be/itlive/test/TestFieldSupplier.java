package be.itlive.test;

import java.lang.reflect.Field;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;

/**
 * A supplier which return the current value of a field of an object.
 *
 * <br />
 * Example of use :
 *
 * <pre>
 * import static be.itlive.test.TestFieldSupplier.field;
 *
 * ...
 *
 * {@literal @}Mock
 * private Connection connection;
 *
 * ...
 *          // Where a Supplier<?> is required you can call
 *         ... field(this, "connection", Connection.class) ...
 * </pre>
 * @author vbiertho
 *
 * @param <T> parametrized type.
 */
public class TestFieldSupplier<T> implements Supplier<T> {

    private Object testClassInstance;

    private Field field;

    private Class<T> fieldType;

    /**
     * @param testClassInstance instance of objet
     * @param fieldName field name
     * @param fieldType type of the field
     */
    public TestFieldSupplier(final Object testClassInstance, final String fieldName, final Class<T> fieldType) {
        this.testClassInstance = testClassInstance;
        this.fieldType = fieldType;
        try {
            field = findDeclaredField(testClassInstance.getClass(), fieldName);
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            if (!fieldType.isAssignableFrom(field.getType())) {
                throw new RuntimeException("The type of the field (" + field.getType() + ") is not assignable to " + fieldType);
            }
        } catch (final NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public T get() {
        try {
            return fieldType.cast(field.get(testClassInstance));
        } catch (final IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException("Can not access to field " + field.getName(), e);
        }
    }

    /**
     * Field the hidden (private, protected) field in a type hierarchy (type and parents)
     * @param type type
     * @param fieldName name of the field
     * @return field.
     * @throws NoSuchFieldException if no field of that name is found
     */
    private static Field findDeclaredField(final Class<?> type, final String fieldName) throws NoSuchFieldException {
        Preconditions.checkNotNull(fieldName);
        Preconditions.checkNotNull(type);
        Class<?> t = type;
        while (t != null) {
            for (Field f : t.getDeclaredFields()) {
                if (f.getName().matches(fieldName)) {
                    return f;
                }
            }
            t = t.getSuperclass();
        }
        throw new NoSuchFieldException("The field '" + fieldName + "' not found in instance of " + type);
    }

    /**
     * @param testClassInstance instance of object
     * @param fieldName name of field to supply
     * @param <T> type of the field
     * @param fieldType type of the field
     * @return supplier
     */
    public static <T> Supplier<T> field(final Object testClassInstance, final String fieldName, final Class<T> fieldType) {
        return new TestFieldSupplier<>(testClassInstance, fieldName, fieldType);
    }

}

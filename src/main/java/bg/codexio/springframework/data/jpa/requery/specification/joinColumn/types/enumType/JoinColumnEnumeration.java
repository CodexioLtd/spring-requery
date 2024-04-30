package bg.codexio.springframework.data.jpa.requery.specification.joinColumn.types.enumType;

import java.util.EnumSet;

/**
 * Represents an enumeration type that is linked to a database column with a
 * primary key.
 * This interface facilitates the mapping between database primary keys and
 * Java enum instances.
 *
 * <p>Each enum implementing this interface is expected to provide a method
 * to get its primary key
 * and a static method to construct an enum instance from a given primary key
 * .</p>
 *
 * @param <PrimaryKey> the type of the primary key associated with the enum
 *                     instances
 * @param <EnumType>   the type of the enum itself, ensuring type safety in
 *                     comparable operations
 */
public interface JoinColumnEnumeration<PrimaryKey, EnumType>
        extends Comparable<EnumType> {

    String PRIMARY_KEY_GET = "getId";

    /**
     * Static method to find an enum instance based on its primary key using
     * reflection. This method assumes all enum constants of type {@code
     * EnumType} implement this interface and have a method named {@code
     * getId} that returns their primary key.
     *
     * @param id       the primary key for which to find the corresponding
     *                 enum instance
     * @param enumType the class of the enum type to search within
     * @param <T>      the type parameter of the enum, ensuring that it
     *                 extends {@code Enum} and implements this interface
     * @return the matching enum instance
     * @throws RuntimeException if the enum instance cannot be found or if a
     *                          reflective operation fails
     */
    static <T extends Enum<T>> JoinColumnEnumeration<?, ?> fromId(
            Object id,
            Class<? extends Enum<?>> enumType
    ) {
        return (JoinColumnEnumeration<?, ?>) EnumSet.allOf((Class<T>) enumType)
                                                    .stream()
                                                    .filter(x -> {
                                                        try {
                                                            return x.getClass()
                                                                    .getMethod(PRIMARY_KEY_GET)
                                                                    .invoke(x)
                                                                    .equals(id);
                                                        } catch (ReflectiveOperationException e) {
                                                            throw new RuntimeException(e);
                                                        }
                                                    })
                                                    .findFirst()
                                                    .orElseThrow();
    }

    PrimaryKey getId();

}
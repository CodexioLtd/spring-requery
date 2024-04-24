package bg.codexio.springframework.data.jpa.requery.specification.joinColumn.types.enumType;

import java.util.EnumSet;

public interface JoinColumnEnumeration<PrimaryKey, EnumType>
        extends Comparable<EnumType> {

    String PRIMARY_KEY_GET = "getId";

    PrimaryKey getId();

    static <T extends Enum<T>> JoinColumnEnumeration<?, ?> fromId(
            Object id,
            Class<? extends Enum<?>> enumType
    ) {
        return (JoinColumnEnumeration<?, ?>) EnumSet.allOf((Class<T>) enumType)
                .stream()
                .filter(x -> {
                    try {
                        return x.getClass().getMethod(PRIMARY_KEY_GET).invoke(x).equals(id);
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException(e);
                    }
                })
                .findFirst()
                .orElseThrow();
    }

}

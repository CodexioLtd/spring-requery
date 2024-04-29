package bg.codexio.springframework.data.jpa.requery.specification.joinColumn.types.enumType;

/**
 * A {@link PrimaryKeyProvider} implementation for converting string
 * representations of primary keys to their corresponding {@code Long} type
 * values.
 *
 * <p>This implementation assumes that the string representation can be
 * directly parsed into a Long, and does not handle formatting or parsing
 * errors beyond those already handled by {@link Long#parseLong(String)}.</p>
 */
public class LongTypePrimaryKeyProvider
        implements PrimaryKeyProvider {

    @Override
    public Object convertPrimaryKeyFromString(String rawPrimaryKey) {
        return Long.parseLong(rawPrimaryKey);
    }
}
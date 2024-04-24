package bg.codexio.springframework.data.jpa.requery.specification.joinColumn.types.enumType;

public class LongTypePrimaryKeyProvider implements PrimaryKeyProvider {

    @Override
    public Object convertPrimaryKeyFromString(String rawPrimaryKey) {
        return Long.parseLong(rawPrimaryKey);
    }
}
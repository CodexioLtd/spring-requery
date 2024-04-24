package bg.codexio.springframework.data.jpa.requery.specification.joinColumn.types.enumType;

public interface PrimaryKeyProvider {

    Object convertPrimaryKeyFromString(String rawPrimaryKey);

}
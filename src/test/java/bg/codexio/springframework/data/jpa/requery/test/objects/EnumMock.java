package bg.codexio.springframework.data.jpa.requery.test.objects;

import bg.codexio.springframework.data.jpa.requery.specification.joinColumn.types.enumType.JoinColumnEnumeration;

public enum EnumMock
        implements JoinColumnEnumeration<Long, EnumMock> {
    ITEM1(1L),
    ITEM2(2L);

    private final Long id;

    EnumMock(Long id) {
        this.id = id;
    }

    public Long getId() {
        return this.id;
    }
}

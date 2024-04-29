package bg.codexio.springframework.data.jpa.requery.resolver.function;

import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CaseInsensitiveLikeSQLFunctionTest {
    @Test
    void render_ShouldAppendCorrectSqlForCaseInsensitiveLike_WhenGivenNodes() {
        var sqlAppender = mock(SqlAppender.class);
        var walker = mock(SqlAstTranslator.class);
        var node1 = mock(SqlAstNode.class);
        var node2 = mock(SqlAstNode.class);
        var returnableType = mock(ReturnableType.class);
        var arguments = List.of(
                node1,
                node2
        );

        var function = new CaseInsensitiveLikeSQLFunction();

        function.render(
                sqlAppender,
                arguments,
                returnableType,
                walker
        );

        var captor = ArgumentCaptor.forClass(String.class);
        verify(
                sqlAppender,
                times(3)
        ).appendSql(captor.capture());
        var allValues = captor.getAllValues();

        assertEquals(
                "(LOWER(",
                allValues.get(0)
        );
        assertEquals(
                ") LIKE LOWER(",
                allValues.get(1)
        );
        assertEquals(
                "))",
                allValues.get(2)
        );

        verify(
                walker,
                times(2)
        ).render(
                any(SqlAstNode.class),
                eq(SqlAstNodeRenderingMode.DEFAULT)
        );
    }
}
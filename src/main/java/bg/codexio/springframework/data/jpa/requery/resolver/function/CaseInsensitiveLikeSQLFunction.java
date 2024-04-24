package bg.codexio.springframework.data.jpa.requery.resolver.function;

import org.hibernate.query.sqm.function.NamedSqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;

import java.util.List;

public class CaseInsensitiveLikeSQLFunction
        extends NamedSqmFunctionDescriptor {

    public static final String FUNC_NAME = "case_insensitive_like";

    public CaseInsensitiveLikeSQLFunction() {
        super(
                CaseInsensitiveLikeSQLFunction.FUNC_NAME,
                true,
                StandardArgumentsValidators.exactly(2),
                null
        );
    }

    public void render(
            SqlAppender sqlAppender,
            List<? extends SqlAstNode> arguments,
            SqlAstTranslator<?> walker
    ) {
        sqlAppender.appendSql("(LOWER(");
        walker.render(
                arguments.get(0),
                SqlAstNodeRenderingMode.DEFAULT
        );
        sqlAppender.appendSql(") LIKE LOWER(");
        walker.render(
                arguments.get(1),
                SqlAstNodeRenderingMode.DEFAULT
        );
        sqlAppender.appendSql("))");
    }
}
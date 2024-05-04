package bg.codexio.springframework.data.jpa.requery.resolver.function;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.function.NamedSqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;

import java.util.List;

/**
 * A SQL function descriptor for Hibernate that renders a case-insensitive
 * "LIKE" SQL operation.
 *
 * <p>This function assumes the SQL dialect supports the LOWER function and
 * that it will be applied
 * to string type columns or values.</p>
 */
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

    /**
     * Renders the SQL for a case-insensitive LIKE operation using the LOWER
     * function on both sides of the LIKE operator.
     *
     * @param sqlAppender the appender to which the SQL is written
     * @param arguments   the arguments of the SQL function, expecting
     *                    exactly two
     * @param walker      the SQL AST translator that handles the rendering
     *                    of {@link SqlAstNode} instances
     */
    public void render(
            SqlAppender sqlAppender,
            List<? extends SqlAstNode> arguments,
            ReturnableType<?> returnType,
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
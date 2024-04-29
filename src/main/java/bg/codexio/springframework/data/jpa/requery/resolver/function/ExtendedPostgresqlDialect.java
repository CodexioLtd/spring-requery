package bg.codexio.springframework.data.jpa.requery.resolver.function;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.PostgreSQLDialect;

/**
 * Extends the standard {@link PostgreSQLDialect} and registers a custom
 * function to support case-insensitive LIKE operations.
 */
public class ExtendedPostgresqlDialect
        extends PostgreSQLDialect {
    @Override
    public void initializeFunctionRegistry(FunctionContributions functionContributions) {
        super.initializeFunctionRegistry(functionContributions);

        functionContributions.getFunctionRegistry()
                             .register(
                                     CaseInsensitiveLikeSQLFunction.FUNC_NAME,
                                     new CaseInsensitiveLikeSQLFunction()
                             );
    }
}
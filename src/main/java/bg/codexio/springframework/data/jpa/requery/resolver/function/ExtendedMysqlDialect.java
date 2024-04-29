package bg.codexio.springframework.data.jpa.requery.resolver.function;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.MySQLDialect;

/**
 * Extends the standard {@link MySQLDialect} and registers a custom function
 * to support case-insensitive LIKE operations.
 */
public class ExtendedMysqlDialect
        extends MySQLDialect {
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
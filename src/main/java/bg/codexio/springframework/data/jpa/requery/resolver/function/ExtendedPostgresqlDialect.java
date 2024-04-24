package bg.codexio.springframework.data.jpa.requery.resolver.function;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.PostgreSQLDialect;

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
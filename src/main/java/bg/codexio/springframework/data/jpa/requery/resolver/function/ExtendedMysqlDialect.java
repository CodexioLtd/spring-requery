package bg.codexio.springframework.data.jpa.requery.resolver.function;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.MySQLDialect;

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
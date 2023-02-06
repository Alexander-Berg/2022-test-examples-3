package ru.yandex.market.loyalty.core.config.validation;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ru.yandex.market.loyalty.core.config.DatasourceType;
import ru.yandex.market.loyalty.test.database.SQLValidator;

import java.util.ArrayList;
import java.util.List;

import static ru.yandex.market.loyalty.core.config.DatasourceType.MANDATORY;
import static ru.yandex.market.loyalty.core.config.DatasourceType.NONE;
import static ru.yandex.market.loyalty.core.config.DatasourceType.READ_WRITE;

public class DatabaseUseSqlValidator implements SQLValidator {
    private static final Logger logger = LogManager.getLogger(DatabaseUseSqlValidator.class);
    private final List<String> failures = new ArrayList<>();

    @Override
    public void validate(String sql) {
        DatasourceType.get().ifPresent(datasourceType -> {
            if (datasourceType == NONE || datasourceType == MANDATORY) {
                addFailure(sql, datasourceType);
            } else if (datasourceType != READ_WRITE) {
                try {
                    Statement stmt = CCJSqlParserUtil.parse(sql);
                    stmt.accept(new StatementVisitorAdapter() {
                        @Override
                        public void visit(Update update) {
                            addFailure(sql, datasourceType);
                        }

                        @Override
                        public void visit(Delete delete) {
                            addFailure(sql, datasourceType);
                        }

                        @Override
                        public void visit(Insert insert) {
                            addFailure(sql, datasourceType);
                        }
                    });
                } catch (JSQLParserException e) {
                    logger.trace("error parsing: {}", sql);
                } catch (UnsupportedOperationException ex) {
                    logger.error("unsupported: {}", ex.getMessage());
                }
            }
        });
    }

    private void addFailure(String sql, DatasourceType datasourceType) {
        failures.add("call " + sql + " but datasourceType is " + datasourceType);
    }

    @Override
    public void startTest() {
        failures.clear();
    }

    @Override
    public List<String> finishTest() {
        List<String> result = new ArrayList<>(failures);
        failures.clear();
        return result;
    }
}

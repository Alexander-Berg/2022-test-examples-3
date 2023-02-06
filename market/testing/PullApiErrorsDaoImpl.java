package ru.yandex.market.api.testing;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang.mutable.MutableInt;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.util.db.DbBulkInserter;
import ru.yandex.market.common.util.db.Object2PreparedStatementSetter;

/**
 * @author kudrale
 */
@SuppressFBWarnings("OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE")
@ParametersAreNonnullByDefault
public class PullApiErrorsDaoImpl implements PullApiErrorsDao {

    private static final int BULK_SIZE = 1000;
    private static final String TABLE = "shops_web.pull_api_errors";
    private static final String INSERT_ERROR_QUERY = "insert into " + TABLE + "(url, num, code) values(?,?,?)";
    private static final String CLEAN_ERRORS_QUERY = "delete from " + TABLE + " where url = ?";
    private static final String SELECT_ERRORS_QUERY =
            "select code from " + TABLE + " t1 where url = ? order by num for update";
    private JdbcTemplate jdbcTemplate;
    private TransactionTemplate transactionTemplate;

    @Override
    public void addErrors(final String path, final List<Integer> errorCodes) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                jdbcTemplate.update(CLEAN_ERRORS_QUERY, path);
                try (DbBulkInserter inserter = new DbBulkInserter<>(jdbcTemplate, INSERT_ERROR_QUERY, BULK_SIZE,
                        new Object2PreparedStatementSetter<Integer>() {
                            int c = 0;

                            @Override
                            public void setValues(Integer o, PreparedStatement ps) throws SQLException {
                                int i = 0;
                                ps.setString(++i, path);
                                ps.setInt(++i, c++);
                                ps.setInt(++i, o);
                            }
                        })) {
                    inserter.process(errorCodes);
                }
            }
        });

    }

    @Nullable
    @Override
    public Integer getAndRemoveError(final String path) {
        final MutableInt error = new MutableInt();
        jdbcTemplate.query(
                con -> {
                    PreparedStatement stmt = con.prepareStatement(SELECT_ERRORS_QUERY,
                            ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                    stmt.setFetchSize(1);
                    stmt.setMaxRows(1);
                    stmt.setString(1, path);
                    return stmt;
                }, rs -> {
                    error.setValue(rs.getInt("code"));
                    rs.deleteRow();
                });
        return error.intValue() != 0 ? error.toInteger() : null;
    }

    @Required
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Required
    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }
}

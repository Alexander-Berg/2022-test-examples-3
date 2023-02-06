package ru.yandex.market.replenishment.autoorder.service.import_pipeline;

import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

public class YtWatchLogSnapshotter {
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public YtWatchLogSnapshotter(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @NonNull
    protected void createYtWatchLogSnapshot() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NotNull TransactionStatus status) {
                jdbcTemplate.execute("select create_tmp_table('yt_table_watch_log');" +
                        "insert into yt_table_watch_log_tmp(id, events_group, cluster, table_path, time_processed, " +
                        "imported) " +
                        "   select * from yt_table_watch_log;");
            }
        });

    }

    protected void restoreYtWatchLogSnapshot() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NotNull TransactionStatus status) {
                jdbcTemplate.execute("select replace_table_with_tmp_table('yt_table_watch_log', null, null);");
            }
        });

    }
}

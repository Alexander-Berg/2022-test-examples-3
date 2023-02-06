package ru.yandex.market.replenishment.autoorder.service.import_pipeline;

import java.util.function.Function;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.EventTriggeredLoader;

public abstract class BaseImportPipelineTest extends FunctionalTest {

    @Autowired
    SqlSessionFactory sqlSessionFactory;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    JdbcTemplate postgreJdbcTemplate;

    protected void load(Function<SqlSession, EventTriggeredLoader> loaderSupplier) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NotNull TransactionStatus status) {
                try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
                    loaderSupplier.apply(session).load();
                    session.commit();
                }
            }
        });
    }

    protected void runAndRestoreYtWatchLog(Runnable runnable) {
        YtWatchLogSnapshotter ytWatchLogSnapshotter = new YtWatchLogSnapshotter(
                postgreJdbcTemplate,
                transactionTemplate);
        ytWatchLogSnapshotter.createYtWatchLogSnapshot();
        runnable.run();
        ytWatchLogSnapshotter.restoreYtWatchLogSnapshot();
    }
}

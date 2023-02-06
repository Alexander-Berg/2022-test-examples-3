package ru.yandex.market.deepmind.tms.executors;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import com.google.common.collect.Iterators;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.mboc.common.infrastructure.util.UnstableInit;
import ru.yandex.market.tms.quartz2.model.VerboseExecutor;

/**
 * Временный класс для проверки скорости скачивания с yt в БД.
 */
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("checkstyle:all")
public class TestDownloadFromYt extends VerboseExecutor {

    private static final int BATCH_SIZE = 5000;
    private final UnstableInit<Yt> ytUnstableInit;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    private volatile Instant startTime;
    private volatile boolean isFinished;

    private volatile int savedCount;
    private volatile int deletedCount;

    private volatile int toSaveCount;
    private volatile int toDeleteCount;

    @Override
    public void doRealJob(JobExecutionContext context) throws Exception {
        try {
            isFinished = false;
            run();
        } catch (Exception e) {
            isFinished = true;
            throw e;
        }
    }

    public void run() {
//        namedParameterJdbcTemplate.getJdbcOperations()
//            .execute("drop table if exists msku.tmp_s_ermakov_service_offers_DEEPMIND_1218");
        namedParameterJdbcTemplate.getJdbcOperations()
            .execute("create table if not exists msku.tmp_s_ermakov_service_offers_DEEPMIND_1218 " +
                "(\n" +
                "    business_id             int     not null,\n" +
                "    supplier_id             int     not null,\n" +
                "    shop_sku                text    not null,\n" +
                "    title                   text    not null,\n" +
                "    last_version            bigint  not null,\n" +
                "    msku_id                 bigint  not null,\n" +
                "    category_id             bigint  not null,\n" +
                "    supplier_type           text    not null,\n" +
                "    bar_code                text,\n" +
                "    vendor_code             text,\n" +
                "    acceptance_status       text,\n" +
                "\n" +
                "    primary key (supplier_id, shop_sku)\n" +
                ");");
        namedParameterJdbcTemplate.getJdbcOperations()
            .execute("create index if not exists supplier_type_indx on msku" +
                ".tmp_s_ermakov_service_offers_DEEPMIND_1218(supplier_type)");

        var yt = ytUnstableInit.get();
        var iterator = yt.tables().read(
            YPath.simple("//home/market/users/s-ermakov/blue_offers_2021_10_18"),
            YTableEntryTypes.YSON
        );

        loggingThread();

        startTime = Instant.now();

        var partitionIterator = Iterators.partition(iterator, BATCH_SIZE);
        while (partitionIterator.hasNext()) {
            var nextList = partitionIterator.next();

            var toDeleteParams = nextList.stream()
                .filter(v -> !hasMskuId(v))
                .map(v -> {
                    var supplier_id = v.getInt("supplier_id");
                    var last_version = v.getLong("last_version");
                    var shop_sku = v.getString("shop_sku");
                    return (SqlParameterSource) new MapSqlParameterSource()
                        .addValue("supplier_id", supplier_id)
                        .addValue("shop_sku", shop_sku)
                        .addValue("last_version", last_version);
                })
                .toArray(SqlParameterSource[]::new);

            var toSaveParams = nextList.stream()
                .map(YTreeNode::mapNode)
                .filter(v -> hasMskuId(v))
                .map(node -> {
                    var business_id = node.getInt("business_id");
                    var supplier_id = node.getInt("supplier_id");
                    var shop_sku = node.getString("shop_sku");
                    var title = node.getString("title");
                    var vendor_code = node.getString("vendor_code");
                    var bar_code = node.getString("bar_code");
                    var category_id = node.getLong("category_id");
                    var acceptance_status = node.getString("acceptance_status");
                    var msku_id = node.getLong("approved_sku_mapping_id");
                    var last_version = node.getLong("last_version");
                    var supplier_type = node.getString("supplier_type");
                    return (SqlParameterSource) new MapSqlParameterSource()
                        .addValue("business_id", business_id)
                        .addValue("supplier_id", supplier_id)
                        .addValue("shop_sku", shop_sku)
                        .addValue("title", title)
                        .addValue("vendor_code", vendor_code)
                        .addValue("bar_code", bar_code)
                        .addValue("category_id", category_id)
                        .addValue("acceptance_status", acceptance_status)
                        .addValue("supplier_type", supplier_type)
                        .addValue("msku_id", msku_id)
                        .addValue("last_version", last_version);
                })
                .toArray(SqlParameterSource[]::new);

            transactionTemplate.execute(new TransactionCallback<Integer>() {
                @Override
                public Integer doInTransaction(TransactionStatus status) {
                    var ints = namedParameterJdbcTemplate.batchUpdate(
                        "insert into msku.tmp_s_ermakov_service_offers_DEEPMIND_1218 as so " +
                            "(business_id, supplier_id, shop_sku, title, vendor_code, bar_code, " +
                            "category_id, acceptance_status, msku_id, last_version, supplier_type)" +
                            "values (:business_id, :supplier_id, :shop_sku, :title, :vendor_code, :bar_code, " +
                            ":category_id, " +
                            ":acceptance_status, :msku_id, :last_version, :supplier_type)" +
                            "on conflict (supplier_id, shop_sku) do update set " +
                            "business_id = excluded.business_id," +
                            "title = excluded.title," +
                            "vendor_code = excluded.vendor_code," +
                            "bar_code = excluded.bar_code," +
                            "category_id = excluded.category_id," +
                            "acceptance_status = excluded.acceptance_status," +
                            "msku_id = excluded.msku_id,\n" +
                            "supplier_type = excluded.supplier_type\n" +
                            "where so.last_version < excluded.last_version",
                        toSaveParams);

                    var delete = namedParameterJdbcTemplate.batchUpdate(
                        "delete from msku.tmp_s_ermakov_service_offers_DEEPMIND_1218 as so " +
                            "where supplier_id = :supplier_id and shop_sku = :shop_sku " +
                            "and last_version < :last_version",
                        toDeleteParams);

                    toSaveCount += toSaveParams.length;
                    toDeleteCount += toDeleteParams.length;
                    savedCount += Arrays.stream(ints).sum();
                    deletedCount += Arrays.stream(delete).sum();
                    return null;
                }
            });
        }
        isFinished = true;
    }

    private Boolean hasMskuId(YTreeMapNode v) {
        return v.getLongO("approved_sku_mapping_id")
            .map(n -> n > 0)
            .orElse(false);
    }

    private Runnable loggingThread() {
        var runnable = new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                while (!isFinished) {
                    Thread.sleep(1000);
                    if (startTime == null) {
                        continue;
                    }

                    var finishTime = Instant.now();
                    var duration = Duration.between(startTime, finishTime);

                    var savedCount = TestDownloadFromYt.this.savedCount;
                    var deletedCount = TestDownloadFromYt.this.deletedCount;
                    var changedCount = savedCount + deletedCount;

                    var toDeleteCount = TestDownloadFromYt.this.toDeleteCount;
                    var toSaveCount = TestDownloadFromYt.this.toSaveCount;
                    var toChangeCount = toSaveCount + toDeleteCount;

                    log.debug("--------------------------------------------");
                    log.debug("Saved: {}, deleted: {}", savedCount, deletedCount);
                    log.debug("Speed is {} offers/secs. Changed {} offers/secs. " +
                            "Total duration: {}, total processed: {}, total changed: {}",
                        toChangeCount / duration.getSeconds(),
                        changedCount / duration.getSeconds(),
                        duration,
                        toChangeCount,
                        changedCount);
                    log.debug("--------------------------------------------");
                }
            }
        };
        var thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.start();
        return runnable;
    }
}

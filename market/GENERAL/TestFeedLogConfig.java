package ru.yandex.market.archive.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.archive.Age;
import ru.yandex.market.archive.JobConfiguration;
import ru.yandex.market.archive.schema.SourceColumn;
import ru.yandex.market.archive.schema.SourceOwner;

import static ru.yandex.market.archive.schema.DateSlice.DT;
import static ru.yandex.market.archive.schema.YtValueType.INT64;

/**
 * Created by skiftcha on 11.04.2017.
 */
@Configuration
public class TestFeedLogConfig extends CommonEntityConfig {

    @Value("${test.feed.log.and.generation.retention.unit}")
    private Age.Unit retentionUnit;

    @Value("${test.feed.log.and.generation.retention.age}")
    private int retentionAge;

    @Value("${test.feed.log.and.generation.deletion.age}")
    private int deletionAge;

    @Value("${test.feed.log.and.generation.migration.unit}")
    private Age.Unit migrationUnit;

    @Value("${test.feed.log.and.generation.migration.age}")
    private int migrationAge;

    @Bean
    public JobConfiguration testFeedLogJobConfig() {
        return getSimpleConfig(SourceOwner.SHOPS_WEB, "TEST_FEED_LOG");
    }

    @Override
    protected List<SourceColumn.Builder> columns() {
        return Arrays.asList(
                new SourceColumn.Builder("gid").type(INT64).verifyKey(),
                new SourceColumn.Builder("name"),
                new SourceColumn.Builder("generation_start_time"),
                new SourceColumn.Builder("end_time"),
                new SourceColumn.Builder("release_time").partitionBy(DT),
                new SourceColumn.Builder("mitype"),
                new SourceColumn.Builder("sc_version"),
                new SourceColumn.Builder("type").type(INT64),
                new SourceColumn.Builder("fid").type(INT64).splitBy(extractWorkerCount),
                new SourceColumn.Builder("feed_start_time"),
                new SourceColumn.Builder("finish_time"),
                new SourceColumn.Builder("yml_time"),
                new SourceColumn.Builder("return_code").type(INT64),
                new SourceColumn.Builder("noffers").type(INT64),
                new SourceColumn.Builder("is_modified").type(INT64),
                new SourceColumn.Builder("download_time"),
                new SourceColumn.Builder("total_offers").type(INT64),
                new SourceColumn.Builder("indexed_status").type(INT64),
                new SourceColumn.Builder("download_retcode").type(INT64),
                new SourceColumn.Builder("download_status"),
                new SourceColumn.Builder("parse_retcode").type(INT64),
                new SourceColumn.Builder("parse_log").clob(),
                new SourceColumn.Builder("cached_parse_log").clob(),
                new SourceColumn.Builder("cpa_offers").type(INT64),
                new SourceColumn.Builder("cpa_real_offers").type(INT64),
                new SourceColumn.Builder("matched_offers").type(INT64),
                new SourceColumn.Builder("discount_offers_count").type(INT64),
                new SourceColumn.Builder("matched_cluster_offer_count").type(INT64),
                new SourceColumn.Builder("parse_log_parsed").clob(),
                new SourceColumn.Builder("feed_file_type").type(INT64),
                new SourceColumn.Builder("market_template").type(INT64),
                new SourceColumn.Builder("shop_id").type(INT64),
                new SourceColumn.Builder("honest_discount_offers_count").type(INT64),
                new SourceColumn.Builder("white_promos_offers_count").type(INT64),
                new SourceColumn.Builder("honest_white_promos_offers_c").type(INT64),
                new SourceColumn.Builder("color"),
                new SourceColumn.Builder("cpc_real_offers").type(INT64)
        );
    }

    @Override
    protected Age getMigrationOld() {
        return new Age(migrationAge, migrationUnit);
    }

    @Override
    protected Age.Unit getRetentionUnit() {
        return retentionUnit;
    }

    @Override
    protected int getRetentionAge() {
        return retentionAge;
    }

    @Override
    protected int getDeletionAge() {
        return deletionAge;
    }
}

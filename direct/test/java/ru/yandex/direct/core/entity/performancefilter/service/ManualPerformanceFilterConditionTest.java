package ru.yandex.direct.core.entity.performancefilter.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import one.util.streamex.EntryStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jooq.Record;
import org.jooq.Result;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.direct.core.configuration.CoreConfiguration;
import ru.yandex.direct.core.entity.feed.model.BusinessType;
import ru.yandex.direct.core.entity.feed.model.FeedType;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterCondition;
import ru.yandex.direct.core.entity.performancefilter.repository.PerformanceFilterRepository;
import ru.yandex.direct.core.entity.performancefilter.schema.FilterSchema;
import ru.yandex.direct.dbschema.ppc.enums.FeedsBusinessType;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.multitype.entity.LimitOffset;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static ru.yandex.direct.dbschema.ppc.Tables.ADGROUPS_PERFORMANCE;
import static ru.yandex.direct.dbschema.ppc.Tables.BIDS_PERFORMANCE;
import static ru.yandex.direct.dbschema.ppc.Tables.FEEDS;

@ContextConfiguration(classes = CoreConfiguration.class)
@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore("Запускать вручную с выставленным -Dyandex.environment.type=development")
public class ManualPerformanceFilterConditionTest {
    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private PerformanceFilterStorage filterSchemaServiceStorage;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private PerformanceFilterRepository performanceFilterRepository;

    private static final String SEPARATOR = "\":[";
    private static final Logger logger = Logger.getLogger(ManualPerformanceFilterConditionTest.class);
    private static final LocalDateTime STOP_DATE = LocalDateTime.of(2019, 1, 1, 0, 0, 0);


    @Test
    public void getFieldStatistics() {
        Pattern fieldAndOperation = Pattern.compile("\"([^\"]+)\":");
        HashMap<FeedType, Map<String, AtomicInteger>> fieldsByFeedType = new HashMap<>();
        shardHelper.forEachShard(shard -> {
            System.out.println("SHARD = " + shard);
            int offset = 0;
            int limit = 1000;
            Result<Record> records = getRecords(shard, new LimitOffset(limit, offset));
            while (records.isNotEmpty()) {
                for (Record record : records) {
                    String stringFeedType = record.get(FEEDS.FEED_TYPE);
                    FeedType feedType = FeedType.fromTypedValue(stringFeedType);
                    Map<String, AtomicInteger> statisticByFieldName =
                            fieldsByFeedType.computeIfAbsent(feedType, k -> new HashMap<>());
                    String conditionJson = record.get(BIDS_PERFORMANCE.CONDITION_JSON);
                    if (isEmpty(conditionJson)) {
                        continue;
                    }
                    Matcher matcher = fieldAndOperation.matcher(conditionJson);
                    if (!matcher.find()) {
                        continue;
                    }
                    for (int i = 0; i < matcher.groupCount(); i++) {
                        String fieldName = matcher.group(i);
                        if (fieldName.contains(" ")) {
                            String[] split = fieldName.split(" ");
                            fieldName = split[0];
                        }
                        AtomicInteger statistic =
                                statisticByFieldName.computeIfAbsent(fieldName, k -> new AtomicInteger(0));
                        statistic.incrementAndGet();
                    }
                }
                offset += records.size();
                records = getRecords(shard, new LimitOffset(limit, offset));
            }
        });
        EntryStream.of(fieldsByFeedType)
                .forKeyValue((feedType, statisticByFieldName) -> {
                    System.out.println(feedType);
                    EntryStream.of(statisticByFieldName)
                            .forKeyValue((fieldName, statistic) -> {
                                System.out.println("\t" + fieldName + ": " + statistic.get());
                            });
                });
    }

    @Test
    public void check() {
        logger.info("\nShard count: " + shardHelper.dbShards().size());
        AtomicInteger mismatched = new AtomicInteger(0);
        AtomicInteger count = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);
        shardHelper.forEachShard(shard -> {
            ShardResult shardResult = checkShard(shard);
            mismatched.addAndGet(shardResult.getMismatched());
            count.addAndGet(shardResult.getTotalCount());
            errors.addAndGet(shardResult.getErrors());
        });
        logger.info("\nRecords count: " + count.get());
        logger.info("\nMismatched: " + mismatched.get());
        logger.info("\nErrors: " + errors.get());
    }

    private ShardResult checkShard(int shard) {
        System.out.println("SHARD = " + shard);
        int mismatched = 0;
        int errors = 0;
        int offset = 0;
        int limit = 1000;
        Result<Record> records = getRecords(shard, new LimitOffset(limit, offset));
        while (records.isNotEmpty()) {
            for (Record record : records) {
                try {
                    FeedsBusinessType feedsBusinessType = record.get(FEEDS.BUSINESS_TYPE);
                    BusinessType businessType = BusinessType.fromSource(feedsBusinessType);
                    String stringFeedType = record.get(FEEDS.FEED_TYPE);
                    FeedType feedType = FeedType.fromTypedValue(stringFeedType);
                    String conditionJson = record.get(BIDS_PERFORMANCE.CONDITION_JSON);
                    String normConditionJson = norm(conditionJson);
                    FilterSchema filterSchema = filterSchemaServiceStorage.getFilterSchema(businessType, feedType);
                    List<PerformanceFilterCondition> parsedConditions =
                            PerformanceFilterConditionDBFormatParser.INSTANCE.parse(filterSchema, conditionJson);
                    String serializedConditions =
                            PerformanceFilterConditionDBFormatSerializer.INSTANCE.serialize(parsedConditions);
                    String normSerializedConditions = norm(serializedConditions);
                    if (Objects.equals(normConditionJson, normSerializedConditions)) {
                        continue;
                    }
                    mismatched++;
                    PerformanceFilter performanceFilter = performanceFilterRepository.jooqMapper.fromDb(record);
                    System.out.println(performanceFilter);
                    System.out.println(conditionJson);
                    System.out.println(StringUtils.repeat('-', 41));
                    System.out.println(normConditionJson + "\n" + normSerializedConditions);
                    System.out.println(StringUtils.repeat('=', 80));
                } catch (Exception ex) {
                    errors++;
                    System.out.println(record.get(BIDS_PERFORMANCE.CONDITION_JSON));
                    ex.printStackTrace(System.out);
                }
            }
            offset += records.size();
            records = getRecords(shard, new LimitOffset(limit, offset));
        }
        System.out.println("count=" + offset);
        return new ShardResult()
                .withTotalCount(offset)
                .withMismatched(mismatched)
                .withErrors(errors);
    }

    private class ShardResult {
        private int totalCount = 0;
        private int mismatched = 0;
        private int errors = 0;

        int getTotalCount() {
            return totalCount;
        }

        ShardResult withTotalCount(int totalCount) {
            this.totalCount = totalCount;
            return this;
        }

        int getMismatched() {
            return mismatched;
        }

        ShardResult withMismatched(int mismatched) {
            this.mismatched = mismatched;
            return this;
        }

        int getErrors() {
            return errors;
        }

        ShardResult withErrors(int errors) {
            this.errors = errors;
            return this;
        }
    }

    private Result<Record> getRecords(int shard, LimitOffset limitOffset) {
        return dslContextProvider.ppc(shard)
                .select(performanceFilterRepository.jooqMapper.getFieldsToRead())
                .from(BIDS_PERFORMANCE)
                .join(ADGROUPS_PERFORMANCE).on(ADGROUPS_PERFORMANCE.PID.eq(BIDS_PERFORMANCE.PID))
                .join(FEEDS).on(FEEDS.FEED_ID.eq(ADGROUPS_PERFORMANCE.FEED_ID))
                .where(BIDS_PERFORMANCE.IS_DELETED.eq(0L))
                .and(BIDS_PERFORMANCE.LAST_CHANGE.lessThan(STOP_DATE))
                .orderBy(BIDS_PERFORMANCE.PERF_FILTER_ID)
                .limit(limitOffset.limit())
                .offset(limitOffset.offset())
                .fetch();
    }

    private String norm(String conditionJson) {
        if (isEmpty(conditionJson)) {
            return conditionJson;
        }
        int index = conditionJson.indexOf(SEPARATOR);
        if (index == -1) {
            return conditionJson;
        }
        String firstPart = conditionJson.substring(0, index);
        String secondPart = conditionJson.substring(index + 3);
        secondPart = secondPart.replaceAll("(\\d+)\\.00", "$1");
        secondPart = secondPart.replaceAll("(\\d+\\.\\d)0", "$1");
        secondPart = secondPart.replaceAll("\"(\\d+)\"", "$1");
        return firstPart + SEPARATOR + secondPart;
    }

}

package ru.yandex.direct.core.entity.dynamictextadtarget.repository;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.jooq.Record;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.direct.core.configuration.CoreConfiguration;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedRule;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.core.entity.dynamictextadtarget.repository.DynamicTextAdTargetMapping.dynamicFeedRulesToJson;
import static ru.yandex.direct.core.entity.dynamictextadtarget.utils.DynamicTextAdTargetHashUtils.getHashForDynamicFeedRules;
import static ru.yandex.direct.dbschema.ppc.Tables.ADGROUPS_DYNAMIC;
import static ru.yandex.direct.dbschema.ppc.Tables.BIDS_DYNAMIC;
import static ru.yandex.direct.dbschema.ppc.Tables.FEEDS;
import static ru.yandex.direct.dbschema.ppc.tables.DynamicConditions.DYNAMIC_CONDITIONS;

@ContextConfiguration(classes = CoreConfiguration.class)
@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore("Запускать вручную с выставленным -Dyandex.environment.type=development")
public class ManualDynamicConditionHashTest {

    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private DynamicTextAdTargetRepository dynamicTextAdTargetRepository;
    @Autowired
    private ShardHelper shardHelper;

    @Test
    public void getDynamicConditionsTest() {
        List<Record> records = getAllDynamicFeedConditions();
        check(records);
    }

    private List<Record> getAllDynamicFeedConditions() {
        // записей на всех шардах 316_448 - поэтому достаем все
        List<Record> records = new ArrayList<>();
        for (Integer shard : shardHelper.dbShards()) {
            List<Record> recordsInShard = dslContextProvider.ppc(shard)
                    .select(DYNAMIC_CONDITIONS.DYN_COND_ID, DYNAMIC_CONDITIONS.CONDITION_HASH,
                            DYNAMIC_CONDITIONS.CONDITION_JSON, FEEDS.FEED_TYPE, FEEDS.BUSINESS_TYPE)
                    .from(DYNAMIC_CONDITIONS)
                    .join(BIDS_DYNAMIC).on(BIDS_DYNAMIC.DYN_COND_ID.eq(DYNAMIC_CONDITIONS.DYN_COND_ID))
                    .join(ADGROUPS_DYNAMIC).on(ADGROUPS_DYNAMIC.PID.eq(DYNAMIC_CONDITIONS.PID))
                    .join(FEEDS).on(FEEDS.FEED_ID.eq(ADGROUPS_DYNAMIC.FEED_ID))
                    .orderBy(DYNAMIC_CONDITIONS.DYN_COND_ID)
                    .limit(1_000_000)
                    .fetch(record -> record);
            records.addAll(recordsInShard);
        }
        return records;
    }

    private void check(List<Record> records) {
        int differentConditionHash = 0;
        int differentConditionJson = 0;
        int errorsCount = 0;
        int total = 0;

        for (Record record : records) {
            String conditionJsonFromDb = record.get(DYNAMIC_CONDITIONS.CONDITION_JSON);
            BigInteger conditionHashFromDb = record.get(DYNAMIC_CONDITIONS.CONDITION_HASH).toBigInteger();
            Long dynamicConditionId = record.get(DYNAMIC_CONDITIONS.DYN_COND_ID);

            try {
                DynamicFeedAdTarget dynamicFeedAdTarget =
                        dynamicTextAdTargetRepository.convertDynamicFeedAdTargetFromDb(record);
                List<DynamicFeedRule> dynamicFeedRules = dynamicFeedAdTarget.getCondition();

                String conditionJson = dynamicFeedRulesToJson(dynamicFeedRules);
                BigInteger conditionHash = getHashForDynamicFeedRules(dynamicFeedRules);

                if (!conditionJson.equals(conditionJsonFromDb)) {
                    differentConditionJson++;
                    System.out.println(String.format("differs conditionJson dyn_cond_id: %s, db: %s, calculated: %s",
                            dynamicConditionId, conditionJsonFromDb, conditionJson));
                }
                if (!conditionHash.equals(conditionHashFromDb)) {
                    differentConditionHash++;
                    System.out.println(String.format("differs conditionHash dyn_cond_id: %s, db: %s, calculated: %s",
                            dynamicConditionId, conditionHashFromDb, conditionHash));
                }
            } catch (RuntimeException e) {
                errorsCount++;
                System.out.println(String.format("error for dyn_cond_id: %s", dynamicConditionId));
            }
            total++;
        }
        System.out.println(String.format("differs conditionHash: %s, differs conditionJson: %s, errorCount: %s, " +
                "total: %s", differentConditionHash, differentConditionJson, errorsCount, total));
    }
}

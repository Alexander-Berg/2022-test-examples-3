package ru.yandex.direct.core.testing.repository;

import java.time.LocalDateTime;

import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.dbschema.ppc.enums.AutoPriceCampQueueQueueType;
import ru.yandex.direct.dbschema.ppc.enums.AutoPriceCampQueueStatus;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.AUTO_PRICE_CAMP_QUEUE;

public class TestAutoPriceCampQueueRepository {

    @Autowired
    private DslContextProvider dslContextProvider;

    public void addDefaultAutoPriceCampQueueRecordInWaitStatus(CampaignInfo campaignInfo) {
        dslContextProvider.ppc(campaignInfo.getShard())
                .insertInto(AUTO_PRICE_CAMP_QUEUE)
                .set(AUTO_PRICE_CAMP_QUEUE.CID, campaignInfo.getCampaignId())
                .set(AUTO_PRICE_CAMP_QUEUE.OPERATOR_UID, campaignInfo.getUid())
                .set(AUTO_PRICE_CAMP_QUEUE.SEND_TIME, LocalDateTime.now())
                .set(AUTO_PRICE_CAMP_QUEUE.STATUS, AutoPriceCampQueueStatus.Wait)
                .set(AUTO_PRICE_CAMP_QUEUE.PARAMS_HASH, ULong.valueOf(123L))
                .set(AUTO_PRICE_CAMP_QUEUE.PARAMS_COMPRESSED, "abc".getBytes())
                .set(AUTO_PRICE_CAMP_QUEUE.NUMBER_OF_PHRASES, 1L)
                .set(AUTO_PRICE_CAMP_QUEUE.QUEUE_TYPE, AutoPriceCampQueueQueueType.easy)
                .execute();
    }

    public Result<Record2<Long, AutoPriceCampQueueStatus>> getAutoPriceCampQueueRecords(CampaignInfo campaignInfo) {
        return dslContextProvider.ppc(campaignInfo.getShard())
                .select(AUTO_PRICE_CAMP_QUEUE.CID, AUTO_PRICE_CAMP_QUEUE.STATUS)
                .from(AUTO_PRICE_CAMP_QUEUE)
                .where(AUTO_PRICE_CAMP_QUEUE.CID.eq(campaignInfo.getCampaignId()))
                .fetch();
    }
}

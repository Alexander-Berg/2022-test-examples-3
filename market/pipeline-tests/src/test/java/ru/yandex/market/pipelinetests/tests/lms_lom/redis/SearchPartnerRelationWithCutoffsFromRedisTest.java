package ru.yandex.market.pipelinetests.tests.lms_lom.redis;

import java.util.List;
import java.util.Set;

import io.qameta.allure.Epic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;

import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.pipelinetests.tests.lms_lom.AbstractSearchPartnerRelationWithCutoffsTest;
import ru.yandex.market.pipelinetests.tests.lms_lom.utils.PartnerRelationCompareUtil;

@Epic("Lms Lom Redis")
@Tag("LmsLomRedisSyncTest")
@DisplayName("Синхронизация данных LMS в redis")
public class SearchPartnerRelationWithCutoffsFromRedisTest extends AbstractSearchPartnerRelationWithCutoffsTest {

    @Override
    public void searchAndComparePartnerRelationsWithCutoffs(
        Long partnerFromId,
        Long partnerToId,
        List<PartnerRelationEntityDto> expectedPartnerRelations
    ) {
        PartnerRelationFilter filter = PartnerRelationFilter.newBuilder()
            .fromPartnersIds(Set.of(partnerFromId))
            .toPartnersIds(Set.of(partnerToId))
            .build();

        List<PartnerRelationEntityDto> lmsPartnerRelations = LMS_STEPS.searchPartnerRelation(filter).getEntities();
        PartnerRelationCompareUtil.comparePartnerRelationWithCutoffs(
            softly,
            expectedPartnerRelations,
            lmsPartnerRelations
        );

        List<PartnerRelationEntityDto> redisPartnerRelations =
            LOM_REDIS_STEPS.searchPartnerRelationWithCutoffsFromRedis(filter);
        PartnerRelationCompareUtil.comparePartnerRelationWithCutoffs(
            softly,
            lmsPartnerRelations,
            redisPartnerRelations
        );
    }
}

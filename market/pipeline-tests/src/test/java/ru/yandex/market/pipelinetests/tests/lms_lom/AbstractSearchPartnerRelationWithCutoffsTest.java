package ru.yandex.market.pipelinetests.tests.lms_lom;

import java.util.Collections;
import java.util.List;

import io.qameta.allure.Epic;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;

import static toolkit.FileUtil.bodyStringFromFile;
import static toolkit.Mapper.mapLmsResponse;

@Epic("Lms Lom Redis")
@Tag("LmsLomRedisSyncTest")
public abstract class AbstractSearchPartnerRelationWithCutoffsTest extends AbstractLmsLomTest {

    private static final List<PartnerRelationEntityDto> EXPECTED_PARTNER_RELATIONS_WITH_CUTOFF = List.of(mapLmsResponse(
        bodyStringFromFile("lms_lom_redis/lms_response/partner_relation/partner_relation_with_cutoffs.json"),
        PartnerRelationEntityDto.class
    ));

    private static final List<PartnerRelationEntityDto> EXPECTED_PARTNER_RELATIONS_WITH_EMPTY_CUTOFF =
        List.of(mapLmsResponse(
            bodyStringFromFile("lms_lom_redis/lms_response/partner_relation/partner_relation_with_empty_cutoffs.json"),
            PartnerRelationEntityDto.class
        ));

    public abstract void searchAndComparePartnerRelationsWithCutoffs(
        Long partnerFromId,
        Long partnerToId,
        List<PartnerRelationEntityDto> expectedPartnerRelations
    );

    @Test
    @SneakyThrows
    @DisplayName("Поиск связок партнеров с катоффами по фильтру")
    public void searchPartnerRelationWithCutoffs() {
        searchAndComparePartnerRelationsWithCutoffs(
            PARTNER_RELATION_FROM_ID,
            PARTNER_RELATION_TO_ID,
            EXPECTED_PARTNER_RELATIONS_WITH_CUTOFF
        );
    }

    @Test
    @SneakyThrows
    @DisplayName("Поиск связок партнеров с пустым списком катоффов по фильтру")
    public void searchPartnerRelationWithEmptyCutoffs() {
        searchAndComparePartnerRelationsWithCutoffs(
            EMPTY_CUTOFFS_PARTNER_RELATION_FROM_ID,
            EMPTY_CUTOFFS_PARTNER_RELATION_TO_ID,
            EXPECTED_PARTNER_RELATIONS_WITH_EMPTY_CUTOFF
        );
    }

    @Test
    @SneakyThrows
    @DisplayName("Поиск отсутствующей связки партнеров с катоффами по фильтру")
    public void searchAbsentPartnerRelationWitCutoffs() {
        searchAndComparePartnerRelationsWithCutoffs(
            EMPTY_CUTOFFS_PARTNER_RELATION_TO_ID,
            EMPTY_CUTOFFS_PARTNER_RELATION_FROM_ID,
            Collections.emptyList()
        );
    }
}

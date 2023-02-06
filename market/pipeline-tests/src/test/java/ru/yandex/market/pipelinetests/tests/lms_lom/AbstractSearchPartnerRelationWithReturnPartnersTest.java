package ru.yandex.market.pipelinetests.tests.lms_lom;

import java.util.Arrays;
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
public abstract class AbstractSearchPartnerRelationWithReturnPartnersTest extends AbstractLmsLomTest {

    private static final PartnerRelationEntityDto FIRST_EXPECTED_PARTNER_RELATION_WITH_RETURN_PARTNER = mapLmsResponse(
        bodyStringFromFile(
            "lms_lom_redis/lms_response/partner_relation/first_partner_relation_with_return_partner.json"
        ),
        PartnerRelationEntityDto.class
    );

    private static final PartnerRelationEntityDto SECOND_EXPECTED_PARTNER_RELATION_WITH_RETURN_PARTNER = mapLmsResponse(
        bodyStringFromFile(
            "lms_lom_redis/lms_response/partner_relation/second_partner_relation_with_return_partner.json"
        ),
        PartnerRelationEntityDto.class
    );

    public abstract void searchAndComparePartnerRelationWithReturnPartners(
        Long partnerFromId,
        List<PartnerRelationEntityDto> expectedPartnerRelations
    );

    @Test
    @SneakyThrows
    @DisplayName("Поиск связок партнера с возвратными партнерами по фильтру")
    public void searchPartnerRelationsWithReturnPartner() {
        searchAndComparePartnerRelationWithReturnPartners(
            PARTNER_RELATION_FROM_ID,
            Arrays.asList(
                FIRST_EXPECTED_PARTNER_RELATION_WITH_RETURN_PARTNER,
                SECOND_EXPECTED_PARTNER_RELATION_WITH_RETURN_PARTNER
            )
        );
    }

    @Test
    @SneakyThrows
    @DisplayName("Поиск несуществующих связок партнера по фильтру")
    public void searchAbsentPartnerRelations() {
        searchAndComparePartnerRelationWithReturnPartners(
            PARTNER_RELATION_TO_ID,
            Collections.emptyList()
        );
    }
}

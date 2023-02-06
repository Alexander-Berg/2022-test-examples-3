package ru.yandex.market.pipelinetests.tests.lms_lom;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.qameta.allure.Epic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.pipelinetests.tests.lms_lom.utils.PartnersCompareUtils;

import static toolkit.FileUtil.bodyStringFromFile;
import static toolkit.Mapper.mapLmsResponse;

@Epic("Lms Lom Redis")
@Tag("LmsLomRedisSyncTest")
public abstract class AbstractGetPartnerTest extends AbstractLmsLomTest {

    protected static final PartnerResponse EXPECTED_LMS_FILLED_PARTNER = mapLmsResponse(
        bodyStringFromFile("lms_lom_redis/lms_response/partner/filled.json"),
        PartnerResponse.class
    );

    protected static final PartnerResponse EXPECTED_LMS_EMPTY_PARTNER = mapLmsResponse(
        bodyStringFromFile("lms_lom_redis/lms_response/partner/empty.json"),
        PartnerResponse.class
    );

    @Test
    @DisplayName("Партнёр со всеми заполненными данными в лмс соответствует ожидаемым данным")
    void checkPreConditionsForFilledPartner() {
        PartnerResponse lmsPartner = LMS_STEPS.getPartner(FILLED_PARTNER_ID);
        PartnersCompareUtils.comparePartners(softly, EXPECTED_LMS_FILLED_PARTNER, lmsPartner);
    }

    @Test
    @DisplayName("Партнёр с заполненными только обязательными данными в лмс соответствует ожидаемым данным")
    void checkPreConditionsForRequieredFieldsOnlyPartner() {
        PartnerResponse lmsPartner = LMS_STEPS.getPartner(EMPTY_PARTNER_ID);
        PartnersCompareUtils.comparePartners(softly, EXPECTED_LMS_EMPTY_PARTNER, lmsPartner);
    }

    @Test
    @DisplayName("Список партнёров в лмс соответствует ожидаемым данным")
    void checkPreConditionForPartnersList() {
        List<PartnerResponse> expectedLmsPartners = new ArrayList<>(2);
        expectedLmsPartners.add(EXPECTED_LMS_FILLED_PARTNER);
        expectedLmsPartners.add(EXPECTED_LMS_EMPTY_PARTNER);

        List<PartnerResponse> lmsPartners = LMS_STEPS.searchPartners(
            SearchPartnerFilter.builder()
                .setIds(Set.of(FILLED_PARTNER_ID, EMPTY_PARTNER_ID))
                .build()
        );
        PartnersCompareUtils.comparePartnerLists(softly, expectedLmsPartners, lmsPartners);
    }

    public abstract void getFilledPartnerById();

    public abstract void getPartnerById();

    public abstract void getPartnersByIds();
}

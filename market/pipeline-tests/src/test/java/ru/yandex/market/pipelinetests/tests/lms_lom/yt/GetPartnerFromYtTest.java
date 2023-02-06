package ru.yandex.market.pipelinetests.tests.lms_lom.yt;

import java.util.List;
import java.util.Set;

import io.qameta.allure.Epic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.pipelinetests.tests.lms_lom.AbstractGetPartnerTest;
import ru.yandex.market.pipelinetests.tests.lms_lom.utils.PartnersCompareUtils;

@Epic("Lms Lom Redis")
@Tag("LmsLomRedisSyncTest")
@DisplayName("Синхронизация данных LMS в YT")
public class GetPartnerFromYtTest extends AbstractGetPartnerTest {

    @Test
    @Override
    @DisplayName("YT: поиск партнёра со всеми заполненными полями")
    public void getFilledPartnerById() {
        PartnerResponse lmsPartner = LMS_STEPS.getPartner(FILLED_PARTNER_ID);
        PartnerResponse ytPartner = LOM_LMS_YT_STEPS.getPartner(FILLED_PARTNER_ID);

        PartnersCompareUtils.comparePartners(softly, lmsPartner, ytPartner);
    }

    @Test
    @Override
    @DisplayName("YT: поиск партнёра с заполненными только обязательными полями")
    public void getPartnerById() {
        PartnerResponse lmsPartner = LMS_STEPS.getPartner(EMPTY_PARTNER_ID);
        PartnerResponse ytPartner = LOM_LMS_YT_STEPS.getPartner(EMPTY_PARTNER_ID);

        PartnersCompareUtils.comparePartners(softly, lmsPartner, ytPartner);
    }

    @Test
    @Override
    @DisplayName("YT: поиск партнёров по нескольким id")
    public void getPartnersByIds() {
        List<PartnerResponse> lmsPartners = LMS_STEPS.searchPartners(
            SearchPartnerFilter.builder()
                .setIds(Set.of(FILLED_PARTNER_ID, EMPTY_PARTNER_ID))
                .build()
        );
        List<PartnerResponse> ytPartners = LOM_LMS_YT_STEPS.getPartners(
            Set.of(FILLED_PARTNER_ID, EMPTY_PARTNER_ID)
        );

        PartnersCompareUtils.comparePartnerLists(softly, lmsPartners, ytPartners);
    }
}

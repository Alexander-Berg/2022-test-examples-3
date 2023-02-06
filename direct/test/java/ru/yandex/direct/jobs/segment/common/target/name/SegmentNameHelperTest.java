package ru.yandex.direct.jobs.segment.common.target.name;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.AdShowType;
import ru.yandex.direct.core.entity.adgroup.model.UsersSegment;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.i18n.Language;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@JobsTest
@ExtendWith(SpringExtension.class)
class SegmentNameHelperTest {

    @Autowired
    private SegmentNameHelper segmentNameHelper;

    static Object[] parametrizedTestData() {
        return new Object[][]{
                {AdGroupType.CPM_VIDEO, "", ""},
                {AdGroupType.CPM_OUTDOOR, "(Наружная реклама)", ""},
                {AdGroupType.BASE, "", " в РСЯ"},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parametrizedTestData")
    void getSegmentName(AdGroupType adGroupType, String expectedAdGroupPart, String expectedActionPart) {
        Long campaignId = 11L;
        Long adGroupId = 12L;
        String campaignName = "Веселые ребята";

        Campaign campaign = new Campaign().withId(campaignId).withName(campaignName);
        UsersSegment usersSegment = new UsersSegment().withAdGroupId(adGroupId).withType(AdShowType.START);

        String name = segmentNameHelper.getSegmentName(usersSegment, campaign, adGroupType, Language.RU);
        String expected = "Кампания:11-\"Веселые ребята\". Группа:12" + expectedAdGroupPart +
                "-Показ" + expectedActionPart;
        assertEquals(expected, name);
    }

    @Test
    public void getSegmentName_TooLongCampaignName() {
        Long campaignId = 234L;
        Long adGroupId = 456L;
        String campaignName = "Очень длинное имя кампании Очень длинное имя кампании Очень длинное имя кампании " +
                "Очень длинное имя кампании Очень длинное имя кампании Очень длинное имя кампании " +
                "Очень длинное имя кампании Очень длинное имя кампании Очень длинное имя кампании";

        Campaign campaign = new Campaign().withId(campaignId).withName(campaignName);
        UsersSegment usersSegment = new UsersSegment().withAdGroupId(adGroupId).withType(AdShowType.MIDPOINT);

        String actual = segmentNameHelper.getSegmentName(usersSegment, campaign, AdGroupType.CPM_VIDEO, Language.RU);
        String expected = "Кампания:234-\"Очень длинное имя кампании Очень длинное имя кампании " +
                "Очень длинное имя кампании Очень длинное имя кампании Очень длинное имя кампании " +
                "Очень длинное имя кампании Очень длинное имя кампании Очень длинное имя ка\". " +
                "Группа:456-Просмотр 50%";
        assertEquals(expected, actual);
        assertEquals(249, actual.length());
    }
}

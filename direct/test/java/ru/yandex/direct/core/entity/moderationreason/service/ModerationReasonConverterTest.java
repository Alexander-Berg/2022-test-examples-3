package ru.yandex.direct.core.entity.moderationreason.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.moderationdiag.ModerationDiagDataConverterKt;
import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiag;
import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiagData;
import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiagType;
import ru.yandex.direct.core.entity.moderationreason.model.AgencyInfo;
import ru.yandex.direct.core.entity.moderationreason.model.ManagerInfo;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReportResponse;
import ru.yandex.direct.core.testing.data.TestModerationDiag;
import ru.yandex.direct.core.testing.data.TestNewTextBanners;
import ru.yandex.direct.liveresource.LiveResourceFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static ru.yandex.direct.core.entity.moderationreason.service.ModerationDiagConverter.stringToLongList;
import static ru.yandex.direct.core.entity.moderationreason.service.ModerationReasonAggregatorService.fillAllBannerRejectedReasons;

public class ModerationReasonConverterTest {

    private static final String MODERATION_DIAG_FILE = "classpath:///moderationreason/moderation_diag.json";
    private static final String BAD_BANNER_FILE = "classpath:///moderationreason/bad_banner.json";
    private static final String BAD_IMAGE_FILE = "classpath:///moderationreason/bad_image.json";
    private static final String BAD_ALL_TYPES = "classpath:///moderationreason/bad_all_types.json";

    private static final ModerationDiag MODERATION_DIAG_1 =
            TestModerationDiag.createModerationDiag1().withDiagText("full text1");

    private static String serialize(Object obj, boolean pretty) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        if (pretty) {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        }

        return mapper.writeValueAsString(obj);
    }

    @Test
    public void serialize_FromModerationDiag_ReturnsCorrect() throws JsonProcessingException {
        ModerationDiagData moderationDiagData = ModerationDiagDataConverterKt.convert(MODERATION_DIAG_1);
        String expected = LiveResourceFactory.get(MODERATION_DIAG_FILE).getContent();
        String test = serialize(moderationDiagData, true);
        assertEquals(expected, test);
    }

    @Test
    public void serializeFullModerationReportResponse_ReturnCorrect() throws JsonProcessingException {
        TextBanner banner = TestNewTextBanners.fullTextBanner().withId(135L)
                .withImageId(444L)
                .withImageHash("#erwr32132")
                .withImageName("pic1.jpg")
                .withVcardId(555L);
        Map<ModerationReasonObjectType, List<ModerationDiag>> map = new EnumMap<>(ModerationReasonObjectType.class);
        for (ModerationReasonObjectType type : EnumSet.allOf(ModerationReasonObjectType.class)) {
            map.put(type, Collections.singletonList(MODERATION_DIAG_1));
        }

        ModerationReportResponse moderationReportResponse = new ModerationReportResponse();
        fillAllBannerRejectedReasons(moderationReportResponse, banner, null, map);

        String test = serialize(moderationReportResponse, true);
        String expected = LiveResourceFactory.get(BAD_ALL_TYPES).getContent();
        assertEquals(expected, test);
    }

    @Test
    public void serializeImageModerationReason_ReturnCorrect() throws JsonProcessingException {
        TextBanner banner = TestNewTextBanners.fullTextBanner().withId(3518381787L)
                .withImageId(444L)
                .withImageName(null)
                .withImageHash("f0tiq6stVD7jc55hHUyhBA")
                .withVcardId(37245641L);
        Map<ModerationReasonObjectType, List<ModerationDiag>> map = new EnumMap<>(ModerationReasonObjectType.class);

        map.put(ModerationReasonObjectType.IMAGE, Collections.singletonList(createImageModerationDiag()));

        ModerationReportResponse moderationReportResponse = new ModerationReportResponse();
        moderationReportResponse.setManagerInfo(createManagerInfo());
        fillAllBannerRejectedReasons(moderationReportResponse, banner, null, map);

        String expected = LiveResourceFactory.get(BAD_IMAGE_FILE).getContent();
        String test = serialize(moderationReportResponse, true);
        assertEquals(expected, test);
    }

    @Test
    public void serializeBannerModerationReason_ReturnCorrect() throws JsonProcessingException {

        TextBanner banner = TestNewTextBanners.fullTextBanner().withId(4977241360L)
                .withVcardId(36996285L);
        Map<ModerationReasonObjectType, List<ModerationDiag>> map = new EnumMap<>(ModerationReasonObjectType.class);

        map.put(ModerationReasonObjectType.BANNER, Collections.singletonList(createBannerModerationDiag()));

        ModerationReportResponse moderationReportResponse = new ModerationReportResponse();
        AgencyInfo agencyInfo = createAgencyInfo();
        moderationReportResponse.setAgencyInfo(agencyInfo);
        moderationReportResponse.setAgencyManager(null);
        fillAllBannerRejectedReasons(moderationReportResponse, banner, null, map);
        String expected = LiveResourceFactory.get(BAD_BANNER_FILE).getContent();
        String test = serialize(moderationReportResponse, true);
        assertEquals(expected, test);
    }

    private static AgencyInfo createAgencyInfo() {
        return new AgencyInfo()
                .withCountryRegionId(String.valueOf(225))
                .withEmail("dggruppanomer3@yandex.ru");
    }

    private static ManagerInfo createManagerInfo() {
        ManagerInfo managerInfo = new ManagerInfo();
        managerInfo.setEmail("o.borshchova@yandex-team.ru");
        return managerInfo;
    }

    @SuppressWarnings("checkstyle:linelength")
    private static ModerationDiag createImageModerationDiag() {
        return new ModerationDiag()
                .withId(286L)
                .withType(ModerationDiagType.COMMON)
                .withShortText("Запрещенная тематика")
                .withFullText(
                        "Реклама товара/услуги запрещена. Если объявление было отклонено на модерации, но вы не " +
                                "рекламируете запрещенные товары и услуги, напишите нам через форму: https://yandex" +
                                ".ru/support/direct/troubleshooting/error-ban.html")
                .withDiagText(
                        "Реклама товара/услуги запрещена. Если объявление было отклонено на модерации, но вы не " +
                                "рекламируете запрещенные товары и услуги, напишите нам через форму: https://yandex" +
                                ".ru/support/direct/troubleshooting/error-ban.html")
                .withAllowFirstAid(false)
                .withStrongReason(true)
                .withUnbanIsProhibited(false)
                .withToken("Yandex policy auto");
    }

    @SuppressWarnings("checkstyle:linelength")
    private static ModerationDiag createBannerModerationDiag() {
        return new ModerationDiag()
                .withId(2L)
                .withType(ModerationDiagType.COMMON)
                .withShortText("По закону")
                .withFullText(
                        "Реклама не соответствует действующему законодательству. Подробнее о причине отклонения вы " +
                                "можете прочитать здесь: https://yandex.ru/support/direct-tooltips/moderation/law.html")
                .withDiagText(
                        "Реклама не соответствует действующему законодательству. Подробнее о причине отклонения вы " +
                                "можете прочитать здесь: https://yandex.ru/support/direct-tooltips/moderation/law.html")
                .withAllowFirstAid(false)
                .withStrongReason(true)
                .withUnbanIsProhibited(false)
                .withToken("Russian law");
    }

    @Test
    public void stringToLongList_ReturnCorrect() {
        List<Long> expected = Arrays.asList(1L, 2L, 3L);
        assertEquals(expected, stringToLongList("1,2,3"));
        assertEquals(expected, stringToLongList("1, 2, 3"));
        assertEquals(expected, stringToLongList("1 , 2,3"));
    }

    @Test
    public void stringToLongList_EmptyString_ReturnNull() {
        assertNull(stringToLongList(""));
    }

    @Test
    public void stringToLongList_NullString_ReturnNull() {
        assertNull(stringToLongList(null));
    }
}


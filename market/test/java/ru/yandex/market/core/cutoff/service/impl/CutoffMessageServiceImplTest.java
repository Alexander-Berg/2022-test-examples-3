package ru.yandex.market.core.cutoff.service.impl;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.cutoff.model.AboScreenshotDto;
import ru.yandex.market.core.cutoff.model.CutoffMessageInfo;
import ru.yandex.market.core.cutoff.service.CutoffMessageService;
import ru.yandex.market.core.message.PartnerNotificationMessageServiceTest;
import ru.yandex.market.partner.notification.client.model.WebUINotificationResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тест для {@link CutoffMessageServiceImpl}.
 */
@DbUnitDataSet(before = "csv/CutoffMessageServiceImplTest.before.csv")
public class CutoffMessageServiceImplTest extends FunctionalTest {
    @Autowired
    private CutoffMessageService cutoffMessageService;

    private static final List<AboScreenshotDto> SCREENSHOT_URLS_1 = List.of(new AboScreenshotDto(17L, "hash1"));
    private static final List<AboScreenshotDto> SCREENSHOT_URLS_2 = List.of(new AboScreenshotDto(19L, "hash2"));
    private static final CutoffMessageInfo INFO_1_DB = new CutoffMessageInfo(
            1L,
            10L,
            "SUBJECT_1",
            "BODY_1",
            SCREENSHOT_URLS_1
    );
    private static final CutoffMessageInfo INFO_2_DB = new CutoffMessageInfo(
            2L,
            20L,
            "SUBJECT_2",
            "BODY_2",
            SCREENSHOT_URLS_2
    );


    @Test
    void testGetMessagesByCutoffs() {
        // given
        PartnerNotificationMessageServiceTest.mockPN(
                partnerNotificationClient,
                new WebUINotificationResponse()
                        .subject("SUBJECT_1")
                        .body("BODY_1")
                        .priority(1L)
                        .groupId(1L),
                new WebUINotificationResponse()
                        .subject("SUBJECT_2")
                        .body("BODY_2")
                        .priority(1L)
                        .groupId(2L)
        );

        // when
        var actual = cutoffMessageService.getMessagesByCutoffs(List.of(10L, 20L));

        // then
        assertThat(actual).isEqualTo(Map.of(
                10L, INFO_1_DB,
                20L, INFO_2_DB
        ));
    }

    @Test
    void shouldReturnEmptyCollection() {
        // when
        var actual = cutoffMessageService.getMessagesByCutoffs(List.of());

        // then
        assertThat(actual).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldPreferPNResultOnAnyFlag(boolean flag) {
        // given
        PartnerNotificationMessageServiceTest.mockPN(
                partnerNotificationClient,
                new WebUINotificationResponse()
                        .subject("subj")
                        .body("body")
                        .shopId(1L)
                        .priority(1L)
                        .groupId(1L)
        );

        // when
        var actual = cutoffMessageService.getMessagesByCutoffs(List.of(10L));

        // then
        assertThat(actual).isEqualTo(Map.of(
                10L, new CutoffMessageInfo(
                        1L,
                        10L,
                        "subj",
                        "body",
                        SCREENSHOT_URLS_1
                )));
    }
}

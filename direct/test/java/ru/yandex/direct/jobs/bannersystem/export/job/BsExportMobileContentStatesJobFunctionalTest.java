package ru.yandex.direct.jobs.bannersystem.export.job;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.testing.info.MobileContentInfo;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestMobileContents.defaultMobileContent;

@JobsTest
@ExtendWith(SpringExtension.class)
class BsExportMobileContentStatesJobFunctionalTest extends BaseBsExportMobileContentJobFunctionalTest {

    /**
     * Проверяем, что не отправляем повторно мобильный контент с StatusBsSynced.Yes
     */
    @Test
    void testJobNotSendingMobileContentInStatusBsSyncedYes() {
        MobileContentInfo mobileContentInfo = mobileContentSteps.createMobileContent(
                new MobileContentInfo()
                        .withMobileContent(defaultMobileContent()
                                .withStatusBsSynced(StatusBsSynced.YES))
        );

        performRequests(mobileContentInfo);

        checkBsSyncStatusIsCorrect(mobileContentInfo, StatusBsSynced.YES);
        assertThat(server.getRequestNum(REQUEST_PATH))
                .as("Запросов к Бк не производилось")
                .isEqualTo(0);
    }

    /**
     * Проверяем, что отправляем мобильный контент с StatusBsSynced.Sending
     */
    @Test
    void testJobSendingMobileContentInStatusBsSyncedSending() {
        MobileContentInfo mobileContentInfo = mobileContentSteps.createMobileContent(
                new MobileContentInfo()
                        .withMobileContent(defaultMobileContent()
                                .withStatusBsSynced(StatusBsSynced.SENDING))
        );

        performRequests(mobileContentInfo);

        checkBsSyncStatusIsCorrect(mobileContentInfo, StatusBsSynced.YES);
        assertThat(server.getRequestNum(REQUEST_PATH))
                .as("Был один запрос в БК")
                .isEqualTo(1);
    }

    /**
     * Проверяем, что отправляем мобильный контент с StatusBsSynced.No
     */
    @Test
    void testJobSendingMobileContentInStatusBsSyncedNo() {
        MobileContentInfo mobileContentInfo = mobileContentSteps.createMobileContent(
                new MobileContentInfo()
                        .withMobileContent(defaultMobileContent()
                                .withStatusBsSynced(StatusBsSynced.NO))
        );

        performRequests(mobileContentInfo);

        checkBsSyncStatusIsCorrect(mobileContentInfo, StatusBsSynced.YES);
        assertThat(server.getRequestNum(REQUEST_PATH))
                .as("Был один запрос в БК")
                .isEqualTo(1);
    }
}

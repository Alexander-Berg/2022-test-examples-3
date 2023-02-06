package ru.yandex.direct.jobs.bannersystem.export.job;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.testing.info.MobileContentInfo;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тестирование обработки ошибок экспортом мобильного контента в БК
 */
@JobsTest
@ExtendWith(SpringExtension.class)
class BsExportMobileContentErrorsJobFunctionalTest extends BaseBsExportMobileContentJobFunctionalTest {

    /**
     * Ожидаем, что, если БК отвечает ошибкой уровня запроса, статус синхронизации для отправленного объекта останется Sending
     */
    @Test
    void testJobWithResponseLevelError() {
        MobileContentInfo mobileContentInfo = mobileContentSteps.createDefaultMobileContent();

        server.addResponse(REQUEST_PATH, "{\"Error\":1,\"ErrorMessage\":\"Some error message\"}");

        job.runIterationsInLock(mobileContentInfo.getShard(), Integer.MAX_VALUE,
                Collections.singletonList(mobileContentInfo.getMobileContentId()), false);

        assertThat(server.getRequestNum(REQUEST_PATH))
                .as("Был один запрос в БК")
                .isEqualTo(1);
        checkBsSyncStatusIsCorrect(mobileContentInfo, StatusBsSynced.SENDING);
    }

    /**
     * Ожидаем, что, если БК отвечает невалидным JSON, статус синхронизации для отправленного объекта останется Sending
     */
    @Test
    void testJobWithCorruptedResponse() {
        MobileContentInfo mobileContentInfo = mobileContentSteps.createDefaultMobileContent();

        server.addResponse(REQUEST_PATH, "{Not a JSON");

        job.runIterationsInLock(mobileContentInfo.getShard(), Integer.MAX_VALUE,
                Collections.singletonList(mobileContentInfo.getMobileContentId()), false);

        assertThat(server.getRequestNum(REQUEST_PATH))
                .as("Был один запрос в БК")
                .isEqualTo(1);
        checkBsSyncStatusIsCorrect(mobileContentInfo, StatusBsSynced.SENDING);
    }

    /**
     * Ожидаем, что, если БК отвечает невалидным JSON, статус синхронизации для отправленного объекта останется Sending
     */
    @Test
    void testJobWithEmptyValidJsonResponse() {
        MobileContentInfo mobileContentInfo = mobileContentSteps.createDefaultMobileContent();

        server.addResponse(REQUEST_PATH, "{}");

        job.runIterationsInLock(mobileContentInfo.getShard(), Integer.MAX_VALUE,
                Collections.singletonList(mobileContentInfo.getMobileContentId()), false);

        assertThat(server.getRequestNum(REQUEST_PATH))
                .as("Был один запрос в БК")
                .isEqualTo(1);
        checkBsSyncStatusIsCorrect(mobileContentInfo, StatusBsSynced.SENDING);
    }

    /**
     * Ожидаем, что, если в ответе БК нет идентификатора отправленного объекта, статус синхронизации для него останется Sending
     */
    @Test
    void testJobWithWrongIdInResponse() {
        MobileContentInfo mobileContentInfo = mobileContentSteps.createDefaultMobileContent();

        server.addResponse(REQUEST_PATH,
                String.format("{\"Result\":[{\"mobile_app_id\":%s}]}", mobileContentInfo.getMobileContentId() + 5));

        job.runIterationsInLock(mobileContentInfo.getShard(), Integer.MAX_VALUE,
                Collections.singletonList(mobileContentInfo.getMobileContentId()), false);

        assertThat(server.getRequestNum(REQUEST_PATH))
                .as("Был один запрос в БК")
                .isEqualTo(1);
        checkBsSyncStatusIsCorrect(mobileContentInfo, StatusBsSynced.SENDING);
    }

    /**
     * Ожидаем, что при отправке двух обхектов, если БК отвечает ошибкой только для одного из них,
     * статус синхронизации для корректного объекта сменится на Yes, а для ошибочного останется Sending
     */
    @Test
    void testJobWithErrorForOneMobileContent() {
        MobileContentInfo mobileContentInfoOne = mobileContentSteps.createDefaultMobileContent();
        MobileContentInfo mobileContentInfoTwo =
                mobileContentSteps.createDefaultMobileContent(mobileContentInfoOne.getShard());

        server.addResponse(REQUEST_PATH, String.format(
                "{\"Result\":[{\"mobile_app_id\":%s},{\"mobile_app_id\":%s,\"Error\":1,\"ErrorMessage\":\"Some error message\"}]}",
                mobileContentInfoOne.getMobileContentId(), mobileContentInfoTwo.getMobileContentId()));

        job.runIterationsInLock(mobileContentInfoOne.getShard(), Integer.MAX_VALUE,
                Arrays.asList(mobileContentInfoOne.getMobileContentId(), mobileContentInfoTwo.getMobileContentId()),
                false);

        assertThat(server.getRequestNum(REQUEST_PATH))
                .as("Был один запрос в БК")
                .isEqualTo(1);
        checkBsSyncStatusIsCorrect(mobileContentInfoOne, StatusBsSynced.YES);
        // Статус у ошибки остался несброшенным
        checkBsSyncStatusIsCorrect(mobileContentInfoTwo, StatusBsSynced.SENDING);
    }

    /**
     * Ожидаем, что, если в ответе БК есть лишний идентификатор неотправленного объекта, статус синхронизации для этого
     * объекта останется No
     */
    @Test
    void testJobWithUnexpectedMobileContentId() {
        MobileContentInfo mobileContentInfoOne = mobileContentSteps.createDefaultMobileContent();
        MobileContentInfo mobileContentInfoTwo =
                mobileContentSteps.createDefaultMobileContent(mobileContentInfoOne.getShard());

        server.addResponse(REQUEST_PATH, String.format(
                "{\"Result\":[{\"mobile_app_id\":%s},{\"mobile_app_id\":%s}]}",
                mobileContentInfoOne.getMobileContentId(), mobileContentInfoTwo.getMobileContentId()));

        job.runIterationsInLock(mobileContentInfoOne.getShard(), Integer.MAX_VALUE,
                Collections.singletonList(mobileContentInfoOne.getMobileContentId()),
                false);

        assertThat(server.getRequestNum(REQUEST_PATH))
                .as("Был один запрос в БК")
                .isEqualTo(1);
        checkBsSyncStatusIsCorrect(mobileContentInfoOne, StatusBsSynced.YES);
        // Статус у лишнего ID остался несброшенным
        checkBsSyncStatusIsCorrect(mobileContentInfoTwo, StatusBsSynced.NO);
    }

    /**
     * Ожидаем, что, если БК отвечает UnDone уровня запроса, статус синхронизации для отправленного объекта останется Sending
     * <p>
     * ВАЖНО: На текущий момент БК не присылает UnDone никогда, но в перловом коде эта логика была поддержана, поэтому
     * поддерживаем ее и здесь
     */
    @Test
    void testUnDoneInResponseLevel() {
        MobileContentInfo mobileContentInfo = mobileContentSteps.createDefaultMobileContent();

        server.addResponse(REQUEST_PATH, "{\"UnDone\":1}");

        job.runIterationsInLock(mobileContentInfo.getShard(), Integer.MAX_VALUE,
                Collections.singletonList(mobileContentInfo.getMobileContentId()), false);

        assertThat(server.getRequestNum(REQUEST_PATH))
                .as("Был один запрос в БК")
                .isEqualTo(1);
        checkBsSyncStatusIsCorrect(mobileContentInfo, StatusBsSynced.SENDING);
    }

    /**
     * Ожидаем, что, если БК отвечает UnDone уровня объекта, статус синхронизации для него останется Sending
     * <p>
     * ВАЖНО: На текущий момент БК не присылает UnDone никогда, но в перловом коде эта логика была поддержана, поэтому
     * поддерживаем ее и здесь
     */
    @Test
    void testUnDoneInMobileContentLevel() {
        MobileContentInfo mobileContentInfo = mobileContentSteps.createDefaultMobileContent();

        server.addResponse(REQUEST_PATH,
                String.format("{\"Result\":[{\"mobile_app_id\":%s,\"Error\":1}]}",
                        mobileContentInfo.getMobileContentId()));

        job.runIterationsInLock(mobileContentInfo.getShard(), Integer.MAX_VALUE,
                Collections.singletonList(mobileContentInfo.getMobileContentId()), false);

        assertThat(server.getRequestNum(REQUEST_PATH))
                .as("Был один запрос в БК")
                .isEqualTo(1);
        checkBsSyncStatusIsCorrect(mobileContentInfo, StatusBsSynced.SENDING);
    }
}

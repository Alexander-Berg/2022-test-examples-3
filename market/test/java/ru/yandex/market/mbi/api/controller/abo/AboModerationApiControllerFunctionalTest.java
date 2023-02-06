package ru.yandex.market.mbi.api.controller.abo;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feature.model.FeatureCutoffType;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.open.api.model.ModerationVerdict;
import ru.yandex.market.mbi.open.api.model.PartnerIndexationStatusResponse;
import ru.yandex.market.mbi.open.api.model.PartnerLiteModerationResultRequest;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 23.03.2022
 */
@DbUnitDataSet(before = "AboModerationApiControllerFunctionalTest.before.csv")
public class AboModerationApiControllerFunctionalTest extends FunctionalTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private FeatureService featureService;

    @Test
    @DisplayName("Отправить магазин на индексацию")
    @DbUnitDataSet(after = "AboModerationApiControllerFunctionalTest.sendToIndexation.after.csv")
    void sendToIndexationTest() {
        sendToIndexationRequest(100);
    }

    @Test
    @DisplayName("Отправить магазин с фатальным катоффом на индексацию")
    void sendToIndexationForPartnerWithFatalCutoffTest() {
        var exception = assertThrows(
                HttpClientErrorException.BadRequest.class,
                () -> sendToIndexationRequest(200L)
        );
        MbiAsserts.assertXmlEquals(
                "<error><message>" +
                        "Indexation blocked for partner 200, because of fatal for placement cutoff" +
                        "</message></error>",
                exception.getResponseBodyAsString()
        );
    }

    @ParameterizedTest
    @CsvSource({"100, true", "200, false"})
    @DisplayName("Получить статус индексации партнеров")
    void getIndexationStatusTest(long partnerId, boolean isInIndex) {
        var indexationStatus = getIndexationStatus(partnerId);
        assertEquals(isInIndex, indexationStatus.getIsInProdIndex());
    }

    @Test
    @DisplayName("Проверить, что на неподдерживаемый тип партнера будет выброшено исключение")
    void getIndexationStatusForUnsupportedTypeTest() {
        var exception = assertThrows(
                HttpClientErrorException.BadRequest.class,
                () -> getIndexationStatus(300L)
        );
        MbiAsserts.assertXmlEquals(
                "<error><message>Unsupported partner type</message></error>",
                exception.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Проверить, что для неизвестного партнера будет выброшено исключение")
    void getIndexationStatusForUnknownPartnerTest() {
        var exception = assertThrows(
                HttpClientErrorException.BadRequest.class,
                () -> getIndexationStatus(400L)
        );
        MbiAsserts.assertXmlEquals(
                "<error><message>Could not find partner with id 400</message></error>",
                exception.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Проверить, что при успешной модерации будет закрыт катофф")
    void processSuccessVerdict() {
        var request = new PartnerLiteModerationResultRequest();
        request.setPartnerId(100L);
        request.setVerdict(ModerationVerdict.SUCCESS);
        sendModerationResultRequest(request);

        var cutoff = featureService.getCutoff(100, FeatureType.DROPSHIP, FeatureCutoffType.QUALITY_COMMON_OTHER);
        assertTrue(cutoff.isEmpty());
    }

    private void sendToIndexationRequest(long partnerId) {
        FunctionalTestHelper.post(baseUrl() + "/partner/" + partnerId + "/indexation/send");
    }

    private PartnerIndexationStatusResponse getIndexationStatus(long partnerId) {
        try {
            var response = FunctionalTestHelper.get(baseUrl() + "/partner/" + partnerId + "/indexation/status");
            return OBJECT_MAPPER.readValue(response.getBody(), PartnerIndexationStatusResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendModerationResultRequest(PartnerLiteModerationResultRequest request) {
        try {
            FunctionalTestHelper.postForJson(
                    baseUrl() + "/partner/moderation/lite/result", OBJECT_MAPPER.writeValueAsString(request)
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

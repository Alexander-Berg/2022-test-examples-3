package ru.yandex.market.mbi.api.controller.abo.testing;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.core.feature.model.cutoff.CommonCutoffs;
import ru.yandex.market.core.feature.model.cutoff.UtilityCutoffs;
import ru.yandex.market.core.moderation.passed.FatalFeatureCutoffFoundException;
import ru.yandex.market.core.testing.ShopProgram;
import ru.yandex.market.mbi.api.client.entity.abo.TurnOnWithoutTestingResponse;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.feature.model.FeatureType.MARKETPLACE_SELF_DELIVERY;

/**
 * Функциональные тесты на ручку
 * {@link ru.yandex.market.mbi.api.controller.AboCutoffController#turnOnWithoutTesting(long, ShopProgram, long)}.
 *
 * @author fbokovikov
 */
class TurnOnWithoutTestingFunctionalTest extends FunctionalTest {

    private static final long SHOP_ID = 774L;

    private static final String SHOP_IS_NOT_IN_TESTING = "Shop is not in a testing: datasourceId = 774";

    @Autowired
    private CheckouterAPI checkouterClient;

    @BeforeEach
    void init() {
        when(checkouterClient.shops()).thenReturn(mock(CheckouterShopApi.class));
    }

    /**
     * Сценарий 1: пытаемся включить без модерации магазин, не находящийся на модерации (формально -
     * отсутствует в таблице SHOPS_WEB.DATASOURCES_IN_TESTING)
     */
    @Test
    void turnOnShopIsNotInTesting() {
        TurnOnWithoutTestingResponse response = mbiApiClient.turnOnWithoutTesting(SHOP_ID, ShopProgram.CPC, 1L);
        TurnOnWithoutTestingResponse expectedResponse =
                new TurnOnWithoutTestingResponse(
                        SHOP_ID,
                        ShopProgram.CPC,
                        new RuntimeException(SHOP_IS_NOT_IN_TESTING));
        assertEquals(expectedResponse, response);
    }

    /**
     * Сценарий 2: пытаемся подключить без модерации магазин к CPA, при этом магазин в текущий момент
     * находится только на модерации по CPC
     */
    @Test
    @DbUnitDataSet(
            before = "turnOnNonExistentCpaCheck.before.csv",
            after = "turnOnNonExistentCpaCheck.after.csv"
    )
    void turnOnShopIsNotOnCpaModeration() {
        TurnOnWithoutTestingResponse response = mbiApiClient.turnOnWithoutTesting(SHOP_ID, ShopProgram.CPA, 1L);
        TurnOnWithoutTestingResponse expectedResponse =
                new TurnOnWithoutTestingResponse(
                        SHOP_ID,
                        ShopProgram.CPA,
                        new RuntimeException(SHOP_IS_NOT_IN_TESTING));
        assertEquals(expectedResponse, response);
    }

    /**
     * Сценарий 3: магазин находится на CPC-модерации, имеет набор отключений, позволяющий включиться без
     * модерации, также магазин имеет перечень CPA-отключений, убеждаемся, что магазин включился по CPC, а
     * CPA-отключения остались, как были
     */
    @Test
    @DbUnitDataSet(
            before = "turnOnCpcSuccess.before.csv",
            after = "turnOnCpcSuccess.after.csv"
    )
    void turnOnCpcSuccess() {
        TurnOnWithoutTestingResponse response = mbiApiClient.turnOnWithoutTesting(SHOP_ID, ShopProgram.CPC, 1L);
        TurnOnWithoutTestingResponse expectedResponse = new TurnOnWithoutTestingResponse(SHOP_ID, ShopProgram.CPC);
        assertEquals(expectedResponse, response);
    }

    /**
     * Сценарий 4: магазин находится на CPA-модерации, имеет набор отключений, позволяющий включиться без
     * модерации, также магазин имеет CPC-отключения, убеждаемся, что магазин включился по CPA, а также
     * CPC-отключения остались, как были
     */
    @Test
    @DbUnitDataSet(
            before = "turnOnCpaSuccess.before.csv",
            after = "turnOnCpaSuccess.after.csv"
    )
    void turnOnCpaSuccess() {
        TurnOnWithoutTestingResponse response = mbiApiClient.turnOnWithoutTesting(SHOP_ID, ShopProgram.CPA, 1L);
        TurnOnWithoutTestingResponse expectedResponse = new TurnOnWithoutTestingResponse(SHOP_ID, ShopProgram.CPA);
        assertEquals(expectedResponse, response);
    }

    /**
     * Сценарий 5: магазин находится на CPA-модерации, получил фатальное отключение за клоновость, таким
     * образом он не может включиться, пока не у него в админке (АБО или MBI) снимут отключение. Ручка
     * включения без модерации должна возвращать сообщение о том, что включение без модерации невозможно
     */
    @Test
    @DbUnitDataSet(
            before = "turnOnFailFatalCutoff.before.csv",
            after = "turnOnFailFatalCutoff.after.csv"
    )
    void turnOnFatalCutoff() {
        TurnOnWithoutTestingResponse response = mbiApiClient.turnOnWithoutTesting(SHOP_ID, ShopProgram.CPA, 1L);
        TurnOnWithoutTestingResponse expectedResponse = new TurnOnWithoutTestingResponse(SHOP_ID, ShopProgram.CPA,
                new FatalFeatureCutoffFoundException(SHOP_ID, MARKETPLACE_SELF_DELIVERY, Set.of(CommonCutoffs.FRAUD)));
        assertEquals(expectedResponse, response);
    }

    /**
     * Сценарий 6: магазину закрыли CPA фатальное отключение, поэтому у него осталось отключение
     * {@link UtilityCutoffs#NEED_TESTING}, а программа в статусе REVOKE, после чего его успешно
     * включают без модерации.
     */
    @Test
    @DbUnitDataSet(
            before = "turnOnCpaFromRevoke.before.csv",
            after = "turnOnCpaFromRevoke.after.csv"
    )
    void turnOnCpaFromRevoke() {
        TurnOnWithoutTestingResponse response = mbiApiClient.turnOnWithoutTesting(SHOP_ID, ShopProgram.CPA, 1L);
        TurnOnWithoutTestingResponse expectedResponse = new TurnOnWithoutTestingResponse(SHOP_ID, ShopProgram.CPA);
        assertEquals(expectedResponse, response);
    }

    @Test
    @DisplayName("Проверка ответа в xml-формате")
    @DbUnitDataSet(
            before = "turnOnCpaFromRevoke.before.csv",
            after = "turnOnCpaFromRevoke.after.csv"
    )
    void rawXmlSerializationTest() {
        ResponseEntity<String> response = FunctionalTestHelper.post(
                "http://localhost:" + port + "/turn-on-without-testing/774?shop_program=CPA&uid=1"
        );
        MbiAsserts.assertXmlEquals(
                //language=xml
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<turn_on_without_testing_response>\n" +
                        "    <datasource_id>774</datasource_id>\n" +
                        "    <shop_program>CPA</shop_program>\n" +
                        "    <result>SUCCESS</result>\n" +
                        "</turn_on_without_testing_response>",
                response.getBody()
        );
    }
}

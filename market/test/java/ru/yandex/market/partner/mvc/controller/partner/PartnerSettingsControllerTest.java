package ru.yandex.market.partner.mvc.controller.partner;

import java.util.concurrent.CompletableFuture;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.checkout.pushapi.client.PushApi;
import ru.yandex.market.checkout.pushapi.settings.Settings;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.state.event.PartnerChangesProtoLBEvent;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.data.GeneralData;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.partner.mvc.controller.partner.model.PartnerNameDTO;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.matchers.HttpClientErrorMatcher.errorListMatchesInAnyOrder;
import static ru.yandex.market.core.matchers.HttpClientErrorMatcher.errorMatches;
import static ru.yandex.market.core.matchers.HttpClientErrorMatcher.hasErrorCode;

/**
 * Тесты для {@link PartnerSettingsController}
 */
@ExtendWith(MockitoExtension.class)
class PartnerSettingsControllerTest extends FunctionalTest {
    private static final Gson GSON = new Gson();
    @Autowired
    private CheckouterClient checkouterClient;
    @Mock
    private CheckouterShopApi shopApi;
    @Autowired
    private PushApi pushApiClient;
    @Autowired
    private LogbrokerEventPublisher<PartnerChangesProtoLBEvent> logbrokerPartnerChangesEventPublisher;

    @BeforeEach
    void setUpMocks() {
        when(checkouterClient.shops()).thenReturn(shopApi);
        when(logbrokerPartnerChangesEventPublisher.publishEventAsync(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0)));
    }

    @Test
    @DbUnitDataSet(before = "TestChangeInternalName.before.csv",
            after = "TestChangeInternalName.supplier.after.csv")
    void testChangeInternalNameSupplier() {
        var partnerEventsCaptor = ArgumentCaptor.forClass(PartnerChangesProtoLBEvent.class);
        PartnerNameDTO partnerNameDTO = new PartnerNameDTO();
        partnerNameDTO.setInternalName("newTest");
        FunctionalTestHelper.put(
                baseUrl + "/partner/settings/internalName?campaignId={id}", partnerNameDTO, 555L
        );
        verify(logbrokerPartnerChangesEventPublisher, times(1)).publishEventAsync(partnerEventsCaptor.capture());
        Assertions.assertEquals(222L, partnerEventsCaptor.getValue().getPayload().getPartnerId());
        Assertions.assertEquals("newTest", partnerEventsCaptor.getValue().getPayload().getInternalName());
        Assertions.assertEquals(GeneralData.ActionType.UPDATE,
                partnerEventsCaptor.getValue().getPayload().getGeneralInfo().getActionType());
    }

    @Test
    @DbUnitDataSet(before = "TestChangeInternalName.before.csv",
            after = "TestChangeInternalName.shop.after.csv")
    void testChangeInternalNameShop() {
        PartnerNameDTO partnerNameDTO = new PartnerNameDTO();
        partnerNameDTO.setInternalName("newTest");
        FunctionalTestHelper.put(
                baseUrl + "/partner/settings/internalName?campaignId={id}", partnerNameDTO, 444L
        );
    }


    @ParameterizedTest
    @ValueSource(strings = {"test1", "test2"})
    @DbUnitDataSet(before = "TestChangeInternalName.before.csv",
            after = "TestChangeInternalName.before.csv")
    void testChangeInternalNameDuplicate(String name) {
        PartnerNameDTO partnerNameDTO = new PartnerNameDTO();
        partnerNameDTO.setInternalName(name);
        HttpClientErrorException e = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.put(
                        baseUrl + "/partner/settings/internalName?campaignId={id}", partnerNameDTO, 666L
                ));
        assertThat(e, hasErrorCode(HttpStatus.BAD_REQUEST));
        assertThat(e, errorListMatchesInAnyOrder(errorMatches("BAD_PARAM", "internalName", "ALREADY_EXISTS")));
    }

    @Test
    @DbUnitDataSet(before = "PartnerOrderProcessingSettingsControllerTest.before.csv")
    void testGetOrderSettings() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/partner/settings/orderProcessing?campaignId={id}",
                555L
        );
        assertResponseVsExpectedFile("PartnerOrderProcessingSettingsResponse.json", response);
    }

    @Test
    @DbUnitDataSet(before = "PartnerOrderProcessingSettingsControllerTestMixed.before.csv")
    void testGetOrderSettingsMixed() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/partner/settings/orderProcessing?campaignId={id}",
                556L
        );
        assertResponseVsExpectedFile("PartnerOrderProcessingSettingsMixedResponse.json", response);
    }

    @Test
    @DisplayName("Проверка, что при клике сохраняется param_value")
    @DbUnitDataSet(before = "PartnerOrderProcessingSettingsControllerTestMixed.before.csv",
            after = "PartnerOrderProcessingSettingsControllerTestMixed.after.csv")
    void saveButtonClickTest() {
        // shop
        FunctionalTestHelper.post(baseUrl + "/partner/settings/orderProcessing/confirm?campaignId=558");
        // double click
        FunctionalTestHelper.post(baseUrl + "/partner/settings/orderProcessing/confirm?campaignId=558");

        verify(shopApi, times(3)).updateShopData(
                eq(5093L),
                argThat(arg -> arg.getCampaignId() == 558L)
        );
        verify(pushApiClient).settings(eq(5093L), refEq(Settings.builder().partnerInterface(true).build()), eq(false));
    }

    @Test
    @DbUnitDataSet(before = "PartnerOrderProcessingSettingsControllerTest.before.csv")
    void testGetOrderProcessingSettingsForNewlyShop() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/partner/settings/orderProcessing?campaignId={id}",
                555L
        );
        assertResponseVsExpectedFile("PartnerOrderProcessingSettingsResponse.json", response);
    }

    @Test
    void testGetIncorrectCampaign() {
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(
                        baseUrl + "/partner/settings/orderProcessing?campaignId={id}",
                        557L
                )
        );
        Assertions.assertEquals(
                HttpStatus.NOT_FOUND,
                httpClientErrorException.getStatusCode()
        );
    }

    private void assertResponseVsExpectedFile(
            String expectedResponseFile,
            ResponseEntity<String> response
    ) {
        String expectedOutput = StringTestUtil.getString(getClass(), expectedResponseFile);
        MbiAsserts.assertJsonEquals(
                expectedOutput,
                GSON.fromJson(response.getBody(), JsonObject.class).get("result").toString()
        );
    }
}

package ru.yandex.market.partner.mvc.controller.partner.placement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.state.event.PartnerChangesProtoLBEvent;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.data.GeneralData;
import ru.yandex.market.mbi.data.PartnerDataOuterClass;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link PartnerPlacementController}
 */
public class PartnerPlacementControllerTest extends FunctionalTest {

    @Autowired
    private LogbrokerEventPublisher<PartnerChangesProtoLBEvent> logbrokerPartnerChangesEventPublisher;

    @BeforeEach
    void setUp() {
        when(logbrokerPartnerChangesEventPublisher.publishEventAsync(any(PartnerChangesProtoLBEvent.class)))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0)));
    }

    @Test
    @DbUnitDataSet(before = "PartnerPlacementControllerTest.before.csv",
            after = "PartnerPlacementControllerTest.after.csv")
    public void testOnCpc() {
        setPartnerPlacementModels(164L,
                new PartnerPlacementModelDTO(PartnerPlacementProgramDTO.CPC, PartnerPlacementActionDTO.ON));
    }

    @Test
    @DbUnitDataSet(before = "PartnerPlacementControllerTestOff.before.csv",
            after = "PartnerPlacementControllerTestOffCpc.after.csv")
    public void testOffCpc() {
        setPartnerPlacementModels(164L,
                new PartnerPlacementModelDTO(PartnerPlacementProgramDTO.CPC, PartnerPlacementActionDTO.OFF));
    }

    @Test
    @DbUnitDataSet(before = "PartnerPlacementControllerTest.before.csv",
            after = "PartnerPlacementControllerTestDsbs.after.csv")
    public void testOnDsbs() {
        setPartnerPlacementModels(164L,
                new PartnerPlacementModelDTO(PartnerPlacementProgramDTO.DSBS, PartnerPlacementActionDTO.ON));
    }

    @Test
    @DbUnitDataSet(before = "PartnerPlacementControllerTestOff.before.csv",
            after = "PartnerPlacementControllerTestOffDsbs.after.csv")
    public void testOffDsbs() {
        setPartnerPlacementModels(164L,
                new PartnerPlacementModelDTO(PartnerPlacementProgramDTO.DSBS, PartnerPlacementActionDTO.OFF));
    }

    @Test
    @DbUnitDataSet(before = "PartnerPlacementControllerTestOverActivated.before.csv",
            after = "PartnerPlacementControllerTestOverActivated.after.csv")
    public void testOverActivatedOffDsbs() {
        setPartnerPlacementModels(164L,
                new PartnerPlacementModelDTO(PartnerPlacementProgramDTO.DSBS, PartnerPlacementActionDTO.OFF));
    }

    @Test
    @DbUnitDataSet(before = "PartnerPlacementControllerTest.before.csv",
            after = "PartnerPlacementControllerTestCpcDsbs.after.csv")
    public void testOnCpcDsbs() {
        var partnerEventsCaptor = ArgumentCaptor.forClass(PartnerChangesProtoLBEvent.class);
        setPartnerPlacementModels(164L,
                new PartnerPlacementModelDTO(PartnerPlacementProgramDTO.CPC, PartnerPlacementActionDTO.ON),
                new PartnerPlacementModelDTO(PartnerPlacementProgramDTO.DSBS, PartnerPlacementActionDTO.ON)
        );
        verify(logbrokerPartnerChangesEventPublisher, times(1)).publishEventAsync(partnerEventsCaptor.capture());
        assertThat(partnerEventsCaptor.getValue().getPayload().getPartnerId()).isEqualTo(512L);
        assertThat(partnerEventsCaptor.getValue().getPayload().getPlacementProgramsList())
                .contains(
                        PartnerDataOuterClass.PlacementProgramType.CPC,
                        PartnerDataOuterClass.PlacementProgramType.DROPSHIP_BY_SELLER
                );
        assertThat(partnerEventsCaptor.getValue().getPayload().getGeneralInfo().getActionType())
                .isEqualTo(GeneralData.ActionType.UPDATE);
    }

    @Test
    @DbUnitDataSet(before = "PartnerPlacementControllerTest.switchOff.csv")
    public void testSwitchingOffAlreadySwitchedOffPlacements() {
        setPartnerPlacementModels(1000L,
                new PartnerPlacementModelDTO(PartnerPlacementProgramDTO.CPC, PartnerPlacementActionDTO.OFF),
                new PartnerPlacementModelDTO(PartnerPlacementProgramDTO.DSBS, PartnerPlacementActionDTO.OFF)
        );
    }


    @Test
    void testIncorrectCampaign() {
        PartnerPlacementModelListDTO list = new PartnerPlacementModelListDTO(new ArrayList<>() {{
            add(new PartnerPlacementModelDTO(PartnerPlacementProgramDTO.DSBS, PartnerPlacementActionDTO.ON));
        }});
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(
                        baseUrl + "/partner/placement?campaignId={id}",
                        list,
                        558L
                )
        );
        Assertions.assertEquals(
                HttpStatus.NOT_FOUND,
                httpClientErrorException.getStatusCode()
        );
    }

    @Test
    void testDuplicatedProgramsInRequest() {
        PartnerPlacementModelListDTO list = new PartnerPlacementModelListDTO(new ArrayList<>() {{
            add(new PartnerPlacementModelDTO(PartnerPlacementProgramDTO.DSBS, PartnerPlacementActionDTO.ON));
            add(new PartnerPlacementModelDTO(PartnerPlacementProgramDTO.DSBS, PartnerPlacementActionDTO.OFF));
        }});
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(
                        baseUrl + "/partner/placement?campaignId={id}",
                        list,
                        558L
                )
        );
        Assertions.assertEquals(
                HttpStatus.BAD_REQUEST,
                httpClientErrorException.getStatusCode()
        );
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerPlacementControllerTest.testOnCrossdock.before.csv",
            after = "PartnerPlacementControllerTest.testOnCrossdock.after.csv"
    )
    public void testOnCrossdock() {
        setPartnerPlacementModels(304304L,
                new PartnerPlacementModelDTO(PartnerPlacementProgramDTO.CROSSDOCK, PartnerPlacementActionDTO.ON));
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerPlacementControllerTest.testOnCrossdockWithFeature.before.csv",
            after = "PartnerPlacementControllerTest.testOnCrossdockWithFeature.after.csv"
    )
    public void testOnCrossdockWithFeature() {
        setPartnerPlacementModels(404404L,
                new PartnerPlacementModelDTO(PartnerPlacementProgramDTO.CROSSDOCK, PartnerPlacementActionDTO.ON));
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerPlacementControllerTest.testOnCrossdock.before.csv",
            after = "PartnerPlacementControllerTest.testOnOffCrossdock.after.csv"
    )
    public void testOnOffCrossdock() {
        setPartnerPlacementModels(304304L,
                new PartnerPlacementModelDTO(PartnerPlacementProgramDTO.CROSSDOCK, PartnerPlacementActionDTO.ON));
        setPartnerPlacementModels(304304L,
                new PartnerPlacementModelDTO(PartnerPlacementProgramDTO.CROSSDOCK, PartnerPlacementActionDTO.OFF));
    }


    @Test
    @DbUnitDataSet(
            before = "PartnerPlacementControllerTest.testOffCrossdock.before.csv",
            after = "PartnerPlacementControllerTest.testOffCrossdock.after.csv"
    )
    public void testOffCrossdock() {
        setPartnerPlacementModels(305305L,
                new PartnerPlacementModelDTO(PartnerPlacementProgramDTO.CROSSDOCK, PartnerPlacementActionDTO.OFF));
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerPlacementControllerTest.testDisableDSBSByDynamic.before.csv",
            after = "PartnerPlacementControllerTest.testDisableDSBSByDynamic.after.csv"
    )
    public void testDisableDSBSByDynamic() {
        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + "/partner/placement/status/OFF?campaignId={id}", "", 306306L);
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @DbUnitDataSet(
            before = "PartnerPlacementControllerTest.testActivateDSBSByDynamic.before.csv",
            after = "PartnerPlacementControllerTest.testActivateDSBSByDynamic.after.csv"
    )
    @Test
    public void testActivateDSBSByDynamic() {
        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + "/partner/placement/status/ON?campaignId={id}", "", 306306L);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"operationStatus\":\"SUCCESS\""));
    }

    @DbUnitDataSet(
            before = "PartnerPlacementControllerTest.testDSBSActivationFailed.before.csv",
            after = "PartnerPlacementControllerTest.testDSBSActivationFailed.after.csv"
    )
    @Test
    public void testDSBSActivationWarning() {
        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + "/partner/placement/status/ON?campaignId={id}", "", 306306L);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"operationStatus\":\"WARN_QUALITY_ISSUES\""));
    }

    @DbUnitDataSet(
            before = "PartnerPlacementControllerTest.testDSBSActivationOkOnIgnoredCutoffs.before.csv",
            after = "PartnerPlacementControllerTest.testDSBSActivationOkOnIgnoredCutoffs.after.csv"
    )
    @Test
    public void testDSBSActivationOkOnIgnoredCutoffs() {
        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + "/partner/placement/status/ON?campaignId={id}", "", 306306L);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"operationStatus\":\"SUCCESS\""));
    }

    @DbUnitDataSet(
            before = "PartnerPlacementControllerTest.testDSBSActivationFailWithIgnoredCutoff.before.csv",
            after = "PartnerPlacementControllerTest.testDSBSActivationFailWithIgnoredCutoff.after.csv"
    )
    @Test
    public void testDSBSActivationFailWithIgnoredCutoff() {
        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + "/partner/placement/status/ON?campaignId={id}", "", 306306L);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"operationStatus\":\"WARN_QUALITY_ISSUES\""));
    }

    @DbUnitDataSet(
            before = "PartnerPlacementControllerTest.testDSBSDisableOkOnOtherCutoffs.before.csv",
            after = "PartnerPlacementControllerTest.testDSBSDisableOkOnOtherCutoffs.after.csv"
    )
    @Test
    public void testDSBSDisableOkOnOtherCutoffs() {
        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + "/partner/placement/status/OFF?campaignId={id}", "", 306306L);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"operationStatus\":\"SUCCESS\""));
    }

    @DbUnitDataSet(
            before = "PartnerPlacementControllerTest.testDisableCrossdockByDynamic.before.csv",
            after = "PartnerPlacementControllerTest.testDisableCrossdockByDynamic.after.csv"
    )
    @Test
    public void testDisableCrossdockByDynamic() {
        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + "/partner/placement/status/OFF?campaignId={id}", "", 307307L);
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @DbUnitDataSet(
            before = "PartnerPlacementControllerTest.testActivateCrossdockByDynamic.before.csv",
            after = "PartnerPlacementControllerTest.testActivateCrossdockByDynamic.after.csv"
    )
    @Test
    public void testActivateCrossdockByDynamic() {
        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + "/partner/placement/status/ON?campaignId={id}", "", 307307L);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"operationStatus\":\"SUCCESS\""));
    }

    @DbUnitDataSet(
            before = "PartnerPlacementControllerTest.testDisableDropshipByDynamic.before.csv",
            after = "PartnerPlacementControllerTest.testDisableDropshipByDynamic.after.csv"
    )
    @Test
    public void testDisableDropshipByDynamic() {
        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + "/partner/placement/status/OFF?campaignId={id}", "", 307307L);
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @DbUnitDataSet(
            before = "PartnerPlacementControllerTest.testActivateDropshipByDynamic.before.csv",
            after = "PartnerPlacementControllerTest.testActivateDropshipByDynamic.after.csv"
    )
    @Test
    public void testActivateDropshipByDynamic() {
        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + "/partner/placement/status/ON?campaignId={id}", "", 307307L);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"operationStatus\":\"SUCCESS\""));
    }

    @DbUnitDataSet(
            before = "PartnerPlacementControllerTest.testDisableDSBSByDynamic.before.csv"
    )
    @Test
    public void testGetDSBSPartnerOnStatus() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/partner/placement/status?campaignId={id}", 306306L);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());

        assertTrue(response.getBody().contains("\"result\":\"ON\""));
    }

    @DbUnitDataSet(
            before = "PartnerPlacementControllerTest.testActivateDSBSByDynamic.before.csv"
    )
    @Test
    public void testGetDSBSPartnerOffStatus() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/partner/placement/status?campaignId={id}", 306306L);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());

        assertTrue(response.getBody().contains("\"result\":\"OFF\""));
    }

    @DbUnitDataSet(
            before = "PartnerPlacementControllerTest.testDisableCrossdockByDynamic.before.csv"
    )
    @Test
    public void testGetCrossdockPartnersOnStatus() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/partner/placement/status?campaignId={id}", 307307L);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());

        assertTrue(response.getBody().contains("\"result\":\"ON\""));
    }

    @DbUnitDataSet(
            before = "PartnerPlacementControllerTest.testActivateCrossdockByDynamic.before.csv"
    )
    @Test
    public void testGetCrossdockPartnersOffStatus() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/partner/placement/status?campaignId={id}", 307307L);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());

        assertTrue(response.getBody().contains("\"result\":\"OFF\""));
    }

    private void setPartnerPlacementModels(long campaignId, PartnerPlacementModelDTO... programs) {
        PartnerPlacementModelListDTO list = new PartnerPlacementModelListDTO(Arrays.asList(programs));
        ResponseEntity<String> response = FunctionalTestHelper.post(
                baseUrl + "/partner/placement?campaignId={id}", list, campaignId);
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}

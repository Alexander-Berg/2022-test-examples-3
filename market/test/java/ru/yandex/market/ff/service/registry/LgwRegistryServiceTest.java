package ru.yandex.market.ff.service.registry;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.dto.FfInboundRegistryDto;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.configuration.FeatureToggleTestConfiguration;
import ru.yandex.market.ff.model.dbqueue.GetFFInboundSuccessPayload;
import ru.yandex.market.ff.model.dbqueue.GetFFOutboundSuccessPayload;
import ru.yandex.market.ff.model.dbqueue.PutFFInboundRegistryPayload;
import ru.yandex.market.ff.model.dbqueue.PutRegistrySuccessPayload;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.service.ShopRequestModificationService;
import ru.yandex.market.ff.util.FileContentUtils;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

class LgwRegistryServiceTest extends IntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LgwRegistryService lgwRegistryService;

    @Autowired
    private ShopRequestModificationService shopRequestModificationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private FeatureToggleTestConfiguration.FTConfig ftConfig;

    @BeforeEach
    public void mockFeature() {
        ftConfig.setSupplyEnabled(false);
    }

    @Test
    @DatabaseSetup("classpath:service/registry/4/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/4/after.xml",
            assertionMode = NON_STRICT
    )
    public void acceptSuccessfulFFGetInbound() throws IOException {
        lgwRegistryService.processSuccessfulFFGetInboundResponse(
                getObjectFormJson("service/registry/4/request.json", GetFFInboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/29/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/29/after.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void acceptSuccessfulFFGetInboundMainSupplyFT() throws IOException {
        ftConfig.setSupplyEnabled(true);
        lgwRegistryService.processSuccessfulFFGetInboundResponse(
                getObjectFormJson("service/registry/29/request.json", GetFFInboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/5/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/5/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptSuccessfulFFGetInboundWithValidationErrors() throws IOException {
        lgwRegistryService.processSuccessfulFFGetInboundResponse(
                getObjectFormJson("service/registry/5/request.json", GetFFInboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/6/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/6/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptSuccessfulFFGetInboundWithAnomalyWithValidationErrors() throws IOException {
        lgwRegistryService.processSuccessfulFFGetInboundResponse(
                getObjectFormJson("service/registry/6/request.json", GetFFInboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/7/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/7/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptSuccessfulFFGetOutbound() throws IOException {
        lgwRegistryService.processSuccessfulFFGetOutboundResponse(
                getObjectFormJson("service/registry/7/request.json", GetFFOutboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/54/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/54/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptSuccessfulFFGetPrepared() throws IOException {
        lgwRegistryService.processSuccessfulFFGetOutboundResponse(
                getObjectFormJson("service/registry/54/request.json", GetFFOutboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/8/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/8/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptSuccessfulFFGetOutboundForInventorying() throws IOException {
        lgwRegistryService.processSuccessfulFFGetOutboundResponse(
                getObjectFormJson("service/registry/8/request.json", GetFFOutboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/9/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/9/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptSuccessfulFFGetOutboundForWithdrawing() throws IOException {
        lgwRegistryService.processSuccessfulFFGetOutboundResponse(
                getObjectFormJson("service/registry/9/request.json", GetFFOutboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/10/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/10/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptSuccessfulFFGetOutboundForWithdrawingWithDeliveredReturnRegistry() throws IOException {
        lgwRegistryService.processSuccessfulFFGetOutboundResponse(
                getObjectFormJson("service/registry/10/request.json", GetFFOutboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/11/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/11/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptSuccessfulFFGetOutboundForWithdrawingWithUndeliveredReturnRegistry() throws IOException {
        lgwRegistryService.processSuccessfulFFGetOutboundResponse(
                getObjectFormJson("service/registry/11/request.json", GetFFOutboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/12/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/12/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptSuccessfulFFGetOutboundWithValidationErrors() throws IOException {
        lgwRegistryService.processSuccessfulFFGetOutboundResponse(
                getObjectFormJson("service/registry/12/request.json", GetFFOutboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/17/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/17/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptSuccessfulSkippingDuplicate() throws IOException {
        lgwRegistryService.processSuccessfulFFGetInboundResponse(
                getObjectFormJson("service/registry/17/request.json", GetFFInboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/18/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/18/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptSuccessfulFFGetInboundWithItemsUpdate() throws IOException {
        lgwRegistryService.processSuccessfulFFGetInboundResponse(
                getObjectFormJson("service/registry/18/request.json", GetFFInboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/19/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/19/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptSuccessfulFFGetInboundWithItemsUpdateForMovement() throws IOException {
        lgwRegistryService.processSuccessfulFFGetInboundResponse(
                getObjectFormJson("service/registry/19/request.json", GetFFInboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/20/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/20/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptSuccessfulFFGetInboundWithItemsUpdateForMovementXDoc() throws IOException {
        setUpdateItemsFromRegistries(16);
        lgwRegistryService.processSuccessfulFFGetInboundResponse(
                getObjectFormJson("service/registry/20/request.json", GetFFInboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/22/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/22/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptSuccessfulRegistry() throws IOException {
        lgwRegistryService.acceptRegistryByService(
                getObjectFormJson("service/registry/22/request.json", PutRegistrySuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/40/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/40/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptSuccessfulRegistryAndSendServiceRequestIdToCS() throws IOException {
        lgwRegistryService.acceptRegistryByService(
                getObjectFormJson("service/registry/40/request.json", PutRegistrySuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/53/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/53/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptSuccessfulRegistryAndSkipSendingDueToCancelledStatus() throws IOException {
        lgwRegistryService.acceptRegistryByService(
                getObjectFormJson("service/registry/53/request.json", PutRegistrySuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/31/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/31/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptSuccessfulRegistryWithProcessedStatus() throws IOException {
        lgwRegistryService.acceptRegistryByService(
                getObjectFormJson("service/registry/31/request.json", PutRegistrySuccessPayload.class)
        );
        Mockito
            .verify(shopRequestModificationService, Mockito.times(0))
            .updateStatus(Mockito.any(ShopRequest.class), Mockito.eq(RequestStatus.PLAN_REGISTRY_ACCEPTED));
        Mockito
            .verify(publishToLogbrokerCalendarShopRequestChangeService)
            .onShopRequestStatusChange(
                Mockito.any(ShopRequest.class),
                Mockito.any(RequestStatus.class),
                Mockito.eq(RequestStatus.PLAN_REGISTRY_ACCEPTED),
                Mockito.any()
            );
    }

    @Test
    @DatabaseSetup("classpath:service/registry/23/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/23/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptSuccessfulFFGetInboundWithIdentifiers() throws IOException {
        jdbcTemplate.execute("alter sequence request_item_id_seq restart with 5");
        jdbcTemplate.execute("alter sequence unit_identifier_id_seq restart with 5");
        lgwRegistryService.processSuccessfulFFGetInboundResponse(
                getObjectFormJson("service/registry/23/request.json", GetFFInboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/24/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/24/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptSuccessfulFFGetInboundForInventorying() throws IOException {
        lgwRegistryService.processSuccessfulFFGetInboundResponse(
                getObjectFormJson("service/registry/24/request.json", GetFFInboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/26/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/26/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptSuccessfulFFGetOutboundForWithdrawingWithUpdatingTransferItemsIdentifiers() throws Exception {
        setUpdateItemsFromRegistries(12);
        lgwRegistryService.processSuccessfulFFGetOutboundResponse(
                getObjectFormJson("service/registry/26/request.json", GetFFOutboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/27/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/27/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptSuccessfulFFGetOutboundForItemWithConsignmentId() throws Exception {
        lgwRegistryService.processSuccessfulFFGetOutboundResponse(
                getObjectFormJson("service/registry/27/request.json", GetFFOutboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/28/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/28/after.xml",
            assertionMode = NON_STRICT
    )
    public void skipUpdatingCountsIfRegistryAlreadySaved() throws IOException {
        lgwRegistryService.processSuccessfulFFGetInboundResponse(
                getObjectFormJson("service/registry/28/request.json", GetFFInboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/30/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/30/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptSuccessfulFFGetInboundWithRestrictedData() throws Exception {
        lgwRegistryService.processSuccessfulFFGetInboundResponse(
                getObjectFormJson("service/registry/30/request.json", GetFFInboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/32/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/32/after.xml",
            assertionMode = NON_STRICT
    )
    void ffPutInboundRegistryWithShopRequestProcessedStatus() throws Exception {
        lgwRegistryService.ffPutInboundRegistry(
                getObjectFormJson("service/registry/32/request.json", FfInboundRegistryDto.class)
        );
        Mockito
            .verify(shopRequestModificationService, Mockito.times(0))
            .updateStatus(Mockito.any(ShopRequest.class), Mockito.eq(RequestStatus.PLAN_REGISTRY_CREATED));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/34/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/34/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptSuccessfulFFGetInboundForInventoryingWithItems() throws IOException {
        lgwRegistryService.processSuccessfulFFGetInboundResponse(
                getObjectFormJson("service/registry/34/request.json", GetFFInboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/34/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/34/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptSuccessfulFFGetInboundForReturn() throws Exception {
        lgwRegistryService.processSuccessfulFFGetInboundResponse(
                getObjectFormJson("service/registry/34/request.json", GetFFInboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/36/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/36/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptSuccessfulFFGetInboundWithItemsUpdateForCustomerReturn() throws IOException {
        lgwRegistryService.processSuccessfulFFGetInboundResponse(
                getObjectFormJson("service/registry/36/request.json", GetFFInboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/37/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/37/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptSuccessfulFFGetInboundWithItemsUpdateForCustomerReturnWithAnomaly() throws IOException {
        lgwRegistryService.processSuccessfulFFGetInboundResponse(
                getObjectFormJson("service/registry/37/request.json", GetFFInboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/38/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/38/after.xml",
            assertionMode = NON_STRICT
    )
    void ffPutInboundRegistryShouldSaveAndSendDocumentId() throws Exception {
        lgwRegistryService.ffPutInboundRegistry(
                getObjectFormJson("service/registry/38/request.json", FfInboundRegistryDto.class)
        );
        var captor = ArgumentCaptor.forClass(PutFFInboundRegistryPayload.class);
        Mockito.verify(putFFInboundRegistryQueueProducer).produceSingle(captor.capture());
        assertions.assertThat(captor.getValue().getInboundRegistry().getDocumentId()).isEqualTo("document-id");
    }

    @Test
    @DatabaseSetup("classpath:service/registry/39/before.xml")
    @ExpectedDatabase(
        value = "classpath:service/registry/39/after.xml",
        assertionMode = NON_STRICT
    )
    void acceptSuccessfulFFGetInboundWithItemsUpdateForInventoryingWithoutItems() throws IOException {
        lgwRegistryService.processSuccessfulFFGetInboundResponse(
            getObjectFormJson("service/registry/39/request.json", GetFFInboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/46/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/46/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptSuccessfulFFGetInboundWithoutRegistries() throws IOException {
        lgwRegistryService.processSuccessfulFFGetInboundResponse(
                getObjectFormJson("service/registry/46/request.json", GetFFInboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/41/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/41/after.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void acceptSuccessfulFFGetInboundForInitialReceivingDetails() throws Exception {
        lgwRegistryService.processSuccessfulFFGetInboundResponse(
                getObjectFormJson("service/registry/41/request.json", GetFFInboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/42/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/42/after.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void acceptSuccessfulFFGetInboundForInitialReceivingDetailsWithOtherStatusesInHistory() throws Exception {
        lgwRegistryService.processSuccessfulFFGetInboundResponse(
                getObjectFormJson("service/registry/42/request.json", GetFFInboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/43/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/43/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptSuccessfulFFGetOutboundForReturnRegistriesFromSc() throws IOException {
        lgwRegistryService.processSuccessfulFFGetOutboundResponse(
                getObjectFormJson("service/registry/43/request.json", GetFFOutboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/44/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/44/after.xml",
            assertionMode = NON_STRICT
    )
    public void acceptSuccessfulFFGetInboundWhenMultipleItemsInRequestItemOnCreation() throws IOException {
        jdbcTemplate.update("update request_subtype set update_items_from_registries = true where id = 16");
        lgwRegistryService.processSuccessfulFFGetInboundResponse(
                getObjectFormJson("service/registry/44/request.json", GetFFInboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/45/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/45/after.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void acceptSuccessfulFFGetInboundWithMultipleSameItemsFromExternalService() throws IOException {
        jdbcTemplate.update("update request_subtype set update_items_from_registries = true where id = 16");
        lgwRegistryService.processSuccessfulFFGetInboundResponse(
                getObjectFormJson("service/registry/45/request.json", GetFFInboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/47/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/47/after.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void acceptSuccessfulFFGetInboundForInitialAcceptanceWithoutRegistries() throws IOException {
        lgwRegistryService.processSuccessfulFFGetInboundResponse(
                getObjectFormJson("service/registry/47/request.json", GetFFInboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/48/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/48/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptSuccessfulFFGetInboundForInitialAcceptanceWithRegistriesOnlySecondaryAcceptanceRegister()
            throws IOException {
        lgwRegistryService.processSuccessfulFFGetInboundResponse(
                getObjectFormJson("service/registry/48/request.json", GetFFInboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/49/before.xml")
    @ExpectedDatabase(
        value = "classpath:service/registry/49/after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void acceptSuccessfulFFGetInboundForInitialReceivingDetailsOnlyPallets() throws Exception {
        lgwRegistryService.processSuccessfulFFGetInboundResponse(
            getObjectFormJson("service/registry/49/request.json", GetFFInboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("/service/registry/50/before.xml")
    @ExpectedDatabase(
            value = "/service/registry/50/after.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void acceptSuccessfulFFGetInboundForInitialReceivingUpdateOrderId() throws Exception {
            lgwRegistryService.processSuccessfulFFGetInboundResponse(
                getObjectFormJson("service/registry/50/request.json", GetFFInboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("/service/registry/51/before.xml")
    @ExpectedDatabase(
            value = "/service/registry/51/after.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void createInternalRequestForUpdatableCustomerReturn() throws Exception {
        lgwRegistryService.processSuccessfulFFGetInboundResponse(
                getObjectFormJson("service/registry/51/request.json", GetFFInboundSuccessPayload.class));
    }

    @Test
    @DatabaseSetup("classpath:service/registry/52/before.xml")
    @ExpectedDatabase(
            value = "classpath:service/registry/52/after.xml",
            assertionMode = NON_STRICT
    )
    void acceptRegistryByServiceChildRegistriesInsteadOfParent() throws Exception {
        jdbcTemplate.execute("update request_subtype set use_parent_request_id_for_send_to_service = true" +
                " where subtype in ('CUSTOMER_RETURN_ENRICHMENT')");
        lgwRegistryService.acceptRegistryByService(
                getObjectFormJson("service/registry/52/request.json", PutRegistrySuccessPayload.class));
    }

    private <T> T getObjectFormJson(String name, Class<T> clazz) throws IOException {
        return objectMapper.readValue(getJsonFromFile(name), clazz);
    }

    private String getJsonFromFile(final String name) throws IOException {
        return FileContentUtils.getFileContent(name);
    }

    private void setUpdateItemsFromRegistries(long typeId) {
        jdbcTemplate.update(
                String.format("update request_subtype set update_items_from_registries = true where id = %d", typeId));
    }

}

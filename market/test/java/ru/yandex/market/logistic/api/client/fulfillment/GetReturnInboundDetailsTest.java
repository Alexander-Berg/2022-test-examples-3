package ru.yandex.market.logistic.api.client.fulfillment;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.CommonServiceClientTest;
import ru.yandex.market.logistic.api.client.FulfillmentClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.common.CompositeId;
import ru.yandex.market.logistic.api.model.common.PartialId;
import ru.yandex.market.logistic.api.model.common.PartialIdType;
import ru.yandex.market.logistic.api.model.fulfillment.InboundUnitDefect;
import ru.yandex.market.logistic.api.model.fulfillment.InboundUnitDefectType;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetReturnInboundDetailsResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.InboundUnitDetails;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.ReturnBoxDetails;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.ReturnInboundDetails;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.ReturnUnitDetails;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Тест для {@link FulfillmentClient#getReturnInboundDetails(ResourceId, PartnerProperties)}
 */
public class GetReturnInboundDetailsTest extends CommonServiceClientTest {

    private static final String YANDEX_ID = "12345";
    private static final String PARTNER_ID = "Zakaz";

    @Test
    void testGetReturnInboundDetailsSucceeded() throws Exception {
        prepareMockServiceNormalized("ff_get_return_inbound_details", PARTNER_URL);

        GetReturnInboundDetailsResponse response =
                fulfillmentClient.getReturnInboundDetails(new ResourceId.ResourceIdBuilder()
                                .setYandexId(YANDEX_ID)
                                .setPartnerId(PARTNER_ID)
                                .setFulfillmentId(PARTNER_ID)
                                .build(),
                        getPartnerProperties());

        assertEquals(
                getExpectedResponse(),
                response,
                "Должен вернуть корректный ответ GetReturnInboundDetailsResponse"
        );
    }

    @Test
    void testGetReturnInboundDetailsWithErrors() throws Exception {
        prepareMockServiceNormalized(
                "ff_get_return_inbound_details",
                "ff_get_return_inbound_details_with_errors",
                PARTNER_URL);

        assertThrows(
                RequestStateErrorException.class,
                () -> fulfillmentClient.getReturnInboundDetails(new ResourceId.ResourceIdBuilder()
                                .setYandexId(YANDEX_ID)
                                .setPartnerId(PARTNER_ID)
                                .setFulfillmentId(PARTNER_ID)
                                .build(),
                        getPartnerProperties())
        );
    }

    private GetReturnInboundDetailsResponse getExpectedResponse() {

        ResourceId inboundId = new ResourceId.ResourceIdBuilder()
                .setYandexId(YANDEX_ID)
                .setPartnerId(PARTNER_ID)
                .setFulfillmentId(PARTNER_ID)
                .build();

        UnitId unitId1 = new UnitId.UnitIdBuilder(123L, "SKU1")
                .setId("SKU1")
                .build();
        UnitId unitId2 = new UnitId.UnitIdBuilder(124L, "SKU2")
                .setId("SKU2")
                .build();

        ReturnBoxDetails returnBoxDetails1 = new ReturnBoxDetails.ReturnBoxDetailsBuilder("P00012321")
                .setOrderId("OrderId1")
                .build();
        ReturnBoxDetails returnBoxDetails2 = new ReturnBoxDetails.ReturnBoxDetailsBuilder("P00012322")
                .build();
        ReturnBoxDetails returnBoxDetails3 = new ReturnBoxDetails.ReturnBoxDetailsBuilder("P00012323")
                .setOrderId("OrderId1")
                .build();
        ReturnBoxDetails returnBoxDetails4 = new ReturnBoxDetails.ReturnBoxDetailsBuilder("P00012324")
                .setOrderId("OrderId2")
                .build();

        List<CompositeId> instances = ImmutableList.of(
            new CompositeId(ImmutableList.of(new PartialId(PartialIdType.CIS, "cis123"))),
            new CompositeId(ImmutableList.of(new PartialId(PartialIdType.CIS, "cis222")))
        );

        List<CompositeId> unfitInstances = ImmutableList.of(
            new CompositeId(ImmutableList.of(new PartialId(PartialIdType.CIS, "cis-unfit-123"))),
            new CompositeId(ImmutableList.of(new PartialId(PartialIdType.CIS, "cis-unfit-222")))
        );

        InboundUnitDetails inboundUnitDetails1 = new InboundUnitDetails(unitId1, 6, 66, 0, 60);
        inboundUnitDetails1.setInstances(instances);
        inboundUnitDetails1.setUnfitInstances(unfitInstances);
        InboundUnitDetails inboundUnitDetails2 = new InboundUnitDetails(unitId2, 7, 77, 2, 70);
        inboundUnitDetails2.setInstances(instances);
        inboundUnitDetails2.setUnfitInstances(unfitInstances);
        ReturnUnitDetails returnUnitDetails1 = new ReturnUnitDetails.ReturnUnitDetailsBuilder(
                "OrderId1", inboundUnitDetails1)
                .setBoxIds(Arrays.asList("P00012321", "P00012323"))
                .setInboundUnitDefects(Arrays.asList(
                        new InboundUnitDefect(unitId1, InboundUnitDefectType.DEFORMED, 1),
                        new InboundUnitDefect(unitId1, InboundUnitDefectType.DISPLAY_BROKEN, 2))
                )
                .build();
        ReturnUnitDetails returnUnitDetails2 = new ReturnUnitDetails.ReturnUnitDetailsBuilder(
                "OrderId2", inboundUnitDetails2).build();

        ReturnInboundDetails returnInboundDetails = new ReturnInboundDetails.ReturnInboundDetailsBuilder(
                inboundId,
                Arrays.asList(returnBoxDetails1, returnBoxDetails2, returnBoxDetails3, returnBoxDetails4))
                .setReturnUnitDetailsList(Arrays.asList(returnUnitDetails1, returnUnitDetails2))
                .build();

        return new GetReturnInboundDetailsResponse(returnInboundDetails);
    }
}

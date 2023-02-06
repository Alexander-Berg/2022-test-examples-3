package ru.yandex.travel.api.services.avia.testcontext;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;

import ru.yandex.avia.booking.partners.gateways.BookingGateway;
import ru.yandex.avia.booking.partners.gateways.aeroflot.converter.AeroflotVariantConverter;
import ru.yandex.avia.booking.partners.gateways.aeroflot.model.AeroflotVariant;
import ru.yandex.avia.booking.partners.gateways.model.availability.AvailabilityCheckRequest;
import ru.yandex.avia.booking.partners.gateways.model.availability.AvailabilityCheckResponse;
import ru.yandex.avia.booking.partners.gateways.model.availability.VariantNotAvailableException;
import ru.yandex.avia.booking.partners.gateways.model.booking.ServicePayload;
import ru.yandex.avia.booking.partners.gateways.model.booking.ServicePayloadInitParams;
import ru.yandex.avia.booking.partners.gateways.model.search.Variant;
import ru.yandex.travel.orders.commons.proto.EAviaCheckAvailabilityOnRedirOutcome;
import ru.yandex.travel.orders.commons.proto.TAviaTestContext;

import static ru.yandex.travel.orders.commons.proto.EAviaCheckAvailabilityOnRedirOutcome.CAOR_NOT_AVAILABLE;
import static ru.yandex.travel.orders.commons.proto.EAviaCheckAvailabilityOnRedirOutcome.CAOR_PRICE_CHANGED;

public class MockAeroflotGateway implements BookingGateway {
    private final BookingGateway gateway;
    private final EAviaCheckAvailabilityOnRedirOutcome availabilityOutcome;

    public MockAeroflotGateway(BookingGateway gateway, TAviaTestContext testContext) {
        this.gateway = gateway;
        this.availabilityOutcome = testContext.getCheckAvailabilityOnRedirOutcome();
    }

    @Override
    public AvailabilityCheckResponse checkAvailabilityAll(AvailabilityCheckRequest request) throws VariantNotAvailableException {
        if (availabilityOutcome == CAOR_NOT_AVAILABLE) {
            throw new VariantNotAvailableException("Variant not available cause of testContext");
        }

        AeroflotVariant aeroflotVariantToken = ensureAeroflotVariant(request.getToken());
        if (availabilityOutcome == CAOR_PRICE_CHANGED) {
            aeroflotVariantToken = MockAeroflotVariantHelper.changeVariantPrice(aeroflotVariantToken);
        }
        return AvailabilityCheckResponse.builder()
                .variant(AeroflotVariantConverter.convertVariant(aeroflotVariantToken))
                .build();
    }

    private AeroflotVariant ensureAeroflotVariant(Object variant) {
        Preconditions.checkArgument(variant instanceof AeroflotVariant,
                "Only aeroflot variants are supported at the moment but got %s", variant.getClass().getName());
        return (AeroflotVariant) variant;
    }

    @Override
    public Object resolveVariantInfo(JsonNode node) {
        return gateway.resolveVariantInfo(node);
    }

    @Override
    public Object resolveVariantInfoAndOptimizeJson(JsonNode node) {
        return gateway.resolveVariantInfoAndOptimizeJson(node);
    }

    @Override
    public void synchronizeUpdatedVariantInfoJson(JsonNode node, Variant variantInfo) {
        gateway.synchronizeUpdatedVariantInfoJson(node, variantInfo);
    }

    @Override
    public String getExternalVariantId(Object variantInfo) {
        return gateway.getExternalVariantId(variantInfo);
    }

    @Override
    public ServicePayload createServicePayload(ServicePayloadInitParams params) {
        return gateway.createServicePayload(params);
    }

    @Override
    public Class<? extends ServicePayload> getPayloadType() {
        return gateway.getPayloadType();
    }
}

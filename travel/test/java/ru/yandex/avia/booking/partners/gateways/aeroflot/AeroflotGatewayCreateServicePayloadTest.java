package ru.yandex.avia.booking.partners.gateways.aeroflot;

import java.util.List;

import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.avia.booking.partners.gateways.model.booking.ServicePayload;
import ru.yandex.avia.booking.partners.gateways.model.booking.ServicePayloadInitParams;
import ru.yandex.avia.booking.partners.gateways.model.booking.TravellerInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.avia.booking.partners.gateways.aeroflot.AeroflotApiStubsHelper.loadSampleTdRequestNdcV3SingleTariff;
import static ru.yandex.avia.booking.partners.gateways.aeroflot.AeroflotApiStubsHelper.testClientInfo;

class AeroflotGatewayCreateServicePayloadTest {
    //private AeroflotGateway gateway = Mockito.mock(AeroflotGateway.class, Mockito.CALLS_REAL_METHODS);
    private final AeroflotGateway gateway = AeroflotApiStubsHelper.defaultGateway("unused");

    @Test
    void createServicePayloadConvertTravellersPassport() {
        TravellerInfo testTraveller = TravellerInfo.builder().documentNumber("1234567890").build();
        ServicePayload payload = gateway.createServicePayload(testPayloadParams(testTraveller));
        // nothing should be changed in the traveller info
        assertThat(payload.getTravellers().get(0)).isEqualTo(testTraveller);
    }

    @Test
    void createServicePayloadConvertTravellersRuBirthCertificate() {
        AeroflotGateway gateway = Mockito.mock(AeroflotGateway.class, Mockito.CALLS_REAL_METHODS);
        ServicePayload payload = gateway.createServicePayload(testPayloadParams(
                TravellerInfo.builder().documentNumber("IIIЯХ234567").build()));
        assertThat(payload.getTravellers().get(0).getDocumentNumber()).isEqualTo("IIIIAKH234567");
    }

    ServicePayloadInitParams testPayloadParams(TravellerInfo traveller) {
        return ServicePayloadInitParams.builder()
                .variantToken(gateway.resolveVariantInfo(loadSampleTdRequestNdcV3SingleTariff()))
                .preliminaryPrice(Money.of(10, "RUB"))
                .travellers(List.of(traveller))
                .clientInfo(testClientInfo())
                .build();
    }
}

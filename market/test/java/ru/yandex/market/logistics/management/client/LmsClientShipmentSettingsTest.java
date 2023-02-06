package ru.yandex.market.logistics.management.client;

import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.entity.request.partner.PlatformClientPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerShipmentSettingsDto;
import ru.yandex.market.logistics.management.entity.response.partner.PlatformClientPartnerDto;
import ru.yandex.market.logistics.management.entity.type.AllowedShipmentWay;
import ru.yandex.market.logistics.management.entity.type.ShipmentType;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;

class LmsClientShipmentSettingsTest extends AbstractClientTest {

    @Test
    @DisplayName("Поиск с пустым фильтром")
    void getPartnerShipmentSettingsEmptyFilter() {
        mockServer.expect(requestTo(uri + "/externalApi/partner-platform-settings/search"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json("{}"))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/shipmentSettings/all.json"))
            );

        List<PlatformClientPartnerDto> shipmentSettingsDtos = client.searchPartnerPlatformSettings(
            PlatformClientPartnerFilter.newBuilder().build()
        );

        softly.assertThat(shipmentSettingsDtos)
            .usingRecursiveFieldByFieldElementComparator()
            .isEqualTo(
                ImmutableList.of(
                    PlatformClientPartnerDto.newBuilder()
                        .id(1L)
                        .partnerId(2L)
                        .platformClientId(3903L)
                        .shipmentSettings(ImmutableSet.of(
                            createShipmentSettings(ShipmentType.IMPORT, AllowedShipmentWay.VIA_SC)
                        ))
                        .build(),
                    PlatformClientPartnerDto.newBuilder()
                        .id(2L)
                        .partnerId(2L)
                        .platformClientId(3901L)
                        .shipmentSettings(ImmutableSet.of(
                            createShipmentSettings(ShipmentType.WITHDRAW, AllowedShipmentWay.DIRECTLY),
                            createShipmentSettings(ShipmentType.WITHDRAW, AllowedShipmentWay.VIA_SC),
                            createShipmentSettings(ShipmentType.IMPORT, AllowedShipmentWay.DIRECTLY),
                            createShipmentSettings(ShipmentType.IMPORT, AllowedShipmentWay.VIA_SC)
                        ))
                        .build(),
                    PlatformClientPartnerDto.newBuilder()
                        .id(3L)
                        .partnerId(4L)
                        .platformClientId(3903L)
                        .shipmentSettings(ImmutableSet.of(
                            createShipmentSettings(ShipmentType.IMPORT, AllowedShipmentWay.VIA_SC)
                        ))
                        .build(),
                    PlatformClientPartnerDto.newBuilder()
                        .id(4L)
                        .partnerId(4L)
                        .platformClientId(3901L)
                        .shipmentSettings(ImmutableSet.of(
                            createShipmentSettings(ShipmentType.WITHDRAW, AllowedShipmentWay.DIRECTLY)
                        ))
                        .build()
                )
            );
    }

    @Test
    @DisplayName("Поиск с заполненным фильтром")
    void getPartnerShipmentSettings() {
        mockServer.expect(requestTo(uri + "/externalApi/partner-platform-settings/search"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json(
                jsonResource("data/controller/shipmentSettings/search_shipment_settings_filter.json"))
            )
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/shipmentSettings/empty.json"))
            );

        List<PlatformClientPartnerDto> shipmentSettingsDtos = client.searchPartnerPlatformSettings(
            PlatformClientPartnerFilter.newBuilder()
                .partnerIds(ImmutableSet.of(1L, 2L))
                .platformClientIds(ImmutableSet.of(3901L, 3903L))
                .build()
        );
        softly.assertThat(shipmentSettingsDtos).hasSize(0);
    }

    @Nonnull
    private PartnerShipmentSettingsDto createShipmentSettings(
        ShipmentType shipmentType,
        AllowedShipmentWay allowedShipmentWay
    ) {
        return PartnerShipmentSettingsDto.newBuilder()
            .shipmentType(shipmentType)
            .allowedShipmentWay(allowedShipmentWay)
            .build();
    }
}

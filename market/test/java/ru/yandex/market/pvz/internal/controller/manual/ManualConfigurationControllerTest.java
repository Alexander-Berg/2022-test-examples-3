package ru.yandex.market.pvz.internal.controller.manual;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.pvz.core.domain.configuration.ConfigurableEntityType;
import ru.yandex.market.pvz.core.domain.configuration.ConfigurationProviderSource;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.configuration.legal_partner.ConfigurationLegalPartnerCommandService;
import ru.yandex.market.pvz.core.domain.configuration.pickup_point.ConfigurationPickupPointCommandService;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.internal.BaseShallowTest;
import ru.yandex.market.pvz.internal.WebLayerTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pvz.core.TestUtils.getFileContent;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointCourierMappingFactory.PickupPointCourierMappingTestParams.DEFAULT_COURIER_DELIVERY_SERVICE_ID;
import static ru.yandex.market.tpl.common.util.StringFormatter.formatVars;

@WebLayerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ManualConfigurationControllerTest extends BaseShallowTest {

    private static final String KEY = "CONF_KEY";
    private static final String VALUE = "CONF_VALUE";

    private final TestPickupPointFactory pickupPointFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;

    private final ConfigurationLegalPartnerCommandService configurationLegalPartnerCommandService;
    private final ConfigurationPickupPointCommandService configurationPickupPointCommandService;

    private final ConfigurationGlobalCommandService configurationGlobalCommandService;
    private final ConfigurationProviderSource configurationProviderSource;

    @Test
    @SneakyThrows
    void testSetValueForPickupPoint() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        String body = formatVars(getFileContent("manual/configuration_set.json"), Map.of(
                "type", ConfigurableEntityType.PICKUP_POINT.name(),
                "entityIds", pickupPoint.getId()
        ));

        mockMvc.perform(post("/manual/configuration/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful());
        configurationProviderSource.changeCacheVersion();

        assertThat(configurationProviderSource.getForPickupPoint(pickupPoint.getId()).getValue(KEY))
                .hasValue(VALUE);
    }

    @Test
    @SneakyThrows
    void testSetValueForLegalPartner() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        String body = formatVars(getFileContent("manual/configuration_set.json"), Map.of(
                "type", ConfigurableEntityType.LEGAL_PARTNER.name(),
                "entityIds", legalPartner.getId()
        ));

        mockMvc.perform(post("/manual/configuration/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful());
        configurationProviderSource.changeCacheVersion();

        assertThat(configurationProviderSource.getForLegalPartner(legalPartner.getId()).getValue(KEY)).hasValue(VALUE);
    }

    @Test
    @SneakyThrows
    void testSetValueForMiddleMile() {
        String body = formatVars(getFileContent("manual/configuration_set.json"), Map.of(
                "type", ConfigurableEntityType.MIDDLE_MILE.name(),
                "entityIds", DEFAULT_COURIER_DELIVERY_SERVICE_ID
        ));

        mockMvc.perform(post("/manual/configuration/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful());
        configurationProviderSource.changeCacheVersion();

        assertThat(configurationProviderSource.getForMiddleMile(DEFAULT_COURIER_DELIVERY_SERVICE_ID).getValue(KEY))
                .hasValue(VALUE);
    }

    @Test
    @SneakyThrows
    void testResetValueForPickupPoint() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        configurationPickupPointCommandService.setValue(pickupPoint.getId(), KEY, VALUE);
        assertThat(configurationProviderSource.getForPickupPoint(pickupPoint.getId()).getValue(KEY)).hasValue(VALUE);

        String body = formatVars(getFileContent("manual/configuration_reset.json"), Map.of(
                "type", ConfigurableEntityType.PICKUP_POINT.name(),
                "entityIds", pickupPoint.getId()
        ));

        mockMvc.perform(post("/manual/configuration/remove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful());
        configurationProviderSource.changeCacheVersion();

        assertThat(configurationProviderSource.getForLegalPartner(pickupPoint.getId()).getValue(KEY)).isEmpty();
    }

    @Test
    @SneakyThrows
    void testResetValueForLegalPartner() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        configurationLegalPartnerCommandService.setValue(legalPartner.getId(), KEY, VALUE);
        assertThat(configurationProviderSource.getForLegalPartner(legalPartner.getId()).getValue(KEY)).hasValue(VALUE);

        String body = formatVars(getFileContent("manual/configuration_reset.json"), Map.of(
                "type", ConfigurableEntityType.LEGAL_PARTNER.name(),
                "entityIds", legalPartner.getId()
        ));

        mockMvc.perform(post("/manual/configuration/remove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful());
        configurationProviderSource.changeCacheVersion();

        assertThat(configurationProviderSource.getForLegalPartner(legalPartner.getId()).getValue(KEY)).isEmpty();
    }

    @Test
    @SneakyThrows
    void testResetValueForMiddleMile() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        configurationPickupPointCommandService.setValue(pickupPoint.getId(), KEY, VALUE);
        assertThat(configurationProviderSource.getForPickupPoint(pickupPoint.getId()).getValue(KEY)).hasValue(VALUE);

        String body = formatVars(getFileContent("manual/configuration_reset.json"), Map.of(
                "type", ConfigurableEntityType.MIDDLE_MILE.name(),
                "entityIds", DEFAULT_COURIER_DELIVERY_SERVICE_ID
        ));

        mockMvc.perform(post("/manual/configuration/remove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful());
        configurationProviderSource.changeCacheVersion();

        assertThat(configurationProviderSource.getForMiddleMile(DEFAULT_COURIER_DELIVERY_SERVICE_ID).getValue(KEY))
                .isEmpty();
    }

    @Test
    @SneakyThrows
    void testNullEntityIdsThrowsException() {
        mockMvc.perform(post("/manual/configuration/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(formatVars(getFileContent("manual/configuration_set.json"), Map.of(
                                "type", ConfigurableEntityType.LEGAL_PARTNER.name(),
                                "entityIds", ""
                        ))))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(post("/manual/configuration/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(formatVars(getFileContent("manual/configuration_set.json"), Map.of(
                                "type", ConfigurableEntityType.PICKUP_POINT.name(),
                                "entityIds", ""
                        ))))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(post("/manual/configuration/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(formatVars(getFileContent("manual/configuration_set.json"), Map.of(
                                "type", ConfigurableEntityType.MIDDLE_MILE.name(),
                                "entityIds", ""
                        ))))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(post("/manual/configuration/remove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(formatVars(getFileContent("manual/configuration_reset.json"), Map.of(
                                "type", ConfigurableEntityType.LEGAL_PARTNER.name(),
                                "entityIds", ""
                        ))))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(post("/manual/configuration/remove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(formatVars(getFileContent("manual/configuration_reset.json"), Map.of(
                                "type", ConfigurableEntityType.PICKUP_POINT.name(),
                                "entityIds", ""
                        ))))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(post("/manual/configuration/remove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(formatVars(getFileContent("manual/configuration_reset.json"), Map.of(
                                "type", ConfigurableEntityType.MIDDLE_MILE.name(),
                                "entityIds", ""
                        ))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @SneakyThrows
    void testSetNullRaisesError() {
        String body = formatVars(getFileContent("manual/configuration_set_null.json"), Map.of(
                "type", ConfigurableEntityType.GLOBAL.name(),
                "entityIds", ""
        ));

        mockMvc.perform(post("/manual/configuration/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @SneakyThrows
    void testSetGlobalValue() {
        String body = formatVars(getFileContent("manual/configuration_set.json"), Map.of(
                "type", ConfigurableEntityType.GLOBAL.name(),
                "entityIds", ""
        ));

        mockMvc.perform(post("/manual/configuration/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful());
        configurationProviderSource.changeCacheVersion();

        assertThat(configurationProviderSource.getGlobal().getValue(KEY)).hasValue(VALUE);
    }

    @Test
    @SneakyThrows
    void testResetGlobalValue() {
        configurationGlobalCommandService.setValue(KEY, VALUE);
        assertThat(configurationProviderSource.getGlobal().getValue(KEY)).hasValue(VALUE);

        String body = formatVars(getFileContent("manual/configuration_reset.json"), Map.of(
                "type", ConfigurableEntityType.GLOBAL.name(),
                "entityIds", ""
        ));

        mockMvc.perform(post("/manual/configuration/remove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful());
        configurationProviderSource.changeCacheVersion();

        assertThat(configurationProviderSource.getGlobal().getValue(KEY)).isEmpty();
    }
}

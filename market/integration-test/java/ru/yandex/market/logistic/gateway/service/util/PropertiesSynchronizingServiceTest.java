package ru.yandex.market.logistic.gateway.service.util;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.config.properties.PropertiesSynchronizingProperties;
import ru.yandex.market.logistic.gateway.exceptions.PropertiesException;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.settings.SettingsApiFilter;
import ru.yandex.market.logistics.management.entity.request.settings.SettingsMethodFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSubtypeResponse;
import ru.yandex.market.logistics.management.entity.response.settings.SettingsApiDto;
import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodDto;
import ru.yandex.market.logistics.management.entity.type.ApiType;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

public class PropertiesSynchronizingServiceTest extends AbstractIntegrationTest {

    @Autowired
    private PropertiesSynchronizingService propertiesSynchronizingService;

    @Autowired
    private PropertiesSynchronizingProperties propertiesSynchronizingProperties;

    @Autowired
    private LMSClient lmsClient;

    @Test
    @DatabaseSetup("/repository/state/properties_before_synchronization_lms.xml")
    @ExpectedDatabase(value = "/repository/expected/properties_after_synchronization_lms.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testSynchronizePropertiesWithNonEmptyDatabase() {
        propertiesSynchronizingProperties.setPartsMode(false);

        prepareData();

        propertiesSynchronizingService.synchronizeProperties();
    }

    @Test
    @DatabaseSetup("/repository/state/empty.xml")
    @ExpectedDatabase(value = "/repository/expected/properties_after_synchronization_lms.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testSynchronizePropertiesWithEmptyDatabase() {
        propertiesSynchronizingProperties.setPartsMode(false);

        prepareData();

        propertiesSynchronizingService.synchronizeProperties();
    }

    @Test
    public void testSynchronizePropertiesWithEmptyApiType() {
        propertiesSynchronizingProperties.setPartsMode(false);

        when(lmsClient.searchPartners(any(SearchPartnerFilter.class)))
            .thenReturn(List.of(createPartner(1L, PartnerType.FULFILLMENT, null)));
        when(lmsClient.searchPartnerApiSettings(any(SettingsApiFilter.class)))
            .thenReturn(List.of(createSettingsApi(10L, 1L, "ABC12345", "JSON", null)));

        softAssert.assertThatThrownBy(() -> propertiesSynchronizingService.synchronizeProperties())
            .as("API type cannot be null")
            .isInstanceOf(PropertiesException.class);
    }

    @Test
    @ExpectedDatabase(value = "/repository/expected/properties_after_synchronization_lms_parts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testSynchronizePropertiesWithNonEmptyDatabaseAndParts() {
        propertiesSynchronizingProperties.setPartsMode(true);
        propertiesSynchronizingProperties.setPartSize(2);

        mockSearchPartners(Set.of(1L), PartnerType.FULFILLMENT, null);
        mockSearchPartners(Set.of(2L, 3L, 4L, 5L), PartnerType.SORTING_CENTER, 1L);
        mockSearchPartners(Set.of(7L), PartnerType.DISTRIBUTION_CENTER, 1331L);

        mockSearchPartnerApiSettings(Set.of(1L));
        mockSearchPartnerApiSettings(Set.of(2L, 3L));
        mockSearchPartnerApiSettings(Set.of(4L, 5L));

        mockSearchPartnerApiSettingsMethods(Set.of(1L));
        mockSearchPartnerApiSettingsMethods(Set.of(2L, 3L));
        mockSearchPartnerApiSettingsMethods(Set.of(4L, 5L));

        propertiesSynchronizingService.synchronizeProperties();
    }

    private void mockSearchPartners(Set<Long> ids, PartnerType partnerType, Long partnerSubtypeId) {
        List<PartnerResponse> partnerResponses = ids.stream()
            .map(id -> createPartner(id, partnerType, partnerSubtypeId))
            .collect(Collectors.toList());
        when(lmsClient.searchPartners(
                eq(SearchPartnerFilter.builder().setTypes(Set.of(partnerType)).build())
            )
        ).thenReturn(partnerResponses);
    }

    private void mockSearchPartnerApiSettings(Set<Long> ids) {
        List<SettingsApiDto> dtos =
            ids.stream().map(
                    id -> createSettingsApi(id * 10, id, "ABC12367", "XML", ApiType.FULFILLMENT))
                .collect(Collectors.toList());
        when(lmsClient.searchPartnerApiSettings(
                eq(SettingsApiFilter.newBuilder().partnerIds(ids).build())
            )
        ).thenReturn(dtos);
    }

    private void mockSearchPartnerApiSettingsMethods(Set<Long> ids) {
        List<SettingsMethodDto> dtos = ids.stream().map(
                id -> createSettingsMethod(
                    id,
                    id * 10,
                    "createOrder",
                    "https://url" + id + ".url/api/createOrder",
                    true
                ))
            .collect(Collectors.toList());

        when(lmsClient.searchPartnerApiSettingsMethods(
                eq(SettingsMethodFilter.newBuilder().partnerIds(ids).build())
            )
        ).thenReturn(dtos);
    }

    private void prepareData() {
        when(lmsClient.searchPartners(any(SearchPartnerFilter.class)))
            .thenReturn(getPartners());
        when(lmsClient.searchPartnerApiSettings(any(SettingsApiFilter.class)))
            .thenReturn(getSettingsApi());
        when(lmsClient.searchPartnerApiSettingsMethods(any(SettingsMethodFilter.class)))
            .thenReturn(getSettingsMethods());
    }

    private List<PartnerResponse> getPartners() {
        return Arrays.asList(
            createPartner(1L, PartnerType.FULFILLMENT, 1L),
            createPartner(2L, PartnerType.SORTING_CENTER, 2L),
            createPartner(3L, PartnerType.DELIVERY, 3L),
            createPartner(4L, PartnerType.XDOC, null),
            createPartner(5L, PartnerType.DROPSHIP, 123456L),
            createPartner(6L, PartnerType.SUPPLIER, null),
            createPartner(7L, PartnerType.OWN_DELIVERY, null),
            createPartner(8L, PartnerType.DROPSHIP_BY_SELLER, null),
            createPartner(9L, PartnerType.DISTRIBUTION_CENTER, null)
        );
    }

    private List<SettingsApiDto> getSettingsApi() {
        return Arrays.asList(
            createSettingsApi(10L, 1L, "ABC12345", "JSON", ApiType.DELIVERY),
            createSettingsApi(20L, 2L, "ABC12367", "XML", ApiType.FULFILLMENT),
            createSettingsApi(30L, 3L, "ABC12345", "XML", ApiType.DELIVERY),
            createSettingsApi(40L, 4L, "ABC12345", "XML", ApiType.FULFILLMENT),
            createSettingsApi(50L, 5L, "ABC12345", "XML", ApiType.FULFILLMENT),
            createSettingsApi(60L, 6L, "ABC12345", "XML", ApiType.FULFILLMENT),
            createSettingsApi(70L, 7L, "ABC12345", "JSON", ApiType.DELIVERY),
            createSettingsApi(80L, 8L, "ABC12345", "XML", ApiType.FULFILLMENT),
            createSettingsApi(90L, 9L, "ABC12345", "XML", ApiType.FULFILLMENT)
        );
    }

    private List<SettingsMethodDto> getSettingsMethods() {
        return Arrays.asList(
            createSettingsMethod(
                1L,
                10L,
                "createOrder",
                "https://url1.url/api/createOrder",
                true,
                "0 0 14 * * ?",
                10,
                5
            ),
            createSettingsMethod(
                1L,
                10L,
                "getLabels",
                "https://url1.url/api/getLabels",
                true,
                "0 0 14 * * ?"
            ),
            createSettingsMethod(
                2L,
                20L,
                "createOrder",
                "https://url2.url/api/createOrder",
                true
            ),
            createSettingsMethod(
                3L,
                30L,
                "createOrder",
                "https://url3.url/api/createOrder",
                true
            ),
            createSettingsMethod(
                3L,
                30L,
                "getLabels",
                "https://url3.url/api/getLabels",
                false
            ),
            createSettingsMethod(
                6L,
                60L,
                "createOrder",
                "https://url6.url/api/createOrder",
                true
            ),
            createSettingsMethod(
                7L,
                70L,
                "createOrder",
                "https://url7.url/api/createOrder",
                false
            ),
            createSettingsMethod(
                8L,
                80L,
                "createOrder",
                "https://url8.url/api/createOrder",
                true
            ),
            createSettingsMethod(
                9L,
                90L,
                "putInbound",
                "https://url9.url/api/putInbound",
                true
            )
        );
    }

    private SettingsMethodDto createSettingsMethod(
        Long id,
        Long settingsApiId,
        String method,
        String url,
        Boolean active
    ) {
        return createSettingsMethod(
            id,
            settingsApiId,
            method,
            url,
            active,
            null
        );
    }

    private SettingsMethodDto createSettingsMethod(
        Long id,
        Long settingsApiId,
        String method,
        String url,
        Boolean active,
        String cron
    ) {
        return createSettingsMethod(
            id,
            settingsApiId,
            method,
            url,
            active,
            cron,
            null,
            null
        );
    }

    private SettingsMethodDto createSettingsMethod(
        Long id,
        Long settingsApiId,
        String method,
        String url,
        Boolean active,
        String cron,
        Integer pollingFreq,
        Integer batchSize
    ) {
        return SettingsMethodDto.newBuilder()
            .id(id)
            .settingsApiId(settingsApiId)
            .method(method)
            .url(url)
            .active(active)
            .cronExpression(cron)
            .entityPollingFrequencyInSecs(pollingFreq)
            .batchSize(batchSize)
            .build();
    }

    private SettingsApiDto createSettingsApi(Long id, Long partnerId, String token, String format, ApiType apiType) {
        return SettingsApiDto.newBuilder()
            .id(id)
            .partnerId(partnerId)
            .apiType(apiType)
            .token(token)
            .format(format)
            .version("1.0")
            .build();
    }

    private PartnerResponse createPartner(Long id, PartnerType type, Long partnerSubtypeId) {
        PartnerResponse.PartnerResponseBuilder partnerResponseBuilder = PartnerResponse.newBuilder()
            .id(id)
            .partnerType(type)
            .name(type.toString() + id);
        if (partnerSubtypeId != null) {
            PartnerSubtypeResponse partnerSubtypeResponse = PartnerSubtypeResponse.newBuilder()
                .id(partnerSubtypeId)
                .name("subtype_" + partnerSubtypeId)
                .build();
            partnerResponseBuilder.subtype(partnerSubtypeResponse);
        }
        return partnerResponseBuilder.build();
    }
}

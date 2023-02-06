package ru.yandex.market.logistics.tarifficator.client.shop;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import retrofit2.Response;
import retrofit2.Retrofit;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.logistics.tarifficator.client.configuration.TestClientConfig;
import ru.yandex.market.logistics.tarifficator.model.enums.YaDeliveryTariffType;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.CourierTariffType;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.PartnerPlacementProgramType;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.RegionGroupFailureReason;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.RegionGroupPaymentType;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.RegionGroupStatus;
import ru.yandex.market.logistics.tarificator.open.api.client.api.RegionGroupApi;
import ru.yandex.market.logistics.tarificator.open.api.client.api.RegionGroupDeliveryServiceApi;
import ru.yandex.market.logistics.tarificator.open.api.client.api.RegionGroupPaymentApi;
import ru.yandex.market.logistics.tarificator.open.api.client.api.RegionGroupStatusApi;
import ru.yandex.market.logistics.tarificator.open.api.client.api.RegionGroupTariffApi;
import ru.yandex.market.logistics.tarificator.open.api.client.api.ShopDeliveryStateApi;
import ru.yandex.market.logistics.tarificator.open.api.client.api.ShopMetaApi;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.ComparisonOperationDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryCostConditionDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryModifierActionDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryModifierConditionDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryModifierDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryServiceCodeDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryServiceDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryServiceStrategyDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryTariffDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.ModifyDeliveryServicesRequestDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.OperationTypeDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.OptionDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.OptionGroupDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.PercentValueLimiterDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.PriceRuleDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.RegionGroupDeliverySummaryDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.RegionGroupDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.RegionGroupMetaInfoDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.RegionGroupPaymentDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.RegionGroupStatusDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.RegionGroupUpdateRequestDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.RegionGroupsStatusesModificationDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.SelectedDeliveryServiceDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.ShopDeliveryStateDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.ShopDeliverySummaryDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.ShopInfoDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.ShopMetaDataDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.ShopRegionGroupsDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.ShopsDeliveryStateRequestDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.ShopsDeliveryStateResponseDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.ValueLimiterDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.ValueModificationRuleDto;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@ExtendWith({
    SpringExtension.class,
    SoftAssertionsExtension.class,
})
@SpringBootTest(classes = {
    TestClientConfig.class
})
@TestPropertySource("classpath:integration-test.properties")
public class ShopTariffsApiTest {

    private static WireMockServer wireMockServer;
    @InjectSoftAssertions
    protected SoftAssertions softly;
    @Autowired
    private Retrofit retrofit;

    @BeforeAll
    public static void setupServer() {
        wireMockServer = new WireMockServer(options().port(9000).withRootDirectory(
            Objects.requireNonNull(getClassPathFile("wiremock")).getAbsolutePath()
        ));
        wireMockServer.start();
    }

    @AfterAll
    public static void shutDownServer() {
        wireMockServer.shutdown();
    }

    @DisplayName("Сохранение метаданных магазина")
    @Test
    void testSaveShopMetaData() throws IOException {
        ShopMetaDataDto dto = new ShopMetaDataDto();
        dto.setLocalRegion(213L);
        dto.setCurrency(Currency.USD);
        dto.setLogisticPartnerId(1121L);
        dto.setPlacementPrograms(Collections.singletonList(PartnerPlacementProgramType.DROPSHIP_BY_SELLER));

        Response<ShopInfoDto> result = retrofit.create(ShopMetaApi.class)
            .saveShopMetaData(774L, 123L, dto)
            .execute();

        softly.assertThat(result).isNotNull();
        softly.assertThat(result.isSuccessful()).isTrue();
        softly.assertThat(result.body())
            .usingRecursiveComparison()
            .isEqualTo(createExpectedShopInfo());
    }

    @DisplayName("Сохранение метаданных магазина")
    @Test
    void testGetRegionGroups() throws IOException {
        ShopMetaDataDto dto = new ShopMetaDataDto();
        dto.setLocalRegion(213L);
        dto.setCurrency(Currency.USD);

        Response<ShopRegionGroupsDto> result = retrofit.create(RegionGroupApi.class)
            .getRegionGroups(774L)
            .execute();

        softly.assertThat(result).isNotNull();
        softly.assertThat(result.isSuccessful()).isTrue();
        softly.assertThat(result.body())
            .usingRecursiveComparison()
            .isEqualTo(createExpectedRegionGroups());
    }

    @DisplayName("Создание региональной группы")
    @Test
    void testCreateRegionGroup() throws IOException {
        RegionGroupUpdateRequestDto dto = new RegionGroupUpdateRequestDto();
        dto.setGroupName("Группа 1");
        dto.setRegions(Collections.singletonList(213L));

        Response<RegionGroupMetaInfoDto> result = retrofit.create(RegionGroupApi.class)
            .updateRegionGroup(774L, 1111L, 123L, dto)
            .execute();

        softly.assertThat(result).isNotNull();
        softly.assertThat(result.isSuccessful()).isTrue();
        softly.assertThat(result.body())
            .usingRecursiveComparison()
            .isEqualTo(createExpectedRegionGroupMetaForCreation());
    }

    @DisplayName("Удаление региональной группы")
    @Test
    void testDeleteRegionGroup() throws IOException {
        Response<Void> result = retrofit.create(RegionGroupApi.class)
            .deleteGroup(774L, 1111L, 123L)
            .execute();

        softly.assertThat(result).isNotNull();
        softly.assertThat(result.isSuccessful()).isTrue();
    }

    @DisplayName("Батчовое удаление региональных групп")
    @Test
    void testBatchDeleteRegionGroups() throws IOException {
        Response<Void> result = retrofit.create(RegionGroupApi.class)
            .deleteGroups(774L, 123L, Collections.singletonList(100500L))
            .execute();

        softly.assertThat(result).isNotNull();
        softly.assertThat(result.isSuccessful()).isTrue();
    }

    @DisplayName("Создание тарифа для региональной группы")
    @Test
    void testCreateTariffForRegionGroup() throws IOException {
        Response<Void> result = retrofit.create(RegionGroupTariffApi.class)
            .createTariff(774L, 101L, 221L, createTariffDto())
            .execute();

        softly.assertThat(result).isNotNull();
        softly.assertThat(result.isSuccessful()).isTrue();
    }

    @DisplayName("Получение тарифа для региональной группы")
    @Test
    void testGetTariffForRegionGroup() throws IOException {
        Response<DeliveryTariffDto> result = retrofit.create(RegionGroupTariffApi.class)
            .getTariff(774L, 101L, 221L)
            .execute();

        softly.assertThat(result).isNotNull();
        softly.assertThat(result.isSuccessful()).isTrue();
        softly.assertThat(result.body())
            .usingRecursiveComparison()
            .isEqualTo(createTariffDto());
    }

    @DisplayName("Получение списка служб доставки, настроеных в региональной группе")
    @Test
    void testGetRegionGroupDeliveryServices() throws IOException {
        Response<List<SelectedDeliveryServiceDto>> result = retrofit.create(RegionGroupDeliveryServiceApi.class)
            .getSelectedDeliveryServiceLinks(774L, 102L, 221L)
            .execute();

        softly.assertThat(result).isNotNull();
        softly.assertThat(result.isSuccessful()).isTrue();
        softly.assertThat(result.body())
            .usingRecursiveComparison()
            .isEqualTo(getExpectedRegionGroupDeliveryServices());
    }

    @DisplayName("Получение списка служб доставки, настроеных магазином")
    @Test
    void testGetShopDeliveryServices() throws IOException {
        Response<List<Long>> result = retrofit.create(RegionGroupDeliveryServiceApi.class)
            .getShopDeliveryServiceLinks(774L, 221L)
            .execute();

        softly.assertThat(result).isNotNull();
        softly.assertThat(result.isSuccessful()).isTrue();
        softly.assertThat(result.body())
            .usingRecursiveComparison()
            .isEqualTo(List.of(1001L, 1002L, 1003L));
    }

    @DisplayName("Тест линка одной СД с региональной группой")
    @Test
    void testLinkRegionGroupWithDeliveryService() throws IOException {
        Response<Void> result = retrofit.create(RegionGroupDeliveryServiceApi.class)
            .selectDeliveryService(
                774L,
                101L,
                1001L,
                221L,
                new DeliveryServiceDto()
                    .courierDeliveryStrategy(DeliveryServiceStrategyDto.AUTO_CALCULATED)
                    .pickupDeliveryStrategy(DeliveryServiceStrategyDto.NO_DELIVERY)
                    .courierDeliveryModifiers(createPartialModifiers())
            )
            .execute();

        softly.assertThat(result).isNotNull();
        softly.assertThat(result.isSuccessful()).isTrue();
    }

    @DisplayName("Батчовое обновление линков региональной группы с СД")
    @Test
    void testBatchUpdateDeliveryServicesLinks() throws IOException {
        Response<Void> result = retrofit.create(RegionGroupDeliveryServiceApi.class)
            .modifyDeliveryServices(
                774L,
                102L,
                221L,
                createBatchDeliveryServiceLinksUpdateRequest()
            )
            .execute();

        softly.assertThat(result).isNotNull();
        softly.assertThat(result.isSuccessful()).isTrue();
    }

    @DisplayName("Удаление линка региональной группы с СД")
    @Test
    void testDeleteRegionGroupWithServiceLink() throws IOException {
        Response<Void> result = retrofit.create(RegionGroupDeliveryServiceApi.class)
            .deleteDeliveryServiceLink(774L, 102L, 1L, 221L)
            .execute();

        softly.assertThat(result).isNotNull();
        softly.assertThat(result.isSuccessful()).isTrue();
    }

    @DisplayName("Получить способы оплаты в региональной группы")
    @Test
    void testGetRegionGroupPayments() throws IOException {
        Response<RegionGroupPaymentDto> result = retrofit.create(RegionGroupPaymentApi.class)
            .getRegionGroupPayments(774L, 101L, 221L)
            .execute();

        softly.assertThat(result).isNotNull();
        softly.assertThat(result.isSuccessful()).isTrue();
        softly.assertThat(result.body())
            .usingRecursiveComparison()
            .isEqualTo(createExpectedRegionGroupPayments());
    }

    @DisplayName("Обновить способы оплаты в региональной группы")
    @Test
    void testSaveRegionGroupPayments() throws IOException {
        Response<RegionGroupPaymentDto> result = retrofit.create(RegionGroupPaymentApi.class)
            .saveRegionGroupPayments(
                774L,
                101L,
                221L,
                List.of(RegionGroupPaymentType.COURIER_CASH, RegionGroupPaymentType.COURIER_CARD)
            )
            .execute();

        softly.assertThat(result).isNotNull();
        softly.assertThat(result.isSuccessful()).isTrue();
        softly.assertThat(result.body())
            .usingRecursiveComparison()
            .isEqualTo(createExpectedRegionGroupPayments());
    }

    @DisplayName("Обновления статусов региональных групп")
    @Test
    void testUpdateRegionGroupStatuses() throws IOException {
        Response<Void> result = retrofit.create(RegionGroupStatusApi.class)
            .saveRegionGroupStatuses(
                774L,
                221L,
                createUpdateStatusesDto()
            )
            .execute();

        softly.assertThat(result).isNotNull();
        softly.assertThat(result.isSuccessful()).isTrue();
    }

    @DisplayName("Получить статус региональных групп")
    @Test
    void testGetRegionGroupStatuses() throws IOException {
        Response<RegionGroupStatusDto> result = retrofit.create(RegionGroupStatusApi.class)
            .getRegionGroupsStatus(
                774L,
                1000L,
                221L
            )
            .execute();

        softly.assertThat(result).isNotNull();
        softly.assertThat(result.isSuccessful()).isTrue();
        softly.assertThat(result.body())
            .usingRecursiveComparison()
            .isEqualTo(createRegionGroupStatusDto());
    }

    @DisplayName("Получить информацию о настройке доставки магазинами")
    @Test
    void testGetDeliveryState() throws IOException {
        Response<ShopsDeliveryStateResponseDto> result = retrofit.create(ShopDeliveryStateApi.class)
            .getDeliveryState(
                new ShopsDeliveryStateRequestDto()
                    .addShopIdsItem(774L)
                    .addShopIdsItem(775L)
            )
            .execute();

        softly.assertThat(result).isNotNull();
        softly.assertThat(result.isSuccessful()).isTrue();
        softly.assertThat(result.body())
            .usingRecursiveComparison()
            .isEqualTo(createExpectedShopsDeliveryState());
    }

    @DisplayName("Получить сводную информацию о настройках доставки")
    @Test
    void testGetDeliverySummary() throws IOException {
        Response<ShopDeliverySummaryDto> result = retrofit.create(ShopDeliveryStateApi.class)
            .getDeliverySummary(774L)
            .execute();

        softly.assertThat(result).isNotNull();
        softly.assertThat(result.isSuccessful()).isTrue();
        softly.assertThat(result.body())
            .usingRecursiveComparison()
            .isEqualTo(createExpectedRegionGroupsSummary());
    }

    /**
     * Получение файла из classpath
     *
     * @param path путь к файлу
     */
    private static File getClassPathFile(String path) {
        ClassLoader classLoader = ShopTariffsApiTest.class.getClassLoader();
        URL url = classLoader.getResource(path);
        if (url == null) {
            return null;
        } else {
            return new File(url.getFile());
        }
    }

    @Nonnull
    private ShopRegionGroupsDto createExpectedRegionGroups() {
        ShopRegionGroupsDto dto = new ShopRegionGroupsDto();
        dto.setMaxRegionsGroups(20);
        dto.setRegionsGroups(Collections.singletonList(createExceptedRegionGroup()));

        return dto;
    }

    @Nonnull
    private RegionGroupDto createExceptedRegionGroup() {
        RegionGroupDto dto = new RegionGroupDto();

        dto.setId(1177470L);
        dto.setDatasourceId(11131342L);
        dto.setName("Санкт-Петербург");
        dto.setSelfRegion(true);
        dto.setCheckStatus(RegionGroupStatus.NEW);
        dto.setTariffType(CourierTariffType.UNIFORM);
        dto.setCurrency(Currency.RUR);
        dto.setModifiedBy(11L);
        dto.setIncludes(Collections.singletonList(2L));
        dto.setExcludes(Arrays.asList(20293L, 20294L));
        dto.setUseYml(false);
        dto.setHasDeliveryService(false);

        return dto;
    }

    @Nonnull
    private RegionGroupMetaInfoDto createExpectedRegionGroupMetaForCreation() {
        RegionGroupMetaInfoDto dto = new RegionGroupMetaInfoDto();

        dto.setGroupId(1L);
        dto.setCurrency(Currency.RUR);
        dto.setStatus(RegionGroupStatus.NEW);

        return dto;
    }

    @Nonnull
    private DeliveryTariffDto createTariffDto() {
        return new DeliveryTariffDto()
            .tariffType(CourierTariffType.PRICE)
            .notes("Запись")
            .priceRules(
                Collections.singletonList(
                    new PriceRuleDto()
                        .orderNum(0)
                        .priceTo(BigDecimal.valueOf(100))
                )
            )
            .optionsGroups(
                Collections.singletonList(
                    new OptionGroupDto()
                        .priceOrderNum(0)
                        .hasDelivery(true)
                        .options(
                            Collections.singletonList(
                                new OptionDto()
                                    .orderNum(0)
                                    .cost(BigDecimal.ZERO)
                                    .daysFrom(1)
                                    .daysTo(1)
                                    .orderBeforeHour(13)
                            )
                        )
                )
            );
    }

    @Nonnull
    private List<SelectedDeliveryServiceDto> getExpectedRegionGroupDeliveryServices() {
        return List.of(
            new SelectedDeliveryServiceDto()
                .deliveryServiceId(1001L)
                .courierDeliveryStrategy(DeliveryServiceStrategyDto.UNKNOWN_COST_TIME)
                .pickupDeliveryStrategy(DeliveryServiceStrategyDto.AUTO_CALCULATED)
                .courierDeliveryModifiers(createFullModifiers())
                .pickupDeliveryModifiers(createPartialModifiers()),
            new SelectedDeliveryServiceDto()
                .deliveryServiceId(1002L)
                .courierDeliveryStrategy(DeliveryServiceStrategyDto.NO_DELIVERY)
                .pickupDeliveryStrategy(DeliveryServiceStrategyDto.FIXED_COST_TIME)
                .courierDeliveryModifiers(createPartialModifiers())
                .pickupDeliveryModifiers(createFullModifiers()),
            new SelectedDeliveryServiceDto()
                .deliveryServiceId(1003L)
                .courierDeliveryStrategy(DeliveryServiceStrategyDto.AUTO_CALCULATED)
                .pickupDeliveryStrategy(DeliveryServiceStrategyDto.UNKNOWN_COST_TIME)
                .courierDeliveryModifiers(createPartialModifiers())
                .pickupDeliveryModifiers(createPartialModifiers())
        );
    }

    @Nonnull
    private List<DeliveryModifierDto> createFullModifiers() {
        var valueLimiter = new ValueLimiterDto()
            .minValue(BigDecimal.ZERO)
            .maxValue(BigDecimal.TEN);
        var valueModificationRule = new ValueModificationRuleDto()
            .operation(OperationTypeDto.SUBTRACT)
            .parameter(BigDecimal.valueOf(12.97))
            .resultLimit(valueLimiter);
        var action = new DeliveryModifierActionDto()
            .costModificationRule(valueModificationRule)
            .timeModificationRule(valueModificationRule)
            .paidByCustomerServices(List.of(DeliveryServiceCodeDto.INSURANCE))
            .isCarrierTurnedOn(true);
        var condition = new DeliveryModifierConditionDto()
            .cost(
                new PercentValueLimiterDto()
                    .minValue(BigDecimal.ZERO)
                    .maxValue(BigDecimal.valueOf(98))
                    .percent(BigDecimal.valueOf(95.5))
            )
            .deliveryCost(
                new DeliveryCostConditionDto()
                    .percentFromOfferPrice(BigDecimal.valueOf(50))
                    .comparisonOperation(ComparisonOperationDto.MORE)
            )
            .weight(valueLimiter)
            .chargeableWeight(valueLimiter)
            .dimension(valueLimiter)
            .carrierIds(List.of(1L))
            .deliveryDestinations(List.of(213))
            .deliveryTypes(List.of(YaDeliveryTariffType.PICKUP));

        return List.of(
            new DeliveryModifierDto()
                .id(100L)
                .timestamp(1583325200238L)
                .action(action)
                .condition(condition),
            new DeliveryModifierDto()
                .id(101L)
                .timestamp(1583325200238L)
                .action(action)
                .condition(condition)
        );
    }

    @Nonnull
    private List<DeliveryModifierDto> createPartialModifiers() {
        return List.of(
            new DeliveryModifierDto()
                .id(200L)
                .timestamp(1583325200247L)
                .action(
                    new DeliveryModifierActionDto()
                        .costModificationRule(
                            new ValueModificationRuleDto()
                                .operation(OperationTypeDto.FIX_VALUE)
                                .parameter(BigDecimal.valueOf(2000))
                        )
                )
        );
    }

    @Nonnull
    private ModifyDeliveryServicesRequestDto createBatchDeliveryServiceLinksUpdateRequest() {
        return new ModifyDeliveryServicesRequestDto()
            .addDeliveryServicesToUpdateItem(
                new SelectedDeliveryServiceDto()
                    .deliveryServiceId(1004L)
                    .courierDeliveryStrategy(DeliveryServiceStrategyDto.NO_DELIVERY)
                    .pickupDeliveryStrategy(DeliveryServiceStrategyDto.AUTO_CALCULATED)
                    .pickupDeliveryModifiers(createFullModifiers())
            )
            .addDeliveryServicesToUpdateItem(
                new SelectedDeliveryServiceDto()
                    .deliveryServiceId(1001L)
                    .courierDeliveryStrategy(DeliveryServiceStrategyDto.AUTO_CALCULATED)
                    .pickupDeliveryStrategy(DeliveryServiceStrategyDto.NO_DELIVERY)
                    .courierDeliveryModifiers(createPartialModifiers())
            )
            .addDeliveryServicesToDeleteItem(1002L);
    }

    @Nonnull
    private RegionGroupPaymentDto createExpectedRegionGroupPayments() {
        return new RegionGroupPaymentDto()
            .regionGroupId(101L)
            .paymentTypes(List.of(RegionGroupPaymentType.COURIER_CASH, RegionGroupPaymentType.COURIER_CARD));
    }

    @Nonnull
    private RegionGroupsStatusesModificationDto createUpdateStatusesDto() {
        return new RegionGroupsStatusesModificationDto()
            .comment("Validation of shop 774 group is partly successful")
            .statuses(Collections.singletonList(createRegionGroupStatusDto()));
    }

    @Nonnull
    private RegionGroupStatusDto createRegionGroupStatusDto() {
        return new RegionGroupStatusDto()
            .regionGroupId(1000L)
            .status(RegionGroupStatus.FAIL)
            .deliveryReasons(List.of(RegionGroupFailureReason.INVALID_DELIVERY_TIME))
            .paymentReasons(List.of(RegionGroupPaymentType.COURIER_CASH))
            .comment("Failed validation for group 1000");
    }

    @Nonnull
    private ShopInfoDto createExpectedShopInfo() {
        return new ShopInfoDto()
            .currency(Currency.USD)
            .localRegion(213L)
            .logisticPartnerId(1121L)
            .placementPrograms(Collections.singletonList(PartnerPlacementProgramType.DROPSHIP_BY_SELLER))
            .selfRegionGroupId(1L);
    }

    @Nonnull
    private ShopsDeliveryStateResponseDto createExpectedShopsDeliveryState() {
        return new ShopsDeliveryStateResponseDto()
            .addDeliveryStatesItem(new ShopDeliveryStateDto()
                .shopId(774L)
                .hasCourierDelivery(true)
                .hasPickupDelivery(false)
                .courierDeliveryRegions(List.of(1L, 2L, 4L))
                .lastEventMillis(0L)
                .hasPrepayInLocalRegion(true)
            )
            .addDeliveryStatesItem(new ShopDeliveryStateDto()
                .shopId(775L)
                .hasCourierDelivery(false)
                .hasPickupDelivery(true)
                .courierDeliveryRegions(new ArrayList<>())
                .lastEventMillis(77750L)
                .hasPrepayInLocalRegion(false)
            );
    }

    @Nonnull
    private ShopDeliverySummaryDto createExpectedRegionGroupsSummary() {
        return new ShopDeliverySummaryDto()
            .maxRegionsGroups(20)
            .addRegionsGroupsItem(
                new RegionGroupDeliverySummaryDto()
                    .id(200L)
                    .datasourceId(774L)
                    .name("Свой регион")
                    .selfRegion(true)
                    .checkStatus(RegionGroupStatus.NEW)
                    .tariffType(CourierTariffType.UNIFORM)
                    .currency(Currency.RUR)
                    .minDeliveryCost(BigDecimal.valueOf(100))
                    .maxDeliveryCost(BigDecimal.valueOf(200))
                    .minDaysFrom(1)
                    .maxDaysTo(5)
                    .deliveryServicesCount(0)
            )
            .addRegionsGroupsItem(
                new RegionGroupDeliverySummaryDto()
                    .id(400L)
                    .datasourceId(774L)
                    .name("Доставка своим тарифом 2")
                    .selfRegion(false)
                    .checkStatus(RegionGroupStatus.NEW)
                    .tariffType(CourierTariffType.DEFAULT)
                    .currency(Currency.RUR)
                    .deliveryServicesCount(1)
            );
    }
}

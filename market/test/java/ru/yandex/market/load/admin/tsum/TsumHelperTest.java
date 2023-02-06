package ru.yandex.market.load.admin.tsum;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import ru.yandex.market.load.admin.converter.in.ShootingConfigConverterIn;
import ru.yandex.market.load.admin.entity.ShootingConfigValue;
import ru.yandex.market.load.admin.entity.ShootingData;
import ru.yandex.market.load.admin.entity.ShootingPlan;
import ru.yandex.market.load.admin.tsum.resources.ArcadiaArcBranchRef;
import ru.yandex.market.load.admin.tsum.resources.DefaultShootingOptions;
import ru.yandex.market.load.admin.tsum.resources.LoyaltyShootingOptions;
import ru.yandex.market.load.admin.tsum.resources.MultipleTanksShootingConfiguration;
import ru.yandex.market.load.admin.tsum.resources.OptionalSandboxResourceHolder;
import ru.yandex.market.load.admin.tsum.resources.PandoraCheckouterConfig;
import ru.yandex.market.load.admin.tsum.resources.PandoraMutableConfig;
import ru.yandex.market.load.admin.tsum.resources.PandoraRegionSpecificConfig;
import ru.yandex.market.load.admin.tsum.resources.PandoraShootingConfig;
import ru.yandex.market.load.admin.tsum.resources.PandoraTankConfig;
import ru.yandex.market.load.admin.tsum.resources.PerTankShootingOptions;
import ru.yandex.market.load.admin.tsum.resources.ShootingDelayOption;
import ru.yandex.market.sdk.userinfo.service.UidConstants;
import ru.yandex.mj.generated.client.tsum.model.Resource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

public class TsumHelperTest {
    private final TsumHelper helper = new TsumHelper();

    @Test
    public void testingMinimalOptionsOk() {
        ShootingConfigValue shootingConfigValue = ShootingConfigValue.builder()
                .pipeId("load-test-production-dev")
                .arcadiaArcBranchRef("arcadia:/arc/trunk/arcadia")
                .stocksRequiredRate(1.0)
                .preferredOphPerTank(30_000)
                .loyaltyShootingOptions(LoyaltyShootingOptions.builder()
                        .percentOfFlashOrders(0)
                        .percentOfCashbackOrders(0)
                        .percentOfOrdersPaidByCoins(0)
                        .percentOfOrdersUsingPromo(0)
                        .coinsPromoId(0)
                        .build())
                .checkouterConfig(PandoraCheckouterConfig.builder()
                        .cartRepeats(2)
                        .offersDistribution("[{'offersCount': 1, 'ordersDistribution': 1}]")
                        .cartsDistribution("[{'internalCarts':1, 'ordersDistribution': 1}]")
                        .cartDurationSec(1)
                        .handlesCommonDelayMs(100)
                        .handles("handles")
                        .build())
                .perTankShootingOptions(Arrays.asList(PerTankShootingOptions.builder()
                        .checkouterConfig(PandoraCheckouterConfig.builder()
                                .balancer("http://checkouter.tst.vs.market.yandex.net:39001")
                                .build())
                        .tankConfig(PandoraTankConfig.builder()
                                .tankBaseUrl("http://tank01ht.market.yandex.net:8083")
                                .build())
                        .build()))
                .build();
        ShootingPlan plan = ShootingPlan
                .builder()
                .ordersPerHour(3600)
                .durationSeconds(10)
                .build();
        ShootingData shooting = ShootingData.builder()
                .configValue(shootingConfigValue)
                .plan(plan)
                .build();
        List<Resource> resources = helper.toResources(shooting);
        assertThat(resources, hasItem(ArcadiaArcBranchRef.builder()
                .ref("arcadia:/arc/trunk/arcadia")
                .build().toResource()));
        assertThat(resources, hasItem(PandoraShootingConfig.builder()
                .useInReport(true)
                .useRangeUid(true)
                .minUidInRange(UidConstants.NO_SIDE_EFFECTS_RANGE.lowerEndpoint())
                .maxUidInRange(UidConstants.NO_SIDE_EFFECTS_RANGE.upperEndpoint())
                .build().toResource()));
        assertThat(resources, hasItem(MultipleTanksShootingConfiguration.builder()
                .shootingDelayOption(ShootingDelayOption.builder().build())
                .defaultShootingOptions(DefaultShootingOptions.builder()
                        .loyaltyShootingOptions(shootingConfigValue.getLoyaltyShootingOptions())
                        .build())
                .perTankShootingOptions(Collections.singletonList(PerTankShootingOptions.builder()
                        .mutableConfig(PandoraMutableConfig.builder()
                                .ordersPerHour(3600)
                                .coinsPerHour(0)
                                .duration(10)
                                .shipmentDay(-1)
                                .distribution("{}")
                                .stocksRequiredRate("1.0")
                                .build())
                        .checkouterConfig(shootingConfigValue.getCheckouterConfig().toBuilder()
                                .balancer("http://checkouter.tst.vs.market.yandex.net:39001")
                                .build())
                        .tankConfig(PandoraTankConfig.builder()
                                .tankBaseUrl("http://tank01ht.market.yandex.net:8083")
                                .build())
                        .build()))
                .build().toResource()));
        assertThat(resources, hasItem(OptionalSandboxResourceHolder.builder().build().toResource()));
    }

    @Test
    public void allOptionsOk() {
        LoyaltyShootingOptions loyaltyShootingOptions = LoyaltyShootingOptions.builder()
                .percentOfFlashOrders(15)
                .percentOfCashbackOrders(20)
                .percentOfOrdersPaidByCoins(25)
                .percentOfOrdersUsingPromo(30)
                .coinsPromoId(1234)
                .build();
        ShootingConfigValue configValue = ShootingConfigValue.builder()
                .arcadiaArcBranchRef("arcadia:/arc/trunk/arcadia")
                .shootingDelayOption(ShootingDelayOption.builder().delayInMinutes(101).build())
                .stocksRequiredRate(1.0)
                .preferredOphPerTank(40_000)
                .pipeId("load-test-production-dev")
                .loyaltyShootingOptions(loyaltyShootingOptions)
                .checkouterConfig(PandoraCheckouterConfig.builder()
                        .cartRepeats(2)
                        .offersDistribution("[{'offersCount': 1, 'ordersDistribution': 1}]")
                        .cartsDistribution("[{'internalCarts':1, 'ordersDistribution': 1}]")
                        .cartDurationSec(1)
                        .handles("handles")
                        .handlesCommonDelayMs(100)
                        .build())
                .perTankShootingOptions(Arrays.asList(
                        PerTankShootingOptions.builder()
                                .checkouterConfig(PandoraCheckouterConfig.builder()
                                        .balancer("balancer1")
                                        .build())
                                .tankConfig(PandoraTankConfig.builder()
                                        .tankBaseUrl("tank1")
                                        .build())
                                .regionSpecificConfig(PandoraRegionSpecificConfig.builder()
                                        .deliveryServices("1005429")
                                        .warehouseId("147")
                                        .regionId("11030")
                                        .deliveryType("DELIVERY")
                                        .flashShopPromoID("flash1")
                                        .addresses("addresses1")
                                        .build())
                                .build(),
                        PerTankShootingOptions.builder()
                                .checkouterConfig(PandoraCheckouterConfig.builder()
                                        .balancer("balancer2")
                                        .build())
                                .tankConfig(PandoraTankConfig.builder()
                                        .tankBaseUrl("tank2")
                                        .build())
                                .regionSpecificConfig(PandoraRegionSpecificConfig.builder()
                                        .deliveryServices("78080")
                                        .warehouseId("300")
                                        .regionId("54")
                                        .deliveryType("DELIVERY")
                                        .flashShopPromoID("flash2")
                                        .addresses("addresses2")
                                        .build())
                                .build(),
                        PerTankShootingOptions.builder()
                                .checkouterConfig(PandoraCheckouterConfig.builder()
                                        .balancer("balancer3")
                                        .build())
                                .tankConfig(PandoraTankConfig.builder()
                                        .tankBaseUrl("tank3")
                                        .build())
                                .regionSpecificConfig(PandoraRegionSpecificConfig.builder()
                                        .deliveryServices("1005429")
                                        .warehouseId("147")
                                        .regionId("11030")
                                        .deliveryType("DELIVERY")
                                        .flashShopPromoID("flash1")
                                        .addresses("addresses1")
                                        .build())
                                .build()
                ))
                .build();
        ShootingPlan plan = ShootingPlan
                .builder()
                .ordersPerHour(90_000)
                .durationSeconds(10)
                .build();
        ShootingData shooting = ShootingData.builder()
                .configValue(configValue)
                .plan(plan)
                .build();
        List<Resource> resources = helper.toResources(shooting);
        assertThat(resources, hasItem(ArcadiaArcBranchRef.builder()
                .ref("arcadia:/arc/trunk/arcadia")
                .build().toResource()));
        assertThat(resources, hasItem(PandoraShootingConfig.builder()
                .useInReport(true)
                .useRangeUid(true)
                .minUidInRange(UidConstants.NO_SIDE_EFFECTS_RANGE.lowerEndpoint())
                .maxUidInRange(UidConstants.NO_SIDE_EFFECTS_RANGE.upperEndpoint())
                .build().toResource()));
        PandoraMutableConfig expectedPandoraMutableConfig = PandoraMutableConfig.builder()
                .ordersPerHour(30_000)
                .coinsPerHour(0)
                .duration(10)
                .shipmentDay(-1)
                .distribution("{}")
                .stocksRequiredRate("1.0")
                .build();
        assertThat(resources, hasItem(MultipleTanksShootingConfiguration.builder()
                .shootingDelayOption(configValue.getShootingDelayOption())
                .perTankShootingOptions(Arrays.asList(
                        PerTankShootingOptions.builder()
                                .checkouterConfig(configValue.getCheckouterConfig().toBuilder()
                                        .balancer("balancer1")
                                        .build())
                                .mutableConfig(expectedPandoraMutableConfig.toBuilder()
                                        .stocksRequiredRate("2.0")
                                        .build())
                                .tankConfig(PandoraTankConfig.builder()
                                        .tankBaseUrl("tank1")
                                        .build())
                                .regionSpecificConfig(PandoraRegionSpecificConfig.builder()
                                        .deliveryServices("1005429")
                                        .warehouseId("147")
                                        .regionId("11030")
                                        .deliveryType("DELIVERY")
                                        .flashShopPromoID("flash1")
                                        .addresses("addresses1")
                                        .build())
                                .build(),
                        PerTankShootingOptions.builder()
                                .checkouterConfig(configValue.getCheckouterConfig().toBuilder()
                                        .balancer("balancer2")
                                        .build())
                                .mutableConfig(expectedPandoraMutableConfig)
                                .tankConfig(PandoraTankConfig.builder()
                                        .tankBaseUrl("tank2")
                                        .build())
                                .regionSpecificConfig(PandoraRegionSpecificConfig.builder()
                                        .deliveryServices("78080")
                                        .warehouseId("300")
                                        .regionId("54")
                                        .deliveryType("DELIVERY")
                                        .flashShopPromoID("flash2")
                                        .addresses("addresses2")
                                        .build())
                                .build(),
                        PerTankShootingOptions.builder()
                                .checkouterConfig(configValue.getCheckouterConfig().toBuilder()
                                        .balancer("balancer3")
                                        .build())
                                .mutableConfig(expectedPandoraMutableConfig.toBuilder()
                                        .stocksRequiredRate("2.0")
                                        .build())
                                .tankConfig(PandoraTankConfig.builder()
                                        .tankBaseUrl("tank3")
                                        .build())
                                .regionSpecificConfig(PandoraRegionSpecificConfig.builder()
                                        .deliveryServices("1005429")
                                        .warehouseId("147")
                                        .regionId("11030")
                                        .deliveryType("DELIVERY")
                                        .flashShopPromoID("flash1")
                                        .addresses("addresses1")
                                        .build())
                                .build()
                ))
                .defaultShootingOptions(DefaultShootingOptions.builder()
                        .loyaltyShootingOptions(loyaltyShootingOptions)
                        .build())
                .build().toResource()));
        assertThat(resources, hasItem(OptionalSandboxResourceHolder.builder().build().toResource()));
    }

    @Test
    public void deserializeConfigValueOk() throws JsonProcessingException {
        String yaml = """
                pipeId: "load-test-production-dev"
                stocksRequiredRate: 1.0
                preferredOphPerTank: 10
                arcadiaArcBranchRef: "arcadia:/arc/trunk/arcadia"
                sandboxResource: null
                shootingDelayOption:
                  delayInMinutes: 0
                loyaltyShootingOptions:
                  percentOfCashbackOrders: 0
                  percentOfOrdersUsingPromo: 0
                  percentOfOrdersPaidByCoins: 0
                  percentOfFlashOrders: 0
                  coinsPromoId: 0
                checkouterConfig:
                  cartRepeats: 2
                  cartDurationSec: 1
                  handlesCommonDelayMs: 100
                  handles: ""
                  cartsDistribution: "[{'internalCarts':1, 'ordersDistribution': 1}]"
                  offersDistribution: "[{'offersCount': 1, 'ordersDistribution': 1}]"
                perTankShootingOptions:
                  - tankConfig:
                      tankBaseUrl: "http://tank01ht.market.yandex.net:8083"
                    regionSpecificConfig:
                      deliveryServices: "1005429"
                      warehouseId: "147"
                      regionId: "11030"
                      addresses: "[{'country': 'Россия','postcode': '346780', 'city': 'Азов', 'street': 'Петровский бульвар', 'house': '40', 'floor': '1','recipient': '000','phone': '+77777777777' },{'country': 'Россия','postcode': '346780', 'city': 'Азов', 'street': 'Московская улица', 'house': '6А','floor':'1','recipient': '000','phone': '+77777777777' },  {'country': 'Россия','postcode': '346780', 'city': 'Азов', 'street': 'улица Толстого', 'house': '19', 'floor': '1','recipient': '000','phone': '+77777777777' },  {'country': 'Россия','postcode': '346780', 'city': 'Азов', 'street': 'улица Пушкина', 'house': '27', 'floor': '1','recipient': '000','phone': '+77777777777' },  {'country': 'Россия','postcode': '346780', 'city': 'Азов', 'street': 'улица Мира', 'house': '24', 'floor': '1','recipient': '000','phone': '+77777777777' }]"
                      flashShopPromoID: "flash1"
                      deliveryType: "DELIVERY"
                    checkouterConfig:
                      balancer: "http://checkouter.tst.vs.market.yandex.net:39001"
                  - tankConfig:
                      tankBaseUrl: "http://tank02ht.market.yandex.net:8083"
                    regionSpecificConfig:
                      deliveryServices: "78080"
                      warehouseId: "300"
                      regionId: "54"
                      addresses: "[{'country':'Россия','postcode':'620026','city':'Екатеринбург','street':'улица Карла Маркса','house':'36','floor':'8','recipient':'000','phone':'77777777777'},{'country':'Россия','postcode':'620072','city':'Екатеринбург','street':'улица Владимира Высоцкого','house':'4/1','floor':'2','recipient':'000','phone':'77777777777'},{'country':'Россия','postcode':'620075','city':'Екатеринбург','street':'улица Малышева','house':'51','floor':'12','recipient':'000','phone':'77777777777'},{'country':'Россия','postcode':'620103','city':'Екатеринбург','street':'Лучистая улица','house':'4','floor':'3','recipient':'000','phone':'77777777777'},{'country':'Россия','postcode':'620130','city':'Екатеринбург','street':'улица 8 Марта','house':'173 ','floor':'10','recipient':'000','phone':'77777777777'}]"
                      flashShopPromoID: "flash2"
                      deliveryType: "DELIVERY"
                    checkouterConfig:
                      balancer: "http://checkouter.tst.vs.market.yandex.net:39001\"""";
        ShootingConfigConverterIn.YAML_MAPPER.readValue(yaml, ShootingConfigValue.class);
    }
}

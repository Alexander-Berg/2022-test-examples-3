package ru.yandex.market.mbo.mdm.common.masterdata.services.iris;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.GoldComputationContext;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.MskuToSskuSilverItem;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.Rsl;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.SskuRsl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mbo.mdm.common.util.TimestampUtil;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class SurplusAndCisGoldenItemServiceImplTest extends MdmBaseDbTestClass {
    private static final int FIRST_PARTY_SUPPLIER_ID = 465852; //beru_id
    private static final int THIRD_PARTY_SUPPLIER_ID = 1;
    private static final String SHOP_SKU = "pesto";
    private static final ShopSkuKey FIRST_PARTY_SHOP_SKU_KEY = new ShopSkuKey(FIRST_PARTY_SUPPLIER_ID, SHOP_SKU);
    private static final ShopSkuKey THIRD_PARTY_SHOP_SKU_KEY = new ShopSkuKey(THIRD_PARTY_SUPPLIER_ID, SHOP_SKU);

    private SurplusAndCisGoldenItemService surplusAndCisGoldenItemService;

    @Before
    public void before() {
        surplusAndCisGoldenItemService = new SurplusAndCisGoldenItemServiceImpl(new BeruIdMock());
    }

    @Test
    public void whenExistingGoldenItemIsEmptyThenShouldCreateItWithOneInfoBlockOfBothSurplusAndCis() {
        Optional<ReferenceItemWrapper> newGoldenItem = surplusAndCisGoldenItemService.calculateGoldenItem(
            THIRD_PARTY_SHOP_SKU_KEY, GoldComputationContext.EMPTY_CONTEXT, List.of(), null);

        basicCheck(newGoldenItem, 1);

        MdmIrisPayload.ReferenceInformation mdmInfo = newGoldenItem.get().getItem().getInformation(0);
        //источник берется из 1-го сгенерированного подблока, surplus генерится в первую очередь, поэтому он и источник
        checkInformationBlockSource(mdmInfo.getSource(), MasterDataSourceType.SURPLUS_SOURCE_ID);

        var surplusHandleInfo = mdmInfo.getSurplusHandleInfo();
        checkSurplus(surplusHandleInfo, MdmIrisPayload.SurplusHandleMode.ACCEPT);

        var cisHandleInfo = mdmInfo.getCisHandleInfo();
        checkCis(cisHandleInfo, MdmIrisPayload.CisHandleMode.NO_RESTRICTION);
    }

    @Test
    public void whenGoldenItemAlreadyContainsSurplusThenShouldAddToTheSameInfoBlockOnlyCis() {
        // 0. Создаем золотую запись, в которой есть данные по surplus.
        var surplusInformation = ItemWrapperTestUtil.createSurplusReferenceInfo(
            MdmIrisPayload.SurplusHandleMode.REJECT);
        var existingGoldenItem = new ReferenceItemWrapper(FIRST_PARTY_SHOP_SKU_KEY);
        existingGoldenItem.setReferenceItem(existingGoldenItem.getItem().toBuilder()
            .addInformation(surplusInformation)
            .build());

        Optional<ReferenceItemWrapper> newGoldenItem = surplusAndCisGoldenItemService.calculateGoldenItem(
            FIRST_PARTY_SHOP_SKU_KEY, GoldComputationContext.EMPTY_CONTEXT, List.of(), existingGoldenItem);

        // 1. Проверяем, что в золоте по-прежнему есть только единственный блок с информацией.
        basicCheck(newGoldenItem, 1);

        // 2. Проверяем, что источником для блока с информацией остался surplus.
        var mdmInfo = newGoldenItem.get().getItem().getInformation(0);
        //источник берется из 1-го ненулевого подблока в существующем золоте, в данном случае - это surplus
        checkInformationBlockSource(mdmInfo.getSource(), MasterDataSourceType.SURPLUS_SOURCE_ID);

        // 3. Проверяем, что содержимое подблока с surplus не изменилось.
        var resultingSurplusHandleInfo = mdmInfo.getSurplusHandleInfo();
        var expectedSurplusHandleInfo = existingGoldenItem.getItem().getInformation(0).getSurplusHandleInfo();
        Assertions.assertThat(resultingSurplusHandleInfo).isEqualTo(expectedSurplusHandleInfo);

        // 4. Проверяем, что сгенерировалось корректное содержимое подблока с cis.
        var cisHandleInfo = mdmInfo.getCisHandleInfo();
        checkCis(cisHandleInfo, MdmIrisPayload.CisHandleMode.ACCEPT_ONLY_DECLARED);
    }

    @Test
    public void whenGoldenItemContainsInvalidSurplusAndCisThenNeedToUpdateThem() {
        // 0. Генерируем золотую запись для sku, привязанной к 1P-поставщику.
        // В итоге флаги для surplus и cis должны быть другими: REJECT и ACCEPT_ONLY_DECLARED, т.к. это 1P-поставщик.
        var surplusAndCisInformation = ItemWrapperTestUtil.createSurplusAndCisReferenceInfo(
            MdmIrisPayload.SurplusHandleMode.ACCEPT, MdmIrisPayload.CisHandleMode.NO_RESTRICTION);

        var warehouseInformation = createShippingUnitReferenceInfo();

        var existingGoldenItem = new ReferenceItemWrapper(FIRST_PARTY_SHOP_SKU_KEY);
        existingGoldenItem.setReferenceItem(existingGoldenItem.getItem().toBuilder()
            .addInformation(warehouseInformation)
            .addInformation(surplusAndCisInformation)
            .build());

        Optional<ReferenceItemWrapper> newGoldenItem = surplusAndCisGoldenItemService.calculateGoldenItem(
            FIRST_PARTY_SHOP_SKU_KEY, GoldComputationContext.EMPTY_CONTEXT, List.of(), existingGoldenItem);

        // 1. Проверяем, что в золоте по-прежнему есть 2 блока с информацией.
        basicCheck(newGoldenItem, 2);

        // 2. Проверяем, что источником для 2-го блока с информацией остался surplus.
        var mdmInfo = newGoldenItem.get().getItem().getInformationList().stream()
            .filter(info -> info.getSource().getType() == MdmIrisPayload.MasterDataSource.MDM)
            .findFirst().get();
        // источник берется из блока с surplus и cis (пришли в золоте),
        // surplus создается в первую очередь, поэтому он и источник
        checkInformationBlockSource(mdmInfo.getSource(), MasterDataSourceType.SURPLUS_SOURCE_ID);

        var surplusHandleInfo = mdmInfo.getSurplusHandleInfo();
        checkSurplus(surplusHandleInfo, MdmIrisPayload.SurplusHandleMode.REJECT);

        var cisHandleInfo = mdmInfo.getCisHandleInfo();
        checkCis(cisHandleInfo, MdmIrisPayload.CisHandleMode.ACCEPT_ONLY_DECLARED);
    }

    @Test
    public void whenExistingGoldenItemAlreadyContainsValidSurplusAndCisThenShouldNotUpdateExistingGoldenItem() {
        var surplusAndCisInformation = ItemWrapperTestUtil.createSurplusAndCisReferenceInfo(
            MdmIrisPayload.SurplusHandleMode.ACCEPT, MdmIrisPayload.CisHandleMode.NO_RESTRICTION);

        var existingGoldenItem = new ReferenceItemWrapper(THIRD_PARTY_SHOP_SKU_KEY);
        existingGoldenItem.setReferenceItem(existingGoldenItem.getItem().toBuilder()
            .addInformation(surplusAndCisInformation)
            .build());

        Optional<ReferenceItemWrapper> newGoldenItem = surplusAndCisGoldenItemService.calculateGoldenItem(
            THIRD_PARTY_SHOP_SKU_KEY, GoldComputationContext.EMPTY_CONTEXT, List.of(), existingGoldenItem);

        // Новая золотая запись не должна была сгенерироваться, т.к. в существующем золоте все блоки корректны.
        Assertions.assertThat(newGoldenItem).isEmpty();
    }

    @Test
    public void whenExistingGoldenItemContainsOnlyCisThenShouldAlsoGenerateSurplusInTheSameInfoBlock() {
        // 0. Генерируем золотую запись для sku, привязанной к 3P-поставщику.
        // В итоге в золоте также должен появиться подблок с surplus.
        var surplusAndCisInformation = ItemWrapperTestUtil.createCisReferenceInfo(
            MdmIrisPayload.CisHandleMode.ACCEPT_ONLY_DECLARED); // должно быть NO_RESTRICTION

        var existingGoldenItem = new ReferenceItemWrapper(THIRD_PARTY_SHOP_SKU_KEY);
        existingGoldenItem.setReferenceItem(existingGoldenItem.getItem().toBuilder()
            .addInformation(surplusAndCisInformation)
            .build());

        Optional<ReferenceItemWrapper> newGoldenItem = surplusAndCisGoldenItemService.calculateGoldenItem(
                THIRD_PARTY_SHOP_SKU_KEY, GoldComputationContext.EMPTY_CONTEXT, List.of(), existingGoldenItem);

        // 1. Проверяем, что в золоте по-прежнему остался единственный блок с информацией.
        basicCheck(newGoldenItem, 1);

        // 2. Проверяем, что источником для блока с информацией является surplus.
        var mdmInfo = newGoldenItem.get().getItem().getInformation(0);
        // источник берется из 1-го ненулевого подблока (то, что пришло в золоте), в данном случае - это cis
        checkInformationBlockSource(mdmInfo.getSource(), MasterDataSourceType.CIS_SOURCE_ID);

        var surplusHandleInfo = mdmInfo.getSurplusHandleInfo();
        checkSurplus(surplusHandleInfo, MdmIrisPayload.SurplusHandleMode.ACCEPT);

        var cisHandleInfo = mdmInfo.getCisHandleInfo();
        checkCis(cisHandleInfo, MdmIrisPayload.CisHandleMode.NO_RESTRICTION);
    }

    @Test
    public void testRslRemainsUnchangedWhenSurplusAndCisAddedToTheSameInfoBlock() {
        var weightDimensionsInfo = MdmIrisPayload.ReferenceInformation.newBuilder()
            .setSource(MdmIrisPayload.Associate.newBuilder()
                .setType(MdmIrisPayload.MasterDataSource.WAREHOUSE).setId("145").build())
            .setItemShippingUnit(
                ItemWrapperTestUtil.generateShippingUnit(10.0, 10.0, 10.0, 1.0, null, null, null))
            .build();
        var rslReferenceInfo = createRslReferenceInfo();

        var existingGoldenItem = new ReferenceItemWrapper();
        existingGoldenItem.setReferenceItem(MdmIrisPayload.Item.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setSupplierId(FIRST_PARTY_SUPPLIER_ID)
                .setShopSku(SHOP_SKU)
                .build())
            .addInformation(weightDimensionsInfo)
            .addInformation(rslReferenceInfo)
            .build());

        Optional<ReferenceItemWrapper> newGoldenItem = surplusAndCisGoldenItemService.calculateGoldenItem(
            FIRST_PARTY_SHOP_SKU_KEY, GoldComputationContext.EMPTY_CONTEXT, List.of(), existingGoldenItem);

        // 1. Проверяем, что в золоте по-прежнему есть 2 блока с информацией.
        basicCheck(newGoldenItem, 2);

        // 2. Проверяем, что источником для блока с информацией от mdm остался rsl.
        var mdmInfo = newGoldenItem.get().getItem().getInformationList()
            .stream()
            .filter(info -> info.getSource().getType() == MdmIrisPayload.MasterDataSource.MDM)
            .findFirst().get();
        // источник берется из блока с rsl (пришло в золоте)
        checkInformationBlockSource(mdmInfo.getSource(), MasterDataSourceType.RSL_SOURCE_ID);

        // 3. Проверяем, что конфигурация rsl осталась без изменений.
        Assertions.assertThat(mdmInfo.getMinInboundLifetimeDay(0).getValue()).isEqualTo(10);
        Assertions.assertThat(mdmInfo.getMinOutboundLifetimeDay(0).getValue()).isEqualTo(20);
        Assertions.assertThat(mdmInfo.getMinInboundLifetimePercentage(0).getValue()).isEqualTo(30);
        Assertions.assertThat(mdmInfo.getMinOutboundLifetimePercentage(0).getValue()).isEqualTo(40);

        var surplusHandleInfo = mdmInfo.getSurplusHandleInfo();
        checkSurplus(surplusHandleInfo, MdmIrisPayload.SurplusHandleMode.REJECT);

        var cisHandleInfo = mdmInfo.getCisHandleInfo();
        checkCis(cisHandleInfo, MdmIrisPayload.CisHandleMode.ACCEPT_ONLY_DECLARED);
    }

    @Test
    public void testCisFor1PAcceptOnlyDeclaredWhenSuitableHonestSignIsMissing() {
        var mskuSilverItems = List.of(
            new MskuToSskuSilverItem(
                List.of(TestMdmParamUtils.createMskuParamValue(KnownMdmParams.CIS_NOT_REQUIRED_FOR_1P, 1L, true,
                    null, null, null, MasterDataSourceType.MDM_OPERATOR, Instant.now())),
                FIRST_PARTY_SHOP_SKU_KEY
            )
        );

        basic1PCisTest(mskuSilverItems, MdmIrisPayload.CisHandleMode.ACCEPT_ONLY_DECLARED);
    }

    @Test
    public void testCisFor1PAcceptOnlyDeclaredWhenNotRequiredFlagIsMissing() {
        var mskuSilverItems = List.of(
            new MskuToSskuSilverItem(
                List.of(TestMdmParamUtils.createMskuParamValue(KnownMdmParams.HONEST_SIGN_CIS_DISTINCT, 1L, true,
                    null, null, null, MasterDataSourceType.MDM_OPERATOR, Instant.now())),
                FIRST_PARTY_SHOP_SKU_KEY
            ),
            new MskuToSskuSilverItem(
                List.of(TestMdmParamUtils.createMskuParamValue(KnownMdmParams.HONEST_SIGN_CIS_REQUIRED, 1L, true,
                    null, null, null, MasterDataSourceType.MDM_OPERATOR, Instant.now())),
                FIRST_PARTY_SHOP_SKU_KEY
            )
        );

        basic1PCisTest(mskuSilverItems, MdmIrisPayload.CisHandleMode.ACCEPT_ONLY_DECLARED);
    }

    @Test
    public void testCisFor1PIsNoRestrictionWhenNotRequiredFlagIsTrue() {
        var mskuSilverItems = List.of(
            new MskuToSskuSilverItem(
                List.of(TestMdmParamUtils.createMskuParamValue(KnownMdmParams.CIS_NOT_REQUIRED_FOR_1P, 1L, true,
                    null, null, null, MasterDataSourceType.MDM_OPERATOR, Instant.now())),
                FIRST_PARTY_SHOP_SKU_KEY
            ),
            new MskuToSskuSilverItem(
                List.of(TestMdmParamUtils.createMskuParamValue(KnownMdmParams.HONEST_SIGN_CIS_REQUIRED, 1L, true,
                    null, null, null, MasterDataSourceType.MDM_OPERATOR, Instant.now())),
                FIRST_PARTY_SHOP_SKU_KEY
            )
        );

        basic1PCisTest(mskuSilverItems, MdmIrisPayload.CisHandleMode.NO_RESTRICTION);
    }

    @Test
    public void testCisFor1PIsAcceptOnlyDeclaredWhenNotRequiredFlagIsFalse() {
        var mskuSilverItems = List.of(
            new MskuToSskuSilverItem(
                List.of(TestMdmParamUtils.createMskuParamValue(KnownMdmParams.CIS_NOT_REQUIRED_FOR_1P, 1L, false,
                    null, null, null, MasterDataSourceType.MDM_OPERATOR, Instant.now())),
                FIRST_PARTY_SHOP_SKU_KEY
            ),
            new MskuToSskuSilverItem(
                List.of(TestMdmParamUtils.createMskuParamValue(KnownMdmParams.HONEST_SIGN_CIS_DISTINCT, 1L, true,
                    null, null, null, MasterDataSourceType.MDM_OPERATOR, Instant.now())),
                FIRST_PARTY_SHOP_SKU_KEY
            )
        );

        basic1PCisTest(mskuSilverItems, MdmIrisPayload.CisHandleMode.ACCEPT_ONLY_DECLARED);
    }

    @Test
    public void testCisFor1PIsNotDefinedWhenNotRequiredFlagIsTrueAndCargyTypeIsFalse() {
        var mskuSilverItems = List.of(
            new MskuToSskuSilverItem(
                List.of(TestMdmParamUtils.createMskuParamValue(KnownMdmParams.CIS_NOT_REQUIRED_FOR_1P, 1L, true,
                    null, null, null, MasterDataSourceType.MDM_OPERATOR, Instant.now())),
                FIRST_PARTY_SHOP_SKU_KEY
            ),
            new MskuToSskuSilverItem(
                List.of(TestMdmParamUtils.createMskuParamValue(KnownMdmParams.HONEST_SIGN_CIS_DISTINCT, 1L, false,
                    null, null, null, MasterDataSourceType.MDM_OPERATOR, Instant.now())),
                FIRST_PARTY_SHOP_SKU_KEY
            ),
            new MskuToSskuSilverItem(
                List.of(TestMdmParamUtils.createMskuParamValue(KnownMdmParams.HONEST_SIGN_CIS_REQUIRED, 1L, false,
                    null, null, null, MasterDataSourceType.MDM_OPERATOR, Instant.now())),
                FIRST_PARTY_SHOP_SKU_KEY
            )
        );

        basic1PCisTest(mskuSilverItems, MdmIrisPayload.CisHandleMode.ACCEPT_ONLY_DECLARED);
    }

    @Test
    public void testCisFor1PIsAcceptOnlyDeclaredWhenNoMskuSilverFlagIsFalse() {
        var mskuSilverItems = List.of(
            new MskuToSskuSilverItem(
                List.of(TestMdmParamUtils.createMskuParamValue(KnownMdmParams.HONEST_SIGN_CIS_OPTIONAL, 1L, true,
                    null, null, null, MasterDataSourceType.MDM_OPERATOR, Instant.now())),
                FIRST_PARTY_SHOP_SKU_KEY
            )
        );

        basic1PCisTest(mskuSilverItems, MdmIrisPayload.CisHandleMode.ACCEPT_ONLY_DECLARED);
    }

    private void basic1PCisTest(
        List<MskuToSskuSilverItem> mskuSilverItems, MdmIrisPayload.CisHandleMode expectedCisMode
    ) {
        // 0. Генерируем золотую запись для sku, привязанной к 1P-поставщику.
        var cisInformation = ItemWrapperTestUtil.createCisReferenceInfo(
            MdmIrisPayload.CisHandleMode.ACCEPT_ONLY_DECLARED);

        var warehouseInformation = createShippingUnitReferenceInfo();

        var existingGoldenItem = new ReferenceItemWrapper(FIRST_PARTY_SHOP_SKU_KEY);
        existingGoldenItem.setReferenceItem(existingGoldenItem.getItem().toBuilder()
            .addInformation(warehouseInformation)
            .addInformation(cisInformation)
            .build());

        Optional<ReferenceItemWrapper> newGoldenItem = surplusAndCisGoldenItemService.calculateGoldenItem(
            FIRST_PARTY_SHOP_SKU_KEY, GoldComputationContext.EMPTY_CONTEXT, mskuSilverItems, existingGoldenItem);

        basicCheck(newGoldenItem, 2);

        var mdmInfo = newGoldenItem.get().getItem().getInformationList().stream()
            .filter(info -> info.getSource().getType() == MdmIrisPayload.MasterDataSource.MDM)
            .findFirst().get();
        checkInformationBlockSource(mdmInfo.getSource(), MasterDataSourceType.CIS_SOURCE_ID);

        var cisHandleInfo = mdmInfo.getCisHandleInfo();
        checkCis(cisHandleInfo, expectedCisMode);
    }

    private static void basicCheck(Optional<ReferenceItemWrapper> newGoldenItem, int expectedInformationBlockCount) {
        Assertions.assertThat(newGoldenItem).isPresent();
        Assertions.assertThat(newGoldenItem.get().getSurplusHandleMode()).isPresent();
        Assertions.assertThat(newGoldenItem.get().getCisHandleMode()).isPresent();
        Assertions.assertThat(newGoldenItem.get().getItem().getInformationCount()).isEqualTo(expectedInformationBlockCount);
    }

    private static void checkInformationBlockSource(MdmIrisPayload.Associate source, String expectedSourceId) {
        Assertions.assertThat(source.getType()).isEqualTo(MdmIrisPayload.MasterDataSource.MDM);
        Assertions.assertThat(source.getId()).isEqualTo(expectedSourceId);
    }

    private static void checkSurplus(MdmIrisPayload.SurplusHandleInfo surplusHandleInfo,
                                     MdmIrisPayload.SurplusHandleMode expectedMode) {
        Assertions.assertThat(surplusHandleInfo.getValue()).isEqualTo(expectedMode);
        checkInformationBlockSource(surplusHandleInfo.getSource(), MasterDataSourceType.SURPLUS_SOURCE_ID);
    }

    private static void checkCis(MdmIrisPayload.CisHandleInfo cisHandleInfo,
                                 MdmIrisPayload.CisHandleMode expectedMode) {
        Assertions.assertThat(cisHandleInfo.getValue()).isEqualTo(expectedMode);
        checkInformationBlockSource(cisHandleInfo.getSource(), MasterDataSourceType.CIS_SOURCE_ID);
    }

    private MdmIrisPayload.ReferenceInformation createShippingUnitReferenceInfo() {
        MdmIrisPayload.ReferenceInformation warehouseInformation = MdmIrisPayload.ReferenceInformation.newBuilder()
            .setSource(MdmIrisPayload.Associate.newBuilder()
                .setId("145")
                .setType(MdmIrisPayload.MasterDataSource.WAREHOUSE))
            .setItemShippingUnit(
                ItemWrapperTestUtil.generateShippingUnit(10.0, 12.0, 14.0, 1.0, null, null))
            .build();
        return warehouseInformation;
    }

    private MdmIrisPayload.ReferenceInformation createRslReferenceInfo() {
        SskuRsl rsl = new SskuRsl();
        rsl.setInRslDays(10);
        rsl.setOutRslDays(20);
        rsl.setInRslPercents(30);
        rsl.setOutRslPercents(40);

        var info = MdmIrisPayload.ReferenceInformation.newBuilder();
        info.setSource(MdmIrisPayload.Associate.newBuilder()
            .setId(MasterDataSourceType.RSL_SOURCE_ID)
            .setType(MdmIrisPayload.MasterDataSource.MDM)
            .build());
        info.addMinInboundLifetimeDay(protoRsl(rsl, rsl.getInRslDays()))
            .addMinInboundLifetimePercentage(protoRsl(rsl, rsl.getInRslPercents()))
            .addMinOutboundLifetimeDay(protoRsl(rsl, rsl.getOutRslDays()))
            .addMinOutboundLifetimePercentage(protoRsl(rsl, rsl.getOutRslPercents()));

        return info.build();
    }

    private MdmIrisPayload.RemainingLifetime protoRsl(Rsl rsl, int value) {
        long startTs = rsl.getActivatedAt().atStartOfDay(TimestampUtil.ZONE_ID).toInstant().toEpochMilli();
        return MdmIrisPayload.RemainingLifetime.newBuilder()
            .setValue(value)
            .setUpdatedTs(Instant.now().plusSeconds(100L).toEpochMilli())
            .setStartDate(startTs)
            .setSource(MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.MDM).build())
            .build();
    }
}

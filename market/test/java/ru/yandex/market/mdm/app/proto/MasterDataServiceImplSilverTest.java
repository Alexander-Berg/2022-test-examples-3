package ru.yandex.market.mdm.app.proto;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.FromIrisItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.FromIrisItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSupplierCachingService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruIdMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.ServiceSskuConverter;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.CommonSskuBuilder;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.MbocBaseProtoConverter;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mdm.http.MasterDataProto;

public class MasterDataServiceImplSilverTest extends MdmBaseDbTestClass {

    @Autowired
    private MasterDataRepository masterDataRepository;
    @Autowired
    private FromIrisItemRepository fromIrisItemRepository;
    @Autowired
    private ServiceSskuConverter sskuConverter;
    @Autowired
    private SilverSskuRepository silverSskuRepository;
    @Autowired
    private MdmParamCache mdmParamCache;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private MdmSupplierCachingService mdmSupplierCachingService;

    private MasterDataServiceImpl masterDataService;

    @Before
    public void setup() {
        masterDataService = new MasterDataServiceImpl(
            masterDataRepository,
            null,
            null,
            null,
            fromIrisItemRepository,
            null,
            null,
            null,
            null,
            sskuConverter,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new BeruIdMock(),
            silverSskuRepository);

        mdmSupplierRepository.insert(new MdmSupplier()
            .setType(MdmSupplierType.BUSINESS)
            .setId(BeruIdMock.DEFAULT_PROD_BIZ_ID));
        mdmSupplierRepository.insert(new MdmSupplier()
            .setType(MdmSupplierType.THIRD_PARTY)
            .setId(BeruIdMock.DEFAULT_PROD_FP_ID)
            .setBusinessEnabled(true)
            .setBusinessId(BeruIdMock.DEFAULT_PROD_BIZ_ID));
        mdmSupplierCachingService.refresh();
    }

    @Test
    public void testSilverReturned() {
        SilverSskuKey serviceKey =
            new SilverSskuKey(BeruIdMock.DEFAULT_PROD_FP_ID, "x", MasterDataSourceType.SUPPLIER, "");
        SilverSskuKey businessKey =
            new SilverSskuKey(BeruIdMock.DEFAULT_PROD_BIZ_ID, "x", MasterDataSourceType.SUPPLIER, "");

        SilverCommonSsku ssku = SilverCommonSsku.fromCommonSsku(new CommonSskuBuilder(
            mdmParamCache, businessKey.getShopSkuKey())
            .withShelfLife(5, TimeInUnits.TimeUnit.DAY, "срок годности")
            .with(KnownMdmParams.BOX_COUNT, 4L)
            .with(KnownMdmParams.MANUFACTURER, "Apple")
            .with(KnownMdmParams.WEIGHT_GROSS, 3L)
            .with(KnownMdmParams.WIDTH, 21L)
            .with(KnownMdmParams.LENGTH, 22L)
            .with(KnownMdmParams.HEIGHT, 23L)
            .startServiceValues(serviceKey.getSupplierId())
            .withVat(VatRate.VAT_18)
            .with(KnownMdmParams.QUANTITY_IN_PACK, 7L)
            .endServiceValues()
            .build(), new MasterDataSource(MasterDataSourceType.SUPPLIER, ""));

        silverSskuRepository.insertOrUpdateSsku(ssku);

        MasterData result;
        var response = masterDataService.search1PSskuSilverData(MasterDataProto.SearchSskuMasterDataRequest.newBuilder()
            .addShopSkuKeys(MbocBaseProtoConverter.pojoToProto(serviceKey.getShopSkuKey()))
            .build());

        Assertions.assertThat(response.getSskuMasterDataCount()).isEqualTo(1);
        result = MbocBaseProtoConverter.protoToPojo(response.getSskuMasterData(0));
        Assertions.assertThat(result.getShopSkuKey()).isEqualTo(serviceKey.getShopSkuKey());
        Assertions.assertThat(result.getBoxCount()).isEqualTo(4);
        Assertions.assertThat(result.getManufacturer()).isEqualTo("Apple");
        Assertions.assertThat(result.getVat()).isEqualTo(VatRate.VAT_18);
        Assertions.assertThat(result.getQuantityInPack()).isEqualTo(7);
        Assertions.assertThat(result.getWeightGross()).isEqualTo(3000000L);
        Assertions.assertThat(result.getItemShippingUnit().getWidthMicrometer().getValue()).isEqualTo(210000L);
        Assertions.assertThat(result.getItemShippingUnit().getLengthMicrometer().getValue()).isEqualTo(220000L);
        Assertions.assertThat(result.getItemShippingUnit().getHeightMicrometer().getValue()).isEqualTo(230000L);

        response = masterDataService.search1PSskuSilverData(MasterDataProto.SearchSskuMasterDataRequest.newBuilder()
            .addShopSkuKeys(MbocBaseProtoConverter.pojoToProto(businessKey.getShopSkuKey()))
            .build());

        Assertions.assertThat(response.getSskuMasterDataCount()).isEqualTo(1);
        result = MbocBaseProtoConverter.protoToPojo(response.getSskuMasterData(0));
        Assertions.assertThat(result.getShopSkuKey()).isEqualTo(businessKey.getShopSkuKey());
        Assertions.assertThat(result.getBoxCount()).isEqualTo(4);
        Assertions.assertThat(result.getManufacturer()).isEqualTo("Apple");
        Assertions.assertThat(result.getVat()).isEqualTo(VatRate.VAT_18);
        Assertions.assertThat(result.getQuantityInPack()).isEqualTo(7);
        Assertions.assertThat(result.getWeightGross()).isEqualTo(3000000L);
        Assertions.assertThat(result.getItemShippingUnit().getWidthMicrometer().getValue()).isEqualTo(210000L);
        Assertions.assertThat(result.getItemShippingUnit().getLengthMicrometer().getValue()).isEqualTo(220000L);
        Assertions.assertThat(result.getItemShippingUnit().getHeightMicrometer().getValue()).isEqualTo(230000L);
    }

    @Test
    public void testFromIrisItemReturned() {
        SilverSskuKey serviceKey =
            new SilverSskuKey(BeruIdMock.DEFAULT_PROD_FP_ID, "x", MasterDataSourceType.SUPPLIER, "");
        SilverSskuKey businessKey =
            new SilverSskuKey(BeruIdMock.DEFAULT_PROD_BIZ_ID, "x", MasterDataSourceType.SUPPLIER, "");

        FromIrisItemWrapper irisItem = sskuConverter.toIrisItem(
            new CommonSskuBuilder(mdmParamCache, businessKey.getShopSkuKey())
                .with(KnownMdmParams.WEIGHT_GROSS, 6L)
                .with(KnownMdmParams.WIDTH, 66L)
                .with(KnownMdmParams.LENGTH, 66L)
                .with(KnownMdmParams.HEIGHT, 66L)
                .buildSupplierOnly());
        var info = irisItem.getReferenceInformation().toBuilder().setSource(MdmIrisPayload.Associate.newBuilder()
            .setId("")
            .setType(MdmIrisPayload.MasterDataSource.SUPPLIER)
            .build()
        ).build();
        irisItem.setReferenceItem(irisItem.getItem().toBuilder().clearInformation().addInformation(info).build());

        fromIrisItemRepository.insert(irisItem);

        MasterData result;
        var response = masterDataService.search1PSskuSilverData(MasterDataProto.SearchSskuMasterDataRequest.newBuilder()
            .addShopSkuKeys(MbocBaseProtoConverter.pojoToProto(serviceKey.getShopSkuKey()))
            .build());

        Assertions.assertThat(response.getSskuMasterDataCount()).isEqualTo(1);
        result = MbocBaseProtoConverter.protoToPojo(response.getSskuMasterData(0));
        Assertions.assertThat(result.getShopSkuKey()).isEqualTo(serviceKey.getShopSkuKey());
        Assertions.assertThat(result.getWeightGross()).isEqualTo(6000000L);
        Assertions.assertThat(result.getItemShippingUnit().getWidthMicrometer().getValue()).isEqualTo(660000L);
        Assertions.assertThat(result.getItemShippingUnit().getLengthMicrometer().getValue()).isEqualTo(660000L);
        Assertions.assertThat(result.getItemShippingUnit().getHeightMicrometer().getValue()).isEqualTo(660000L);

        response = masterDataService.search1PSskuSilverData(MasterDataProto.SearchSskuMasterDataRequest.newBuilder()
            .addShopSkuKeys(MbocBaseProtoConverter.pojoToProto(businessKey.getShopSkuKey()))
            .build());

        Assertions.assertThat(response.getSskuMasterDataCount()).isEqualTo(1);
        result = MbocBaseProtoConverter.protoToPojo(response.getSskuMasterData(0));
        Assertions.assertThat(result.getShopSkuKey()).isEqualTo(businessKey.getShopSkuKey());
        Assertions.assertThat(result.getWeightGross()).isEqualTo(6000000L);
        Assertions.assertThat(result.getItemShippingUnit().getWidthMicrometer().getValue()).isEqualTo(660000L);
        Assertions.assertThat(result.getItemShippingUnit().getLengthMicrometer().getValue()).isEqualTo(660000L);
        Assertions.assertThat(result.getItemShippingUnit().getHeightMicrometer().getValue()).isEqualTo(660000L);
    }

    @Test
    public void testMasterDataReturned() {
        SilverSskuKey serviceKey =
            new SilverSskuKey(BeruIdMock.DEFAULT_PROD_FP_ID, "x", MasterDataSourceType.SUPPLIER, "");
        SilverSskuKey businessKey =
            new SilverSskuKey(BeruIdMock.DEFAULT_PROD_BIZ_ID, "x", MasterDataSourceType.SUPPLIER, "");

        MasterData masterData = new MasterData().setShopSkuKey(serviceKey.getShopSkuKey())
            .setBoxCount(6)
            .setManufacturerCountries(List.of("Россия", "Китай"));
        masterDataRepository.insert(masterData);

        MasterData result;
        var response = masterDataService.search1PSskuSilverData(MasterDataProto.SearchSskuMasterDataRequest.newBuilder()
            .addShopSkuKeys(MbocBaseProtoConverter.pojoToProto(serviceKey.getShopSkuKey()))
            .build());

        Assertions.assertThat(response.getSskuMasterDataCount()).isEqualTo(1);
        result = MbocBaseProtoConverter.protoToPojo(response.getSskuMasterData(0));
        Assertions.assertThat(result.getShopSkuKey()).isEqualTo(serviceKey.getShopSkuKey());
        Assertions.assertThat(result.getBoxCount()).isEqualTo(6);
        Assertions.assertThat(result.getManufacturerCountries()).containsExactlyInAnyOrder("Россия", "Китай");

        response = masterDataService.search1PSskuSilverData(MasterDataProto.SearchSskuMasterDataRequest.newBuilder()
            .addShopSkuKeys(MbocBaseProtoConverter.pojoToProto(businessKey.getShopSkuKey()))
            .build());

        Assertions.assertThat(response.getSskuMasterDataCount()).isEqualTo(1);
        result = MbocBaseProtoConverter.protoToPojo(response.getSskuMasterData(0));
        Assertions.assertThat(result.getShopSkuKey()).isEqualTo(businessKey.getShopSkuKey());
        Assertions.assertThat(result.getBoxCount()).isEqualTo(6);
        Assertions.assertThat(result.getManufacturerCountries()).containsExactlyInAnyOrder("Россия", "Китай");
    }

    @Test
    public void testSilverDominatesMdAndReferenceItem() {
        SilverSskuKey serviceKey =
            new SilverSskuKey(BeruIdMock.DEFAULT_PROD_FP_ID, "x", MasterDataSourceType.SUPPLIER, "");
        SilverSskuKey businessKey =
            new SilverSskuKey(BeruIdMock.DEFAULT_PROD_BIZ_ID, "x", MasterDataSourceType.SUPPLIER, "");

        SilverCommonSsku ssku = SilverCommonSsku.fromCommonSsku(new CommonSskuBuilder(
            mdmParamCache, businessKey.getShopSkuKey())
            .withShelfLife(5, TimeInUnits.TimeUnit.DAY, "срок годности")
            .with(KnownMdmParams.BOX_COUNT, 4L)
            .with(KnownMdmParams.MANUFACTURER, "Apple")
            .with(KnownMdmParams.WEIGHT_GROSS, 3L)
            .with(KnownMdmParams.WIDTH, 21L)
            .with(KnownMdmParams.LENGTH, 22L)
            .with(KnownMdmParams.HEIGHT, 23L)
            .startServiceValues(serviceKey.getSupplierId())
            .withVat(VatRate.VAT_18)
            .with(KnownMdmParams.QUANTITY_IN_PACK, 7L)
            .endServiceValues()
            .build(), new MasterDataSource(MasterDataSourceType.SUPPLIER, ""));

        FromIrisItemWrapper irisItem = sskuConverter.toIrisItem(
            new CommonSskuBuilder(mdmParamCache, businessKey.getShopSkuKey())
                .with(KnownMdmParams.WEIGHT_GROSS, 6L)
                .with(KnownMdmParams.WIDTH, 66L)
                .with(KnownMdmParams.LENGTH, 66L)
                .with(KnownMdmParams.HEIGHT, 66L)
                .buildSupplierOnly());
        var info = irisItem.getReferenceInformation().toBuilder().setSource(MdmIrisPayload.Associate.newBuilder()
            .setId("")
            .setType(MdmIrisPayload.MasterDataSource.SUPPLIER)
            .build()
        ).build();
        irisItem.setReferenceItem(irisItem.getItem().toBuilder().clearInformation().addInformation(info).build());

        MasterData masterData = new MasterData().setShopSkuKey(serviceKey.getShopSkuKey())
            .setBoxCount(6)
            .setManufacturerCountries(List.of("Россия", "Китай"));

        silverSskuRepository.insertOrUpdateSsku(ssku);
        fromIrisItemRepository.insert(irisItem);
        masterDataRepository.insert(masterData);

        MasterData result;
        var response = masterDataService.search1PSskuSilverData(MasterDataProto.SearchSskuMasterDataRequest.newBuilder()
            .addShopSkuKeys(MbocBaseProtoConverter.pojoToProto(serviceKey.getShopSkuKey()))
            .build());

        Assertions.assertThat(response.getSskuMasterDataCount()).isEqualTo(1);
        result = MbocBaseProtoConverter.protoToPojo(response.getSskuMasterData(0));
        Assertions.assertThat(result.getShopSkuKey()).isEqualTo(serviceKey.getShopSkuKey());
        Assertions.assertThat(result.getBoxCount()).isEqualTo(4);
        Assertions.assertThat(result.getManufacturer()).isEqualTo("Apple");
        Assertions.assertThat(result.getVat()).isEqualTo(VatRate.VAT_18);
        Assertions.assertThat(result.getQuantityInPack()).isEqualTo(7);
        Assertions.assertThat(result.getWeightGross()).isEqualTo(3000000L);
        Assertions.assertThat(result.getItemShippingUnit().getWidthMicrometer().getValue()).isEqualTo(210000L);
        Assertions.assertThat(result.getItemShippingUnit().getLengthMicrometer().getValue()).isEqualTo(220000L);
        Assertions.assertThat(result.getItemShippingUnit().getHeightMicrometer().getValue()).isEqualTo(230000L);
        Assertions.assertThat(result.getManufacturerCountries()).containsExactlyInAnyOrder("Россия", "Китай");

        response = masterDataService.search1PSskuSilverData(MasterDataProto.SearchSskuMasterDataRequest.newBuilder()
            .addShopSkuKeys(MbocBaseProtoConverter.pojoToProto(businessKey.getShopSkuKey()))
            .build());

        Assertions.assertThat(response.getSskuMasterDataCount()).isEqualTo(1);
        result = MbocBaseProtoConverter.protoToPojo(response.getSskuMasterData(0));
        Assertions.assertThat(result.getShopSkuKey()).isEqualTo(businessKey.getShopSkuKey());
        Assertions.assertThat(result.getBoxCount()).isEqualTo(4);
        Assertions.assertThat(result.getManufacturer()).isEqualTo("Apple");
        Assertions.assertThat(result.getVat()).isEqualTo(VatRate.VAT_18);
        Assertions.assertThat(result.getQuantityInPack()).isEqualTo(7);
        Assertions.assertThat(result.getWeightGross()).isEqualTo(3000000L);
        Assertions.assertThat(result.getItemShippingUnit().getWidthMicrometer().getValue()).isEqualTo(210000L);
        Assertions.assertThat(result.getItemShippingUnit().getLengthMicrometer().getValue()).isEqualTo(220000L);
        Assertions.assertThat(result.getItemShippingUnit().getHeightMicrometer().getValue()).isEqualTo(230000L);
        Assertions.assertThat(result.getManufacturerCountries()).containsExactlyInAnyOrder("Россия", "Китай");
    }

    @Test
    public void testNonSupplierSilverIgnored() {
        SilverSskuKey serviceKey =
            new SilverSskuKey(BeruIdMock.DEFAULT_PROD_FP_ID, "x", MasterDataSourceType.MEASUREMENT, "");
        SilverSskuKey businessKey =
            new SilverSskuKey(BeruIdMock.DEFAULT_PROD_BIZ_ID, "x", MasterDataSourceType.MEASUREMENT, "");

        SilverCommonSsku ssku = SilverCommonSsku.fromCommonSsku(new CommonSskuBuilder(
            mdmParamCache, businessKey.getShopSkuKey())
            .withShelfLife(5, TimeInUnits.TimeUnit.DAY, "срок годности")
            .with(KnownMdmParams.BOX_COUNT, 4L)
            .with(KnownMdmParams.MANUFACTURER, "Apple")
            .with(KnownMdmParams.WEIGHT_GROSS, 3L)
            .with(KnownMdmParams.WIDTH, 21L)
            .with(KnownMdmParams.LENGTH, 22L)
            .with(KnownMdmParams.HEIGHT, 23L)
            .startServiceValues(serviceKey.getSupplierId())
            .withVat(VatRate.VAT_18)
            .with(KnownMdmParams.QUANTITY_IN_PACK, 7L)
            .endServiceValues()
            .build(), new MasterDataSource(MasterDataSourceType.MEASUREMENT, ""));

        FromIrisItemWrapper irisItem = sskuConverter.toIrisItem(
            new CommonSskuBuilder(mdmParamCache, businessKey.getShopSkuKey())
                .with(KnownMdmParams.WEIGHT_GROSS, 6L)
                .with(KnownMdmParams.WIDTH, 66L)
                .with(KnownMdmParams.LENGTH, 66L)
                .with(KnownMdmParams.HEIGHT, 66L)
                .buildSupplierOnly());
        var info = irisItem.getReferenceInformation().toBuilder().setSource(MdmIrisPayload.Associate.newBuilder()
            .setId("")
            .setType(MdmIrisPayload.MasterDataSource.MEASUREMENT)
            .build()
        ).build();
        irisItem.setReferenceItem(irisItem.getItem().toBuilder().clearInformation().addInformation(info).build());

        silverSskuRepository.insertOrUpdateSsku(ssku);
        fromIrisItemRepository.insert(irisItem);

        var response = masterDataService.search1PSskuSilverData(MasterDataProto.SearchSskuMasterDataRequest.newBuilder()
            .addShopSkuKeys(MbocBaseProtoConverter.pojoToProto(serviceKey.getShopSkuKey()))
            .build());

        Assertions.assertThat(response.getSskuMasterDataCount()).isZero();
        response = masterDataService.search1PSskuSilverData(MasterDataProto.SearchSskuMasterDataRequest.newBuilder()
            .addShopSkuKeys(MbocBaseProtoConverter.pojoToProto(businessKey.getShopSkuKey()))
            .build());
        Assertions.assertThat(response.getSskuMasterDataCount()).isZero();
    }
}

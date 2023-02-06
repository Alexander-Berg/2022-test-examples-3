package ru.yandex.market.mbo.mdm.common.masterdata.services.ssku;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.VghValidationRequirements;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuGoldenParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.RslMarkups;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.MdmSskuKeyGroup;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.FromIrisItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MdmQueuesManager;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SendToDatacampQRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.ExistingGoldenItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.GoldenItemSaveResult;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.ServiceSskuConverter;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuProcessingData;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuProcessingPipeProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuProcessingPipeProcessorImpl;
import ru.yandex.market.mbo.mdm.common.util.SskuGoldenParamUtil;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

/**
 * Тест проверяет, что при отправке в Датакемп в send_to_datacamp-очередь добавляется только
 * - business-ключи AND
 * - по которым произошли существенные изменения после вычисления золота ssku
 */
public class SskuProcessingPipeProcessorSendToDatacampTest extends MdmBaseDbTestClass {
    private static final String WAREHOUSE_ID = "172";

    private static int supplierIdSeq = 1;
    private static final String SKU_A = "a";

    private static final ShopSkuKey BUSINESS_KEY_A = new ShopSkuKey(supplierIdSeq++, SKU_A);
    private static final ShopSkuKey SERVICE_A1 = new ShopSkuKey(supplierIdSeq++, SKU_A);
    private static final ShopSkuKey SERVICE_A2 = new ShopSkuKey(supplierIdSeq++, SKU_A);

    @Autowired
    private MdmQueuesManager mdmQueuesManager;
    @Autowired
    private ServiceSskuConverter serviceSskuConverter;
    @Autowired
    protected SskuGoldenParamUtil sskuGoldenParamUtil;
    @Autowired
    private SendToDatacampQRepository sendToDatacampQRepository;

    private SskuProcessingPipeProcessor sskuProcessingPipeProcessor;

    @Before
    public void setup() {
        sskuProcessingPipeProcessor = new SskuProcessingPipeProcessorImpl(mdmQueuesManager, serviceSskuConverter);
    }

    @Test
    public void enqueueBizKeyTestToSendToDatacampQueue() {
        // given
        SskuProcessingData sskuProcessingData = generateDataWithTestContent();

        ReferenceItemWrapper goldenItemCommon = new ReferenceItemWrapper(ItemWrapperTestUtil.createItem(
            SERVICE_A1, MdmIrisPayload.MasterDataSource.WAREHOUSE, WAREHOUSE_ID,
            ItemWrapperTestUtil.generateShippingUnit(
                31.0, 32.0, 5.0, 0.4, 0.4, null,
                1234L)));
        List<SskuGoldenParamValue> goldenParamValues = sskuGoldenParamUtil.createSskuGoldenParamValuesFromReferenceItem(
            goldenItemCommon, SskuGoldenParamUtil.ParamsGroup.COMMON);

        CommonSsku commonSsku = new CommonSsku(SERVICE_A1);
        Stream.of(goldenParamValues)
            .flatMap(List::stream)
            .forEach(commonSsku::addBaseValue);

        MasterData masterData = generateMasterDataForSsku(SERVICE_A1);

        GoldenItemSaveResult goldenItemSaveResult = new GoldenItemSaveResult(
            Map.of(SERVICE_A1, goldenItemCommon),
            Map.of(SERVICE_A1, commonSsku),
            Map.of(SERVICE_A1, masterData)
        );

        // when
        sskuProcessingPipeProcessor.sendToDatacamp(sskuProcessingData, goldenItemSaveResult, 0);

        // then
        Assertions.assertThat(sendToDatacampQRepository.findAll().size()).isOne();
        Assertions.assertThat(sendToDatacampQRepository.findAll().get(0).getEntityKey()).isEqualTo(BUSINESS_KEY_A);
    }

    private MasterData generateMasterDataForSsku(ShopSkuKey offer) {
        MasterData md = new MasterData();
        md.setShopSkuKey(offer);
        md.setVat(VatRate.fromString("7").orElseThrow());
        md.setGtins(List.of("6930145000617"));
        md.setMinShipment(0);
        md.setDeliveryTime(0);
        md.setManufacturer("Gold apple");
        md.setQuantityInPack(0);
        md.setQuantumOfSupply(0);
        md.setShelfLifeRequired(false);
        md.setTransportUnitSize(0);
        md.setManufacturerCountries(List.of("Россия"));

        return md;
    }

    private static SskuProcessingData generateDataWithTestContent() {
        Set<ShopSkuKey> initialKeys = Set.of(SERVICE_A1);
        Set<ShopSkuKey> allServiceKeys = Set.of(SERVICE_A1, SERVICE_A2);
        List<MdmSskuKeyGroup> keyGroups = List.of(
            MdmSskuKeyGroup.createBusinessGroup(BUSINESS_KEY_A, List.of(SERVICE_A1, SERVICE_A2))
        );
        Map<ShopSkuKey, MdmSskuKeyGroup> groupsByKeys = mapify(keyGroups);
        Map<ShopSkuKey, MappingCacheDao> mappings = Map.of(
            SERVICE_A2, new MappingCacheDao().setMskuId(78888593L)
        );
        Map<ShopSkuKey, List<FromIrisItemWrapper>> irisItems = Map.of(SERVICE_A2, List.of());
        Map<ShopSkuKey, List<SilverCommonSsku>> silverItems = Map.of(SERVICE_A2, List.of());
        Map<ShopSkuKey, ExistingGoldenItemWrapper> goldenItems = Map.of(SERVICE_A2, gold());
        Map<ShopSkuKey, MasterData> masterData = Map.of(SERVICE_A2, new MasterData());

        RslMarkups rslMarkups = new RslMarkups();
        return new SskuProcessingData(
            initialKeys,
            allServiceKeys,
            keyGroups,
            groupsByKeys,
            mappings,
            irisItems,
            silverItems,
            goldenItems,
            Map.of(),
            Map.of(),
            Map.of(),
            masterData,
            VghValidationRequirements.NO_REQUIREMENTS,
            Set.of(),
            rslMarkups,
            Map.of(),
            Map.of(),
            Map.of(),
            Set.of(),
            Map.of(),
            Map.of(),
            Map.of());
    }

    private static Map<ShopSkuKey, MdmSskuKeyGroup> mapify(List<MdmSskuKeyGroup> keyGroups) {
        Map<ShopSkuKey, MdmSskuKeyGroup> result = new HashMap<>();
        for (MdmSskuKeyGroup group : keyGroups) {
            group.getAll().forEach(k -> result.put(k, group));
        }
        return result;
    }

    private static ExistingGoldenItemWrapper gold() {
        return new ExistingGoldenItemWrapper(new ReferenceItemWrapper(), new ReferenceItemWrapper());
    }
}

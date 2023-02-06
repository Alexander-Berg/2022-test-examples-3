package ru.yandex.market.mbo.mdm.common.masterdata.services.business;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.ServiceOfferMigrationInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.MdmSskuGroup;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.MdmSskuKeyGroup;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.ServiceSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmBusinessStage;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ServiceOfferMigrationRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruIdMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.ServiceSskuConverter;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.DocumentOfferRelation;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.masterdata.repository.document.QualityDocumentRepository;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

@SuppressWarnings("checkstyle:MagicNumber")
public class MdmSskuGroupManagerTest extends MdmBaseDbTestClass {
    private static final int SUPPLIER_BIZ_U = 777;
    private static final int SUPPLIER_BIZ_V = 888;
    private static final int SUPPLIER_3P_XU = 1234;
    private static final int SUPPLIER_3P_YU = 1235;
    private static final int SUPPLIER_3P_ZV = 1236;
    private static final int SUPPLIER_3P_ORPHAN = 2222;
    private static final int SUPPLIER_BIZ_ORPHAN = 111;

    private static final int SUPPLIER_WHITE_1U = 2347;
    private static final int SUPPLIER_WHITE_ORPHAN = 3333;

    private static final int SUPPLIER_UNKNOWN = 9999;

    private static final int SUPPLIER_1P_FP = BeruIdMock.DEFAULT_PROD_FP_ID;
    private static final int SUPPLIER_1P_BIZ = BeruIdMock.DEFAULT_PROD_BIZ_ID;

    @Autowired
    private MasterDataRepository masterDataRepository;

    @Autowired
    private QualityDocumentRepository qualityDocumentRepository;

    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;

    @Autowired
    private ReferenceItemRepository referenceItemRepository;

    @Autowired
    private ServiceSskuConverter converter;

    @Autowired
    private StorageKeyValueService storageKeyValueService;
    @Autowired
    private MdmSupplierCachingService mdmSupplierCachingService;
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private ServiceOfferMigrationRepository serviceOfferMigrationRepository;
    @Autowired
    private SskuExistenceRepository sskuExistenceRepository;

    private MdmSskuGroupManager groupManager;
    private EnhancedRandom random;

    @Before
    public void setup() {
        random = TestDataUtils.defaultRandom(1234560);
        groupManager = new MdmSskuGroupManagerImpl(masterDataRepository, referenceItemRepository,
            new MdmSupplierCachingServiceImpl(mdmSupplierRepository, storageKeyValueService), converter,
            mappingsCacheRepository,
            serviceOfferMigrationRepository, storageKeyValueService,
                sskuExistenceRepository, new BeruIdMock());
        mdmSupplierRepository.insertBatch(
            supplier(SUPPLIER_BIZ_U, MdmSupplierType.BUSINESS, null),
            supplier(SUPPLIER_BIZ_V, MdmSupplierType.BUSINESS, null),
            supplier(SUPPLIER_BIZ_ORPHAN, MdmSupplierType.BUSINESS, null),
            supplier(SUPPLIER_3P_XU, MdmSupplierType.THIRD_PARTY, SUPPLIER_BIZ_U),
            supplier(SUPPLIER_3P_YU, MdmSupplierType.THIRD_PARTY, SUPPLIER_BIZ_U),
            supplier(SUPPLIER_3P_ZV, MdmSupplierType.THIRD_PARTY, SUPPLIER_BIZ_V),
            supplier(SUPPLIER_3P_ORPHAN, MdmSupplierType.THIRD_PARTY, null),
            supplier(SUPPLIER_WHITE_1U, MdmSupplierType.MARKET_SHOP, SUPPLIER_BIZ_U),
            supplier(SUPPLIER_WHITE_ORPHAN, MdmSupplierType.MARKET_SHOP, null),
            supplier(SUPPLIER_1P_BIZ, MdmSupplierType.BUSINESS, null)
        );

        sskuExistenceRepository.markExistence(List.of(
            new ShopSkuKey(SUPPLIER_3P_XU, "u"),
            new ShopSkuKey(SUPPLIER_3P_YU, "u"),
            new ShopSkuKey(SUPPLIER_3P_ZV, "v"),
            new ShopSkuKey(SUPPLIER_3P_ORPHAN, "lonely 3p"),
            new ShopSkuKey(SUPPLIER_WHITE_1U, "u"),
            new ShopSkuKey(SUPPLIER_WHITE_ORPHAN, "lonely white")
        ), true);

        storageKeyValueService.invalidateCache();
        mdmSupplierCachingService.refresh();
    }

    @Test
    public void whenSearchForOrphansShouldReturnSeparateOrphans() {
        ServiceSsku businessOrphan = generateMD(SUPPLIER_BIZ_ORPHAN, "lonely business");
        ServiceSsku thirdPartyOrphan = generateMD(SUPPLIER_3P_ORPHAN, "lonely 3p");
        ServiceSsku whiteOrphan = generateMD(SUPPLIER_WHITE_ORPHAN, "lonely white");
        ServiceSsku unknown = generateMD(SUPPLIER_UNKNOWN, "unknown");

        List<MdmSskuGroup> groups = groupManager.findGroupsByKeys(List.of(
            businessOrphan.getKey(),
            thirdPartyOrphan.getKey(),
            whiteOrphan.getKey(),
            unknown.getKey()
        ));

        Assertions.assertThat(groups).containsExactlyInAnyOrder(
            MdmSskuGroup.createNoBusinessGroup(thirdPartyOrphan)
        );
    }

    @Test
    public void whenSearchForBusinessByOneOfRelatedSuppliersShouldFetchWholeGroup() {
        ServiceSsku thirdPartyXU = generateMD(SUPPLIER_3P_XU, "u");
        ServiceSsku thirdPartyYU = generateMD(SUPPLIER_3P_YU, "u");
        ServiceSsku thirdPartyZV = generateMD(SUPPLIER_3P_ZV, "v");
        ServiceSsku businessU = generateMD(SUPPLIER_BIZ_U, "u");
        ServiceSsku businessV = generateMD(SUPPLIER_BIZ_V, "v");
        ServiceSsku white1U = generateMD(SUPPLIER_WHITE_1U, "u");

        List<MdmSskuGroup> groups = groupManager.findGroupsByKeys(List.of(
            thirdPartyYU.getKey(),
            thirdPartyZV.getKey()
        ));

        Assertions.assertThat(groups).containsExactlyInAnyOrder(
            MdmSskuGroup.createBusinessGroup(businessU, List.of(thirdPartyXU, thirdPartyYU)),
            MdmSskuGroup.createBusinessGroup(businessV, List.of(thirdPartyZV))
        );
    }

    @Test
    public void whenSearchForBusinessByBusinessKeysShouldFetchWholeGroup() {
        ServiceSsku thirdPartyXU = generateMD(SUPPLIER_3P_XU, "u");
        ServiceSsku thirdPartyYU = generateMD(SUPPLIER_3P_YU, "u");
        ServiceSsku thirdPartyZV = generateMD(SUPPLIER_3P_ZV, "v");
        ServiceSsku businessU = generateMD(SUPPLIER_BIZ_U, "u");
        ServiceSsku businessV = generateMD(SUPPLIER_BIZ_V, "v");
        ServiceSsku white1U = generateMD(SUPPLIER_WHITE_1U, "u");

        List<MdmSskuGroup> groups = groupManager.findGroupsByKeys(List.of(
            businessU.getKey(),
            businessV.getKey()
        ));

        Assertions.assertThat(groups).containsExactlyInAnyOrder(
            MdmSskuGroup.createBusinessGroup(businessU, List.of(thirdPartyXU, thirdPartyYU)),
            MdmSskuGroup.createBusinessGroup(businessV, List.of(thirdPartyZV))
        );
    }

    @Test
    public void whenSearchByBusinessKeysShouldFetchWholeGroupExceptThoseNotFoundInEOX() {
        ServiceSsku thirdPartyXU = generateMD(SUPPLIER_3P_XU, "u");
        ServiceSsku thirdPartyYU = generateMD(SUPPLIER_3P_YU, "u");
        ServiceSsku thirdPartyZV = generateMD(SUPPLIER_3P_ZV, "v");
        ServiceSsku businessU = generateMD(SUPPLIER_BIZ_U, "u");
        ServiceSsku businessV = generateMD(SUPPLIER_BIZ_V, "v");
        sskuExistenceRepository.markExistence(new ShopSkuKey(SUPPLIER_3P_YU, "u"), false);

        List<MdmSskuGroup> groups = groupManager.findGroupsByKeys(List.of(
            businessU.getKey(),
            businessV.getKey()
        ));

        Assertions.assertThat(groups).containsExactlyInAnyOrder(
            MdmSskuGroup.createBusinessGroup(businessU, List.of(thirdPartyXU)),
            MdmSskuGroup.createBusinessGroup(businessV, List.of(thirdPartyZV))
        );
    }

    @Test
    public void whenSearchForAbsentBusinessByBusinessKeyShouldFetchWholeGroupWithPlaceholderSsku() {
        ServiceSsku thirdPartyXU = generateMD(SUPPLIER_3P_XU, "u");
        ServiceSsku thirdPartyYU = generateMD(SUPPLIER_3P_YU, "u");
        ServiceSsku white1U = generateMD(SUPPLIER_WHITE_1U, "u");
        List<MdmSskuGroup> groups = groupManager.findGroupsByKeys(List.of(
            new ShopSkuKey(SUPPLIER_BIZ_U, "u")
        ));

        ServiceSsku expectedBusinessSskuPlaceholder = new ServiceSsku(new ShopSkuKey(SUPPLIER_BIZ_U, "u"));

        Assertions.assertThat(groups).containsExactlyInAnyOrder(
            MdmSskuGroup.createBusinessGroup(expectedBusinessSskuPlaceholder, List.of(thirdPartyXU, thirdPartyYU))
        );
    }

    @Test
    public void whenSearchWithSuppliersOrphansAndBusinessShouldStillBeOk() {
        ServiceSsku thirdPartyXU = generateMD(SUPPLIER_3P_XU, "u");
        ServiceSsku thirdPartyYU = generateMD(SUPPLIER_3P_YU, "u");
        ServiceSsku thirdPartyZV = generateMD(SUPPLIER_3P_ZV, "v");
        ServiceSsku businessU = generateMD(SUPPLIER_BIZ_U, "u");
        ServiceSsku businessV = generateMD(SUPPLIER_BIZ_V, "v");
        ServiceSsku businessOrphan = generateMD(SUPPLIER_BIZ_ORPHAN, "lonely business");
        ServiceSsku thirdPartyOrphan = generateMD(SUPPLIER_3P_ORPHAN, "lonely 3p");
        ServiceSsku white1U = generateMD(SUPPLIER_WHITE_1U, "u");
        ServiceSsku whiteOrphan = generateMD(SUPPLIER_WHITE_ORPHAN, "lonely white");
        ServiceSsku unknown = generateMD(SUPPLIER_UNKNOWN, "unknown");


        List<MdmSskuGroup> groups = groupManager.findGroupsByKeys(List.of(
            thirdPartyXU.getKey(),
            businessV.getKey(),
            businessOrphan.getKey(),
            thirdPartyOrphan.getKey(),
            whiteOrphan.getKey(),
            unknown.getKey()
        ));

        Assertions.assertThat(groups).containsExactlyInAnyOrder(
            MdmSskuGroup.createBusinessGroup(businessU, List.of(thirdPartyXU, thirdPartyYU)),
            MdmSskuGroup.createBusinessGroup(businessV, List.of(thirdPartyZV)),
            MdmSskuGroup.createNoBusinessGroup(thirdPartyOrphan)
        );
    }

    @Test
    public void whenSearchAllCasesInSeparateModeShouldProvideGroupForEachKey() {
        ServiceSsku thirdPartyXU = generateMD(SUPPLIER_3P_XU, "u");
        ServiceSsku thirdPartyYU = generateMD(SUPPLIER_3P_YU, "u");
        ServiceSsku thirdPartyZV = generateMD(SUPPLIER_3P_ZV, "v");
        ServiceSsku businessU = generateMD(SUPPLIER_BIZ_U, "u");
        ServiceSsku businessV = generateMD(SUPPLIER_BIZ_V, "v");
        ServiceSsku businessOrphan = generateMD(SUPPLIER_BIZ_ORPHAN, "lonely business");
        ServiceSsku thirdPartyOrphan = generateMD(SUPPLIER_3P_ORPHAN, "lonely 3p");
        ServiceSsku white1U = generateMD(SUPPLIER_WHITE_1U, "u");
        ServiceSsku whiteOrphan = generateMD(SUPPLIER_WHITE_ORPHAN, "lonely white");
        ServiceSsku unknown = generateMD(SUPPLIER_UNKNOWN, "unknown");

        Map<ShopSkuKey, MdmSskuGroup> groups = groupManager.findSeparateGroupForEachKey(List.of(
            thirdPartyXU.getKey(),
            thirdPartyYU.getKey(),
            thirdPartyZV.getKey(),
            businessU.getKey(),
            businessV.getKey(),
            businessOrphan.getKey(),
            thirdPartyOrphan.getKey(),
            white1U.getKey(),
            whiteOrphan.getKey(),
            unknown.getKey()
        ));

        var uxGroup = MdmSskuGroup.createBusinessGroup(businessU,
            List.of(thirdPartyXU, thirdPartyYU));
        var uGroup = MdmSskuGroup.createBusinessGroup(businessU, List.of(thirdPartyXU, thirdPartyYU));
        var vGroup = MdmSskuGroup.createBusinessGroup(businessV, List.of(thirdPartyZV));
        Assertions.assertThat(groups)
            .containsEntry(thirdPartyXU.getKey(), uxGroup)
            .containsEntry(businessU.getKey(), uGroup)
            .containsEntry(businessV.getKey(), vGroup)
            .containsEntry(thirdPartyOrphan.getKey(), MdmSskuGroup.createNoBusinessGroup(thirdPartyOrphan));
        Assertions.assertThat(groups)
            .doesNotContainKey(white1U.getKey())
            .doesNotContainKey(whiteOrphan.getKey())
            .doesNotContainKey(unknown.getKey());
    }

    @Test
    public void when1pNotInEkatShouldReturnAsOrphans() {
        ServiceSsku baseOffer = generateMD(SUPPLIER_1P_BIZ, "meow");
        ServiceSsku beruOffer = generateMD(SUPPLIER_1P_FP, "meow");
        prepare1p(false, beruOffer.getKey()); // по приколу разметим сервис как из ЕОХ, хотя 1Р даже не подключены

        Assertions.assertThat(groupManager.findGroupsByKeys(List.of(baseOffer.getKey()))).isEmpty();
        Assertions.assertThat(groupManager.findGroupsByKeys(List.of(beruOffer.getKey()))).containsExactly(
            MdmSskuGroup.createNoBusinessGroup(beruOffer)
        );
    }

    @Test
    public void when1pInEkatButNotFromEoxShouldReturnAsOrphans() {
        ServiceSsku baseOffer = generateMD(SUPPLIER_1P_BIZ, "meow");
        ServiceSsku beruOffer = generateMD(SUPPLIER_1P_FP, "meow");
        prepare1p(true); // сервис не из ЕОХ, однако 1Р теперь подключены

        var expected = MdmSskuGroup.createNoBusinessGroup(beruOffer);

        Assertions.assertThat(groupManager.findGroupsByKeys(List.of(baseOffer.getKey()))).containsExactly(expected);
        Assertions.assertThat(groupManager.findGroupsByKeys(List.of(beruOffer.getKey()))).containsExactly(expected);
        Assertions.assertThat(groupManager.findGroupsByKeys(List.of(beruOffer.getKey(), baseOffer.getKey())))
            .containsExactly(expected);
    }

    @Test
    public void when1pInEkatAndEoxShouldReturnAsBusinessGroup() {
        ServiceSsku baseOffer = generateMD(SUPPLIER_1P_BIZ, "meow");
        ServiceSsku beruOffer = generateMD(SUPPLIER_1P_FP, "meow");
        prepare1p(true, beruOffer.getKey());

        var expected = MdmSskuGroup.createBusinessGroup(baseOffer, List.of(beruOffer));

        Assertions.assertThat(groupManager.findGroupsByKeys(List.of(baseOffer.getKey()))).containsExactly(expected);
        Assertions.assertThat(groupManager.findGroupsByKeys(List.of(beruOffer.getKey()))).containsExactly(expected);
        Assertions.assertThat(groupManager.findGroupsByKeys(List.of(beruOffer.getKey(), baseOffer.getKey())))
            .containsExactly(expected);
    }

    @Test
    public void canLoadDocumentsIntoSskus() {
        QualityDocument documentXU = generateDocument();
        QualityDocument documentYU = generateDocument();
        QualityDocument documentU = generateDocument();
        qualityDocumentRepository.insertOrUpdateAll(List.of(documentXU, documentYU, documentU));

        ServiceSsku serviceXU = generateMD(SUPPLIER_3P_XU, "u", documentXU);
        ServiceSsku serviceYU = generateMD(SUPPLIER_3P_YU, "u", documentYU);
        ServiceSsku businessU = generateMD(SUPPLIER_BIZ_U, "u", documentU);
        List<MdmSskuGroup> result = groupManager.findGroupsByKeys(List.of(serviceXU.getKey(), serviceYU.getKey(),
            businessU.getKey()),
            MdmBusinessStage.BUSINESS_DISABLED);

        Assertions.assertThat(result.size()).isEqualTo(1);
        Assertions.assertThat(result.get(0).getBusiness()).isEqualTo(businessU);
        Assertions.assertThat(result.get(0).getServiceSskus())
            .containsExactlyInAnyOrder(serviceXU, serviceYU);
        Assertions.assertThat(result.get(0).getBusiness()
            .getParamValue(KnownMdmParams.DOCUMENT_REG_NUMBER).get().getStrings())
            .containsExactlyInAnyOrder(documentU.getRegistrationNumber());
        Assertions.assertThat(result.get(0).getServiceSskus().stream()
            .map(ssku -> ssku.getParamValue(KnownMdmParams.DOCUMENT_REG_NUMBER).get())
            .flatMap(paramValue -> paramValue.getStrings().stream())
            .collect(Collectors.toList()))
            .containsExactlyInAnyOrder(documentXU.getRegistrationNumber(), documentYU.getRegistrationNumber());
    }

    @Test
    public void whenRetainNonEoxedShouldCollectCorrectGroup() {
        ServiceSsku thirdPartyXU = generateMD(SUPPLIER_3P_XU, "u");
        ServiceSsku thirdPartyYU = generateMD(SUPPLIER_3P_YU, "u");
        ServiceSsku thirdPartyZV = generateMD(SUPPLIER_3P_ZV, "v");
        ServiceSsku businessU = generateMD(SUPPLIER_BIZ_U, "u");
        ServiceSsku businessV = generateMD(SUPPLIER_BIZ_V, "v");
        sskuExistenceRepository.markExistence(new ShopSkuKey(SUPPLIER_3P_YU, "u"), false);

        List<MdmSskuGroup> groups = groupManager.findGroupsByKeys(List.of(
            businessU.getKey(),
            businessV.getKey()
        ), false);

        Assertions.assertThat(groups).containsExactlyInAnyOrder(
            MdmSskuGroup.createBusinessGroup(businessU, List.of(thirdPartyXU, thirdPartyYU)),
            MdmSskuGroup.createBusinessGroup(businessV, List.of(thirdPartyZV))
        );
    }

    @Test
    public void whenRetainBlueSkuKeysThenNoWhiteKeys() {
        ServiceSsku thirdPartyXU = generateMD(SUPPLIER_3P_XU, "u");
        ServiceSsku thirdPartyYU = generateMD(SUPPLIER_3P_YU, "u");
        ServiceSsku thirdPartyZV = generateMD(SUPPLIER_3P_ZV, "v");
        ServiceSsku businessU = generateMD(SUPPLIER_BIZ_U, "u");
        ServiceSsku businessV = generateMD(SUPPLIER_BIZ_V, "v");
        ServiceSsku businessOrphan = generateMD(SUPPLIER_BIZ_ORPHAN, "lonely business");
        ServiceSsku thirdPartyOrphan = generateMD(SUPPLIER_3P_ORPHAN, "lonely 3p");
        ServiceSsku white1U = generateMD(SUPPLIER_WHITE_1U, "u");
        ServiceSsku whiteOrphan = generateMD(SUPPLIER_WHITE_ORPHAN, "lonely white");

        List<ShopSkuKey> result = groupManager.retainOnlyMdmCompatibleSkuKeys(List.of(
            thirdPartyXU.getKey(),
            thirdPartyYU.getKey(),
            thirdPartyZV.getKey(),
            businessU.getKey(),
            businessV.getKey(),
            businessOrphan.getKey(),
            thirdPartyOrphan.getKey(),
            white1U.getKey(),
            whiteOrphan.getKey()));

        Assertions.assertThat(result).containsExactlyInAnyOrder(
            thirdPartyXU.getKey(),
            thirdPartyYU.getKey(),
            thirdPartyZV.getKey(),
            businessU.getKey(),
            businessV.getKey(),
            businessOrphan.getKey(),
            thirdPartyOrphan.getKey());
    }

    @Test
    public void processMigrationRequestToExistingGroup() {
        storageKeyValueService.putValue(MdmProperties.HANDLE_MIGRATION_IN_SSKU_GROUP_MANAGER, true);

        ServiceSsku thirdPartyXU = generateMD(SUPPLIER_3P_XU, "u");
        ServiceSsku thirdPartyYU = generateMD(SUPPLIER_3P_YU, "u");
        ServiceSsku thirdPartyZV = generateMD(SUPPLIER_3P_ZV, "u");
        ServiceSsku businessU = generateMD(SUPPLIER_BIZ_U, "u");
        ServiceSsku businessV = generateMD(SUPPLIER_BIZ_V, "u");

        sskuExistenceRepository.markExistence(new ShopSkuKey(SUPPLIER_3P_ZV, "v"), false);
        sskuExistenceRepository.markExistence(new ShopSkuKey(SUPPLIER_3P_ZV, "u"), true);

        // migration from U to V
        serviceOfferMigrationRepository.insert(new ServiceOfferMigrationInfo()
            .setSupplierId(SUPPLIER_3P_XU)
            .setShopSku("u")
            .setSrcBusinessId(SUPPLIER_BIZ_U)
            .setDstBusinessId(SUPPLIER_BIZ_V)
            .setAddedTimestamp(Instant.now())
            .setProcessed(true)
            .setProcessedTimestamp(Instant.now()));

        List<MdmSskuGroup> groups = groupManager.findGroupsByKeys(List.of(
            businessU.getKey(),
            businessV.getKey()
        ));

        Assertions.assertThat(groups).containsExactlyInAnyOrder(
            MdmSskuGroup.createBusinessGroup(businessU, List.of(thirdPartyYU)),
            MdmSskuGroup.createBusinessGroup(businessV, List.of(thirdPartyXU, thirdPartyZV))
        );

        // try to find the same by service keys
        List<MdmSskuGroup> groupsByServiceKeys = groupManager.findGroupsByKeys(List.of(
            thirdPartyXU.getKey(),
            thirdPartyYU.getKey(),
            thirdPartyZV.getKey()
        ));

        Assertions.assertThat(groupsByServiceKeys).containsExactlyInAnyOrder(
            MdmSskuGroup.createBusinessGroup(businessU, List.of(thirdPartyYU)),
            MdmSskuGroup.createBusinessGroup(businessV, List.of(thirdPartyXU, thirdPartyZV))
        );
    }

    @Test
    public void processMigrationRequestToOrphanAndMakeSourceBusinessOrphan() {
        storageKeyValueService.putValue(MdmProperties.HANDLE_MIGRATION_IN_SSKU_GROUP_MANAGER, true);

        ServiceSsku thirdPartyXU = generateMD(SUPPLIER_3P_XU, "u");
        ServiceSsku businessU = generateMD(SUPPLIER_BIZ_U, "u");
        ServiceSsku businessOrphan = generateMD(SUPPLIER_BIZ_ORPHAN, "u");

        sskuExistenceRepository.markExistence(new ShopSkuKey(SUPPLIER_3P_ZV, "v"), false);
        sskuExistenceRepository.markExistence(new ShopSkuKey(SUPPLIER_3P_ZV, "u"), true);

        // migration from U to orphan
        serviceOfferMigrationRepository.insert(new ServiceOfferMigrationInfo()
            .setSupplierId(SUPPLIER_3P_XU)
            .setShopSku("u")
            .setSrcBusinessId(SUPPLIER_BIZ_U)
            .setDstBusinessId(SUPPLIER_BIZ_ORPHAN)
            .setAddedTimestamp(Instant.now())
            .setProcessed(true)
            .setProcessedTimestamp(Instant.now()));

        List<MdmSskuGroup> groups = groupManager.findGroupsByKeys(List.of(
            businessU.getKey(),
            businessOrphan.getKey()
        ));

        Assertions.assertThat(groups).containsExactlyInAnyOrder(
            MdmSskuGroup.createBusinessGroup(businessU, List.of()),
            MdmSskuGroup.createBusinessGroup(businessOrphan, List.of(thirdPartyXU))
        );

        // try to find the same by service key
        List<MdmSskuGroup> groupsByServiceKeys = groupManager.findGroupsByKeys(List.of(
            thirdPartyXU.getKey()
        ));

        Assertions.assertThat(groupsByServiceKeys).containsExactlyInAnyOrder(
            MdmSskuGroup.createBusinessGroup(businessU, List.of()),
            MdmSskuGroup.createBusinessGroup(businessOrphan, List.of(thirdPartyXU))
        );
    }

    /**
     * Not contract, just observation.
     */
    @Test
    public void whenSearchByNotExistingServiceKeyReturnGroupWithoutGivenServiceKey() {
        ShopSkuKey businessKey = new ShopSkuKey(SUPPLIER_BIZ_U, "u");
        ShopSkuKey key3PX = new ShopSkuKey(SUPPLIER_3P_XU, "u");
        ShopSkuKey key3PY = new ShopSkuKey(SUPPLIER_3P_YU, "u");

        sskuExistenceRepository.markExistence(key3PX, false);
        Assertions.assertThat(groupManager.findKeyGroupsByKeys(List.of(key3PX)))
            .containsExactly(MdmSskuKeyGroup.createBusinessGroup(businessKey, List.of(key3PY)));
    }

    private void prepare1p(boolean ekat, ShopSkuKey... keysToEox) {
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_1P_FP).setType(MdmSupplierType.FIRST_PARTY)
            .setBusinessId(SUPPLIER_1P_BIZ).setBusinessEnabled(ekat));
        sskuExistenceRepository.markExistence(List.of(keysToEox), true);
        mdmSupplierCachingService.refresh();
    }

    private MdmSupplier supplier(int id, MdmSupplierType type, Integer business) {
        MdmSupplier s = new MdmSupplier();
        s.setId(id);
        s.setType(type);
        if (business != null) {
            s.setBusinessId(business);
        }
        s.setBusinessEnabled(true);
        return s;
    }

    private ServiceSsku generateMD(int supplierId, String shopSku, QualityDocument... documents) {
        MasterData md = TestDataUtils.generateMasterData(new ShopSkuKey(supplierId, shopSku), random, documents);

        if (documents != null && documents.length > 0) {
            List<DocumentOfferRelation> relations = DocumentOfferRelation.fromMasterData(md);
            qualityDocumentRepository.insertOrUpdateRelations(relations);
        }
        masterDataRepository.insert(md);

        return converter.toServiceSsku(masterDataRepository.findById(md.getShopSkuKey()), (ReferenceItemWrapper) null);
    }

    private QualityDocument generateDocument() {
        return TestDataUtils.generateDocument(random);
    }
}

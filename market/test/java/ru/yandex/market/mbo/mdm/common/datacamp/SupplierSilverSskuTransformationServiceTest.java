package ru.yandex.market.mbo.mdm.common.datacamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.ImpersonalSourceId;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverServiceSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.FromDatacampOfferConvertResult;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;

import static ru.yandex.market.mboc.common.utils.MdmProperties.DATACAMP_UNMARK_EXISTENCE_FOR_REMOVED;

public class SupplierSilverSskuTransformationServiceTest {

    private SupplierSilverSskuTransformationService service;
    private SilverSskuRepositoryMock silverSskuRepository;
    private SskuExistenceRepositoryMock sskuExistenceRepository;
    private StorageKeyValueServiceMock storageKeyValueService;

    @Before
    public void setup() {
        silverSskuRepository = new SilverSskuRepositoryMock();
        sskuExistenceRepository = new SskuExistenceRepositoryMock();
        storageKeyValueService = new StorageKeyValueServiceMock();
        service = new SupplierSilverSskuTransformationServiceImpl(silverSskuRepository, sskuExistenceRepository,
            storageKeyValueService);
    }

    @Test
    public void testDupesInEoxBatchShouldMergeIntoOneSilverWithRespectToVersions() {
        // given
        var sskus = List.of(
            ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(1001, "Икра Икара"), // version == 0
                flatSsku(1002, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 900L)),
                flatSsku(1003, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 800L)),
                flatSsku(1004, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 1L))
            ),
            ssku(200, "Уроки рока", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(2001, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 12L)),
                flatSsku(2002, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 13L)),
                flatSsku(2003, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 14L))
            ),
            ssku(300, "Мечта тракториста", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(3001, "Мечта тракториста", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 9000L)),
                flatSsku(3002, "Мечта тракториста"), // version == 0
                flatSsku(3003, "Мечта тракториста", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 8000L))
            ),
            ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(1001, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 100L)),
                flatSsku(1002, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 750L)),
                flatSsku(1003, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 950L))
            ),
            ssku(200, "Уроки рока", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(2001, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 10L)),
                flatSsku(2002, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 10L)),
                flatSsku(2003, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 10L))
            ).addBaseValue(param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 10L)),
            ssku(300, "Мечта тракториста", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(3001, "Мечта тракториста", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 9001L))
            )
        );
        List<FromDatacampOfferConvertResult> rawFormat = sskus.stream()
            .map(s -> new FromDatacampOfferConvertResult(s.toCommonSsku(), null))
            .collect(Collectors.toList());

        // when
        var result = service.convertToSilver(rawFormat);

        // then
        Assertions.assertThat(result).containsExactlyInAnyOrder(
            ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(1001, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 100L)),
                flatSsku(1002, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 900L)),
                flatSsku(1003, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 950L)),
                flatSsku(1004, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 1L))
            ),
            ssku(200, "Уроки рока", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(2001, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 12L)),
                flatSsku(2002, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 13L)),
                flatSsku(2003, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 14L))
            ).addBaseValue(param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 10L)),
            ssku(300, "Мечта тракториста", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(3001, "Мечта тракториста", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 9001L)),
                flatSsku(3002, "Мечта тракториста"), // version == 0
                flatSsku(3003, "Мечта тракториста", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 8000L))
            )
        );
    }

    @Test
    public void testFetchExistingSilverFetchesEoxDataOnly() {
        // given
        var sskus = List.of(
            ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(1001, "Икра Икара"),
                flatSsku(1002, "Икра Икара")
            ).addBaseValue(bmdmId(888881, 111)),
            ssku(200, "Уроки рока", MasterDataSourceType.SUPPLIER, "000064",
                flatSsku(2001, "Уроки рока"),
                flatSsku(2002, "Уроки рока")
            ).addBaseValue(bmdmId(888882, 222)),
            ssku(300, "Мечта тракториста", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.IRIS.name(), // not EOX
                flatSsku(3001, "Мечта тракториста"),
                flatSsku(3002, "Мечта тракториста")
            ),
            ssku(400, "Уран Бурана", MasterDataSourceType.WAREHOUSE, ImpersonalSourceId.DATACAMP.name(), // not EOX
                flatSsku(4001, "Уран Бурана"),
                flatSsku(4002, "Уран Бурана"),
                flatSsku(4003, "Уран Бурана")
            ),
            ssku(500, "Враг варяга", MasterDataSourceType.DBS, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(5001, "Враг варяга"),
                flatSsku(5003, "Враг варяга")
            ).addBaseValue(bmdmId(888885, 555)),
            ssku(600, "Ноги миноги", MasterDataSourceType.DBS, "ххх",
                flatSsku(6001, "Ноги миноги"),
                flatSsku(6003, "Ноги миноги")
            ).addBaseValue(bmdmId(888886, 666)),
            ssku(700, "Ротор роты", MasterDataSourceType.DBS, ImpersonalSourceId.IRIS.name(), // not EOX
                flatSsku(7001, "Ротор роты")
            )
        );
        silverSskuRepository.insertOrUpdateSskus(sskus);

        // when
        var fetchedSilver = service.fetchExistingSilver(sskus.stream()
            .map(SilverCommonSsku::getBusinessKey)
            .map(SilverSskuKey::getShopSkuKey)
            .collect(Collectors.toList()));

        // then
        Assertions.assertThat(fetchedSilver.getOriginalSilver()).containsExactlyInAnyOrder(
            ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(1001, "Икра Икара"),
                flatSsku(1002, "Икра Икара")
            ).addBaseValue(bmdmId(888881, 111)),
            ssku(200, "Уроки рока", MasterDataSourceType.SUPPLIER, "000064",
                flatSsku(2001, "Уроки рока"),
                flatSsku(2002, "Уроки рока")
            ).addBaseValue(bmdmId(888882, 222)),
            ssku(500, "Враг варяга", MasterDataSourceType.DBS, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(5001, "Враг варяга"),
                flatSsku(5003, "Враг варяга")
            ).addBaseValue(bmdmId(888885, 555)),
            ssku(600, "Ноги миноги", MasterDataSourceType.DBS, "ххх",
                flatSsku(6001, "Ноги миноги"),
                flatSsku(6003, "Ноги миноги")
            ).addBaseValue(bmdmId(888886, 666))
        );

        var allBmdmVersionFroms = fetchedSilver.getOriginalSilver()
            .stream()
            .map(ssku -> ssku.getBaseValue(KnownMdmParams.BMDM_ID).get().getUpdatedTs().toEpochMilli())
            .collect(Collectors.toList());

        Assertions.assertThat(allBmdmVersionFroms).containsExactlyInAnyOrder(111L, 222L, 555L, 666L);
    }

    @Test
    public void testFetchExistingMergesExplodedSourceIdsIntoOneSilver() {
        // given
        var sskus = List.of(
            ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, "xxx",
                flatSsku(1001, "Икра Икара"), // version == 0
                flatSsku(1002, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 900L)),
                flatSsku(1003, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 800L)),
                flatSsku(1004, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 1L))
            ),
            ssku(200, "Уроки рока", MasterDataSourceType.SUPPLIER, "00064",
                flatSsku(2001, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 12L)),
                flatSsku(2002, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 13L)),
                flatSsku(2003, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 14L))
            ),
            ssku(300, "Мечта тракториста", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(3001, "Мечта тракториста", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 9000L)),
                flatSsku(3002, "Мечта тракториста"), // version == 0
                flatSsku(3003, "Мечта тракториста", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 8000L))
            ),
            ssku(100, "Икра Икара", MasterDataSourceType.DBS, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(1001, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 100L)),
                flatSsku(1002, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 750L)),
                flatSsku(1003, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 950L))
            ),
            ssku(200, "Уроки рока", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(2001, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 10L)),
                flatSsku(2002, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 10L)),
                flatSsku(2003, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 10L))
            ).addBaseValue(param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 10L))
        );
        silverSskuRepository.insertOrUpdateSskus(sskus);

        // when
        var fetchedSilver = service.fetchExistingSilver(sskus.stream()
            .map(SilverCommonSsku::getBusinessKey)
            .map(SilverSskuKey::getShopSkuKey)
            .collect(Collectors.toList()));

        // then
        Assertions.assertThat(fetchedSilver.getUnifiedSilver()).containsExactlyInAnyOrder(
            ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(1001, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 100L)),
                flatSsku(1002, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 900L)),
                flatSsku(1003, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 950L)),
                flatSsku(1004, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 1L))
            ),
            ssku(200, "Уроки рока", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(2001, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 12L)),
                flatSsku(2002, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 13L)),
                flatSsku(2003, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 14L))
            ).addBaseValue(param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 10L)),
            ssku(300, "Мечта тракториста", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(3001, "Мечта тракториста", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 9000L)),
                flatSsku(3002, "Мечта тракториста"), // version == 0
                flatSsku(3003, "Мечта тракториста", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 8000L))
            )
        );
    }

    @Test
    public void testFetchExistingProvidesCorrectOriginalSetForFutureOperations() {
        // given
        var sskus = List.of(
            ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, "xxx",
                flatSsku(1001, "Икра Икара"), // version == 0
                flatSsku(1002, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 900L)),
                flatSsku(1003, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 800L)),
                flatSsku(1004, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 1L))
            ),
            ssku(200, "Уроки рока", MasterDataSourceType.SUPPLIER, "00064",
                flatSsku(2001, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 12L)),
                flatSsku(2002, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 13L)),
                flatSsku(2003, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 14L))
            ),
            ssku(300, "Мечта тракториста", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(3001, "Мечта тракториста", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 9000L)),
                flatSsku(3002, "Мечта тракториста"), // version == 0
                flatSsku(3003, "Мечта тракториста", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 8000L))
            ),
            ssku(100, "Икра Икара", MasterDataSourceType.DBS, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(1001, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 100L)),
                flatSsku(1002, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 750L)),
                flatSsku(1003, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 950L))
            ),
            ssku(200, "Уроки рока", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(2001, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 10L)),
                flatSsku(2002, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 10L)),
                flatSsku(2003, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 10L))
            ).addBaseValue(param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 10L))
        );
        silverSskuRepository.insertOrUpdateSskus(sskus);

        // when
        var fetchedSilver = service.fetchExistingSilver(sskus.stream()
            .map(SilverCommonSsku::getBusinessKey)
            .map(SilverSskuKey::getShopSkuKey)
            .collect(Collectors.toList()));

        // then
        Assertions.assertThat(fetchedSilver.getOriginalSilver()).containsExactlyInAnyOrderElementsOf(sskus);
    }

    @Test
    public void testMergeExistingAndNewWorksOnNewOffer() {
        // given
        var sskus = List.of(
            ssku(300, "Мечта тракториста", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(3001, "Мечта тракториста", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 9000L)),
                flatSsku(3002, "Мечта тракториста", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 8000L))
            )
        );

        // when
        var merged = service.mergeExistingAndNew(Map.of(), sskus);

        // then
        Assertions.assertThat(merged).containsExactlyInAnyOrderElementsOf(sskus);
    }

    @Test
    public void testMergeExistingAndNewDropsNakedBases() {
        // given
        var sskus = List.of(
            ssku(300, "Мечта тракториста", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name())
        );

        // when
        var merged = service.mergeExistingAndNew(Map.of(), sskus);

        // then
        Assertions.assertThat(merged).isEmpty();
    }

    @Test
    public void testMergeExistingAndNewWorksOnPartiallyUpdatedOffer() {
        // given
        var before1 = ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
            flatSsku(1001, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 100L)),
            flatSsku(1002, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 750L)),
            flatSsku(1003, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 950L))
        );
        var before2 = ssku(200, "Уроки рока", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
            flatSsku(2001, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 10L)),
            flatSsku(2002, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 10L)),
            flatSsku(2003, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 10L))
        ).addBaseValue(param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 10L));
        var after1 = ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
            flatSsku(1001, "Икра Икара"), // version == 0
            flatSsku(1002, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 900L)),
            flatSsku(1003, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 800L)),
            flatSsku(1004, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 1L))
        );
        var after2 = ssku(200, "Уроки рока", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
            flatSsku(2001, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 12L)),
            flatSsku(2002, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 13L)),
            flatSsku(2003, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 14L))
        );

        // when
        var merged = service.mergeExistingAndNew(Map.of(
            before1.getBusinessKey(), before1,
            before2.getBusinessKey(), before2
        ), List.of(after1, after2));

        // then
        Assertions.assertThat(merged).containsExactlyInAnyOrder(
            ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(1001, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 100L)),
                flatSsku(1002, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 900L)),
                flatSsku(1003, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 950L)),
                flatSsku(1004, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 1L))
            ),
            ssku(200, "Уроки рока", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(2001, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 12L)),
                flatSsku(2002, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 13L)),
                flatSsku(2003, "Уроки рока", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 14L))
            ).addBaseValue(param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 10L))
        );
    }

    @Test
    public void testMergeExistingAndNewKeepsBmdmId() {
        // given
        var before1 = ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
            flatSsku(1001, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 100L)),
            flatSsku(1002, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 750L)),
            flatSsku(1003, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 950L))
        ).addBaseValue(bmdmId(905065828, 111));

        var after1 = ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
            flatSsku(1001, "Икра Икара"), // version == 0
            flatSsku(1002, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 900L)),
            flatSsku(1003, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 800L)),
            flatSsku(1004, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 1L))
        ).addBaseValue(param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 10L));

        // when
        var merged = service.mergeExistingAndNew(Map.of(
            before1.getBusinessKey(), before1
        ), List.of(after1));

        // then
        Assertions.assertThat(merged).containsExactlyInAnyOrder(
            ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(1001, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 100L)),
                flatSsku(1002, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 900L)),
                flatSsku(1003, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 950L)),
                flatSsku(1004, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 1L))
            ).addBaseValue(bmdmId(905065828, 111)).addBaseValue(param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 10L))
        );

        Assertions.assertThat(merged.get(0).getBaseValue(KnownMdmParams.BMDM_ID).get().getUpdatedTs().toEpochMilli())
            .isEqualTo(111);
    }

    @Test
    public void testAdjustmentsChangeTypeToDbsWhenAllServicesAreDbs() {
        // given
        var ssku = ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
            dsbsSskuWithSupplierType(1001, "Икра Икара"),
            dsbsSskuWithSupplierType(1002, "Икра Икара")
        ).addBaseValue(bmdmId(43536543, 111));

        // when
        var adjusted = service.adjustTypeTransportAndVersion(List.of(ssku));

        // then
        Assertions.assertThat(adjusted).containsExactlyInAnyOrder(
            ssku(100, "Икра Икара", MasterDataSourceType.DBS, ImpersonalSourceId.DATACAMP.name(),
                dsbsSskuWithDbsType(1001, "Икра Икара"),
                dsbsSskuWithDbsType(1002, "Икра Икара")
            ).addBaseValue(bmdmId(43536543, 111))
        );
        Assertions.assertThat(adjusted.get(0).getBaseValue(KnownMdmParams.BMDM_ID).get().getUpdatedTs().toEpochMilli())
            .isEqualTo(111);
    }

    @Test
    public void testAdjustmentsChangeTypeToSupplierWhenAtLeastOneServiceIsBlue() {
        // given
        var ssku = ssku(100, "Икра Икара", MasterDataSourceType.DBS, ImpersonalSourceId.DATACAMP.name(),
            flatSsku(1001, "Икра Икара"), // supplier
            dsbsSskuWithDbsType(1002, "Икра Икара")
        ).addBaseValue(bmdmId(43536543, 111));

        // when
        var adjusted = service.adjustTypeTransportAndVersion(List.of(ssku));

        // then
        Assertions.assertThat(adjusted).containsExactlyInAnyOrder(
            ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(1001, "Икра Икара"), // supplier
                dsbsSskuWithSupplierType(1002, "Икра Икара")
            ).addBaseValue(bmdmId(43536543, 111))
        );
        Assertions.assertThat(adjusted.get(0).getBaseValue(KnownMdmParams.BMDM_ID).get().getUpdatedTs().toEpochMilli())
            .isEqualTo(111);
    }

    @Test
    public void testAdjustmentsCopyBaseVersionToUnversionedServices() {
        // given
        var ssku = ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
            flatSsku(1001, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 9L)),
            flatSsku(1002, "Икра Икара") // no version
        ).addBaseValue(param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 10L));

        // when
        var adjusted = service.adjustTypeTransportAndVersion(List.of(ssku));

        // then
        Assertions.assertThat(adjusted).containsExactlyInAnyOrder(
            ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(1001, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 9L)), // remains
                flatSsku(1002, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 10L)) // copied
            ).addBaseValue(param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 10L))
        );
    }

    @Test
    public void testAdjustmentsSetDatacampTransportOnEverything() {
        // given
        var ssku = ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
            flatSsku(1001, "Икра Икара"),
            flatSsku(1002, "Икра Икара")
        );

        // when
        var adjusted = service.adjustTypeTransportAndVersion(List.of(ssku));

        // then
        var transports = adjusted.stream()
            .map(SilverCommonSsku::getBaseValues)
            .flatMap(Collection::stream)
            .map(SskuSilverParamValue::getSskuSilverTransport)
            .collect(Collectors.toSet());
        Assertions.assertThat(transports).containsExactly(SskuSilverParamValue.SskuSilverTransportType.DATACAMP);
    }

    @Test
    public void testJunkRemovalFailsOnNonEoxInput() {
        // given
        var sskus = List.of(
            ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.IRIS.name()),
            ssku(200, "Уроки рока", MasterDataSourceType.MEASUREMENT, ImpersonalSourceId.DATACAMP.name())
        );
        var updatedSskus = List.of(
            ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name()),
            ssku(200, "Уроки рока", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name())
        );

        // when & then
        for (SilverCommonSsku badSsku : sskus) {
            Assertions.assertThatThrownBy(() -> service.cleanJunkSilver(List.of(badSsku), updatedSskus))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Illegal remove request of non-datacamp silver");
        }
    }

    @Test
    public void testJunkRemovalIgnoresNiceDatacampSourceId() {
        // given
        var sskus = List.of(
            ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name()),
            ssku(200, "Уроки рока", MasterDataSourceType.DBS, ImpersonalSourceId.DATACAMP.name())
        );
        silverSskuRepository.insertOrUpdateSskus(sskus);
        var anyOtherSsku = ssku(300, "Мечта тракториста", MasterDataSourceType.DBS, ImpersonalSourceId.DATACAMP.name());

        // when
        service.cleanJunkSilver(sskus, List.of(anyOtherSsku));

        // then
        Assertions.assertThat(silverSskuRepository.allSskus()).containsExactlyInAnyOrderElementsOf(sskus);
    }

    @Test
    public void testJunkRemovalAbortedOnEmptyReferenceSskus() {
        // given
        var sskus = List.of(
            ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, "-"),
            ssku(200, "Уроки рока", MasterDataSourceType.DBS, "-")
        );
        silverSskuRepository.insertOrUpdateSskus(sskus);

        // when
        service.cleanJunkSilver(sskus, List.of());

        // then
        Assertions.assertThat(silverSskuRepository.allSskus()).containsExactlyInAnyOrderElementsOf(sskus);
    }

    @Test
    public void testJunkRemovalDoesNotRemovesActualUpdates() {
        // given
        var original = List.of(
            ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, "000064"),
            ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name()),
            ssku(200, "Уроки рока", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name()),
            ssku(200, "Уроки рока", MasterDataSourceType.DBS, ImpersonalSourceId.DATACAMP.name())
        );
        var updated = List.of(
            ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name()),
            ssku(200, "Уроки рока", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name())
        );
        silverSskuRepository.insertOrUpdateSskus(original);

        // when
        service.cleanJunkSilver(original, updated);

        // then
        Assertions.assertThat(silverSskuRepository.allSskus()).containsExactlyInAnyOrderElementsOf(updated);
    }

    @Test
    public void testJunkRemovalDoesNotRemoveActualUpdatesByMdmIds() {
        // given
        var original = List.of(
            ssku(
                100, "Икра Икара", MasterDataSourceType.SUPPLIER, "bad source id"
            ).addBaseValue(bmdmId(1, 11)),
            ssku(
                200, "Уроки рока", MasterDataSourceType.DBS, ImpersonalSourceId.DATACAMP.name()
            ).addBaseValue(bmdmId(2, 22)),
            ssku(
                300, "Мечта тракториста", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name()
            ).addBaseValue(bmdmId(3, 33))
        );
        var updated = List.of(
            // Этот обновился in-place по корректному mdm_id, а потому не триггернёт вычистку мусора
            ssku(
                100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name()
            ).addBaseValue(bmdmId(1, 11)),
            // У этого mdm_id получился свежий, а потому он сохранится сызнова как новая запись. А старая удалится.
            ssku(
                200, "Уроки рока", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name()
            ).addBaseValue(bmdmId(201, 22)),
            // А третий вообще пусть без mdm_id (в реальности не должны такие быть, но мало ли). В таком случае удалим
            // мусорную старую версию.
            ssku(300, "Мечта тракториста", MasterDataSourceType.DBS, ImpersonalSourceId.DATACAMP.name())
        );
        silverSskuRepository.insertOrUpdateSskus(original);

        // when
        service.cleanJunkSilver(original, updated);

        // then
        Assertions.assertThat(silverSskuRepository.allSskus()).containsExactlyInAnyOrder(
            ssku(
                100, "Икра Икара", MasterDataSourceType.SUPPLIER, "bad source id"
            ).addBaseValue(bmdmId(1, 11))
        );
    }

    @Test
    public void testFetchExistingAndJunkRemovalTogether() {
        // given
        var original = List.of(
            ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, "000064"),
            ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name()),
            ssku(200, "Уроки рока", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name()),
            ssku(200, "Уроки рока", MasterDataSourceType.DBS, ImpersonalSourceId.DATACAMP.name())
        );
        silverSskuRepository.insertOrUpdateSskus(original);

        // when
        ExistingSilverContainer existing = service.fetchExistingSilver(List.of(
            new ShopSkuKey(100, "Икра Икара"),
            new ShopSkuKey(200, "Уроки рока")
        ));
        service.cleanJunkSilver(existing.getOriginalSilver(), existing.getUnifiedSilver());

        // then
        Assertions.assertThat(silverSskuRepository.allSskus())
            .containsExactlyInAnyOrderElementsOf(existing.getUnifiedSilver());
    }

    @Test
    public void testSaveStoresSilverAndMarksServiceExistence() {
        // given
        var sskus = List.of(
            ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(1001, "Икра Икара"),
                flatSsku(1002, "Икра Икара"),
                flatSsku(1003, "Икра Икара"),
                flatSsku(1004, "Икра Икара")
            ),
            ssku(200, "Уроки рока", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(2001, "Уроки рока"),
                flatSsku(2002, "Уроки рока"),
                flatSsku(2003, "Уроки рока")
            )
        );

        // when
        service.save(sskus);

        // then
        Set<ShopSkuKey> keys = sskus.stream()
            .map(SilverCommonSsku::getServiceSskus)
            .flatMap(v -> v.values().stream())
            .map(SilverServiceSsku::getShopSkuKey)
            .collect(Collectors.toSet());

        Assertions.assertThat(silverSskuRepository.allSskus()).containsExactlyInAnyOrderElementsOf(sskus);
        Assertions.assertThat(sskuExistenceRepository.retainExisting(keys)).containsExactlyInAnyOrderElementsOf(keys);
    }

    @Test
    public void testUnmarkExistence() {
        // given
        storageKeyValueService.putValue(DATACAMP_UNMARK_EXISTENCE_FOR_REMOVED, true);

        var updateWithRemovalKey = new ShopSkuKey(1001, "Икра Икара");
        var updateWithoutRemovalKey = new ShopSkuKey(2001, "Уроки рока");
        var original = List.of(
            ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(updateWithRemovalKey.getSupplierId(), updateWithRemovalKey.getShopSku(),
                    param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 900L))
            ),
            ssku(200, "Уроки рока", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(updateWithoutRemovalKey.getSupplierId(), updateWithoutRemovalKey.getShopSku(),
                    param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 12L))
            )
        );
        service.save(original);

        var updated = List.of(
            ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(updateWithRemovalKey.getSupplierId(), updateWithRemovalKey.getShopSku(),
                    param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 900L),
                    param(KnownMdmParams.IS_REMOVED, true))
            ),
            // not removed
            ssku(200, "Уроки рока", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(updateWithoutRemovalKey.getSupplierId(), updateWithoutRemovalKey.getShopSku(),
                    param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 13L))
            )
        );

        // when
        service.save(updated);

        // then
        var allKeys = Set.of(updateWithoutRemovalKey, updateWithRemovalKey);
        Assertions.assertThat(sskuExistenceRepository.retainExisting(allKeys)).containsExactly(updateWithoutRemovalKey);
    }

    @Test
    public void testExistingSskusCollectionsDoesNotContainSameInstances() {
        // given
        var before = ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
            flatSsku(1001, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 100L))
        ).addBaseValue(bmdmId(905065, 111)).addBaseValue(param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 100L));

        silverSskuRepository.insertOrUpdateSskus(List.of(before));

        // when
        ExistingSilverContainer existing = service.fetchExistingSilver(List.of(before.getKey()));

        // then
        // Пока они равны
        Assertions.assertThat(existing.getOriginalSilver()).containsExactly(before);
        Assertions.assertThat(existing.getUnifiedSilver()).containsExactly(before);
        // Но инстансы должны быть разные по ==
        Assertions.assertThat(existing.getOriginalSilver().get(0)).isNotSameAs(existing.getUnifiedSilver().get(0));
    }

    @Test
    public void testUpdatesOnSeparateExistingSskusCollectionsDoesNotAffectEachOther() {
        // given 1: генерим сску и сохраняем в БД
        var before = ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
            flatSsku(1001, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 100L))
        ).addBaseValue(bmdmId(905065, 111)).addBaseValue(param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 100L));

        silverSskuRepository.insertOrUpdateSskus(List.of(before));

        // given 2: формируем три сскушки - одна "свежая", вторая existing-original, а третья - unified-original.
        var updates = ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
            flatSsku(1001, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 101L))
        ).addBaseValue(param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 101L));

        ExistingSilverContainer existing = service.fetchExistingSilver(List.of(before.getKey()));

        SilverCommonSsku beforeOriginal = existing.getOriginalSilver().get(0);
        SilverCommonSsku beforeUnified = existing.getUnifiedSilver().get(0);
        SilverCommonSsku after = updates;

        // when: делаем несколько инвазивных преобразований над beforeUnified + after
        var merged = service.mergeExistingAndNew(Map.of(beforeUnified.getBusinessKey(), before), List.of(after));
        var adjusted = service.adjustTypeTransportAndVersion(merged);

        // then: проверим, что всё это не зааффектило before-original и в целом что сработало.
        Assertions.assertThat(adjusted).containsExactlyInAnyOrder(
            ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(1001, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 101L))
            ).addBaseValue(bmdmId(905065, 111)).addBaseValue(param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 101L))
        );
        Assertions.assertThat(beforeOriginal).isEqualTo(
            ssku(100, "Икра Икара", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(),
                flatSsku(1001, "Икра Икара", param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 100L))
            ).addBaseValue(bmdmId(905065, 111)).addBaseValue(param(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, 100L))
        ); // -------------- ^ ------------- особенно важно не потерять здесь BMDM_ID
        // потому отдельно ещё проверим UpdatedTs этого атрибута, т.к. от него зависит работа стораджей
        // (а стандартный компаратор не проверяет таймштампы)

        Assertions.assertThat(adjusted.get(0).getBaseValue(KnownMdmParams.BMDM_ID).get().getUpdatedTs().toEpochMilli())
            .isEqualTo(111);
        Assertions.assertThat(beforeOriginal.getBaseValue(KnownMdmParams.BMDM_ID).get().getUpdatedTs().toEpochMilli())
            .isEqualTo(111);
    }

    private SilverServiceSsku flatSsku(int supplierId,
                                       String shopSku,
                                       MasterDataSourceType type,
                                       String sourceId,
                                       SskuSilverParamValue... values
    ) {
        SilverServiceSsku ssku = new SilverServiceSsku(new SilverSskuKey(supplierId, shopSku, type, sourceId));
        SskuSilverParamValue someData = new SskuSilverParamValue();
        someData.setMdmParamId(Long.hashCode(supplierId));
        someData.setNumeric(BigDecimal.valueOf(supplierId * 2L));
        ssku.addParamValue(someData);

        someData = new SskuSilverParamValue();
        someData.setMdmParamId(shopSku.hashCode());
        someData.setString(shopSku);
        ssku.addParamValue(someData);
        ssku.addParamValues(List.of(values));
        return ssku;
    }

    private SilverServiceSsku flatSsku(int supplierId, String shopSku, SskuSilverParamValue... values) {
        return flatSsku(supplierId, shopSku, MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), values);
    }

    private SilverServiceSsku dsbsSskuWithSupplierType(int supplierId, String shopSku, SskuSilverParamValue... values) {
        SskuSilverParamValue flag = new SskuSilverParamValue();
        flag.setMdmParamId(KnownMdmParams.IS_DBS);
        flag.setBool(true);
        return (SilverServiceSsku) flatSsku(supplierId, shopSku,
            MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), values).addParamValue(flag);
    }

    private SilverServiceSsku dsbsSskuWithDbsType(int supplierId, String shopSku, SskuSilverParamValue... values) {
        SskuSilverParamValue flag = new SskuSilverParamValue();
        flag.setMdmParamId(KnownMdmParams.IS_DBS);
        flag.setBool(true);
        return (SilverServiceSsku) flatSsku(supplierId, shopSku,
            MasterDataSourceType.DBS, ImpersonalSourceId.DATACAMP.name(), values).addParamValue(flag);
    }

    private SilverCommonSsku ssku(int businessId,
                                  String shopSku,
                                  MasterDataSourceType type,
                                  String sourceId,
                                  SilverServiceSsku... services) {
        SilverCommonSsku ssku = new SilverCommonSsku(new SilverSskuKey(businessId, shopSku, type, sourceId));
        ssku.putServiceSskus(List.of(services));
        SskuSilverParamValue someData = new SskuSilverParamValue();
        someData.setMdmParamId(Long.hashCode(businessId));
        someData.setNumeric(BigDecimal.valueOf(businessId * 2L));
        ssku.addBaseValue(someData);

        someData = new SskuSilverParamValue();
        someData.setMdmParamId(shopSku.hashCode());
        someData.setString(shopSku);
        ssku.addBaseValue(someData);
        return ssku;
    }

    private SskuSilverParamValue param(long paramId, long val) {
        SskuSilverParamValue value = new SskuSilverParamValue();
        value.setNumeric(BigDecimal.valueOf(val));
        value.setMdmParamId(paramId);
        return value;
    }

    private SskuSilverParamValue bmdmId(long id, long versionFrom) {
        SskuSilverParamValue value = new SskuSilverParamValue();
        value.setNumeric(BigDecimal.valueOf(id));
        value.setUpdatedTs(Instant.ofEpochMilli(versionFrom));
        value.setMdmParamId(KnownMdmParams.BMDM_ID);
        return value;
    }

    private SskuSilverParamValue param(long paramId, boolean val) {
        SskuSilverParamValue value = new SskuSilverParamValue();
        value.setBool(val);
        value.setMdmParamId(paramId);
        return value;
    }
}

package ru.yandex.market.mboc.common.services.monitorings;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.storage.StorageKeyValueRepository;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mbo.storage.StorageKeyValueServiceImpl;
import ru.yandex.market.mboc.app.proto.MasterDataServiceMock;
import ru.yandex.market.mboc.app.proto.SupplierDocumentServiceMock;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.Offer.MappingDestination;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.monitorings.offers.OfferMonitoringStrategy.Resolution;
import ru.yandex.market.mboc.common.services.monitorings.offers.OffersHasCountriesMonitoringStrategy;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("checkstyle:MagicNumber")
public class OffersHasCountriesStrategyTest extends BaseDbTestClass {

    private static final long SEED = 1020304L;

    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private StorageKeyValueRepository keyValueRepository;

    private OffersHasCountriesMonitoringStrategy strategy;

    private MasterDataServiceMock masterDataServiceMock;
    private SupplierDocumentServiceMock supplierDocumentServiceMock;
    private MasterDataHelperService masterDataHelperService;

    private EnhancedRandom defaultRandom;

    @Before
    public void setup() {
        defaultRandom = TestDataUtils.defaultRandom(SEED);
        masterDataServiceMock = new MasterDataServiceMock();
        supplierDocumentServiceMock = new SupplierDocumentServiceMock(masterDataServiceMock);
        masterDataHelperService = new MasterDataHelperService(masterDataServiceMock, supplierDocumentServiceMock,
                new SupplierConverterServiceMock(), storageKeyValueService);

        keyValueRepository.deleteAll();
        StorageKeyValueService keyValueService = new StorageKeyValueServiceImpl(keyValueRepository, null);
        strategy = new OffersHasCountriesMonitoringStrategy(
            null,
            masterDataHelperService,
            keyValueService,
            offerRepository
        );
        supplierRepository.insert(OfferTestUtils.simpleSupplier());
        supplierRepository.insert(OfferTestUtils.fmcgSupplier());
    }

    @Test
    public void whenNoOffersThenOk() {
        Resolution resolution = strategy.monitorOffers(Collections.emptyList());
        assertEquals(MonitoringStatus.OK, resolution.getOverallStatus());
    }

    @Test
    public void whenOkMarkExistsThenTakeNewerOffers() throws InterruptedException {
        // Создадим два оффера без стран производителей и в недалёком прошлом, но до отсечки.
        generateAndInsertOffer();
        generateAndInsertOffer();

        // Сгенерируем успешную отсечку мониторинга
        strategy.onMonitoringFinish(MonitoringStatus.OK);

        // Мониторинг проигнорит эти оффера, т.к. они созданы до последнего ОКа.
        assertThat(strategy.findConformingOffers()).isEmpty();
        assertThat(strategy.monitorOffers().getOverallStatus()).isEqualTo(MonitoringStatus.OK);

        // Теперь создадим оффер ПОСЛЕ отсечки. Он найдётся, и будет унижен и оскорблён мониторингом.
        Thread.sleep(100L);
        generateAndInsertOffer();
        Resolution resolution = strategy.monitorOffers();
        assertEquals(MonitoringStatus.CRITICAL, resolution.getOverallStatus());
    }

    @Test
    public void whenNewOffersNoMasterDataThenFail() {
        generateAndInsertOffer();
        Resolution resolution = strategy.monitorOffers();
        assertEquals(MonitoringStatus.CRITICAL, resolution.getOverallStatus());
    }

    @Test
    public void whenNewOffersNoCountriesThenFail() throws InterruptedException {
        strategy.onMonitoringFinish(MonitoringStatus.OK);
        Thread.sleep(100L);
        generateAndInsertMasterData(generateAndInsertOffer(), "Шир");
        generateAndInsertMasterData(generateAndInsertOffer());
        generateAndInsertMasterData(generateAndInsertOffer());
        Resolution resolution = strategy.monitorOffers();
        assertEquals(MonitoringStatus.CRITICAL, resolution.getOverallStatus());
    }

    @Test
    public void whenNewOffersCountriesExistThenOk() throws InterruptedException {
        strategy.onMonitoringFinish(MonitoringStatus.OK);
        Thread.sleep(100L);
        generateAndInsertMasterData(generateAndInsertOffer(), "Шир", "Гондор", "Рохан", "Мёрквуд/Лихолесье");
        generateAndInsertMasterData(generateAndInsertOffer(), "Мордор");
        generateAndInsertMasterData(generateAndInsertOffer(), "Лотлориэн", "Ривендейл/Дольн");
        Resolution resolution = strategy.monitorOffers();
        assertEquals(MonitoringStatus.OK, resolution.getOverallStatus());
    }

    @Test
    public void whenBadWhiteOffersThenOkAnyway() throws InterruptedException {
        strategy.onMonitoringFinish(MonitoringStatus.OK);
        Thread.sleep(100L);
        generateAndInsertMasterData(generateAndInsertWhiteOffer());
        generateAndInsertMasterData(generateAndInsertWhiteOffer());
        Resolution resolution = strategy.monitorOffers();
        assertEquals(MonitoringStatus.OK, resolution.getOverallStatus());
    }

    @Test
    public void whenOffersFromFmcgDoNotTakeThem() {
        Offer fmcgOffer = generateAndInsertFmcgOffer();
        Offer goodOldNormalOffer = generateAndInsertOffer();
        generateAndInsertMasterData(goodOldNormalOffer, "Denmark");

        // Проверяем, что fmcg оффер не нашелся
        List<Offer> foundOffers = strategy.findConformingOffers();
        assertThat(foundOffers).containsExactly(goodOldNormalOffer);

        Resolution resolution = strategy.monitorOffers(foundOffers);
        assertEquals(MonitoringStatus.OK, resolution.getOverallStatus());

        assertThat(strategy.monitorOffers()).isEqualTo(resolution);
    }

    private Offer generateAndInsertOffer() {
        return generateAndInsertOffer(DateTimeUtils.dateTimeNow());
    }

    private Offer generateAndInsertOffer(LocalDateTime createdTime) {
        Offer offer = OfferTestUtils.nextOffer()
            .setCreated(createdTime)
            .setUpdated(createdTime)
            .setMappingDestination(MappingDestination.BLUE);
        boolean inserted = offerRepository.insertOffer(offer);
        assertTrue(inserted);
        return offer;
    }

    private Offer generateAndInsertWhiteOffer() {
        Offer offer = OfferTestUtils.nextOffer()
            .setCreated(DateTimeUtils.dateTimeNow())
            .setUpdated(DateTimeUtils.dateTimeNow())
            .setMappingDestination(MappingDestination.WHITE);
        boolean inserted = offerRepository.insertOffer(offer);
        assertTrue(inserted);
        return offer;
    }

    private Offer generateAndInsertFmcgOffer() {
        Offer offer = OfferTestUtils.nextOffer()
            .setCreated(DateTimeUtils.dateTimeNow())
            .setUpdated(DateTimeUtils.dateTimeNow())
            .setMappingDestination(MappingDestination.FMCG)
            .setBusinessId(OfferTestUtils.FMCG_SUPPLIER_ID);
        boolean inserted = offerRepository.insertOffer(offer);
        assertTrue(inserted);
        return offer;
    }

    private MasterData generateAndInsertMasterData(Offer offer, String... countries) {
        MasterData md = TestDataUtils.generateMasterData(offer.getShopSku(), offer.getBusinessId(), defaultRandom);
        md.getManufacturerCountries().clear();
        md.addAllManufacturerCountries(Arrays.asList(countries));
        masterDataHelperService.saveSskuMasterDataAndDocuments(Collections.singletonList(md));
        return md;
    }
}

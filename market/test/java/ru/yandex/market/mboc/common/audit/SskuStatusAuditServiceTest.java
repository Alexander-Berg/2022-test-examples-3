package ru.yandex.market.mboc.common.audit;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.http.MboAuditService;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.offers.repository.MboAuditServiceMock;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static ru.yandex.market.mboc.common.audit.SskuStatusAuditServiceImpl.INTERVAL;
import static ru.yandex.market.mboc.common.dict.MbocSupplierType.BUSINESS;
import static ru.yandex.market.mboc.common.dict.MbocSupplierType.THIRD_PARTY;

public class SskuStatusAuditServiceTest extends BaseDbTestClass {
    @Resource
    private SupplierRepository supplierRepository;
    @Resource
    private OfferRepository offerRepository;

    private final MboAuditService mboAuditService = new MboAuditServiceMock();
    private SskuStatusAuditService auditService;

    private Supplier bizSupplier1000;
    private Supplier supplier1;
    private Supplier supplier2;
    private Supplier supplier42;

    @Before
    public void setUp() {
        // for base offer
        bizSupplier1000 = new Supplier().setId(1000).setName("biz").setType(BUSINESS);
        supplier1 = new Supplier().setId(1).setName("test").setType(THIRD_PARTY)
            .setBusinessId(1000);
        supplier2 = new Supplier().setId(2).setName("test2").setType(THIRD_PARTY)
            .setBusinessId(1000);
        supplier42 = new Supplier().setId(42).setName("test").setType(THIRD_PARTY);
        supplierRepository.insertBatch(bizSupplier1000, supplier1, supplier2, supplier42);
        auditService = new SskuStatusAuditServiceImpl(
            mboAuditService,
            offerRepository,
            supplierRepository
        );
    }

    @Test
    public void readWriteStatusAuditInfoTest() {
        var shopSkuKey = new ShopSkuKey(1, "shopsku1");
        offerRepository.insertOffer(createTestOffer(111, shopSkuKey.getShopSku(), supplier1));

        var auditInfo = auditInfo(shopSkuKey, "myuser", instantNow().minus(5, ChronoUnit.SECONDS))
            .setOldDataMap(statusMap("ACTIVE", "commentOld"))
            .setNewDataMap(statusMap("INACTIVE", "commentNew"));
        auditService.writeSskuStatusAuditInfo(List.of(auditInfo));

        var result = auditService.getSskuStatusAuditInfo(shopSkuKey, null);

        Assertions.assertThat(result.getAuditInfoList()).containsExactly(auditInfo.toReadInfo());
        Assertions.assertThat(result.getLastTs()).isEqualTo(auditInfo.getModifiedTs());
    }

    @Test
    public void readWriteStatusAuditInfoMultipleEventsTest() {
        var shopSkuKey1 = new ShopSkuKey(1, "shopsku");
        var shopSkuKey2 = new ShopSkuKey(2, "shopsku");
        var shopSkuKey42 = new ShopSkuKey(42, "shopsku42");
        var offer1 = createTestOffer(111, shopSkuKey1.getShopSku(), supplier1, supplier2);
        var offer42 = createTestOffer(142, shopSkuKey42.getShopSku(), supplier42);
        offerRepository.insertOffers(offer1, offer42);

        var auditInfo1 = auditInfo(shopSkuKey1, "myuser", instantNow().minus(5, ChronoUnit.SECONDS))
            .setOldDataMap(statusMap("ACTIVE", "commentOld"))
            .setNewDataMap(statusMap("INACTIVE", "commentNew"));
        var auditInfo2 = auditInfo(shopSkuKey2, "myuser2", instantNow().minus(4, ChronoUnit.SECONDS))
            .setOldDataMap(statusMap("ACTIVE", "commentOld2"))
            .setNewDataMap(statusMap("INACTIVE", "commentNew2"));
        var auditInfo3 = auditInfo(shopSkuKey2, "myuser3", instantNow().minus(3, ChronoUnit.SECONDS))
            .setOldDataMap(statusMap(null, null))
            .setNewDataMap(statusMap("INACTIVE", "commentNew3"));
        var auditInfo4 = auditInfo(shopSkuKey42, "myuser3", instantNow().minus(2, ChronoUnit.SECONDS))
            .setOldDataMap(statusMap(null, null))
            .setNewDataMap(statusMap("INACTIVE", null));
        auditService.writeSskuStatusAuditInfo(List.of(auditInfo1, auditInfo2, auditInfo3, auditInfo4));

        var result1 = auditService.getSskuStatusAuditInfo(shopSkuKey1, null);
        Assertions.assertThat(result1.getAuditInfoList()).containsExactly(
            auditInfo1.toReadInfo()
        );
        Assertions.assertThat(result1.getLastTs()).isEqualTo(auditInfo1.getModifiedTs());

        var result2 = auditService.getSskuStatusAuditInfo(shopSkuKey2, null);
        Assertions.assertThat(result2.getAuditInfoList()).containsExactly(
            auditInfo3.toReadInfo(),
            auditInfo2.toReadInfo()
        );
        Assertions.assertThat(result2.getLastTs()).isEqualTo(auditInfo2.getModifiedTs());

        var result3 = auditService.getSskuStatusAuditInfo(shopSkuKey42, null);
        Assertions.assertThat(result3.getAuditInfoList()).containsExactly(
            auditInfo4.toReadInfo()
        );
        Assertions.assertThat(result3.getLastTs()).isEqualTo(auditInfo4.getModifiedTs());
    }

    @Test
    public void readEmptyAuditInfoTest() {
        var shopSkuKey = new ShopSkuKey(1, "shopsku1");
        offerRepository.insertOffer(createTestOffer(111, shopSkuKey.getShopSku(), supplier1));

        var result = auditService.getSskuStatusAuditInfo(shopSkuKey, null);

        Assertions.assertThat(result.getAuditInfoList()).isEmpty();

        // от интервала отнимаем немного времени, чтобы не допускать флапов в тестах
        Assertions.assertThat(result.getLastTs()).isBefore(Instant.now().minus(INTERVAL.minusMinutes(1)));
    }

    @Test
    public void saveOldEmpty() {
        var shopSkuKey = new ShopSkuKey(1, "shopsku1");
        offerRepository.insertOffer(createTestOffer(111, shopSkuKey.getShopSku(), supplier1));

        var auditInfo = auditInfo(shopSkuKey, "myuser", instantNow().minus(5, ChronoUnit.SECONDS))
            .setNewDataMap(statusMap("ACTIVE", "commentOld"));
        auditService.writeSskuStatusAuditInfo(List.of(auditInfo));

        var result = auditService.getSskuStatusAuditInfo(shopSkuKey, null);

        Assertions.assertThat(result.getAuditInfoList()).containsExactly(auditInfo.toReadInfo());
        Assertions.assertThat(result.getLastTs()).isEqualTo(auditInfo.getModifiedTs());
    }

    @Test
    public void saveNewEmpty() {
        var shopSkuKey = new ShopSkuKey(1, "shopsku1");
        offerRepository.insertOffer(createTestOffer(111, shopSkuKey.getShopSku(), supplier1));

        var auditInfo = auditInfo(shopSkuKey, "myuser", instantNow().minus(5, ChronoUnit.SECONDS))
            .setOldDataMap(statusMap("ACTIVE", "commentOld"));
        auditService.writeSskuStatusAuditInfo(List.of(auditInfo));

        var result = auditService.getSskuStatusAuditInfo(shopSkuKey, null);

        Assertions.assertThat(result.getAuditInfoList()).containsExactly(auditInfo.toReadInfo());
        Assertions.assertThat(result.getLastTs()).isEqualTo(auditInfo.getModifiedTs());
    }

    @Test
    public void saveFinishTime() {
        var offer = createTestOffer(111, "shopsku1", supplier1);
        offerRepository.insertOffer(offer);

        var key = offer.getShopSkuKey(supplier1.getId());

        var statusFinishAt = Instant.now();
        var statusMap = statusMap("ACTIVE", null);
        statusMap.put("statusFinishAt", statusFinishAt.toString());
        var auditInfo = auditInfo(key, "myuser", instantNow().minus(5, ChronoUnit.SECONDS))
            .setNewDataMap(statusMap);
        auditService.writeSskuStatusAuditInfo(List.of(auditInfo));

        var result = auditService.getSskuStatusAuditInfo(key, null);

        Assertions.assertThat(result.getAuditInfoList())
            .extracting(OfferStatusAuditInfoRead::getNewFinishStatusTime)
            .containsExactly(statusFinishAt);
        Assertions.assertThat(result.getAuditInfoList()).containsExactly(auditInfo.toReadInfo());
        Assertions.assertThat(result.getLastTs()).isEqualTo(auditInfo.getModifiedTs());
    }

    @Test
    public void severalReadsAuditInfoTest() {
        var shopSkuKey = new ShopSkuKey(1, "shopsku1");
        offerRepository.insertOffer(createTestOffer(111, shopSkuKey.getShopSku(), supplier1));

        var auditInfo = auditInfo(shopSkuKey, "myuser", instantNow().minus(5, ChronoUnit.SECONDS))
            .setOldDataMap(statusMap("ACTIVE", "commentOld"))
            .setNewDataMap(statusMap("INACTIVE", "commentNew"));
        var auditInfo2 = auditInfo(shopSkuKey, "myuser2", instantNow().minus(4, ChronoUnit.SECONDS))
            .setModifiedTs(instantNow().minus(Duration.ofDays(35)))
            .setOldDataMap(statusMap("ACTIVE", "commentOld2"))
            .setNewDataMap(statusMap("INACTIVE", "commentNew2"));
        var auditInfo3 = auditInfo(shopSkuKey, "myuser3", instantNow().minus(3, ChronoUnit.SECONDS))
            .setOldDataMap(statusMap(null, null))
            .setNewDataMap(statusMap("INACTIVE", null))
            .setModifiedTs(instantNow().minus(Duration.ofDays(36)));
        auditService.writeSskuStatusAuditInfo(List.of(auditInfo, auditInfo2, auditInfo3));

        var result = auditService.getSskuStatusAuditInfo(shopSkuKey, null);
        Assertions.assertThat(result.getAuditInfoList()).containsExactly(auditInfo.toReadInfo());
        Assertions.assertThat(result.getLastTs()).isEqualTo(auditInfo.getModifiedTs());

        result = auditService.getSskuStatusAuditInfo(shopSkuKey, result.getLastTs());
        Assertions.assertThat(result.getAuditInfoList()).isEmpty();
        Assertions.assertThat(result.getLastTs()).isBefore(auditInfo.getModifiedTs());

        result = auditService.getSskuStatusAuditInfo(shopSkuKey, result.getLastTs());
        Assertions.assertThat(result.getAuditInfoList())
            .containsExactly(auditInfo2.toReadInfo(), auditInfo3.toReadInfo());
        Assertions.assertThat(result.getLastTs()).isEqualTo(auditInfo3.getModifiedTs());

        result = auditService.getSskuStatusAuditInfo(shopSkuKey, result.getLastTs());
        Assertions.assertThat(result.getAuditInfoList()).isEmpty();
    }

    @Test
    public void onlyOneEventInAudit() {
        var shopSkuKey = new ShopSkuKey(1, "shopsku1");
        offerRepository.insertOffer(createTestOffer(111, shopSkuKey.getShopSku(), supplier1));

        var auditInfo = auditInfo(shopSkuKey, "myuser", instantNow().minus(5, ChronoUnit.SECONDS))
            .setOldDataMap(statusMap("ACTIVE", "commentOld"))
            .setNewDataMap(statusMap("INACTIVE", "commentNew"));
        auditService.writeSskuStatusAuditInfo(List.of(auditInfo));

        var result1 = auditService.getSskuStatusAuditInfo(shopSkuKey, null);
        Assertions.assertThat(result1.getAuditInfoList()).containsExactly(auditInfo.toReadInfo());
        Assertions.assertThat(result1.getLastTs()).isEqualTo(auditInfo.getModifiedTs());

        var result2 = auditService.getSskuStatusAuditInfo(shopSkuKey, result1.getLastTs());
        Assertions.assertThat(result2.getAuditInfoList()).isEmpty();
        Assertions.assertThat(result2.getLastTs()).isBefore(result1.getLastTs());
    }

    @Test
    public void auditShouldBeWrittenForOfferWithoutMapping() {
        var shopSkuKey = new ShopSkuKey(1, "shopsku1");
        var offer = createTestOffer(111, shopSkuKey.getShopSku(), supplier1);
        offer.setApprovedSkuMappingInternal(null);
        offerRepository.insertOffer(offer);

        // check no mapping
        Assertions
            .assertThat(offerRepository.findOffersByBusinessSkuKeys(
                new BusinessSkuKey(offer.getBusinessId(), offer.getShopSku())))
            .hasSize(1)
            .usingElementComparatorOnFields("businessId", "shopSku", "approvedSkuMapping")
            .containsExactly(offer);

        var auditInfo = auditInfo(shopSkuKey, "myuser", instantNow().minus(5, ChronoUnit.SECONDS))
            .setOldDataMap(statusMap("ACTIVE", "commentOld"))
            .setNewDataMap(statusMap("INACTIVE", "commentNew"));
        auditService.writeSskuStatusAuditInfo(List.of(auditInfo));

        var result = auditService.getSskuStatusAuditInfo(shopSkuKey, null);
        Assertions.assertThat(result.getAuditInfoList()).containsExactly(auditInfo.toReadInfo());
        Assertions.assertThat(result.getLastTs()).isEqualTo(auditInfo.getModifiedTs());

        // add mapping
        offer = offerRepository.findOfferByBusinessSkuKey(offer.getBusinessSkuKey());
        offer.setApprovedSkuMappingInternal(new Offer.Mapping(1, LocalDateTime.now(), Offer.SkuType.MARKET));
        offerRepository.updateOffer(offer);

        // check has mapping
        Assertions
            .assertThat(offerRepository.findOffersByBusinessSkuKeys(
                new BusinessSkuKey(offer.getBusinessId(), offer.getShopSku())))
            .hasSize(1)
            .usingElementComparatorOnFields("businessId", "shopSku", "approvedSkuMapping")
            .containsExactly(offer);

        var auditInfo2 = auditInfo(shopSkuKey, "myuser", instantNow().minus(3, ChronoUnit.SECONDS))
            .setOldDataMap(auditInfo.getNewDataMap())
            .setNewDataMap(statusMap("DELISTED", "todelisted"));
        auditService.writeSskuStatusAuditInfo(List.of(auditInfo2));

        var result2 = auditService.getSskuStatusAuditInfo(shopSkuKey, null);
        Assertions.assertThat(result2.getAuditInfoList()).containsExactly(
            auditInfo2.toReadInfo(),
            auditInfo.toReadInfo()
        );
        Assertions.assertThat(result2.getLastTs()).isEqualTo(auditInfo.getModifiedTs());
    }

    private SskuStatusAuditInfoWrite auditInfo(ShopSkuKey shopSkuKey, String user, Instant ts) {
        return new SskuStatusAuditInfoWrite()
            .setModifiedTs(ts)
            .setShopSkuKey(shopSkuKey)
            .setAuthor(user);
    }

    private static Map<String, String> statusMap(String status, String comment) {
        var map = new HashMap<String, String>();
        if (status != null) {
            map.put("availability", status);
        }
        if (comment != null) {
            map.put("comment", comment);
        }
        return map;
    }

    private Offer createTestOffer(int id, String shopSku, Supplier... serviceSuppliers) {
        var suppliers = Arrays.stream(serviceSuppliers)
            .map(v -> new Supplier()
                .setId(v.getId())
                .setName(v.getName())
                .setType(MbocSupplierType.valueOf(v.getType().name()))
                .setBusinessId(v.getBusinessId())
            )
            .collect(Collectors.toList());

        var sets = suppliers.stream()
            .map(Supplier::getEffectiveBusinessId)
            .collect(Collectors.toSet());
        if (sets.size() > 1) {
            throw new IllegalArgumentException("Service suppliers should have equal biz_id: " + sets);
        }

        return OfferTestUtils.nextOffer()
            .setId(id)
            .setShopSku(shopSku)
            .setBusinessId(sets.iterator().next())
            .setTitle("Offer_" + id)
            .setCategoryIdForTests(1L, Offer.BindingKind.SUGGESTED)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT)
            .setApprovedSkuMappingInternal(new Offer.Mapping(1, LocalDateTime.now(), Offer.SkuType.MARKET))
            .addNewServiceOfferIfNotExistsForTests(suppliers);
    }

    private static Instant instantNow() {
        // При конвертации в long теряется точность и падают тесты
        // Совершаю эту конвертацию принудительно
        return Instant.ofEpochMilli(Instant.now().toEpochMilli());
    }
}

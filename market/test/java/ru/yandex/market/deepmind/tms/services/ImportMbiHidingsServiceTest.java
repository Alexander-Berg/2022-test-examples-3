package ru.yandex.market.deepmind.tms.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.HidingReasonType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Hiding;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.HidingReasonDescription;
import ru.yandex.market.deepmind.common.hiding.HidingReason;
import ru.yandex.market.deepmind.common.hiding.HidingRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.yql_query_service.service.QueryService;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.ABO_LEGAL_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.ABO_OTHER_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.ABO_REASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.FEED_35J_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.FEED_45L_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.FEED_52L_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.FEED_REASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.MDM_MBOC_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.MDM_REASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.PARTNER_API_REASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.PARTNER_API_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.SKK_45J_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.SKK_45K_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.SKK_45Y_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.SKK_REASON;

public class ImportMbiHidingsServiceTest extends BaseHidingsServiceTest {

    @Resource
    private HidingRepository slaveHidingRepository;

    private ImportMbiHidingsService importService;

    private ServiceOfferReplica offer1;
    private ServiceOfferReplica offer2;
    private ServiceOfferReplica offer3;
    private ServiceOfferReplica offer4;
    private ServiceOfferReplica businessOffer1;
    private ServiceOfferReplica businessOffer2;
    private HidingReasonDescription aboLegalDescr;
    private HidingReasonDescription skk45KDescr;
    private HidingReasonDescription skk45JDescr;
    private HidingReasonDescription skk45YDescr;
    private HidingReasonDescription partnerApiDescr;
    private HidingReasonDescription feed52LDescr;
    private HidingReasonDescription feed35jDescr;
    private HidingReasonDescription feed45LDescr;
    private HidingReasonDescription mdmMbocDescr;
    private QueryService queryService;

    @Before
    public void setUp() {
        super.setUp();

        offer1 = offer(1, "sku-1", 100L);
        offer2 = offer(2, "sku-2", 1L);
        offer3 = offer(3, "sku-3");
        offer4 = offer(77, "sku-4");
        businessOffer1 = offer(201, "sku-5").setSupplierType(SupplierType.THIRD_PARTY);
        businessOffer2 = offer(202, "sku-5").setSupplierType(SupplierType.FIRST_PARTY);
        serviceOfferReplicaRepository.save(offer1, offer2, offer3, offer4, businessOffer1, businessOffer2);

        var ignoreReasons = HidingReason.getReasonsStrNotOf(HidingReason.SyncService.MBI).stream()
            .map(r -> "'" + r + "'")
            .collect(Collectors.joining(", "));
        var ignoreSubreasons = HidingReason.getSubReasonsStrNotOf(HidingReason.SyncService.MBI).stream()
            .map(r -> "'" + r + "'")
            .collect(Collectors.joining(", "));
        queryService = Mockito.mock(QueryService.class);
        Mockito.when(queryService.getQuery(Mockito.anyString(), Mockito.anyMap()))
            .thenReturn("" +
                " select o.raw_supplier_id, o.raw_shop_sku, max(h.hidden_at) as hidden_at, h.reason, h.subreason " +
                " from " + mbiHidingsTable + " as h " +
                " join " + offersTable + " as o on o.supplier_id = h.supplier_id and o.shop_sku = h.shop_sku " +
                " where reason not in (" + ignoreReasons + ") " +
                "   and subreason not in (" + ignoreSubreasons + ") " +
                " group by o.raw_supplier_id, o.raw_shop_sku, h.reason, h.subreason "
            );
        importService = new ImportMbiHidingsService(
            namedYqlJdbcTemplate,
            queryService,
            hidingRepository,
            slaveHidingRepository,
            hidingReasonDescriptionRepository,
            TransactionHelper.MOCK,
            TransactionHelper.MOCK,
            mbiHidingsTable.toString(),
            offersTable.toString(),
            "//tmp/supplier/table",
            mapReducePool
        );
        var hidingsDescriptionMap = insertHidingsReasonDescriptionsWithRes(
            createReasonDescription(ABO_LEGAL_SUBREASON.toReasonKey()),
            createReasonDescription(SKK_45K_SUBREASON.toReasonKey()),
            createReasonDescription(SKK_45J_SUBREASON.toReasonKey()),
            createReasonDescription(SKK_45Y_SUBREASON.toReasonKey()),
            createReasonDescription(PARTNER_API_SUBREASON.toReasonKey()),
            createReasonDescription(HidingReason.FEED_52L_SUBREASON.toReasonKey()),
            createReasonDescription(HidingReason.FEED_35J_SUBREASON.toReasonKey()),
            createReasonDescription(HidingReason.FEED_45L_SUBREASON.toReasonKey()),
            createReasonDescription(MDM_MBOC_SUBREASON.toReasonKey())
        );
        aboLegalDescr = hidingsDescriptionMap.get(ABO_LEGAL_SUBREASON.toReasonKey());
        skk45KDescr = hidingsDescriptionMap.get(SKK_45K_SUBREASON.toReasonKey());
        skk45JDescr = hidingsDescriptionMap.get(SKK_45J_SUBREASON.toReasonKey());
        skk45YDescr = hidingsDescriptionMap.get(SKK_45Y_SUBREASON.toReasonKey());
        partnerApiDescr = hidingsDescriptionMap.get(PARTNER_API_SUBREASON.toReasonKey());
        feed52LDescr = hidingsDescriptionMap.get(HidingReason.FEED_52L_SUBREASON.toReasonKey());
        feed35jDescr = hidingsDescriptionMap.get(HidingReason.FEED_35J_SUBREASON.toReasonKey());
        feed45LDescr = hidingsDescriptionMap.get(HidingReason.FEED_45L_SUBREASON.toReasonKey());
        mdmMbocDescr = hidingsDescriptionMap.get(MDM_MBOC_SUBREASON.toReasonKey());
    }

    @Test
    public void testImport() {
        addMbiHiding(MDM_REASON, MDM_MBOC_SUBREASON, offer1, "2019-06-14 19:51:59");
        addMbiHiding(FEED_REASON, FEED_35J_SUBREASON, offer2, "2020-02-17");
        addMbiHiding(FEED_REASON, FEED_52L_SUBREASON, offer3, "2020-01-01");

        importService.syncUnpublishedOffersWithMbi();

        assertThat(getAllHidings())
            .usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(
                createHiding(mdmMbocDescr.getId(), MDM_MBOC_SUBREASON, offer1, LocalDateTime.parse(
                    "2019-06-14T19:51:59")),
                createHiding(feed35jDescr.getId(), FEED_35J_SUBREASON, offer2, LocalDateTime.parse(
                    "2020-02-17T00:00:00")),
                createHiding(feed52LDescr.getId(), FEED_52L_SUBREASON, offer3, LocalDateTime.parse(
                    "2020-01-01T00:00:00"))
            );
    }

    @Test
    public void testImportOnOneOffer() {
        addMbiHiding(MDM_REASON, MDM_MBOC_SUBREASON, offer1, "2020-01-01");
        addMbiHiding(SKK_REASON, SKK_45Y_SUBREASON, offer1, "2020-01-01");
        addMbiHiding(FEED_REASON, FEED_52L_SUBREASON, offer2, "2020-01-01");

        importService.syncUnpublishedOffersWithMbi();

        assertThat(getAllHidings())
            .usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(
                createHiding(mdmMbocDescr.getId(), MDM_MBOC_SUBREASON, offer1, LocalDateTime.parse(
                    "2020-01-01T00:00:00")),
                createHiding(skk45YDescr.getId(), SKK_45Y_SUBREASON, offer1, LocalDateTime.parse(
                    "2020-01-01T00:00:00")),
                createHiding(feed52LDescr.getId(), FEED_52L_SUBREASON, offer2, LocalDateTime.parse(
                    "2020-01-01T00:00:00"))
            );
    }

    // https://st.yandex-team.ru/MBI-201634#5ea9871fe35bfe0647f0a988
    @Test
    public void testDuplicatesOnYt() {
        addMbiHiding(FEED_REASON, FEED_35J_SUBREASON, offer1, "2020-03-13 07:53:45.0");
        addMbiHiding(FEED_REASON, FEED_35J_SUBREASON, offer1, "2020-03-13 04:52:58.0");
        addMbiHiding(FEED_REASON, FEED_35J_SUBREASON, offer1, "2020-03-13 11:20:22.0");
        addMbiHiding(FEED_REASON, FEED_35J_SUBREASON, offer1, "2020-03-13 07:24:18.0");

        importService.syncUnpublishedOffersWithMbi();

        assertThat(getAllHidings())
            .usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(
                createHiding(feed35jDescr.getId(), FEED_35J_SUBREASON, offer1,
                    LocalDateTime.parse("2020-03-13T11:20:22"))
            );
    }

    @Test
    public void testImportShouldNotAffectOtherHidings() {
        var boomDescr = hidingReasonDescriptionRepository.save(new HidingReasonDescription()
            .setReasonKey("ABO_BOOM")
            .setType(HidingReasonType.REASON_KEY)
            .setExtendedDesc("Boom!!!!!")
        );

        addMbiHiding(MDM_REASON, MDM_MBOC_SUBREASON, offer1, "2020-01-01");
        addMbiHiding(SKK_REASON, SKK_45K_SUBREASON, offer2, "2020-01-01");
        addMbiHiding(ABO_REASON, ABO_OTHER_SUBREASON, offer2, "2020-01-01");
        addMbiHiding(ABO_REASON.toString(), "BOOM", offer2, "2020-01-01");
        for (HidingReason ignoreReason : HidingReason.getReasonsNotOf(HidingReason.SyncService.MBI)) {
            addMbiHiding(ignoreReason, FEED_52L_SUBREASON, offer2, null);
        }
        for (HidingReason ignoreSubReason : HidingReason.getSubReasonsNotOf(HidingReason.SyncService.MBI)) {
            addMbiHiding(ignoreSubReason.getReason(), ignoreSubReason, offer2, null);
        }

        // в базе будут записи:
        Hiding saved1 = createHiding(skk45KDescr.getId(), "стоп", offer1, USER_1, null, "skk45k");
        Hiding saved2 = createHiding(aboLegalDescr.getId(), "legal", offer1, USER_1, null, "legal");
        Hiding saved3 = createHiding(boomDescr.getId(), "boom", offer2, USER_1, null, "boom");
        hidingRepository.save(saved1, saved2, saved3);

        importService.syncUnpublishedOffersWithMbi();

        Assertions.assertThat(getAllHidings())
            .usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(
                saved1,
                saved2,
                saved3,
                createHiding(mdmMbocDescr.getId(), MDM_MBOC_SUBREASON, offer1,
                    LocalDateTime.parse("2020-01-01T00:00:00"))
            );
    }

    @Test
    public void testIgnoreOtherHidings() {
        addMbiHiding(MDM_REASON, MDM_MBOC_SUBREASON, offer1, "2020-01-01");
        addMbiHiding(SKK_REASON, SKK_45K_SUBREASON, offer2, "2020-01-01");

        importService.syncUnpublishedOffersWithMbi();

        Assertions.assertThat(getAllHidings())
            .usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(
                createHiding(mdmMbocDescr.getId(), MDM_MBOC_SUBREASON, offer1, LocalDateTime.parse(
                    "2020-01-01T00:00:00"))
            );
    }

    @Test
    public void testDeleteUpdateAndInsertNewImports() {
        // в базе будут записи:
        Hiding saved1 = createHiding(mdmMbocDescr.getId(), MDM_MBOC_SUBREASON, offer1,
            LocalDateTime.parse("2020-01-01T00:00:00"));
        Hiding saved2 = createHiding(feed45LDescr.getId(), FEED_45L_SUBREASON, offer1,
            LocalDateTime.parse("2020-01-01T00:00:00"));
        Hiding saved3 = createHiding(partnerApiDescr.getId(), PARTNER_API_SUBREASON,
            offer1, LocalDateTime.parse("2020-01-01T00:00:00"));
        insertHidings(saved1, saved2, saved3);

        // в yt будут записи:
        // эта запись соответствует saved1 -> в БД запись не должна измениться, т.к. дата не изменилась
        addMbiHiding(MDM_REASON, MDM_MBOC_SUBREASON, offer1, "2020-01-01");
        // эта запись соответствует saved2, обновилась дата -> в БД нет изменений, так как на дату не смотрим
        addMbiHiding(FEED_REASON, FEED_45L_SUBREASON, offer1, "2021-02-02");
        // новая запись
        addMbiHiding(MDM_REASON, MDM_MBOC_SUBREASON, offer2, "2020-01-01");
        // saved3 будет удалена

        importService.syncUnpublishedOffersWithMbi();

        Assertions.assertThat(getAllHidings())
            .usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(
                saved1,
                saved2,
                createHiding(mdmMbocDescr.getId(), MDM_MBOC_SUBREASON, offer2,
                    LocalDateTime.parse("2020-01-01T00:00:00"))
            );
    }

    @Test
    public void testImportShouldNotCorruptId() {
        Hiding saved1 = createHiding(feed45LDescr.getId(), FEED_45L_SUBREASON,
            offer1, "2020-01-01T00:00:00Z").setUserId(10L);
        Hiding saved2 = createHiding(feed52LDescr.getId(), FEED_52L_SUBREASON,
            offer1, "2020-01-01T00:00:00Z");
        insertHidings(saved1, saved2);

        Map<Long, Long> hidingIdByShopSku = getAllHidings()
            .stream()
            .collect(Collectors.toMap(Hiding::getReasonKeyId, Hiding::getId));
        saved1.setId(hidingIdByShopSku.get(saved1.getReasonKeyId()));
        saved2.setId(hidingIdByShopSku.get(saved2.getReasonKeyId()));

        addMbiHiding(FEED_REASON, FEED_45L_SUBREASON, offer1, "2021-02-02");
        addMbiHiding(FEED_REASON, FEED_52L_SUBREASON, offer1, "2021-02-02");

        importService.syncUnpublishedOffersWithMbi();

        Assertions.assertThat(getAllHidings())
            .containsExactlyInAnyOrder(
                saved1
                    // hidden_at поменяется, только когда другие поля поменяются
                    // поэтому для saved1 новое время, а в saved2 старое время, поэтому запись не поменялась
                    .setHiddenAt(LocalDate.parse("2021-02-02").atStartOfDay(ZoneId.systemDefault()).toInstant())
                    .setUserId(null),
                saved2
            );
    }

    @Test
    public void testSubreasonAlwaysShouldExist() {
        addMbiHiding(offer2.getBusinessId(), offer2.getShopSku(),
            "2020-02-17", PARTNER_API_REASON.toString(), "");

        importService.syncUnpublishedOffersWithMbi();
        assertThat(getAllHidings())
            .usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(
                createHiding(partnerApiDescr.getId(), PARTNER_API_SUBREASON, offer2,
                    LocalDateTime.parse("2020-02-17T00:00:00"))
            );
    }

    @Test
    public void testImportHidingOn1POffer() {
        addMbiHiding(BERU_ID, "000042." + offer4.getShopSku(),
            "2020-02-17", PARTNER_API_REASON.toString(), "");

        importService.syncUnpublishedOffersWithMbi();
        assertThat(getAllHidings())
            .usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(
                createHiding(partnerApiDescr.getId(), PARTNER_API_SUBREASON, offer4,
                    LocalDateTime.parse("2020-02-17T00:00:00"))
            );
    }

    @Test
    public void testImportHidingOn1POfferAsIsWillBeSkipped() {
        addMbiHiding(offer4.getSupplierId(), offer4.getShopSku(),
            "2020-02-17", PARTNER_API_REASON.toString(), "");

        importService.syncUnpublishedOffersWithMbi();
        assertThat(getAllHidings()).isEmpty();
    }

    @Test
    public void testImportBusinessOffer() {
        addMbiHiding(MDM_REASON, MDM_MBOC_SUBREASON, offer1, "2019-06-14 19:51:59");
        addMbiHiding(FEED_REASON, FEED_35J_SUBREASON, offer2, "2020-02-17");
        addMbiHiding(FEED_REASON, FEED_52L_SUBREASON, businessOffer1, "2020-01-01");
        addMbiHiding(FEED_REASON, FEED_52L_SUBREASON, businessOffer2, "2020-01-01");

        importService.syncUnpublishedOffersWithMbi();

        assertThat(getAllHidings())
            .usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(
                createHiding(mdmMbocDescr.getId(), MDM_MBOC_SUBREASON, offer1, LocalDateTime.parse(
                    "2019-06-14T19:51:59")),
                createHiding(feed35jDescr.getId(), FEED_35J_SUBREASON, offer2, LocalDateTime.parse(
                    "2020-02-17T00:00:00")),
                createHiding(feed52LDescr.getId(), FEED_52L_SUBREASON, businessOffer1,
                    LocalDateTime.parse("2020-01-01T00:00:00")),
                createHiding(feed52LDescr.getId(), FEED_52L_SUBREASON, businessOffer2,
                    LocalDateTime.parse("2020-01-01T00:00:00"))
            );
    }

    @Test
    public void testRemoveAll() {
        // в базе будут записи:
        Hiding saved1 = createHiding(mdmMbocDescr.getId(), MDM_MBOC_SUBREASON, offer1,
            "2020-01-01T00:00:00Z");
        Hiding saved2 = createHiding(feed45LDescr.getId(), FEED_45L_SUBREASON, offer1,
            "2020-01-01T00:00:00Z");
        Hiding saved3 = createHiding(partnerApiDescr.getId(), PARTNER_API_SUBREASON, offer1,
            "2020-01-01T00:00:00Z");
        insertHidings(saved1, saved2, saved3);

        importService.syncUnpublishedOffersWithMbi();

        Assertions.assertThat(getAllHidings()).isEmpty();
    }

    @Test
    public void testHidingIterator() {
        var h1 = createHiding(feed35jDescr.getId(), FEED_35J_SUBREASON, offer1, LocalDateTime.parse(
            "2020-02-17T00:00:00"));
        var h2 = createHiding(mdmMbocDescr.getId(), MDM_MBOC_SUBREASON, offer1, LocalDateTime.parse(
            "2019-06-14T19:51:59"));
        var h3 = createHiding(feed52LDescr.getId(), FEED_52L_SUBREASON, offer2, LocalDateTime.parse(
            "2020-01-01T00:00:00"));
        List<Hiding> hidings = List.of(h1, h2, h3);
        var hidingIterator = new ImportMbiHidingsService.HidingIterator(hidings.iterator());
        assertThat(hidingIterator.next())
            .isEqualTo(h1);
        assertThat(hidingIterator.next())
            .isEqualTo(h2);
        assertThat(hidingIterator.next())
            .isEqualTo(h3);
        assertThat(hidingIterator.hasNext())
            .isEqualTo(false);
    }

    @Test
    public void testHidingIteratorSorting() {
        var h1 = createHiding(feed35jDescr.getId(), FEED_35J_SUBREASON, offer1, LocalDateTime.parse(
            "2020-02-17T00:00:00"));
        var h2 = createHiding(mdmMbocDescr.getId(), MDM_MBOC_SUBREASON, offer1, LocalDateTime.parse(
            "2019-06-14T19:51:59"));
        var h3 = createHiding(feed52LDescr.getId(), FEED_52L_SUBREASON, offer2, LocalDateTime.parse(
            "2020-01-01T00:00:00"));
        List<Hiding> hidings = List.of(h2, h1, h3);
        var hidingIterator = new ImportMbiHidingsService.HidingIterator(hidings.iterator());
        assertThat(hidingIterator.next())
            .isEqualTo(h1);
        assertThat(hidingIterator.next())
            .isEqualTo(h2);
        assertThat(hidingIterator.next())
            .isEqualTo(h3);
        assertThat(hidingIterator.hasNext())
            .isEqualTo(false);
    }
}

package ru.yandex.market.deepmind.tms.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.db.jooq.generated.msku.Tables;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.HiddenSupplier;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Hiding;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.HidingReasonDescription;
import ru.yandex.market.deepmind.common.hiding.HidingReason;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;

import static ru.yandex.market.deepmind.common.hiding.HidingReason.ABO_LEGAL_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.PARTNER_API_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.SKK_45K_SUBREASON;

/**
 * @author kravchenko-aa
 * @date 16.07.2020
 */
public class ImportHiddenSuppliersServiceTest extends BaseHidingsServiceTest {

    private ImportHiddenSuppliersService importHiddenSuppliersService;
    private HidingReasonDescription aboLegalDescr;
    private HidingReasonDescription skk45KDescr;
    private HidingReasonDescription partnerApiDescr;
    private HidingReasonDescription feed52LDescr;
    private HidingReasonDescription supplierDescr;

    @Before
    public void setUp() {
        super.setUp();

        importHiddenSuppliersService = new ImportHiddenSuppliersService(
            TransactionHelper.MOCK,
            namedParameterJdbcTemplate,
            hidingReasonDescriptionRepository,
                deepmindStorageKeyValueService,
            namedYqlJdbcTemplate,
            mapReducePool,
            hiddenSuppliersTable.toString()
        );

        var hidingsDescriptionMap = insertHidingsReasonDescriptionsWithRes(
            createReasonDescription(ABO_LEGAL_SUBREASON.toReasonKey()),
            createReasonDescription(SKK_45K_SUBREASON.toReasonKey()),
            createReasonDescription(PARTNER_API_SUBREASON.toReasonKey()),
            createReasonDescription(HidingReason.FEED_52L_SUBREASON.toReasonKey())
        );
        aboLegalDescr = hidingsDescriptionMap.get(ABO_LEGAL_SUBREASON.toReasonKey());
        skk45KDescr = hidingsDescriptionMap.get(SKK_45K_SUBREASON.toReasonKey());
        partnerApiDescr = hidingsDescriptionMap.get(PARTNER_API_SUBREASON.toReasonKey());
        feed52LDescr = hidingsDescriptionMap.get(HidingReason.FEED_52L_SUBREASON.toReasonKey());
        supplierDescr = hidingReasonDescriptionRepository.findByReasonKeys(
            HidingReason.HIDDEN_SUPPLIER_SUBREASON.toReasonKey()).get(0);
    }

    @Test
    public void importShouldUpdateCreateDeleteSupplierHidings() {
        dslContext.newRecord(Tables.HIDDEN_SUPPLIER,
            new HiddenSupplier().setSupplierId(1).setHiddenAt(LocalDate.parse("2020-07-10"))).insert();
        dslContext.newRecord(Tables.HIDDEN_SUPPLIER,
            new HiddenSupplier().setSupplierId(2).setHiddenAt(LocalDate.parse("2020-07-10"))).insert();

        Assertions.assertThat(dslContext.selectFrom(Tables.HIDDEN_SUPPLIER).fetchInto(HiddenSupplier.class))
            .usingElementComparatorOnFields("supplierId", "hiddenAt")
            .containsExactlyInAnyOrder(
                new HiddenSupplier().setSupplierId(1).setHiddenAt(LocalDate.parse("2020-07-10")),
                new HiddenSupplier().setSupplierId(2).setHiddenAt(LocalDate.parse("2020-07-10"))
            );

        addHiddenSupplier(1, LocalDate.parse("2020-07-11"));
        addHiddenSupplier(3, LocalDate.parse("2020-07-12"));
        importHiddenSuppliersService.importHiddenSupplier();

        Assertions.assertThat(dslContext.selectFrom(Tables.HIDDEN_SUPPLIER).fetchInto(HiddenSupplier.class))
            .usingElementComparatorOnFields("supplierId", "hiddenAt")
            .containsExactlyInAnyOrder(
                new HiddenSupplier().setSupplierId(1).setHiddenAt(LocalDate.parse("2020-07-11")),
                new HiddenSupplier().setSupplierId(3).setHiddenAt(LocalDate.parse("2020-07-12"))
            );
    }

    @Test
    public void importShouldIgnoreDuplicateSuppliers() {
        addHiddenSupplier(1, LocalDate.parse("2020-07-11"));
        addHiddenSupplier(1, LocalDate.parse("2020-07-10"));
        addHiddenSupplier(1, LocalDate.parse("2020-06-11"));
        addHiddenSupplier(1, LocalDate.parse("2020-05-12"));
        addHiddenSupplier(2, LocalDate.parse("2020-07-11"));
        addHiddenSupplier(2, LocalDate.parse("2020-01-12"));
        addHiddenSupplier(2, LocalDate.parse("2010-07-11"));
        addHiddenSupplier(3, LocalDate.parse("2020-07-12"));

        importHiddenSuppliersService.importHiddenSupplier();
        Assertions.assertThat(dslContext.selectFrom(Tables.HIDDEN_SUPPLIER).fetchInto(HiddenSupplier.class))
            .usingElementComparatorOnFields("supplierId", "hiddenAt")
            .containsExactlyInAnyOrder(
                new HiddenSupplier().setSupplierId(1).setHiddenAt(LocalDate.parse("2020-07-11")),
                new HiddenSupplier().setSupplierId(2).setHiddenAt(LocalDate.parse("2020-07-11")),
                new HiddenSupplier().setSupplierId(3).setHiddenAt(LocalDate.parse("2020-07-12"))
            );
    }

    @Test
    public void testImportShouldNotAffectOtherHidings() {
        ServiceOfferReplica offer1 = offer(1, "sku-1", 100L);
        ServiceOfferReplica offer2 = offer(2, "sku-2", 200L);
        serviceOfferReplicaRepository.save(offer1, offer2);
        dslContext.newRecord(Tables.HIDDEN_SUPPLIER,
            new HiddenSupplier().setSupplierId(2).setHiddenAt(LocalDate.parse("2020-07-10"))).insert();

        List<Hiding> hidings = new ArrayList<>();
        Hiding saved1 = createHiding(skk45KDescr.getId(), null, offer1, USER_1, null, null);
        Hiding saved2 = createHiding(aboLegalDescr.getId(), "1", offer1, USER_1, null, null);
        hidings.add(saved1);
        hidings.add(saved2);

        Hiding saved3 = createHiding(feed52LDescr.getId(), HidingReason.FEED_52L_SUBREASON, offer1);
        hidings.add(saved3);
        Hiding saved4 = createHiding(partnerApiDescr.getId(), HidingReason.PARTNER_API_SUBREASON, offer1);
        hidings.add(saved4);

        insertHidings(saved1, saved2, saved3, saved4);

        importHiddenSuppliersService.syncHiddenOffers();
        ArrayList<Hiding> expected = new ArrayList<>(hidings);
        expected.add(createHiding(supplierDescr.getId(), Integer.toString(offer2.getSupplierId()), offer2, USER_1,
            "2020-07-10T00:00:00.00Z", null));
        Assertions.assertThat(getAllHidings())
            .usingElementComparatorOnFields("reasonKeyId", "subreasonId", "supplierId", "shopSku")
            .containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void testUpdateAndInsertNewOfferHidings() {
        ServiceOfferReplica offer1 = offer(1, "sku-1", 100L);
        ServiceOfferReplica offer2 = offer(2, "sku-2", 200L);
        serviceOfferReplicaRepository.save(offer1, offer2);
        dslContext.newRecord(Tables.HIDDEN_SUPPLIER,
            new HiddenSupplier().setSupplierId(1).setHiddenAt(LocalDate.parse("2020-07-10"))).insert();

        importHiddenSuppliersService.syncHiddenOffers();

        Assertions.assertThat(getAllHidings())
            .usingElementComparatorOnFields("reasonKeyId", "supplierId", "shopSku", "hiddenAt")
            .containsExactlyInAnyOrder(
                createHiding(supplierDescr.getId(), HidingReason.HIDDEN_SUPPLIER_SUBREASON, offer1,
                    "2020-07-10T00:00:00.00Z")
            );

        ServiceOfferReplica offer3 = offer(1, "sku-3", 200L);
        serviceOfferReplicaRepository.save(offer3);
        dslContext.newRecord(Tables.HIDDEN_SUPPLIER,
            new HiddenSupplier().setSupplierId(2).setHiddenAt(LocalDate.parse("2020-06-09"))).insert();

        importHiddenSuppliersService.syncHiddenOffers();
        Assertions.assertThat(getAllHidings())
            .usingElementComparatorOnFields("reasonKeyId", "supplierId", "shopSku", "hiddenAt")
            .containsExactlyInAnyOrder(
                createHiding(supplierDescr.getId(),
                    HidingReason.HIDDEN_SUPPLIER_SUBREASON, offer1, "2020-07-10T00:00:00.00Z"),
                createHiding(supplierDescr.getId(),
                    HidingReason.HIDDEN_SUPPLIER_SUBREASON, offer2, "2020-06-09T00:00:00.00Z"),
                createHiding(supplierDescr.getId(),
                    HidingReason.HIDDEN_SUPPLIER_SUBREASON, offer3, "2020-07-10T00:00:00.00Z")
            );

        dslContext.deleteFrom(Tables.HIDDEN_SUPPLIER).where(Tables.HIDDEN_SUPPLIER.SUPPLIER_ID.eq(1)).execute();
        importHiddenSuppliersService.syncHiddenOffers();
        Assertions.assertThat(getAllHidings())
            .usingElementComparatorOnFields("reasonKeyId", "supplierId", "shopSku", "hiddenAt")
            .containsExactlyInAnyOrder(
                createHiding(supplierDescr.getId(),
                    HidingReason.HIDDEN_SUPPLIER_SUBREASON, offer2, "2020-06-09T00:00:00.00Z")
            );
    }

    @Test
    public void testUpdateAndInsertNewServiceOfferHidings() {
        var offer = offer(100, "sku-1", 100L);
        offer = serviceOfferReplicaRepository.save(offer.setSupplierId(101)).get(0);
        dslContext.newRecord(Tables.HIDDEN_SUPPLIER,
            new HiddenSupplier().setSupplierId(101).setHiddenAt(LocalDate.parse("2020-07-10"))).insert();

        importHiddenSuppliersService.syncHiddenOffers();

        Assertions.assertThat(getAllHidings())
            .usingElementComparatorOnFields("reasonKeyId", "supplierId", "shopSku", "hiddenAt")
            .containsExactlyInAnyOrder(
                createHiding(supplierDescr.getId(),
                    HidingReason.HIDDEN_SUPPLIER_SUBREASON, offer, 101, "2020-07-10T00:00:00.00Z")
            );

        serviceOfferReplicaRepository.save(serviceOfferReplicaRepository
            .findOfferByKey(new ServiceOfferKey(101, "sku-1")).setSupplierId(102));
        dslContext.newRecord(Tables.HIDDEN_SUPPLIER,
            new HiddenSupplier().setSupplierId(102).setHiddenAt(LocalDate.parse("2020-06-09"))).insert();

        importHiddenSuppliersService.syncHiddenOffers();
        Assertions.assertThat(getAllHidings())
            .usingElementComparatorOnFields("reasonKeyId", "supplierId", "shopSku", "hiddenAt")
            .containsExactlyInAnyOrder(
                createHiding(supplierDescr.getId(),
                    HidingReason.HIDDEN_SUPPLIER_SUBREASON, offer, 101, "2020-07-10T00:00:00.00Z"),
                createHiding(supplierDescr.getId(),
                    HidingReason.HIDDEN_SUPPLIER_SUBREASON, offer, 102, "2020-06-09T00:00:00.00Z")
            );

        dslContext.deleteFrom(Tables.HIDDEN_SUPPLIER).where(Tables.HIDDEN_SUPPLIER.SUPPLIER_ID.eq(101)).execute();
        importHiddenSuppliersService.syncHiddenOffers();
        Assertions.assertThat(getAllHidings())
            .usingElementComparatorOnFields("reasonKeyId", "supplierId", "shopSku", "hiddenAt")
            .containsExactlyInAnyOrder(
                createHiding(supplierDescr.getId(),
                    HidingReason.HIDDEN_SUPPLIER_SUBREASON, offer, 102, "2020-06-09T00:00:00.00Z")
            );
    }
}

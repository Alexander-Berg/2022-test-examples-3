package ru.yandex.market.deepmind.tms.services;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import ru.yandex.market.deepmind.common.availability.ssku.SskuAvailabilityFilter;
import ru.yandex.market.deepmind.common.availability.task_queue.handlers.ShopSkuAvailabilityChangedHandler;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.BlockReasonKey;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Hiding;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.HidingReasonDescription;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuAvailabilityMatrix;
import ru.yandex.market.deepmind.common.hiding.HidingReason;
import ru.yandex.market.deepmind.common.hiding.HidingReasonDescriptionRepository;
import ru.yandex.market.deepmind.common.hiding.HidingRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.ssku.SskuAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.services.DeepmindConstants;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.ABO_FAULTY_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.ABO_LEGAL_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.SKK_45J_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.SKK_45K_SUBREASON;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.MARSHRUT_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_RETURN_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;
import static ru.yandex.market.deepmind.tms.services.SyncSskuAvailabilityMatrixService.AUTO_HIDING_COMMENT;


/**
 * @author kravchenko-aa
 * @date 19.03.2020
 */
public class SyncSskuAvailabilityMatrixServiceTest extends BaseHidingsServiceTest {
    @Autowired
    private SskuAvailabilityMatrixRepository sskuAvailabilityMatrixRepository;
    @Autowired
    private NamedParameterJdbcOperations namedJdbcTemplate;
    @Autowired
    private HidingRepository hidingRepository;
    @Autowired
    private HidingReasonDescriptionRepository hidingReasonDescriptionRepository;

    private SyncSskuAvailabilityMatrixService syncSskuAvailabilityMatrixService;

    private ServiceOfferReplica offer1;
    private ServiceOfferReplica offer2;
    private ServiceOfferReplica offer3;
    private ServiceOfferReplica offer4;
    private ServiceOfferReplica offer5;
    private HidingReasonDescription aboFaultyDescr;
    private HidingReasonDescription aboLegalDescr;
    private HidingReasonDescription skk45KDescr;
    private HidingReasonDescription skk45JDescr;

    @Before
    public void setUp() {
        super.setUp();
        syncSskuAvailabilityMatrixService = new SyncSskuAvailabilityMatrixService(namedJdbcTemplate,
            Mockito.mock(ShopSkuAvailabilityChangedHandler.class), hidingReasonDescriptionRepository);

        offer1 = offer(1, "ssku1");
        offer2 = offer(2, "ssku2");
        offer3 = offer(3, "ssku3");
        offer4 = offer(3, "ssku4");
        offer5 = offer(3, "ssku5");

        var hidingsDescriptionMap = insertHidingsReasonDescriptionsWithRes(
            createReasonDescription(ABO_FAULTY_SUBREASON.toReasonKey()),
            createReasonDescription(ABO_LEGAL_SUBREASON.toReasonKey()),
            createReasonDescription(SKK_45K_SUBREASON.toReasonKey()),
            createReasonDescription(SKK_45J_SUBREASON.toReasonKey())
        );
        aboFaultyDescr = hidingsDescriptionMap.get(ABO_FAULTY_SUBREASON.toReasonKey());
        skk45KDescr = hidingsDescriptionMap.get(SKK_45K_SUBREASON.toReasonKey());
        skk45JDescr = hidingsDescriptionMap.get(SKK_45J_SUBREASON.toReasonKey());
        aboLegalDescr = hidingsDescriptionMap.get(ABO_LEGAL_SUBREASON.toReasonKey());
    }

    @Test
    public void testDryRun() {
        run();
        List<SskuAvailabilityMatrix> all = sskuAvailabilityMatrixRepository.findAll();
        assertThat(all).isEmpty();

        SskuAvailabilityMatrix ignoreAvailability = row(offer1, 0L);
        sskuAvailabilityMatrixRepository.save(ignoreAvailability);

        run();
        all = sskuAvailabilityMatrixRepository.findAll();
        assertThat(all)
            .usingElementComparatorOnFields("supplierId", "shopSku", "available", "warehouseId")
            .containsExactlyInAnyOrder(ignoreAvailability);
    }

    @Test
    public void testInsertSeveralHidings() {
        Hiding aboFaulty = createHiding(aboFaultyDescr.getId(), HidingReason.ABO_FAULTY_SUBREASON.toString(), offer1,
            USER_1, null, null);
        Hiding aboLegal = createHiding(aboLegalDescr.getId(), HidingReason.ABO_LEGAL_SUBREASON.toString(), offer2,
            USER_1, null, null);
        Hiding skk45j = createHiding(skk45JDescr.getId(), HidingReason.SKK_45J_SUBREASON.toString(), offer3,
            USER_1, null, null);
        Hiding ass = createHiding(skk45KDescr.getId(), "жопа", offer4, null, null, null);
        Hiding drugs = createHiding(skk45KDescr.getId(), "наркотики", offer5, null, null, null);

        insertHidings(aboFaulty, aboLegal, skk45j, ass, drugs);

        run();
        List<SskuAvailabilityMatrix> all = sskuAvailabilityMatrixRepository.findAll();
        assertThat(all)
            .usingElementComparatorIgnoringFields("id", "createdAt", "modifiedAt")
            .containsExactlyInAnyOrder(
                row(aboLegal, ROSTOV_ID).setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK),
                row(aboLegal, SOFINO_ID).setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK),
                row(aboLegal, TOMILINO_ID).setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK),
                row(aboLegal, MARSHRUT_ID).setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK),
                row(aboLegal, SOFINO_RETURN_ID).setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK),

                row(ass, ROSTOV_ID).setBlockReasonKey(BlockReasonKey.SSKU_STOP_WORD_BLOCK),
                row(ass, SOFINO_ID).setBlockReasonKey(BlockReasonKey.SSKU_STOP_WORD_BLOCK),
                row(ass, TOMILINO_ID).setBlockReasonKey(BlockReasonKey.SSKU_STOP_WORD_BLOCK),
                row(ass, MARSHRUT_ID).setBlockReasonKey(BlockReasonKey.SSKU_STOP_WORD_BLOCK),
                row(ass, SOFINO_RETURN_ID).setBlockReasonKey(BlockReasonKey.SSKU_STOP_WORD_BLOCK),

                row(drugs, ROSTOV_ID).setBlockReasonKey(BlockReasonKey.SSKU_STOP_WORD_BLOCK),
                row(drugs, SOFINO_ID).setBlockReasonKey(BlockReasonKey.SSKU_STOP_WORD_BLOCK),
                row(drugs, TOMILINO_ID).setBlockReasonKey(BlockReasonKey.SSKU_STOP_WORD_BLOCK),
                row(drugs, MARSHRUT_ID).setBlockReasonKey(BlockReasonKey.SSKU_STOP_WORD_BLOCK),
                row(drugs, SOFINO_RETURN_ID).setBlockReasonKey(BlockReasonKey.SSKU_STOP_WORD_BLOCK)
            );
    }

    @Test
    public void testInsertThenDelete() {
        Hiding aboLegal = createHiding(aboLegalDescr.getId(),
            HidingReason.ABO_LEGAL_SUBREASON.toString(), offer3, USER_1, null, null);
        Hiding ass = createHiding(skk45KDescr.getId(), "жопа", offer1, null, null, null);
        Hiding drugs = createHiding(skk45KDescr.getId(), "наркотики", offer2, null, null, null);

        insertHidings(aboLegal, ass, drugs);

        run();
        List<SskuAvailabilityMatrix> all = sskuAvailabilityMatrixRepository.findAll();
        assertThat(all)
            .usingElementComparatorIgnoringFields("id", "createdAt", "createdLogin", "modifiedAt")
            .containsExactlyInAnyOrder(
                row(offer1, ROSTOV_ID).setBlockReasonKey(BlockReasonKey.SSKU_STOP_WORD_BLOCK),
                row(offer1, SOFINO_ID).setBlockReasonKey(BlockReasonKey.SSKU_STOP_WORD_BLOCK),
                row(offer1, TOMILINO_ID).setBlockReasonKey(BlockReasonKey.SSKU_STOP_WORD_BLOCK),
                row(offer1, MARSHRUT_ID).setBlockReasonKey(BlockReasonKey.SSKU_STOP_WORD_BLOCK),
                row(offer1, SOFINO_RETURN_ID).setBlockReasonKey(BlockReasonKey.SSKU_STOP_WORD_BLOCK),

                row(offer2, ROSTOV_ID).setBlockReasonKey(BlockReasonKey.SSKU_STOP_WORD_BLOCK),
                row(offer2, SOFINO_ID).setBlockReasonKey(BlockReasonKey.SSKU_STOP_WORD_BLOCK),
                row(offer2, TOMILINO_ID).setBlockReasonKey(BlockReasonKey.SSKU_STOP_WORD_BLOCK),
                row(offer2, MARSHRUT_ID).setBlockReasonKey(BlockReasonKey.SSKU_STOP_WORD_BLOCK),
                row(offer2, SOFINO_RETURN_ID).setBlockReasonKey(BlockReasonKey.SSKU_STOP_WORD_BLOCK),

                row(offer3, ROSTOV_ID).setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK),
                row(offer3, SOFINO_ID).setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK),
                row(offer3, TOMILINO_ID).setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK),
                row(offer3, MARSHRUT_ID).setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK),
                row(offer3, SOFINO_RETURN_ID).setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK)
            );

        hidingRepository.delete(getAllHidings().stream()
            .filter(h -> h.getShopSku().equals("ssku2") || h.getShopSku().equals("ssku3"))
            .map(Hiding::getId)
            .collect(Collectors.toList()));

        run();
        all = sskuAvailabilityMatrixRepository.findAll();
        assertThat(all)
            .usingElementComparatorIgnoringFields("id", "createdAt", "createdLogin", "modifiedAt")
            .containsExactlyInAnyOrder(
                row(offer1, ROSTOV_ID).setBlockReasonKey(BlockReasonKey.SSKU_STOP_WORD_BLOCK),
                row(offer1, SOFINO_ID).setBlockReasonKey(BlockReasonKey.SSKU_STOP_WORD_BLOCK),
                row(offer1, TOMILINO_ID).setBlockReasonKey(BlockReasonKey.SSKU_STOP_WORD_BLOCK),
                row(offer1, MARSHRUT_ID).setBlockReasonKey(BlockReasonKey.SSKU_STOP_WORD_BLOCK),
                row(offer1, SOFINO_RETURN_ID).setBlockReasonKey(BlockReasonKey.SSKU_STOP_WORD_BLOCK)
            );
    }

    @Test
    public void testInsertThenDeleteThenInsert() {
        Hiding aboLegal = createHiding(aboLegalDescr.getId(),
            HidingReason.ABO_LEGAL_SUBREASON.toString(), offer1, USER_1, null, null);
        Hiding drugs = createHiding(skk45KDescr.getId(), "наркотики", offer2, null, null, null);

        insertHidings(aboLegal, drugs);

        run();
        List<SskuAvailabilityMatrix> all = sskuAvailabilityMatrixRepository.findAll();
        assertThat(all)
            .usingElementComparatorIgnoringFields("id", "createdAt", "createdLogin", "modifiedAt")
            .containsExactlyInAnyOrder(
                row(offer1, ROSTOV_ID).setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK),
                row(offer1, SOFINO_ID).setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK),
                row(offer1, TOMILINO_ID).setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK),
                row(offer1, MARSHRUT_ID).setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK),
                row(offer1, SOFINO_RETURN_ID).setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK),

                row(offer2, ROSTOV_ID).setBlockReasonKey(BlockReasonKey.SSKU_STOP_WORD_BLOCK),
                row(offer2, SOFINO_ID).setBlockReasonKey(BlockReasonKey.SSKU_STOP_WORD_BLOCK),
                row(offer2, TOMILINO_ID).setBlockReasonKey(BlockReasonKey.SSKU_STOP_WORD_BLOCK),
                row(offer2, MARSHRUT_ID).setBlockReasonKey(BlockReasonKey.SSKU_STOP_WORD_BLOCK),
                row(offer2, SOFINO_RETURN_ID).setBlockReasonKey(BlockReasonKey.SSKU_STOP_WORD_BLOCK)
            );

        hidingRepository.delete(getAllHidings().stream()
            .filter(h -> h.getShopSku().equals("ssku2")).findFirst().get().getId());

        run();
        all = sskuAvailabilityMatrixRepository.findAll();
        assertThat(all)
            .usingElementComparatorIgnoringFields("id", "createdAt", "createdLogin", "modifiedAt")
            .containsExactlyInAnyOrder(
                row(offer1, ROSTOV_ID).setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK),
                row(offer1, SOFINO_ID).setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK),
                row(offer1, TOMILINO_ID).setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK),
                row(offer1, MARSHRUT_ID).setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK),
                row(offer1, SOFINO_RETURN_ID).setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK)
            );

        Hiding aboLegal2 = createHiding(aboLegalDescr.getId(), HidingReason.ABO_LEGAL_SUBREASON.toString(), offer2,
            USER_1, null, null);
        insertHidings(aboLegal2);

        run();
        all = sskuAvailabilityMatrixRepository.findAll();
        assertThat(all)
            .usingElementComparatorIgnoringFields("id", "createdAt", "createdLogin", "modifiedAt")
            .containsExactlyInAnyOrder(
                row(offer1, ROSTOV_ID).setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK),
                row(offer1, SOFINO_ID).setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK),
                row(offer1, TOMILINO_ID).setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK),
                row(offer1, MARSHRUT_ID).setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK),
                row(offer1, SOFINO_RETURN_ID).setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK),

                row(offer2, ROSTOV_ID).setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK),
                row(offer2, SOFINO_ID).setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK),
                row(offer2, TOMILINO_ID).setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK),
                row(offer2, MARSHRUT_ID).setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK),
                row(offer2, SOFINO_RETURN_ID).setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK)
            );
    }

    @Test
    public void testInsertHidingShouldNotCorruptOtherReasons() {
        SskuAvailabilityMatrix ignoreAvailability = row(offer1, 0L);
        sskuAvailabilityMatrixRepository.save(ignoreAvailability);

        run();
        List<SskuAvailabilityMatrix> all = sskuAvailabilityMatrixRepository.findAll();
        assertThat(all)
            .usingElementComparatorOnFields("supplierId", "shopSku", "available", "warehouseId")
            .containsExactlyInAnyOrder(ignoreAvailability);

        Hiding aboLegal = createHiding(aboLegalDescr.getId(),
            HidingReason.ABO_LEGAL_SUBREASON.toString(), offer1, USER_1, null, null);
        insertHidings(aboLegal);

        run();
        all = sskuAvailabilityMatrixRepository.findAll();
        assertThat(all)
            .usingElementComparatorOnFields("supplierId", "shopSku", "available", "warehouseId")
            .containsExactlyInAnyOrder(
                ignoreAvailability,

                row(aboLegal, ROSTOV_ID),
                row(aboLegal, SOFINO_ID),
                row(aboLegal, TOMILINO_ID),
                row(aboLegal, MARSHRUT_ID),
                row(aboLegal, SOFINO_RETURN_ID)
            );
    }

    @Test
    public void testRemoveAvailability() {
        Hiding hiding = createHiding(skk45KDescr.getId(), "жопа", offer1, null, null, null);
        insertHidings(hiding);

        run();
        assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "shopSku", "available", "warehouseId")
            .containsExactlyInAnyOrder(
                row(hiding, ROSTOV_ID),
                row(hiding, SOFINO_ID),
                row(hiding, TOMILINO_ID),
                row(hiding, MARSHRUT_ID),
                row(hiding, SOFINO_RETURN_ID)
            );

        hidingRepository.deleteAll();

        run();
        assertThat(sskuAvailabilityMatrixRepository.findAll()).isEmpty();
    }

    @Test
    public void testNotDeleteManualChangedAvailability() {
        Hiding hiding = createHiding(skk45KDescr.getId(), "жопа", offer1, null, null, null);
        insertHidings(hiding);

        run();
        assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "shopSku", "available", "warehouseId")
            .containsExactlyInAnyOrder(
                row(hiding, ROSTOV_ID),
                row(hiding, SOFINO_ID),
                row(hiding, TOMILINO_ID),
                row(hiding, MARSHRUT_ID),
                row(hiding, SOFINO_RETURN_ID)
            );

        SskuAvailabilityMatrix matrix = sskuAvailabilityMatrixRepository.find(new SskuAvailabilityFilter()
            .setWarehouseIds(List.of(ROSTOV_ID)))
            .get(0).setDateFrom(LocalDate.now());

        sskuAvailabilityMatrixRepository.save(matrix);
        hidingRepository.deleteAll();

        run();
        assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "shopSku", "available", "warehouseId")
            .containsExactlyInAnyOrder(
                row(hiding, ROSTOV_ID)
            );
    }

    @Test
    public void testNotInsertNewAvailabilityIfUserDeletedIt() {
        Hiding hiding = createHiding(skk45KDescr.getId(), "жопа", offer1, null, null, null);
        insertHidings(hiding);

        run();
        assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "shopSku", "available", "warehouseId")
            .containsExactlyInAnyOrder(
                row(hiding, ROSTOV_ID),
                row(hiding, SOFINO_ID),
                row(hiding, TOMILINO_ID),
                row(hiding, MARSHRUT_ID),
                row(hiding, SOFINO_RETURN_ID)
            );

        SskuAvailabilityMatrix matrix = sskuAvailabilityMatrixRepository.find(new SskuAvailabilityFilter()
            .setWarehouseIds(List.of(ROSTOV_ID))).get(0);
        sskuAvailabilityMatrixRepository.save(matrix.setAvailable(null));

        run();
        assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "shopSku", "available", "warehouseId")
            .containsExactlyInAnyOrder(
                row(hiding, ROSTOV_ID).setAvailable(null),
                row(hiding, SOFINO_ID),
                row(hiding, TOMILINO_ID),
                row(hiding, MARSHRUT_ID),
                row(hiding, SOFINO_RETURN_ID)
            );
    }

    private void run() {
        syncSskuAvailabilityMatrixService.syncHidingsAvailabilities();
    }

    private SskuAvailabilityMatrix row(ServiceOfferReplica offer, long warehouseId) {
        return new SskuAvailabilityMatrix()
            .setSupplierId(offer.getBusinessId())
            .setShopSku(offer.getShopSku())
            .setAvailable(false)
            .setWarehouseId(warehouseId)
            .setCreatedLogin("test")
            .setComment(AUTO_HIDING_COMMENT);
    }

    private SskuAvailabilityMatrix row(Hiding hiding, long warehouseId) {
        return new SskuAvailabilityMatrix()
            .setSupplierId(hiding.getSupplierId())
            .setShopSku(hiding.getShopSku())
            .setAvailable(false)
            .setWarehouseId(warehouseId)
            .setCreatedLogin(DeepmindConstants.AUTO_HIDING_ROBOT)
            .setComment(AUTO_HIDING_COMMENT);
    }
}

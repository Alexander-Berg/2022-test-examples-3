package ru.yandex.market.deepmind.tms.services;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Hiding;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.HidingReasonDescription;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.hiding.BlueOfferAboHiding;
import ru.yandex.market.deepmind.common.hiding.HidingReason;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.services.mbousers.models.MboUser;

import static ru.yandex.market.deepmind.common.hiding.HidingReason.ABO_FAULTY_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.ABO_LEGAL_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.ABO_OTHER_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.SKK_45J_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.SKK_45K_SUBREASON;

@SuppressWarnings({"checkstyle:MagicNumber", "checkstyle:LineLength"})
public class ImportAboHidingsServiceTest extends BaseHidingsServiceTest {

    private ImportAboHidingsService importService;
    private List<BlueOfferAboHiding> aboHidings = new ArrayList<>();
    private HidingReasonDescription aboFaultyDescr;
    private HidingReasonDescription aboLegalDescr;
    private HidingReasonDescription aboOtherDescr;
    private HidingReasonDescription aboWrongGoodInfoDescr;
    private HidingReasonDescription skk45KDescr;
    private HidingReasonDescription skk45JDescr;

    @Before
    public void setUp() {
        super.setUp();

        importService = Mockito.spy(new ImportAboHidingsService(
            namedParameterJdbcTemplate, TransactionHelper.MOCK, null, "//url",
            BERU_ID));
        try {
            Mockito.doAnswer(__ -> aboHidings).when(importService).fetchData(Mockito.anyString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        var hidingsDescriptionMap = insertHidingsReasonDescriptionsWithRes(
            createReasonDescription(ABO_FAULTY_SUBREASON.toReasonKey(), "Брак"),
            createReasonDescription(ABO_LEGAL_SUBREASON.toReasonKey(), "Брак"),
            createReasonDescription(ABO_OTHER_SUBREASON.toReasonKey(), "Брак"),
            createReasonDescription(SKK_45K_SUBREASON.toReasonKey(),
                "Не важно что это тут за текст :,.;№%", "По стоп слову"),
            createReasonDescription(SKK_45J_SUBREASON.toReasonKey(),
                "Предложение невозможно разместить на Маркете"),
            createReasonDescription(HidingReason.ABO_WRONG_GOOD_INFO_SUBREASON.toReasonKey())
        );
        aboFaultyDescr = hidingsDescriptionMap.get(ABO_FAULTY_SUBREASON.toReasonKey());
        aboLegalDescr = hidingsDescriptionMap.get(ABO_LEGAL_SUBREASON.toReasonKey());
        aboOtherDescr = hidingsDescriptionMap.get(ABO_OTHER_SUBREASON.toReasonKey());
        skk45KDescr = hidingsDescriptionMap.get(SKK_45K_SUBREASON.toReasonKey());
        skk45JDescr = hidingsDescriptionMap.get(SKK_45J_SUBREASON.toReasonKey());
        aboWrongGoodInfoDescr = hidingsDescriptionMap.get(HidingReason.ABO_WRONG_GOOD_INFO_SUBREASON.toReasonKey());
    }

    @Test
    public void testImport() {
        var offer1 = offer(1, "sku-1", 200);
        var offer2 = offer(2, "sku-2", 200);
        var offer3 = offer(3, "sku-3", 200);
        serviceOfferReplicaRepository.save(offer1, offer2, offer3);

        addAboHiding(offer1, USER_1, 1, HidingReason.ABO_FAULTY_SUBREASON, null, "txt");
        addAboHiding(offer2, USER_2, 2, HidingReason.ABO_LEGAL_SUBREASON, "2007-12-03T10:15:30.00Z", null);

        importService.syncAboHidingsFromS3ToPg();

        List<Hiding> hidings = getAllHidings();

        Assertions.assertThat(hidings)
            .usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(
                createHiding(aboFaultyDescr.getId(), offer1, USER_1, 1, HidingReason.ABO_FAULTY_SUBREASON, null, "txt"),
                createHiding(aboLegalDescr.getId(), offer2, USER_2, 2, HidingReason.ABO_LEGAL_SUBREASON, "2007-12-03T10:15:30.00Z", null)
            );
    }

    @Test
    public void testSavingReasonKeyWhileImport() {
        var offer1 = offer(1, "sku-1", 200);
        var offer2 = offer(2, "sku-2", 200);
        var offer3 = offer(3, "sku-3", 200);
        serviceOfferReplicaRepository.save(offer1, offer2, offer3);

        addAboHiding(offer1, USER_1, 1, HidingReason.ABO_FAULTY_SUBREASON + "_newsubreason", null, "txt");
        addAboHiding(offer2, USER_2, 2, HidingReason.ABO_LEGAL_SUBREASON, "2007-12-03T10:15:30.00Z", null);

        importService.syncAboHidingsFromS3ToPg();

        List<Hiding> hidings = getAllHidings();
        var newReasonKeyId = hidingReasonDescriptionRepository.findByReasonKeys(
            HidingReason.ABO_FAULTY_SUBREASON.toReasonKey() + "_newsubreason")
            .get(0).getId();

        Assertions.assertThat(hidings)
            .usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(
                createHiding(newReasonKeyId, "1", offer1, USER_1, null, "txt"),
                createHiding(aboLegalDescr.getId(), "2", offer2, USER_2, "2007-12-03T10:15:30.00Z", null)
            );

        Assertions.assertThat(getExistingReasonKeys())
            .contains(HidingReason.ABO_FAULTY_SUBREASON.toReasonKey() + "_newsubreason");
    }

    @Test
    public void testImport1PHidings() {
        // offers on real suppliers
        var realOffer71 = offer(77, "real-sku-1", 200);
        var realOffer72 = offer(77, "real-sku-2", 700);
        var realOffer73 = offer(77, "real-sku-3", 700);
        var realOffer74 = offer(77, "real-sku-4", 300);
        serviceOfferReplicaRepository.save(realOffer71, realOffer72, realOffer73,
            realOffer74);

        addAboHiding(BERU_ID, "000042.real-sku-1", USER_2, 4, ABO_OTHER_SUBREASON, null, null);
        addAboHiding(BERU_ID, 700, USER_2, 5, ABO_OTHER_SUBREASON, null, null);
        // this will be skipped, because we don't import 1P directly DEEPMIND-297
        addAboHiding(77, "real-sku-4", USER_1, 6, HidingReason.ABO_FAULTY_SUBREASON, null, null);

        importService.syncAboHidingsFromS3ToPg();

        List<Hiding> hidings = getAllHidings();

        Assertions.assertThat(hidings)
            .usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(
                createHiding(aboOtherDescr.getId(), realOffer71, USER_2, 4, ABO_OTHER_SUBREASON, null, null),
                createHiding(aboOtherDescr.getId(), realOffer72, USER_2, 5, ABO_OTHER_SUBREASON, null, null),
                createHiding(aboOtherDescr.getId(), realOffer73, USER_2, 5, ABO_OTHER_SUBREASON, null, null)
            );
    }

    @Test
    public void testImportInDifferentCombinations() {
        var offer1 = offer(1, "sku-1", 200);
        var offer2 = offer(2, "sku-2", 200);
        var offer3 = offer(3, "sku-3", 300);
        var offer4 = offer(42, "sku-4", 300);
        // offers on real suppliers
        var realOffer71 = offer(77, "real-sku-1", 200);
        var realOffer72 = offer(77, "real-sku-2", 700);
        var realOffer73 = offer(77, "real-sku-3", 700);
        var realOffer74 = offer(77, "real-sku-4", 300);
        serviceOfferReplicaRepository.save(offer1, offer2, offer3, offer4, realOffer71, realOffer72, realOffer73,
            realOffer74);

        addAboHiding(offer1, USER_1, 1, HidingReason.ABO_FAULTY_SUBREASON, null, null);
        addAboHiding(2, 200, USER_1, 2, HidingReason.ABO_FAULTY_SUBREASON, null, null);
        addAboHiding(300, USER_1, 3, HidingReason.ABO_FAULTY_SUBREASON, null, null);
        addAboHiding(BERU_ID, "000042.real-sku-1", USER_2, 4, ABO_OTHER_SUBREASON, null, null);
        addAboHiding(BERU_ID, 700, USER_2, 5, ABO_OTHER_SUBREASON, null, null);

        importService.syncAboHidingsFromS3ToPg();

        List<Hiding> hidings = getAllHidings();

        Assertions.assertThat(hidings)
            .usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(
                createHiding(aboFaultyDescr.getId(), offer1, USER_1, 1, HidingReason.ABO_FAULTY_SUBREASON, null, null),
                createHiding(aboFaultyDescr.getId(), offer2, USER_1, 2, HidingReason.ABO_FAULTY_SUBREASON, null, null),
                createHiding(aboFaultyDescr.getId(), offer3, USER_1, 3, HidingReason.ABO_FAULTY_SUBREASON, null, null),
                createHiding(aboFaultyDescr.getId(), offer4, USER_1, 3, HidingReason.ABO_FAULTY_SUBREASON, null, null),

                createHiding(aboOtherDescr.getId(), realOffer71, USER_2, 4, ABO_OTHER_SUBREASON, null, null),
                createHiding(aboOtherDescr.getId(), realOffer72, USER_2, 5, ABO_OTHER_SUBREASON, null, null),
                createHiding(aboOtherDescr.getId(), realOffer73, USER_2, 5, ABO_OTHER_SUBREASON, null, null),
                createHiding(aboFaultyDescr.getId(), realOffer74, USER_1, 3, HidingReason.ABO_FAULTY_SUBREASON, null, null)
            );
    }

    @Test
    public void testImportShouldNotAffectOtherHidings() {
        Msku msku1 = createMsku(100L).setTitle("Уцененные наркотики");
        deepmindMskuRepository.save(msku1);

        ServiceOfferReplica offer1 = offer(1, "sku-1", msku1);
        serviceOfferReplicaRepository.save(offer1);

        addAboHiding(offer1, USER_1, 5, HidingReason.ABO_FAULTY_SUBREASON, null, null);

        // в базе будут записи:
        Hiding saved1 = createHiding(aboFaultyDescr.getId(), "1", offer1, USER_1, null, null);
        Hiding saved2 = createHiding(skk45KDescr.getId(), "stop-word", offer1, USER_1, null, null);
        Hiding saved3 = createHiding(skk45JDescr.getId(), null, offer1, USER_1, null, null);
        insertHidings(saved1, saved2, saved3);

        importService.syncAboHidingsFromS3ToPg();

        List<Hiding> hidings = getAllHidings();

        Assertions.assertThat(hidings)
            .usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(
                saved2,
                saved3,
                createHiding(aboFaultyDescr.getId(), offer1, USER_1, 5, HidingReason.ABO_FAULTY_SUBREASON, null, null)
            );
    }

    @Test
    public void testDeleteUpdateAndInsertNewImports() {
        ServiceOfferReplica offer1 = offer(1, "sku-1", 100);
        ServiceOfferReplica offer2 = offer(2, "sku-2", 200);
        ServiceOfferReplica offer3 = offer(3, "sku-3", 200);
        serviceOfferReplicaRepository.save(offer1, offer2, offer3);

        // в базе будут записи:
        Hiding saved1 = createHiding(aboFaultyDescr.getId(), offer1, USER_1, 1, HidingReason.ABO_FAULTY_SUBREASON, "2020-01-01T00:00:00.00Z", null);
        Hiding saved2 = createHiding(aboLegalDescr.getId(), offer2, USER_1, 2, HidingReason.ABO_LEGAL_SUBREASON, "2020-01-01T00:00:00.00Z",
            "text");
        Hiding saved3 = createHiding(aboOtherDescr.getId(), offer3, USER_2, 3, ABO_OTHER_SUBREASON, "2020-01-01T00:00:00.00Z", null);
        insertHidings(saved1, saved2, saved3);

        // в s3 будут записи:
        // эта запись соответствует saved1, обновился комментарий -> в БД запись должна измениться
        addAboHiding(offer1, USER_1, 1, HidingReason.ABO_FAULTY_SUBREASON, "2020-01-01T00:00:00.00Z", "new comment");
        // эта запись соответствует saved2, ничего не поменялось -> в БД запись не должна измениться
        addAboHiding(2, 200, USER_1, 2, HidingReason.ABO_LEGAL_SUBREASON, "2020-01-01T00:00:00.00Z", "text");
        // новая запись
        addAboHiding(200, USER_2, 100500, HidingReason.ABO_WRONG_GOOD_INFO_SUBREASON, null, null);
        // saved3 будет удалена

        importService.syncAboHidingsFromS3ToPg();

        List<Hiding> hidings = getAllHidings();

        Assertions.assertThat(hidings)
            .usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(
                saved1.setComment("new comment"),
                saved2,
                createHiding(aboWrongGoodInfoDescr.getId(), offer2, USER_2, 100500, HidingReason.ABO_WRONG_GOOD_INFO_SUBREASON, null, null),
                createHiding(aboWrongGoodInfoDescr.getId(), offer3, USER_2, 100500, HidingReason.ABO_WRONG_GOOD_INFO_SUBREASON, null, null)
            );
    }

    @Test
    public void testUpdateImportsServiceOffers() {
        ServiceOfferReplica offer1 = offer(101, "sku-1", 200).setBusinessId(100);
        serviceOfferReplicaRepository.save(offer1);
        serviceOfferReplicaRepository.save(offer(102, "sku-1", 200).setBusinessId(100));
        serviceOfferReplicaRepository.save(offer(103, "sku-1", 200).setBusinessId(100));

        // в базе будут записи:
        Hiding saved1 = createHiding(aboFaultyDescr.getId(), offer1, 101, USER_1, 1, HidingReason.ABO_FAULTY_SUBREASON, "2020-01-01T00:00:00.00Z", null);
        Hiding saved2 = createHiding(aboFaultyDescr.getId(), offer1, 102, USER_1, 1, HidingReason.ABO_FAULTY_SUBREASON, "2020-01-01T00:00:00.00Z", null);
        insertHidings(saved1, saved2);

        // в s3 будут записи:
        // эта запись соответствует saved1, обновился комментарий -> в БД запись должна измениться
        addAboHiding(offer1, 101, USER_1, 1, HidingReason.ABO_FAULTY_SUBREASON, "2020-01-01T00:00:00.00Z", "new comment");
        // saved2 будет удалена
        // новая запись
        addAboHiding(103, 200, USER_2, 100500, HidingReason.ABO_WRONG_GOOD_INFO_SUBREASON, null, null);

        importService.syncAboHidingsFromS3ToPg();

        List<Hiding> hidings = getAllHidings();

        Assertions.assertThat(hidings)
            .usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(
                saved1.setComment("new comment"),
                createHiding(aboWrongGoodInfoDescr.getId(), offer1, 103, USER_2, 100500, HidingReason.ABO_WRONG_GOOD_INFO_SUBREASON, null, null)
            );
    }

    @Test
    public void testImportShouldNotCorruptId() {
        ServiceOfferReplica offer1 = offer(1, "sku-1", 200);
        ServiceOfferReplica offer2 = offer(2, "sku-2", 200);
        serviceOfferReplicaRepository.save(offer1, offer2);

        Hiding saved1 = createHiding(aboFaultyDescr.getId(), offer1, USER_1, 1, HidingReason.ABO_FAULTY_SUBREASON, "2020-01-01T00:00:00.00Z", null);
        Hiding saved2 = createHiding(aboLegalDescr.getId(), offer2, USER_1, 2, HidingReason.ABO_LEGAL_SUBREASON, "2020-01-01T00:00:00.00Z", null);
        insertHidings(saved1, saved2);

        Map<Long, Long> hidingIdByReasonKeyId = getAllHidings()
            .stream()
            .collect(Collectors.toMap(Hiding::getReasonKeyId, Hiding::getId));
        saved1.setId(hidingIdByReasonKeyId.get(saved1.getReasonKeyId()));
        saved2.setId(hidingIdByReasonKeyId.get(saved2.getReasonKeyId()));

        addAboHiding(offer1, USER_1, 1, HidingReason.ABO_FAULTY_SUBREASON, "2010-10-10T10:10:10.00Z", null);
        addAboHiding(offer2, USER_1, 2, HidingReason.ABO_LEGAL_SUBREASON, "2020-01-01T00:00:00.00Z", null);

        importService.syncAboHidingsFromS3ToPg();

        Assertions.assertThat(getAllHidings())
            .containsExactlyInAnyOrder(
                saved1.setHiddenAt(Instant.parse("2010-10-10T10:10:10.00Z")),
                saved2
            );
    }

    public void addAboHiding(ServiceOfferReplica offer, MboUser user, int subreasonId, HidingReason subreason, String dateTime,
                             String comment) {
        addAboHiding(offer.getBusinessId(), offer.getShopSku(), null, user, subreasonId, subreason, dateTime,
            comment);
    }

    public void addAboHiding(ServiceOfferReplica offer, MboUser user, int subreasonId, String subreason, String dateTime,
                             String comment) {
        addAboHiding(offer.getBusinessId(), offer.getShopSku(), null, user, subreasonId, subreason, dateTime,
            comment);
    }

    public void addAboHiding(ServiceOfferReplica offer, int supplierId, MboUser user, int subreasonId, HidingReason subreason,
                             String dateTime, String comment) {
        addAboHiding(supplierId, offer.getShopSku(), null, user, subreasonId, subreason, dateTime,
            comment);
    }

    public void addAboHiding(int supplier, String shopSku, MboUser user, int subreasonId, HidingReason subreason,
                             String dateTime, String comment) {
        addAboHiding(supplier, shopSku, null, user, subreasonId, subreason, dateTime, comment);
    }

    public void addAboHiding(int supplier, long mskuId, MboUser user, int subreasonId, HidingReason subreason,
                             String dateTime, String comment) {
        addAboHiding(supplier, null, mskuId, user, subreasonId, subreason, dateTime, comment);
    }

    public void addAboHiding(long mskuId, MboUser user, int subreasonId, HidingReason subreason,
                             String dateTime, String comment) {
        addAboHiding(null, null, mskuId, user, subreasonId, subreason, dateTime, comment);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public void addAboHiding(Integer supplierId, String shopSku, Long mskuId, MboUser user, int subreasonId,
                             HidingReason subreason, String dateTime, String comment) {
        Preconditions.checkArgument(subreason.isSubReason());
        addAboHiding(supplierId, shopSku, mskuId, user, subreasonId, subreason.toString(), dateTime, comment);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public void addAboHiding(Integer supplierId, String shopSku, Long mskuId, MboUser user, int subreasonId,
                             String subreason, String dateTime, String comment) {
        BlueOfferAboHiding hiding = new BlueOfferAboHiding();
        hiding.setId(subreasonId);
        hiding.setHidingReason(subreason);
        if (supplierId != null) {
            hiding.setSupplierId(supplierId);
        }
        if (shopSku != null) {
            hiding.setShopSku(shopSku);
        }
        if (mskuId != null) {
            hiding.setMarketSku(mskuId);
        }
        hiding.setUserId(user.getUid());
        hiding.setName(user.getFullName());
        hiding.setComment(comment);
        hiding.setCreationTime(dateTime == null ? null : Instant.parse(dateTime).toEpochMilli());
        aboHidings.add(hiding);
    }

    public Hiding createHiding(Long reasonKeyId, ServiceOfferReplica offer, MboUser user, int subreasonId, HidingReason subreason, String hidingAt,
                               String comment) {
        return createHiding(reasonKeyId, String.valueOf(subreasonId), offer, user, hidingAt,
            comment);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public Hiding createHiding(Long reasonKeyId, ServiceOfferReplica offer, int supplierId, MboUser user, int subreasonId, HidingReason subreason, String hidingAt,
                               String comment) {
        return createHiding(reasonKeyId, String.valueOf(subreasonId), offer, user, hidingAt,
            comment).setSupplierId(supplierId);
    }
}

package ru.yandex.market.deepmind.tms.services;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.stubs.YtTablesStub;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Hiding;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.HidingReasonDescription;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.mboc.common.ReplicaCluster;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.infrastructure.util.UnstableInit;
import ru.yandex.market.mboc.common.services.mbousers.models.MboUser;

import static ru.yandex.market.deepmind.common.hiding.HidingReason.ABO_FAULTY_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.SKK_45J_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.SKK_45K_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.SKK_45Y_SUBREASON;


@SuppressWarnings("checkstyle:MagicNumber")
public class ImportStopWordsHidingsServiceTest extends BaseHidingsServiceTest {
    private ImportStopWordsHidingsService importService;

    private HidingReasonDescription aboFaultyDescr;
    private HidingReasonDescription skk45KDescr;
    private HidingReasonDescription skk45JDescr;
    private HidingReasonDescription skk45YDescr;

    @Before
    @Override
    public void setUp() {
        super.setUp();

        UnstableInit<Yt> ytMockUnst = Mockito.mock(UnstableInit.class);
        Yt ytMock = Mockito.mock(Yt.class);
        Mockito.when(ytMockUnst.get()).thenReturn(ytMock);

        importService = new ImportStopWordsHidingsService(
            namedParameterJdbcTemplate,
            hidingReasonDescriptionRepository,
            ReplicaCluster.HAHN,
            ytMockUnst,
            yqlJdbcTemplate,
            TransactionHelper.MOCK,
            stopWordsTable,
            offersTable,
            mapReducePool);

        var format = "//tmp/deepmind/importstopwordshidingsservice/test";
        importService.setTmpTableFormat(format);
        var tmpTable = YPath.simple(format);

        yqlJdbcTemplate.execute("" +
            "create table " + tmpTable + " (\n" +
            "  raw_supplier_id      bigint,\n" +
            "  raw_shop_sku        text,\n" +
            "  stop_word    text,\n" +
            "  user_id      bigint,\n" +
            "  user_name    text,\n" +
            "  comment      text,\n" +
            "  creation_time bigint\n" +
            ");");

        var ytTablesStub = Mockito.mock(YtTablesStub.class);
        Mockito.when(ytMock.tables()).thenReturn(ytTablesStub);
        Mockito.doAnswer(invocation -> {
            var consumer = (Consumer<YTreeMapNode>) invocation.getArgument(2);
            String yql = "" +
                "select " +
                "    raw_supplier_id, " +
                "    raw_shop_sku, " +
                "    stop_word, " +
                "    user_id, " +
                "    user_name, " +
                "    comment, " +
                "    creation_time " +
                "from " + tmpTable;

            yqlJdbcTemplate.query(yql, rs -> {
                var node = YTree.mapBuilder()
                    .key("raw_supplier_id").value(rs.getInt("raw_supplier_id"))
                    .key("raw_shop_sku").value(rs.getString("raw_shop_sku"))
                    .key("stop_word").value(rs.getString("stop_word"))
                    .key("user_id").value(rs.getLong("user_id"))
                    .key("user_name").value(rs.getString("user_name"))
                    .key("comment").value(rs.getString("comment"))
                    .key("creation_time").value(rs.getLong("creation_time"))
                    .buildMap();
                consumer.accept(node);
            });
            return null;
        }).when(ytTablesStub).read(Mockito.any(), Mockito.any(), Mockito.any(Consumer.class));

        var hidingsDescriptionMap = insertHidingsReasonDescriptionsWithRes(
            createReasonDescription(ABO_FAULTY_SUBREASON.toReasonKey()),
            createReasonDescription(SKK_45K_SUBREASON.toReasonKey()),
            createReasonDescription(SKK_45J_SUBREASON.toReasonKey()),
            createReasonDescription(SKK_45Y_SUBREASON.toReasonKey())
        );
        aboFaultyDescr = hidingsDescriptionMap.get(ABO_FAULTY_SUBREASON.toReasonKey());
        skk45KDescr = hidingsDescriptionMap.get(SKK_45K_SUBREASON.toReasonKey());
        skk45JDescr = hidingsDescriptionMap.get(SKK_45J_SUBREASON.toReasonKey());
        skk45YDescr = hidingsDescriptionMap.get(SKK_45Y_SUBREASON.toReasonKey());
    }

    @Test
    public void emptyTable() {
        importService.syncStopWordsHidingsFromYtToPg();
    }

    @Test
    public void testImportStopWords() {
        Msku msku1 = createMsku(100L).setTitle("Макароны с наркотиками");
        Msku msku2 = createMsku(200L).setTitle("Уцененные макароны");
        deepmindMskuRepository.save(msku1, msku2);

        var offer1 = offer(1, "sku-1", msku1);
        var offer2 = offer(2, "sku-2", msku2);
        var offer3 = offer(3, "sku-3", msku2);
        serviceOfferReplicaRepository.save(offer1, offer2, offer3);

        addStopWordHiding(msku1, USER_1, "наркотики", "", null);
        addStopWordHiding(msku2, USER_2, "Уцененка", "уцененка запрещена", "2007-12-03T10:15:30.00Z");

        importService.syncStopWordsHidingsFromYtToPg();

        List<Hiding> hidings = getAllHidings();

        Assertions.assertThat(hidings)
            .usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(
                createHiding(offer1, USER_1, "наркотики", null, null),
                createHiding(offer2, USER_2, "Уцененка", "уцененка запрещена", "2007-12-03T10:15:30.00Z"),
                createHiding(offer3, USER_2, "Уцененка", "уцененка запрещена", "2007-12-03T10:15:30.00Z")
            );
    }

    @Test
    public void testImportStopWordsOnOneSsku() {
        Msku msku1 = createMsku(100L).setTitle("Уцененные наркотики");
        deepmindMskuRepository.save(msku1);

        var offer1 = offer(1, "sku-1", msku1);
        serviceOfferReplicaRepository.save(offer1);

        addStopWordHiding(msku1, USER_1, "наркотики", "наркотики запрещены", null);
        addStopWordHiding(msku1, USER_2, "уцененка", null, null);

        importService.syncStopWordsHidingsFromYtToPg();

        List<Hiding> hidings = getAllHidings();

        Assertions.assertThat(hidings)
            .usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(
                createHiding(offer1, USER_1, "наркотики", "наркотики запрещены", null),
                createHiding(offer1, USER_2, "уцененка", null, null)
            );
    }

    @Test
    public void testImportShouldNotAffectOtherHiddings() {
        Msku msku1 = createMsku(100L).setTitle("Уцененные наркотики");
        deepmindMskuRepository.save(msku1);

        var offer1 = offer(1, "sku-1", msku1);
        serviceOfferReplicaRepository.save(offer1);

        addStopWordHiding(msku1, USER_1, "наркотики", "наркотики запрещены", null);
        addStopWordHiding(msku1, USER_2, "уцененка", null, null);

        // в базе будут записи:
        Hiding saved1 = createHiding(aboFaultyDescr.getId(), null, offer1, USER_1, null, null);
        Hiding saved2 = createHiding(skk45JDescr.getId(), null, offer1, USER_1, null, null);
        Hiding saved3 = createHiding(skk45YDescr.getId(), null, offer1, USER_1, null, null);
        insertHidings(saved1, saved2, saved3);

        importService.syncStopWordsHidingsFromYtToPg();

        List<Hiding> hidings = getAllHidings();

        Assertions.assertThat(hidings)
            .usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(
                saved1,
                saved2,
                saved3,
                createHiding(offer1, USER_1, "наркотики", "наркотики запрещены", null),
                createHiding(offer1, USER_2, "уцененка", null, null)
            );
    }

    @Test
    public void testDeleteUpdateAndInsertNewImports() {
        Msku msku1 = createMsku(100L).setTitle("Уцененные макароны с наркотиками");
        Msku msku2 = createMsku(200L).setTitle("Уцененные макароны с наркотиками");
        deepmindMskuRepository.save(msku1);

        var offer1 = offer(1, "sku-1", msku1);
        var offer2 = offer(2, "sku-2", msku2);
        serviceOfferReplicaRepository.save(offer1, offer2);

        // в базе будут записи:
        Hiding saved1 = createHiding(offer1, USER_1, "уцененка", null, "2020-01-01T00:00:00.00Z");
        Hiding saved2 = createHiding(offer1, USER_1, "наркотики", null, "2020-01-01T00:00:00.00Z");
        Hiding saved3 = createHiding(offer1, USER_1, "кексы", null, "2020-01-01T00:00:00.00Z");
        insertHidings(saved1, saved2, saved3);

        // в yt будут записи:
        // эта запись соответствует saved1, ничего не поменялось -> в БД запись не должна измениться,
        addStopWordHiding(msku1, USER_1, "уцененка", null, "2020-01-01T00:00:00.00Z");
        // эта запись соответствует saved2, обновился комментарий и дата -> в БД запись должна поменяться
        addStopWordHiding(msku1, USER_1, "наркотики", "коммент 2", "2021-01-01T00:00:00.00Z");
        // новая запись
        addStopWordHiding(msku1, USER_2, "Лапша", "не продаем лапшу", "2021-01-01T00:00:00.00Z");
        // saved3 будет удалена

        importService.syncStopWordsHidingsFromYtToPg();

        List<Hiding> hidings = getAllHidings();

        Assertions.assertThat(hidings)
            .usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(
                saved1,
                saved2.setComment("коммент 2").setHiddenAt(Instant.parse("2021-01-01T00:00:00.00Z")),
                createHiding(offer1, USER_2, "Лапша", "не продаем лапшу", "2021-01-01T00:00:00.00Z")
            );
    }

    @Test
    public void testImportShouldNotCorruptId() {
        Msku msku1 = createMsku(100L).setTitle("Уцененные макароны с наркотиками");
        Msku msku2 = createMsku(200L).setTitle("Уцененные макароны с наркотиками");
        deepmindMskuRepository.save(msku1);

        var offer1 = offer(1, "sku-1", msku1);
        var offer2 = offer(2, "sku-2", msku2);
        serviceOfferReplicaRepository.save(offer1, offer2);

        // в базе будут записи:
        Hiding saved1 = createHiding(offer1, USER_1, "наркотики", null, "2020-01-01T00:00:00.00Z");
        Hiding saved2 = createHiding(offer1, USER_1, "макароны", null, "2020-01-01T00:00:00.00Z");
        insertHidings(saved1, saved2);

        Map<String, Long> hidingIdByShopSku = getAllHidings()
            .stream()
            .collect(Collectors.toMap(Hiding::getSubreasonId, Hiding::getId));
        saved1.setId(hidingIdByShopSku.get(saved1.getSubreasonId()));
        saved2.setId(hidingIdByShopSku.get(saved2.getSubreasonId()));

        addStopWordHiding(msku1, USER_1, "наркотики", "коммент 2", "2021-01-01T00:00:00.00Z");
        addStopWordHiding(msku1, USER_1, "макароны", null, "2020-01-01T00:00:00.00Z");
        importService.syncStopWordsHidingsFromYtToPg();

        List<Hiding> hidings = getAllHidings();

        Assertions.assertThat(hidings)
            .containsExactlyInAnyOrder(
                saved1.setComment("коммент 2").setHiddenAt(Instant.parse("2021-01-01T00:00:00.00Z")),
                saved2
            );
    }

    @Test(expected = org.springframework.dao.DuplicateKeyException.class)
    public void testEqualHidingsWontSave() {
        Msku msku1 = createMsku(100L).setTitle("Msku");
        deepmindMskuRepository.save(msku1);

        var offer1 = offer(1, "sku-1", msku1);
        serviceOfferReplicaRepository.save(offer1);

        Hiding saved1 = createHiding(offer1, USER_2, "aaa", null, "2020-01-01T00:00:00.00Z");
        Hiding saved2 = new Hiding(saved1);
        insertHidings(saved1, saved2);
    }

    @Test
    public void testIgnoreBusinessOffer() {
        Msku msku1 = createMsku(100L).setTitle("Макароны с наркотиками");
        deepmindMskuRepository.save(msku1);

        var businessOffer1 = offer(200, "sku-3", msku1).setSupplierId(2).setSupplierType(SupplierType.THIRD_PARTY);
        var businessOffer2 = offer(200, "sku-3", msku1).setSupplierId(3).setSupplierType(SupplierType.REAL_SUPPLIER);
        serviceOfferReplicaRepository.save(businessOffer1, businessOffer2);

        addStopWordHiding(msku1, USER_1, "наркотики", "", null);

        importService.syncStopWordsHidingsFromYtToPg();

        List<Hiding> hidings = getAllHidings();

        Assertions.assertThat(hidings)
            .usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(
                createHiding(businessOffer1, USER_1, "наркотики", null, null),
                createHiding(businessOffer2, USER_1, "наркотики", null, null)
            );
    }

    private Hiding createHiding(ServiceOfferReplica offer, MboUser user, String stopWord, String comment,
                                String hidingAt) {
        return createHiding(skk45KDescr.getId(), stopWord, offer, user, hidingAt, comment);
    }
}

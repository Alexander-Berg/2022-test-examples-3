package ru.yandex.market.deepmind.common.services.hiding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.HidingReasonType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.HidingReasonDescription;
import ru.yandex.market.deepmind.common.services.tanker.TankerServiceMock;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.deepmind.common.services.hiding.ImportHidingReasonDescriptionService.REASON_KEY_SET;
import static ru.yandex.market.deepmind.common.services.hiding.ImportHidingReasonDescriptionService.SUBREASON_KEY_SET;

/**
 * @author kravchenko-aa
 * @date 04.03.2020
 */
@SuppressWarnings({"checkstyle:magicNumber"})
public class ImportHidingReasonDescriptionServiceTest extends DeepmindBaseDbTestClass {
    @Autowired
    private JdbcOperations jdbcOperations;

    private TankerServiceMock tankerClientMock;
    private ImportHidingReasonDescriptionService service;

    @Before
    public void setUp() {
        tankerClientMock = new TankerServiceMock();
        service = new ImportHidingReasonDescriptionService(jdbcOperations, tankerClientMock);
    }

    @Test
    public void testImport() {
        tankerClientMock.addKeys(REASON_KEY_SET, Map.of(
            "ABO", "Скрыты сотрудниками Беру",
            "BAD_QUALITY", "Ошибки качества"));
        tankerClientMock.addKeys(SUBREASON_KEY_SET, Map.of(
            "ABO_FAULTY", "Брак",
            "ABO_LEGAL", "Товар скрыт по запросу правообладателя",
            "BAD_QUALITY_71", "На сайте магазина нет контактной информации"));

        service.syncDescriptionWithTanker();

        List<HidingReasonDescription> descriptions = getAllDescriptions();
        assertThat(descriptions)
            .usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(
                new HidingReasonDescription().setExtendedDesc("Скрыты сотрудниками Беру")
                    .setReasonKey("ABO").setType(HidingReasonType.REASON),
                new HidingReasonDescription().setExtendedDesc("Ошибки качества")
                    .setReasonKey("BAD_QUALITY").setType(HidingReasonType.REASON),
                new HidingReasonDescription().setExtendedDesc("Брак")
                    .setReasonKey("ABO_FAULTY").setType(HidingReasonType.REASON_KEY),
                new HidingReasonDescription().setExtendedDesc("Товар скрыт по запросу правообладателя")
                    .setReasonKey("ABO_LEGAL").setType(HidingReasonType.REASON_KEY),
                new HidingReasonDescription().setExtendedDesc("На сайте магазина нет контактной информации")
                    .setReasonKey("BAD_QUALITY_71").setType(HidingReasonType.REASON_KEY),
                new HidingReasonDescription().setExtendedDesc("Поставщик скрыт")
                    .setReasonKey("HIDDEN_SUPPLIER").setType(HidingReasonType.REASON),
                new HidingReasonDescription().setExtendedDesc("Поставщик скрыт")
                    .setReasonKey("HIDDEN_SUPPLIER_HIDDEN_SUPPLIER").setType(HidingReasonType.REASON_KEY),
                new HidingReasonDescription().setExtendedDesc("")
                    .setReasonKey("FEED_ERROR_SKK_PRICE_DIFF").setType(HidingReasonType.REASON_KEY),
                new HidingReasonDescription().setExtendedDesc("")
                    .setReasonKey("FEED_ERROR_49Q").setType(HidingReasonType.REASON_KEY)
            );
    }

    @Test
    // танкер для неподтвержденных переводов отдает пару key -> ""
    public void testImportNotApprovedDesc() {
        tankerClientMock.addKeys(REASON_KEY_SET, Map.of(
            "ABO", ""));
        tankerClientMock.addKeys(SUBREASON_KEY_SET, Map.of(
            "ABO_FAULTY", "Брак",
            "ABO_LEGAL", "Товар скрыт по запросу правообладателя",
            "BAD_QUALITY_71", ""));
        service.syncDescriptionWithTanker();

        List<HidingReasonDescription> descriptions = getAllDescriptions();
        assertThat(descriptions)
            .usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(
                new HidingReasonDescription().setExtendedDesc("Брак")
                    .setReasonKey("ABO_FAULTY").setType(HidingReasonType.REASON_KEY),
                new HidingReasonDescription().setExtendedDesc("Товар скрыт по запросу правообладателя")
                    .setReasonKey("ABO_LEGAL").setType(HidingReasonType.REASON_KEY),

                new HidingReasonDescription().setExtendedDesc("Поставщик скрыт")
                    .setReasonKey("HIDDEN_SUPPLIER").setType(HidingReasonType.REASON),
                new HidingReasonDescription().setExtendedDesc("Поставщик скрыт")
                    .setReasonKey("HIDDEN_SUPPLIER_HIDDEN_SUPPLIER").setType(HidingReasonType.REASON_KEY),
                new HidingReasonDescription().setExtendedDesc("")
                    .setReasonKey("FEED_ERROR_SKK_PRICE_DIFF").setType(HidingReasonType.REASON_KEY),
                new HidingReasonDescription().setExtendedDesc("")
                    .setReasonKey("FEED_ERROR_49Q").setType(HidingReasonType.REASON_KEY)
            );
    }

    private List<HidingReasonDescription> getAllDescriptions() {
        List<HidingReasonDescription> result = new ArrayList<>();
        jdbcOperations.query("select * from msku.hiding_reason_description", rs -> {
            HidingReasonDescription hiding = new HidingReasonDescription()
                .setExtendedDesc(rs.getString("extended_desc"))
                .setReasonKey(rs.getString("reason_key"))
                .setType(HidingReasonType.valueOf(rs.getString("type")));

            result.add(hiding);
        });
        return result;
    }
}

package ru.yandex.market.core.feed.tanker;

import java.util.List;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.feed.supplier.tanker.MessageSource;
import ru.yandex.market.core.feed.supplier.tanker.SupplierTankerService;
import ru.yandex.market.core.indexer.model.IndexerError;
import ru.yandex.market.core.indexer.model.TranslatedIndexerError;
import ru.yandex.market.core.language.model.Language;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.partner.test.context.FunctionalTest;

/**
 * Тесты на {@link SupplierTankerService преобразование кодов индексатора}.
 *
 * @author fbokovikov
 */
@DbUnitDataSet(before = "SupplierTankerServiceTest.csv")
class SupplierTankerServiceTest extends FunctionalTest {

    private static final String _45G_DETAILS =
            "{\n" +
                    "  \"classifier_magic_id\":\"\",\n" +
                    "  \"posColumn\":0,\n" +
                    "  \"posLine\":7,\n" +
                    "  \"tagName\":\"shop-sku\",\n" +
                    "  \"code\":\"45G\",\n" +
                    "  \"offerId\":\"\",\n" +
                    "  \"groupId\":\"tagName=shop-sku;offerType=assortment\",\n" +
                    "  \"offerType\":\"assortment\"\n" +
                    "}";

    private static final String _396_DETAILS =
            "{\n" +
                    "  \"classifier_magic_id\":\"\",\n" +
                    "  \"posColumn\":0,\n" +
                    "  \"posLine\":7,\n" +
                    "  \"tagName\":\"pictures\",\n" +
                    "  \"code\":\"396\",\n" +
                    "  \"offerId\":\"\",\n" +
                    "  \"groupId\":\"tagName=pictures;offerType=assortment\",\n" +
                    "  \"offerType\":\"assortment\"\n" +
                    "}";

    @Autowired
    private SupplierTankerService supplierTankerService;

    @Autowired
    private EnvironmentService environmentService;

    /**
     * Обработка неизвестного кода индексатора.
     */
    @Test
    @DbUnitDataSet
    void testIndexerCodeNotFound() {
        environmentService.setValue("validation.new.error.representation", "false");
        List<TranslatedIndexerError> translated = supplierTankerService.translateFeedCheckerCodes(
                ImmutableList.of(new IndexerError.Builder().setPosition("1:1")
                        .setCode("45AA")
                        .setDetails("")
                        .setText("Forty-five-A-A")
                        .build()),
                MessageSource.INDEXER_YML,
                Language.RUSSIAN
        );
        ReflectionAssert.assertReflectionEquals(
                ImmutableList.of(new TranslatedIndexerError("1:1", "Forty-five-A-A")),
                translated
        );
    }

    /**
     * Обработка известных кодов индексатора + проверяется подстановка полей в шаблон.
     */
    @Test
    @DbUnitDataSet
    void testMbocCodesTranslating() {
        List<TranslatedIndexerError> translated = supplierTankerService.translateFeedCheckerCodes(
                ImmutableList.of(
                        new IndexerError.Builder().setPosition("0:0")
                                .setCode("mboc.error.msku-not-exists")
                                .setDetails("{\"skuId\":100136953859}")
                                .setTemplate("SKU {{skuId}} не существует.")
                                .setText("SKU {{skuId}} не существует.")
                                .build(),
                        new IndexerError.Builder().setPosition("0:1")
                                .setCode("mboc.error.msku-is-not-published")
                                .setDetails("{\"skuId\":100136953860}")
                                .setTemplate("SKU {{skuId}} не опубликован.")
                                .setText("SKU {{skuId}} не опубликован.")
                                .build()
                ),
                MessageSource.MBOC,
                Language.RUSSIAN
        );
        ReflectionAssert.assertReflectionEquals(
                ImmutableList.of(
                        new TranslatedIndexerError("0:0", "SKU 100136953859 не существует."),
                        new TranslatedIndexerError("0:1", "SKU 100136953860 не опубликован.")
                ),
                translated
        );
    }

    @Test
    @DisplayName("Перевод для шаблона, у которого в аргументах есть список значений")
    @DbUnitDataSet
    void testTranslationWithListArguments() {
        List<TranslatedIndexerError> translated = supplierTankerService.translateFeedCheckerCodes(
                List.of(
                        new IndexerError.Builder()
                                .setPosition("0:0")
                                .setCode("ir.partner_content.dcp.validation.groupId.sameParamsWithinBatch")
                                .setDetails("{\"groupId\":123,\"thisShopSku\":\"this_sku\"," +
                                        "\"otherShopSkus\":[\"sku_1\", \"sku_2\"]}")
                                .setTemplate("Внутри группы {{groupId}} у офера {{thisShopSku}} такой же набор " +
                                        "параметров, как и у офера/оферов {{#otherShopSkus}}{{.}}, {{/otherShopSkus}}")
                                .setText("default test")
                                .build()
                ),
                MessageSource.IR_CONTENT_API,
                Language.RUSSIAN
        );

        Assertions.assertThat(translated)
                .singleElement()
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .isEqualTo(new TranslatedIndexerError("0:0", "Внутри группы 123 у офера this_sku такой же набор " +
                        "параметров, как и у офера/оферов sku_1, sku_2,"));
    }

    /**
     * Обработка известных кодов индексатора + проверяется подстановка полей в шаблон.
     */
    @Test
    @DbUnitDataSet
    void testIndexerCodesTranslating() {
        environmentService.setValue("validation.new.error.representation", "false");
        List<TranslatedIndexerError> translated = supplierTankerService.translateFeedCheckerCodes(
                ImmutableList.of(
                        new IndexerError.Builder().setPosition("0:0")
                                .setCode("401")
                                .setDetails("{}")
                                .setText("Four-o-One")
                                .build(),
                        new IndexerError.Builder().setPosition("7:0")
                                .setCode("45A")
                                .setDetails("{}")
                                .setText("Forty-five-A")
                                .build(),
                        new IndexerError.Builder().setPosition("9:0")
                                .setCode("45G")
                                .setDetails(_45G_DETAILS)
                                .setText("Forty-five-G")
                                .build()
                ),
                MessageSource.INDEXER_YML,
                Language.RUSSIAN
        );
        ReflectionAssert.assertReflectionEquals(
                ImmutableList.of(
                        new TranslatedIndexerError("0:0", "Некорректная ссылка на прайс-лист"),
                        new TranslatedIndexerError("7:0", "Предложение отклонено"),
                        new TranslatedIndexerError("9:0", "Нет элемента <shop-sku>")
                ),
                translated
        );
    }


    @DisplayName("Если в шаблоне, загруженном из танкера есть ошибка, то не стоит падать. Просто взять defaultValue")
    @Test
    @DbUnitDataSet
    void translateError() {
        environmentService.setValue("validation.new.error.representation", "true");
        List<TranslatedIndexerError> translated = supplierTankerService.translateFeedCheckerCodes(
                ImmutableList.of(
                        new IndexerError.Builder().setPosition("0:0")
                                .setCode("401")
                                .setDetails("{}")
                                .setText("Four-o-One")
                                .build(),
                        new IndexerError.Builder().setPosition("9:0")
                                .setCode("45c")
                                .setDetails("")
                                .setText("Forty-five-c")
                                .build(),
                        new IndexerError.Builder().setPosition("10:0")
                                .setCode("45G")
                                .setDetails("{")
                                .setText("Forty-five-G")
                                .build(),
                        new IndexerError.Builder().setPosition("11:0")
                                .setCode("45A")
                                .setDetails("null")
                                .setText("Forty-five-A")
                                .build(),
                        new IndexerError.Builder().setPosition("12:0")
                                .setCode("26a")
                                .setDetails("null")
                                .setText("Twenty-six-a")
                                .build(),
                        new IndexerError.Builder().setPosition("13:0")
                                .setCode("396")
                                .setDetails(_396_DETAILS)
                                .setText("bla-bla")
                                .build()
                ),
                MessageSource.INDEXER_YML,
                Language.RUSSIAN
        );
        ReflectionAssert.assertReflectionEquals(
                ImmutableList.of(
                        new TranslatedIndexerError("0:0", "Некорректная ссылка на прайс-лист:Four-o-One"),
                        new TranslatedIndexerError("9:0", "Forty-five-c:Forty-five-c"),
                        new TranslatedIndexerError("10:0", "Forty-five-G:Forty-five-G"),
                        new TranslatedIndexerError("11:0", "Предложение отклонено:Forty-five-A"),
                        new TranslatedIndexerError("12:0", "Не указана цена товара:Twenty-six-a"),
                        new TranslatedIndexerError("13:0",
                                "Не заполнено обязательное поле:Заполните обязательное поле – «Ссылка на изображение»"
                        )
                ),
                translated
        );
    }

}

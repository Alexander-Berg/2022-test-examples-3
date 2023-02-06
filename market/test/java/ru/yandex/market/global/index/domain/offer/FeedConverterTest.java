package ru.yandex.market.global.index.domain.offer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.xmlunit.assertj3.XmlAssert;

import ru.yandex.market.global.index.BaseFunctionalTest;
import ru.yandex.market.global.index.domain.offer.model.CSVOffer2;
import ru.yandex.market.global.index.domain.offer.model.CSVParsingError;
import ru.yandex.market.global.index.domain.offer.model.CSVParsingResult;
import ru.yandex.market.global.index.domain.offer.model.CSVShopCategory;
import ru.yandex.mj.generated.server.model.AttributeDto;
import ru.yandex.mj.generated.server.model.AttributeType;
import ru.yandex.mj.generated.server.model.EnumOptionDto;
import ru.yandex.mj.generated.server.model.Locale;
import ru.yandex.mj.generated.server.model.LocalizedStringDto;
import ru.yandex.mj.generated.server.model.MarketCategoryDto;
import ru.yandex.mj.generated.server.model.ShopExportDto;

import static java.nio.charset.StandardCharsets.UTF_8;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FeedConverterTest extends BaseFunctionalTest {
    private static final List<AttributeDto> ATTRIBUTES = List.of(
            new AttributeDto()
                    .code("name")
                    .common(true)
                    .required(true)
                    .type(AttributeType.STRING),
            new AttributeDto()
                    .code("description")
                    .common(true)
                    .required(true)
                    .type(AttributeType.STRING),
            new AttributeDto()
                    .code("pictures")
                    .common(true)
                    .required(true)
                    .type(AttributeType.PICTURE),
            new AttributeDto()
                    .code("price")
                    .common(true)
                    .required(true)
                    .type(AttributeType.DECIMAL),
            new AttributeDto()
                    .code("weight")
                    .common(false)
                    .required(false)
                    .type(AttributeType.DECIMAL),
            new AttributeDto()
                    .code("volume")
                    .common(false)
                    .required(false)
                    .type(AttributeType.INTEGER),
            new AttributeDto()
                    .code("pieces")
                    .common(false)
                    .required(false)
                    .type(AttributeType.INTEGER),
            new AttributeDto()
                    .code("isAdult")
                    .common(false)
                    .required(false)
                    .type(AttributeType.BOOLEAN),
            new AttributeDto()
                    .code("measure")
                    .common(false)
                    .required(false)
                    .type(AttributeType.STRING),
            new AttributeDto()
                    .code("voltage")
                    .common(false)
                    .required(false)
                    .type(AttributeType.INTEGER),
            new AttributeDto()
                    .code("material")
                    .common(false)
                    .required(false)
                    .type(AttributeType.ENUM)
                    .options(List.of(
                            new EnumOptionDto()
                                    .value("gold")
                                    .title(List.of(new LocalizedStringDto().locale(Locale.EN).value("Golden"))),
                            new EnumOptionDto()
                                    .value("wood")
                                    .title(List.of(new LocalizedStringDto().locale(Locale.EN).value("Wooden")))
                    )),
            new AttributeDto()
                    .code("size")
                    .common(false)
                    .required(false)
                    .type(AttributeType.DECIMAL)
    );

    private static final long ELECTRONICS_ID = 1L;
    private static final long ALCOHOL_ID = 2L;

    private static final List<MarketCategoryDto> CATEGORIES = List.of(
            new MarketCategoryDto()
                    .id(ELECTRONICS_ID)
                    .parentId(null)
                    .code("electronics")
                    .title(List.of(new LocalizedStringDto().locale(Locale.EN).value("Electronics")))
                    .attributes(ATTRIBUTES),
            new MarketCategoryDto()
                    .id(ALCOHOL_ID)
                    .parentId(null)
                    .code("alcohol")
                    .title(List.of(new LocalizedStringDto().locale(Locale.EN).value("Alcohol")))
                    .attributes(ATTRIBUTES)
    );

    private static final RecursiveComparisonConfiguration RECURSIVE_COMPARISON_CONFIGURATION =
            RecursiveComparisonConfiguration.builder()
                    .withIgnoreCollectionOrder(true)
                    .withIgnoreAllExpectedNullFields(true)
                    .build();
    private final FeedConverter feedConverter;

    @Test
    public void testParseCsv() {
        CSVParsingResult parsingResult = feedConverter.parseCsv(
                readResourceAsStream("/offer/min-fields-feed2.csv"),
                CATEGORIES
        );

        Assertions.assertThat(parsingResult.getErrors())
                .isEmpty();

        Assertions.assertThat(parsingResult.getShopCategories())
                .usingRecursiveFieldByFieldElementComparator(RECURSIVE_COMPARISON_CONFIGURATION)
                .containsExactlyInAnyOrder(
                        new CSVShopCategory()
                                .setId(453188L)
                                .setName("smartphones"),
                        new CSVShopCategory()
                                .setId(258078L)
                                .setName("бетономешалки")
                );

        Assertions.assertThat(parsingResult.getOffers())
                .usingRecursiveFieldByFieldElementComparator(RECURSIVE_COMPARISON_CONFIGURATION)
                .containsExactlyInAnyOrder(
                        new CSVOffer2()
                                .setId("QQQ00012")
                                .setShopCategoryId(453188L)
                                .setMarketCategoryId(ELECTRONICS_ID)
                                .setAvailable(true)
                                .setAttributes(Map.of(
                                        "name", "bebebe",
                                        "description", "some cool model",
                                        "pictures", "https://drive.google.com/uc?export=download&id=123",
                                        "price", 22.22
                                )),
                        new CSVOffer2()
                                .setId("QQQ00013")
                                .setShopCategoryId(258078L)
                                .setMarketCategoryId(ELECTRONICS_ID)
                                .setAvailable(null)
                                .setAttributes(Map.of(
                                        "name", "bububu",
                                        "description", "some cool model",
                                        "pictures", "https://d3m9l0v76dty0.cloudfront" +
                                                ".net/system/photos/7661138/original/b2424f62f0bf5e6ea76d5c1502c881eb" +
                                                ".jpg",
                                        "price", 33.33
                                )),
                        new CSVOffer2()
                                .setId("QQQ00014")
                                .setShopCategoryId(453188L)
                                .setMarketCategoryId(ELECTRONICS_ID)
                                .setAvailable(false)
                                .setAttributes(Map.of(
                                        "name", "bababa",
                                        "description", "some cool model",
                                        "pictures", "https://drive.google.com/uc?export=download&id=123",
                                        "price", 44.44
                                )));
    }

    @Test
    public void testParseCsvWithoutAvailable() {
        CSVParsingResult parsingResult = feedConverter.parseCsv(
                readResourceAsStream("/offer/min-fields-feed2-without-available.csv"),
                CATEGORIES
        );

        Assertions.assertThat(parsingResult.getErrors())
                .isEmpty();

        Assertions.assertThat(parsingResult.getOffers().get(0)).usingRecursiveComparison()
                .isEqualTo(
                        new CSVOffer2()
                                .setId("QQQ00012")
                                .setShopCategoryId(453188L)
                                .setMarketCategoryId(ELECTRONICS_ID)
                                .setAvailable(null)
                                .setAttributes(Map.of(
                                        "name", "bebebe",
                                        "description", "some cool model",
                                        "pictures", "https://drive.google.com/uc?export=download&id=123",
                                        "price", 22.22
                                )));
    }

    @Test
    public void testCreateFeed() {
        CSVParsingResult parsingResult = feedConverter.parseCsv(
                readResourceAsStream("/offer/all-fields-feed2.csv"),
                CATEGORIES
        );
        String feed = feedConverter.createFeed(new ShopExportDto().name("Test shop"),
                parsingResult
        );
        XmlAssert.assertThat(feed).and(readResourceAsString("/offer/generated-feed2.xml"))
                .normalizeWhitespace()
                .areIdentical();
    }

    @Test
    public void testParseBrokenFieldsCsv() {
        CSVParsingResult result = feedConverter.parseCsv(
                readResourceAsStream("/offer/broken-fields-feed2.csv"),
                CATEGORIES
        );

        Assertions.assertThat(result.getErrors())
                .usingRecursiveFieldByFieldElementComparator(RECURSIVE_COMPARISON_CONFIGURATION)
                .containsExactlyInAnyOrder(
                        new CSVParsingError()
                                .setLine(2).setColumn("weight").setMessage("Incorrect value"),
                        new CSVParsingError()
                                .setLine(2).setColumn("volume").setMessage("Incorrect value"),
                        new CSVParsingError()
                                .setLine(2).setColumn("pieces").setMessage("Incorrect value")
                );
    }


    @Test
    public void testBrokenId() {
        CSVParsingResult result = feedConverter.parseCsv(
                readResourceAsStream("/offer/broken-id-feed.csv"),
                CATEGORIES
        );

        Assertions.assertThat(result.getErrors())
                .usingRecursiveFieldByFieldElementComparator(RECURSIVE_COMPARISON_CONFIGURATION)
                .containsExactlyInAnyOrder(
                        new CSVParsingError()
                                .setLine(2).setColumn("id")
                                .setMessage("Incorrect value БЕБЕБЕ. " +
                                        "May contain only english letters, digits, '_' and '-'  characters"
                                )
                );
    }

    @Test
    public void testRequiredFieldsCsv() {
        CSVParsingResult result = feedConverter.parseCsv(
                readResourceAsStream("/offer/no-required-fields-feed2.csv"),
                CATEGORIES
        );

        Assertions.assertThat(result.getErrors())
                .usingRecursiveFieldByFieldElementComparator(RECURSIVE_COMPARISON_CONFIGURATION)
                .containsExactlyInAnyOrder(
                        new CSVParsingError()
                                .setLine(2).setColumn("name").setMessage("Field is required but empty"),
                        new CSVParsingError()
                                .setLine(2).setColumn("description").setMessage("Field is required but empty"),
                        new CSVParsingError()
                                .setLine(2).setColumn("pictures").setMessage("Field is required but empty")
                );
    }


    @Test
    public void testRequiredHeaderCsv() {
        CSVParsingResult result = feedConverter.parseCsv(
                readResourceAsStream("/offer/no-required-header-feed.csv"),
                CATEGORIES
        );

        Assertions.assertThat(result.getErrors())
                .usingRecursiveFieldByFieldElementComparator(RECURSIVE_COMPARISON_CONFIGURATION)
                .containsExactlyInAnyOrder(
                        new CSVParsingError()
                                .setLine(1).setMessage("Required header id not found"),
                        new CSVParsingError()
                                .setLine(1).setMessage("Required header marketCategory not found")
                );
    }

    private static InputStream readResourceAsStream(String path) {
        return new ByteArrayInputStream(readResourceAsString(path).getBytes());
    }

    private static String readResourceAsString(String path) {
        //noinspection ConstantConditions
        return new Scanner(
                FeedConverterTest.class.getResourceAsStream(path), UTF_8
        ).useDelimiter("\\A").next();
    }

}

package ru.yandex.market.logistics.tarifficator.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.hamcrest.Description;
import org.hamcrest.StringDescription;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.common.util.collections.Triple;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.tarifficator.exception.DatasetGenerationException;
import ru.yandex.market.logistics.tarifficator.exception.RevisionItemGenerationException;
import ru.yandex.market.logistics.tarifficator.model.export.CalculatorTariff;
import ru.yandex.market.logistics.tarifficator.service.export.DeliveryCalculatorDatasetGenerator;
import ru.yandex.market.logistics.test.integration.matchers.XmlMatcher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.tarifficator.util.TestUtils.PARAMETERIZED_TEST_DEFAULT_NAME;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@Transactional
@DisplayName("Интеграционный тест генерации выгрузок для Калькулятора Доставки")
@DatabaseSetup("/tags/tags.xml")
class DeliveryCalculatorDatasetGeneratorTest extends AbstractContextualTest {
    private static final Instant TIME_11_AM = Instant.parse("2019-08-12T11:00:00.00Z");

    @Autowired
    private MappingJackson2XmlHttpMessageConverter calculatorTariffConverter;
    @Autowired
    private DeliveryCalculatorDatasetGenerator deliveryCalculatorDatasetGenerator;
    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private FeatureProperties featureProperties;

    @BeforeEach
    void beforeEach() {
        clock.setFixed(TIME_11_AM, ZoneOffset.UTC);
        mockLmsClient();
    }

    @AfterEach
    void after() {
        clock.clearFixed();
    }

    @DisplayName("Успешная генерация прайс-листа")
    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    @MethodSource("priceListProvider")
    @DatabaseSetup({
        "/pickup-points/pickup-points.xml",
        "/tms/revision/before/regenerate-without-any-changes.xml",
        "/service/calculator-dataset/db/pickup-tariff.xml",
    })
    @DatabaseSetup(
        value = {
            "/tags/relations/add-market-delivery-to-tariff-1.xml",
            "/service/calculator-dataset/db/post-tariff-locality-true.xml",
            "/service/calculator-dataset/db/market-courier-tariff.xml",
        },
        type = DatabaseOperation.INSERT
    )
    void generateSuccess(
        @SuppressWarnings("unused") String caseName,
        long priceListId,
        String expectedDatasetPath
    ) {
        byte[] datasetData = deliveryCalculatorDatasetGenerator.generate(priceListId);
        softlyAssertThatXmlEquals(
            extractFileContent(expectedDatasetPath),
            new String(datasetData, StandardCharsets.UTF_8)
        );
    }

    @Test
    @DisplayName("Успешная генерация прайс-листа для самовывоза включая неактивную точку")
    @DatabaseSetup({
        "/pickup-points/pickup-points.xml",
        "/tms/revision/before/regenerate-without-any-changes.xml",
        "/service/calculator-dataset/db/pickup-tariff.xml",
    })
    @DatabaseSetup(
        value = {
            "/tags/relations/add-market-delivery-to-tariff-1.xml",
            "/service/calculator-dataset/db/post-tariff-locality-true.xml",
        },
        type = DatabaseOperation.INSERT
    )
    void generateSuccessWithInactivePoints() {
        when(featureProperties.isPickupPointsFetchInactiveByDefault()).thenReturn(true);
        byte[] datasetData = deliveryCalculatorDatasetGenerator.generate(300L);
        softlyAssertThatXmlEquals(
            extractFileContent(
                "service/calculator-dataset/delivery-calculator-dataset-for-price-list-300-with-inactive_points.xml"
            ),
            new String(datasetData, StandardCharsets.UTF_8)
        );
    }

    @Test
    @DisplayName("Направления без ПВЗ для ПВЗ-тарифов не учитываются при генерации прайс-листа")
    @DatabaseSetup("/pickup-points/pickup-points.xml")
    @DatabaseSetup("/service/calculator-dataset/db/pickup-tariff.xml")
    @DatabaseSetup(
        value = "/service/calculator-dataset/db/extra_directions.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/service/calculator-dataset/db/revision_item_xml_building_history_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void filterDirectionsWithoutPickupPoints() {
        byte[] datasetData = deliveryCalculatorDatasetGenerator.generate(300L);
        softlyAssertThatXmlEquals(
            extractFileContent(
                "service/calculator-dataset/delivery-calculator-dataset-for-price-list-300-with-extra-directions.xml"
            ),
            new String(datasetData, StandardCharsets.UTF_8)
        );
    }

    @Test
    @DisplayName("Ошибка генерации прайс-листа при отсутствии направлений")
    @DatabaseSetup("/service/calculator-dataset/db/pickup-tariff.xml")
    @ExpectedDatabase(
        value = "/service/calculator-dataset/db/revision_item_xml_building_history_fail_no_directions.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void generateErrorDirectionsAreEmpty() {
        softly.assertThatThrownBy(() -> deliveryCalculatorDatasetGenerator.generate(300L))
            .isInstanceOf(DatasetGenerationException.class)
            .hasMessage("Cannot generate dataset for price-list 300")
            .hasCause(new RevisionItemGenerationException("Directions are empty", false));
    }

    @Test
    @DisplayName("Не смогли сгенерировать прайс, потому что нет точек для партнёра у курьерского тарифа")
    @DatabaseSetup(
        value = {
            "/service/calculator-dataset/db/market-courier-tariff.xml",
        },
        type = DatabaseOperation.INSERT
    )
    void generateErrorDestinationPartnersEmpty() {
        softly.assertThatThrownBy(
            () -> deliveryCalculatorDatasetGenerator.generate(600L)
        )
            .isInstanceOf(DatasetGenerationException.class)
            .hasMessage("Cannot generate dataset for price-list 600");
    }

    @Test
    @DisplayName("Ошибка генерации XML")
    @DatabaseSetup("/tms/revision/before/regenerate-without-any-changes.xml")
    @ExpectedDatabase(
        value = "/service/calculator-dataset/db/revision_item_xml_building_history_fail_generation_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void generateError() throws JsonProcessingException {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        doReturn(objectMapper).when(calculatorTariffConverter).getObjectMapper();
        doThrow(new JsonGenerationException("Failed", mock(JsonGenerator.class)))
            .when(objectMapper).writeValueAsBytes(any(CalculatorTariff.class));

        softly.assertThatThrownBy(() -> deliveryCalculatorDatasetGenerator.generate(1L))
            .isInstanceOf(DatasetGenerationException.class)
            .hasMessage("Cannot generate dataset for price-list 1");
    }

    private void softlyAssertThatXmlEquals(String expectedDataset, String dataset) {
        Description description = new StringDescription();
        XmlMatcher matcher = new XmlMatcher(expectedDataset);
        matcher.describeTo(description);

        softly.assertThat(dataset)
            .matches(matcher::matches, buildErrorMessage(dataset, matcher, description));
    }

    @Nonnull
    private static String buildErrorMessage(String dataset, XmlMatcher matcher, Description description) {
        matcher.describeMismatch(dataset, description);
        return description.toString();
    }

    @Nonnull
    private static Stream<Arguments> priceListProvider() {
        return Stream.of(
            Triple.of(
                "курьерской доставки",
                2L,
                "service/calculator-dataset/delivery-calculator-dataset-for-price-list-2.xml"
            ),
            Triple.of(
                "курьерской доставки, для своей СД магазина",
                100L,
                "service/calculator-dataset/delivery-calculator-dataset-for-price-list-100.xml"
            ),
            Triple.of(
                "самовывоза c нестандартным scale и нецелочисленными весовыми брейками",
                300L,
                "service/calculator-dataset/delivery-calculator-dataset-for-price-list-300.xml"
            ),
            Triple.of(
                "непубличного прайс-листа курьерской доставки",
                1L,
                "service/calculator-dataset/delivery-calculator-dataset-for-price-list-1.xml"
            ),
            Triple.of(
                "Почта России, тариф с вложенными точкам",
                400L,
                "service/calculator-dataset/delivery-calculator-dataset-for-price-list-400.xml"
            ),
            Triple.of(
                "Почта России, тариф без вложенных точек",
                401L,
                "service/calculator-dataset/delivery-calculator-dataset-for-price-list-401.xml"
            ),
            Triple.of(
                "Курьерский тариф, тариф с вложенными партнёрами",
                500L,
                "service/calculator-dataset/delivery-calculator-dataset-for-price-list-500.xml"
            ),
            Triple.of(
                "Курьерский тариф, тариф с вложенными партнёрами, простым прайсом и localityOnly = true",
                501L,
                "service/calculator-dataset/delivery-calculator-dataset-for-price-list-501.xml"
            ),
            Triple.of(
                "Курьерский тариф, тариф с вложенными партнёрами простым прайсом и localityOnly = false",
                502L,
                "service/calculator-dataset/delivery-calculator-dataset-for-price-list-502.xml"
            )
        ).map(triple -> Arguments.of(triple.first, triple.second, triple.third));
    }

    private void mockLmsClient() {
        when(lmsClient.getPartner(any(Long.class))).thenAnswer(
            invocation -> {
                Long partnerId = invocation.getArgument(0);
                return Optional.of(
                    PartnerResponse.newBuilder()
                        .id(partnerId)
                        .name("partner_" + partnerId)
                        .build()
                );
            }
        );
    }
}

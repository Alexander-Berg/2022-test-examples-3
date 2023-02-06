package ru.yandex.market.ff.integration;

import java.util.List;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.Multimap;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.DocumentType;
import ru.yandex.market.ff.grid.model.cell.DefaultGridCell;
import ru.yandex.market.ff.grid.model.cell.GridCell;
import ru.yandex.market.ff.grid.model.grid.Grid;
import ru.yandex.market.ff.grid.reader.GridReader;
import ru.yandex.market.ff.grid.validation.GridValidator;
import ru.yandex.market.ff.grid.validation.Violation;
import ru.yandex.market.ff.grid.validation.ViolationsContainer;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author kotovdv 02/08/2017.
 */
@SuppressWarnings("AvoidEscapedUnicodeCharacters")
public abstract class AbstractGridValidatorIntegrationTest extends IntegrationTest {

    private static final String VALID = "supply/valid";
    private static final String INVALID_AMOUNT = "supply/invalid_amount";
    private static final String MISSING_COLUMN = "supply/missing_column";
    private static final String OPTIONAL_COLUMN_NOT_FILLED = "supply/optional_column_not_filled";
    private static final String INVALID_SKU = "supply/invalid_sku";
    private static final String INVALID_BARCODE = "supply/invalid_barcode";
    private static final String NO_ROWS = "supply/no_rows";
    private static final String TOO_MANY_ROWS = "supply/too_many_rows";
    private static final String DUPLICATED_SKU = "supply/duplicated_sku";
    private static final String INVALID_CHAR = "supply/invalid_char";

    protected final ConcreteEnvironmentParamService paramService = Mockito.mock(ConcreteEnvironmentParamService.class);

    @Autowired
    private GridValidator gridValidator;

    protected abstract GridReader getGridReader();

    protected abstract String getExtension();

    private String fullFileName(String name) {
        return String.format("%s.%s", name, getExtension());
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void testValidateCorrectShortSupply() {
        ViolationsContainer container = getViolationContainer(VALID);

        assertThat(container.isEmpty())
                .as("Assert that violation container is empty")
                .isTrue();
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void testValidateGridWithMissingColumn() {
        ViolationsContainer container = getViolationContainer(MISSING_COLUMN);

        assertThat(container.getCellViolations().isEmpty())
                .as("Assert that there are no cell violations when column is missing")
                .isEqualTo(true);

        assertThat(container.getColumnViolations().keySet())
                .as("Assert that missing columns violations contain 'Количество поставки' (idx=2)")
                .containsExactly(3, 4, 5);

        assertThat(container.getColumnViolations().values().stream()
                .map(Violation::getMessage).collect(Collectors.toList()))
                .containsExactly(
                        "Названия или порядок полей в загруженном файле не соответствуют шаблону: " +
                                "'Количество товаров в поставке'",
                        "Названия или порядок полей в загруженном файле не соответствуют шаблону: " +
                                "'Объявленная ценность одного товара, руб.'",
                        "Названия или порядок полей в загруженном файле не соответствуют шаблону: " +
                                "'Комментарий для склада'");
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void testAmountOfSupplyIsInvalid() {
        ViolationsContainer container = getViolationContainer(INVALID_AMOUNT);

        Multimap<GridCell, Violation> violations = container.getCellViolations();

        GridCell firstErrorCell = new DefaultGridCell(0, 3, "first");
        GridCell secondErrorCell = new DefaultGridCell(1, 3, "second");

        assertThat(violations.keySet())
                .as("Assert that violations contain exactly two error cells for column 'Количество поставки'")
                .containsExactlyInAnyOrder(firstErrorCell, secondErrorCell);
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void testValidateWithOptionalColumnNotFilled() {
        ViolationsContainer container = getViolationContainer(OPTIONAL_COLUMN_NOT_FILLED);

        assertThat(container.isEmpty())
                .as("Assert that optional column does not generate any violations")
                .isTrue();
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void testSkuIsInvalid() {
        ViolationsContainer container = getViolationContainer(INVALID_SKU);

        Multimap<GridCell, Violation> violations = container.getCellViolations();

        GridCell errorCell = new DefaultGridCell(0, 0, ";AAAAAAAAAAAAAAAAAA");

        assertThat(violations.keySet())
                .as("Assert that violations contain exactly two error cells for column 'Артикул'")
                .contains(errorCell);
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void testBarcodeIsInvalid() {
        ViolationsContainer container = getViolationContainer(INVALID_BARCODE);

        Multimap<GridCell, Violation> violations = container.getCellViolations();

        GridCell errorCell = new DefaultGridCell(0, 2, "123456679012,_.-3846578491843");

        assertThat(violations.keySet())
                .as("Assert that violations contain exactly two error cells for column 'Штрихкоды'")
                .contains(errorCell);
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void testNoRows() {
        ViolationsContainer container = getViolationContainer(NO_ROWS);
        final List<Violation> commonViolations = container.getCommonViolations();

        assertThat(commonViolations.stream().map(Violation::getMessage).collect(Collectors.toList()))
                .as("Assert that violation contains error message about invalid rows count")
                .contains("Некорректное количество строк. Допустимое количество строк: 1 - 10000");
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void testTooManyRows() {
        ViolationsContainer container = getViolationContainer(TOO_MANY_ROWS);
        final List<Violation> commonViolations = container.getCommonViolations();

        assertThat(commonViolations.stream().map(Violation::getMessage).collect(Collectors.toList()))
                .as("Assert that violation contains error message about invalid rows count")
                .contains("Некорректное количество строк. Допустимое количество строк: 1 - 10000");
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void testDuplicatedSku() {
        ViolationsContainer container = getViolationContainer(DUPLICATED_SKU);
        final Multimap<GridCell, Violation> cellViolations = container.getCellViolations();

        GridCell errorCell1 = new DefaultGridCell(1, 0, "BBBBBBBBBBBBBBBBBB");
        GridCell errorCell2 = new DefaultGridCell(2, 0, "BBBBBBBBBBBBBBBBBB");
        GridCell errorCell3 = new DefaultGridCell(3, 0, "CCCCCCCCCCCCCCCCCC");
        GridCell errorCell4 = new DefaultGridCell(4, 0, "CCCCCCCCCCCCCCCCCC");

        assertThat(cellViolations.keySet())
                .as("Assert that violations contain duplicated SKU errors")
                .contains(errorCell1, errorCell2, errorCell3, errorCell4);
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void testInvalidChar() {
        ViolationsContainer container = getViolationContainer(INVALID_CHAR);

        Multimap<GridCell, Violation> violations = container.getCellViolations();

        GridCell errorCell = new DefaultGridCell(0, 1, "Росмэн.Все-все для малышей. Гуси,гуси,га-га-га!\u0007Потешки");

        assertThat(violations.keySet())
                .as("Assert that violations contain exactly one error cells for column 'Название товара'")
                .contains(errorCell);
        assertThat(violations.get(errorCell).iterator().next().getMessage())
                .isEqualTo("Значение содержит недопустимые символы");
    }


    private ViolationsContainer getViolationContainer(final String resource) {
        final String fullName = fullFileName(resource);
        final Grid grid = getGridReader().read(ClassLoader.getSystemResourceAsStream(fullName));
        return gridValidator.validate(DocumentType.SUPPLY, grid).getViolationsContainer();
    }

}

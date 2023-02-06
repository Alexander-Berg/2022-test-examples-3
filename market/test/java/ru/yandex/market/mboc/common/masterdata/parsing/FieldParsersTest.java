package ru.yandex.market.mboc.common.masterdata.parsing;

import java.util.List;

import org.junit.Test;

import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.services.converter.models.OffersParseException;
import ru.yandex.market.mboc.common.services.excel.ExcelHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author jkt on 02.10.18.
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class FieldParsersTest {

    @Test
    public void whenCountriesContainsUnnecessaryWhiteSpacesShouldTrim() {
        List<String> countries = FieldParsers.asManufacturerCountry(" Китай, Россия ");
        assertThat(countries).containsExactlyInAnyOrder("Китай", "Россия");
    }

    @Test
    public void whenCountryAliasIsProvidedShouldReplaceByRealCountryName() {
        List<String> countries = FieldParsers.asManufacturerCountry("Республика Корея");
        assertThat(countries).containsExactlyInAnyOrder("Южная Корея");
    }

    @Test
    public void whenParsingCountryShouldParseCaseInsensitive() {
        List<String> countries = FieldParsers.asManufacturerCountry("РЕСПУБЛИКА Корея, КИТАЙ");
        assertThat(countries).containsExactlyInAnyOrder("Южная Корея", "Китай");
    }

    @Test
    public void whenCountryIsInvalidShouldThrowException() {
        assertThatThrownBy(() -> FieldParsers.asManufacturerCountry("INVALID COUNTRY"))
            .isInstanceOf(OffersParseException.class);
    }

    @Test
    public void whenSplitsByCommaListHandlesWhitespaces() {
        List<String> countries = FieldParsers.asSplitByCommaList("один,два, три ,четыре , \n пять");
        assertThat(countries).containsExactly("один", "два", "три", "четыре", "пять");
    }

    @Test
    public void whenParsingPeriodsShouldDropUnlimitedPeriodAliases() {
        Integer period = FieldParsers.asPeriodDays("9999");
        assertThat(period).isNull();
    }

    @Test
    public void whenParsingWithoutWhitespacesShouldRemoveAll() {
        assertSoftly(softly -> {
            softly.assertThat(FieldParsers.withoutWhitespaces(" ")).isEqualTo("");
            softly.assertThat(FieldParsers.withoutWhitespaces(" 1 h k_ ")).isEqualTo("1hk_");
        });
    }

    @Test
    public void whenParsingCommentsShouldTrimAndMakeFirstLetterLowercase() {
        String comment = FieldParsers.asComment(" Тестовый Комментарий!  ");
        assertThat(comment).isEqualTo("тестовый Комментарий!");
    }

    @Test
    public void testNoConversion() {
        var tu = FieldParsers.asTransportUnit("");
        assertThat(tu.hasQuantityInPack()).isFalse();
        assertThat(tu.hasTransportUnitSize()).isFalse();
    }

    @Test
    public void testInvalidTUS() {
        var tu1 = "/24";
        var tu2 = "ab,c/24";
        var tu3 = "3.14";
        List.of(tu1, tu2, tu3).forEach(tu -> {
            try {
                FieldParsers.asTransportUnit(tu);
                fail("Should throw OffersParseException");
            } catch (Exception ex) {
                assertTrue(ex instanceof OffersParseException);
                var offerException = (OffersParseException) ex;
                assertThat(offerException.getErrorInfoList().get(0)).isEqualTo(MbocErrors.get().excelValueMustBeNumber(
                    ExcelHeaders.TRANSPORT_UNIT_SIZE.getTitle(), tu));
            }
        });
    }

    @Test
    public void testInvalidQIP() {
        var tu1 = "1/";
        var tu2 = "1/ab.c";
        var tu3 = "1/3.14";
        List.of(tu1, tu2, tu3).forEach(tu -> {
            try {
                FieldParsers.asTransportUnit(tu);
                fail("Should throw OffersParseException");
            } catch (Exception ex) {
                assertTrue(ex instanceof OffersParseException);
                var offerException = (OffersParseException) ex;
                assertThat(offerException.getErrorInfoList().get(0)).isEqualTo(MbocErrors.get().excelValueMustBeNumber(
                    ExcelHeaders.TRANSPORT_UNIT_SIZE.getTitle(), tu));
            }
        });
    }

    @Test
    public void testValidTUS() {
        var tu1 = FieldParsers.asTransportUnit("1");
        var tu2 = FieldParsers.asTransportUnit("1/40");
        var tu3 = FieldParsers.asTransportUnit(" 1 ");
        List.of(tu1, tu2, tu3).forEach(tu -> {
            assertThat(tu.hasTransportUnitSize()).isTrue();
            assertThat(tu.getTransportUnitSize()).isEqualTo(1);
        });
    }

    @Test
    public void testValidQIP() {
        var tu1 = FieldParsers.asTransportUnit("5/40");
        var tu2 = FieldParsers.asTransportUnit("1/40");
        var tu3 = FieldParsers.asTransportUnit(" 1/40 ");
        List.of(tu1, tu2, tu3).forEach(tu -> {
            assertThat(tu.hasQuantityInPack()).isTrue();
            assertThat(tu.getQuantityInPack()).isEqualTo(40);
        });
    }

    @Test
    public void testValidBoth() {
        var tu = FieldParsers.asTransportUnit("15/30");
        assertThat(tu.hasTransportUnitSize()).isTrue();
        assertThat(tu.getTransportUnitSize()).isEqualTo(15);
        assertThat(tu.hasQuantityInPack()).isTrue();
        assertThat(tu.getQuantityInPack()).isEqualTo(30);
    }
}

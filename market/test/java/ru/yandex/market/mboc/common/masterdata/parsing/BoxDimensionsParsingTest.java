package ru.yandex.market.mboc.common.masterdata.parsing;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mboc.common.masterdata.model.BoxDimensionsInUm;
import ru.yandex.market.mboc.common.services.converter.models.OffersParseException;

/**
 * @author dmserebr
 * @date 15/10/2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class BoxDimensionsParsingTest {
    @Test
    public void parseWeightSuccess() {
        Assertions.assertThat(BoxDimensionsParser.asWeightInMg("10")).isEqualTo(10000000L);
        Assertions.assertThat(BoxDimensionsParser.asWeightInMg("0.02")).isEqualTo(20000L);
        Assertions.assertThat(BoxDimensionsParser.asWeightInMg("0.00019")).isEqualTo(190L);
        Assertions.assertThat(BoxDimensionsParser.asWeightInMg(" 7 ")).isEqualTo(7000000L);
        Assertions.assertThat(BoxDimensionsParser.asWeightInMg("0")).isEqualTo(0L);
        Assertions.assertThat(BoxDimensionsParser.asWeightInMg("")).isNull();
    }

    @Test
    public void parseWeightWithException() {
        try {
            BoxDimensionsParser.asWeightInMg("test");
        } catch (OffersParseException ex) {
            Assertions.assertThat(ex.toString()).endsWith("Значение 'test' для колонки '' должно быть числом");
        }
    }

    @Test
    public void parseDimensionsSuccess() {
        Assertions.assertThat(BoxDimensionsParser.asBoxDimensionsInUm("20/30/40"))
            .isEqualTo(new BoxDimensionsInUm(200000L, 300000L, 400000L));
        Assertions.assertThat(BoxDimensionsParser.asBoxDimensionsInUm("0.1/0.99/20.1 "))
            .isEqualTo(new BoxDimensionsInUm(1000L, 9900L, 201000L));
        Assertions.assertThat(BoxDimensionsParser.asBoxDimensionsInUm("0/0/0"))
            .isEqualTo(new BoxDimensionsInUm(0L, 0L, 0L));
        Assertions.assertThat(BoxDimensionsParser.asBoxDimensionsInUm("3 / 4 / 5"))
            .isEqualTo(new BoxDimensionsInUm(30000L, 40000L, 50000L));
        Assertions.assertThat(BoxDimensionsParser.asBoxDimensionsInUm("")).isNull();
    }

    @Test
    public void parseDimensionsWithException() {
        try {
            BoxDimensionsParser.asBoxDimensionsInUm("test");
        } catch (OffersParseException ex) {
            Assertions.assertThat(ex.toString()).endsWith(
                "Значение 'test' в колонке '' не соответствует требуемому формату");
        }

        try {
            BoxDimensionsParser.asBoxDimensionsInUm("2//4");
        } catch (OffersParseException ex) {
            Assertions.assertThat(ex.toString()).endsWith(
                "Значение '2//4' в колонке '' не соответствует требуемому формату");
        }

        try {
            BoxDimensionsParser.asBoxDimensionsInUm("2.3/4.2");
        } catch (OffersParseException ex) {
            Assertions.assertThat(ex.toString()).endsWith(
                "Значение '2.3/4.2' в колонке '' не соответствует требуемому формату");
        }

        try {
            BoxDimensionsParser.asBoxDimensionsInUm("2.3/78.6/3/1");
        } catch (OffersParseException ex) {
            Assertions.assertThat(ex.toString()).endsWith(
                "Значение '2.3/78.6/3/1' в колонке '' не соответствует требуемому формату");
        }
    }

    @Test
    public void testDecimalCommaTolerance() {
        Assertions.assertThat(BoxDimensionsParser.asWeightInMg("10,4")).isEqualTo(10400000L);
        Assertions.assertThat(BoxDimensionsParser.asBoxDimensionsInUm("0,1/0,99/20,1 "))
            .isEqualTo(new BoxDimensionsInUm(1000L, 9900L, 201000L));
    }
}

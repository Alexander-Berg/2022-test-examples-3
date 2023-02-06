package ru.yandex.market;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import ru.yandex.market.identifier.Identifier;
import ru.yandex.market.identifier.rules.Err;
import ru.yandex.market.identifier.rules.Result;
import ru.yandex.market.identifier.rules.Validator;

import static java.util.function.Predicate.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.identifier.TypeOfIdentity.CIS_FULL;
import static ru.yandex.market.identifier.TypeOfIdentity.CIS_SHORT;
import static ru.yandex.market.identifier.TypeOfIdentity.IMEI;
import static ru.yandex.market.identifier.TypeOfIdentity.SN;

public class ValidatorTest {

    private final Validator validator = new Validator();

    @Test
    void goodDifferentCisList() {
        var allMatch = Stream.of(
                        "0104601662000016215RNef*\u001d93B0Ik",
                        "01046056740000192153E4I:ndGuD4B\u001d93rD+D",
                        "0104650194496408215'4iRsB_JcDQ-\u001d91EE07\u001d" +
                                "929CmJoU45/CPoiJh7p7ajYJWdze5wgGIQJsxmw9fqurc=",
                        "0104600840269047215dU.W/a'4w/zRVeFJe(,\u001d91EE06\u001d" +
                                "92JZmRp7gT5Rhy7BQjaDAJI8bh1gef4GrJMr/hNKfKqB8=",
                        "010463011132146421V!VeVCzqYrNi_\u001d910098\u001d" +
                                "92rKl1F4poDIm7lNxYpopchwRkhKfjPSZtyGRFb17Zk2x1MxujYHTQxiZTBjn7UV6AJ" +
                                "+qcTKgTbA1SbHRPH5fkcw=="
                )
                .map(it -> Identifier.of(CIS_FULL, it))
                .map(validator::validate)
                .allMatch(Result::isOk);
        assertTrue(allMatch);
    }

    @Test
    void goodDifferentTypes() {
        var allMatch = Stream.of(
                        Identifier.of(CIS_SHORT, "0104601662000016215RNef*"),
                        Identifier.of(IMEI, "356586864559339"),
                        Identifier.of(SN, "SDX3H5SG5N73D"),
                        Identifier.of(CIS_FULL, "0104601662000016215RNef*\u001d93B0Ik")
                )
                .map(validator::validate)
                .allMatch(Result::isOk);
        assertTrue(allMatch);
    }

    @Test
    void identifierListWithOneError() {
        var failure = Stream.of(
                        Identifier.of(CIS_SHORT, "0104601662000016215RNef*"),
                        Identifier.of(IMEI, "356586864559339"),
                        Identifier.of(SN, "SDX3H5SG5N73D"),
                        Identifier.of(CIS_FULL, "")
                )
                .map(validator::validate)
                .filter(not(Result::isOk))
                .findAny();
        assertTrue(failure.isPresent());
        assertEquals(CIS_FULL, failure.orElseThrow().getIdentifier().getType());
    }

    @Test
    void goodCisFull() {
        var value = "0104650194495494215+il=lIKPb(aH\u001d91EE07\u001d92IJ+tVyHSg6PCWpjud5XqGHYmAbvAn8qA3OCkQX+QW1c=";
        var result = validator.validate(Identifier.of(CIS_FULL, value));
        assertTrue(result.isOk());
        assertTrue(result.getErr().isEmpty());
    }

    @Test
    void goodCisShort() {
        var result = validator.validate(Identifier.of(CIS_SHORT, "0104650194495494215+il=lIKPb(aH"));
        assertTrue(result.isOk());
        assertTrue(result.getErr().isEmpty());
    }

    @Test
    void goodIMEI() {
        var result = validator.validate(Identifier.of(IMEI, "017678219968983"));
        assertTrue(result.isOk());
        assertTrue(result.getErr().isEmpty());
    }

    @Test
    void goodSN() {
        var result = validator.validate(Identifier.of(SN, "S5GCNS0R701792"));
        assertTrue(result.isOk());
        assertTrue(result.getErr().isEmpty());
    }

    @Test
    void regexErrorOfCisFull() {
        var result = validator.validate(Identifier.of(CIS_FULL, "0104650194495494215+il=lIKPb(aH"));
        assertFalse(result.isOk());
        assertTrue(result.getErr().isPresent());
        assertEquals(Err.REGEX, result.getErr().orElseThrow());
    }

    @Test
    void cisWithPrefixSuffix() {
        var value = "\u001d0104650194495494215+il=lIKPb(aH\u001d91EE07\u001d" +
                "92IJ+tVyHSg6PCWpjud5XqGHYmAbvAn8qA3OCkQX+QW1c=\u001d";
        var result = validator.validate(Identifier.of(CIS_FULL, value));
        assertTrue(result.isOk(), result.getErr().toString());
        assertTrue(result.getErr().isEmpty());
    }

    @Test
    void cisCryptoInMiddle() {
        var value = "0103041094787443215Qbag!\u001d93Zjqw\u001d3103000353";
        var result = validator.validate(Identifier.of(CIS_FULL, value));
        assertTrue(result.isOk(), result.getErr().toString());
        assertTrue(result.getErr().isEmpty());
    }

    @Test
    void cisTailMissing() {
        var value = "0108411061948903215)E+Jv6NN<Od)\u001d91EE06\u001d";
        var result = validator.validate(Identifier.of(CIS_FULL, value));
        assertFalse(result.isOk());
        assertTrue(result.getErr().isPresent());
        assertEquals(Err.CIS_TAIL_BASE64, result.getErr().orElseThrow());
    }

    @Test
    void cisTailIsNotBase64() {
        var value = "0104650194496408215'4iRsB_JcDQ-\u001d91EE07\u001d92.a1";
        var result = validator.validate(Identifier.of(CIS_FULL, value));
        assertFalse(result.isOk());
        assertTrue(result.getErr().isPresent());
        assertEquals(Err.CIS_TAIL_BASE64, result.getErr().orElseThrow());
    }

    @Test
    void cisRepeated() {
        var value = "0104601662000016215RNef*\u001d93B0Ik0104601662000016215RNef*\u001d93B0Ik";
        var result = validator.validate(Identifier.of(CIS_FULL, value));
        assertFalse(result.isOk());
        assertTrue(result.getErr().isPresent());
        assertEquals(Err.CIS_REPEATED, result.getErr().orElseThrow());
    }

    @Test
    void cisCaseInsensitive() {
        var value = "0104650194496408215'4IRSB_JCDQ-\u001d91EE07\u001d929CMJOU45/CPOIJH7P7AJYJWDZE5WGGIQJSXMW9FQURC=";
        var result = validator.validate(Identifier.of(CIS_FULL, value));
        assertFalse(result.isOk());
        assertTrue(result.getErr().isPresent());
        assertEquals(Err.CIS_CASE_INSENSITIVE, result.getErr().orElseThrow());

        assertTrue(validator.validate(Identifier.of(CIS_FULL, "0104601662000016215RNEF*\u001d93B0IK")).isOk());
    }

    @Test
    void regexErrorOfCisShort() {
        var result = validator.validate(Identifier.of(CIS_SHORT, "123"));
        assertFalse(result.isOk());
        assertTrue(result.getErr().isPresent());
        assertEquals(Err.REGEX, result.getErr().orElseThrow());
    }

    @Test
    void regexErrorOfIMEI() {
        var result = validator.validate(Identifier.of(IMEI, "1111"));
        assertFalse(result.isOk());
        assertTrue(result.getErr().isPresent());
        assertEquals(Err.REGEX, result.getErr().orElseThrow());
    }

    @Test
    void mod10ErrorOfIMEI() {
        var result = validator.validate(Identifier.of(IMEI, "351014294139313"));
        assertFalse(result.isOk());
        assertTrue(result.getErr().isPresent());
        assertEquals(Err.CHECKSUM, result.getErr().orElseThrow());
    }

    @Test
    void regexErrorOfSN() {
        var result = validator.validate(Identifier.of(SN, "ABC"));
        assertFalse(result.isOk());
        assertTrue(result.getErr().isPresent());
        assertEquals(Err.REGEX, result.getErr().orElseThrow());
    }

    @Test
    void snIsLikeIMEI() {
        var result = validator.validate(Identifier.of(SN, "351014294139312"));
        assertFalse(result.isOk());
        assertTrue(result.getErr().isPresent());
        assertEquals(Err.SN_AS_IMEI, result.getErr().orElseThrow());
    }

    @Test
    void snNumeric() {
        var result = validator.validate(Identifier.of(SN, "1234567890"));
        assertFalse(result.isOk());
        assertTrue(result.getErr().isPresent());
        assertEquals(Err.SN_NUMERIC, result.getErr().orElseThrow());
    }

    @Test
    void nullSafety() {
        var result = validator.validate(null);
        assertNotNull(result);
        assertFalse(result.isOk());
        var err = result.getErr().orElseThrow();
        assertEquals(Err.NULL_IDENTIFIER, err);
    }

    @Test
    void furCis() {
        var result = validator.validate(Identifier.of(CIS_SHORT, "RU-430301-AAA0020659"));
        assertTrue(result.isOk());
        assertTrue(result.getErr().isEmpty());
    }

    @Test
    void furCisIsNotFull() {
        var result = validator.validate(Identifier.of(CIS_FULL, "RU-430301-AAA0020659"));
        assertFalse(result.isOk());
        assertTrue(result.getErr().isPresent());
        assertEquals(Err.CIS_INCORRECT_GTIN, result.getErr().orElseThrow());
    }
}

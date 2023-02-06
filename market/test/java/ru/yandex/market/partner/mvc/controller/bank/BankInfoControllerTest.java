package ru.yandex.market.partner.mvc.controller.bank;

import java.util.Objects;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

@ParametersAreNonnullByDefault
class BankInfoControllerTest extends FunctionalTest {

    private static Stream<String> getInvalidBics() {
        return Stream.of(
                "randomText",
                "0411111111",
                "04222222",
                "041_111_111",
                "O41111111"
        );
    }

    @Test
    @DisplayName("Корректный ответ ручки")
    @DbUnitDataSet(before = "csv/BankInfoController.active_bank.csv")
    void testCorrectResponse() {
        final String presentBic = "041111111";
        final ResponseEntity<String> response = FunctionalTestHelper.get(getUrl(presentBic));
        JsonTestUtil.assertEquals(response, this.getClass(), "json/BankInfoController.correct_response.json");
    }

    @Test
    @DisplayName("Закрытый банк с подходящим БИК не выдается ручкой")
    @DbUnitDataSet(before = "csv/BankInfoController.archived_bank.csv")
    void testArchivedBankIsIgnored() {
        final String archivedBankBic = "041111111";
        final HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(getUrl(archivedBankBic))
        );
        JsonTestUtil.assertResponseErrorMessage(exception,
                this.getClass(), "json/BankInfoController.bank_not_found.json");
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Передан некорректный БИК")
    @MethodSource("getInvalidBics")
    void testInvalidArguments(@Nullable final String bic) {
        final HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(getUrl(bic))
        );
        JsonTestUtil.assertResponseErrorMessage(exception,
                this.getClass(), "json/BankInfoController.invalid_param.json");
    }

    private String getUrl(final String bic) {
        return baseUrl + "/banks/" + Objects.toString(bic, "");
    }
}

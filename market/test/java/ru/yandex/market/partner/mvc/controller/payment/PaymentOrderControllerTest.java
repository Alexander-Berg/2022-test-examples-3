package ru.yandex.market.partner.mvc.controller.payment;

import java.time.LocalDate;
import java.util.Objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.shop.BeruVirtualShop;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Тесты для {@link PaymentOrderController}
 */
@DbUnitDataSet(before = "PaymentOrderControllerTest.get_number_of_payment_orders.before.csv")
class PaymentOrderControllerTest extends FunctionalTest {
    private static final long CAMPAIGN_ID = 10101;

    @Test
    void testGetNumbersOfPaymentOrders() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/payment-orders/" + CAMPAIGN_ID + "?target_date=" +
                        LocalDate.of(2020, 7, 30)
        );
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        JsonArray array = JsonTestUtil.parseJson(Objects.requireNonNull(response.getBody()))
                .getAsJsonObject()
                .get("result")
                .getAsJsonArray();
        assertThat(array.get(0).getAsLong(), is(485002L));
        assertThat(array.get(1).getAsLong(), is(485008L));
    }

    @Test
    void testBusinessPaymentOrders() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/business/"+ 1000 + "/payment-orders?target_date=" +
                        LocalDate.of(2020, 7, 30)
        );
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        JsonArray array = JsonTestUtil.parseJson(Objects.requireNonNull(response.getBody()))
                .getAsJsonObject()
                .get("result")
                .getAsJsonArray();
        assertThat(array.get(0).getAsLong(), is(485002L));
        assertThat(array.get(1).getAsLong(), is(485008L));
        assertThat(array.get(2).getAsLong(), is(7294353L));
    }

    @Test
    @DisplayName("Тесты на получение клиентской ошибки, если передали пустую дату")
    void testGetClientExceptionIfDateIsEmpty() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(baseUrl + "/payment-orders/" + CAMPAIGN_ID + "?target_date=")
        );
        JsonObject error = JsonTestUtil.parseJson(exception.getResponseBodyAsString())
                .getAsJsonObject().get("errors")
                .getAsJsonArray().get(0)
                .getAsJsonObject();
        assertThat(error.get("code").getAsString(), is("BAD_PARAM"));
        assertThat(error.get("message").getAsString(), is("The target_date can't be null or empty"));
    }

    @Test
    void testGetNumbersOfPaymentOrdersForDsbsCampaign() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/payment-orders/" + BeruVirtualShop.ID + "?target_date=" + LocalDate.of(2020, 7, 30)
        );
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        JsonArray array = JsonTestUtil.parseJson(Objects.requireNonNull(response.getBody()))
                .getAsJsonObject()
                .get("result")
                .getAsJsonArray();
        assertNotNull(array);
        assertEquals(1, array.size());
        assertEquals(7294353L, array.get(0).getAsLong());
    }
}

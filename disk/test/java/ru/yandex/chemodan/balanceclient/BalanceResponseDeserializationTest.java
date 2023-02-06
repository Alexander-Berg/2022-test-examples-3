package ru.yandex.chemodan.balanceclient;

import java.math.BigDecimal;
import java.net.URL;
import java.util.TimeZone;

import lombok.SneakyThrows;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.balanceclient.model.method.BalanceClassMethodSpec;
import ru.yandex.chemodan.balanceclient.model.response.GetPartnerBalanceContractResponseItem;
import ru.yandex.misc.test.Assert;

public class BalanceResponseDeserializationTest {
    @Test
    @SneakyThrows
    public void getPartnerBalanceContractResponseItem() {
        BalanceXmlRpcClient client = new BalanceXmlRpcClient(
                new BalanceXmlRpcClientConfig("logger", new URL("http://ya.ru"), TimeZone.getTimeZone("Europe/Moscow"),
                        Option.empty(), null, null));

        DateTimeZone.setDefault(DateTimeZone.forOffsetHours(0));
        String rawResponse = "[{" +
                "\"FirstDebtPaymentTermDT\":\"2021-07-20T00:00:00\"," +
                "\"Amount\":0," +
                "\"ConsumeSum\":\"347.97\"," +
                "\"LastActDT\":\"2021-07-31T00:00:00\"," +
                "\"ContractID\":2542038," +
                "\"ActSum\":\"347.97\"," +
                "\"ExpiredDT\":\"2021-07-20T00:00:00\"," +
                "\"FirstDebtFromDT\":\"2021-06-30T00:00:00\"," +
                "\"ReceiptSum\":\"0\"," +
                "\"DT\":\"2021-12-27T11:07:37.194094\"," +
                "\"Currency\":\"RUB\"," +
                "\"ExpiredDebtAmount\":\"347.97\"," +
                "\"FirstDebtAmount\":\"155.24\"}]";

        GetPartnerBalanceContractResponseItem response = client.parseResponse(JsonUtils.fromJson(rawResponse),
                new BalanceClassMethodSpec<>("handleName", GetPartnerBalanceContractResponseItem[].class))[0];
        Assert.equals(Instant.parse("2021-12-27T11:07:37.194094+0300"), response.getActualityDate());
        Assert.equals("RUB", response.getCurrencyCode());
        Assert.equals(BigDecimal.valueOf(347.97), response.getConsumeSum());
        Assert.equals(BigDecimal.valueOf(347.97), response.getActSum());
        Assert.equals(2542038L, response.getContractId());
        Assert.equals(Instant.parse("2021-07-20T00:00:00+0300"), response.getFirstDebtPaymentTermDT().get());
        Assert.equals(BigDecimal.valueOf(155.24), response.getFirstDebtAmount().get());
        Assert.equals(Instant.parse("2021-07-20T00:00:00+0300"), response.getExpiredDT().get());
        Assert.equals(BigDecimal.valueOf(347.97), response.getExpiredDebtAmount().get());
    }

    private static <R> R getParsed(String rawResponse, Class<R> cls) {
        return JsonUtils.MAPPER.convertValue(JsonUtils.fromJson(rawResponse), cls);
    }
}

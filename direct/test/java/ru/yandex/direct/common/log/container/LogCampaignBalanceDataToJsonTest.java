package ru.yandex.direct.common.log.container;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsType;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.utils.JsonUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class LogCampaignBalanceDataToJsonTest {
    private static final TypeReference<HashMap<String, String>> MAP_TYPE_REFERENCE =
            new TypeReference<HashMap<String, String>>() {
            };

    @org.junit.Test
    public void logCampaignBalanceData_convertedToJsonCorrectly() {
        // просто проверяем, что в JSON'е всё корректно после конвертации
        LogCampaignBalanceData logCampaignBalanceData = new LogCampaignBalanceData()
                .withCid(RandomNumberUtils.nextPositiveLong())
                .withClientId(RandomNumberUtils.nextPositiveLong())
                .withTid(RandomNumberUtils.nextPositiveLong())
                .withType(CampaignsType.text.getLiteral())
                .withCurrency(CurrencyCode.EUR.name())
                .withSum(RandomNumberUtils.nextPositiveBigDecimal())
                .withSumBalance(RandomNumberUtils.nextPositiveBigDecimal())
                .withSumDelta(RandomNumberUtils.nextPositiveBigDecimal());

        Map<String, String> actual = JsonUtils.fromJson(JsonUtils.toJson(logCampaignBalanceData), MAP_TYPE_REFERENCE);

        // Сравниваем строками, чтобы было нагляднее, что за значения ожидаем в числовых значениях
        assertThat(actual).containsOnly(
                entry("cid", String.valueOf(logCampaignBalanceData.getCid())),
                entry("ClientID", String.valueOf(logCampaignBalanceData.getClientId())),
                entry("tid", String.valueOf(logCampaignBalanceData.getTid())),
                entry("sum", logCampaignBalanceData.getSum().toString()),
                entry("sum_balance", logCampaignBalanceData.getSumBalance().toString()),
                entry("sum_delta", logCampaignBalanceData.getSumDelta().toString()),
                entry("type", logCampaignBalanceData.getType()),
                entry("currency", logCampaignBalanceData.getCurrency())
        );
    }
}

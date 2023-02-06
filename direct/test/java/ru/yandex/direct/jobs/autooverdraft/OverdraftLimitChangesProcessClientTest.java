package ru.yandex.direct.jobs.autooverdraft;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class OverdraftLimitChangesProcessClientTest {
    @SuppressWarnings("ConstantConditions")
    static OverdraftLimitChangesMailerJob job = new OverdraftLimitChangesMailerJob(
            null, null, null, null, null, null);

    static Stream<Arguments> testData() {
        return Stream.of(
                arguments(CurrencyCode.BYN, BigDecimal.valueOf(12.34D), BigDecimal.valueOf(15D), false,
                        new OverdraftLimitChangesInfo(ClientId.fromLong(1L), 12.34D, "BYN", true),
                        false),
                arguments(CurrencyCode.BYN, BigDecimal.valueOf(12.34D), BigDecimal.valueOf(15D), false,
                        new OverdraftLimitChangesInfo(ClientId.fromLong(1L), 12.36D, "BYN", true),
                        false),
                arguments(CurrencyCode.BYN, BigDecimal.valueOf(12.34D), BigDecimal.valueOf(15D), false,
                        new OverdraftLimitChangesInfo(ClientId.fromLong(1L), 11D, "BYN", true),
                        true),
                arguments(CurrencyCode.BYN, BigDecimal.valueOf(12.34D), BigDecimal.valueOf(15D), false,
                        new OverdraftLimitChangesInfo(ClientId.fromLong(1L), 11D, "BYN", false),
                        false),
                arguments(CurrencyCode.BYN, BigDecimal.valueOf(12.34D), BigDecimal.valueOf(15D), false,
                        new OverdraftLimitChangesInfo(ClientId.fromLong(1L), 16D, "BYN", false),
                        false),
                arguments(CurrencyCode.RUB, BigDecimal.valueOf(0D), BigDecimal.valueOf(15D), false,
                        new OverdraftLimitChangesInfo(ClientId.fromLong(1L), 16D, "RUB", true),
                        true),
                arguments(CurrencyCode.BYN, BigDecimal.valueOf(0D), BigDecimal.valueOf(15D), false,
                        new OverdraftLimitChangesInfo(ClientId.fromLong(1L), 11D, "RUB", true),
                        false),
                arguments(CurrencyCode.BYN, BigDecimal.valueOf(12.34D), BigDecimal.valueOf(0), false,
                        new OverdraftLimitChangesInfo(ClientId.fromLong(1L), 11D, "BYN", true),
                        false),
                arguments(CurrencyCode.BYN, BigDecimal.valueOf(12.34D), BigDecimal.valueOf(15D), true,
                        new OverdraftLimitChangesInfo(ClientId.fromLong(1L), 11D, "BYN", true),
                        false)
        );
    }

    @ParameterizedTest(name = "ClientCurrency = {0}, AutoOverdraftLimit = {1}, OverdraftLimit = {2}, IsBalanceBanned " +
            "= {3}, Changes = {4}, Should add = {5}")
    @MethodSource("testData")
    void test(CurrencyCode clientCurrencyCode, BigDecimal autoOverdraftLimit, BigDecimal overDraftLimit,
              boolean isBalanceBanned, OverdraftLimitChangesInfo changes, boolean shouldAdd) {
        // эти два списка заполняются синхронно со словарём клиентов, которым нужно отправить письмо
        var clientIdsToFetchChefs = new ArrayList<ClientId>();
        var clientsToFetchVat = new ArrayList<Client>();
        var client = new Client().withWorkCurrency(clientCurrencyCode).withAutoOverdraftLimit(autoOverdraftLimit)
                .withOverdraftLimit(overDraftLimit).withStatusBalanceBanned(isBalanceBanned);
        job.processClient(client, changes, clientIdsToFetchChefs, clientsToFetchVat);
        assertEquals(shouldAdd, clientIdsToFetchChefs.size() > 0);
        assertEquals(shouldAdd, clientsToFetchVat.size() > 0);
        if (shouldAdd) {
            assertEquals(ClientId.fromLong(1L), clientIdsToFetchChefs.get(0));
            assertTrue(job.changesToSend.containsKey(ClientId.fromLong(1L)));
        }
    }
}

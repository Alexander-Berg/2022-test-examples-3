package ru.yandex.direct.core.testing.data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import javax.annotation.Nullable;

import org.apache.commons.lang3.RandomUtils;

import ru.yandex.direct.core.entity.deal.model.Deal;
import ru.yandex.direct.core.entity.deal.model.DealPlacement;
import ru.yandex.direct.core.entity.deal.model.DealType;
import ru.yandex.direct.core.entity.deal.model.StatusAdfox;
import ru.yandex.direct.core.entity.deal.model.StatusAdfoxSync;
import ru.yandex.direct.core.entity.deal.model.StatusDirect;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Percent;
import ru.yandex.direct.dbutil.model.ClientId;

public class TestDeals {
    @SuppressWarnings("WeakerAccess")
    public static final Long MIN_TEST_DEAL_ID = 2_000_000L;
    public static final Long MAX_TEST_DEAL_ID = 10_000_000L;
    private static final Long MAX = 10000000L;

    private TestDeals() {
    }

    public static Deal defaultPrivateDeal(@Nullable ClientId clientId) {
        Long id = RandomUtils.nextLong(MIN_TEST_DEAL_ID, MAX_TEST_DEAL_ID);
        return defaultPrivateDeal(clientId, id);
    }

    public static Deal defaultPrivateDeal(@Nullable ClientId clientId, Long id) {
        Deal deal = new Deal();
        deal.withId(id)
                .withPublisherName("Publisher name for " + id)
                .withAdfoxName("Adfox name for " + id)
                .withAdfoxDescription("Adfox description for " + id)
                .withAdfoxStatus(StatusAdfox.CREATED)
                .withName("Name for " + id)
                .withDescription("Description for " + id)
                .withDirectStatus(StatusDirect.RECEIVED)
                .withStatusAdfoxSync(StatusAdfoxSync.NO)
                .withDateStart(LocalDateTime.now().withNano(0).plusDays(1))
                .withDateEnd(LocalDateTime.now().withNano(0).plusDays(15))
                .withTargetingsText("Targetings Text for " + id)
                .withContacts("Contacts for " + id)
                .withCpm(BigDecimal.valueOf(RandomUtils.nextLong(0, MAX)))
                .withExpectedImpressionsPerWeek(RandomUtils.nextLong(0, MAX))
                .withExpectedMoneyPerWeek(RandomUtils.nextLong(0, MAX))
                .withMarginRatio(Percent.fromPercent(BigDecimal.valueOf(5L)))
                .withAgencyFeePercent(Percent.fromPercent(BigDecimal.TEN))
                .withAgencyFeeType(12L)
                .withDealType(DealType.PREFERRED_DEAL)
                .withDealJson("{}")
                .withCurrencyCode(CurrencyCode.RUB)
                .withDateCreated(LocalDateTime.now())
                .withClientId(clientId != null ? clientId.asLong() : null)
                .withPlacements(Collections.singletonList(new DealPlacement()
                        .withPageId(123L)
                        .withImpId(Arrays.asList(1L, 2L))));
        return deal;
    }
}

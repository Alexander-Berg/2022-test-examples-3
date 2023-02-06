package ru.yandex.market.loyalty.admin.yt.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import ru.yandex.market.loyalty.admin.yt.mapper.importer.concrete.CashbackYtMapper;
import ru.yandex.market.loyalty.admin.yt.model.PromoYtDescription;
import ru.yandex.market.loyalty.admin.yt.source.YtSource;
import ru.yandex.market.loyalty.core.model.ReportPromoType;
import ru.yandex.market.loyalty.core.model.cashback.CashbackPromoDescription;
import Market.Promo.Promo.PromoDetails;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static ru.yandex.market.loyalty.core.utils.CommonTestUtils.randomString;
import static ru.yandex.market.loyalty.lightweight.DateUtils.toDate;

public class CashbackYtMapperTest {
    private static final long FEED_ID = 1234;
    private static final String ANAPLAN_ID = "anaplan promo Id";
    private static final String SHOP_PROMO_ID = "some shop promo Id";
    private static final Long START_TIME = 1625086800L;
    private static final Long END_TIME = 1627851599L;
    private static final Integer HUNDRED = 100;
    private static final BigDecimal HUNDRED_PENS = BigDecimal.valueOf(100);
    private static final Long MAX_CASHBACK = 300000L;
    private static final Double SHARE = 0.05D;

    private final CashbackYtMapper mapper = new CashbackYtMapper();

    @Test
    public void shouldBasicProportionalCashBackDescription() {
        CashbackPromoDescription description = mapper.mapDescription(new PromoYtDescription(
                0,
                FEED_ID,
                randomString(),
                ReportPromoType.BLUE_CASHBACK,
                YtSource.PERSONAL_PROMO_PIPELINE,
                PromoDetails.newBuilder()
                        .setAnaplanPromoId(ANAPLAN_ID)
                        .setShopPromoId(SHOP_PROMO_ID)
                        .setStartDate(START_TIME)   //секунды
                        .setEndDate(END_TIME)       //секунды
                        .setDescription(randomString())
                        .setRestrictions(PromoDetails.Restrictions.newBuilder()
                                .setBudgetLimit(PromoDetails.Money.newBuilder()
                                        .setValue(60405L)))
                        .setBlueCashback(PromoDetails.BlueCashback.newBuilder()
                                .setShare(SHARE)                    // в протобуфке процент от 0 до 1
                                .setVersion(1)
                                .setMaxOfferCashback(MAX_CASHBACK)) //в протобуфке лежат копейки!
                        .setSameTypePriority(HUNDRED)
                        .build()
        ));

        assertThat(description.getNominal(), comparesEqualTo(BigDecimal.valueOf(SHARE).multiply(HUNDRED_PENS)));
        assertThat(description.getStartTime(), comparesEqualTo(toDate(START_TIME, TimeUnit.SECONDS)));
        assertThat(description.getEndTime(), comparesEqualTo(toDate(END_TIME, TimeUnit.SECONDS)));
        assertThat(description.getPriority(), comparesEqualTo(HUNDRED));
        assertThat(description.getMaxCashback(), comparesEqualTo(
                BigDecimal.valueOf(MAX_CASHBACK)
                        .divide(HUNDRED_PENS, RoundingMode.UP)
        ));
    }
}

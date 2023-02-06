package ru.yandex.direct.core.entity.promocodes.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@CoreTest
@RunWith(Parameterized.class)
public class PromocodesAntiFraudServiceisAcceptableWalletTest {
    private static PromocodesAntiFraudService service;

    @Parameterized.Parameter
    public CampaignType type;

    @Parameterized.Parameter(1)
    public DateTimeType createTime;

    @Parameterized.Parameter(2)
    public boolean expectedAnswer;

    @Parameterized.Parameters(name = "Campaign type: {0}, createTime: {1}, expected answer: {2}")
    public static Iterable<Object[]> data() {
        return new ArrayList<>(Arrays.asList(new Object[][]{
                {CampaignType.TEXT, DateTimeType.OLD, false},
                {CampaignType.TEXT, DateTimeType.NEW, false},
                {CampaignType.WALLET, DateTimeType.OLD, false},
                {CampaignType.WALLET, DateTimeType.NEW, true},
        }));
    }

    private static LocalDateTime oldTime;
    private static LocalDateTime newTime;

    @BeforeClass
    public static void prepare() {
        service = mock(PromocodesAntiFraudService.class);
        when(service.isAcceptableWallet(any())).thenCallRealMethod();
        // если инициализировать константы сразу в enum, то у них может оказаться другая таймзона
        oldTime = LocalDateTime.now().minusHours(25);
        newTime = LocalDateTime.now().minusHours(23);
    }

    @Test
    public void testIsAcceptableWallet() {
        assertThat(
                service.isAcceptableWallet(new Campaign().withType(type).withCreateTime(createTime.getTime())),
                is(expectedAnswer)
        );
    }

    private enum DateTimeType {
        OLD,
        NEW;

        LocalDateTime getTime() {
            switch (this) {
                case OLD:
                    return oldTime;
                case NEW:
                    return newTime;
                default:
                    return null;
            }
        }
    }
}

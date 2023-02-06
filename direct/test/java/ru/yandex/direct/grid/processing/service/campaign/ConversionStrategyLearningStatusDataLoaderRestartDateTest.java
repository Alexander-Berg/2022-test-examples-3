package ru.yandex.direct.grid.processing.service.campaign;

import java.time.LocalDate;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.grid.core.entity.model.campaign.GdiCampaignStats;
import ru.yandex.direct.grid.model.campaign.GdTextCampaign;

import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.processing.service.campaign.ConversionStrategyLearningStatusDataLoader.CONVERSION_STRATEGY_LEARNING_START_DATE;

@RunWith(Parameterized.class)
public class ConversionStrategyLearningStatusDataLoaderRestartDateTest {

    @Parameterized.Parameter(0)
    public String name;

    @Parameterized.Parameter(1)
    public ConversionStrategyWithCampaignLearningData data;

    @Parameterized.Parameter(2)
    public LocalDate now;

    @Parameterized.Parameter(3)
    public LocalDate expectedRestartDate;

    private static final LocalDate NOW = now();

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] parameters() {
        return new Object[][]{
                //restart date undefined
                {"campaign start in future", data(NOW.minusDays(1), NOW.plusDays(1)), NOW, null},

                //restart date defined
                {"restart bidder time is null", data(null, NOW.minusDays(2)), NOW, NOW.minusDays(2)},
                {"restart bidder time is not null", data(NOW.minusDays(1), NOW.minusDays(2)), NOW, NOW.minusDays(1)},
                {"restart bidder time is before start time", data(NOW.minusDays(2), NOW.minusDays(1)), NOW,
                        NOW.minusDays(1)},
                {"restart bidder time is null and start day before learning start", data(null,
                        CONVERSION_STRATEGY_LEARNING_START_DATE.minusDays(1)), NOW,
                        CONVERSION_STRATEGY_LEARNING_START_DATE},
                {"campaign pause ok status", data(NOW.minusDays(2), NOW.minusDays(1)), NOW, NOW.minusDays(1)}
        };
    }

    @Test
    public void testGetRestartDate() {
        var restartDate = ConversionStrategyLearningStatusDataLoader.getRestartDate(data, now);
        assertThat(restartDate).isEqualTo(expectedRestartDate);
    }

    private static ConversionStrategyWithCampaignLearningData data(LocalDate restartBidderTime, LocalDate startDate) {
        var campaign = new GdTextCampaign()
                .withStartDate(startDate);

        return new ConversionStrategyWithCampaignLearningData(
                campaign,
                restartBidderTime,
                null,
                new GdiCampaignStats(),
                new GdiCampaignStats()
        );
    }
}

package ru.yandex.market.logistics.management.service.notifications.telegram;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.service.notification.email.DayOffNotification;
import ru.yandex.market.logistics.management.service.notification.telegram.TelegramMessageBuilder;

class TelegramMessageBuilderTest {

    private TelegramMessageBuilder telegramMessageBuilder = new TelegramMessageBuilder();

    @Test
    void buildCreateMessageTest() {
        Assertions.assertThat(telegramMessageBuilder.buildCreateMessage(getDayOffNotification()))
            .isEqualTo("✅✅✅ - Сработал DayOff\n" +
                "\n" +
                "*Delivery*: [СДЭК (51)](https://lms.market.yandex-team.ru/lms/partner/51)\n" +
                "*RegionFrom*: Москва и Московская область (1)\n" +
                "*RegionTo*: Москва (213)\n" +
                "*DayOff*: 2019-05-04\n" +
                "*Capacity*: [100](https://lms.market.yandex-team.ru/lms/partner-capacity/216)\n" +
                "*Platform*: Beru\n" +
                "\n" +
                "\uD83E\uDD84\uD83E\uDD84\uD83E\uDD84");
    }

    @Test
    void buildRevertMessageTest() {
        Assertions.assertThat(telegramMessageBuilder.buildRevertMessage(getDayOffNotification()))
            .isEqualTo("\uD83D\uDEAB\uD83D\uDEAB\uD83D\uDEAB - Откатили DayOff\n" +
                "\n" +
                "*Delivery*: [СДЭК (51)](https://lms.market.yandex-team.ru/lms/partner/51)\n" +
                "*RegionFrom*: Москва и Московская область (1)\n" +
                "*RegionTo*: Москва (213)\n" +
                "*DayOff*: 2019-05-04\n" +
                "*Capacity*: [100](https://lms.market.yandex-team.ru/lms/partner-capacity/216)\n" +
                "*Platform*: Beru\n" +
                "\n" +
                "\uD83E\uDD84\uD83E\uDD84\uD83E\uDD84");
    }

    private DayOffNotification getDayOffNotification() {
        return new DayOffNotification(
            "Delivery",
            "СДЭК",
            "51",
            "Москва и Московская область",
            "1",
            "Москва",
            "213",
            "2019-05-04",
            "216",
            "100",
            null,
            "Beru"
        );
    }
}

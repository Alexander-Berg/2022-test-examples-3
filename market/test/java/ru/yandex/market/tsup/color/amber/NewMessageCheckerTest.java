package ru.yandex.market.tsup.color.amber;

import java.time.LocalDateTime;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tsup.service.data_provider.entity.run.enums.RunMessageType;
import ru.yandex.market.tsup.service.data_provider.primitive.external.tpl_carrier.model.PartnerRunDriverLog;
import ru.yandex.market.tsup.service.rating.TripData;
import ru.yandex.market.tsup.service.rating.checker.amber.NewMessageChecker;

public class NewMessageCheckerTest {

    private final NewMessageChecker newMessageChecker = new NewMessageChecker();


    @Test
    void processedMessage() {
        Assertions.assertThat(newMessageChecker.isApplicable(
            new TripData(
                1,
                null,
                PartnerRunDriverLog.builder()
                    .lastMessageTime(LocalDateTime.of(2021, 9, 15, 21, 0, 0))
                    .lastMessageType(RunMessageType.RESOLUTION)
                    .build(),
                null,
                null,
                null
            )
        )).isFalse();
    }

    @Test
    void newCriticalMessage() {
        Assertions.assertThat(newMessageChecker.isApplicable(
            new TripData(
                1,
                null,
                PartnerRunDriverLog.builder()
                    .lastMessageTime(LocalDateTime.of(2021, 9, 15, 21, 0, 0))
                    .lastMessageType(RunMessageType.CRITICAL_ISSUE)
                    .build(),
                null,
                null,
                null
            )
        )).isTrue();
    }

    @Test
    void newOkMessage() {
        Assertions.assertThat(newMessageChecker.isApplicable(
            new TripData(
                1,
                null,
                PartnerRunDriverLog.builder()
                    .lastMessageTime(LocalDateTime.of(2021, 9, 15, 21, 0, 0))
                    .lastMessageType(RunMessageType.ON_ROUTE)
                    .build(),
                null,
                null,
                null
            )
        )).isFalse();
    }

}

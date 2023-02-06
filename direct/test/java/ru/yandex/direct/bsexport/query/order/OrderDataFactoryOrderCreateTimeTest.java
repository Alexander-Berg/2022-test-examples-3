package ru.yandex.direct.bsexport.query.order;

import java.time.Instant;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.bsexport.model.Order;
import ru.yandex.direct.bsexport.snapshot.BsExportSnapshot;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.test.utils.RandomNumberUtils.nextPositiveLong;
import static ru.yandex.direct.utils.DateTimeUtils.MSK;

class OrderDataFactoryOrderCreateTimeTest {

    private OrderDataFactory orderDataFactory = new OrderDataFactory(mock(BsExportSnapshot.class));


    @Test
    void createTimeIsNull() {
        var campaign = new TextCampaign().withId(nextPositiveLong());
        var builder = Order.newBuilder();
        orderDataFactory.addOrderCreateTime(builder, campaign);
        assertThat(builder.hasOrderCreateTime()).isFalse();
    }

    @Test
    void createTimeIsNotNull() {
        var campaign = new TextCampaign().withId(nextPositiveLong())
                .withCreateTime(LocalDateTime.ofInstant(Instant.ofEpochSecond(1590490975L), MSK));
        var builder = Order.newBuilder();
        orderDataFactory.addOrderCreateTime(builder, campaign);
        assertThat(builder.hasOrderCreateTime()).isTrue();
        assertThat(builder.getOrderCreateTime()).isEqualTo(1590490975L);
    }
}

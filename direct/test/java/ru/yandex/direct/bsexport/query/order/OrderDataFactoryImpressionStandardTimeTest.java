package ru.yandex.direct.bsexport.query.order;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.bsexport.model.ImpressionStandardType;
import ru.yandex.direct.bsexport.model.Order;
import ru.yandex.direct.bsexport.snapshot.BsExportSnapshot;
import ru.yandex.direct.core.entity.campaign.model.CpmYndxFrontpageCampaign;
import ru.yandex.direct.core.entity.campaign.model.ImpressionStandardTime;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.test.utils.RandomNumberUtils.nextPositiveLong;

class OrderDataFactoryImpressionStandardTimeTest {

    private OrderDataFactory orderDataFactory = new OrderDataFactory(mock(BsExportSnapshot.class));


    @Test
    void campaignWithoutImpressionStandardTime() {
        var campaign = new TextCampaign().withId(nextPositiveLong());
        var builder = Order.newBuilder();
        orderDataFactory.addImpressionStandardTimeFields(builder, campaign);
        assertThat(builder.hasImpressionStandardTime()).isFalse();
        assertThat(builder.hasImpressionStandardType()).isFalse();
    }

    @Test
    void campaignWithNullImpressionStandardTime() {
        var campaign = new CpmYndxFrontpageCampaign().withId(nextPositiveLong());
        var builder = Order.newBuilder();
        orderDataFactory.addImpressionStandardTimeFields(builder, campaign);
        assertThat(builder.hasImpressionStandardTime()).isFalse();
        assertThat(builder.hasImpressionStandardType()).isFalse();
    }

    @Test
    void campaignWithImpressionStandardTime() {
        var campaign = new CpmYndxFrontpageCampaign().withId(nextPositiveLong())
                .withImpressionStandardTime(ImpressionStandardTime.MRC);
        var builder = Order.newBuilder();
        orderDataFactory.addImpressionStandardTimeFields(builder, campaign);
        assertThat(builder.hasImpressionStandardTime()).isTrue();
        assertThat(builder.getImpressionStandardTime()).isEqualTo(1000);
        assertThat(builder.hasImpressionStandardType()).isTrue();
        assertThat(builder.getImpressionStandardType()).isEqualTo(ImpressionStandardType.mrc);
    }
}

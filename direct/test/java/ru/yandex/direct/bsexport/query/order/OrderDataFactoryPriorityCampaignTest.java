package ru.yandex.direct.bsexport.query.order;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.bsexport.model.Order;
import ru.yandex.direct.bsexport.snapshot.BsExportSnapshotTestBase;
import ru.yandex.direct.bsexport.snapshot.model.ExportedClient;
import ru.yandex.direct.core.entity.client.model.ClientFlags;

import static org.assertj.core.api.Assertions.assertThat;

class OrderDataFactoryPriorityCampaignTest extends BsExportSnapshotTestBase {
    private OrderDataFactory orderDataFactory;

    @BeforeEach
    void prepareDataAndCreateDataFactory() {
        orderDataFactory = new OrderDataFactory(snapshot);
    }

    @Test
    void notPriorityCampaignTest() {
        var client = new ExportedClient()
                .withId(1L)
                .withFlags(Set.of());
        var builder = Order.newBuilder();
        orderDataFactory.addIsPriorityCampaign(builder, client);
        assertThat(builder.hasIsPriorityCampaign()).isFalse();
    }

    @Test
    void priorityCampaignTest() {
        var client = new ExportedClient()
                .withId(1L)
                .withFlags(Set.of(ClientFlags.AS_SOON_AS_POSSIBLE));
        var builder = Order.newBuilder();
        orderDataFactory.addIsPriorityCampaign(builder, client);
        assertThat(builder.hasIsPriorityCampaign()).isTrue();
        assertThat(builder.getIsPriorityCampaign()).isEqualTo(1);
    }
}

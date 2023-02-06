package ru.yandex.market.ff.dbqueue.consumer.functional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.dbqueue.consumer.GetFFInboundSuccessQueueConsumer;
import ru.yandex.market.ff.model.dbqueue.GetFFInboundSuccessPayload;
import ru.yandex.market.logistic.gateway.common.model.common.CompositeId;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.common.Inbound;
import ru.yandex.market.logistic.gateway.common.model.common.InboundType;
import ru.yandex.market.logistic.gateway.common.model.common.PartialId;
import ru.yandex.market.logistic.gateway.common.model.common.PartialIdType;
import ru.yandex.market.logistic.gateway.common.model.common.RegistryBox;
import ru.yandex.market.logistic.gateway.common.model.common.RegistryType;
import ru.yandex.market.logistic.gateway.common.model.common.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.common.UnitCount;
import ru.yandex.market.logistic.gateway.common.model.common.UnitCountType;
import ru.yandex.market.logistic.gateway.common.model.common.UnitInfo;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundRegistry;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.RegistryItem;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class GetFFInboundSuccessQueueConsumerTest extends IntegrationTestWithDbQueueConsumers {

    @Autowired
    private GetFFInboundSuccessQueueConsumer consumer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DatabaseSetup("classpath:consumer/get-ff-inbound-success/before-successful-customer-return.xml")
    @ExpectedDatabase(value = "classpath:consumer/get-ff-inbound-success/after-successful-customer-return.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void processCustomerReturnSuccess() {
        setUpdateItemsFromRegistries();
        var payload = new GetFFInboundSuccessPayload(1L, createInbound(),
                List.of(createFactualReturnsSecondaryRegistry(),
                        createFactualRegistry(),
                        createFactualAcceptanceSecondaryRegistry(),
                        createFactualAcceptanceInitialRegistry()
                ), null);
        var task = new Task<>(new QueueShardId("shard"), payload, 0,
                ZonedDateTime.now(ZoneId.systemDefault()), null, null);
        consumer.execute(task);
    }

    @Test
    @DatabaseSetup("classpath:consumer/get-ff-inbound-non-complient/before-successful-customer-return.xml")
    @ExpectedDatabase(value = "classpath:consumer/get-ff-inbound-non-complient/after-successful-customer-return.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void processCustomerReturnWithNonComplientUnitCountSuccess() {
        setUpdateItemsFromRegistries();
        var payload = new GetFFInboundSuccessPayload(1L, createInbound(),
                List.of(createFactualReturnsSecondaryRegistryWithNonComplient()
                ), null);
        var task = new Task<>(new QueueShardId("shard"), payload, 0,
                ZonedDateTime.now(ZoneId.systemDefault()), null, null);
        consumer.execute(task);
    }

    private Inbound createInbound() {
        return Inbound.builder(
                ResourceId.builder().setYandexId("1").setPartnerId("p1").build(),
                InboundType.RETURNS,
                DateTimeInterval.fromFormattedValue("2021-12-08T14:28:11+00:00/2021-12-08T14:28:11+00:00")
        ).build();
    }

    private InboundRegistry createFactualReturnsSecondaryRegistry() {
        return InboundRegistry.builder(
                ResourceId.builder().setYandexId("9-1").setPartnerId("p-9-1").build(),
                ResourceId.builder().setYandexId("1").setPartnerId("p1").build(),
                RegistryType.FACTUAL_RETURNS_SECONDARY
        ).setItems(List.of(createRegistryItem()))
                .build();
    }

    private InboundRegistry createFactualRegistry() {
        return InboundRegistry.builder(
                ResourceId.builder().setYandexId("1-1").setPartnerId("p-1-1").build(),
                ResourceId.builder().setYandexId("1").setPartnerId("p1").build(),
                RegistryType.FACTUAL
        ).setItems(List.of(createRegistryItem()))
                .build();
    }

    private InboundRegistry createFactualAcceptanceSecondaryRegistry() {
        return InboundRegistry.builder(
                ResourceId.builder().setYandexId("5-1").setPartnerId("p-5-1").build(),
                ResourceId.builder().setYandexId("1").setPartnerId("p1").build(),
                RegistryType.FACTUAL_ACCEPTANCE_SECONDARY
        ).setItems(List.of(createRegistryItem()))
                .build();
    }

    private InboundRegistry createFactualAcceptanceInitialRegistry() {
        return InboundRegistry.builder(
                ResourceId.builder().setYandexId("4-1").setPartnerId("p-4-1").build(),
                ResourceId.builder().setYandexId("1").setPartnerId("p1").build(),
                RegistryType.FACTUAL_ACCEPTANCE_INITIAL
        ).setBoxes(List.of(createRegistryBox()))
                .build();
    }

    private RegistryItem createRegistryItem() {
        return RegistryItem.builder(
                UnitInfo.builder()
                        .setCompositeId(CompositeId.builder(
                                List.of(
                                        new PartialId(PartialIdType.ARTICLE, "sku1"),
                                        new PartialId(PartialIdType.VENDOR_ID, "1"),
                                        new PartialId(PartialIdType.ORDER_ID, "78647161")
                                )
                        ).build())
                        .setCounts(List.of(
                               new UnitCount.UnitCountBuilder().setCountType(UnitCountType.FIT)
                                       .setQuantity(10).build(),
                               new UnitCount.UnitCountBuilder().setCountType(UnitCountType.DEFECT)
                                       .setQuantity(3).build()
                        ))
                        .build()
        )
                .setName("name")
                .build();
    }

    private InboundRegistry createFactualReturnsSecondaryRegistryWithNonComplient() {
        return InboundRegistry.builder(
                        ResourceId.builder().setYandexId("9-1").setPartnerId("p-9-1").build(),
                        ResourceId.builder().setYandexId("1").setPartnerId("p1").build(),
                        RegistryType.FACTUAL_RETURNS_SECONDARY
                ).setItems(
                        List.of(
                                createRegistryItemWithNonComplientUnitCount(),
                                createRegistryItemWithSingleNonComplientUnitCount()
                        )
                )
                .build();
    }

    private RegistryItem createRegistryItemWithNonComplientUnitCount() {
        return RegistryItem.builder(
                        UnitInfo.builder()
                                .setCompositeId(CompositeId.builder(
                                        List.of(
                                                new PartialId(PartialIdType.ARTICLE, "sku1"),
                                                new PartialId(PartialIdType.VENDOR_ID, "1"),
                                                new PartialId(PartialIdType.ORDER_ID, "78647161")
                                        )
                                ).build())
                                .setCounts(List.of(
                                        new UnitCount.UnitCountBuilder().setCountType(UnitCountType.FIT)
                                                .setQuantity(10).build(),
                                        new UnitCount.UnitCountBuilder().setCountType(UnitCountType.NON_COMPLIENT)
                                                .setQuantity(3).build()
                                ))
                                .build()
                )
                .setName("name")
                .build();
    }

    private RegistryItem createRegistryItemWithSingleNonComplientUnitCount() {
        return RegistryItem.builder(
                        UnitInfo.builder()
                                .setCompositeId(CompositeId.builder(
                                        List.of(
                                                new PartialId(PartialIdType.ARTICLE, "sku2"),
                                                new PartialId(PartialIdType.VENDOR_ID, "1"),
                                                new PartialId(PartialIdType.ORDER_ID, "78647161")
                                        )
                                ).build())
                                .setCounts(List.of(
                                        new UnitCount.UnitCountBuilder().setCountType(UnitCountType.NON_COMPLIENT)
                                                .setQuantity(1).build()
                                ))
                                .build()
                )
                .setName("name")
                .build();
    }

    private RegistryBox createRegistryBox() {
        return new RegistryBox(
                UnitInfo.builder()
                        .setCompositeId(CompositeId.builder(
                                List.of(new PartialId(PartialIdType.BOX_ID, "VOZVRAT_SF_PVZ_1436198"))
                        ).build())
                        .setCounts(List.of(
                                new UnitCount.UnitCountBuilder().setCountType(UnitCountType.FIT).setQuantity(1).build()
                        ))
                        .build()
        );
    }

    private void setUpdateItemsFromRegistries() {
        jdbcTemplate.update("update request_subtype set update_items_from_registries = true where id = 7");
    }
}

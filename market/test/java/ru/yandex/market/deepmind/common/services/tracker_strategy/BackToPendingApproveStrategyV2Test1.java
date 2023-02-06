package ru.yandex.market.deepmind.common.services.tracker_strategy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepmind.common.assertions.DeepmindAssertions;
import ru.yandex.market.deepmind.common.category.models.Category;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.AssortSsku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.EnrichApproveToPendingExcelComposer;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.Header;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.Headers;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.KeyMetaV2;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.MetaV2;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.SpecialOrderData;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.TrackerApproverValidationResultMap;
import ru.yandex.market.deepmind.common.utils.SessionUtils;
import ru.yandex.market.deepmind.tracker_approver.pojo.ProcessRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.ProcessResponse;
import ru.yandex.market.deepmind.tracker_approver.pojo.TicketState;
import ru.yandex.market.deepmind.tracker_approver.pojo.TrackerApproverRawData;
import ru.yandex.market.deepmind.tracker_approver.pojo.TrackerApproverTicketRawStatus;
import ru.yandex.market.deepmind.tracker_approver.utils.JsonWrapper;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.ApprovedSpecialOrderItem;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.CreateSpecialOrderItem;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.CreateSpecialOrderRequest;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.CreateSpecialOrderResponse;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.DeclinedSpecialOrderItem;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.SpecialOrderCreateKey;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.SpecialOrderDateType;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.SpecialOrderType;

import static org.mockito.ArgumentMatchers.any;
import static ru.yandex.market.deepmind.common.pojo.SskuStatusReason.NO_PURCHASE_PRICE;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SAINT_PETERSBURG_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SAMARA_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.EnrichApproveToPendingExcelComposer.WAREHOUSE_FIRST_SUPPLY_CNT_ENDING;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.EnrichApproveToPendingExcelComposer.WAREHOUSE_ORDER_DATE_ENDING;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.Headers.SHOP_SKU_KEY;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.Headers.SUPPLIER_ID_KEY;

public class BackToPendingApproveStrategyV2Test1 extends BackToPendingApproveStrategyBaseTestClass {

    @Test
    public void startStepTest() {
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE, "comment2", null)
        );
        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222")
        );
        var meta = new MetaV2().setAuthor("author1").setDescription("description1");
        var ticketKey = facade.start(shopSkuKeys, meta);
        Assertions.assertThat(ticketKey).isEqualTo("TEST-1");
        Assertions.assertThat(session.issues().get(ticketKey).getComponents())
            .extracting(v -> v.load().getName())
            .containsExactlyInAnyOrder("Возвращение ассортимента", "1P");
        Assertions
            .assertThat(session.issues().get(ticketKey).getSummary())
            .contains("Заявка на возвращение ассортимента");
        var description = session.issues().get(ticketKey).getDescription();
        Assertions
            .assertThat(description.get())
            .contains(meta.getDescription(), "Прошу согласовать возвращение ассортимента",
                "shop-sku-111", "shop-sku-222")
            .contains(meta.getDescription(),
                "* ((http://localhost:8080/#/availability/ssku/blocking_and_statuses?" +
                    "shop_sku_keys=111:shop-sku-111,222:shop-sku-222 Ссылка на все 2 corefix ssku))");
    }

    @Test
    public void enrichStepTest() {
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.ACTIVE, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.ACTIVE, "comment2", null)
        );

        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222")
        );
        var meta = new MetaV2().setAuthor("author1").setDescription("description1");
        var ticket = facade.start(shopSkuKeys, meta);
        facade.enrich(ticket);
        var attachments = session.attachments().getAll(ticket);
        Assertions.assertThat(attachments.stream().collect(Collectors.toList()))
            .hasSize(1);
        Assertions.assertThat(session.issues().getSummonees(ticket))
            .isEmpty(); // because dev environment
    }


    @Test
    public void allSpecialOrdersCreatedTest() {
        sskuStatusRepository.save(
            sskuStatus(111, "shop-sku-111", OfferAvailability.INACTIVE, "comment1"),
            sskuStatus(222, "shop-sku-222", OfferAvailability.DELISTED, "comment2")
        );

        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222")
        );
        var meta = new MetaV2().setAuthor("author1").setDescription("description1");
        var ticket = facade.start(shopSkuKeys, meta);
        executor.run();

        var attachments = session.attachments().getAll(ticket);
        Assertions.assertThat(attachments.stream().collect(Collectors.toList()))
            .hasSize(1);

        Map<ServiceOfferKey, KeyMetaV2> keyMetaMap = shopSkuKeys.stream().collect(
            Collectors.toMap(
                Function.identity(), ssku -> new KeyMetaV2().addSpecialOrderData(
                    List.of(
                        new SpecialOrderData(SOFINO_ID, 2L, null, null, 1, LocalDate.now(), null)
                    ))));

        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            fromUserExcelComposer.processKeys(shopSkuKeys, strategySpy.getNotCreatedSpecialOrderData(keyMetaMap)),
            user);

        SessionUtils.check(session, ticket);

        Mockito.doReturn(new CreateSpecialOrderResponse().approvedItems(List.of(
                new ApprovedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111")
                ).demandId(113154600L),
                new ApprovedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000222.shop-sku-222")
                ).demandId(113154600L)
            ))
        ).when(replenishmentService).specialOrderRequestCreateAll(
            new CreateSpecialOrderRequest().ticketId(ticket)
                .specialOrderItems(List.of(
                    new CreateSpecialOrderItem().key(
                            new SpecialOrderCreateKey().ssku("000111.shop-sku-111").warehouseId(SOFINO_ID)
                        ).price(BigDecimal.TEN).orderType(SpecialOrderType.NEW).deliveryDate(LocalDate.now())
                        .orderDateType(SpecialOrderDateType.TODAY).quantity(2L).quantum(1),
                    new CreateSpecialOrderItem().key(
                            new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000222.shop-sku-222")
                        ).price(BigDecimal.TEN).orderType(SpecialOrderType.NEW).deliveryDate(LocalDate.now())
                        .orderDateType(SpecialOrderDateType.TODAY).quantity(2L).quantum(1)
                )));

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
    }

    @Test
    @SuppressWarnings("checkstyle:MethodLength")
    public void rereadQuantumFieldTest() {
        sskuStatusRepository.save(
            sskuStatus(111, "shop-sku-111", OfferAvailability.INACTIVE, "comment1"),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE, "comment2"),
            sskuStatus(333, "shop-sku-333", OfferAvailability.DELISTED, "comment3")
        );

        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        );
        var meta = new MetaV2().setAuthor("author1").setDescription("description1");
        var ticket = facade.start(shopSkuKeys, meta);
        executor.run();
        var attachments = session.attachments().getAll(ticket);
        Assertions.assertThat(attachments.stream().collect(Collectors.toList()))
            .hasSize(1);

        Map<ServiceOfferKey, KeyMetaV2> keyMetaMap = Map.of(
            new ServiceOfferKey(222, "shop-sku-222"),
            new KeyMetaV2().addSpecialOrderData(
                List.of(
                    new SpecialOrderData(SOFINO_ID, 10L, null, null, 3, LocalDate.now(), null)
                )),
            new ServiceOfferKey(333, "shop-sku-333"),
            new KeyMetaV2().addSpecialOrderData(
                List.of(
                    new SpecialOrderData(SOFINO_ID, 10L, null, null, 1, LocalDate.now(), null)
                ))
        );

        // Прикрепляем криво заполненный пользователем с точки зрения Реплена эксель
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            fromUserExcelComposer.processKeys(keyMetaMap.keySet(),
                strategySpy.getNotCreatedSpecialOrderData(keyMetaMap)),
            user);
        SessionUtils.check(session, ticket);
        Mockito.doReturn(
                new CreateSpecialOrderResponse()
                    .declinedItems(List.of(
                        new DeclinedSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000222.shop-sku-222"))
                            .error("Для заявки на SSKU 000304.200503 для недели №1 количество отгрузки (10) " +
                                "не кратно кванту (3)")
                    )))
            .when(replenishmentService).specialOrderRequestCreateAll(
                new CreateSpecialOrderRequest()
                    .ticketId(ticket)
                    .specialOrderItems(List.of(
                        new CreateSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000222.shop-sku-222")
                            ).price(BigDecimal.TEN).orderType(SpecialOrderType.NEW).deliveryDate(LocalDate.now())
                            .orderDateType(SpecialOrderDateType.TODAY).quantity(10L).quantum(3),
                        new CreateSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000333.shop-sku-333")
                            ).price(BigDecimal.TEN).orderType(SpecialOrderType.NEW).deliveryDate(LocalDate.now())
                            .orderDateType(SpecialOrderDateType.TODAY).quantity(10L).quantum(1)
                    )));
        // CЗ не смогли создаться пачкой
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PROCESSED);

        SessionUtils.awaitsActivation(session, ticket);
        Mockito.doReturn(new CreateSpecialOrderResponse()
                .approvedItems(List.of(
                    new ApprovedSpecialOrderItem().key(
                        new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000333.shop-sku-333")
                    ).demandId(113154600L)))
                .declinedItems(List.of(
                    new DeclinedSpecialOrderItem()
                        .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000222.shop-sku-222"))
                        .error("Для заявки на SSKU 000304.200503 для недели №1 количество отгрузки (10)" +
                            " не кратно кванту (3)")
                )))
            .when(replenishmentService).specialOrderRequestCreateAny(
                new CreateSpecialOrderRequest()
                    .ticketId(ticket)
                    .specialOrderItems(List.of(
                        new CreateSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000222.shop-sku-222"))
                            .price(BigDecimal.TEN)
                            .orderType(SpecialOrderType.NEW)
                            .deliveryDate(LocalDate.now())
                            .orderDateType(SpecialOrderDateType.TODAY)
                            .quantity(10L)
                            .quantum(3),
                        new CreateSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000333.shop-sku-333"))
                            .price(BigDecimal.TEN)
                            .orderType(SpecialOrderType.NEW)
                            .deliveryDate(LocalDate.now())
                            .orderDateType(SpecialOrderDateType.TODAY)
                            .quantity(10L)
                            .quantum(1)
                    )));
        // CЗ одна успешная при поштучном создании
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PROCESSED);

        keyMetaMap = Map.of(
            new ServiceOfferKey(222, "shop-sku-222"),
            new KeyMetaV2().addSpecialOrderData(
                List.of(
                    new SpecialOrderData(SOFINO_ID, 10L, null, null, 1, LocalDate.now(), null)
                ))
        );

        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            fromUserExcelComposer.processKeys(keyMetaMap.keySet(),
                strategySpy.getNotCreatedSpecialOrderData(keyMetaMap)), user);

        Mockito.doReturn(new CreateSpecialOrderResponse().approvedItems(List.of(
                new ApprovedSpecialOrderItem()
                    .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000222.shop-sku-222"))
                    .demandId(113154601L)
            ))
        ).when(replenishmentService).specialOrderRequestCreateAll(new CreateSpecialOrderRequest().ticketId(ticket)
            .specialOrderItems(List.of(
                new CreateSpecialOrderItem()
                    .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000222.shop-sku-222"))
                    .price(BigDecimal.TEN)
                    .orderType(SpecialOrderType.NEW)
                    .deliveryDate(LocalDate.now())
                    .orderDateType(SpecialOrderDateType.TODAY)
                    .quantity(10L)
                    .quantum(1)
            )));

        SessionUtils.check(session, ticket);
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
    }

    @Test
    public void noSpecialOrdersInExcelTest() {
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.INACTIVE, "")
        );

        var ticket = facade.start(List.of(new ServiceOfferKey(111, "shop-sku-111")), new MetaV2());

        // mark ticket to be checked
        SessionUtils.check(session, ticket);
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        Assertions.assertThat(sskuStatusRepository.findByKey(111, "shop-sku-111"))
            .get().extracting(SskuStatus::getAvailability)
            .isEqualTo(OfferAvailability.PENDING);
    }

    @Test
    public void simpleRun() {
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.INACTIVE, "")
        );

        var ticket = facade.start(List.of(new ServiceOfferKey(111, "shop-sku-111")), new MetaV2());
        executor.run();

        Map<ServiceOfferKey, KeyMetaV2> keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new KeyMetaV2().addSpecialOrderData(
                List.of(
                    new SpecialOrderData(SOFINO_ID, 111L, null, null, 1, LocalDate.now().plusDays(10), null)
                ))
        );

        // Прикрепляем заполненный пользователем эксель
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            fromUserExcelComposer.processKeys(keyMetaMap.keySet(),
                strategySpy.getNotCreatedSpecialOrderData(keyMetaMap)), user);

        Mockito.doReturn(
                new CreateSpecialOrderResponse()
                    .approvedItems(List.of(
                        new ApprovedSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111"))
                            .demandId(1L)
                    )))
            .when(replenishmentService).specialOrderRequestCreateAll(
                new CreateSpecialOrderRequest()
                    .ticketId(ticket)
                    .specialOrderItems(List.of(
                        new CreateSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111"))
                            .price(BigDecimal.TEN).orderType(SpecialOrderType.NEW)
                            .deliveryDate(LocalDate.now().plusDays(10))
                            .orderDateType(SpecialOrderDateType.LOG_PARAM)
                            .quantity(111L)
                            .quantum(1)
                    )));

        // mark ticket to be checked
        SessionUtils.check(session, ticket);
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        Assertions.assertThat(sskuStatusRepository.findByKey(111, "shop-sku-111"))
            .get().extracting(SskuStatus::getAvailability)
            .isEqualTo(OfferAvailability.PENDING);
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket))
            .contains("Обработка тикета завершена.\n" +
                    "((http://localhost:8080/#/availability/ssku/blocking_and_statuses?supplier_ids=111" +
                    "&shop_sku_search_text=shop-sku-111 Обработано 1 corefix ssku.))");
    }

    @Test
    public void simpleRunWithNewFile() {
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.INACTIVE, "")
        );

        var key = new ServiceOfferKey(111, "shop-sku-111");
        var ticket = facade.start(List.of(key), new MetaV2());

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);

        // Прикрепляем заполненный пользователем эксель
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            fromUserExcelComposer.processKeys(List.of(key),
                new HashMap<>()), user);

        SessionUtils.check(session, ticket);
        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        var metrics = economicMetricsRepository.findAll();
        Assertions.assertThat(metrics)
            .hasSize(1)
            .usingElementComparatorOnFields("ticket", "businessProcess")
            .containsOnly(businessProcessMetric(ticket, BackToPendingApproveStrategy.TYPE));
    }

    @Test
    public void checkEconomicMetrics() {
        sskuStatusRepository.save(
            sskuStatus(111, "shop-sku-111", OfferAvailability.INACTIVE, "comment1")
        );

        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111")
        );
        var meta = new MetaV2().setAuthor("author1").setDescription("description1");
        var ticket = facade.start(shopSkuKeys, meta);
        executor.run();
        var attachments = session.attachments().getAll(ticket);
        Assertions.assertThat(attachments.stream().collect(Collectors.toList()))
            .hasSize(1);

        var date = LocalDate.of(2022, 1, 30);
        Map<ServiceOfferKey, KeyMetaV2> keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new KeyMetaV2().addSpecialOrderData(
                List.of(
                    new SpecialOrderData(SOFINO_ID, 10L, null, null, 2, date, null)
                ))
        );

        // Прикрепляем криво заполненный пользователем с точки зрения Реплена эксель
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            fromUserExcelComposer.processKeys(keyMetaMap.keySet(),
                strategySpy.getNotCreatedSpecialOrderData(keyMetaMap)),
            user);
        SessionUtils.check(session, ticket);
        Mockito.doReturn(
                new CreateSpecialOrderResponse()
                    .approvedItems(List.of(
                        new ApprovedSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111"))
                            .demandId(113154600L))))
            .when(replenishmentService).specialOrderRequestCreateAll(
                new CreateSpecialOrderRequest()
                    .ticketId(ticket)
                    .specialOrderItems(List.of(
                        new CreateSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111"))
                            .price(BigDecimal.TEN).orderType(SpecialOrderType.NEW).deliveryDate(date)
                            .orderDateType(SpecialOrderDateType.TODAY).quantity(10L).quantum(2)
                    )));
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        var metrics = economicMetricsRepository.findAll();
        Assertions.assertThat(metrics)
            .usingElementComparatorOnFields("ticket", "businessProcess")
            .containsOnly(businessProcessMetric(ticket, BackToPendingApproveStrategyV2.TYPE));
        var whName = deepmindWarehouseRepository.getByIds(SOFINO_ID).get(0).getName();
        Assertions.assertThat(metrics.get(0).getData())
            .containsEntry(SHOP_SKU_KEY, "shop-sku-111")
            .containsEntry(SUPPLIER_ID_KEY, "111")
            .containsEntry(whName + WAREHOUSE_FIRST_SUPPLY_CNT_ENDING, "10")
            .containsEntry(whName + WAREHOUSE_ORDER_DATE_ENDING, "2022-01-30")
            .containsEntry(Headers.QUANT_KEY, "2");
    }

    @Test
    public void checkStepTest() {
        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333"),
            new ServiceOfferKey(444, "shop-sku-444"),
            new ServiceOfferKey(555, "shop-sku-555")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE, "comment2", null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.ACTIVE, "comment3", null),
            sskuStatus(444, "shop-sku-444", OfferAvailability.INACTIVE_TMP, "comment4", null),
            sskuStatus(555, "shop-sku-555", OfferAvailability.INACTIVE_TMP, NO_PURCHASE_PRICE.getLiteral(), null)
        );

        var ticket = facade.start(shopSkuKeys, new MetaV2());

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);

        //first run: wrong sskus
        Instant firstAttachCreatedAt = Instant.now().plusSeconds(1);
        SessionUtils.addExcelAttachment(session, ticket, "excel1.xlsx", firstAttachCreatedAt,
            createCorrectExcelFileWithLegend(List.of(new ServiceOfferKey(2, "b")),
                EnrichApproveToPendingExcelComposer.HEADERS), user);
        SessionUtils.check(session, ticket);

        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PREPROCESSED);
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket))
            .contains(MbocErrors.get().sskuUpdateNotMatched(new ServiceOfferKey(2, "b").toString()).toString());
        Mockito.verify(strategySpy, Mockito.times(1)).process(any());

        //second run: wrong excel
        Instant secondAttachCreatedAt = firstAttachCreatedAt.plusSeconds(10);
        SessionUtils.addExcelAttachment(session, ticket, "excel2.xlsx", secondAttachCreatedAt,
            createNotCorrectExcelFile(List.of(
                new ServiceOfferKey(222, "shop-sku-222"),
                new ServiceOfferKey(444, "shop-sku-444"),
                new ServiceOfferKey(555, "shop-sku-555")
            )),
            user
        );
        SessionUtils.check(session, ticket);

        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PREPROCESSED);
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket)).contains(
            MbocErrors.get().assortCommitExcelHeaderAdditionError("Bad header").toString()
        );

        //third run: correct excel data
        Instant thirdAttachCreatedAt = secondAttachCreatedAt.plusSeconds(10);
        SessionUtils.addExcelAttachment(session, ticket, "excel3.xlsx", thirdAttachCreatedAt,
            createCorrectExcelFileWithLegend(List.of(
                new ServiceOfferKey(222, "shop-sku-222"),
                new ServiceOfferKey(444, "shop-sku-444"),
                new ServiceOfferKey(555, "shop-sku-555")
            ), EnrichApproveToPendingExcelComposer.HEADERS),
            user
        );
        SessionUtils.check(session, ticket);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        var sortedBySupplierResult = sskuStatusRepository.find(shopSkuKeys)
            .stream()
            .sorted(Comparator.comparingInt(SskuStatus::getSupplierId))
            .collect(Collectors.toList());
        Assertions
            .assertThat(sortedBySupplierResult)
            .extracting(SskuStatus::getAvailability)
            .containsExactly(
                OfferAvailability.DELISTED,
                OfferAvailability.PENDING,
                OfferAvailability.ACTIVE,
                OfferAvailability.INACTIVE_TMP,
                OfferAvailability.INACTIVE_TMP
            );
    }

    @Test
    public void excelHeadersValidationTest() {
        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333"),
            new ServiceOfferKey(444, "shop-sku-444"),
            new ServiceOfferKey(555, "shop-sku-555")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE, "comment2", null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.ACTIVE, "comment3", null),
            sskuStatus(444, "shop-sku-444", OfferAvailability.INACTIVE_TMP, "comment4", null),
            sskuStatus(555, "shop-sku-555", OfferAvailability.INACTIVE_TMP, NO_PURCHASE_PRICE.getLiteral(), null)
        );

        var ticket = facade.start(shopSkuKeys, new MetaV2());

        //first run:add header
        Instant firstAttachCreatedAt = Instant.now().plusSeconds(1);
        SessionUtils.addExcelAttachment(session, ticket, "excel2.xlsx", firstAttachCreatedAt,
            createNotCorrectExcelFile(List.of(
                new ServiceOfferKey(222, "shop-sku-222"),
                new ServiceOfferKey(444, "shop-sku-444"),
                new ServiceOfferKey(555, "shop-sku-555")
            )),
            user
        );
        SessionUtils.check(session, ticket);

        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PREPROCESSED);
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket)).contains(
            MbocErrors.get().assortCommitExcelHeaderAdditionError("Bad header").toString()
        );

        //second run: deleted header
        var headers = new ArrayList<>(EnrichApproveToPendingExcelComposer.HEADERS);
        headers.remove(Headers.SHOP_SKU);
        Instant secondAttachCreatedAt = firstAttachCreatedAt.plusSeconds(10);
        SessionUtils.addExcelAttachment(session, ticket, "excel2.xlsx", secondAttachCreatedAt,
            createCorrectExcelFile(List.of(
                new ServiceOfferKey(222, "shop-sku-222"),
                new ServiceOfferKey(444, "shop-sku-444"),
                new ServiceOfferKey(555, "shop-sku-555")
            ), headers),
            user
        );
        SessionUtils.check(session, ticket);

        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PREPROCESSED);
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket)).contains(
            MbocErrors.get().assortCommitExcelHeaderDeletionError(SHOP_SKU_KEY).toString()
        );

        //third run: renamed header
        headers = new ArrayList<>(EnrichApproveToPendingExcelComposer.HEADERS);
        headers.remove(Headers.SHOP_SKU);
        var renamedHeader = SHOP_SKU_KEY + "_sth";
        headers.add(new Header(renamedHeader));
        Instant thirdAttachCreatedAt = secondAttachCreatedAt.plusSeconds(10);
        SessionUtils.addExcelAttachment(session, ticket, "excel2.xlsx", thirdAttachCreatedAt,
            createCorrectExcelFile(List.of(
                new ServiceOfferKey(222, "shop-sku-222"),
                new ServiceOfferKey(444, "shop-sku-444"),
                new ServiceOfferKey(555, "shop-sku-555")
            ), headers),
            user
        );
        SessionUtils.check(session, ticket);

        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PREPROCESSED);
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket)).contains(
            MbocErrors.get().assortCommitExcelHeaderRenameError(renamedHeader).toString()
        );

        //fourth run: correct excel data
        Instant fourthAttachCreatedAt = thirdAttachCreatedAt.plusSeconds(10);
        SessionUtils.addExcelAttachment(session, ticket, "excel3.xlsx", fourthAttachCreatedAt,
            createCorrectExcelFileWithLegend(List.of(
                new ServiceOfferKey(222, "shop-sku-222"),
                new ServiceOfferKey(444, "shop-sku-444"),
                new ServiceOfferKey(555, "shop-sku-555")
            ), EnrichApproveToPendingExcelComposer.HEADERS),
            user
        );
        SessionUtils.check(session, ticket);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        var sortedBySupplierResult = sskuStatusRepository.find(shopSkuKeys)
            .stream()
            .sorted(Comparator.comparingInt(SskuStatus::getSupplierId))
            .collect(Collectors.toList());
        Assertions
            .assertThat(sortedBySupplierResult)
            .extracting(SskuStatus::getAvailability)
            .containsExactly(
                OfferAvailability.DELISTED,
                OfferAvailability.PENDING,
                OfferAvailability.ACTIVE,
                OfferAvailability.INACTIVE_TMP,
                OfferAvailability.INACTIVE_TMP
            );
    }

    @Test
    public void noReopenHappens() {
        deepmindSupplierRepository.save(new Supplier().setId(1).setName("1"));
        categoryCachingService.addCategory(
            deepmindCategoryRepository.insert(new Category().setCategoryId(1L).setName("category1"))
        );
        deepmindMskuRepository.save(msku(1, 1));
        serviceOfferReplicaRepository.save(offer(1, "a", 1, 1));
        sskuStatusRepository.save(
            sskuStatus(1, "a", OfferAvailability.ACTIVE, "comment1")
        );

        AtomicBoolean processEndedWithError = new AtomicBoolean();
        Mockito.doAnswer(invoke -> {
            // first run is failed
            if (!processEndedWithError.get()) {
                ProcessRequest<ServiceOfferKey, MetaV2, ?> request = invoke.getArgument(0);
                // заполняем ошибки
                var meta = request.getMeta();
                meta.setParsingErrors(List.of(MbocErrors.get().invalidValue("a", "b")));

                processEndedWithError.set(true);
                return ProcessResponse.of(ProcessResponse.Status.NOT_OK, meta, request.getKeyMetaMap());
            } else {
                // on second call run real method
                return invoke.callRealMethod();
            }
        }).when(strategySpy).process(any());

        List<ServiceOfferKey> shopSkuList = List.of(new ServiceOfferKey(1, "a"));
        var ticket = facade.start(shopSkuList, new MetaV2());
        // запускаем в первый раз
        SessionUtils.check(session, ticket);
        executor.run();
        Mockito.verify(strategySpy, Mockito.times(1)).reopen(any());
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.TO_REOPEN);
        Assertions.assertThat(facade.findTicketStatus(ticket).getRetryCount()).isOne();

        // second run
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getRetryCount()).isEqualTo(2);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getRetryCount()).isEqualTo(3);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getRetryCount()).isEqualTo(4);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getRetryCount()).isEqualTo(5);

        // nothing to do
        Mockito.clearInvocations(strategySpy);
        executor.run();
        Mockito.verifyNoMoreInteractions(strategySpy);
    }

    @Test
    public void processStepTest() {
        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1"),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE, "comment2"),
            sskuStatus(333, "shop-sku-333", OfferAvailability.ACTIVE, "comment3"),
            sskuStatus(444, "shop-sku-444", OfferAvailability.ACTIVE, "В тикете БП этой позиции нет")
        );

        var ticket = facade.start(shopSkuKeys, new MetaV2());

        executor.run();

        Map<ServiceOfferKey, KeyMetaV2> keyMetaMap = Map.of(
            new ServiceOfferKey(222, "shop-sku-222"),
            new KeyMetaV2().addSpecialOrderData(
                List.of(
                    new SpecialOrderData(SOFINO_ID, 10L, null, null, 2, LocalDate.now().plusDays(10), null),
                    new SpecialOrderData(SAMARA_ID, 100L, null, null, 2, LocalDate.now(), null),
                    new SpecialOrderData(SAINT_PETERSBURG_ID, 90L, null, null, 2, LocalDate.now().plusDays(100), null)
                ))
        );

        //first run: wrong sskus
        Instant firstAttachCreatedAt = Instant.now().plusSeconds(1);
        var sskuKeys = new HashSet<>(keyMetaMap.keySet());
        sskuKeys.add(new ServiceOfferKey(444, "shop-sku-444"));
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            fromUserExcelComposer.processKeys(sskuKeys, strategySpy.getNotCreatedSpecialOrderData(keyMetaMap)), user);

        SessionUtils.check(session, ticket);
        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PREPROCESSED);
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket))
            .contains(MbocErrors.get().sskuUpdateNotMatched(new ServiceOfferKey(444, "shop-sku-444").toString())
                .toString());
        Mockito.verify(strategySpy, Mockito.times(1)).process(any());

        //second run: correct sskus
        SessionUtils.check(session, ticket);
        Instant secondAttachCreatedAt = firstAttachCreatedAt.plusSeconds(10);
        //approve only supplier 222
        // Прикрепляем заполненный пользователем эксель
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", secondAttachCreatedAt,
            fromUserExcelComposer.processKeys(keyMetaMap.keySet(),
                strategySpy.getNotCreatedSpecialOrderData(keyMetaMap)), user);

        Mockito.doReturn(new CreateSpecialOrderResponse().approvedItems(List.of(
                new ApprovedSpecialOrderItem()
                    .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000222.shop-sku-222"))
                    .demandId(1L),
                new ApprovedSpecialOrderItem()
                    .key(new SpecialOrderCreateKey().warehouseId(SAMARA_ID).ssku("000222.shop-sku-222"))
                    .demandId(1L),
                new ApprovedSpecialOrderItem()
                    .key(new SpecialOrderCreateKey().warehouseId(SAINT_PETERSBURG_ID).ssku("000222.shop-sku-222"))
                    .demandId(1L)
            ))
        ).when(replenishmentService).specialOrderRequestCreateAll(new CreateSpecialOrderRequest().ticketId(ticket)
            .specialOrderItems(List.of(
                new CreateSpecialOrderItem()
                    .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000222.shop-sku-222"))
                    .price(BigDecimal.TEN)
                    .orderType(SpecialOrderType.NEW)
                    .deliveryDate(LocalDate.now().plusDays(10))
                    .orderDateType(SpecialOrderDateType.LOG_PARAM)
                    .quantity(10L)
                    .quantum(2),
                new CreateSpecialOrderItem()
                    .key(new SpecialOrderCreateKey().warehouseId(SAINT_PETERSBURG_ID).ssku("000222.shop-sku-222"))
                    .price(BigDecimal.TEN)
                    .orderType(SpecialOrderType.NEW)
                    .deliveryDate(LocalDate.now().plusDays(100))
                    .orderDateType(SpecialOrderDateType.LOG_PARAM)
                    .quantity(90L)
                    .quantum(2),
                new CreateSpecialOrderItem()
                    .key(new SpecialOrderCreateKey().warehouseId(SAMARA_ID).ssku("000222.shop-sku-222"))
                    .price(BigDecimal.TEN)
                    .orderType(SpecialOrderType.NEW)
                    .deliveryDate(LocalDate.now())
                    .orderDateType(SpecialOrderDateType.TODAY)
                    .quantity(100L)
                    .quantum(2)
            )));

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        Mockito.verify(strategySpy, Mockito.times(2)).process(any());
        Mockito.verify(strategySpy, Mockito.times(1)).postprocess(any());

        var sortedBySupplierResult = sskuStatusRepository.find(shopSkuKeys)
            .stream()
            .sorted(Comparator.comparingInt(SskuStatus::getSupplierId))
            .collect(Collectors.toList());
        Assertions
            .assertThat(sortedBySupplierResult)
            .extracting(SskuStatus::getAvailability)
            .containsExactly(
                OfferAvailability.DELISTED,
                OfferAvailability.PENDING,
                OfferAvailability.ACTIVE);
    }

    @Test
    public void processStepHeadersNotMatchTest() {
        sskuStatusRepository.save(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1")
        );

        List<ServiceOfferKey> keys = List.of(new ServiceOfferKey(111, "shop-sku-111"));
        var ticket = facade.start(keys, new MetaV2());

        executor.run();
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            createNotCorrectExcelFile(keys), user);

        SessionUtils.check(session, ticket);
        executor.run();
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket)).contains(
            MbocErrors.get().assortCommitExcelHeaderAdditionError("Bad header").toString()
        );
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PREPROCESSED);
    }

    @Test
    public void processStepClosedWithoutResolutionTest() {
        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.INACTIVE_TMP, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE_TMP, "comment2", null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.INACTIVE_TMP, "comment3", null)
        );

        var ticket = facade.start(shopSkuKeys, new MetaV2());
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.NEW);
        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);
        // закрываем без резолюции
        SessionUtils.close(session, ticket);
        executor.run();
        // проверяем что после закрытия без резолюции тикет не уходит в CLOSED, а остается в ENRICHED
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);
    }

    @Test
    public void closeWithWontDoWillChangeToPending() {
        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE, "comment2", null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.ACTIVE, "comment3", null)
        );

        var ticket = facade.start(shopSkuKeys, new MetaV2());

        SessionUtils.close(session, ticket, TicketResolution.WONT_DO);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        // nothing changed
        Assertions.assertThat(sskuStatusRepository.findByKey(111, "shop-sku-111").get().getAvailability())
            .isEqualTo(OfferAvailability.DELISTED);
        Assertions.assertThat(sskuStatusRepository.findByKey(222, "shop-sku-222").get().getAvailability())
            .isEqualTo(OfferAvailability.INACTIVE);
        Assertions.assertThat(sskuStatusRepository.findByKey(333, "shop-sku-333").get().getAvailability())
            .isEqualTo(OfferAvailability.ACTIVE);
    }

    @Test
    public void closeWithWontFixBeforeEnrichWillBeNoChanges() {
        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE, "comment2", null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.ACTIVE, "comment3", null)
        );

        var ticket = facade.start(shopSkuKeys, new MetaV2());

        // тикет отменен после создания
        SessionUtils.close(session, ticket, TicketResolution.WONT_FIX);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        Assertions.assertThat(session.attachments().getAll(ticket).stream().collect(Collectors.toList())).isEmpty();

        // no changes performed
        Assertions.assertThat(sskuStatusRepository.findByKey(111, "shop-sku-111").get().getAvailability())
            .isEqualTo(OfferAvailability.DELISTED);
        Assertions.assertThat(sskuStatusRepository.findByKey(222, "shop-sku-222").get().getAvailability())
            .isEqualTo(OfferAvailability.INACTIVE);
        Assertions.assertThat(sskuStatusRepository.findByKey(333, "shop-sku-333").get().getAvailability())
            .isEqualTo(OfferAvailability.ACTIVE);
    }

    @Test
    public void closeWithWontFixAfterEnrichWillBeNoChanges() {
        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE, "comment2", null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.ACTIVE, "comment3", null)
        );

        var ticket = facade.start(shopSkuKeys, new MetaV2());
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);

        // тикет отменен после обогащения
        SessionUtils.close(session, ticket, TicketResolution.WONT_FIX);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        Assertions.assertThat(session.attachments().getAll(ticket).stream().collect(Collectors.toList())).hasSize(1);

        // no changes performed
        Assertions.assertThat(sskuStatusRepository.findByKey(111, "shop-sku-111").get().getAvailability())
            .isEqualTo(OfferAvailability.DELISTED);
        Assertions.assertThat(sskuStatusRepository.findByKey(222, "shop-sku-222").get().getAvailability())
            .isEqualTo(OfferAvailability.INACTIVE);
        Assertions.assertThat(sskuStatusRepository.findByKey(333, "shop-sku-333").get().getAvailability())
            .isEqualTo(OfferAvailability.ACTIVE);
    }

    @Test
    public void validateSskuNotMatch() {
        String ticket = "TEST-1";
        String type = "test";
        ticketRepository.save(new TrackerApproverTicketRawStatus(ticket, type, TicketState.NEW));

        var sskuInRepo = List.of(
            new ServiceOfferKey(1, "a"),
            new ServiceOfferKey(2, "b"));

        var trackerApproverData = sskuInRepo.stream()
            .map(key -> new TrackerApproverRawData()
                .setTicket(ticket)
                .setKey(JsonWrapper.fromObject(key))
                .setType(type)
            ).collect(Collectors.toList());

        dataRepository.save(trackerApproverData);

        var specialOrderData = List.of(
            new SpecialOrderData(SOFINO_ID, 10L, null, null, 2, LocalDate.now().plusDays(10), null),
            new SpecialOrderData(SAMARA_ID, 100L, null, null, 2, LocalDate.now(), null),
            new SpecialOrderData(SAINT_PETERSBURG_ID, 90L, null, null, 2, LocalDate.now().plusDays(100), null)
        );

        var sskuToTest = Map.of(
            new ServiceOfferKey(1, "a"), specialOrderData,
            new ServiceOfferKey(3, "c"), specialOrderData);
        TrackerApproverValidationResultMap<ServiceOfferKey, List<SpecialOrderData>> result =
            strategySpy.validateExcelReadingResult(sskuToTest, sskuInRepo);
        Assertions.assertThat(result.getErrors().size()).isEqualTo(1);
        Assertions.assertThat(List.of(new ServiceOfferKey(1, "a")))
            .isEqualTo(result.getValidShopSkuKeyToSpecialOrderDataMap().keySet().stream().collect(Collectors.toList()));
    }

    @Test
    public void validateSskuAllMatch() {
        String ticket = "TEST-1";
        String type = "test";
        ticketRepository.save(new TrackerApproverTicketRawStatus(ticket, type, TicketState.NEW));

        var sskuInRepo = List.of(
            new ServiceOfferKey(1, "a"),
            new ServiceOfferKey(2, "b"));

        var trackerApproverData = sskuInRepo.stream()
            .map(key -> new TrackerApproverRawData()
                .setTicket(ticket)
                .setKey(JsonWrapper.fromObject(key))
                .setType(type)
            ).collect(Collectors.toList());

        dataRepository.save(trackerApproverData);

        var specialOrderData = List.of(
            new SpecialOrderData(SOFINO_ID, 10L, null, null, 2, LocalDate.now().plusDays(10), null),
            new SpecialOrderData(SAMARA_ID, 100L, null, null, 2, LocalDate.now(), null),
            new SpecialOrderData(SAINT_PETERSBURG_ID, 90L, null, null, 2, LocalDate.now().plusDays(100), null)
        );

        var sskuToTest = Map.of(
            new ServiceOfferKey(1, "a"), specialOrderData,
            new ServiceOfferKey(2, "b"), specialOrderData);

        TrackerApproverValidationResultMap<ServiceOfferKey, List<SpecialOrderData>> result =
            strategySpy.validateExcelReadingResult(sskuToTest, sskuInRepo);
        Assertions.assertThat(result.getErrors().size()).isZero();
        Assertions.assertThat(result.getValidShopSkuKeyToSpecialOrderDataMap().keySet())
            .containsExactlyInAnyOrder(sskuToTest.keySet().toArray(new ServiceOfferKey[0]));
    }

    @Test // DEEPMIND-2638
    public void saveEconomicMetricsEvenIfColumnNameIsEmpty() {
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.ACTIVE, "")
        );

        var key = new ServiceOfferKey(111, "shop-sku-111");

        var ticket = facade.start(List.of(key), new MetaV2());

        executor.run();

        // Нам надо создать файл, у которого будет +1 новая пустая колонка
        var line = 2;
        var headerSize = headerList.size();
        //        var excelFile = createCorrectExcelFileWithLegend(List.of(new ServiceOfferKey(1, "a")));
        var excelFile = fromUserExcelComposer.processKeys(List.of(key), new HashMap<>());
        // Проверяем, что line - эта та линия, на которой данные находятся. Это важно.
        DeepmindAssertions.assertThat(excelFile)
            .containsValue(line, SHOP_SKU_KEY, "shop-sku-111")
            .hasHeaderSize(headerSize);
        // Добавляем значение при пустой колонке
        excelFile = excelFile.toBuilder()
            .setValue(line, headerSize + 1, "3.1415926535")
            .build();

        SessionUtils.check(session, ticket);
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            excelFile, user);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        // one more run
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        var metrics = economicMetricsRepository.findAll();
        Assertions.assertThat(metrics)
            .hasSize(1)
            .usingElementComparatorOnFields("ticket", "businessProcess")
            .containsOnly(businessProcessMetric(ticket, BackToPendingApproveStrategy.TYPE));
    }

    @Test
    public void processStepEndsWithOneDeclinedItemOnDifferentWarehouses() {
        sskuStatusRepository.save(
            sskuStatus(111, "shop-sku-111", OfferAvailability.INACTIVE, "comment1")
        );

        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111")
        );
        var meta = new MetaV2().setAuthor("author1").setDescription("description1");
        var ticket = facade.start(shopSkuKeys, meta);
        executor.run();

        Map<ServiceOfferKey, KeyMetaV2> keyMetaMap = shopSkuKeys.stream().collect(
            Collectors.toMap(
                Function.identity(), ssku -> new KeyMetaV2().addSpecialOrderData(
                    List.of(
                        new SpecialOrderData(ROSTOV_ID, 20L, null, BigDecimal.TEN,
                            1, LocalDate.now(), null),
                        new SpecialOrderData(SOFINO_ID, 2L, null, BigDecimal.TEN,
                            1, LocalDate.now(), null)
                    ))));

        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            fromUserExcelComposer.processKeys(shopSkuKeys, strategySpy.getNotCreatedSpecialOrderData(keyMetaMap)),
            user);

        SessionUtils.check(session, ticket);

        Mockito.doReturn(new CreateSpecialOrderResponse().declinedItems(List.of(
                new DeclinedSpecialOrderItem()
                    .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111"))
                    .error("отсутствуют логистические параметры"),
                new DeclinedSpecialOrderItem()
                    .key(new SpecialOrderCreateKey().warehouseId(ROSTOV_ID).ssku("000111.shop-sku-111"))
                    .error("отсутствуют логистические параметры")
            ))
        ).when(replenishmentService).specialOrderRequestCreateAll(
            new CreateSpecialOrderRequest().ticketId(ticket)
                .specialOrderItems(List.of(
                    new CreateSpecialOrderItem().key(
                            new SpecialOrderCreateKey().warehouseId(ROSTOV_ID).ssku("000111.shop-sku-111")
                        ).price(BigDecimal.TEN).orderType(SpecialOrderType.NEW).deliveryDate(LocalDate.now())
                        .orderDateType(SpecialOrderDateType.TODAY).quantity(20L).quantum(1),
                    new CreateSpecialOrderItem().key(
                            new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111")
                        ).price(BigDecimal.TEN).orderType(SpecialOrderType.NEW).deliveryDate(LocalDate.now())
                        .orderDateType(SpecialOrderDateType.TODAY).quantity(2L).quantum(1)
                )));

        executor.run();
        var issue = facade.findTicketStatus(ticket);
        Assertions.assertThat(issue.getState()).isEqualTo(TicketState.PROCESSED);
        Assertions.assertThat(issue.getLastException()).isNull();
    }

    @Test
    public void noSpecialOrdersToCreateLeftTest() {
        sskuStatusRepository.save(
            sskuStatus(111, "shop-sku-111", OfferAvailability.ACTIVE, "comment1")
        );

        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111")
        );
        var meta = new MetaV2().setAuthor("author1").setDescription("description1");
        var ticket = facade.start(shopSkuKeys, meta);
        executor.run();

        Map<ServiceOfferKey, KeyMetaV2> keyMetaMap = shopSkuKeys.stream().collect(
            Collectors.toMap(
                Function.identity(), ssku -> new KeyMetaV2().addSpecialOrderData(
                    List.of(
                        new SpecialOrderData(SOFINO_ID, 2L, null, BigDecimal.TEN, 1, LocalDate.now(), null),
                        new SpecialOrderData(ROSTOV_ID, 20L, null, BigDecimal.TEN, 1, LocalDate.now(), null)
                    ))));

        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            fromUserExcelComposer.processKeys(shopSkuKeys, strategySpy.getNotCreatedSpecialOrderData(keyMetaMap)),
            user);

        SessionUtils.check(session, ticket);

        executor.run();
        var issue = facade.findTicketStatus(ticket);
        Assertions.assertThat(issue.getState()).isEqualTo(TicketState.CLOSED);
        Assertions.assertThat(issue.getLastException()).isNull();
    }

    @Test
    public void ticketMovedToWaitingForActivationAfterNeedCorrectionNoSskuStatusesChanged() {
        sskuStatusRepository.save(
            sskuStatus(111, "shop-sku-111", OfferAvailability.INACTIVE, "comment1")
        );

        List<ServiceOfferKey> keys = List.of(new ServiceOfferKey(111, "shop-sku-111"));
        var ticket = facade.start(keys, new MetaV2());

        executor.run();

        var excelFile = createCorrectExcelFile(keys,
            List.of(Headers.SHOP_SKU, Headers.SUPPLIER_ID)).toBuilder()
            .setValue(3, SHOP_SKU_KEY, "222")
            .setValue(3, SUPPLIER_ID_KEY, "shop-sku-222")
            .build();
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            excelFile, user);
        SessionUtils.check(session, ticket);
        executor.run();
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket)).contains(
            // не стал перечилсять все колонки, факта что каких-то нет достаточно
            MbocErrors.get().assortCommitExcelHeaderDeletionError("").toString()
        );
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PREPROCESSED);
        Assertions.assertThat(session.issues().get(ticket).getStatus().getKey())
            .isEqualTo(TicketStatus.NEED_CORRECTION.getStatusAliases().get(0));

        Map<ServiceOfferKey, KeyMetaV2> keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new KeyMetaV2().addSpecialOrderData(
                List.of(
                    new SpecialOrderData(SOFINO_ID, 111L, null, null, 1, LocalDate.now().plusDays(10), null)
                ))
        );

        // Прикрепляем заполненный пользователем эксель
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            fromUserExcelComposer.processKeys(keyMetaMap.keySet(),
                strategySpy.getNotCreatedSpecialOrderData(keyMetaMap)), user);

        Mockito.doReturn(
                new CreateSpecialOrderResponse()
                    .approvedItems(List.of(
                        new ApprovedSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111"))
                            .demandId(1L)
                    )))
            .when(replenishmentService).specialOrderRequestCreateAny(
                new CreateSpecialOrderRequest()
                    .ticketId(ticket)
                    .specialOrderItems(List.of(
                        new CreateSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111"))
                            .price(BigDecimal.TEN).orderType(SpecialOrderType.NEW)
                            .deliveryDate(LocalDate.now().plusDays(10))
                            .orderDateType(SpecialOrderDateType.LOG_PARAM)
                            .quantity(111L)
                            .quantum(1)
                    )));

        // пользователь согласно флоу сдвинул тикет в "Ожидает активации"
        SessionUtils.awaitsActivation(session, ticket);

        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        Assertions.assertThat(sskuStatusRepository.findByKey(111, "shop-sku-111"))
            .get().extracting(SskuStatus::getAvailability)
            .isEqualTo(OfferAvailability.PENDING);
    }

    @Test
    public void userDateInputTest() {
        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE, "comment2", null)
        );

        var ticket = facade.start(shopSkuKeys, new MetaV2());
        executor.run();

        // user excel with date in format dd.MM.yyyy (01.12.2022, 21.12.2022)
        var userExcel = getExcelFrom("excel_files/pending_date_test.xlsx");

        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now(), userExcel, user);
        SessionUtils.check(session, ticket);

        Mockito.doReturn(new CreateSpecialOrderResponse().declinedItems(List.of(
                new DeclinedSpecialOrderItem()
                    .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111"))
                    .error("Ошибка создания потребности")
            ))
        ).when(replenishmentService).specialOrderRequestCreateAll(any());

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PROCESSED);
        Assertions.assertThat(facade.findByKeys(shopSkuKeys).stream()
                .flatMap(data -> data.getMeta().getSpecialOrderData().stream()
                    .map(spData -> spData.getOrderDate())).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(
                LocalDate.of(2026, Month.DECEMBER, 1),
                LocalDate.of(2026, Month.DECEMBER, 21)
            );
    }

    @Test
    public void assortmentSskuActivationTest() {
        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(901, "shop-sku-111"),
            new ServiceOfferKey(902, "shop-sku-222"),
            new ServiceOfferKey(903, "shop-sku-333"),
            new ServiceOfferKey(904, "shop-sku-444"),
            new ServiceOfferKey(905, "shop-sku-555")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(901, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(902, "shop-sku-222", OfferAvailability.INACTIVE, "comment2", null),
            sskuStatus(903, "shop-sku-333", OfferAvailability.INACTIVE, "comment3", null),
            sskuStatus(904, "shop-sku-444", OfferAvailability.INACTIVE, "comment4", null),
            sskuStatus(905, "shop-sku-555", OfferAvailability.INACTIVE, "comment5", null),

            sskuStatus(901, "assort-sku-1", OfferAvailability.DELISTED, "comment5", null),
            sskuStatus(902, "assort-sku-2", OfferAvailability.INACTIVE, "comment5", null),
            sskuStatus(903, "assort-sku-3", OfferAvailability.PENDING, "comment5", null),
            sskuStatus(904, "assort-sku-4", OfferAvailability.ACTIVE, "comment5", null),
            sskuStatus(905, "assort-sku-5", OfferAvailability.INACTIVE, "comment5", null)
        );
        assortSskuRepository.save(
            new AssortSsku(901, "shop-sku-111", "assort-sku-1", null),
            new AssortSsku(902, "shop-sku-222", "assort-sku-2", null),
            new AssortSsku(903, "shop-sku-333", "assort-sku-3", null),
            new AssortSsku(904, "shop-sku-444", "assort-sku-4", null),
            new AssortSsku(905, "shop-sku-555", "assort-sku-5", null)
        );
        List<Long> mskuIds = List.of(99901L, 99902L, 99903L, 99904L, 99905L);
        mskuStatusRepository.save(
            mskuStatus(99901, MskuStatusValue.REGULAR),
            mskuStatus(99902, MskuStatusValue.END_OF_LIFE),
            mskuStatus(99903, MskuStatusValue.ARCHIVE),
            mskuStatus(99904, MskuStatusValue.PRE_NPD),
            mskuStatus(99905, MskuStatusValue.SEASONAL)
        );

        var meta = new MetaV2().setAuthor("author1").setDescription("description1");
        var ticket = facade.start(shopSkuKeys, meta);
        executor.run();

        Map<ServiceOfferKey, KeyMetaV2> keyMetaMap = shopSkuKeys.stream().collect(
            Collectors.toMap(
                Function.identity(), ssku -> new KeyMetaV2().addSpecialOrderData(
                    List.of(
                        new SpecialOrderData(SOFINO_ID, 2L, null, null, 1, LocalDate.now(), null)
                    ))));

        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            fromUserExcelComposer.processKeys(shopSkuKeys, strategySpy.getNotCreatedSpecialOrderData(keyMetaMap)),
            user);

        SessionUtils.check(session, ticket);

        Mockito.doReturn(new CreateSpecialOrderResponse().approvedItems(List.of(
                new ApprovedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000901.shop-sku-111")
                ).demandId(113154600L),
                new ApprovedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000902.shop-sku-222")
                ).demandId(113154600L),
                new ApprovedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000903.shop-sku-333")
                ).demandId(113154600L),
                new ApprovedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000904.shop-sku-444")
                ).demandId(113154600L),
                new ApprovedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000905.shop-sku-555")
                ).demandId(113154600L)
            ))
        ).when(replenishmentService).specialOrderRequestCreateAll(any());

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        Assertions.assertThat(mskuStatusRepository.getByIds(mskuIds))
            .usingElementComparatorOnFields("marketSkuId", "mskuStatus")
            .containsExactlyInAnyOrder(
                mskuStatus(99901, MskuStatusValue.REGULAR),
                mskuStatus(99902, MskuStatusValue.REGULAR),
                mskuStatus(99903, MskuStatusValue.REGULAR),
                mskuStatus(99904, MskuStatusValue.PRE_NPD),
                mskuStatus(99905, MskuStatusValue.SEASONAL)
            );

        var assortList = assortSskuRepository.findByIds(shopSkuKeys).stream()
            .map(assortSssku -> new ServiceOfferKey(assortSssku.getSupplierId(), assortSssku.getAssortSsku()))
            .collect(Collectors.toList());
        Assertions.assertThat(sskuStatusRepository.find(assortList))
            .usingElementComparatorOnFields("supplierId", "shopSku", "availability")
            .containsExactlyInAnyOrder(
                sskuStatus(901, "assort-sku-1", OfferAvailability.PENDING, "comment5", null),
                sskuStatus(902, "assort-sku-2", OfferAvailability.PENDING, "comment5", null),
                sskuStatus(903, "assort-sku-3", OfferAvailability.PENDING, "comment5", null),
                sskuStatus(904, "assort-sku-4", OfferAvailability.ACTIVE, "comment5", null),
                sskuStatus(905, "assort-sku-5", OfferAvailability.PENDING, "comment5", null)
            );
    }

    @Test
    public void inactiveMskuAreBeingActivated() {
        List<ServiceOfferKey> serviceOfferKeys = List.of(
            new ServiceOfferKey(901, "shop-sku-111"),
            new ServiceOfferKey(902, "shop-sku-222")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(901, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(902, "shop-sku-222", OfferAvailability.INACTIVE, "comment2", null)
        );
        List<Long> mskuIds = List.of(99901L, 99902L, 123456L);
        mskuStatusRepository.save(
            mskuStatus(99901, MskuStatusValue.END_OF_LIFE),
            mskuStatus(99902, MskuStatusValue.ARCHIVE),
            mskuStatus(123456L, MskuStatusValue.END_OF_LIFE)
        );

        var meta = new MetaV2().setAuthor("author1").setDescription("description1");
        var ticket = facade.start(serviceOfferKeys, meta);
        executor.run();

        Map<ServiceOfferKey, KeyMetaV2> keyMetaMap = serviceOfferKeys.stream().collect(
            Collectors.toMap(
                Function.identity(), ssku -> new KeyMetaV2().addSpecialOrderData(
                    List.of(
                        new SpecialOrderData(SOFINO_ID, 2L, null, null, 1, LocalDate.now(), null)
                    ))));

        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            fromUserExcelComposer.processKeys(serviceOfferKeys, strategySpy.getNotCreatedSpecialOrderData(keyMetaMap)),
            user);

        SessionUtils.check(session, ticket);

        Mockito.doReturn(new CreateSpecialOrderResponse().approvedItems(List.of(
                new ApprovedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000901.shop-sku-111")
                ).demandId(113154600L),
                new ApprovedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000902.shop-sku-222")
                ).demandId(113154600L)
            ))
        ).when(replenishmentService).specialOrderRequestCreateAll(any());

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        Assertions.assertThat(mskuStatusRepository.getByIds(mskuIds))
            .usingElementComparatorOnFields("marketSkuId", "mskuStatus")
            .containsExactlyInAnyOrder(
                mskuStatus(99901, MskuStatusValue.REGULAR),
                mskuStatus(99902, MskuStatusValue.REGULAR),
                mskuStatus(123456L, MskuStatusValue.END_OF_LIFE)
            );
    }

    @Test
    public void alreadyActiveMskuAreNotBeingChanged() {
        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(901, "shop-sku-111"),
            new ServiceOfferKey(902, "shop-sku-222"),
            new ServiceOfferKey(903, "shop-sku-333"),
            new ServiceOfferKey(904, "shop-sku-444"),
            new ServiceOfferKey(905, "shop-sku-555")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(901, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(902, "shop-sku-222", OfferAvailability.INACTIVE, "comment2", null),
            sskuStatus(903, "shop-sku-333", OfferAvailability.INACTIVE, "comment3", null),
            sskuStatus(904, "shop-sku-444", OfferAvailability.INACTIVE, "comment4", null),
            sskuStatus(905, "shop-sku-555", OfferAvailability.INACTIVE, "comment5", null)
        );
        List<Long> mskuIds = List.of(99901L, 99902L, 99903L, 99904L, 99905L);
        mskuStatusRepository.save(
            mskuStatus(99901, MskuStatusValue.PRE_NPD),
            mskuStatus(99902, MskuStatusValue.NPD).setNpdStartDate(LocalDate.now()),
            mskuStatus(99903, MskuStatusValue.REGULAR),
            mskuStatus(99904, MskuStatusValue.SEASONAL),
            mskuStatus(99905, MskuStatusValue.IN_OUT)
        );

        var meta = new MetaV2().setAuthor("author1").setDescription("description1");
        var ticket = facade.start(shopSkuKeys, meta);
        executor.run();

        Map<ServiceOfferKey, KeyMetaV2> keyMetaMap = shopSkuKeys.stream().collect(
            Collectors.toMap(
                Function.identity(), ssku -> new KeyMetaV2().addSpecialOrderData(
                    List.of(
                        new SpecialOrderData(SOFINO_ID, 2L, null, null, 1, LocalDate.now(), null)
                    ))));

        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            fromUserExcelComposer.processKeys(shopSkuKeys, strategySpy.getNotCreatedSpecialOrderData(keyMetaMap)),
            user);

        SessionUtils.check(session, ticket);

        Mockito.doReturn(new CreateSpecialOrderResponse().approvedItems(List.of(
                new ApprovedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000901.shop-sku-111")
                ).demandId(113154600L),
                new ApprovedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000902.shop-sku-222")
                ).demandId(113154600L),
                new ApprovedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000903.shop-sku-333")
                ).demandId(113154600L),
                new ApprovedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000904.shop-sku-444")
                ).demandId(113154600L),
                new ApprovedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000905.shop-sku-555")
                ).demandId(113154600L)
            ))
        ).when(replenishmentService).specialOrderRequestCreateAll(any());

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        Assertions.assertThat(mskuStatusRepository.getByIds(mskuIds))
            .usingElementComparatorOnFields("marketSkuId", "mskuStatus")
            .containsExactlyInAnyOrder(
                mskuStatus(99901, MskuStatusValue.PRE_NPD),
                mskuStatus(99902, MskuStatusValue.NPD),
                mskuStatus(99903, MskuStatusValue.REGULAR),
                mskuStatus(99904, MskuStatusValue.SEASONAL),
                mskuStatus(99905, MskuStatusValue.IN_OUT)
            );
    }

    @Test
    public void inactiveSeasonalMskuActivatedAsSeasonal() {
        List<ServiceOfferKey> serviceOfferKeys = List.of(
            new ServiceOfferKey(901, "shop-sku-111"),
            new ServiceOfferKey(902, "shop-sku-222")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(901, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(902, "shop-sku-222", OfferAvailability.INACTIVE, "comment2", null)
        );
        List<Long> mskuIds = List.of(99901L, 99902L);

        mskuStatusRepository.save(
            mskuStatus(99901, MskuStatusValue.END_OF_LIFE),
            mskuStatus(99902, MskuStatusValue.END_OF_LIFE)
        );

        //deleting previous offer because it's contains regular season
        serviceOfferReplicaRepository.delete(new ServiceOfferKey(901, "shop-sku-111"));
        serviceOfferReplicaRepository.save(offer(901, "shop-sku-111", 99901, 1L));
        makeCategorySeasonal(1L);

        var meta = new MetaV2().setAuthor("author1").setDescription("description1");
        var ticket = facade.start(serviceOfferKeys, meta);
        executor.run();

        Map<ServiceOfferKey, KeyMetaV2> keyMetaMap = serviceOfferKeys.stream().collect(
            Collectors.toMap(
                Function.identity(), ssku -> new KeyMetaV2().addSpecialOrderData(
                    List.of(
                        new SpecialOrderData(SOFINO_ID, 2L, null, null, 1, LocalDate.now(), null)
                    ))));

        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            fromUserExcelComposer.processKeys(serviceOfferKeys, strategySpy.getNotCreatedSpecialOrderData(keyMetaMap)),
            user);

        SessionUtils.check(session, ticket);

        Mockito.doReturn(new CreateSpecialOrderResponse().approvedItems(List.of(
                new ApprovedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000901.shop-sku-111")
                ).demandId(113154600L),
                new ApprovedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000902.shop-sku-222")
                ).demandId(113154600L)
            ))
        ).when(replenishmentService).specialOrderRequestCreateAll(any());

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        Assertions.assertThat(mskuStatusRepository.getByIds(mskuIds))
            .usingElementComparatorOnFields("marketSkuId", "mskuStatus")
            .containsExactlyInAnyOrder(
                mskuStatus(99901, MskuStatusValue.SEASONAL),
                mskuStatus(99902, MskuStatusValue.REGULAR)
            );
    }
}

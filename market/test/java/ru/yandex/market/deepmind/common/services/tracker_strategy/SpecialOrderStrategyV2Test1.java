//CHECKSTYLE:OFF
package ru.yandex.market.deepmind.common.services.tracker_strategy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepmind.common.category.models.Category;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.AssortSsku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.EnrichSpecialOrderExcelComposer;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.Header;
import ru.yandex.market.deepmind.common.services.tracker_approver.excel.Headers;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.KeyMetaV2;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.MetaV2;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.SpecialOrderData;
import ru.yandex.market.deepmind.common.utils.SessionUtils;
import ru.yandex.market.deepmind.tracker_approver.pojo.ProcessRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.ProcessResponse;
import ru.yandex.market.deepmind.tracker_approver.pojo.StartRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.TicketState;
import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.replenishment.autoorder.openapi.client.ApiException;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.ApproveSpecialOrderRequest;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.ApproveSpecialOrderResponse;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.ApprovedSpecialOrderItem;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.DeclineSpecialOrderRequest;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.DeclinedSpecialOrderItem;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.MessageDTO;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.SpecialOrderCreateKey;
import ru.yandex.market.replenishment.autoorder.openapi.client.model.SpecialOrderType;

import static org.mockito.ArgumentMatchers.any;
import static ru.yandex.market.deepmind.common.pojo.SskuStatusReason.NO_PURCHASE_PRICE;
import static ru.yandex.market.deepmind.common.pojo.SskuStatusReason.UNDER_CONSIDERATION;
import static ru.yandex.market.deepmind.common.pojo.SskuStatusReason.WAITING_FOR_ENTER;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.EKATERINBURG_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.ExcelComposer.WAREHOUSE_FIRST_SUPPLY_CNT_ENDING;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.ExcelComposer.WAREHOUSE_ORDER_DATE_ENDING;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.Headers.QUANT_KEY;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.Headers.SHOP_SKU;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.Headers.SHOP_SKU_KEY;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.Headers.SUPPLIER_ID;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.Headers.SUPPLIER_ID_KEY;

public class SpecialOrderStrategyV2Test1 extends SpecialOrderStrategyBaseTestClass {

    @Test
    public void startStepTest() {
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.ACTIVE, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE_TMP, "comment2", null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.INACTIVE_TMP, WAITING_FOR_ENTER.getLiteral(), null)
        );
        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        );
        var meta = new MetaV2().setAuthor("author1").setDescription("description1");
        var ticketKey = facade.start(shopSkuKeys, meta);
        Assertions.assertThat(ticketKey).isEqualTo("TEST-1");
        Assertions.assertThat(session.issues().get(ticketKey).getComponents())
            .extracting(v -> v.load().getName())
            .containsExactlyInAnyOrder("Спец.закупка", "1P");
        Assertions
            .assertThat(session.issues().get(ticketKey).getSummary())
            .contains("Заявка на спец. заказ");
        var description = session.issues().get(ticketKey).getDescription();
        Assertions
            .assertThat(description.get())
            .contains(meta.getDescription(), "Прошу согласовать спец. заказ.",
                "shop-sku-222", "shop-sku-333")
            .contains(meta.getDescription(), "* ((http://localhost:8080/#/availability/" +
                "ssku/blocking_and_statuses?shop_sku_keys=111:shop-sku-111," +
                "222:shop-sku-222,333:shop-sku-333 Ссылка на все 3 corefix ssku))");
        Assertions
            .assertThat(sskuStatusRepository.find(shopSkuKeys))
            .extracting(SskuStatus::getComment)
            .containsExactlyInAnyOrder(
                "comment1",
                "comment2",
                UNDER_CONSIDERATION.getLiteral()
            );
    }

    @Test
    public void enrichStepTest() {
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE_TMP, "comment2", null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.INACTIVE_TMP, UNDER_CONSIDERATION.getLiteral(), null)
        );

        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        );
        var meta = new MetaV2().setAuthor("author1").setDescription("description1");
        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 5L, SpecialOrderType.NEW,
                        BigDecimal.valueOf(1006.0), 1, null, null)
                ),
            new ServiceOfferKey(222, "shop-sku-222"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(TOMILINO_ID, 5L, SpecialOrderType.SEASONAL,
                        BigDecimal.valueOf(1000.0), 1, null, null)
                ),
            new ServiceOfferKey(333, "shop-sku-333"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(ROSTOV_ID, 5L, SpecialOrderType.LOT,
                        BigDecimal.valueOf(100.0), 1, LocalDate.of(2022, 1, 12), null)
                )
        );
        var ticket = facade.start(new StartRequest(shopSkuKeys, meta, keyMetaMap));
        facade.enrich(ticket);
        var attachments = session.attachments().getAll(ticket);
        Assertions.assertThat(attachments.stream().collect(Collectors.toList()))
            .hasSize(1);
        Assertions.assertThat(session.issues().getSummonees(ticket))
            .isEmpty(); // because dev environment
    }

    @Test
    public void simpleRun() {
        sskuStatusRepository.save(sskuStatus(111, "shop-sku-111", OfferAvailability.ACTIVE, "comment1", null));
        var shopSkus = List.of(new ServiceOfferKey(111, "shop-sku-111"));
        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 5L, SpecialOrderType.LOT,
                        BigDecimal.valueOf(100.0), 1, LocalDate.now().minusDays(1), null))
                .addSpecialOrderData(
                    new SpecialOrderData(TOMILINO_ID, 18L, SpecialOrderType.LOT,
                        BigDecimal.valueOf(1000.0), 1, LocalDate.now().minusDays(8), null))
        );
        var ticket = facade.start(new StartRequest<>(shopSkus, new MetaV2(), keyMetaMap));

        SessionUtils.check(session, ticket);

        Mockito.doReturn(new ApproveSpecialOrderResponse()
                .approvedItems(List.of(
                    new ApprovedSpecialOrderItem()
                        .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111"))
                        .demandId(113154600L),
                    new ApprovedSpecialOrderItem()
                        .key(new SpecialOrderCreateKey().warehouseId(TOMILINO_ID).ssku("000111.shop-sku-111"))
                        .demandId(113154600L))
                ))
            .when(replenishmentService).specialOrderRequestApproveAll(
                new ApproveSpecialOrderRequest()
                    .ticketId(ticket)
                    .keys(List.of(
                        new SpecialOrderCreateKey().warehouseId(TOMILINO_ID).ssku("000111.shop-sku-111"),
                        new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111")
                    ))
            );

        Mockito.doReturn(
                new MessageDTO()
            )
            .when(replenishmentService).specialOrderRequestDeclineRest(new DeclineSpecialOrderRequest()
                .ticketId(ticket)
            );

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        // one more run
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket))
            .contains("Обработка тикета завершена.\n" +
                "((http://localhost:8080/#/availability/ssku/blocking_and_statuses?supplier_ids=111" +
                "&shop_sku_search_text=shop-sku-111 Обработано 1 corefix ssku.))");
    }

    @Test
    public void checkPendingAndActiveSuccess() {
        sskuStatusRepository.save(
            sskuStatus(111, "shop-sku-111", OfferAvailability.ACTIVE, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.PENDING, "comment2", null)
        );
        var keys = List.of(new ServiceOfferKey(111, "shop-sku-111"), new ServiceOfferKey(222, "shop-sku-222"));
        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 5L, SpecialOrderType.NEW,
                        BigDecimal.valueOf(100.0), 1, null, null)
                ),
            new ServiceOfferKey(222, "shop-sku-222"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 500L, SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0), 10, null, null)
                )
        );
        var ticket = facade.start(new StartRequest<>(keys, new MetaV2(), keyMetaMap));

        Mockito.doReturn(new ApproveSpecialOrderResponse()
                .approvedItems(List.of(
                    new ApprovedSpecialOrderItem()
                        .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111"))
                        .demandId(113154600L),
                    new ApprovedSpecialOrderItem()
                        .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000222.shop-sku-222"))
                        .demandId(113154600L))
                ))
            .when(replenishmentService).specialOrderRequestApproveAll(
                new ApproveSpecialOrderRequest()
                    .ticketId(ticket)
                    .keys(List.of(
                        new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111"),
                        new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000222.shop-sku-222")
                    ))
            );

        Mockito.doReturn(
                new MessageDTO()
            )
            .when(replenishmentService).specialOrderRequestDeclineRest(new DeclineSpecialOrderRequest()
                .ticketId(ticket)
            );

        SessionUtils.check(session, ticket);
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        // one more run
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        Assertions
            .assertThat(sskuStatusRepository.find(keys))
            .extracting(SskuStatus::getAvailability)
            .containsExactlyInAnyOrder(OfferAvailability.PENDING, OfferAvailability.ACTIVE);
    }

    @Test
    public void simpleRunWithNewFile() {
        sskuStatusRepository.save(
            sskuStatus(111, "shop-sku-111", OfferAvailability.ACTIVE, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.ACTIVE, "comment1", null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.PENDING, "comment1", null)
        );
        var shopSkus = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        );
        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 500L, SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0), 10, LocalDate.now(), null)
                ),
            new ServiceOfferKey(222, "shop-sku-222"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 500L, SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0), 10, LocalDate.now(), null)
                ),
            new ServiceOfferKey(333, "shop-sku-333"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 500L, SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0), 10, LocalDate.now(), null)
                )
        );
        var ticket = facade.start(new StartRequest<>(shopSkus, new MetaV2(), keyMetaMap));

        executor.run();

        SessionUtils.check(session, ticket);

        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            fromUserExcelComposer.processKeys(List.of(new ServiceOfferKey(111, "shop-sku-111")),
                strategySpy.getNotCreatedSpecialOrderData(keyMetaMap)), user);

        Mockito.doReturn(new ApproveSpecialOrderResponse()
                .approvedItems(List.of(
                    new ApprovedSpecialOrderItem()
                        .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111"))
                        .demandId(113154600L))
                ))
            .when(replenishmentService).specialOrderRequestApproveAll(
                new ApproveSpecialOrderRequest()
                    .ticketId(ticket)
                    .keys(List.of(
                        new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111")
                    ))
            );

        Mockito.doReturn(
                new MessageDTO()
            )
            .when(replenishmentService).specialOrderRequestDeclineRest(new DeclineSpecialOrderRequest()
                .ticketId(ticket)
            );
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        // one more run
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        var metrics = economicMetricsRepository.findAll();
        Assertions.assertThat(metrics)
            .hasSize(1)
            .usingElementComparatorOnFields("ticket", "businessProcess")
            .containsOnly(businessProcessMetric(ticket, SpecialOrderStrategy.TYPE));
    }

    @Test
    public void checkEconomicMetrics() {
        sskuStatusRepository.save(
            sskuStatus(111, "shop-sku-111", OfferAvailability.ACTIVE, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.ACTIVE, "comment1", null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.PENDING, "comment1", null)
        );
        var shopSkus = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        );

        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 500L, SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0), 10, LocalDate.now(), null)
                ),
            new ServiceOfferKey(222, "shop-sku-222"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 500L, SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0), 10, LocalDate.now(), null)
                ),
            new ServiceOfferKey(333, "shop-sku-333"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 500L, SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0), 10, LocalDate.now(), null)
                )
        );
        var ticket = facade.start(new StartRequest<>(shopSkus, new MetaV2(), keyMetaMap));

        executor.run();

        var date = LocalDate.of(2022, 1, 30);
        var userKeyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 50L, SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0), 5, date, null)
                ));
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            fromUserExcelComposer.processKeys(userKeyMetaMap.keySet(),
                strategySpy.getNotCreatedSpecialOrderData(userKeyMetaMap)), user);

        Mockito.doReturn(new ApproveSpecialOrderResponse()
                .approvedItems(List.of(
                    new ApprovedSpecialOrderItem()
                        .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111"))
                        .demandId(113154600L))
                ))
            .when(replenishmentService).specialOrderRequestApproveAll(
                new ApproveSpecialOrderRequest()
                    .ticketId(ticket)
                    .keys(List.of(
                        new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111")
                    ))
            );

        Mockito.doReturn(new MessageDTO())
            .when(replenishmentService).specialOrderRequestDeclineRest(
                new DeclineSpecialOrderRequest()
                    .ticketId(ticket)
            );

        SessionUtils.check(session, ticket);

        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        var metrics = economicMetricsRepository.findAll();
        Assertions.assertThat(metrics)
            .usingElementComparatorOnFields("ticket", "businessProcess")
            .containsOnly(businessProcessMetric(ticket, SpecialOrderStrategy.TYPE));
        var whName = deepmindWarehouseRepository.getByIds(SOFINO_ID).get(0).getName();
        Assertions.assertThat(metrics.get(0).getData())
            .containsEntry(SHOP_SKU_KEY, "shop-sku-111")
            .containsEntry(SUPPLIER_ID_KEY, "111")
            .containsEntry(whName + WAREHOUSE_FIRST_SUPPLY_CNT_ENDING, "50")
            .containsEntry(whName + WAREHOUSE_ORDER_DATE_ENDING, "2022-01-30")
            .containsEntry(QUANT_KEY, "5");
    }

    @Test
    public void simpleRunWithNewFileWithRsId() {
        sskuStatusRepository.save(sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null));

        var keys = List.of(new ServiceOfferKey(111, "shop-sku-111"));
        var userKeyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 500L, SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0), 10, null, null)
                )
        );
        var ticket = facade.start(new StartRequest<>(keys, new MetaV2(), userKeyMetaMap));

        executor.run();

        Mockito.doReturn(new ApproveSpecialOrderResponse()
                .approvedItems(List.of(
                    new ApprovedSpecialOrderItem()
                        .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111"))
                        .demandId(113154600L))
                ))
            .when(replenishmentService).specialOrderRequestApproveAll(
                new ApproveSpecialOrderRequest()
                    .ticketId(ticket)
                    .keys(List.of(
                        new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111")
                    ))
            );

        Mockito.doReturn(new MessageDTO())
            .when(replenishmentService).specialOrderRequestDeclineRest(
                new DeclineSpecialOrderRequest()
                    .ticketId(ticket)
            );

        SessionUtils.check(session, ticket);
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        // one more run
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        Assertions
            .assertThat(sskuStatusRepository.find(keys))
            .extracting(SskuStatus::getAvailability)
            .containsExactly(OfferAvailability.PENDING);
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
        }).when(strategySpy).process(Mockito.any());

        List<ServiceOfferKey> shopSkuList = List.of(new ServiceOfferKey(1, "a"));
        var ticket = facade.start(shopSkuList, new MetaV2());
        // запускаем в первый раз
        SessionUtils.check(session, ticket);
        executor.run();
        Mockito.verify(strategySpy, Mockito.times(1)).reopen(Mockito.any());
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
    public void checkStepTest() {
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.ACTIVE, "comment2", null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.ACTIVE, "comment3", null)
        );

        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        );
        var meta = new MetaV2().setAuthor("author1").setDescription("description1");
        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 5L, SpecialOrderType.NEW,
                        BigDecimal.valueOf(1006.0), 1, null, null)
                ),
            new ServiceOfferKey(222, "shop-sku-222"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(TOMILINO_ID, 5L, SpecialOrderType.SEASONAL,
                        BigDecimal.valueOf(1000.0), 1, null, null)
                ),
            new ServiceOfferKey(333, "shop-sku-333"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(ROSTOV_ID, 5L, SpecialOrderType.LOT,
                        BigDecimal.valueOf(100.0), 1, null, null)
                )
        );
        var ticket = facade.start(new StartRequest<>(shopSkuKeys, meta, keyMetaMap));

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);

        //first run: wrong sskus
        Instant firstAttachCreatedAt = Instant.now().plusSeconds(1);
        var excel =  fromUserExcelComposer.processKeys(keyMetaMap.keySet(),
            strategySpy.getNotCreatedSpecialOrderData(keyMetaMap)).toBuilder();
        excel.setValue(6, SHOP_SKU_KEY, "b");
        excel.setValue(6, SUPPLIER_ID_KEY, 2);
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            excel.build(), user);

        SessionUtils.check(session, ticket);
        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PREPROCESSED);
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket))
            .contains(MbocErrors.get().sskuUpdateNotMatched(new ServiceOfferKey(2, "b").toString()).toString());
        Mockito.verify(strategySpy, Mockito.times(1)).process(Mockito.any());

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
        Mockito.verify(strategySpy, Mockito.times(2)).process(Mockito.any());
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket)).contains(
            MbocErrors.get().assortCommitExcelHeaderAdditionError("Bad header").toString()
        );

        //third run: correct excel data
        Instant thirdAttachCreatedAt = secondAttachCreatedAt.plusSeconds(10);

        Mockito.doReturn(new ApproveSpecialOrderResponse()
                .approvedItems(List.of(
                        new ApprovedSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(ROSTOV_ID).ssku("000333.shop-sku-333"))
                            .demandId(113154600L),
                        new ApprovedSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(TOMILINO_ID).ssku("000222.shop-sku-222"))
                            .demandId(113154600L),
                        new ApprovedSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111"))
                            .demandId(113154600L)
                    )
                ))
            .when(replenishmentService).specialOrderRequestApproveAll(
                new ApproveSpecialOrderRequest()
                    .ticketId(ticket)
                    .keys(List.of(
                        new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111"),
                        new SpecialOrderCreateKey().warehouseId(TOMILINO_ID).ssku("000222.shop-sku-222"),
                        new SpecialOrderCreateKey().warehouseId(ROSTOV_ID).ssku("000333.shop-sku-333")
                    ))
            );

        Mockito.doReturn(new MessageDTO())
            .when(replenishmentService).specialOrderRequestDeclineRest(
                new DeclineSpecialOrderRequest()
                    .ticketId(ticket)
            );
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", thirdAttachCreatedAt,
            fromUserExcelComposer.processKeys(keyMetaMap.keySet(),
                strategySpy.getNotCreatedSpecialOrderData(keyMetaMap)), user);

        SessionUtils.check(session, ticket);
        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        Mockito.verify(strategySpy, Mockito.times(3)).process(Mockito.any());
        Mockito.verify(strategySpy, Mockito.times(1)).postprocess(Mockito.any());

        var sortedBySupplierResult = sskuStatusRepository.find(shopSkuKeys)
            .stream()
            .sorted(Comparator.comparingInt(SskuStatus::getSupplierId))
            .collect(Collectors.toList());
        Assertions
            .assertThat(sortedBySupplierResult)
            .extracting(SskuStatus::getAvailability)
            .containsExactly(
                OfferAvailability.PENDING,
                OfferAvailability.ACTIVE,
                OfferAvailability.ACTIVE
            );
    }

    @SuppressWarnings("checkstyle:MethodLength")
    @Test
    public void excelHeadersValidationTest() {
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.ACTIVE, "comment2", null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.ACTIVE, "comment3", null)
        );

        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        );
        var meta = new MetaV2().setAuthor("author1").setDescription("description1");
        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 5L, SpecialOrderType.NEW,
                        BigDecimal.valueOf(1006.0), 1, null, null)
                ),
            new ServiceOfferKey(222, "shop-sku-222"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(TOMILINO_ID, 5L, SpecialOrderType.SEASONAL,
                        BigDecimal.valueOf(1000.0), 1, null, null)
                ),
            new ServiceOfferKey(333, "shop-sku-333"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(ROSTOV_ID, 5L, SpecialOrderType.LOT,
                        BigDecimal.valueOf(100.0), 1, null, null)
                )
        );
        var ticket = facade.start(new StartRequest<>(shopSkuKeys, meta, keyMetaMap));

        executor.run();

        //first run: added header
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
        var headers = new ArrayList<>(EnrichSpecialOrderExcelComposer.HEADERS);
        headers.remove(SHOP_SKU);
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
        headers = new ArrayList<>(EnrichSpecialOrderExcelComposer.HEADERS);
        headers.remove(SHOP_SKU);
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

        Mockito.doReturn(new ApproveSpecialOrderResponse()
                .approvedItems(List.of(
                        new ApprovedSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(ROSTOV_ID).ssku("000333.shop-sku-333"))
                            .demandId(113154600L),
                        new ApprovedSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(TOMILINO_ID).ssku("000222.shop-sku-222"))
                            .demandId(113154600L),
                        new ApprovedSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111"))
                            .demandId(113154600L)
                    )
                ))
            .when(replenishmentService).specialOrderRequestApproveAll(
                new ApproveSpecialOrderRequest()
                    .ticketId(ticket)
                    .keys(List.of(
                        new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111"),
                        new SpecialOrderCreateKey().warehouseId(TOMILINO_ID).ssku("000222.shop-sku-222"),
                        new SpecialOrderCreateKey().warehouseId(ROSTOV_ID).ssku("000333.shop-sku-333")
                    ))
            );

        Mockito.doReturn(new MessageDTO())
            .when(replenishmentService).specialOrderRequestDeclineRest(
                new DeclineSpecialOrderRequest()
                    .ticketId(ticket)
            );
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", fourthAttachCreatedAt,
            fromUserExcelComposer.processKeys(keyMetaMap.keySet(),
                strategySpy.getNotCreatedSpecialOrderData(keyMetaMap)), user);
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
                OfferAvailability.PENDING,
                OfferAvailability.ACTIVE,
                OfferAvailability.ACTIVE
            );
    }

    @Test
    public void processStepTest() {
        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333"),
            new ServiceOfferKey(444, "shop-sku-444")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE_TMP, UNDER_CONSIDERATION.getLiteral(), null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.INACTIVE, "comment3", null),
            sskuStatus(444, "shop-sku-444", OfferAvailability.PENDING, "comment4", null)
        );

        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 500L, SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0), 10, null, null)
                ),
            new ServiceOfferKey(222, "shop-sku-222"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 500L, SpecialOrderType.NEW,
                        BigDecimal.valueOf(100.0), 10, null, "1123437")
                ),
            new ServiceOfferKey(333, "shop-sku-333"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 500L, SpecialOrderType.SEASONAL,
                        BigDecimal.valueOf(1000.0), 10, null, null)
                ),
            new ServiceOfferKey(444, "shop-sku-444"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 500L, SpecialOrderType.LOT,
                        BigDecimal.valueOf(1000.0), 10, null, null)
                )
        );
        var ticket = facade.start(new StartRequest<>(shopSkuKeys, new MetaV2(), keyMetaMap));
        executor.run();

        //first run: wrong sskus
        Instant firstAttachCreatedAt = Instant.now().plusSeconds(1);
        var excel =  fromUserExcelComposer.processKeys(keyMetaMap.keySet(),
            strategySpy.getNotCreatedSpecialOrderData(keyMetaMap)).toBuilder();
        excel.setValue(6, SHOP_SKU_KEY, "b");
        excel.setValue(6, SUPPLIER_ID_KEY, 2);
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            excel.build(), user);
        SessionUtils.check(session, ticket);

        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PREPROCESSED);
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket))
            .contains(MbocErrors.get().sskuUpdateNotMatched(new ServiceOfferKey(2, "b").toString()).toString());
        Mockito.verify(strategySpy, Mockito.times(1)).process(any());

        //second run: correct sskus
        SessionUtils.check(session, ticket);
        Instant secondAttachCreatedAt = firstAttachCreatedAt.plusSeconds(10);
        //approve only supplier 222,333
        var approvedList = List.of(new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333"));
        var userKeyMetaMap = keyMetaMap.entrySet().stream()
            .filter(entry -> approvedList.contains(entry.getKey()))
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", secondAttachCreatedAt,
            fromUserExcelComposer.processKeys(userKeyMetaMap.keySet(),
                strategySpy.getNotCreatedSpecialOrderData(userKeyMetaMap)), user);

        Mockito.doReturn(new ApproveSpecialOrderResponse()
                .approvedItems(List.of(
                        new ApprovedSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000333.shop-sku-333"))
                            .demandId(113154600L),
                        new ApprovedSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000222.shop-sku-222"))
                            .demandId(113154600L)
                    )
                ))
            .when(replenishmentService).specialOrderRequestApproveAll(
                new ApproveSpecialOrderRequest()
                    .ticketId(ticket)
                    .keys(List.of(
                        new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000222.shop-sku-222"),
                        new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000333.shop-sku-333")
                    ))
            );

        Mockito.doReturn(new MessageDTO())
            .when(replenishmentService).specialOrderRequestDeclineRest(
                new DeclineSpecialOrderRequest()
                    .ticketId(ticket)
            );

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
                OfferAvailability.PENDING,
                OfferAvailability.PENDING);
    }

    @Test
    @SuppressWarnings("checkstyle:MethodLength")
    public void processStepApproveAnyAndThenCloseTicketTest() {

        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333"),
            new ServiceOfferKey(444, "shop-sku-444")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE_TMP, UNDER_CONSIDERATION.getLiteral(), null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.INACTIVE, "comment3", null),
            sskuStatus(444, "shop-sku-444", OfferAvailability.PENDING, "comment4", null)
        );

        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(ROSTOV_ID, 500L, SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0), 10, null, null)
                ),
            new ServiceOfferKey(222, "shop-sku-222"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(ROSTOV_ID, 500L, SpecialOrderType.LOT,
                        BigDecimal.valueOf(100.0), 10, null, "12316716236")
                ),
            new ServiceOfferKey(333, "shop-sku-333"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(ROSTOV_ID, 50L, SpecialOrderType.SEASONAL,
                        BigDecimal.valueOf(1700.0), 1, null, null)
                ),
            new ServiceOfferKey(444, "shop-sku-444"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(ROSTOV_ID, 1000L, SpecialOrderType.LOT,
                        BigDecimal.valueOf(1000.0), 10, null, null)
                )
        );
        var ticket = facade.start(new StartRequest<>(shopSkuKeys, new MetaV2(), keyMetaMap));

        executor.run();

        Instant firstAttachCreatedAt = Instant.now().plusSeconds(1);
        //approve only supplier 222,333
        var approvedList = List.of(new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333"));
        var userKeyMetaMap = keyMetaMap.entrySet().stream()
            .filter(entry -> approvedList.contains(entry.getKey()))
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", firstAttachCreatedAt,
            fromUserExcelComposer.processKeys(userKeyMetaMap.keySet(),
                strategySpy.getNotCreatedSpecialOrderData(userKeyMetaMap)), user);

        String errorText = "Для поставщика ООО Марвел КТ и склада Яндекс.Маркет (Новосибирск)" +
            " отсутствуют логистические параметры";
        Mockito.doReturn(new ApproveSpecialOrderResponse()
                .declinedItems(List.of(
                        new DeclinedSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(ROSTOV_ID).ssku("000333.shop-sku-333"))
                            .error(errorText)
                    )
                ))
            .when(replenishmentService).specialOrderRequestApproveAll(
                new ApproveSpecialOrderRequest()
                    .ticketId(ticket)
                    .keys(List.of(
                        new SpecialOrderCreateKey().warehouseId(ROSTOV_ID).ssku("000222.shop-sku-222"),
                        new SpecialOrderCreateKey().warehouseId(ROSTOV_ID).ssku("000333.shop-sku-333")
                    ))
            );

        SessionUtils.check(session, ticket);
        executor.run();

        Assertions.assertThat(SessionUtils.getLastComment(session, ticket))
            .contains(errorText);
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PROCESSED);
        Assertions.assertThat(facade.findTicketStatus(ticket).getRetryCount()).isZero();
        Mockito.verify(strategySpy, Mockito.times(1)).process(any());
        Mockito.verify(strategySpy, Mockito.times(1)).postprocess(any());

        Mockito.doReturn(new ApproveSpecialOrderResponse()
                .approvedItems(List.of(
                        new ApprovedSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(ROSTOV_ID).ssku("000222.shop-sku-222"))
                            .demandId(113154600L)
                    )
                )
                .declinedItems(List.of(
                        new DeclinedSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(ROSTOV_ID).ssku("000333.shop-sku-333"))
                            .error(errorText)
                    )
                )
            )
            .when(replenishmentService).specialOrderRequestApproveAny(
                new ApproveSpecialOrderRequest()
                    .ticketId(ticket)
                    .keys(List.of(
                        new SpecialOrderCreateKey().warehouseId(ROSTOV_ID).ssku("000222.shop-sku-222"),
                        new SpecialOrderCreateKey().warehouseId(ROSTOV_ID).ssku("000333.shop-sku-333")
                    ))
            );
        SessionUtils.awaitsActivation(session, ticket);
        executor.run();

        Mockito.doReturn(new MessageDTO())
            .when(replenishmentService).specialOrderRequestDeclineRest(
                new DeclineSpecialOrderRequest()
                    .ticketId(ticket)
            );

        // пользователь решил не бороться с лог параметрами
        SessionUtils.close(session, ticket, TicketResolution.FIXED);
        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        Mockito.verify(strategySpy, Mockito.times(1)).process(any());
        Mockito.verify(strategySpy, Mockito.times(3)).postprocess(any());
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
                OfferAvailability.PENDING,
                OfferAvailability.PENDING);
    }

    @Test
    public void processStepReplenishmentFailedTest() {

        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333"),
            new ServiceOfferKey(444, "shop-sku-444")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE_TMP, UNDER_CONSIDERATION.getLiteral(), null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.INACTIVE, "comment3", null),
            sskuStatus(444, "shop-sku-444", OfferAvailability.PENDING, "comment4", null)
        );

        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 500L, SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0), 10, null, null)
                ),
            new ServiceOfferKey(222, "shop-sku-222"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 500L, SpecialOrderType.LOT,
                        BigDecimal.valueOf(100.0), 10, null, "12316716236")
                ),
            new ServiceOfferKey(333, "shop-sku-333"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 50L, SpecialOrderType.SEASONAL,
                        BigDecimal.valueOf(1700.0), 1, null, null)
                ),
            new ServiceOfferKey(444, "shop-sku-444"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 1000L, SpecialOrderType.LOT,
                        BigDecimal.valueOf(1000.0), 10, null, null)
                )
        );
        var ticket = facade.start(new StartRequest<>(shopSkuKeys, new MetaV2(), keyMetaMap));
        executor.run();
        //attach correct sskus
        SessionUtils.check(session, ticket);
        Instant firstAttachCreatedAt = Instant.now().plusSeconds(1);
        //approve only supplier 222,333
        var approvedList = List.of(new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333"));
        var userKeyMetaMap = keyMetaMap.entrySet().stream()
            .filter(entry -> approvedList.contains(entry.getKey()))
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", firstAttachCreatedAt,
            fromUserExcelComposer.processKeys(userKeyMetaMap.keySet(),
                strategySpy.getNotCreatedSpecialOrderData(userKeyMetaMap)), user);

        String errorText = "some error occurred";
        Mockito.doThrow(new ApiException(errorText))
            .when(replenishmentService).specialOrderRequestApproveAll(any());
        executor.run();
        Assertions.assertThat(session.issues().getSummonees(ticket))
            .doesNotContain(MbocErrors.get().cannotProcessSpecialOrderTicket(errorText).toString());
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PROCESSED);
        Assertions.assertThat(facade.findTicketStatus(ticket).getRetryCount()).isOne();
        Assertions.assertThat(facade.postprocess(ticket).getErrorMessage()).contains(errorText);

        Mockito.verify(strategySpy, Mockito.times(2)).postprocess(any());

        Mockito.doReturn(new ApproveSpecialOrderResponse()
                .approvedItems(List.of(
                        new ApprovedSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000333.shop-sku-333"))
                            .demandId(113154600L),
                        new ApprovedSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000222.shop-sku-222"))
                            .demandId(113154600L)
                    )
                ))
            .when(replenishmentService).specialOrderRequestApproveAll(
                new ApproveSpecialOrderRequest()
                    .ticketId(ticket)
                    .keys(List.of(
                        new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000222.shop-sku-222"),
                        new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000333.shop-sku-333")
                    ))
            );

        Mockito.doReturn(new MessageDTO())
            .when(replenishmentService).specialOrderRequestDeclineRest(
                new DeclineSpecialOrderRequest()
                    .ticketId(ticket)
            );

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        Mockito.verify(strategySpy, Mockito.times(3)).postprocess(any());
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
                OfferAvailability.PENDING,
                OfferAvailability.PENDING);
    }

    @Test
    public void processStepReplenishmentClientWithExceptionTest() {

        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333"),
            new ServiceOfferKey(444, "shop-sku-444")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE_TMP, UNDER_CONSIDERATION.getLiteral(), null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.INACTIVE, "comment3", null),
            sskuStatus(444, "shop-sku-444", OfferAvailability.PENDING, "comment4", null)
        );

        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 500L, SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0), 10, null, null)
                ),
            new ServiceOfferKey(222, "shop-sku-222"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 500L, SpecialOrderType.LOT,
                        BigDecimal.valueOf(100.0), 10, null, "12316716236")
                ),
            new ServiceOfferKey(333, "shop-sku-333"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 50L, SpecialOrderType.SEASONAL,
                        BigDecimal.valueOf(1700.0), 1, null, null)
                ),
            new ServiceOfferKey(444, "shop-sku-444"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 1000L, SpecialOrderType.LOT,
                        BigDecimal.valueOf(1000.0), 10, null, null)
                )
        );
        var ticket = facade.start(new StartRequest<>(shopSkuKeys, new MetaV2(), keyMetaMap));
        executor.run();

        //first run: wrong sskus
        Instant firstAttachCreatedAt = Instant.now().plusSeconds(1);
        var excel =  fromUserExcelComposer.processKeys(keyMetaMap.keySet(),
            strategySpy.getNotCreatedSpecialOrderData(keyMetaMap)).toBuilder();
        excel.setValue(6, SHOP_SKU_KEY, "b");
        excel.setValue(6, SUPPLIER_ID_KEY, 2);
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            excel.build(), user);

        SessionUtils.check(session, ticket);
        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PREPROCESSED);
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket))
            .contains(MbocErrors.get().sskuUpdateNotMatched(new ServiceOfferKey(2, "b").toString()).toString());
        Mockito.verify(strategySpy, Mockito.times(1)).process(any());
        Mockito.verify(strategySpy, Mockito.times(0)).postprocess(any());

        //second run: correct sskus
        SessionUtils.check(session, ticket);
        Instant secondAttachCreatedAt = firstAttachCreatedAt.plusSeconds(10);
        //approve only supplier 222,333
        var approvedList = List.of(new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333"));
        var userKeyMetaMap = keyMetaMap.entrySet().stream()
            .filter(entry -> approvedList.contains(entry.getKey()))
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", secondAttachCreatedAt,
            fromUserExcelComposer.processKeys(userKeyMetaMap.keySet(),
                strategySpy.getNotCreatedSpecialOrderData(userKeyMetaMap)), user);

        Mockito.doReturn(new ApproveSpecialOrderResponse()
                .approvedItems(List.of(
                        new ApprovedSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000333.shop-sku-333"))
                            .demandId(113154600L),
                        new ApprovedSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000222.shop-sku-222"))
                            .demandId(113154600L)
                    )
                ))
            .when(replenishmentService).specialOrderRequestApproveAll(
                new ApproveSpecialOrderRequest()
                    .ticketId(ticket)
                    .keys(List.of(
                        new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000222.shop-sku-222"),
                        new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000333.shop-sku-333")
                    ))
            );

        Mockito.doReturn(new MessageDTO())
            .when(replenishmentService).specialOrderRequestDeclineRest(
                new DeclineSpecialOrderRequest()
                    .ticketId(ticket)
            );

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
                OfferAvailability.PENDING,
                OfferAvailability.PENDING);
    }

    @Test
    public void processStepHeadersNotMatchTest() {
        deepmindSupplierRepository.save(new Supplier().setId(1).setName("1"));
        categoryCachingService.addCategory(
            deepmindCategoryRepository.insert(new Category().setCategoryId(1L).setName("category1"))
        );
        deepmindMskuRepository.save(msku(1, 1));
        serviceOfferReplicaRepository.save(offer(1, "a", 1, 1));
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(1, "a", OfferAvailability.DELISTED, "comment1", null)
        );

        var shopSkus = List.of(new ServiceOfferKey(1, "a"));
        var keyMetaMap = Map.of(
            new ServiceOfferKey(1, "a"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 500L, SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0), 10, LocalDate.now().minusDays(3), null)
                )
        );
        var ticket = facade.start(new StartRequest<>(shopSkus, new MetaV2(), keyMetaMap));

        executor.run();

        SessionUtils.check(session, ticket);
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            createNotCorrectExcelFile(shopSkus), user);
        // запускаем в первый раз
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
            new ServiceOfferKey(333, "shop-sku-333"),
            new ServiceOfferKey(444, "shop-sku-444")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.DELISTED, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE_TMP, UNDER_CONSIDERATION.getLiteral(), null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.INACTIVE, "comment3", null),
            sskuStatus(444, "shop-sku-444", OfferAvailability.PENDING, "comment4", null)
        );

        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 500L, SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0), 10, null, null)
                ),
            new ServiceOfferKey(222, "shop-sku-222"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 500L, SpecialOrderType.LOT,
                        BigDecimal.valueOf(100.0), 10, null, "12316716236")
                ),
            new ServiceOfferKey(333, "shop-sku-333"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 50L, SpecialOrderType.SEASONAL,
                        BigDecimal.valueOf(1700.0), 1, null, null)
                ),
            new ServiceOfferKey(444, "shop-sku-444"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 1000L, SpecialOrderType.LOT,
                        BigDecimal.valueOf(1000.0), 10, null, null)
                )
        );
        var ticket = facade.start(new StartRequest<>(shopSkuKeys, new MetaV2(), keyMetaMap));

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
    public void closeWithWontDoWillChangeToInactiveOnlyNewOffers() {
        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.INACTIVE_TMP, UNDER_CONSIDERATION.getLiteral(), null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE_TMP, "comment2", null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.DELISTED, "comment3", null)
        );
        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 500L, SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0), 10, null, null)
                ),
            new ServiceOfferKey(222, "shop-sku-222"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 500L, SpecialOrderType.LOT,
                        BigDecimal.valueOf(1000.0), 10, null, null)
                ),
            new ServiceOfferKey(333, "shop-sku-333"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 500L, SpecialOrderType.SEASONAL,
                        BigDecimal.valueOf(1000.0), 10, null, null)
                )
        );

        var ticket = facade.start(new StartRequest<>(shopSkuKeys, new MetaV2(), keyMetaMap));
        Assertions.assertThat(session.attachments().getAll(ticket).stream().collect(Collectors.toList())).isEmpty();
        SessionUtils.close(session, ticket, TicketResolution.WONT_DO);

        Mockito.doReturn(new MessageDTO())
            .when(replenishmentService).specialOrderRequestDeclineRest(
                new DeclineSpecialOrderRequest()
                    .ticketId(ticket)
            );

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        Assertions
            .assertThat(sskuStatusRepository.find(shopSkuKeys))
            .extracting(SskuStatus::getAvailability)
            .containsExactlyInAnyOrder(OfferAvailability.INACTIVE, OfferAvailability.INACTIVE_TMP,
                OfferAvailability.DELISTED);
    }

    @Test
    public void closeWithWontFixBeforeEnrichWillChangeToInactiveTmp() {
        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333"),
            new ServiceOfferKey(444, "shop-sku-444"),
            new ServiceOfferKey(555, "shop-sku-555")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.INACTIVE_TMP, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE_TMP, UNDER_CONSIDERATION.getLiteral(), null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.INACTIVE_TMP, UNDER_CONSIDERATION.getLiteral(), null),
            sskuStatus(444, "shop-sku-444", OfferAvailability.DELISTED, "comment4", null),
            sskuStatus(555, "shop-sku-555", OfferAvailability.INACTIVE, "comment5", null)
        );

        var ticket = facade.start(shopSkuKeys, new MetaV2());

        // тикет отменен после создания
        SessionUtils.close(session, ticket, TicketResolution.WONT_FIX);
        Mockito.doReturn(new MessageDTO())
            .when(replenishmentService).specialOrderRequestDeclineRest(
                new DeclineSpecialOrderRequest()
                    .ticketId(ticket)
            );
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        Assertions
            .assertThat(sskuStatusRepository.find(shopSkuKeys))
            .extracting(SskuStatus::getAvailability)
            .containsExactlyInAnyOrder(OfferAvailability.INACTIVE_TMP, OfferAvailability.INACTIVE_TMP,
                OfferAvailability.INACTIVE_TMP, OfferAvailability.DELISTED, OfferAvailability.INACTIVE);

        Assertions
            .assertThat(sskuStatusRepository.find(shopSkuKeys))
            .extracting(SskuStatus::getComment)
            .containsExactlyInAnyOrder("comment1", WAITING_FOR_ENTER.getLiteral(), WAITING_FOR_ENTER.getLiteral(),
                "comment4", "comment5");
    }

    @Test
    public void closeWithWontFixAfterEnrichWillChangeToInactiveTmp() {
        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333"),
            new ServiceOfferKey(444, "shop-sku-444"),
            new ServiceOfferKey(555, "shop-sku-555")
        );
        sskuMskuStatusService.saveSskuStatuses(
            sskuStatus(111, "shop-sku-111", OfferAvailability.INACTIVE_TMP, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.INACTIVE_TMP, UNDER_CONSIDERATION.getLiteral(), null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.INACTIVE_TMP, UNDER_CONSIDERATION.getLiteral(), null),
            sskuStatus(444, "shop-sku-444", OfferAvailability.DELISTED, "comment4", null),
            sskuStatus(555, "shop-sku-555", OfferAvailability.INACTIVE, "comment5", null)
        );
        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 500L, SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0), 10, null, null)
                ),
            new ServiceOfferKey(222, "shop-sku-222"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(ROSTOV_ID, 500L, SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0), 10, null, null)
                ),
            new ServiceOfferKey(333, "shop-sku-333"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 500L, SpecialOrderType.SEASONAL,
                        BigDecimal.valueOf(1000.0), 10, null, null)
                ),
            new ServiceOfferKey(444, "shop-sku-444"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 500L, SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0), 10, LocalDate.now().minusDays(7), "98778467623")
                ),
            new ServiceOfferKey(555, "shop-sku-555"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 90L, SpecialOrderType.NEW,
                        BigDecimal.valueOf(100.0), 10, null, null)
                )
        );
        var ticket = facade.start(new StartRequest<>(shopSkuKeys, new MetaV2(), keyMetaMap));

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);

        // тикет отменен после обогащения
        SessionUtils.close(session, ticket, TicketResolution.WONT_FIX);

        Mockito.doReturn(new MessageDTO())
            .when(replenishmentService).specialOrderRequestDeclineRest(
                new DeclineSpecialOrderRequest()
                    .ticketId(ticket)
            );

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
        Assertions.assertThat(session.attachments().getAll(ticket).stream().collect(Collectors.toList())).hasSize(1);

        // all to inactive_tmp
        var result = sskuStatusRepository.find(shopSkuKeys);
        Assertions
            .assertThat(result)
            .extracting(SskuStatus::getAvailability)
            .containsExactlyInAnyOrder(OfferAvailability.INACTIVE_TMP, OfferAvailability.INACTIVE_TMP,
                OfferAvailability.INACTIVE_TMP, OfferAvailability.DELISTED, OfferAvailability.INACTIVE);

        Assertions
            .assertThat(sskuStatusRepository.find(shopSkuKeys))
            .extracting(SskuStatus::getComment)
            .containsExactlyInAnyOrder("comment1", WAITING_FOR_ENTER.getLiteral(), WAITING_FOR_ENTER.getLiteral(),
                "comment4", "comment5");
    }

    @Test
    public void processStepEndsWithOneDeclinedItemOnDifferentWarehouses() {
        sskuStatusRepository.save(
            sskuStatus(111, "shop-sku-111", OfferAvailability.INACTIVE, "comment1")
        );

        List<ServiceOfferKey> shopSkuKeys = List.of(
            new ServiceOfferKey(111, "shop-sku-111")
        );
        Map<ServiceOfferKey, KeyMetaV2> keyMetaMap = shopSkuKeys.stream().collect(
            Collectors.toMap(
                Function.identity(), ssku -> new KeyMetaV2().addSpecialOrderData(
                    List.of(
                        new SpecialOrderData(SOFINO_ID, 2L, null,
                            BigDecimal.TEN, 1, LocalDate.now(), null),
                        new SpecialOrderData(ROSTOV_ID, 20L, null,
                            BigDecimal.TEN, 1, LocalDate.now(), null)
                    ))));
        var ticket = facade.start(new StartRequest<>(shopSkuKeys, new MetaV2(), keyMetaMap));
        executor.run();

        SessionUtils.check(session, ticket);

        Mockito.doReturn(new ApproveSpecialOrderResponse().declinedItems(List.of(
                new DeclinedSpecialOrderItem()
                    .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111"))
                    .error("отсутствуют логистические параметры"),
                new DeclinedSpecialOrderItem()
                    .key(new SpecialOrderCreateKey().warehouseId(ROSTOV_ID).ssku("000111.shop-sku-111"))
                    .error("отсутствуют логистические параметры")
            ))
        ).when(replenishmentService).specialOrderRequestApproveAll(
            new ApproveSpecialOrderRequest().ticketId(ticket)
                .keys(
                    List.of(
                        new SpecialOrderCreateKey().warehouseId(ROSTOV_ID).ssku("000111.shop-sku-111"),
                        new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111")
                    )
                ));

        executor.run();
        var issue = facade.findTicketStatus(ticket);
        Assertions.assertThat(issue.getState()).isEqualTo(TicketState.PROCESSED);
        Assertions.assertThat(issue.getLastException()).isNull();
    }

    @Test
    public void ticketMovedToWaitingForActivationAfterNeedCorrectionNoSskuStatusesChanged() {
        sskuStatusRepository.save(
            sskuStatus(111, "shop-sku-111", OfferAvailability.INACTIVE_TMP, "comment1")
        );

        List<ServiceOfferKey> keys = List.of(new ServiceOfferKey(111, "shop-sku-111"));
        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 5L, SpecialOrderType.NEW, BigDecimal.valueOf(100.0), 1, null, null)
                )
        );
        var ticket = facade.start(new StartRequest<>(keys, new MetaV2(), keyMetaMap));
        executor.run();

        var excelFile = createCorrectExcelFile(keys, List.of(SHOP_SKU, SUPPLIER_ID)).toBuilder()
            .setValue(3, Headers.SHOP_SKU_KEY, "222")
            .setValue(3, Headers.SUPPLIER_ID_KEY, "shop-sku-222")
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

        // Прикрепляем заполненный пользователем эксель
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            fromUserExcelComposer.processKeys(keyMetaMap.keySet(),
                strategySpy.getNotCreatedSpecialOrderData(keyMetaMap)), user);

        Mockito.doReturn(
                new ApproveSpecialOrderResponse()
                    .approvedItems(List.of(
                        new ApprovedSpecialOrderItem()
                            .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111"))
                            .demandId(1L)
                    )))
            .when(replenishmentService).specialOrderRequestApproveAny(
                new ApproveSpecialOrderRequest()
                    .ticketId(ticket)
                    .keys(List.of(
                        new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111")
                    )));

        Mockito.doReturn(
                new MessageDTO()
            )
            .when(replenishmentService).specialOrderRequestDeclineRest(new DeclineSpecialOrderRequest()
                .ticketId(ticket)
            );

        // пользователь согласно флоу сдвинул тикет в "Ожидает активации"
        SessionUtils.awaitsActivation(session, ticket);

        executor.run();

        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);

        Assertions.assertThat(sskuStatusRepository.findByKey(111, "shop-sku-111"))
            .get().extracting(SskuStatus::getAvailability)
            .isEqualTo(OfferAvailability.PENDING);
    }

    @Test
    public void checkEconomicMetricsNotAbsentTest() {
        sskuStatusRepository.save(
            sskuStatus(111, "shop-sku-111", OfferAvailability.ACTIVE, "comment1", null),
            sskuStatus(222, "shop-sku-222", OfferAvailability.ACTIVE, "comment1", null),
            sskuStatus(333, "shop-sku-333", OfferAvailability.PENDING, "comment1", null)
        );
        var shopSkus = List.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new ServiceOfferKey(222, "shop-sku-222"),
            new ServiceOfferKey(333, "shop-sku-333")
        );

        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 500L, SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0), 10, LocalDate.now(), null)
                ),
            new ServiceOfferKey(222, "shop-sku-222"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 500L, SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0), 10, LocalDate.now(), null)
                ),
            new ServiceOfferKey(333, "shop-sku-333"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 500L, SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0), 10, LocalDate.now(), null)
                )
        );
        var ticket = facade.start(new StartRequest<>(shopSkus, new MetaV2(), keyMetaMap));

        executor.run();

        var date = LocalDate.of(2022, 1, 30);
        var userKeyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new KeyMetaV2()
                .addSpecialOrderData(
                    new SpecialOrderData(SOFINO_ID, 50L, SpecialOrderType.NEW,
                        BigDecimal.valueOf(1000.0), 5, date, null)
                ));

        ExcelFile attachedFile = getExcelFrom("excel_files/Special_order_27_05_2022.xlsx");
        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            attachedFile, user);

        Mockito.doReturn(new ApproveSpecialOrderResponse()
                .approvedItems(List.of(
                    new ApprovedSpecialOrderItem()
                        .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111"))
                        .demandId(113154600L))
                ))
            .when(replenishmentService).specialOrderRequestApproveAll(
                new ApproveSpecialOrderRequest()
                    .ticketId(ticket)
                    .keys(List.of(
                        new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111")
                    ))
            );

        Mockito.doReturn(new MessageDTO())
            .when(replenishmentService).specialOrderRequestDeclineRest(
                new DeclineSpecialOrderRequest()
                    .ticketId(ticket)
            );

        SessionUtils.check(session, ticket);

        executor.run();

        var metrics = economicMetricsRepository.findAll();
        Assertions.assertThat(metrics.size()).isOne();

        Assertions.assertThat(metrics)
            .usingElementComparatorOnFields("ticket", "businessProcess")
            .containsOnly(businessProcessMetric(ticket, SpecialOrderStrategy.TYPE));
        var sofino = deepmindWarehouseRepository.getByIds(SOFINO_ID).get(0).getName();
        var ekat = deepmindWarehouseRepository.getByIds(EKATERINBURG_ID).get(0).getName();
        Assertions.assertThat(metrics.get(0).getData())
            .containsEntry(SHOP_SKU_KEY, "shop-sku-111")
            .containsEntry(SUPPLIER_ID_KEY, "111")
            .containsEntry(sofino + WAREHOUSE_FIRST_SUPPLY_CNT_ENDING, "500")
            .containsEntry(ekat + WAREHOUSE_FIRST_SUPPLY_CNT_ENDING, "10")
            .containsEntry(QUANT_KEY, "10");
        Assertions.assertThat(metrics.get(0).getData().keySet())
            .doesNotContain(ekat + WAREHOUSE_ORDER_DATE_ENDING)
            .doesNotContain(sofino + WAREHOUSE_ORDER_DATE_ENDING);
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
            sskuStatus(903, "shop-sku-333", OfferAvailability.INACTIVE_TMP, NO_PURCHASE_PRICE.getLiteral(), null),
            sskuStatus(904, "shop-sku-444", OfferAvailability.INACTIVE_TMP, NO_PURCHASE_PRICE.getLiteral(), null),
            sskuStatus(905, "shop-sku-555", OfferAvailability.INACTIVE_TMP, NO_PURCHASE_PRICE.getLiteral(), null),

            sskuStatus(901, "assort-sku-1", OfferAvailability.DELISTED, "comment5", null),
            sskuStatus(902, "assort-sku-2", OfferAvailability.INACTIVE, "comment5", null),
            sskuStatus(903, "assort-sku-3", OfferAvailability.PENDING, "comment5", null),
            sskuStatus(904, "assort-sku-4", OfferAvailability.ACTIVE, "comment5", null),
            sskuStatus(905, "assort-sku-5", OfferAvailability.INACTIVE_TMP, "comment5", null)
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

        Mockito.doReturn(new ApproveSpecialOrderResponse().approvedItems(List.of(
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
        ).when(replenishmentService).specialOrderRequestApproveAll(any());

        Mockito.doReturn(new MessageDTO())
            .when(replenishmentService).specialOrderRequestDeclineRest(
                new DeclineSpecialOrderRequest()
                    .ticketId(ticket)
            );

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
            sskuStatus(901, "shop-sku-111", OfferAvailability.INACTIVE_TMP, NO_PURCHASE_PRICE.getLiteral(), null),
            sskuStatus(902, "shop-sku-222", OfferAvailability.INACTIVE_TMP, NO_PURCHASE_PRICE.getLiteral(), null)
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

        Mockito.doReturn(new ApproveSpecialOrderResponse().approvedItems(List.of(
                new ApprovedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000901.shop-sku-111")
                ).demandId(113154600L),
                new ApprovedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000902.shop-sku-222")
                ).demandId(113154600L)
            ))
        ).when(replenishmentService).specialOrderRequestApproveAll(any());

        Mockito.doReturn(new MessageDTO())
            .when(replenishmentService).specialOrderRequestDeclineRest(
                new DeclineSpecialOrderRequest()
                    .ticketId(ticket)
            );

        Mockito.doReturn(new MessageDTO())
            .when(replenishmentService).specialOrderRequestDeclineRest(
                new DeclineSpecialOrderRequest()
                    .ticketId(ticket)
            );

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

        Mockito.doReturn(new ApproveSpecialOrderResponse().approvedItems(List.of(
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
        ).when(replenishmentService).specialOrderRequestApproveAll(any());

        Mockito.doReturn(new MessageDTO())
            .when(replenishmentService).specialOrderRequestDeclineRest(
                new DeclineSpecialOrderRequest()
                    .ticketId(ticket)
            );

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

        Mockito.doReturn(new ApproveSpecialOrderResponse().approvedItems(List.of(
                new ApprovedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000901.shop-sku-111")
                ).demandId(113154600L),
                new ApprovedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000902.shop-sku-222")
                ).demandId(113154600L)
            ))
        ).when(replenishmentService).specialOrderRequestApproveAll(any());

        Mockito.doReturn(new MessageDTO())
            .when(replenishmentService).specialOrderRequestDeclineRest(
                new DeclineSpecialOrderRequest()
                    .ticketId(ticket)
            );

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

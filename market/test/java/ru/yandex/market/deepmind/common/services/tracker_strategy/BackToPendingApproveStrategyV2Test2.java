package ru.yandex.market.deepmind.common.services.tracker_strategy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.KeyMetaV2;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.MetaV2;
import ru.yandex.market.deepmind.common.services.tracker_approver.pojo.SpecialOrderData;
import ru.yandex.market.deepmind.common.utils.SessionUtils;
import ru.yandex.market.deepmind.tracker_approver.pojo.StartRequest;
import ru.yandex.market.deepmind.tracker_approver.pojo.TicketState;
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
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.ACTIVE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.DELISTED;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.INACTIVE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.INACTIVE_TMP;
import static ru.yandex.market.deepmind.common.pojo.SskuStatusReason.NO_PURCHASE_PRICE;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;

public class BackToPendingApproveStrategyV2Test2 extends BackToPendingApproveStrategyBaseTestClass {

    @Test
    public void autoRuleWithoutUserAttachmentTest() {
        sskuStatusRepository.save(sskuStatus(111, "shop-sku-111", INACTIVE, "comment1", null));
        var shopSkus = List.of(new ServiceOfferKey(111, "shop-sku-111"));
        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new KeyMetaV2().addSpecialOrderData(
                new SpecialOrderData(SOFINO_ID, 5L, SpecialOrderType.NEW,
                    BigDecimal.valueOf(100.0), 1, LocalDate.now().minusDays(1), null))
        );
        var ticket = facade.start(new StartRequest<>(shopSkus, new MetaV2(), keyMetaMap));
        executor.run();

        SessionUtils.autoRule(session, ticket);
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);
        var attachments = session.attachments().getAll(ticket);
        Assertions.assertThat(attachments.stream().collect(Collectors.toList()))
            .hasSize(2);

        Mockito.doReturn(new CreateSpecialOrderResponse().approvedItems(List.of(
                new ApprovedSpecialOrderItem()
                    .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111"))
                    .demandId(113154600L))
            )
        ).when(replenishmentService).specialOrderRequestCreateAll(any());

        SessionUtils.check(session, ticket);
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
    }

    @Test
    public void autoRuleFailTest() {
        sskuStatusRepository.save(sskuStatus(111, "shop-sku-111", INACTIVE, "comment1", null));
        var shopSkus = List.of(new ServiceOfferKey(111, "shop-sku-111"));
        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new KeyMetaV2().addSpecialOrderData(
                new SpecialOrderData(SOFINO_ID, 5L, SpecialOrderType.NEW,
                    BigDecimal.valueOf(100.0), 1, LocalDate.now().minusDays(1), null))
        );
        var ticket = facade.start(new StartRequest<>(shopSkus, new MetaV2(), keyMetaMap));
        executor.run();

        var userAttachment = getExcelFrom("excel_files/pendingAutoRuleFailTest.xlsx");
        SessionUtils.addExcelAttachment(session, ticket, "test.xlsx", Instant.now().plusSeconds(1),
            userAttachment, user);

        SessionUtils.autoRule(session, ticket);
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);
        var attachments = session.attachments().getAll(ticket);
        Assertions.assertThat(attachments.stream().collect(Collectors.toList()))
            .hasSize(3);

        Mockito.doReturn(new CreateSpecialOrderResponse().approvedItems(List.of(
                new ApprovedSpecialOrderItem()
                    .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111"))
                    .demandId(113154600L))
            )
        ).when(replenishmentService).specialOrderRequestCreateAll(any());

        SessionUtils.check(session, ticket);
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
    }

    @Test
    public void autoRuleOkTest() {
        sskuStatusRepository.save(sskuStatus(111, "shop-sku-111", INACTIVE, "comment1", null));
        var shopSkus = List.of(new ServiceOfferKey(111, "shop-sku-111"));
        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new KeyMetaV2().addSpecialOrderData(
                new SpecialOrderData(SOFINO_ID, 5L, SpecialOrderType.NEW,
                    BigDecimal.valueOf(100.0), 1, LocalDate.now().minusDays(1), null))
        );
        var ticket = facade.start(new StartRequest<>(shopSkus, new MetaV2(), keyMetaMap));
        executor.run();

        var userAttachment = getExcelFrom("excel_files/pendingAutoRuleOkTest.xlsx");
        SessionUtils.addExcelAttachment(session, ticket, "test.xlsx", Instant.now().plusSeconds(1),
            userAttachment, user);

        SessionUtils.autoRule(session, ticket);
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);
        var attachments = session.attachments().getAll(ticket);
        Assertions.assertThat(attachments.stream().collect(Collectors.toList()))
            .hasSize(3);

        Mockito.doReturn(new CreateSpecialOrderResponse().approvedItems(List.of(
                new ApprovedSpecialOrderItem()
                    .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111"))
                    .demandId(113154600L))
            )
        ).when(replenishmentService).specialOrderRequestCreateAll(any());

        SessionUtils.check(session, ticket);
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
    }

    @Test
    public void autoRuleVisitTwiceTest() {
        sskuStatusRepository.save(sskuStatus(111, "shop-sku-111", INACTIVE, "comment1", null));
        var shopSkus = List.of(new ServiceOfferKey(111, "shop-sku-111"));
        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new KeyMetaV2().addSpecialOrderData(
                new SpecialOrderData(SOFINO_ID, 5L, SpecialOrderType.NEW,
                    BigDecimal.valueOf(100.0), 1, LocalDate.now().minusDays(1), null))
        );
        var ticket = facade.start(new StartRequest<>(shopSkus, new MetaV2(), keyMetaMap));
        executor.run();

        var userAttachment = getExcelFrom("excel_files/pendingAutoRuleFailTest.xlsx");
        SessionUtils.addExcelAttachment(session, ticket, "test.xlsx", Instant.now().plusSeconds(1),
            userAttachment, user);

        SessionUtils.autoRule(session, ticket);
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);
        var attachments = session.attachments().getAll(ticket);
        Assertions.assertThat(attachments.stream().collect(Collectors.toList()))
            .hasSize(3);

        userAttachment = getExcelFrom("excel_files/pendingAutoRuleOkTest.xlsx");
        SessionUtils.addExcelAttachment(session, ticket, "test.xlsx", Instant.now().plusSeconds(1),
            userAttachment, user);

        SessionUtils.autoRule(session, ticket);
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);
        attachments = session.attachments().getAll(ticket);
        Assertions.assertThat(attachments.stream().collect(Collectors.toList()))
            .hasSize(5);

        Mockito.doReturn(new CreateSpecialOrderResponse().approvedItems(List.of(
                new ApprovedSpecialOrderItem()
                    .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111"))
                    .demandId(113154600L))
            )
        ).when(replenishmentService).specialOrderRequestCreateAll(any());

        SessionUtils.check(session, ticket);
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
    }

    @Test
    public void autoRuleWithWrongExcelTest() {
        sskuStatusRepository.save(sskuStatus(111, "shop-sku-111", INACTIVE, "comment1", null));
        var shopSkus = List.of(new ServiceOfferKey(111, "shop-sku-111"));
        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new KeyMetaV2().addSpecialOrderData(
                new SpecialOrderData(SOFINO_ID, 5L, SpecialOrderType.NEW,
                    BigDecimal.valueOf(100.0), 1, LocalDate.now().minusDays(1), null))
        );
        var ticket = facade.start(new StartRequest<>(shopSkus, new MetaV2(), keyMetaMap));
        executor.run();

        var userAttachment = createNotCorrectExcelFile(shopSkus);
        SessionUtils.addExcelAttachment(session, ticket, "test.xlsx", Instant.now().plusSeconds(1),
            userAttachment, user);

        SessionUtils.autoRule(session, ticket);
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);
        var attachments = session.attachments().getAll(ticket);
        Assertions.assertThat(attachments.stream().collect(Collectors.toList()))
            .hasSize(2);
        Assertions.assertThat(SessionUtils.getLastComment(session, ticket)).contains(
            MbocErrors.get().assortCommitExcelHeaderAdditionError("Bad header").toString()
        );

        userAttachment = getExcelFrom("excel_files/pendingAutoRuleOkTest.xlsx");
        SessionUtils.addExcelAttachment(session, ticket, "test.xlsx", Instant.now().plusSeconds(1),
            userAttachment, user);

        SessionUtils.autoRule(session, ticket);
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.ENRICHED);
        attachments = session.attachments().getAll(ticket);
        Assertions.assertThat(attachments.stream().collect(Collectors.toList()))
            .hasSize(4);

        Mockito.doReturn(new CreateSpecialOrderResponse().approvedItems(List.of(
                new ApprovedSpecialOrderItem()
                    .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111"))
                    .demandId(113154600L))
            )
        ).when(replenishmentService).specialOrderRequestCreateAll(any());

        SessionUtils.check(session, ticket);
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
    }

    @Test
    public void economicMetricsRowWithoutSupplierIdTest() {
        sskuStatusRepository.save(sskuStatus(111, "shop-sku-111", INACTIVE, "comment1", null));
        var shopSkus = List.of(new ServiceOfferKey(111, "shop-sku-111"));
        var keyMetaMap = Map.of(
            new ServiceOfferKey(111, "shop-sku-111"),
            new KeyMetaV2().addSpecialOrderData(
                new SpecialOrderData(SOFINO_ID, 5L, SpecialOrderType.NEW,
                    BigDecimal.valueOf(100.0), 1, LocalDate.now().minusDays(1), null))
        );
        var ticket = facade.start(new StartRequest<>(shopSkus, new MetaV2(), keyMetaMap));
        executor.run();

        var userAttachment = getExcelFrom("excel_files/pendingRowWithoutSupplierId.xlsx");
        SessionUtils.addExcelAttachment(session, ticket, "test.xlsx", Instant.now().plusSeconds(1),
            userAttachment, user);

        Mockito.doReturn(new CreateSpecialOrderResponse().approvedItems(List.of(
                new ApprovedSpecialOrderItem()
                    .key(new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111"))
                    .demandId(113154600L))
            )
        ).when(replenishmentService).specialOrderRequestCreateAll(any());

        SessionUtils.check(session, ticket);
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
    }

    @Test
    public void sskuStatusChangedBeforeCreateSpecialOrderForAllTest() {
        sskuStatusRepository.save(
            sskuStatus(111, "shop-sku-111", INACTIVE, "comment1"),
            sskuStatus(222, "shop-sku-222", DELISTED, "comment2")
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
                        new SpecialOrderData(SOFINO_ID, 2L, null, null, 1, LocalDate.now(), null),
                        new SpecialOrderData(ROSTOV_ID, 20L, null, null, 1, LocalDate.now(), null)
                    ))));

        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            fromUserExcelComposer.processKeys(shopSkuKeys, strategySpy.getNotCreatedSpecialOrderData(keyMetaMap)),
            user);

        SessionUtils.check(session, ticket);

        Mockito.doReturn(new CreateSpecialOrderResponse().declinedItems(List.of(
                new DeclinedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111")
                ).error("some error")
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
                        .orderDateType(SpecialOrderDateType.TODAY).quantity(2L).quantum(1),
                    new CreateSpecialOrderItem().key(
                            new SpecialOrderCreateKey().warehouseId(ROSTOV_ID).ssku("000222.shop-sku-222")
                        ).price(BigDecimal.TEN).orderType(SpecialOrderType.NEW).deliveryDate(LocalDate.now())
                        .orderDateType(SpecialOrderDateType.TODAY).quantity(20L).quantum(1),
                    new CreateSpecialOrderItem().key(
                            new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000222.shop-sku-222")
                        ).price(BigDecimal.TEN).orderType(SpecialOrderType.NEW).deliveryDate(LocalDate.now())
                        .orderDateType(SpecialOrderDateType.TODAY).quantity(2L).quantum(1)
                )));

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PROCESSED);

        var sskuStatus = sskuStatusRepository.findByKey(111, "shop-sku-111").get();
        sskuStatus.setAvailability(INACTIVE_TMP)
            .setReason(NO_PURCHASE_PRICE);
        sskuStatusRepository.save(sskuStatus);

        SessionUtils.check(session, ticket);

        Mockito.doReturn(new CreateSpecialOrderResponse().declinedItems(List.of(
                new DeclinedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000222.shop-sku-222")
                ).error("some error")
            ))
        ).when(replenishmentService).specialOrderRequestCreateAll(
            new CreateSpecialOrderRequest().ticketId(ticket)
                .specialOrderItems(List.of(
                    new CreateSpecialOrderItem().key(
                            new SpecialOrderCreateKey().warehouseId(ROSTOV_ID).ssku("000222.shop-sku-222")
                        ).price(BigDecimal.TEN).orderType(SpecialOrderType.NEW).deliveryDate(LocalDate.now())
                        .orderDateType(SpecialOrderDateType.TODAY).quantity(20L).quantum(1),
                    new CreateSpecialOrderItem().key(
                            new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000222.shop-sku-222")
                        ).price(BigDecimal.TEN).orderType(SpecialOrderType.NEW).deliveryDate(LocalDate.now())
                        .orderDateType(SpecialOrderDateType.TODAY).quantity(2L).quantum(1)
                )));

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PROCESSED);

        SessionUtils.check(session, ticket);
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PROCESSED);

        SessionUtils.check(session, ticket);
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PROCESSED);

        //пользователи прогрузили закупочные цены
        sskuStatus = sskuStatusRepository.findByKey(111, "shop-sku-111").get();
        sskuStatus.setAvailability(ACTIVE);
        sskuStatusRepository.save(sskuStatus);

        Mockito.doReturn(new CreateSpecialOrderResponse().approvedItems(List.of(
                new ApprovedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111")
                ).demandId(1234567L),
                new ApprovedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(ROSTOV_ID).ssku("000111.shop-sku-111")
                ).demandId(1234567L),
                new ApprovedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000222.shop-sku-222")
                ).demandId(1234567L),
                new ApprovedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(ROSTOV_ID).ssku("000222.shop-sku-222")
                ).demandId(1234567L)
            ))
        ).when(replenishmentService).specialOrderRequestCreateAll(any());
        SessionUtils.check(session, ticket);

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
    }

    @Test
    public void sskuStatusChangedBeforeCreateSpecialOrderForAnyTest() {
        sskuStatusRepository.save(
            sskuStatus(111, "shop-sku-111", INACTIVE, "comment1"),
            sskuStatus(222, "shop-sku-222", DELISTED, "comment2")
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
                        new SpecialOrderData(SOFINO_ID, 2L, null, null, 1, LocalDate.now(), null),
                        new SpecialOrderData(ROSTOV_ID, 20L, null, null, 1, LocalDate.now(), null)
                    ))));

        SessionUtils.addExcelAttachment(session, ticket, "excel.xlsx", Instant.now().plusSeconds(1),
            fromUserExcelComposer.processKeys(shopSkuKeys, strategySpy.getNotCreatedSpecialOrderData(keyMetaMap)),
            user);

        SessionUtils.check(session, ticket);

        Mockito.doReturn(new CreateSpecialOrderResponse().declinedItems(List.of(
                new DeclinedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111")
                ).error("some error")
            ))
        ).when(replenishmentService).specialOrderRequestCreateAll(any());

        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PROCESSED);

        // статус сскю в процессе изменился из-за ЖЦ
        var sskuStatus = sskuStatusRepository.findByKey(111, "shop-sku-111").get();
        sskuStatus.setAvailability(INACTIVE_TMP)
            .setReason(NO_PURCHASE_PRICE);
        sskuStatusRepository.save(sskuStatus);

        SessionUtils.awaitsActivation(session, ticket);

        Mockito.doReturn(new CreateSpecialOrderResponse().approvedItems(List.of(
                new ApprovedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(ROSTOV_ID).ssku("000222.shop-sku-222")
                ).demandId(1234567L),
                new ApprovedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000222.shop-sku-222")
                ).demandId(1234567L)
            ))
        ).when(replenishmentService).specialOrderRequestCreateAny(
            new CreateSpecialOrderRequest().ticketId(ticket)
                .specialOrderItems(List.of(
                    new CreateSpecialOrderItem().key(
                            new SpecialOrderCreateKey().warehouseId(ROSTOV_ID).ssku("000222.shop-sku-222")
                        ).price(BigDecimal.TEN).orderType(SpecialOrderType.NEW).deliveryDate(LocalDate.now())
                        .orderDateType(SpecialOrderDateType.TODAY).quantity(20L).quantum(1),
                    new CreateSpecialOrderItem().key(
                            new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000222.shop-sku-222")
                        ).price(BigDecimal.TEN).orderType(SpecialOrderType.NEW).deliveryDate(LocalDate.now())
                        .orderDateType(SpecialOrderDateType.TODAY).quantity(2L).quantum(1)
                )));
        executor.run();
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.PROCESSED);

        //пользователи прогрузили закупочные цены
        sskuStatus = sskuStatusRepository.findByKey(111, "shop-sku-111").get();
        sskuStatus.setAvailability(ACTIVE);
        sskuStatusRepository.save(sskuStatus);

        SessionUtils.awaitsActivation(session, ticket);

        Mockito.doReturn(new CreateSpecialOrderResponse().approvedItems(List.of(
                new ApprovedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(ROSTOV_ID).ssku("000111.shop-sku-111")
                ).demandId(1234567L),
                new ApprovedSpecialOrderItem().key(
                    new SpecialOrderCreateKey().warehouseId(SOFINO_ID).ssku("000111.shop-sku-111")
                ).demandId(1234567L)
            ))
        ).when(replenishmentService).specialOrderRequestCreateAny(
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
        Assertions.assertThat(facade.findTicketStatus(ticket).getState()).isEqualTo(TicketState.CLOSED);
    }
}

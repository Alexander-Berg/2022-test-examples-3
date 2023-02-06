package ru.yandex.market.checkout.checkouter.storage.returns;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnDelivery;
import ru.yandex.market.checkout.checkouter.returns.ReturnDeliveryHistory;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkouter.jooq.tables.records.ReturnDeliveryHistoryRecord;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;
import static ru.yandex.market.checkouter.jooq.Tables.RETURN_DELIVERY_HISTORY;

public class ReturnDeliveryHistoryDaoTest extends AbstractWebTestBase {

    private static final Long DELIVERY_SERVICE_ID = 123L;

    @Autowired
    ReturnDeliveryHistoryDao returnDeliveryHistoryDao;

    @Autowired
    ReturnDeliveryDao returnDeliveryDao;

    @Autowired
    ReturnHelper returnHelper;

    @Autowired
    ReturnsDao returnsDao;

    @Autowired
    private DSLContext dsl;

    private Order order;
    private Return aReturn;

    @Test
    public void insertAndReadReturnDeliveryWithHistory() {
        ReturnDelivery returnDelivery = returnHelper.getDefaultReturnDelivery();
        returnDelivery.setReturnId(aReturn.getId());
        final ReturnDelivery re = createReturnDeliveryInDb(returnDelivery);
        assertThat(re.getId(), allOf(notNullValue(), not(equalTo(0L))));
        Optional<ReturnDelivery> readRe = returnDeliveryDao.findReturnDeliveryById(re.getId());
        assertTrue(readRe.isPresent(), "Return delivery not found");
        ClientInfo clientInfo = ClientInfo.builder(ClientRole.USER).withId(123L).build();
        ReturnDeliveryHistory entity = createReturnDeliveryHistory(readRe.get());
        createReturnDeliveryHistoryInDb(entity, clientInfo);

        List<ReturnDeliveryHistory> histories = getReturnDeliveryHistoryByReturnDeliveryId(readRe.get().getId());
        assertThat(histories.size(), equalTo(1));

        ReturnDeliveryHistory entity2 = createReturnDeliveryHistory(readRe.get());
        createReturnDeliveryHistoryInDb(entity2, clientInfo);

        histories = getReturnDeliveryHistoryByReturnDeliveryId(readRe.get().getId());
        assertThat(histories.size(), equalTo(2));

        assertThat(histories.get(1).getPrevReturnDelivery(), notNullValue());

        List<ReturnDeliveryHistory> deliveryHistoriesByReturn = getReturnDeliveryHistoryByReturnId(aReturn.getId());
        assertEquals(2, deliveryHistoriesByReturn.size());
    }

    private List<ReturnDeliveryHistory> getReturnDeliveryHistoryByReturnDeliveryId(Long returnDeliveryId) {
        return dsl.selectFrom(RETURN_DELIVERY_HISTORY)
                .where(RETURN_DELIVERY_HISTORY.ID.eq(returnDeliveryId))
                .orderBy(RETURN_DELIVERY_HISTORY.HISTORY_ID)
                .fetch(this::fetchReturnDeliveryHistory);
    }

    private List<ReturnDeliveryHistory> getReturnDeliveryHistoryByReturnId(Long returnId) {
        return dsl.selectFrom(RETURN_DELIVERY_HISTORY)
                .where(RETURN_DELIVERY_HISTORY.RETURN_ID.eq(returnId))
                .orderBy(RETURN_DELIVERY_HISTORY.HISTORY_ID)
                .fetch(this::fetchReturnDeliveryHistory);
    }

    @Nullable
    private ReturnDeliveryHistory getReturnDeliveryHistoryByHistoryId(@Nullable Long historyId) {
        if (historyId == null) {
            return null;
        }
        return dsl.selectFrom(RETURN_DELIVERY_HISTORY)
                .where(RETURN_DELIVERY_HISTORY.HISTORY_ID.eq(historyId))
                .fetchOne(this::fetchReturnDeliveryHistory);
    }

    @NotNull
    private ReturnDeliveryHistory fetchReturnDeliveryHistory(ReturnDeliveryHistoryRecord r) {
        ReturnDeliveryHistory history = new ReturnDeliveryHistory();
        history.setHistoryId(r.getHistoryId());
        history.setId(r.getId());
        history.setReturnId(r.getReturnId());
        history.setPrevReturnDelivery(getReturnDeliveryHistoryByHistoryId(r.getPrevHistoryId()));
        return history;
    }

    private ReturnDelivery createReturnDeliveryInDb(ReturnDelivery returnToSave) {
        Long id = transactionTemplate.execute((txStatus) -> returnDeliveryDao
                .insertReturnDelivery(returnToSave, order.getId()));
        if (id == null) {
            throw new RuntimeException("Return delivery not saved");
        }
        ReturnDelivery savedRet = returnDeliveryDao.findReturnDeliveryById(id)
                .orElseThrow(() -> new RuntimeException("Return Delivery not found"));
        assertThat(savedRet.getId(), equalTo(id));
        return savedRet;
    }

    @Nonnull
    private ReturnDeliveryHistory createReturnDeliveryHistory(ReturnDelivery readRe) {
        Instant now = Instant.now();
        ReturnDeliveryHistory entity = new ReturnDeliveryHistory();
        entity.setId(readRe.getId());
        entity.setReturnId(aReturn.getId());
        entity.setDeliveryServiceId(DELIVERY_SERVICE_ID);
        entity.setType(DeliveryType.PICKUP);
        entity.setStatusUpdatedAt(now);
        entity.setStatus(readRe.getStatus());
        return entity;
    }

    private void createReturnDeliveryHistoryInDb(ReturnDelivery returnDelivery, ClientInfo clientInfo) {
        transactionTemplate.execute((txStatus) ->
                returnDeliveryHistoryDao.insertReturnDeliveryHistory(returnDelivery, clientInfo));
    }


    /**
     * Поскольку у нас есть жёсткие ссылки в БД, нам надо заиметь некоторые сущности
     */
    @BeforeEach
    public void setUpSuite() {
        Pair<Order, Return> orderAndReturn = returnHelper.createOrderAndReturn(defaultBlueOrderParameters(), null);
        order = orderAndReturn.getFirst();
        aReturn = orderAndReturn.getSecond();
    }

}

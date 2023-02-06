package ru.yandex.market.checkout.checkouter.tasks.v2.personaldata;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractArchiveWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.Recipient;
import ru.yandex.market.checkout.checkouter.delivery.RecipientPerson;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.archive.requests.OrderMovingDirection;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.storage.StorageType;
import ru.yandex.market.checkout.checkouter.tasks.v2.AbstractTask;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskResult;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskRunType;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskStageType;
import ru.yandex.market.checkout.checkouter.tasks.v2.factory.BasePartitionTaskV2Factory;
import ru.yandex.market.checkout.checkouter.tasks.v2.tms.TaskPropertiesDao;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.ReturnProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.checkouter.jooq.tables.records.DeliveryAddressRecord;
import ru.yandex.market.checkouter.jooq.tables.records.OrderBuyerHistoryRecord;
import ru.yandex.market.checkouter.jooq.tables.records.OrderBuyerRecord;
import ru.yandex.market.checkouter.jooq.tables.records.ReturnHistoryRecord;
import ru.yandex.market.checkouter.jooq.tables.records.ReturnRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkouter.jooq.Tables.DELIVERY_ADDRESS;
import static ru.yandex.market.checkouter.jooq.Tables.ORDER_BUYER;
import static ru.yandex.market.checkouter.jooq.Tables.ORDER_BUYER_HISTORY;
import static ru.yandex.market.checkouter.jooq.Tables.RETURN;
import static ru.yandex.market.checkouter.jooq.Tables.RETURN_HISTORY;

public class MigratePersonalDataTest extends AbstractArchiveWebTestBase {

    @Autowired
    private MigrateDeliveryAddressPersonalDataTaskV2Factory migrateDeliveryAddressPersonalDataTaskV2Factory;
    @Autowired
    private MigrateBuyerPersonalDataTaskV2Factory migrateBuyerPersonalDataTaskV2Factory;
    @Autowired
    private MigrateBuyerHistoryPersonalDataTaskV2Factory migrateBuyerHistoryPersonalDataTaskV2Factory;
    @Autowired
    private MigrateReturnPersonalDataTaskV2Factory migrateReturnPersonalDataTaskV2Factory;
    @Autowired
    private MigrateReturnHistoryPersonalDataTaskV2Factory migrateReturnHistoryPersonalDataTaskV2Factory;
    @Autowired
    private ReturnHelper returnHelper;
    @Autowired
    private TaskPropertiesDao taskPropertiesDao;

    @Test
    public void shouldCreateMigrationTaskForEachDatabase() {
        assertEquals(3, migrateDeliveryAddressPersonalDataTaskV2Factory.getTasks().size());
        assertEquals(3, migrateBuyerPersonalDataTaskV2Factory.getTasks().size());
        assertEquals(3, migrateBuyerHistoryPersonalDataTaskV2Factory.getTasks().size());
        assertEquals(3, migrateReturnPersonalDataTaskV2Factory.getTasks().size());
    }

    @Test
    public void migrateOrderRelatedPersonalDataSuccess() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        Buyer buyer = parameters.getBuyer();
        buyer.setPersonalPhoneId(null);
        buyer.setPersonalEmailId(null);
        buyer.setPersonalFullNameId(null);
        Recipient recipient = new Recipient(
                new RecipientPerson(buyer.getFirstName(), buyer.getMiddleName(), buyer.getLastName()), null,
                buyer.getPhone(), null, buyer.getEmail(), null
        );
        parameters.getOrder().getDelivery().setRecipient(recipient);
        parameters.getOrder().getDelivery().setBuyerAddress(AddressProvider.getAddress(recipient));
        orderCreateHelper.createOrder(parameters);
        personalMockConfigurer.mockV1MultiTypesStore();
        AbstractTask<?> migrateDeliveryAddress = enableTask(migrateDeliveryAddressPersonalDataTaskV2Factory, 0);
        AbstractTask<?> migrateBuyer = enableTask(migrateBuyerPersonalDataTaskV2Factory, 0);
        AbstractTask<?> migrateBuyerHistory = enableTask(migrateBuyerHistoryPersonalDataTaskV2Factory, 0);

        TaskResult migrateDeliveryAddressResult = migrateDeliveryAddress.run(TaskRunType.ONCE);
        TaskResult migrateBuyerResult = migrateBuyer.run(TaskRunType.ONCE);
        TaskResult migrateBuyerHistoryResult = migrateBuyerHistory.run(TaskRunType.ONCE);

        assertTaskSuccess(3, migrateDeliveryAddressResult);
        assertTaskSuccess(3, migrateBuyerResult);
        assertTaskSuccess(3, migrateBuyerHistoryResult);

        DeliveryAddressRecord deliveryAddressRecord = dsl.selectFrom(DELIVERY_ADDRESS).fetchSingle();
        assertEquals("0123456789abcdef0123456789abcdef", deliveryAddressRecord.getPersonalPhoneId());
        assertEquals("4621897c54fd9ef81e33c0502bd6ab7a", deliveryAddressRecord.getPersonalEmailId());
        assertEquals("81e33d098f095f67b1622ccde7a4a5b4", deliveryAddressRecord.getPersonalFullNameId());
        String deliveryAddressLastId = taskPropertiesDao.getPayload(migrateDeliveryAddress.getTaskName());
        assertEquals(List.of(deliveryAddressRecord.getId()).toString(), deliveryAddressLastId);

        OrderBuyerRecord orderBuyerRecord = dsl.selectFrom(ORDER_BUYER).fetchSingle();
        assertEquals("0123456789abcdef0123456789abcdef", orderBuyerRecord.getPersonalPhoneId());
        assertEquals("4621897c54fd9ef81e33c0502bd6ab7a", orderBuyerRecord.getPersonalEmailId());
        assertEquals("81e33d098f095f67b1622ccde7a4a5b4", orderBuyerRecord.getPersonalFullNameId());
        String orderBuyerLastId = taskPropertiesDao.getPayload(migrateBuyer.getTaskName());
        assertEquals(List.of(orderBuyerRecord.getId()).toString(), orderBuyerLastId);

        OrderBuyerHistoryRecord orderBuyerHistoryRecord = dsl.selectFrom(ORDER_BUYER_HISTORY).fetchSingle();
        assertEquals("0123456789abcdef0123456789abcdef", orderBuyerHistoryRecord.getPersonalPhoneId());
        assertEquals("4621897c54fd9ef81e33c0502bd6ab7a", orderBuyerHistoryRecord.getPersonalEmailId());
        assertEquals("81e33d098f095f67b1622ccde7a4a5b4", orderBuyerHistoryRecord.getPersonalFullNameId());
        String orderBuyerHistoryLastId = taskPropertiesDao.getPayload(migrateBuyerHistory.getTaskName());
        assertEquals(List.of(orderBuyerHistoryRecord.getHistoryId(), orderBuyerHistoryRecord.getId()).toString(),
                orderBuyerHistoryLastId);
    }

    @Test
    public void migrateBuyerPhoneInArchiveSuccess() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.getBuyer().setPersonalPhoneId(null);
        parameters.getBuyer().setPersonalEmailId("4621897c54fd9ef81e33c0502bd6ab7a");
        parameters.getBuyer().setPersonalFullNameId("81e33d098f095f67b1622ccde7a4a5b4");
        orderCreateHelper.createOrder(parameters);
        moveArchivingData(OrderMovingDirection.BASIC_TO_ARCHIVE, 0);
        personalMockConfigurer.mockV1MultiTypesStore();
        AbstractTask<?> migrateBuyerPhone = enableTask(migrateBuyerPersonalDataTaskV2Factory, 1);

        TaskResult migrateBuyerPhoneResult = migrateBuyerPhone.run(TaskRunType.ONCE);

        assertTaskSuccess(1, migrateBuyerPhoneResult);
        OrderBuyerRecord orderBuyerRecord = archiveStorageManager.doWithArchiveContext(0, () ->
                getDsl(StorageType.ARCHIVE).selectFrom(ORDER_BUYER).fetchSingle()
        );
        assertEquals("0123456789abcdef0123456789abcdef", orderBuyerRecord.getPersonalPhoneId());
        String orderBuyerLastId = taskPropertiesDao.getPayload(migrateBuyerPhone.getTaskName());
        assertEquals(List.of(orderBuyerRecord.getId()).toString(), orderBuyerLastId);
    }

    @Test
    public void shouldSkipWrongData() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.getBuyer().setPersonalPhoneId(null);
        orderCreateHelper.createOrder(parameters);
        personalMockConfigurer.mockV1MultiTypesStoreInvalidNumber();
        AbstractTask<?> migrateBuyerPhone = enableTask(migrateBuyerPersonalDataTaskV2Factory, 0);

        TaskResult migrateBuyerPhoneResult = migrateBuyerPhone.run(TaskRunType.ONCE);

        assertEquals(TaskStageType.SUCCESS, migrateBuyerPhoneResult.getStage());
        assertEquals(0, migrateBuyerPhoneResult.getSuccess());
        assertEquals(3, migrateBuyerPhoneResult.getFailed());
        OrderBuyerRecord orderBuyerRecord = dsl.selectFrom(ORDER_BUYER).fetchSingle();
        assertNull(orderBuyerRecord.getPersonalPhoneId());
        String orderBuyerLastId = taskPropertiesDao.getPayload(migrateBuyerPhone.getTaskName());
        assertEquals(List.of(orderBuyerRecord.getId()).toString(), orderBuyerLastId);
    }

    @Test
    public void shouldRetryWhenPersonalUnavailable() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.getBuyer().setPersonalPhoneId(null);
        orderCreateHelper.createOrder(parameters);
        personalMockConfigurer.mockUnavailable(502);
        AbstractTask<?> migrateBuyerPhone = enableTask(migrateBuyerPersonalDataTaskV2Factory, 0);

        TaskResult migrateBuyerPhoneResult = migrateBuyerPhone.run(TaskRunType.ONCE);

        assertEquals(TaskStageType.SUCCESS, migrateBuyerPhoneResult.getStage());
        assertEquals(0, migrateBuyerPhoneResult.getSuccess());
        assertEquals(1, migrateBuyerPhoneResult.getFailed());
        OrderBuyerRecord orderBuyerRecord = dsl.selectFrom(ORDER_BUYER).fetchSingle();
        assertNull(orderBuyerRecord.getPersonalPhoneId());
        String orderBuyerLastId = taskPropertiesDao.getPayload(migrateBuyerPhone.getTaskName());
        assertNull(orderBuyerLastId, "Last ID must not be set to retry on next run");
    }

    @Test
    public void migrateReturnRelatedPersonalDataSuccess() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        Return ret = ReturnProvider.generateReturn(order);
        ret.setUserPhone("+79998887766");
        ret.setPersonalPhoneId(null);
        ret.setUserEmail("user@example.com");
        ret.setPersonalEmailId(null);
        ret.getBankDetails().setPersonalFullNameId(null);
        returnHelper.createReturn(order.getId(), ret);
        personalMockConfigurer.mockV1MultiTypesStore();
        AbstractTask<?> migrateReturn = enableTask(migrateReturnPersonalDataTaskV2Factory, 0);
        AbstractTask<?> migrateReturnHistory = enableTask(migrateReturnHistoryPersonalDataTaskV2Factory, 0);

        TaskResult migrateReturnResult = migrateReturn.run(TaskRunType.ONCE);
        TaskResult migrateReturnHistoryResult = migrateReturnHistory.run(TaskRunType.ONCE);

        assertTaskSuccess(3, migrateReturnResult);
        ReturnRecord returnRecord = dsl.selectFrom(RETURN).fetchSingle();
        assertEquals("14e3595b076f62fb1cdc84e23a87a09d", returnRecord.getPersonalPhoneId());
        assertEquals("c6edd583a6774a2b2b1f5f980e09c341", returnRecord.getPersonalEmailId());
        assertEquals("94108c324cfba2f0e676e587a3b5dd91", returnRecord.getPersonalFullNameId());
        String returnLastId = taskPropertiesDao.getPayload(migrateReturn.getTaskName());
        assertEquals(List.of(returnRecord.getId()).toString(), returnLastId);

        assertTaskSuccess(2, migrateReturnHistoryResult);
        List<ReturnHistoryRecord> returnHistoryRecords = dsl.selectFrom(RETURN_HISTORY).fetch();
        assertEquals(2, returnHistoryRecords.size());
        returnHistoryRecords.forEach(returnHistoryRecord ->
                assertEquals("94108c324cfba2f0e676e587a3b5dd91", returnHistoryRecord.getPersonalFullNameId())
        );
        long returnHistoryMaxId = returnHistoryRecords.stream()
                .mapToLong(ReturnHistoryRecord::getId)
                .max().orElseThrow();
        String returnHistoryLastId = taskPropertiesDao.getPayload(migrateReturn.getTaskName());
        assertEquals(List.of(returnHistoryMaxId).toString(), returnHistoryLastId);
    }

    private AbstractTask<?> enableTask(BasePartitionTaskV2Factory taskFactory, int partition) {
        AbstractTask<?> task = taskFactory.getTasks().get(partition);
        taskPropertiesDao.save(task.getTaskName());
        taskPropertiesDao.setEnabled(task.getTaskName(), true);
        return task;
    }

    private void assertTaskSuccess(int successCount, TaskResult result) {
        assertEquals(TaskStageType.SUCCESS, result.getStage());
        assertEquals(successCount, result.getSuccess());
        assertEquals(0, result.getFailed());
    }
}


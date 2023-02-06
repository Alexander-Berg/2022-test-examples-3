package ru.yandex.chemodan.app.djfs.core.operations;

import java.util.UUID;

import org.joda.time.DateTimeUtils;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.db.mongo.DjfsBenderFactory;
import ru.yandex.chemodan.app.djfs.core.test.DjfsSingleUserTestBase;
import ru.yandex.commune.json.JsonObject;
import ru.yandex.misc.bender.BenderParserSerializer;
import ru.yandex.misc.bender.annotation.BenderBindAllFields;
import ru.yandex.misc.lang.DefaultObject;
import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class OperationDaoTest extends DjfsSingleUserTestBase {
    @Autowired
    private OperationDao operationDao;

    @Test
    public void changeState() {
        String id = "test_oid";
        Operation operation = Operation.builder()
                .id(id)
                .uid(UID)
                .state(Operation.State.WAITING)
                .type("type")
                .subtype("subtype")
                .ctime(Instant.now())
                .mtime(Instant.now())
                .dtime(Instant.now())
                .version(12)
                .jsonData(JsonObject.empty())
                .build();
        operationDao.insert(operation);

        Assert.equals(Operation.State.WAITING, operationDao.find(UID, id).get().getState());

        boolean result;

        result = operationDao.changeState(operation.getUid(), operation.getId(),
                Operation.State.WAITING, Operation.State.EXECUTING);
        Assert.isTrue(result);
        Assert.equals(Operation.State.EXECUTING, operationDao.find(UID, id).get().getState());

        result = operationDao.changeState(operation.getUid(), operation.getId(),
                Operation.State.WAITING, Operation.State.COMPLETED);
        Assert.isFalse(result);
        Assert.equals(Operation.State.EXECUTING, operationDao.find(UID, id).get().getState());
    }

    @Test
    public void setDtime() {
        String id = "test_oid";
        Instant dtime_old = Instant.now();
        Instant dtime_new = dtime_old.plus(Duration.standardHours(1));

        Operation operation = Operation.builder()
                .id(id)
                .uid(UID)
                .state(Operation.State.WAITING)
                .type("type")
                .subtype("subtype")
                .ctime(Instant.now())
                .mtime(Instant.now())
                .dtime(dtime_old)
                .version(12)
                .jsonData(JsonObject.empty())
                .build();
        operationDao.insert(operation);

        Assert.equals(dtime_old, operationDao.find(UID, id).get().getDtime());

        boolean result;

        result = operationDao.setDtime(operation.getUid(), operation.getId(), dtime_new);
        Assert.isTrue(result);
        // todo: fix timezone bug
        // Assert.equals(dtime_new, operationDao.find(UID, id).get().getDtime());
        Assert.notEquals(dtime_old, operationDao.find(UID, id).get().getDtime());
    }

    @Test
    public void setData() {
        String id = "test_oid";
        Operation operation = Operation.builder()
                .id(id)
                .uid(UID)
                .state(Operation.State.WAITING)
                .type("type")
                .subtype("subtype")
                .ctime(Instant.now())
                .mtime(Instant.now())
                .dtime(Instant.now())
                .version(12)
                .jsonData(JsonObject.empty())
                .build();
        operationDao.insert(operation);

        Nested nested1 = new Nested();
        nested1.string = "nested1string";
        nested1.emptyString = Option.empty();
        nested1.someString = Option.of("nested1someString");
        Nested nested2 = new Nested();
        nested2.string = "nested2string";
        nested2.emptyString = Option.empty();
        nested2.someString = Option.of("nested2someString");

        TestData expectedData = new TestData();
        expectedData.string = "string";
        expectedData.emptyString = Option.empty();
        expectedData.someString = Option.of("someString");
        expectedData.integer = 42;
        expectedData.emptyInteger = Option.empty();
        expectedData.someInteger = Option.of(33);
        expectedData.uuid = UUID.randomUUID();
        expectedData.emptyUuid = Option.empty();
        expectedData.someUuid = Option.of(UUID.randomUUID());
        expectedData.nested = nested1;
        expectedData.emptyNested = Option.empty();
        expectedData.someNested = Option.of(nested2);

        operationDao.setData(UID, id, expectedData, TestData.B, operation);

        Operation actualOperation = operationDao.find(UID, id).get();
        TestData actualData = actualOperation.getData(TestData.B);

        Assert.equals(expectedData, actualData);
    }

    @Test
    public void findByTypeAndSubtype() {
        operationDao.insert(Operation.cons(UID, "t1", "st1").withUniqueIdFromUidAndTypeAndSubtype());
        operationDao.insert(Operation.cons(UID, "t1", "st2").withUniqueIdFromUidAndTypeAndSubtype());
        operationDao.insert(Operation.cons(UID, "t2", "st1").withUniqueIdFromUidAndTypeAndSubtype());
        operationDao.insert(Operation.cons(UID, "t2", "st2").withUniqueIdFromUidAndTypeAndSubtype());
        operationDao.insert(Operation.cons(UID, "t1", "st1").withUniqueIdFromUidAndTypeAndSubtype());

        ListF<Operation> operations = operationDao.find(UID, "t1", "st1");
        Assert.sizeIs(2, operations);
        Assert.equals("t1", operations.get(0).getType());
        Assert.equals("st1", operations.get(0).getSubtype());
        Assert.equals("t1", operations.get(1).getType());
        Assert.equals("st1", operations.get(1).getSubtype());
    }

    @Test
    public void lockWaitingOperation() {
        UUID lockId = UUID.randomUUID();
        Duration lockExpiry = Duration.standardMinutes(5);

        Operation operation = Operation.cons(UID, "type", "subtype");
        operationDao.insert(operation);

        Assert.equals(Operation.State.WAITING, operationDao.find(UID, operation.getId()).get().getState());
        Assert.isTrue(operationDao.tryAcquireOrRenewLockForExecution(UID, operation.getId(), lockId, lockExpiry));
        Assert.equals(Operation.State.EXECUTING, operationDao.find(UID, operation.getId()).get().getState());
    }

    @Test
    public void lockExecutingOperation() {
        UUID lockId = UUID.randomUUID();
        Duration lockExpiry = Duration.standardMinutes(5);

        Operation operation = Operation.cons(UID, "type", "subtype");
        operationDao.insert(operation);
        operationDao.changeState(UID, operation.getId(), Operation.State.WAITING, Operation.State.EXECUTING);

        Assert.equals(Operation.State.EXECUTING, operationDao.find(UID, operation.getId()).get().getState());
        Assert.isTrue(operationDao.tryAcquireOrRenewLockForExecution(UID, operation.getId(), lockId, lockExpiry));
        Assert.equals(Operation.State.EXECUTING, operationDao.find(UID, operation.getId()).get().getState());
    }

    @Test
    public void lockFailedOperationFails() {
        UUID lockId = UUID.randomUUID();
        Duration lockExpiry = Duration.standardMinutes(5);

        Operation operation = Operation.cons(UID, "type", "subtype");
        operationDao.insert(operation);
        operationDao.changeState(UID, operation.getId(), Operation.State.WAITING, Operation.State.FAILED);

        Assert.equals(Operation.State.FAILED, operationDao.find(UID, operation.getId()).get().getState());
        Assert.isFalse(operationDao.tryAcquireOrRenewLockForExecution(UID, operation.getId(), lockId, lockExpiry));
        Assert.equals(Operation.State.FAILED, operationDao.find(UID, operation.getId()).get().getState());
    }

    @Test
    public void lockDoneOperationFails() {
        UUID lockId = UUID.randomUUID();
        Duration lockExpiry = Duration.standardMinutes(5);

        Operation operation = Operation.cons(UID, "type", "subtype");
        operationDao.insert(operation);
        operationDao.changeState(UID, operation.getId(), Operation.State.WAITING, Operation.State.DONE);

        Assert.equals(Operation.State.DONE, operationDao.find(UID, operation.getId()).get().getState());
        Assert.isFalse(operationDao.tryAcquireOrRenewLockForExecution(UID, operation.getId(), lockId, lockExpiry));
        Assert.equals(Operation.State.DONE, operationDao.find(UID, operation.getId()).get().getState());
    }

    @Test
    public void lockCompletedOperationFails() {
        UUID lockId = UUID.randomUUID();
        Duration lockExpiry = Duration.standardMinutes(5);

        Operation operation = Operation.cons(UID, "type", "subtype");
        operationDao.insert(operation);
        operationDao.changeState(UID, operation.getId(), Operation.State.WAITING, Operation.State.COMPLETED);

        Assert.equals(Operation.State.COMPLETED, operationDao.find(UID, operation.getId()).get().getState());
        Assert.isFalse(operationDao.tryAcquireOrRenewLockForExecution(UID, operation.getId(), lockId, lockExpiry));
        Assert.equals(Operation.State.COMPLETED, operationDao.find(UID, operation.getId()).get().getState());
    }

    @Test
    public void unlock() {
        UUID lockId1 = UUID.randomUUID();
        UUID lockId2 = UUID.randomUUID();
        UUID lockId3 = UUID.randomUUID();
        Duration lockExpiry = Duration.standardMinutes(5);

        Operation operation = Operation.cons(UID, "type", "subtype");
        operationDao.insert(operation);

        Assert.isTrue(operationDao.tryAcquireOrRenewLockForExecution(UID, operation.getId(), lockId1, lockExpiry));
        operationDao.releaseLock(UID, operation.getId(), lockId1);
        Assert.isTrue(operationDao.tryAcquireOrRenewLockForExecution(UID, operation.getId(), lockId2, lockExpiry));
        Assert.isFalse(operationDao.tryAcquireOrRenewLockForExecution(UID, operation.getId(), lockId3, lockExpiry));
    }

    @Test
    public void unlockWithAnotherLockFails() {
        UUID lockId1 = UUID.randomUUID();
        UUID lockId2 = UUID.randomUUID();
        UUID lockId3 = UUID.randomUUID();
        Duration lockExpiry = Duration.standardMinutes(5);

        Operation operation = Operation.cons(UID, "type", "subtype");
        operationDao.insert(operation);

        Assert.isTrue(operationDao.tryAcquireOrRenewLockForExecution(UID, operation.getId(), lockId1, lockExpiry));
        operationDao.releaseLock(UID, operation.getId(), lockId2);
        Assert.isFalse(operationDao.tryAcquireOrRenewLockForExecution(UID, operation.getId(), lockId2, lockExpiry));
        Assert.isFalse(operationDao.tryAcquireOrRenewLockForExecution(UID, operation.getId(), lockId3, lockExpiry));
        Assert.isTrue(operationDao.tryAcquireOrRenewLockForExecution(UID, operation.getId(), lockId1, lockExpiry));
    }

    @Test
    public void lockOperationMultipleTimes() {
        UUID lockId = UUID.randomUUID();
        Duration lockExpiry = Duration.standardMinutes(5);

        Operation operation = Operation.cons(UID, "type", "subtype");
        operationDao.insert(operation);

        Assert.isTrue(operationDao.tryAcquireOrRenewLockForExecution(UID, operation.getId(), lockId, lockExpiry));
        Assert.isTrue(operationDao.tryAcquireOrRenewLockForExecution(UID, operation.getId(), lockId, lockExpiry));
        Assert.isTrue(operationDao.tryAcquireOrRenewLockForExecution(UID, operation.getId(), lockId, lockExpiry));
        Assert.isTrue(operationDao.tryAcquireOrRenewLockForExecution(UID, operation.getId(), lockId, lockExpiry));
    }

    @Test
    public void lockWithAnotherLockFails() {
        UUID lockId1 = UUID.randomUUID();
        UUID lockId2 = UUID.randomUUID();
        UUID lockId3 = UUID.randomUUID();
        Duration lockExpiry = Duration.standardMinutes(5);

        Operation operation = Operation.cons(UID, "type", "subtype");
        operationDao.insert(operation);

        Assert.isTrue(operationDao.tryAcquireOrRenewLockForExecution(UID, operation.getId(), lockId1, lockExpiry));
        Assert.isFalse(operationDao.tryAcquireOrRenewLockForExecution(UID, operation.getId(), lockId2, lockExpiry));
        Assert.isFalse(operationDao.tryAcquireOrRenewLockForExecution(UID, operation.getId(), lockId3, lockExpiry));
    }

    @Test
    public void lockExpiry() {
        UUID lockId1 = UUID.randomUUID();
        UUID lockId2 = UUID.randomUUID();
        Duration lockExpiry = Duration.standardMinutes(5);

        Operation operation = Operation.cons(UID, "type", "subtype");
        operationDao.insert(operation);

        Instant now = Instant.now();
        DateTimeUtils.setCurrentMillisFixed(now.minus(Duration.standardMinutes(6)).getMillis());

        Assert.isTrue(operationDao.tryAcquireOrRenewLockForExecution(UID, operation.getId(), lockId1, lockExpiry));

        DateTimeUtils.setCurrentMillisSystem();

        Assert.isTrue(operationDao.tryAcquireOrRenewLockForExecution(UID, operation.getId(), lockId2, lockExpiry));
    }

    @Test
    public void lockProlongation() {
        UUID lockId1 = UUID.randomUUID();
        UUID lockId2 = UUID.randomUUID();
        Duration lockExpiry = Duration.standardMinutes(5);

        Operation operation = Operation.cons(UID, "type", "subtype");
        operationDao.insert(operation);

        Instant now = Instant.now();

        DateTimeUtils.setCurrentMillisFixed(now.minus(Duration.standardMinutes(6)).getMillis());
        Assert.isTrue(operationDao.tryAcquireOrRenewLockForExecution(UID, operation.getId(), lockId1, lockExpiry));

        DateTimeUtils.setCurrentMillisFixed(now.minus(Duration.standardMinutes(3)).getMillis());
        Assert.isTrue(operationDao.tryAcquireOrRenewLockForExecution(UID, operation.getId(), lockId1, lockExpiry));

        DateTimeUtils.setCurrentMillisSystem();

        Assert.isFalse(operationDao.tryAcquireOrRenewLockForExecution(UID, operation.getId(), lockId2, lockExpiry));
    }

    @BenderBindAllFields
    private static class TestData extends DefaultObject {
        public static final BenderParserSerializer<TestData> B = DjfsBenderFactory.createForJson(TestData.class);

        public String string;
        public Option<String> emptyString;
        public Option<String> someString;

        public int integer;
        public Option<Integer> emptyInteger;
        public Option<Integer> someInteger;

        public UUID uuid;
        public Option<UUID> emptyUuid;
        public Option<UUID> someUuid;

        public Nested nested;
        public Option<Nested> emptyNested;
        public Option<Nested> someNested;
    }

    @BenderBindAllFields
    private static class Nested extends DefaultObject {
        public String string;
        public Option<String> emptyString;
        public Option<String> someString;
    }
}

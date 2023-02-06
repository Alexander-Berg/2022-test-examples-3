package ru.yandex.direct.rbac;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.assertj.core.api.SoftAssertions;
import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardKey;
import ru.yandex.direct.dbutil.sharding.ShardSupport;
import ru.yandex.direct.rbac.configuration.RbacTest;
import ru.yandex.direct.rbac.model.ClientsRelationType;

import static org.assertj.core.api.Assertions.assertThat;

@RbacTest
@ExtendWith(SpringExtension.class)
public class RbacMobileGoalAccessClientsRelationsTest {

    @Autowired
    private RbacClientsRelations rbacClientsRelations;

    @Autowired
    private RbacClientsRelationsStorage rbacClientsRelationsStorage;


    @Autowired
    ShardSupport shardSupport;

    private ClientId consumer1;
    private ClientId consumer2;
    private ClientId owner1;
    private ClientId owner2;

    private final static AtomicLong currentClientId = new AtomicLong(111111L);
    private ClientId anotherConsumer1;
    private ClientId anotherConsumer2;
    private ClientId anotherOwner1;

    @BeforeEach
    public void beforeEach() throws Exception {
        consumer1 = createFakeClientWithId(1);
        consumer2 = createFakeClientWithId(2);
        owner1 = createFakeClientWithId(1);
        owner2 = createFakeClientWithId(2);
        rbacClientsRelations.addMobileGoalsAccessRelations(List.of(consumer1, consumer2), owner1);
        rbacClientsRelations.addMobileGoalsAccessRelations(List.of(consumer1), owner2);

        ClientId consumer3toBeRemoved = createFakeClientWithId(1);
        rbacClientsRelations.addMobileGoalsAccessRelations(List.of(consumer3toBeRemoved), owner2);
        ClientId consumer4toBeRemoved = createFakeClientWithId(2);
        rbacClientsRelations.addMobileGoalsAccessRelations(List.of(consumer4toBeRemoved), owner2);

        rbacClientsRelations.removeMobileGoalsAccessRelations(
                List.of(consumer3toBeRemoved, consumer4toBeRemoved), owner2);


        anotherConsumer1 = createFakeClientWithId(1);
        anotherConsumer2 = createFakeClientWithId(2);
        anotherOwner1 = createFakeClientWithId(1);

    }

    @Test
    public void testGetOwnersOfMobileGoals() {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(rbacClientsRelations.getOwnersOfMobileGoals(consumer1))
                    .containsExactlyInAnyOrder(owner1, owner2);
            softly.assertThat(rbacClientsRelations.getOwnersOfMobileGoals(consumer2))
                    .containsExactlyInAnyOrder(owner1);
        });
    }

    @Test
    public void testGetConsumersOfMobileGoals() {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(rbacClientsRelations.getConsumersOfMobileGoals(owner1))
                    .containsExactlyInAnyOrder(consumer1, consumer2);
            softly.assertThat(rbacClientsRelations.getConsumersOfMobileGoals(owner2))
                    .containsExactlyInAnyOrder(consumer1);
        });
    }

    @Test
    public void testMassGetOwnersOfMobileGoals() {
        assertThat(rbacClientsRelations.getOwnersOfMobileGoals(List.of(consumer1, consumer2)))
                .usingRecursiveComparison()
                .ignoringCollectionOrder()
                .isEqualTo(Map.of(
                        consumer1, List.of(owner1, owner2),
                        consumer2, List.of(owner1)
                ));
    }

    @Test
    public void testDataAccessExceptionDuringAddCancelWholeAddition() {
        // Добавим неконсистентность в БД для того чтобы добавление reverse_relation ещё раз вызвало исключение
        rbacClientsRelationsStorage.addReverseRelation(
                2, anotherConsumer2, anotherOwner1, ClientsRelationType.MOBILE_GOALS_ACCESS);

        Exception exception = null;
        try {
            rbacClientsRelations.addMobileGoalsAccessRelations(
                    List.of(anotherConsumer1, anotherConsumer2), anotherOwner1);
        } catch (Exception ex) {
            exception = ex;
        }

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(exception)
                .as("Во время выполнения добавления выкинуто исключение")
                .isInstanceOf(DataAccessException.class);
        softly.assertThat(rbacClientsRelations.getConsumersOfMobileGoals(anotherOwner1))
                .as("Доступ к anotherOwner1 не добавился никому")
                .isEmpty();
        softly.assertAll();
    }

    @Test
    public void testAnyExceptionDuringAddCancelWholeAddition() {
        var anotherConsumer3 = createFakeClientWithId(3);
        Exception exception = null;
        try {
            rbacClientsRelations.addMobileGoalsAccessRelations(
                    List.of(anotherConsumer1, anotherConsumer2, anotherConsumer3), anotherOwner1);
        } catch (Exception ex) {
            exception = ex;
        }

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(exception)
                .as("Во время выполнения добавления выкинуто исключение")
                .isInstanceOf(Exception.class);
        softly.assertThat(rbacClientsRelations.getConsumersOfMobileGoals(anotherOwner1))
                .as("Доступ к anotherOwner1 не добавился никому")
                .isEmpty();
        softly.assertAll();
    }

    private ClientId createFakeClientWithId(int shard) {
        long clientId = currentClientId.getAndIncrement();
        shardSupport.saveValue(ShardKey.CLIENT_ID, clientId, ShardKey.SHARD, shard);
        return ClientId.fromLong(clientId);
    }
}

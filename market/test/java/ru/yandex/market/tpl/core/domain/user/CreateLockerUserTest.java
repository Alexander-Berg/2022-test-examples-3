package ru.yandex.market.tpl.core.domain.user;


import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.locker.boxbot.request.BoxBotUserDto;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.external.boxbot.LockerApi;
import ru.yandex.market.tpl.core.service.user.lockerusers.LockerUserProducer;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RequiredArgsConstructor
public class CreateLockerUserTest extends TplAbstractTest {

    private final TransactionTemplate transactionTemplate;
    private final LockerUserProducer lockerUserProducer;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final UserRepository userRepository;
    private final TestUserHelper testUserHelper;
    private final LockerApi lockerApi;

    @Test
    void shouldAddUserToQueue() {
        assertThat(dbQueueTestUtil.getQueue(QueueType.LOCKER_USER)).isEmpty();
        var user1 = createTestUser(11112222L, "Турута Кувончбек", "+79667756611");
        var user2 = createTestUser(21112222L, "Турута Кувончбек2", "+79667756612");

        lockerUserProducer.produce(user1.getExtId());
        lockerUserProducer.produce(user2.getExtId());
        assertThat(dbQueueTestUtil.getQueue(QueueType.LOCKER_USER))
                .containsAll(List.of(Long.toString(user1.getExtId()), Long.toString(user2.getExtId())));

        dbQueueTestUtil.executeAllQueueItems(QueueType.LOCKER_USER);
        var userDtoCaptor = ArgumentCaptor.forClass(BoxBotUserDto.class);
        verify(lockerApi, times(2)).createUser(userDtoCaptor.capture());
        var userDtoList = userDtoCaptor.getAllValues();
        var wrongOrder = userDtoList.get(1).getUid() == user1.getUid();
        var createDto1 = wrongOrder ? userDtoList.get(1) : userDtoList.get(0);
        var createDto2 = wrongOrder ? userDtoList.get(0) : userDtoList.get(1);
        verifyUserDataMatch(user1, createDto1);
        verifyUserDataMatch(user2, createDto2);
    }

    private void verifyUserDataMatch(BoxBotUserDto expected, BoxBotUserDto actual) {
        assertThat(actual.getExtId()).isEqualTo(expected.getExtId());
        assertThat(actual.getUid()).isEqualTo(expected.getUid());
        assertThat(actual.getName()).isEqualTo(expected.getName());
        assertThat(actual.getPhone()).isEqualTo(expected.getPhone().substring(1));
    }

    private BoxBotUserDto createTestUser(long uid, String name, String phone) {
        var user = transactionTemplate.execute(status -> {
            var userEntity = testUserHelper.createUserWithoutSchedule(uid);
            userEntity.setName(name);
            userEntity.setPhone(phone);
            return userRepository.save(userEntity);
        });
        return BoxBotUserDto.builder()
                .extId(user.getId())
                .uid(user.getUid())
                .name(user.getName())
                .phone(user.getPhone())
                .build();
    }

}

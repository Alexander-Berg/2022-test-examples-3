package ru.yandex.market.tpl.core.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.transport.RoutingVehicleType;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.domain.user.UserUtil;
import ru.yandex.market.tpl.core.service.user.transport.TransportType;
import ru.yandex.market.tpl.core.service.user.transport.TransportTypeRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author kukabara
 */
@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class UserRepositoryTest {

    private final UserRepository userRepository;
    private final TransportTypeRepository transportTypeRepository;
    private final TestUserHelper testUserHelper;

    @Test
    void add() {
        User user = testUserHelper.createUserWithoutSchedule(197474L);
        userRepository.save(user);

        assertThat(user.getId()).isNotNull();
        assertThat(user.getCreatedAt()).isNotNull();

        Optional<User> foundUser = userRepository.findById(user.getId());
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get()).isEqualTo(user);

        assertThat(userRepository.findByEmail(user.getEmail())).contains(user);
        assertThat(userRepository.findAll()).contains(user);

    }

    @Test
    void findByUids() {
        User user1 = testUserHelper.createUserWithoutSchedule(1974892L);
        User user2 = testUserHelper.createUserWithoutSchedule(1974896L);
        userRepository.save(user1);
        userRepository.save(user2);

        List<User> result = userRepository.findAllByUidIn(List.of(user1.getUid(), user2.getUid()));
        assertThat(result).contains(user1);
        assertThat(result).contains(user2);
    }

    @Test
    void testFindByTransportTypeId() {
        TransportType transportType = new TransportType();
        transportType.setName("some name");
        transportType.setCapacity(BigDecimal.valueOf(1.3));
        transportType.setRoutingPriority(100);
        transportType.setRoutingVehicleType(RoutingVehicleType.COMMON);
        transportType.setPalletsCapacity(0);
        TransportType savedTransportType = transportTypeRepository.saveAndFlush(transportType);

        User user = testUserHelper.createUserWithoutSchedule(197474L);
        UserUtil.setTransportType(user, savedTransportType);
        userRepository.saveAndFlush(user);

        long count1 = userRepository.countByTransportType(savedTransportType);
        assertThat(count1).isEqualTo(1);

    }

    @Test
    void testFindByDsmExternalId() {
        User user = testUserHelper.createUserWithoutSchedule(197475L);
        String dsmId = "34209220";
        UserUtil.setDsmExternalId(user, dsmId);
        userRepository.saveAndFlush(user);

        Optional<User> optional = userRepository.findUserByDsmExternalId(dsmId);
        assertThat(optional).isPresent();

    }

    @Test
    void testFindAllByDsmExternalId() {
        User user = testUserHelper.createUserWithoutSchedule(197476L);
        String dsmId = "34209221";
        UserUtil.setDsmExternalId(user, dsmId);
        userRepository.saveAndFlush(user);

        Set<User> list = userRepository.findAllByDsmExternalIdIn(Set.of(dsmId, "334535908987645"));
        assertThat(list).hasSize(1);
    }

}

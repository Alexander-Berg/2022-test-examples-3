package ru.yandex.market.global.checkout.factory;

import java.util.function.Function;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.global.checkout.domain.user.UserRepository;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.market.global.db.jooq.tables.pojos.User;

@Transactional
public class TestUserFactory {

    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(TestUserFactory.class).build();

    @Autowired
    private UserRepository userRepository;

    public User createUser() {
        return createUser(Function.identity());
    }

    public User createUser(Function<User, User> setupUser) {
        User user = setupUser.apply(
                RANDOM.nextObject(User.class)
                        .setDeleted(false));
        userRepository.insert(user);
        return user;
    }

}

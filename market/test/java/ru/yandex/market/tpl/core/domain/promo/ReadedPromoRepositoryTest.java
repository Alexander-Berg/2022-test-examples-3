package ru.yandex.market.tpl.core.domain.promo;

import java.util.List;

import javax.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;


@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ReadedPromoRepositoryTest {

    private final UserRepository userRepository;
    private final TestUserHelper testUserHelper;
    private final PromoTestHelper promoTestHelper;
    private final ReadedPromoRepository readedPromoRepository;
    private final EntityManager entityManager;

    @Test
    void save() {
        String promoId = "123";

        promoTestHelper.createPromo(promoId);

        User user = createUser();

        ReadedPromo readedPromo = new ReadedPromo();
        readedPromo.setPromoId(promoId);
        readedPromo.setUserId(user.getId());

        readedPromoRepository.save(readedPromo);
        readedPromoRepository.flush();

        List<ReadedPromo> all = readedPromoRepository.findAll();
        assertThat(all).hasSize(1);

        entityManager.clear();

        ReadedPromo doubleReadedPromo = new ReadedPromo();
        doubleReadedPromo.setPromoId(promoId);
        doubleReadedPromo.setUserId(user.getId());

        readedPromoRepository.save(doubleReadedPromo);
        readedPromoRepository.flush();
        entityManager.clear();

        all = readedPromoRepository.findAll();
        assertThat(all).hasSize(1);

    }

    private User createUser() {
        User user = testUserHelper.createUserWithoutSchedule(1123);

        userRepository.save(user);
        return user;
    }

}

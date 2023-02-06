package ru.yandex.market.global.checkout.domain.user;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.common.test.TestUtil;
import ru.yandex.market.global.db.jooq.tables.pojos.User;

import static ru.yandex.market.global.checkout.domain.user.UserCommandService.PreferredUser.LOGGEDIN;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserCommandServiceTest extends BaseFunctionalTest {
    private final UserCommandService userCommandService;

    @Test
    public void testParallelCalls() {
        TestUtil.ParallelCallResults<User> results = TestUtil.doParallelCalls(1, List.of(
                () -> userCommandService.merge(1L, "123", LOGGEDIN),
                () -> userCommandService.getOrCreate(1L, "123")
        ));

        Assertions.assertThat(results.getErrors()).isEmpty();
        Assertions.assertThat(results.getResults().get(0).getId())
                .isEqualTo(results.getResults().get(1).getId());
    }
}

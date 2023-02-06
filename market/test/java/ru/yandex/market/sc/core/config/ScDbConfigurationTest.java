package ru.yandex.market.sc.core.config;

import javax.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.courier.repository.CourierRepository;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author valter
 */
@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ScDbConfigurationTest {
    private final CourierRepository courierRepository;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;
    private final JdbcTemplate longTimeoutJdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    @Test
    void hibernateAndJdbcHasSingleTransactionManager() {
        assertThatThrownBy(() -> transactionTemplate.execute(ts -> {
            courierRepository.save(new Courier(1L, "c1", null, null, null, null,null));
            entityManager.flush();
            jdbcTemplate.update("INSERT INTO courier (id, created_at, updated_at, name) " +
                            "VALUES (?, NOW(), NOW(), ?) ", 2, "c2");
            longTimeoutJdbcTemplate.update("INSERT INTO courier (id, created_at, updated_at, name) " +
                            "VALUES (?, NOW(), NOW(), ?) ", 2, "c2");
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(id) FROM courier", Long.class)).isEqualTo(3L);
            assertThat(longTimeoutJdbcTemplate.queryForObject("SELECT COUNT(id) FROM courier", Long.class))
                    .isEqualTo(3L);
            assertThat(courierRepository.findAll()).hasSize(3);
            throw new RuntimeException();
        })).isInstanceOf(RuntimeException.class);

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(id) FROM courier", Long.class)).isEqualTo(0L);
        assertThat(longTimeoutJdbcTemplate.queryForObject("SELECT COUNT(id) FROM courier", Long.class)).isEqualTo(0L);
        assertThat(courierRepository.findAll()).isEmpty();
    }

}

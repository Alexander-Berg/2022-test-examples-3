package ru.yandex.market.mbo.cardrender.app.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.mbo.cardrender.app.BaseTest;
import ru.yandex.market.mbo.cardrender.app.model.saas.DeleteModelSaasRow;

import static ru.yandex.market.mbo.cardrender.app.model.saas.DeleteModelSaasRow.fullDelete;

/**
 * @author apluhin
 * @created 12/8/21
 */
public class DeleteModelLogRepositoryTest extends BaseTest {

    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private DataSource dataSource;

    private DeleteModelLogRepository deleteModelLogRepository;

    @Before
    public void setUp() throws Exception {
        deleteModelLogRepository = new DeleteModelLogRepository(
                new NamedParameterJdbcTemplate(dataSource),
                transactionTemplate
        );
    }

    @Test
    public void testRemoveByTtl() {
        deleteModelLogRepository.insertBatch(
                fullDelete(1L, LocalDateTime.now().minusDays(1)),
                fullDelete(2L, LocalDateTime.now().minusDays(2)),
                fullDelete(3L, LocalDateTime.now().minusDays(3).minusHours(1)),
                fullDelete(4L, LocalDateTime.now().minusDays(4)),
                fullDelete(5L, LocalDateTime.now().minusDays(5))
        );
        Assertions.assertThat(deleteModelLogRepository.findAll().size()).isEqualTo(5);
        List<DeleteModelSaasRow> oldRows = deleteModelLogRepository.findTaskOlderTtl(24 * 3);
        Assertions.assertThat(oldRows.stream().map(DeleteModelSaasRow::getModelId).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(3L, 4L, 5L);
    }

}

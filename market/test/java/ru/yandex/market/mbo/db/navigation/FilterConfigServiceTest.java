package ru.yandex.market.mbo.db.navigation;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.db.IdGenerator;
import ru.yandex.market.mbo.db.IdGeneratorStub;

@RunWith(MockitoJUnitRunner.class)
public class FilterConfigServiceTest {

    private static final int ELEMENT_NUM = 1500;

    private JdbcTemplate jdbcTemplateMock;
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private PlatformTransactionManager platformTransactionManagerMock;
    private TransactionTemplate transactionTemplateMock;
    private IdGenerator idGenerator;
    private FilterConfigService filterConfigService;

    @Before
    public void setUp() {
        jdbcTemplateMock = Mockito.mock(JdbcTemplate.class);
        namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplateMock);
        platformTransactionManagerMock = Mockito.mock(PlatformTransactionManager.class);
        transactionTemplateMock = new TransactionTemplate(platformTransactionManagerMock);
        idGenerator = new IdGeneratorStub();
        filterConfigService = new FilterConfigService(namedParameterJdbcTemplate, transactionTemplateMock, idGenerator);
    }

    @Test
    public void testDeletionMoreThan1000Nodes() {
        List<Long> nodesId = new ArrayList<>();
        for (long i = 0; i < ELEMENT_NUM; i++) {
            nodesId.add(i);
        }

        filterConfigService.deleteNodesFilterConfigs(nodesId);

        Mockito.verify(jdbcTemplateMock, Mockito.times(2)).update(Mockito.anyString());
    }
}

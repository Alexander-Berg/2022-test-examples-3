package ru.yandex.market.ir.uee.tms.service;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.ir.uee.jooq.generated.enums.TaskPriority;
import ru.yandex.market.ir.uee.jooq.generated.tables.daos.AccountDao;
import ru.yandex.market.ir.uee.jooq.generated.tables.daos.AccountUsageQuotaDao;
import ru.yandex.market.ir.uee.jooq.generated.tables.pojos.Account;
import ru.yandex.market.ir.uee.jooq.generated.tables.pojos.AccountUsageQuota;
import ru.yandex.market.ir.uee.jooq.generated.tables.records.UserRunRecord;
import ru.yandex.market.ir.uee.model.UserRunState;
import ru.yandex.market.ir.uee.model.UserRunType;
import ru.yandex.market.ir.uee.tms.pojos.PipelineContextPojo;
import ru.yandex.market.ir.uee.tms.pojos.PriorityLimit;
import ru.yandex.market.ir.uee.tms.pojos.UserRunPojo;
import ru.yandex.market.ir.uee.tms.repository.PipelineContextRepository;
import ru.yandex.market.ir.uee.tms.repository.UserRunRepository;
import ru.yandex.market.ir.uee.tms.utils.DefaultMapper;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class QuotaManagerImplTest {

    private static final PriorityLimit DEFAULT_PRIORITY_LIMIT =
            new PriorityLimit().setCountOfLine(100).setPriority(TaskPriority.NORMAL);

    @InjectMocks
    private QuotaManagerImpl quotaManager;
    @Mock
    private AccountUsageQuotaDao accountUsageQuotaDao;
    @Mock
    private AccountDao accountDao;
    @Mock
    private PipelineContextRepository pipelineContextRepository;
    @Mock
    private UserRunRepository userRunRepo;

    @Captor
    private ArgumentCaptor<TaskPriority> taskPriorityArgumentCaptor;
    @Captor
    private ArgumentCaptor<AccountUsageQuota> accountUsageQuotaArgumentCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Проверяет кейс, где используется дефолтная квота и количество строк для обогащения превышает оставшуюся квоту
     */
    @Test
    public void useQuotaForUserRun_defaultQuota_HugeEnrich() {
        final int accountId = 10;
        final int userRunId = 1;

        when(userRunRepo.getUserRunById(anyInt())).thenReturn(new UserRunPojoBuilder()
                .setAccountId(accountId)
                        .setType(UserRunType.UC)
                .build());
        when(accountDao.fetchOneById(eq(accountId))).thenReturn(new Account()
                .setPriorityLimit(DefaultMapper.toJSONB(Map.of(
                        QuotaManagerImpl.DEFAULT_TYPE, DEFAULT_PRIORITY_LIMIT
                ))));

        PipelineContextPojo.PipelineContextData pipelineContextPojoData =
                Mockito.mock(PipelineContextPojo.PipelineContextData.class);
        when(pipelineContextPojoData.getRowCount()).thenReturn(150);
        PipelineContextPojo pipelineContextPojo = new PipelineContextPojo();
        pipelineContextPojo.setData(pipelineContextPojoData);
        when(pipelineContextRepository.getByUserRunId(eq(userRunId))).thenReturn(pipelineContextPojo);

        quotaManager.useQuotaForUserRun(userRunId);

        verify(userRunRepo).updatePriority(anyInt(), taskPriorityArgumentCaptor.capture());
        TaskPriority taskPriority = taskPriorityArgumentCaptor.getValue();
        assertEquals(TaskPriority.LOW, taskPriority);
    }

    /**
     * Проверяет кейс, где используется квота на определенный тип обогащения и количество строк меньше оставшейся квоты
     */
    @Test
    public void useQuotaForUserRun_typeQuota_smallEnrich() {
        final int accountId = 10;
        final int userRunId = 1;
        final int ucQuotaCount = 150;
        final int useRowCount = 50;
        final UserRunType userRunType = UserRunType.UC;

        when(userRunRepo.getUserRunById(anyInt())).thenReturn(new UserRunPojoBuilder()
                .setAccountId(accountId)
                .setType(userRunType)
                .build());
        when(accountDao.fetchOneById(eq(accountId))).thenReturn(new Account()
                .setPriorityLimit(DefaultMapper.toJSONB(Map.of(
                        QuotaManagerImpl.DEFAULT_TYPE, DEFAULT_PRIORITY_LIMIT,
                        userRunType, new PriorityLimit().setPriority(TaskPriority.HIGH).setCountOfLine(ucQuotaCount)
                ))));

        PipelineContextPojo.PipelineContextData pipelineContextPojoData =
                Mockito.mock(PipelineContextPojo.PipelineContextData.class);
        when(pipelineContextPojoData.getRowCount()).thenReturn(useRowCount);
        PipelineContextPojo pipelineContextPojo = new PipelineContextPojo();
        pipelineContextPojo.setData(pipelineContextPojoData);
        when(pipelineContextRepository.getByUserRunId(eq(userRunId))).thenReturn(pipelineContextPojo);

        quotaManager.useQuotaForUserRun(userRunId);

        verify(userRunRepo).updatePriority(anyInt(), taskPriorityArgumentCaptor.capture());
        TaskPriority taskPriority = taskPriorityArgumentCaptor.getValue();
        assertEquals(TaskPriority.HIGH, taskPriority);

        verify(accountUsageQuotaDao).update(accountUsageQuotaArgumentCaptor.capture());
        final AccountUsageQuota accountUsageQuota = accountUsageQuotaArgumentCaptor.getValue();
        final HashMap<String, PriorityLimit> priorityLimits =
                DefaultMapper.fromJSONB(accountUsageQuota.getUsageQuota(), QuotaManagerImpl.usageQuotaType);
        final PriorityLimit defaultLimit = priorityLimits.get(QuotaManagerImpl.DEFAULT_TYPE);
        assertEquals(DEFAULT_PRIORITY_LIMIT.getCountOfLine(), defaultLimit.getCountOfLine());
        assertEquals(DEFAULT_PRIORITY_LIMIT.getPriority(), defaultLimit.getPriority());

        PriorityLimit priorityLimit = priorityLimits.get(userRunType.name());
        assertEquals(TaskPriority.HIGH, priorityLimit.getPriority());
        assertEquals(ucQuotaCount - useRowCount, priorityLimit.getCountOfLine());
    }

    private static class UserRunPojoBuilder {
        private final UserRunRecord userRunRecord = new UserRunRecord();

        UserRunPojoBuilder setAccountId(int accountId) {
            userRunRecord.setAccountId(accountId);
            return this;
        }

        UserRunPojoBuilder setType(UserRunType userRunType) {
            userRunRecord.setType(userRunType.name());
            return this;
        }

        UserRunPojo build() {
            userRunRecord.setState(UserRunState.RUNNING.name())
                    .setId(1);
            return new UserRunPojo(userRunRecord);
        }
    }


}

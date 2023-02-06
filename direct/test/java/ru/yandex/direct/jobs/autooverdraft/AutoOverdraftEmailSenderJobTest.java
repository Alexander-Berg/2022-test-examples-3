package ru.yandex.direct.jobs.autooverdraft;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.core.entity.client.repository.ClientOptionsRepository;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.sender.YandexSenderClient;
import ru.yandex.direct.ytwrapper.client.YtClusterConfig;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.direct.ytwrapper.model.YtOperator;
import ru.yandex.qatools.allure.annotations.Description;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.common.db.PpcPropertyNames.CLIENTS_CAN_ENABLE_AUTO_OVERDRAFT_LAST_UPLOAD_TIME;

@ParametersAreNonnullByDefault
public class AutoOverdraftEmailSenderJobTest {
    private static final String DATE_UPLOAD_TABLE = "2020-01-01T01:01:00.1234";
    private static final String DATE_BEFORE = "2020-01-01T01:00:59.1234";
    private static final String DATE_AFTER = "2020-01-01T01:01:01.1234";
    private static final String DATE_EQUALS = DATE_UPLOAD_TABLE;
    private static final YtCluster DEFAULT_YT_CLUSTER = YtCluster.HAHN;

    private AutoOverdraftEmailSenderJob job;


    @Mock
    private YtOperator ytOperator;

    @Mock
    private YtClusterConfig ytClusterConfig;

    @Mock
    private ShardHelper shardHelper;

    @Mock
    private YandexSenderClient yandexSenderClient;

    @Mock
    private ClientOptionsRepository clientOptionsRepository;

    @Mock
    private RbacService rbacService;

    @Mock
    private UserService userService;

    @Mock
    private AutoOverdraftMailTemplateResolver autoOverdraftMailTemplateResolver;

    @Mock
    private YtProvider ytProvider;

    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Mock
    private PpcProperty<String> lastUploadTimeProp = mock(PpcProperty.class);


    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);
        when(ytProvider.getOperator(DEFAULT_YT_CLUSTER)).thenReturn(ytOperator);
        when(ytProvider.getClusterConfig(DEFAULT_YT_CLUSTER)).thenReturn(ytClusterConfig);

        when(ytOperator.exists(any())).thenReturn(true);
        when(ytOperator.readTableUploadTime(any())).thenReturn(DATE_UPLOAD_TABLE);

        when(ppcPropertiesSupport.get(CLIENTS_CAN_ENABLE_AUTO_OVERDRAFT_LAST_UPLOAD_TIME)).thenReturn(lastUploadTimeProp);

        job = new AutoOverdraftEmailSenderJob(shardHelper, yandexSenderClient, clientOptionsRepository, rbacService,
                userService, autoOverdraftMailTemplateResolver, ytProvider, DEFAULT_YT_CLUSTER, ppcPropertiesSupport);
    }

    @Test
    @Description("Выполняем при отсутствии значения времени последней обработки таблицы")
    void testRunOnFirstTime() {
        when(lastUploadTimeProp.get()).thenReturn(null);
        assertFalse(job.isDataProcessed(DATE_UPLOAD_TABLE), "Считаем что нужно обновить данные");
    }

    @Test
    @Description("Выполняем для новой таблицы, убеждаемся, что она будет использована")
    void testRunOnNewTable() {
        when(lastUploadTimeProp.get()).thenReturn(DATE_BEFORE);
        assertFalse(job.isDataProcessed(DATE_UPLOAD_TABLE), "Считаем что нужно обновить данные");
    }

    @Test
    @Description("Выполняем для старой таблицы, убеждаемся, что она не будет использована")
    void testRunOnOldTable() {
        when(lastUploadTimeProp.get()).thenReturn(DATE_AFTER);
        assertTrue(job.isDataProcessed(DATE_UPLOAD_TABLE), "Считаем что не нужно обновить данные");
    }

    @Test
    @Description("Выполняем для уже обработанной таблицы, убеждаемся, что она не будет использована")
    void testRunOnCurrentTable() {
        when(lastUploadTimeProp.get()).thenReturn(DATE_EQUALS);
        assertTrue(job.isDataProcessed(DATE_UPLOAD_TABLE), "Считаем что не нужно обновить данные");
    }
}

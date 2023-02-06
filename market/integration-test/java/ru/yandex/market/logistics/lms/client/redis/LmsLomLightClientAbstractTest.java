package ru.yandex.market.logistics.lms.client.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.market.logistics.lom.configuration.properties.LmsYtProperties;
import ru.yandex.market.logistics.lom.lms.client.LmsLomLightClient;
import ru.yandex.market.logistics.lom.repository.InternalVariableRepository;
import ru.yandex.market.logistics.lom.service.redis.AbstractRedisTest;
import ru.yandex.market.logistics.lom.utils.YtLmsVersionsUtils;
import ru.yandex.market.logistics.management.client.LMSClient;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class LmsLomLightClientAbstractTest extends AbstractRedisTest {
    protected static final String REDIS_ACTUAL_VERSION = "0";
    protected static final String YT_ACTUAL_VERSION = "2022-03-02T08:05:24Z";

    @Autowired
    protected LmsYtProperties lmsYtProperties;

    @Autowired
    protected LmsLomLightClient lmsLomLightClient;

    @Autowired
    protected LMSClient lmsClient;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected Yt hahnYt;

    @Autowired
    protected YtTables ytTables;

    @Autowired
    protected InternalVariableRepository internalVariableRepository;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        doReturn(ytTables)
            .when(hahnYt).tables();

        YtLmsVersionsUtils.mockYtVersionTable(ytTables, lmsYtProperties, YT_ACTUAL_VERSION);
    }

    @AfterEach
    @Override
    public void tearDown() {
        super.tearDown();
        verifyNoMoreInteractions(lmsClient, ytTables);
    }
}

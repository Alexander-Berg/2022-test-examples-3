package ru.yandex.market.logistics.lms.client.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.lms.client.LmsFallbackClient;
import ru.yandex.market.logistics.lom.lms.client.LmsLomLightCommonClient;
import ru.yandex.market.logistics.lom.lms.client.LmsLomRedisClient;
import ru.yandex.market.logistics.lom.lms.client.LmsLomYtClient;
import ru.yandex.market.logistics.lom.lms.model.logging.enums.LmsLomLoggingCode;
import ru.yandex.market.logistics.lom.lms.service.log.LmsLomMethodsLoggingInfoServiceImpl;
import ru.yandex.market.logistics.lom.repository.InternalVariableRepository;
import ru.yandex.market.logistics.lom.service.internalVariable.InternalVariableServiceImpl;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public abstract class LmsLomLightCommonClientMockedAbstractTest extends AbstractContextualTest {

    @Autowired
    private InternalVariableRepository internalVariableRepository;

    protected LmsLomRedisClient mockedRedisClient = mock(LmsLomRedisClient.class);
    protected LmsLomYtClient mockedYtClient = mock(LmsLomYtClient.class);
    protected LmsFallbackClient mockedLmsFallbackClient = mock(LmsFallbackClient.class);

    protected LmsLomLightCommonClient mockedLmsLomLightCommonClient = new LmsLomLightCommonClient(
        mockedRedisClient,
        mockedYtClient,
        mockedLmsFallbackClient,
        new InternalVariableServiceImpl(internalVariableRepository),
        new LmsLomMethodsLoggingInfoServiceImpl()
    );

    @BeforeEach
    void setUpLoggingCodes() {
        doReturn(LmsLomLoggingCode.LMS_LOM_REDIS)
            .when(mockedRedisClient).getLoggingCode();

        doReturn(LmsLomLoggingCode.LMS_LOM_YT)
            .when(mockedYtClient).getLoggingCode();
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(mockedRedisClient, mockedYtClient, mockedLmsFallbackClient);
    }
}

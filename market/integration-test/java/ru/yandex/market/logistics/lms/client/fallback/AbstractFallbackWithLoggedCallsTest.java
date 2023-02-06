package ru.yandex.market.logistics.lms.client.fallback;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lms.client.models.FallbackClientLogTestInfo;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.repository.InternalVariableRepository;

@ParametersAreNonnullByDefault
@DisplayName("Логирование вызовов методов клиентов для данных LMS")
public abstract class AbstractFallbackWithLoggedCallsTest extends AbstractContextualTest {

    protected List<FallbackClientLogTestInfo> fallbackTests;

    @Autowired
    protected InternalVariableRepository internalVariableRepository;

    @BeforeEach
    void setUp() {
        fallbackTests = List.of(
            getLogisticsPointTestInfo(),
            getPartnerTestInfo(),
            getPartnersTestInfo(),
            getLogisticsPointsTestInfo(),
            getPartnerExternalParamsTestInfo(),
            searchPartnerRelationWithCutoffsTestInfo(),
            searchPartnerRelationsWithReturnPartnersTestInfo(),
            searchInboundScheduleTestInfo(),
            searchPartnerApiSettingsMethodsTestInfo(),
            getScheduleDayTestInfo()
        );
    }

    @Test
    @DisplayName("Вызов клиента пишется в лог")
    void fallbackCallingWithLog() {
        setWriteInClientLog(true);

        for (FallbackClientLogTestInfo testInfo : fallbackTests) {
            testInfo.setLoggingEnabled(true);
            callClientAndVerifyAndCheckLog(testInfo);
        }
    }

    @Test
    @DisplayName("Вызов клиента не пишется в лог")
    void fallbackCallingWithoutLog() {
        setWriteInClientLog(false);

        for (FallbackClientLogTestInfo testInfo : fallbackTests) {
            testInfo.setLoggingEnabled(false);
            callClientAndVerifyAndCheckLog(testInfo);
        }
    }

    abstract void setWriteInClientLog(boolean loggingEnabled);

    abstract void callClientAndVerifyAndCheckLog(FallbackClientLogTestInfo testInfo);

    @Nonnull
    abstract FallbackClientLogTestInfo getLogisticsPointTestInfo();

    @Nonnull
    abstract FallbackClientLogTestInfo getPartnerTestInfo();

    @Nonnull
    abstract FallbackClientLogTestInfo getPartnersTestInfo();

    @Nonnull
    abstract FallbackClientLogTestInfo getLogisticsPointsTestInfo();

    @Nonnull
    abstract FallbackClientLogTestInfo getPartnerExternalParamsTestInfo();

    @Nonnull
    abstract FallbackClientLogTestInfo searchPartnerRelationWithCutoffsTestInfo();

    @Nonnull
    abstract FallbackClientLogTestInfo searchPartnerRelationsWithReturnPartnersTestInfo();

    @Nonnull
    abstract FallbackClientLogTestInfo searchInboundScheduleTestInfo();

    @Nonnull
    abstract FallbackClientLogTestInfo searchPartnerApiSettingsMethodsTestInfo();

    @Nonnull
    abstract FallbackClientLogTestInfo getScheduleDayTestInfo();
}

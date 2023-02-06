package ru.yandex.market.deepmind.common.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;

import ru.yandex.market.deepmind.common.services.audit.MskuStatusAuditRecorder;
import ru.yandex.market.deepmind.common.services.audit.MskuStatusAuditService;
import ru.yandex.market.mboc.common.audit.SskuStatusAuditService;

@Profile("test")
@TestConfiguration
public class TestAuditConfig extends AuditConfig {
    public TestAuditConfig() {
        super(null, null);
    }

    @Override
    public SskuStatusAuditService offerStatusAuditService() {
        return Mockito.mock(SskuStatusAuditService.class);
    }

    @Override
    public MskuStatusAuditService mskuStatusAuditService() {
        return Mockito.mock(MskuStatusAuditService.class);
    }

    @Override
    public MskuStatusAuditRecorder mskuStatusAuditRecorder() {
        return Mockito.mock(MskuStatusAuditRecorder.class);
    }
}

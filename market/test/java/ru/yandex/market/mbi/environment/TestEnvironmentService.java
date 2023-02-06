package ru.yandex.market.mbi.environment;

import java.util.List;

import org.springframework.context.event.EventListener;

import ru.yandex.common.util.application.EnvironmentType;
import ru.yandex.market.common.test.spring.event.AfterTestMethodEvent;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class TestEnvironmentService extends MultiEnvironmentService {

    private EnvironmentType environmentType = detectEnvironmentType();

    public TestEnvironmentService(List<EnvironmentService> environmentServiceList) {
        super(environmentServiceList);
    }

    @Override
    public EnvironmentType getCurrentEnvironmentType() {
        return environmentType;
    }

    public void setEnvironmentType(EnvironmentType environmentType) {
        this.environmentType = environmentType;
    }

    @EventListener(AfterTestMethodEvent.class)
    public void reset() {
        environmentType = detectEnvironmentType();
    }
}

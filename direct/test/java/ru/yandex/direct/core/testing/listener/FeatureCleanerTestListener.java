package ru.yandex.direct.core.testing.listener;

import org.apache.commons.lang3.StringUtils;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import ru.yandex.direct.core.entity.feature.service.DirectAuthContextService;

/**
 * В конце выполнения тестового класса очищает из контекста {@link DirectAuthContextService} текущего клиента и
 * оператора
 */
public class FeatureCleanerTestListener extends AbstractTestExecutionListener {
    @Override
    public void afterTestClass(TestContext testContext) throws Exception {
        var context = testContext.getApplicationContext();
        var beanName = StringUtils.uncapitalize(DirectAuthContextService.class.getSimpleName());
        if (context.containsBeanDefinition(beanName)) {
            testContext.getApplicationContext().getBean(DirectAuthContextService.class).clearThreadLocal();
        }
    }
}

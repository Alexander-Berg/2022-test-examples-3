package ru.yandex.chemodan.app.psbilling.web;

import org.springframework.test.context.ContextConfiguration;

import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;

@ContextConfiguration(classes = {
        PsBillingWebTestConfig.class,
})
public abstract class BaseWebTest extends AbstractPsBillingCoreTest {
}

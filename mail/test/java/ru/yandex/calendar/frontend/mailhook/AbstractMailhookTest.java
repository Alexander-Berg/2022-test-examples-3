package ru.yandex.calendar.frontend.mailhook;

import org.springframework.test.context.ContextConfiguration;

import ru.yandex.calendar.test.generic.AbstractConfTest;

@ContextConfiguration(classes = MailhookContextTestConfiguration.class)
public abstract class AbstractMailhookTest extends AbstractConfTest {
}

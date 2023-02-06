package ru.yandex.calendar.frontend.caldav.proto.facade;

import org.springframework.test.context.ContextConfiguration;
import ru.yandex.calendar.frontend.caldav.CaldavContextTestConfiguration;
import ru.yandex.calendar.test.generic.AbstractConfTest;

@ContextConfiguration(classes = CaldavContextTestConfiguration.class)
public abstract class AbstractCaldavTest extends AbstractConfTest {
}

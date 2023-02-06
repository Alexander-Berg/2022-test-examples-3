package ru.yandex.market.checkout.checkouter.events;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

/**
 * @author mmetlov
 */

public abstract class AbstractEventsControllerTestBase extends AbstractWebTestBase {

    @Autowired
    protected TestSerializationService serializationService;
}

package ru.yandex.market.pricelabs.tms.processing.events;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.pricelabs.model.types.AutostrategyTarget;
import ru.yandex.market.pricelabs.processing.autostrategies.AutostrategiesMetaProcessor;
import ru.yandex.market.pricelabs.tms.processing.autostrategies.events.EventsGenerator;

@Disabled
@Slf4j
public class ShopWithNoAutostrategiesEventsTestsWhite extends AbstractShopWithNoAutostrategiesEventsTests {
    @Qualifier("shopsWithTurnedOffAutostrategiesEventsGeneratorWhite")
    @Autowired
    private EventsGenerator noActiveEventsGeneratorWhite;

    @Qualifier("shopsWithDeletedAutostrategiesEventsGeneratorWhite")
    @Autowired
    private EventsGenerator noCurrentEventsGeneratorWhite;

    @Qualifier("autostrategiesMetaWhite")
    @Autowired
    private AutostrategiesMetaProcessor metaProcessorWhite;

    @BeforeEach
    void beforeEach() {
        init(
                noActiveEventsGeneratorWhite,
                noCurrentEventsGeneratorWhite,
                metaProcessorWhite,
                AutostrategyTarget.white);
    }
}

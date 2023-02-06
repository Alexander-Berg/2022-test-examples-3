package ru.yandex.market.pricelabs.tms.processing.events;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.pricelabs.model.types.AutostrategyTarget;
import ru.yandex.market.pricelabs.processing.autostrategies.AutostrategiesMetaProcessor;
import ru.yandex.market.pricelabs.tms.processing.autostrategies.events.EventsGenerator;

@Slf4j
public class ShopWithNoAutostrategiesEventsTestsBlue extends AbstractShopWithNoAutostrategiesEventsTests {
    @Qualifier("shopsWithTurnedOffAutostrategiesEventsGeneratorBlue")
    @Autowired
    private EventsGenerator noActiveEventsGeneratorBlue;

    @Qualifier("shopsWithDeletedAutostrategiesEventsGeneratorBlue")
    @Autowired
    private EventsGenerator noCurrentEventsGeneratorBlue;

    @Qualifier("autostrategiesMetaBlue")
    @Autowired
    private AutostrategiesMetaProcessor metaProcessorBlue;

    @BeforeEach
    void beforeEach() {
        init(
                noActiveEventsGeneratorBlue,
                noCurrentEventsGeneratorBlue,
                metaProcessorBlue,
                AutostrategyTarget.blue);
    }
}

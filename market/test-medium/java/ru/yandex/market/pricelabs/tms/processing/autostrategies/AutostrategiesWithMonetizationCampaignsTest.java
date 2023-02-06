package ru.yandex.market.pricelabs.tms.processing.autostrategies;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.pricelabs.model.types.AutostrategyTarget;
import ru.yandex.market.pricelabs.processing.autostrategies.AutostrategiesMetaProcessor;
import ru.yandex.market.pricelabs.services.database.SequenceService;
import ru.yandex.market.pricelabs.services.database.SequenceServiceImpl;
import ru.yandex.market.pricelabs.tms.AbstractTmsSpringConfiguration;

public class AutostrategiesWithMonetizationCampaignsTest extends AbstractTmsSpringConfiguration {

    @Autowired
    @Qualifier("autostrategiesMetaWhite")
    private AutostrategiesMetaProcessor metaWhite;
    @Autowired
    @Qualifier("autostrategiesMetaBlue")
    private AutostrategiesMetaProcessor metaBlue;
    @Autowired
    @Qualifier("autostrategiesStateWhite")
    private AutostrategiesStateProcessor stateWhite;
    @Autowired
    @Qualifier("autostrategiesStateBlue")
    private AutostrategiesStateProcessor stateBlue;
    @Autowired
    private SequenceService sequenceService;


    static final int UID = 1000;
    static final int SHOP1 = 111;
    static final int SHOP2 = 222;
    private AutostrategyTarget target;
    private AutostrategiesMetaProcessor autostrategyService;

    @BeforeEach
    void init() {
        this.target = AutostrategyTarget.white;

        this.autostrategyService = target.get(metaWhite, metaBlue);
        var stateProcessor = target.get(stateWhite, stateBlue);

        AbstractAutostrategiesMetaProcessorTest.cleanupTables(autostrategyService, stateProcessor, testControls);
        ((SequenceServiceImpl) sequenceService).resetSequences();
    }

    @Test
    public void testListAllAutostrategiesIncludingMonetizationCampaigns() {

    }
}

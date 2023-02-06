package ru.yandex.market.pipelinetests.tests.yandex_go;

import step.CapacityStorageSteps;
import step.CombinatorSteps;
import step.L4GSteps;
import step.LgwSteps;
import step.LmsSteps;
import step.LogPlatformTaxiSteps;
import step.LomOrderSteps;
import step.TplPvzSteps;
import step.TrackerSteps;

import ru.yandex.market.pipelinetests.tests.AbstractTest;

public abstract class AbstractYandexGoTest extends AbstractTest {

    protected static final LogPlatformTaxiSteps LOG_PLATFORM_STEPS = new LogPlatformTaxiSteps();
    protected static final LomOrderSteps LOM_ORDER_STEPS = new LomOrderSteps();
    protected static final L4GSteps L4G_STEPS = new L4GSteps();
    protected static final TrackerSteps TRACKER_STEPS = new TrackerSteps();
    protected static final TplPvzSteps TPL_PVZ_STEPS = new TplPvzSteps();
    protected static final LgwSteps LGW_STEPS = new LgwSteps();
    protected static final CapacityStorageSteps CAPACITY_STORAGE_STEPS = new CapacityStorageSteps();
    protected static final LmsSteps LMS_STEPS = new LmsSteps();
    protected static final CombinatorSteps COMBINATOR_STEPS = new CombinatorSteps();
}

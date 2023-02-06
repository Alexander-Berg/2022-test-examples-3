package ru.yandex.market.mbo.db.modelstorage.partnergeneralization;

import org.junit.Before;
import ru.yandex.market.mbo.db.modelstorage.health.OperationStats;
import ru.yandex.market.mbo.user.TestAutoUser;

/**
 * @author s-ermakov
 */
public class BasePartnerGeneralizationTest {

    protected static final OperationStats STATS = new OperationStats();

    protected PartnerGeneralizationServiceImpl generalizationService;

    @Before
    public void setUp() throws Exception {
        generalizationService = new PartnerGeneralizationServiceImpl(TestAutoUser.create());
    }
}

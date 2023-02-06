package ru.yandex.market.psku.postprocessor.service.yt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.config.ManualTestConfig;
import ru.yandex.market.psku.postprocessor.service.yt.session.SessionParam;
import ru.yandex.market.psku.postprocessor.service.yt.session.YtPskuSessionService;

/**
 * @author dergachevfv
 * @since 6/3/20
 */
@Ignore("для ручной проверки yql запросов")
@ContextConfiguration(classes = ManualTestConfig.class)
public class YtDataServiceManualTest extends BaseDBTest {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private YtDataService ytDataService;

    @Autowired
    private YtPskuSessionService ytPskuSessionService;

    @Autowired
    @Qualifier("hahnYtApi")
    private Yt yt;

    @Test
    public void testLoadPmodelsForDeletion() {
        YPath sessionPath = YPath.simple("//home/market/testing/ir/psku-post-processor/pmodel-deleter-sessions/test");
        yt.cypress().create(sessionPath, CypressNodeType.MAP, true, true);
        ytDataService.loadPmodelsForDeletion("test");
    }

    @Test
    public void testLoadPsku20(){
        SessionParam sessionParam = ytPskuSessionService.startNewPskuSession();
        ytDataService.loadPskus(sessionParam.getName());
    }

    @Test
    public void testProcessPmodelsDataForDeletionSession() {
        ytDataService.processPmodelsDataForDeletion("test",
                modelWithSkuMappings -> log.info(modelWithSkuMappings.toString())
        );
    }
}

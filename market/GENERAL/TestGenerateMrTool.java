package ru.yandex.mbo.tool.jira.MBO27157;

import javax.annotation.Resource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.mbo.yt.ReplicatedTableTimestampService;
import ru.yandex.mbo.tool.Tool;

@Component
public class TestGenerateMrTool implements Tool {
    private static final Logger log = LogManager.getLogger();
    private static final int RETRIES = 10;

    @Value("${mbo.modelstorage.table.models}")
    private String mboModelTablePath;
    @Resource
    private ReplicatedTableTimestampService replicatedTableTimestampService;


    @Override
    @SuppressWarnings("checkstyle:magicnumber")
    public void start(String[] args) throws Exception {
        log.info(YPath.simple(mboModelTablePath));
        log.info(YPath.simple(mboModelTablePath).toString());
        log.info(YPath.simple(mboModelTablePath).name());
        for (int i = 0; i < RETRIES; i++) {
            log.info("Here is ts:" + replicatedTableTimestampService.getPathWithTimestamp(
                GUID.valueOf("c79a-a495e-40502c5-e4e73178"), YPath.simple(mboModelTablePath)));
            Thread.sleep(5000);
        }
    }
}

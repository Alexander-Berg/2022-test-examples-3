package ru.yandex.market.ir.tms.logs.saasoffers;

import com.google.common.collect.ImmutableSet;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.tms.quartz2.model.VerboseExecutor;
import ru.yandex.market.ir.tms.dao.MboLogsController;
import ru.yandex.market.mbo.gwt.models.dashboard.SuperControllerSession;

/**
 * Джоба создана исключительно для проверки работспособности скриптов в деве/тестинге.
 * Не стоит её запускать с существующими сессиями.
 *
 * @author prediger
 * @created 23.04.18
 */
@Component
@Profile("yt")
public class TestMboLogsControllerJob extends VerboseExecutor {
    @Autowired
    private MboLogsController mboLogsController;
    @Value("${indexer.type}")
    private String indexerType;

    @Override
    public void doRealJob(JobExecutionContext context) {
        mboLogsController.runMbologs(SuperControllerSession.Database.YT, indexerType,
            "not_exsiting_dashboard", YPath.simple("//home/not_existing_table"), "no_no_no",
            "session", ImmutableSet.of("session_1", "session_2"), GUID.valueOf("a-b-c-d"), false);
    }
}

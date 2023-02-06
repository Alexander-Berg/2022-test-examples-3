package ru.yandex.market.ir.tms.logs.saasoffers;

import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ru.yandex.market.tms.quartz2.model.VerboseExecutor;

/**
 * Джоба создана исключительно для проверки работспособности скриптов в деве/тестинге.
 * Не стоит её запускать с существующими сессиями.
 *
 * @author prediger
 * @created 23.04.18
 */
@Component
@Profile("yt")
public class TestStandaloneIndexerControllerJob extends VerboseExecutor {
    @Autowired
    private StandaloneIndexerController standaloneIndexerController;

    @Override
    public void doRealJob(JobExecutionContext context) {
        standaloneIndexerController.buildIndex("this_is_not_a_stat_table",
            "this_is_not_an_index_table", System.currentTimeMillis(), null);
    }
}

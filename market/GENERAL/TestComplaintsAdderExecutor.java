package ru.yandex.market.mbo.tms.tt;

import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Required;
import ru.yandex.market.mbo.modelbug.BugMetaInfo;
import ru.yandex.market.mbo.modelbug.ModelBugSource;
import ru.yandex.market.mbo.modelbug.ReportModelBugDao;
import ru.yandex.market.tms.quartz2.model.VerboseExecutor;

import java.util.Set;

/**
 * @author Sergey Skrobotov, sskrobotov@yandex-team.ru
 */
public class TestComplaintsAdderExecutor extends VerboseExecutor {

    private static final Logger log = Logger.getLogger(TestComplaintsAdderExecutor.class);

    private ReportModelBugDao reportModelBugDao;

    private Set<Long> modelIds;


    @Required
    public void setReportModelBugDao(ReportModelBugDao reportModelBugDao) {
        this.reportModelBugDao = reportModelBugDao;
    }

    public void setModelIds(Set<Long> modelIds) {
        this.modelIds = modelIds;
    }

    @Override
    public void doRealJob(JobExecutionContext context) {
        for (long modelId: modelIds) {
            log.debug("adding test complaint for the model #" + modelId);
            try {
                reportModelBugDao.reportModelBug(modelId,
                    BugMetaInfo.metaInfo("mbo-adv-test", "test complaint", "mbo-adv-test@yandex.ru"),
                    null, ModelBugSource.MODEL_ERROR_FROM_USER);
            } catch (Exception e) {
                log.debug("adding test complaint for the model #" + modelId);
            }
        }
    }
}

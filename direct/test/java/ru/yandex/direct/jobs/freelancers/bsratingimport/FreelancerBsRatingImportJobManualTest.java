package ru.yandex.direct.jobs.freelancers.bsratingimport;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.jobs.configuration.JobsConfiguration;
import ru.yandex.direct.ytwrapper.YtPathUtil;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.direct.ytwrapper.model.YtTable;


/**
 * Импорт рейтингов специалистов из указанной таблицы на YT в БД Директа.
 */
@ExtendWith(SpringExtension.class)
@Disabled("For manual run")
@ContextConfiguration(classes = JobsConfiguration.class)
class FreelancerBsRatingImportJobManualTest {

    @Autowired
    FreelancerBsRatingImportJob job;

    /**
     * Вместо production БД для отладки можно использовать копию с фрилансерами с devtest.
     * Методика копирования данных описана в {@code yql_copy_prod_rating_to_dev.md}
     */
    @Test
    void execute() {
        YtCluster ytCluster = YtCluster.HAHN;
        YtTable table = new YtTable(YtPathUtil.generatePath("/home/bs/freelancers/direct/export", "2018-11-13"));

        job.importFreelancerRatingToDirect(ytCluster, table);
    }
}

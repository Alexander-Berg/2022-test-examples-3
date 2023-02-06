package ru.yandex.market.pharmatestshop.executor;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.market.pharmatestshop.domain.pharmacy.PharmacyService;
import ru.yandex.market.tms.quartz2.model.VerboseExecutor;
import ru.yandex.market.tms.quartz2.spring.CronTrigger;


@Slf4j
@Component
@CronTrigger(cronExpression = "0 0 * * * ? *",
        description = "Обновляет статусы")
public class UpdatePharmaShopSettings extends VerboseExecutor {

    @Autowired
    private PharmacyService pharmacyService;

    @Override
    public void doRealJob(JobExecutionContext context) throws Exception {
        log.info("Start updating shop settings..");

        pharmacyService.updatePharmacySettingsDate();

        log.info("Shop settings updated successfully!");
    }
}


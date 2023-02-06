package ru.yandex.market.hrms.tms.executor.jobstate;

import lombok.RequiredArgsConstructor;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import ru.yandex.market.hrms.core.domain.domain.repo.DomainRepo;
import ru.yandex.market.hrms.tms.HrmsExecutor;

@RequiredArgsConstructor
@Component
public class JobStateTestExecutor extends HrmsExecutor {

    private final DomainRepo domainRepo;

    @Override
    public void executeJob(JobExecutionContext context) {
        domainRepo.deleteById(1L);
    }
}

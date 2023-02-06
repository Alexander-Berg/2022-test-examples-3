package ru.yandex.market.hrms.core.service.timex;

import java.time.Clock;

import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeRepo;
import ru.yandex.market.hrms.core.domain.timex.repo.TimexHistoryRepo;
import ru.yandex.market.hrms.core.domain.timex.repo.TimexOperationAreaRepo;
import ru.yandex.market.hrms.core.service.environment.EnvironmentService;

public class TimexServiceMock extends TimexService {
    public TimexServiceMock(TimexMapper timexMapper,
                            TimexApiFacade timexApiFacade,
                            TimexHistoryRepo timexHistoryRepo,
                            TimexOperationAreaRepo timexOperationAreaRepo,
                            EmployeeRepo employeeRepo,
                            EnvironmentService environmentService,
                            Clock clock
    ) {
        super(timexMapper, timexHistoryRepo, timexOperationAreaRepo,
                employeeRepo, timexApiFacade, environmentService, clock);
    }
}

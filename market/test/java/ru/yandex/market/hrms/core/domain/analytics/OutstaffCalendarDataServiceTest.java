package ru.yandex.market.hrms.core.domain.analytics;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.analytics.repo.OutstaffCalendarDataRepo;
import ru.yandex.market.hrms.core.domain.domain.repo.DomainRepo;
import ru.yandex.market.hrms.core.domain.employee.PageRequest;
import ru.yandex.market.hrms.core.service.outstaff.OutstaffCalendarPageLoaderService;
import ru.yandex.market.hrms.core.domain.outstaff.OutstaffCalendarRequest;
import ru.yandex.market.hrms.core.domain.outstaff.repo.OutstaffEntityRepo;
import ru.yandex.market.hrms.core.security.InternalRoles;
import ru.yandex.market.hrms.model.calendar.CalendarPage;
import ru.yandex.market.hrms.model.view.outstaff.OutstaffActivityFilter;
import ru.yandex.market.tpl.common.util.datetime.LocalDateInterval;

public class OutstaffCalendarDataServiceTest extends AbstractCoreTest {

    @Mock
    private OutstaffCalendarPageLoaderService outstaffCalendarPageLoaderService;
    @Autowired
    private DomainRepo domainRepo;
    @Autowired
    private OutstaffEntityRepo outstaffEntityRepo;
    @Autowired
    private OutstaffCalendarDataRepo outstaffCalendarDataRepo;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setAuthentication() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "system",
                "system",
                List.of(new GrantedAuthority() {
                    @Override
                    public String getAuthority() {
                        return InternalRoles.SYSTEM_ROLE;
                    }
                })
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DbUnitDataSet(before = "OutstaffCalendarDataServiceTest.CollectData.before.csv",
            after = "OutstaffCalendarDataServiceTest.CollectData.after.csv")
    void collectData() throws IOException {
        LocalDate today = LocalDate.of(2021, 10, 4);
        mockClock(today);
        YearMonth month = YearMonth.of(today.getYear(), today.getMonthValue());

        Mockito
                .when(outstaffCalendarPageLoaderService.getOutstaffCalendarPage(
                        OutstaffCalendarRequest.builder()
                                .domainId(1)
                                .interval(new LocalDateInterval(month.atDay(1), month.atEndOfMonth()))
                                .showBlocked(true)
                                .activityFilter(OutstaffActivityFilter.SHOW_ONLY_ACTIVE)
                                .build(),
                        new PageRequest(0, 100, null),
                        false
                ))
                .thenReturn(objectMapper.readValue(
                        getClass().getResourceAsStream("/results/outstaff_calendar_page.json"),
                        CalendarPage.class
                ));

        OutstaffCalendarDataService outstaffCalendarDataService = new OutstaffCalendarDataService(
                outstaffCalendarPageLoaderService,
                domainRepo,
                outstaffEntityRepo,
                outstaffCalendarDataRepo
        );
        outstaffCalendarDataService.sync(month);
    }
}

package ru.yandex.market.mbo.reactui.controller;

import java.time.LocalDate;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import ru.yandex.common.gwt.security.AccessControlManager;
import ru.yandex.common.gwt.shared.User;
import ru.yandex.market.mbo.billing.report.personal.PersonalBilling;
import ru.yandex.market.mbo.billing.report.personal.PersonalBillingFilter;
import ru.yandex.market.mbo.billing.report.personal.PersonalBillingReportService;
import ru.yandex.market.mbo.reactui.dto.ListDto;
import ru.yandex.market.mbo.reactui.dto.ResponseDto;
import ru.yandex.market.mbo.user.UserManager;

import static java.time.LocalDate.now;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.billing.report.personal.PersonalBillingFilter.DEFAULT_LIMIT;

@SuppressWarnings("checkstyle:magicNumber")
public class PersonalBillingControllerTest {

    @InjectMocks
    private PersonalBillingController controller;
    @Mock
    private PersonalBillingReportService reportService;
    @Mock
    private AccessControlManager accessControlManager;
    @Mock
    private UserManager userManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(reportService.countByFilter(any())).thenReturn(3);
        when(userManager.isAdmin(anyLong())).thenReturn(false);
    }

    @Test
    public void personalBilling() {
        when(accessControlManager.getCachedUser()).thenReturn(new User("Test", 12345L, null, 12L));

        ResponseDto<ListDto<PersonalBilling>> responseDto =
            controller.personalBilling(new PersonalBillingFilter().setOperatorId(12345L));

        ListDto<PersonalBilling> data = responseDto.getData();
        assertEquals(3, data.getTotal());
        assertEquals(0, data.getOffset());
        assertEquals(DEFAULT_LIMIT, data.getLimit());

        ArgumentCaptor<PersonalBillingFilter> requestCaptor = ArgumentCaptor.forClass(PersonalBillingFilter.class);
        Mockito.verify(reportService, times(1)).findByFilters(requestCaptor.capture());
        PersonalBillingFilter value = requestCaptor.getValue();
        assertEquals(LocalDate.of(now().getYear(), now().getMonth(), 1), value.getFromDate());
        assertEquals(now(), value.getToDate());
    }

    @Test(expected = ResponseStatusException.class)
    public void personalBillingForbidden() {
        when(accessControlManager.getCachedUser()).thenReturn(new User("Test", 543L, null, 12L));
        controller.personalBilling(new PersonalBillingFilter().setOperatorId(12345L));
    }

    @Test
    public void personalBillingAdminEnable() {
        when(userManager.isAdmin(anyLong())).thenReturn(true);
        when(accessControlManager.getCachedUser()).thenReturn(new User("Test", 543L, null, 12L));
        controller.personalBilling(new PersonalBillingFilter().setOperatorId(12345L));
    }
}

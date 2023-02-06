package ru.yandex.direct.internaltools.tools.searchspamusers;

import java.time.LocalDate;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.internaltools.tools.searchspamusers.model.SearchSpamUsersLogTableEnum;
import ru.yandex.direct.internaltools.tools.searchspamusers.model.SearchSpamUsersParameters;
import ru.yandex.direct.internaltools.tools.searchspamusers.service.SearchSpamUsersValidationService;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;

public class SearchSpamUsersToolValidationTest {
    private SearchSpamUsersTool tool;

    @Before
    public void before() {
        tool = new SearchSpamUsersTool(null);
    }

    @Test
    public void testValidationCorrectAll() {
        SearchSpamUsersParameters parameters = new SearchSpamUsersParameters()
                .withLogins("login")
                .withIp("127.0.0.1")
                .withLogTable(SearchSpamUsersLogTableEnum.CMD)
                .withDateFrom(LocalDate.now())
                .withDateTo(LocalDate.now());
        ValidationResult<SearchSpamUsersParameters, Defect> result = tool.validate(parameters);
        assertThat(result.hasAnyErrors())
                .isFalse();
    }

    @Test
    public void testValidationCorrectNoIp() {
        SearchSpamUsersParameters parameters = new SearchSpamUsersParameters()
                .withLogins("login")
                .withLogTable(SearchSpamUsersLogTableEnum.CMD)
                .withDateFrom(LocalDate.now())
                .withDateTo(LocalDate.now());
        ValidationResult<SearchSpamUsersParameters, Defect> result = tool.validate(parameters);
        assertThat(result.hasAnyErrors())
                .isFalse();
    }


    @Test
    public void testValidationCorrectNoLogins() {
        SearchSpamUsersParameters parameters = new SearchSpamUsersParameters()
                .withIp("127.0.0.1")
                .withLogTable(SearchSpamUsersLogTableEnum.CMD)
                .withDateFrom(LocalDate.now())
                .withDateTo(LocalDate.now());
        ValidationResult<SearchSpamUsersParameters, Defect> result = tool.validate(parameters);
        assertThat(result.hasAnyErrors())
                .isFalse();
    }

    @Test
    public void testValidationInvalidNoLoginsAndIp() {
        SearchSpamUsersParameters parameters = new SearchSpamUsersParameters()
                .withLogTable(SearchSpamUsersLogTableEnum.CMD)
                .withDateFrom(LocalDate.now())
                .withDateTo(LocalDate.now());
        ValidationResult<SearchSpamUsersParameters, Defect> result = tool.validate(parameters);
        assertThat(result.hasAnyErrors())
                .isTrue();
    }

    @Test
    public void testValidationCorrectDateRangeLessThanMax() {
        SearchSpamUsersParameters parameters = new SearchSpamUsersParameters()
                .withLogins("login")
                .withLogTable(SearchSpamUsersLogTableEnum.CMD)
                .withDateFrom(LocalDate.now().minusDays(SearchSpamUsersValidationService.MAX_RANGE_IN_DAYS - 1))
                .withDateTo(LocalDate.now());
        ValidationResult<SearchSpamUsersParameters, Defect> result = tool.validate(parameters);
        assertThat(result.hasAnyErrors())
                .isFalse();
    }

    @Test
    public void testValidationCorrectDateRangeEqualsMax() {
        SearchSpamUsersParameters parameters = new SearchSpamUsersParameters()
                .withLogins("login")
                .withLogTable(SearchSpamUsersLogTableEnum.CMD)
                .withDateFrom(LocalDate.now().minusDays(SearchSpamUsersValidationService.MAX_RANGE_IN_DAYS))
                .withDateTo(LocalDate.now());
        ValidationResult<SearchSpamUsersParameters, Defect> result = tool.validate(parameters);
        assertThat(result.hasAnyErrors())
                .isFalse();
    }

    @Test
    public void testValidationInvalidDateRangeGreaterThanMax() {
        SearchSpamUsersParameters parameters = new SearchSpamUsersParameters()
                .withLogins("login")
                .withLogTable(SearchSpamUsersLogTableEnum.CMD)
                .withDateFrom(LocalDate.now().minusDays(SearchSpamUsersValidationService.MAX_RANGE_IN_DAYS + 1))
                .withDateTo(LocalDate.now());
        ValidationResult<SearchSpamUsersParameters, Defect> result = tool.validate(parameters);
        assertThat(result.hasAnyErrors())
                .isTrue();
    }
}

package ru.yandex.market.hrms.api.controller.partner.create;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import one.util.streamex.EntryStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.hrms.core.service.environment.EnvironmentService;
import ru.yandex.market.hrms.core.service.environment.repo.EnvironmentKey;
import ru.yandex.market.hrms.core.service.outstaff.account.dto.OutstaffDomainRequest;
import ru.yandex.market.logistics.management.plugin.hrms.HrmsPlugin;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.common.util.StringFormatter.sf;

@DbUnitDataSet(before = "CreateSameOutstaffPersonMultipleTimesTest.before.csv")
public class CreateSameOutstaffPersonMultipleTimesTest extends AbstractApiTest {
    public static LocalDate CURRENT_DATE = LocalDate.parse("2022-06-01");

    @Autowired
    private EnvironmentService environmentService;

    @BeforeEach
    public void init() {
        mockClock(CURRENT_DATE);
    }

    @ParameterizedTest
    @EnumSource(value = EnvironmentKey.class,
            names = {"CREATE_OUTSTAFF_BY_OLD_SCHEME", "CREATE_OUTSTAFF_BY_NEW_SCHEME"})
    public void shouldNotAllowToCreateOutstaffWithSameNameAndBirthDate(EnvironmentKey creationSchema) throws Exception {
        environmentService.setFlagEnabled(creationSchema);
        mockMvc.perform(prepareRequest(
                "Иван", "Иванов", "Иванович",
                "2000-01-01", "+79001001001", "ВК 000-001",
                new OutstaffDomainRequest(1L, CURRENT_DATE)
        )).andExpect(status().isOk());
        mockMvc.perform(prepareRequest(
                "Иван", "Иванов",
                "2000-01-01", "+79001001002", "ВК 000-002",
                new OutstaffDomainRequest(2L, CURRENT_DATE.plusDays(3))
        )).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @EnumSource(value = EnvironmentKey.class,
            names = {"CREATE_OUTSTAFF_BY_OLD_SCHEME", "CREATE_OUTSTAFF_BY_NEW_SCHEME"})
    public void shouldNotAllowToCreateOutstaffWithSamePhone(EnvironmentKey creationSchema) throws Exception {
        environmentService.setFlagEnabled(creationSchema);
        mockMvc.perform(prepareRequest(
                "Иван", "Иванов",
                "2000-01-01", "+79001001001", "ВК 000-001",
                new OutstaffDomainRequest(1L, CURRENT_DATE)
        )).andExpect(status().isOk());
        mockMvc.perform(prepareRequest(
                "Петр", "Петров",
                "2000-01-02", "+79001001001", "ВК 000-002",
                new OutstaffDomainRequest(2L, CURRENT_DATE.plusDays(3))
        )).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @EnumSource(value = EnvironmentKey.class,
            names = {"CREATE_OUTSTAFF_BY_OLD_SCHEME", "CREATE_OUTSTAFF_BY_NEW_SCHEME"})
    public void shouldNotAllowToCreateOutstaffWithSamePassport(EnvironmentKey creationSchema) throws Exception {
        environmentService.setFlagEnabled(creationSchema);
        mockMvc.perform(prepareRequest(
                "Иван", "Иванов",
                "2000-01-01", "+79001001001", "ВК 000-001",
                new OutstaffDomainRequest(1L, CURRENT_DATE)
        )).andExpect(status().isOk());
        mockMvc.perform(prepareRequest(
                "Петр", "Петров",
                "2000-01-02", "+79001001002", "ВК000001",
                new OutstaffDomainRequest(2L, CURRENT_DATE.plusDays(3))
        )).andExpect(status().isBadRequest());
    }

    private MockHttpServletRequestBuilder prepareRequest(String firstName,
                                                         String lastName,
                                                         String birthDate,
                                                         String phone,
                                                         String passport,
                                                         OutstaffDomainRequest... domainRequests) {
        return prepareRequest(firstName, lastName, null, birthDate, phone, passport, domainRequests);
    }

    private MockHttpServletRequestBuilder prepareRequest(String firstName,
                                                         String lastName,
                                                         @Nullable String midName,
                                                         String birthDate,
                                                         String phone,
                                                         String passport,
                                                         OutstaffDomainRequest... domainRequests) {
        var requestBuilder = multipart("/lms/external/partner/person")
                .file("documents[0].file", "encoded".getBytes())
                .file("documents[3].file", "encoded".getBytes())
                .file("documents[4].file", "encoded".getBytes())
                .file("documents[5].file", "encoded".getBytes())
                .queryParam("companyId", "7")
                .queryParam("positionId", "21")
                .queryParam("firstName", firstName)
                .queryParam("lastName", lastName)
                .queryParam("birthDate", birthDate)
                .queryParam("phone", phone)
                .queryParam("sex", "MALE")
                .queryParam("documents[0].documentType", "PHOTO")
                .queryParam("documents[1].documentType", "PASSPORT")
                .queryParam("documents[1].serialNumber", passport)
                .queryParam("documents[1].cityInPassport", "Москва")
                .queryParam("documents[2].documentType", "MEDBOOK")
                .queryParam("documents[2].serialNumber", "1234567890")
                .queryParam("documents[3].documentType", "HYGIENIC_EXAM")
                .queryParam("documents[3].issuedAt", "2022-05-30")
                .queryParam("documents[4].documentType", "PRE_MED_EXAM")
                .queryParam("documents[4].issuedAt", "2022-05-30")
                .queryParam("documents[5].documentType", "PSYCHO_EXAM")
                .queryParam("documents[5].issuedAt", "2022-05-30")
                .queryParam("russianCitizenship", "true")
                .contentType(MULTIPART_FORM_DATA_VALUE)
                .with(SecurityMockMvcRequestPostProcessors.user("yndxtm-hrms-demo-hr1")
                        .authorities(List.of(
                                new SimpleGrantedAuthority(HrmsPlugin.AUTHORITY_FUNC_BE_MULTIPARTNER)
                        )));
        Optional.ofNullable(midName)
                .ifPresent(s -> requestBuilder.queryParam("midName", s));
        EntryStream.of(domainRequests)
                .forKeyValue((i, x) -> requestBuilder
                        .queryParam(sf("domains[{}].domainId", i), x.getDomainId().toString())
                        .queryParam(sf("domains[{}].startDate", i), x.getStartDate().toString()));
        return requestBuilder;
    }
}
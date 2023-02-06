package ru.yandex.market.tpl.tms.service.handlers;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.company_draft.CompanyDraft;
import ru.yandex.market.tpl.core.domain.company_draft.CompanyDraftRepository;
import ru.yandex.market.tpl.core.domain.company_draft.CreateDbsStatus;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.tms.service.task.handlers.RegistrationNewMbiCabinetHandler;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class RegistrationNewMbiCabinetHandlerTest extends TplTmsAbstractTest {
    private final RegistrationNewMbiCabinetHandler registrationNewMbiCabinetHandler;
    private final TestUserHelper testUserHelper;
    private final CompanyDraftRepository repository;

    @Test
    void test() {
        CompanyDraft draft = testUserHelper.getCompanyDraft();
        draft.setJuridicalName("NAME");
        draft.setLogin("LOGIN");
        draft.setPhone("888888");
        draft.setJuridicalAddress("ADDRESS");
        repository.save(draft);
        registrationNewMbiCabinetHandler.handle(draft);
        Optional<CompanyDraft> optional = repository.findById(draft.getId());
        assertThat(optional.isPresent()).isTrue();
        CompanyDraft newCompanyDraft = optional.get();
        assertThat(newCompanyDraft.getStatus()).isEqualTo(CreateDbsStatus.CREATE_SC_FOR_LMS);
        assertThat(newCompanyDraft.getCompanyId()).isNotNull();
    }
}

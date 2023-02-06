package ru.yandex.market.tpl.tms.service.handlers;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.company_draft.CompanyDraft;
import ru.yandex.market.tpl.core.domain.company_draft.CompanyDraftRepository;
import ru.yandex.market.tpl.core.domain.company_draft.CreateDbsStatus;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.tms.service.task.handlers.PutDbsInformationToMdbHandler;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class PutDbsInformationToMdbHandlerTest extends TplTmsAbstractTest {
    private final TestUserHelper userHelper;
    private final PutDbsInformationToMdbHandler getDbsInformationToMdbHandler;
    private final CompanyDraftRepository companyDraftRepository;

    @Test
    void testGetDbsInformationToMdb() {
        CompanyDraft companyDraft = userHelper.getCompanyDraft();
        companyDraftRepository.save(companyDraft);
        companyDraft.setDsId(1L);
        getDbsInformationToMdbHandler.handle(companyDraft);
        Optional<CompanyDraft> optionalDraft = companyDraftRepository.findById(companyDraft.getId());
        assertThat(optionalDraft).isPresent();
        CompanyDraft updateCompanyDraft = optionalDraft.get();
        assertThat(updateCompanyDraft.getStatus()).isEqualTo(CreateDbsStatus.COMPLETE);
    }
}

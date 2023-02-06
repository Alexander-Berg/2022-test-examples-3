package ru.yandex.market.tpl.core.service.company_draft;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.company_draft.CompanyDraft;
import ru.yandex.market.tpl.core.domain.company_draft.CompanyDraftQueryService;
import ru.yandex.market.tpl.core.domain.company_draft.CompanyDraftRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author zaxarello
 */
@RequiredArgsConstructor
public class CompanyDraftQueryServiceTest extends TplAbstractTest {
    private final CompanyDraftQueryService companyDraftQueryService;
    private final CompanyDraftRepository companyDraftRepository;
    private final TestUserHelper testUserHelper;

    @Test
    void getById() {
        CompanyDraft companyDraft = testUserHelper.getCompanyDraft();
        companyDraftRepository.save(companyDraft);
        CompanyDraft companyDraftAfter = companyDraftQueryService.getById(companyDraft.getId());

        assertThat(companyDraft).isEqualTo(companyDraftAfter);
        this.clearAfterTest(companyDraft);
    }
}

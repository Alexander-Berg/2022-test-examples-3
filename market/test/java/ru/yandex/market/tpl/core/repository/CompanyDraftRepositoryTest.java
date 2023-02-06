package ru.yandex.market.tpl.core.repository;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.company_draft.CompanyDraft;
import ru.yandex.market.tpl.core.domain.company_draft.CompanyDraftRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author zaxarello
 */
@RequiredArgsConstructor
public class CompanyDraftRepositoryTest extends TplAbstractTest {
    private final CompanyDraftRepository companyDraftRepository;
    private final TestUserHelper testUserHelper;
    private CompanyDraft companyDraft;

    @BeforeEach
    void before() {
        companyDraft = testUserHelper.getCompanyDraft();
        companyDraftRepository.save(companyDraft);
        this.clearAfterTest(companyDraft);
    }

    @Test
    void findByLogin() {
        Optional<CompanyDraft> optional = companyDraftRepository.findByLogin(companyDraft.getLogin());
        assertThat(optional).isPresent();
        assertThat(optional.get()).isEqualTo(companyDraft);
        assertThat(companyDraftRepository.findAll()).contains(companyDraft);
    }

    @Test
    void findById() {
        CompanyDraft companyDraft = testUserHelper.getCompanyDraft();
        companyDraftRepository.save(companyDraft);
        Optional<CompanyDraft> optional = companyDraftRepository.findById(companyDraft.getId());
        assertThat(optional).isPresent();
        assertThat(optional.get()).isEqualTo(companyDraft);
        assertThat(companyDraftRepository.findAll()).contains(companyDraft);
    }

}

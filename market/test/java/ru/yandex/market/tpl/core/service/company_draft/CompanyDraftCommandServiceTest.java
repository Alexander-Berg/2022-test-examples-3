package ru.yandex.market.tpl.core.service.company_draft;


import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.company_draft.CompanyDraft;
import ru.yandex.market.tpl.core.domain.company_draft.CompanyDraftRepository;
import ru.yandex.market.tpl.core.domain.company_draft.commands.CompanyDraftCommand;
import ru.yandex.market.tpl.core.domain.company_draft.commands.CompanyDraftCommandService;
import ru.yandex.market.tpl.core.domain.company_draft.commands.CompanyDraftOrgInfoData;
import ru.yandex.market.tpl.core.domain.company_draft.commands.NewCompanyDraftData;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author zaxarello
 */
@RequiredArgsConstructor
public class CompanyDraftCommandServiceTest extends TplAbstractTest {
    private final CompanyDraftCommandService companyDraftCommandService;
    private final CompanyDraftRepository companyDraftRepository;
    private final TestUserHelper testUserHelper;
    private CompanyDraft companyDraft;

    @AfterEach
    void after() {
        this.clearAfterTest(companyDraft);
    }

    @Test
    void createCompanyDraft() {
        String testLogin = "testLogin";
        Long businessId = 2L;
        Double scLatitude = 1.1;
        Double scLongitude = 2.2;
        String scName = "NAME";
        String scAddress = "ADDRESS";
        CompanyDraftCommand.Create create =
                new CompanyDraftCommand.Create(NewCompanyDraftData.builder()
                        .businessId(businessId)
                        .login(testLogin)
                        .scLatitude(scLatitude)
                        .scLongitude(scLongitude)
                        .scName(scName)
                        .scAddress(scAddress)
                        .build());
        companyDraftCommandService.createCompanyDraft(create);
        Optional<CompanyDraft> optional = companyDraftRepository.findByLogin(testLogin);
        assertThat(optional).isPresent();
        companyDraft = optional.get();
        assertThat(companyDraft.getBusinessId()).isEqualTo(businessId);
        assertThat(companyDraft.getLogin()).isEqualTo(testLogin);
        assertThat(companyDraft.getScLatitude()).isEqualTo(scLatitude);
        assertThat(companyDraft.getScLongitude()).isEqualTo(scLongitude);
        assertThat(companyDraft.getScName()).isEqualTo(scName);
        assertThat(companyDraft.getScAddress()).isEqualTo(scAddress);
    }

    @Test
    void updateByOrgInfo() {
        String phone = "PHONE";
        String juridicalAddress = "ADDRESS";
        String juridicalName = "NAME";
        String ogrn = "OGRN";
        String url = "URL";
        companyDraft = testUserHelper.getCompanyDraft();
        companyDraftRepository.save(companyDraft);
        CompanyDraftCommand.UpdateOrgInfo update =
                CompanyDraftCommand.UpdateOrgInfo.builder()
                        .companyDraftId(companyDraft.getId())
                        .data(CompanyDraftOrgInfoData.builder()
                                .juridicalAddress(juridicalAddress)
                                .phone(phone)
                                .juridicalName(juridicalName)
                                .ogrn(ogrn)
                                .url(url)
                                .build())
                        .build();
        companyDraftCommandService.updateOrgInfo(update);
        Optional<CompanyDraft> optional = companyDraftRepository.findByLogin(companyDraft.getLogin());
        assertThat(optional).isPresent();
        companyDraft = optional.get();
        assertThat(companyDraft.getPhone()).isEqualTo(phone);
        assertThat(companyDraft.getJuridicalAddress()).isEqualTo(juridicalAddress);
        assertThat(companyDraft.getJuridicalName()).isEqualTo(juridicalName);
        assertThat(companyDraft.getOgrn()).isEqualTo(ogrn);
        assertThat(companyDraft.getUrl()).isEqualTo(url);
        this.clearAfterTest(companyDraft);
    }

    @Test
    void updateByScId() {
        Long scId = 1L;
        companyDraft = testUserHelper.getCompanyDraft();
        companyDraftRepository.save(companyDraft);
        CompanyDraftCommand.UpdateScId update =
                CompanyDraftCommand.UpdateScId.builder()
                        .companyDraftId(companyDraft.getId())
                        .scId(scId)
                        .build();
        companyDraftCommandService.updateScId(update);
        Optional<CompanyDraft> optional = companyDraftRepository.findByLogin(companyDraft.getLogin());
        assertThat(optional).isPresent();
        companyDraft = optional.get();
        assertThat(companyDraft.getScId()).isEqualTo(scId);
        this.clearAfterTest(companyDraft);
    }

    @Test
    void updateByDsId() {
        Long dsId = 1L;
        companyDraft = testUserHelper.getCompanyDraft();
        companyDraftRepository.save(companyDraft);
        CompanyDraftCommand.UpdateDsId update =
                CompanyDraftCommand.UpdateDsId.builder()
                        .companyDraftId(companyDraft.getId())
                        .dsId(dsId)
                        .build();
        companyDraftCommandService.updateDsId(update);
        Optional<CompanyDraft> optional = companyDraftRepository.findByLogin(companyDraft.getLogin());
        assertThat(optional).isPresent();
        companyDraft = optional.get();
        assertThat(companyDraft.getDsId()).isEqualTo(dsId);
        this.clearAfterTest(companyDraft);
    }

    @Test
    void updateByCompanyId() {
        Long companyId = 1L;
        companyDraft = testUserHelper.getCompanyDraft();
        companyDraftRepository.save(companyDraft);
        CompanyDraftCommand.UpdateCompanyId update =
                CompanyDraftCommand.UpdateCompanyId.builder()
                        .companyDraftId(companyDraft.getId())
                        .companyId(companyId)
                        .build();
        companyDraftCommandService.updateCompanyId(update);
        Optional<CompanyDraft> optional = companyDraftRepository.findByLogin(companyDraft.getLogin());
        assertThat(optional).isPresent();
        companyDraft = optional.get();
        assertThat(companyDraft.getCompanyId()).isEqualTo(companyId);
        this.clearAfterTest(companyDraft);
    }
}

package ru.yandex.market.tpl.tms.service.handlers;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.orginfo.model.OrganizationInfoSource;
import ru.yandex.market.core.orginfo.model.OrganizationType;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerInfoDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerOrgInfoDTO;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;
import ru.yandex.market.tpl.core.domain.company_draft.CompanyDraft;
import ru.yandex.market.tpl.core.domain.company_draft.CompanyDraftRepository;
import ru.yandex.market.tpl.core.domain.company_draft.CreateDbsStatus;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.tms.service.task.handlers.CompanyDraftJuridicalInfoHandler;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class CompanyDraftJuridicalInfoHandlerTest extends TplTmsAbstractTest {
    private final CompanyDraftJuridicalInfoHandler companyDraftJuridicalInfoHandler;
    private final TestUserHelper testUserHelper;
    private final CompanyDraftRepository repository;

    private final MbiOpenApiClient mbiOpenApiClient;

    private final MbiApiClient mbiApiClient;

    private PartnerOrgInfoDTO partnerOrgInfoDTO;

    private PartnerInfoDTO partnerInfoDTO;

    @BeforeEach
    void before() {
        partnerOrgInfoDTO = new PartnerOrgInfoDTO(OrganizationType.AO, "aaa", "ogrn", "address",
                "address", OrganizationInfoSource.PARTNER_INTERFACE, "11", "url");
        partnerInfoDTO = new PartnerInfoDTO(1L, 1L, CampaignType.BUSINESS, "aaa", "bbb", "2222",
                "address", partnerOrgInfoDTO, true, null);
        Mockito.when(mbiOpenApiClient.getPartnerIdsByBusinessId(Mockito.anyLong())).thenReturn(List.of(1L));
        Mockito.when(mbiApiClient.getPartnerInfo(Mockito.anyLong())).thenReturn(partnerInfoDTO);
    }

    @AfterEach
    void after() {
        Mockito.reset(mbiOpenApiClient);
        Mockito.reset(mbiApiClient);
    }

    @Test
    void test() {
        CompanyDraft draft = testUserHelper.getCompanyDraft();
        repository.save(draft);
        companyDraftJuridicalInfoHandler.handle(draft);
        Optional<CompanyDraft> optional = repository.findById(draft.getId());
        assertThat(optional.isPresent()).isTrue();
        CompanyDraft newCompanyDraft = optional.get();
        assertThat(newCompanyDraft.getStatus()).isEqualTo(CreateDbsStatus.REGISTRATION_OF_A_NEW_CABINET);
        assertThat(newCompanyDraft.getJuridicalAddress()).isEqualTo(partnerOrgInfoDTO.getJuridicalAddress());
        assertThat(newCompanyDraft.getJuridicalName()).isEqualTo(partnerOrgInfoDTO.getName());
        assertThat(newCompanyDraft.getOgrn()).isEqualTo(partnerOrgInfoDTO.getOgrn());
        assertThat(newCompanyDraft.getPhone()).isEqualTo(partnerInfoDTO.getPhoneNumber());
        assertThat(newCompanyDraft.getUrl()).isEqualTo(partnerOrgInfoDTO.getInfoUrl());
    }
}

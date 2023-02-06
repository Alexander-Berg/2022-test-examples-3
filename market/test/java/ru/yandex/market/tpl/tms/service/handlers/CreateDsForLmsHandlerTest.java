package ru.yandex.market.tpl.tms.service.handlers;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.tpl.core.domain.company_draft.CompanyDraft;
import ru.yandex.market.tpl.core.domain.company_draft.CompanyDraftRepository;
import ru.yandex.market.tpl.core.domain.company_draft.CreateDbsStatus;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.tms.service.task.handlers.CreateDsForLmsHandler;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@RequiredArgsConstructor
public class CreateDsForLmsHandlerTest extends TplTmsAbstractTest {
    private final TestUserHelper testUserHelper;
    private final CreateDsForLmsHandler createDsForLmsHandler;
    private final CompanyDraftRepository companyDraftRepository;
    @InjectMocks
    private final LMSClient lmsClient;

    @Test
    void test() {
        CompanyDraft draft = testUserHelper.getCompanyDraft();
        companyDraftRepository.save(draft);
        Mockito.when(lmsClient.createPartner(any())).thenReturn(PartnerResponse.newBuilder().id(1L).build());
        createDsForLmsHandler.handle(draft);
        Optional<CompanyDraft> companyDraftOptional = companyDraftRepository.findById(draft.getId());
        assertThat(companyDraftOptional.isPresent()).isTrue();
        CompanyDraft newDraft = companyDraftOptional.get();
        assertThat(newDraft.getDsId()).isEqualTo(1L);
        assertThat(newDraft.getStatus()).isEqualTo(CreateDbsStatus.CREATE_DS_FOR_TPL);
        clearAfterTest(draft);
        Mockito.reset(lmsClient);
    }
}

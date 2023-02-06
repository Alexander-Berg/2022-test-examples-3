package ru.yandex.market.tpl.tms.service.handlers;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.company.CompanyRoleEnum;
import ru.yandex.market.tpl.core.domain.company.Company;
import ru.yandex.market.tpl.core.domain.company.CompanyRepository;
import ru.yandex.market.tpl.core.domain.company_draft.CompanyDraft;
import ru.yandex.market.tpl.core.domain.company_draft.CompanyDraftRepository;
import ru.yandex.market.tpl.core.domain.company_draft.CreateDbsStatus;
import ru.yandex.market.tpl.core.domain.lms.deliveryservice.LmsDeliveryServiceCreateDto;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.DeliveryServiceRoleEnum;
import ru.yandex.market.tpl.core.domain.partner.PartnerRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.service.lms.deliveryservice.DeliveryServiceCommandService;
import ru.yandex.market.tpl.tms.service.task.handlers.ConfigureDbsParametersForEntitiesHandler;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class ConfigureDbsParametersForEntitiesHandlerTest extends TplTmsAbstractTest {
    private final TestUserHelper testUserHelper;
    private static final Long DS_ID = 10345352L;
    private final DeliveryServiceCommandService deliveryServiceCommandService;
    private final ConfigureDbsParametersForEntitiesHandler configureDbsParametersForEntitiesHandler;
    private final CompanyRepository companyRepository;
    private final PartnerRepository<DeliveryService> deliveryServiceRepository;
    private final CompanyDraftRepository companyDraftRepository;

    @Test
    void testConfigureDbsParameters() {
        CompanyDraft draft = testUserHelper.getCompanyDraft();
        Long companyId = testUserHelper.findOrCreateCompany("CompanyBBB").getId();
        deliveryServiceCommandService.createDeliveryService(LmsDeliveryServiceCreateDto.builder()
                .deliveryServiceId(DS_ID)
                .deliveryAreaMarginWidth(0L)
                .name("B")
                .build());
        draft.setCompanyId(companyId);
        draft.setDsId(DS_ID);
        companyDraftRepository.save(draft);
        configureDbsParametersForEntitiesHandler.handle(draft);
        Company company = companyRepository.findByIdOrThrow(companyId);
        DeliveryService deliveryService = deliveryServiceRepository.findByIdOrThrow(DS_ID);
        CompanyDraft updateDraft = companyDraftRepository.findByIdOrThrow(draft.getId());
        assertThat(company.getCompanyRole().getName()).isEqualTo(CompanyRoleEnum.DBS);
        assertThat(deliveryService.getDeliveryServiceRoleEnum()).isEqualTo(DeliveryServiceRoleEnum.DBS);
        assertThat(updateDraft.getStatus()).isEqualTo(CreateDbsStatus.PUT_DBS_INFORMATION_TO_MDB);
    }
}

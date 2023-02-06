package ru.yandex.market.tpl.tms.service.handlers;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.core.domain.company.Company;
import ru.yandex.market.tpl.core.domain.company.CompanyRepository;
import ru.yandex.market.tpl.core.domain.company_draft.CompanyDraft;
import ru.yandex.market.tpl.core.domain.company_draft.CompanyDraftRepository;
import ru.yandex.market.tpl.core.domain.lms.deliveryservice.LmsDeliveryServiceCreateDto;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.PartnerRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.service.lms.deliveryservice.DeliveryServiceCommandService;
import ru.yandex.market.tpl.tms.service.task.handlers.CreateRelationshipBetweenEntities;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class CreateRelationshipBetweenEntitiesTest extends TplTmsAbstractTest {
    private static final Long DS_ID = 100334L;
    private static final Long SC_ID = 1003345L;
    private final CompanyDraftRepository companyDraftRepository;
    private final TestUserHelper testUserHelper;
    private final DeliveryServiceCommandService deliveryServiceCommandService;
    private final CreateRelationshipBetweenEntities createRelationshipBetweenEntities;
    private final CompanyRepository companyRepository;
    private final SortingCenterRepository sortingCenterRepository;
    private final PartnerRepository<DeliveryService> deliveryServiceRepository;
    private final TransactionTemplate transactionTemplate;

    @Test
    void test() {
        CompanyDraft draft = testUserHelper.getCompanyDraft();
        draft.setDsId(DS_ID);
        draft.setScId(SC_ID);
        Long companyId = testUserHelper.findOrCreateCompany("CompanyAAA").getId();
        draft.setCompanyId(companyId);
        companyDraftRepository.save(draft);
        testUserHelper.sortingCenter(SC_ID);
        deliveryServiceCommandService.createDeliveryService(LmsDeliveryServiceCreateDto.builder()
                .deliveryServiceId(DS_ID)
                .deliveryAreaMarginWidth(0L)
                .name("A")
                .build());
        createRelationshipBetweenEntities.handle(draft);
        transactionTemplate.execute(status -> {
            Company company = companyRepository.findByIdOrThrow(companyId);
            SortingCenter sortingCenter = sortingCenterRepository.findByIdOrThrow(SC_ID);
            DeliveryService deliveryService = deliveryServiceRepository.findByIdOrThrow(DS_ID);
            assertThat(company.getSortingCenters()).contains(sortingCenter);
            assertThat(sortingCenter.getDeliveryServices()).contains(deliveryService);
            assertThat(sortingCenter.getCompanies()).contains(company);
            assertThat(deliveryService.getSortingCenter()).isEqualTo(sortingCenter);
            clearAfterTest(company);
            clearAfterTest(sortingCenter);
            clearAfterTest(deliveryService);
            return null;
        });
        clearAfterTest(draft);
    }
}

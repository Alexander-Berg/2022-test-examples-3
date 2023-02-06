package ru.yandex.market.tpl.tms.service.handlers;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.company_draft.CompanyDraft;
import ru.yandex.market.tpl.core.domain.company_draft.CompanyDraftRepository;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.PartnerRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.tms.service.task.handlers.CreateDsForTplHandler;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class CreateDsFroTplHandlerTest extends TplTmsAbstractTest {
    private final TestUserHelper testUserHelper;
    private final CreateDsForTplHandler createDsForTplHandler;
    private final PartnerRepository<DeliveryService> partnerRepository;
    private final CompanyDraftRepository companyDraftRepository;

    @Test
    void test() {
        CompanyDraft draft = testUserHelper.getCompanyDraft();
        draft.setJuridicalName("NAME");
        draft.setLogin("LOGIN");
        draft.setPhone("888888");
        draft.setJuridicalAddress("ADDRESS");
        draft.setDsId(37593879L);

        String name = "DS " + Optional.ofNullable(draft.getJuridicalName()).orElse("");

        companyDraftRepository.save(draft);
        createDsForTplHandler.handle(draft);

        Optional<DeliveryService> deliveryServiceOptional = partnerRepository.findById(37593879L);
        assertThat(deliveryServiceOptional.isPresent()).isTrue();
        DeliveryService deliveryService = deliveryServiceOptional.get();
        assertThat(deliveryService.getId()).isEqualTo(draft.getDsId());
        assertThat(deliveryService.getName()).isEqualTo(name);
        assertThat(deliveryService.getDeliveryAreaMarginWidth()).isEqualTo(0L);
        clearAfterTest(deliveryService);
        clearAfterTest(draft);
    }
}

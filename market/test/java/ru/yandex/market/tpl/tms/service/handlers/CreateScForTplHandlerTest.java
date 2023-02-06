package ru.yandex.market.tpl.tms.service.handlers;

import java.time.ZoneOffset;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.company_draft.CompanyDraft;
import ru.yandex.market.tpl.core.domain.company_draft.CompanyDraftRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.tms.service.task.handlers.CreateScForTplHandler;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class CreateScForTplHandlerTest extends TplTmsAbstractTest {
    private final TestUserHelper testUserHelper;
    private final CreateScForTplHandler createScForTplHandler;
    private final SortingCenterRepository sortingCenterRepository;
    private final CompanyDraftRepository companyDraftRepository;

    @Test
    void test() {
        CompanyDraft draft = testUserHelper.getCompanyDraft();
        draft.setJuridicalName("NAME");
        draft.setLogin("LOGIN");
        draft.setPhone("888888");
        draft.setJuridicalAddress("ADDRESS");
        draft.setScName("SC NAME");
        draft.setScId(37593875L);
        draft.setScLatitude(1.);
        draft.setScLongitude(1.);
        companyDraftRepository.save(draft);
        createScForTplHandler.handle(draft);
        Optional<SortingCenter> sortingCenterOptional = sortingCenterRepository.findById(37593875L);
        assertThat(sortingCenterOptional.isPresent()).isTrue();
        SortingCenter sortingCenter = sortingCenterOptional.get();
        assertThat(sortingCenter.getZoneOffset()).isEqualTo(ZoneOffset.ofHours(+3));
        assertThat(sortingCenter.getId()).isEqualTo(draft.getScId());
        assertThat(sortingCenter.getLatitude().doubleValue()).isEqualTo(draft.getScLatitude());
        assertThat(sortingCenter.getLongitude().doubleValue()).isEqualTo(draft.getScLongitude());
        assertThat(sortingCenter.getName()).isEqualTo(draft.getScName());
        assertThat(sortingCenter.getPartnerName()).isEqualTo(draft.getJuridicalName());
        assertThat(sortingCenter.getAddress()).isEqualTo(draft.getScAddress());
        clearAfterTest(sortingCenter);
        clearAfterTest(draft);
    }
}

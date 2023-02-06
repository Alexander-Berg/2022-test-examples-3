package ru.yandex.market.sc.core.domain.sorting_center;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterPartner;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterPartnerRepository;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class SortingCenterPartnerCommandServiceTest {

    private static final long PARTNER_ID = 40948L;
    private static final String TOKEN = "544cc6c0f11e403da4ef84b27381d0f4898205270d5949609cc4876fc9903889";

    private final SortingCenterPartnerCommandService sortingCenterPartnerCommandService;
    private final SortingCenterPartnerRepository sortingCenterPartnerRepository;

    @Test
    void createSortingCenterPartner() {
        sortingCenterPartnerCommandService.create(PARTNER_ID, TOKEN);

        var actual = sortingCenterPartnerRepository.findByToken(TOKEN);

        var expected = new SortingCenterPartner(PARTNER_ID, TOKEN);

        assertThat(actual).isPresent();
        assertThat(actual.get()).isEqualTo(expected);
    }

    @Test
    void idempotentCreateSortingCenterPartnerWithTheSameToken() {
        sortingCenterPartnerCommandService.create(PARTNER_ID, TOKEN);

        var actual = sortingCenterPartnerCommandService.create(PARTNER_ID, TOKEN);

        var expected = new SortingCenterPartner(PARTNER_ID, TOKEN);

        assertThat(actual).isEqualTo(expected);
    }
}

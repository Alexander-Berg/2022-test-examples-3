package ru.yandex.market.sc.core.domain.outbound;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.sorting_center.repository.PartnerMappingGroupRepository;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PartnerMappingGroupRepositoryTest {

    private final TestFactory testFactory;
    private final PartnerMappingGroupRepository partnerMappingGroupRepository;

    @ParameterizedTest
    @ValueSource(longs = {1L, 2L, 3L})
    void findGroupItems(Long value) {
        Long[] group = {1L, 2L, 3L};
        testFactory.storedPartnerMappingGroup(group);
        assertThat(partnerMappingGroupRepository.findAllByGroupItem(value)).containsExactlyInAnyOrder(group);
        assertThat(partnerMappingGroupRepository.findAllByGroupItem(100L)).isEmpty();
    }

}

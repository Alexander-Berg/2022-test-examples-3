package ru.yandex.market.deepmind.tms.services;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.HidingReasonType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.HidingReasonDescription;
import ru.yandex.market.deepmind.common.hiding.HidingReasonDescriptionRepository;

import static org.assertj.core.api.Assertions.assertThat;

public class HidingReasonDescriptionRepositoryTest extends BaseHidingsServiceTest {

    @Autowired
    private HidingReasonDescriptionRepository hidingReasonDescriptionRepository;

    @Test
    public void testSavingHidingDescription() {
        var keysToAdd = List.of("reason1_subreason1", "reason2_subreason2");
        assertThat(getExistingReasonKeys()).doesNotContainAnyElementsOf(keysToAdd);

        hidingReasonDescriptionRepository.addHidingDescriptionsIfNotExistsByKey(keysToAdd);
        var allKeys = getExistingReasonKeys();
        assertThat(allKeys).containsAll(keysToAdd);

        hidingReasonDescriptionRepository.addHidingDescriptionsIfNotExistsByKey(keysToAdd);
        assertThat(getExistingReasonKeys().size()).isEqualTo(allKeys.size());
    }

    @Test
    public void testSavingHidingDescriptionMap() {
        var descriptionsToAdd = List.of(
            new HidingReasonDescription().setReasonKey("reason1_subreason1")
                .setType(HidingReasonType.REASON_KEY).setExtendedDesc("111"),
            new HidingReasonDescription().setReasonKey("reason2_subreason2")
                .setType(HidingReasonType.REASON_KEY).setExtendedDesc("222"));

        var keys = descriptionsToAdd.stream().map(HidingReasonDescription::getReasonKey)
            .collect(Collectors.toSet());
        assertThat(getExistingReasonKeys()).doesNotContainAnyElementsOf(keys);

        hidingReasonDescriptionRepository.addHidingDescriptionsIfNotExists(descriptionsToAdd);
        assertThat(hidingReasonDescriptionRepository.findAll())
            .usingElementComparatorIgnoringFields("id")
            .containsAll(descriptionsToAdd);
        var allKeys = getExistingReasonKeys();
        assertThat(allKeys).containsAll(keys);

        hidingReasonDescriptionRepository.addHidingDescriptionsIfNotExists(descriptionsToAdd);
        assertThat(getExistingReasonKeys().size()).isEqualTo(allKeys.size());
    }

    @Test
    public void testFindHidingDescriptionMap() {
        var descriptionsToAdd = List.of(
            new HidingReasonDescription().setReasonKey("reason1_subreason1")
                .setType(HidingReasonType.REASON_KEY).setExtendedDesc("111"),
            new HidingReasonDescription().setReasonKey("reason2_subreason2")
                .setType(HidingReasonType.REASON_KEY).setExtendedDesc("222"));

        hidingReasonDescriptionRepository.addHidingDescriptionsIfNotExists(descriptionsToAdd);

        var result = hidingReasonDescriptionRepository.findByReasonKeysMap(
            descriptionsToAdd.stream().map(HidingReasonDescription::getReasonKey).collect(Collectors.toSet()));
        assertThat(result.values())
            .usingElementComparatorIgnoringFields("id")
            .containsAll(descriptionsToAdd);
    }
}

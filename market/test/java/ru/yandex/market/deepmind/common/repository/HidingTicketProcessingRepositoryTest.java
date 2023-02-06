package ru.yandex.market.deepmind.common.repository;

import java.util.Arrays;
import java.util.Comparator;

import javax.annotation.Resource;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.HidingTicketProcessing;

public class HidingTicketProcessingRepositoryTest extends DeepmindBaseDbTestClass {

    @Resource
    private HidingTicketProcessingRepository hidingTicketProcessingRepository;
    private EnhancedRandom random;

    @Before
    public void setUp() {
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(1).build();
    }

    @Test
    public void testAllFieldsAreConverted() {
        var processing1 = random.nextObject(HidingTicketProcessing.class);

        hidingTicketProcessingRepository.save(processing1);

        var rows = hidingTicketProcessingRepository.findAll();
        var configs = hidingTicketProcessingRepository.getAllConfigs();

        Assertions.assertThat(configs.get(0))
            .usingComparatorForFields(listToArrayComparator(), "followers")
            .isEqualToComparingFieldByFieldRecursively(rows.get(0));
    }

    @Test
    public void testAllFieldsAreConvertedAlsoWithNullOrEmpty() {
        var processing2 = random.nextObject(HidingTicketProcessing.class);
        processing2.setAssignee(null).setFollowers(new String[0]);

        hidingTicketProcessingRepository.save(processing2);

        var rows = hidingTicketProcessingRepository.findAll();
        var configs = hidingTicketProcessingRepository.getAllConfigs();

        Assertions.assertThat(configs.get(0))
            .usingComparatorForFields(listToArrayComparator(), "followers")
            .isEqualToComparingFieldByFieldRecursively(rows.get(0));
    }

    private static Comparator<Object> listToArrayComparator() {
        return (o1, o2) -> {
            if (o1 instanceof String[]) {
                o1 = Arrays.asList((String[]) o1);
            }
            if (o2 instanceof String[]) {
                o2 = Arrays.asList((String[]) o2);
            }

            return o1.equals(o2) ? 0 : 1;
        };
    }
}

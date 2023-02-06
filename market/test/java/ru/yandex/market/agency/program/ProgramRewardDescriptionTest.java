package ru.yandex.market.agency.program;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.core.agency.program.ProgramRewardType;

/**
 * Проверка настроек {@link ProgramRewardDescription}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class ProgramRewardDescriptionTest extends FunctionalTest {

    @Autowired
    private List<ProgramRewardDescription> allProgramRewardDescriptions;

    @Test
    @DisplayName("Все типы премий должны иметь ProgramRewardDescription")
    void testProgramRewardDescription() {
        final Set<ProgramRewardType> actual = allProgramRewardDescriptions.stream()
                .map(ProgramRewardDescription::getProgramRewardType)
                .collect(Collectors.toSet());

        MatcherAssert.assertThat(actual,
                Matchers.containsInAnyOrder(
                        Arrays.stream(ProgramRewardType.values())
                        .filter(it -> !ProgramRewardType.ON_BOARDING.equals(it)
                            &&!ProgramRewardType.GMV.equals(it))
                        .toArray(ProgramRewardType[]::new)
                ));
    }
}

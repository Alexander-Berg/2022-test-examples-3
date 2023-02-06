package ru.yandex.direct.core.entity.deal.service;

import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.deal.model.DealPlacement;
import ru.yandex.direct.core.entity.placements.model.Placement;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static ru.yandex.direct.core.entity.deal.service.DealService.extractFormatsFromPlacement;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class DealServiceExtractFormatsFromPlacementTest {

    @Parameterized.Parameter
    public String blocks;

    @Parameterized.Parameter(1)
    public List<Long> impId;

    @Parameterized.Parameter(2)
    public Set<String> expected;

    @Parameterized.Parameters()
    public static Object[][] getParameters() {
        return new Object[][]{
                {
                        "{\"1\": [\"400x200\", \"0x0\"], \"2\": [\"400x200\", \"1000x1000\"], \"3\": [\"400x200\", \"800x600\"]}",
                        asList(1L, 2L),
                        asSet("400x200", "0x0", "1000x1000")
                },
                {
                        null,
                        asList(1L, 2L),
                        emptySet()
                },
                {
                        "{\"1\": [\"400x200\", \"0x0\"], \"2\": [\"400x200\", \"1000x1000\"], \"3\": [\"400x200\", \"800x600\"]}",
                        asList(4L, 5L),
                        emptySet()
                },
        };
    }

    @Test
    public void test() {
        assertThat(
                extractFormatsFromPlacement(new Placement().withBlocks(blocks), new DealPlacement().withImpId(impId)))
                .isEqualTo(expected);
    }
}

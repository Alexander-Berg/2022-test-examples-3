package ru.yandex.market.logistics.management.service.export.dynamic;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.Mockito;

import ru.yandex.market.logistics.Logistics;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.CutoffDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerRelationDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;

class RelationsBuilderCutoffsTest extends AbstractDynamicBuilderTest {
    private static final String FILE_PATH = PATH_PREFIX + "cutoffs/";

    @ParameterizedTest(name = "{index} : {1}")
    @ArgumentsSource(TestArgumentsProvider.class)
    void testBuildWithCutoffs(Set<CutoffDto> cutoffs, String filename) {
        List<PartnerRelationDto> partnerRelations = createPartnerRelations();
        partnerRelations.get(0).getCutoffs().addAll(cutoffs);
        Mockito.when(partnerRelationRepository.findAllForDynamic(any(), anySet(), anySet(), any()))
            .thenReturn(partnerRelations)
            .thenReturn(Collections.emptyList());

        Logistics.MetaInfo metaInfo = buildReport();

        String path = FILE_PATH + filename + ".json";

        softly.assertThat(metaInfo).as("Fulfillments and delivery services are equal")
            .hasSameFFsAndDssAs(path);
    }

    static class TestArgumentsProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                Arguments.arguments(createCutoffs(), "two_cutoffs"),
                Arguments.arguments(Collections.emptySet(), "empty_cutoffs")
            );
        }

        private static Set<CutoffDto> createCutoffs() {
            return Sets.newHashSet(
                new CutoffDto()
                    .setCutoffTime(LocalTime.of(10 + 1, 0))
                    .setPackagingDuration(Duration.ofHours(12))
                    .setLocationId(10 * 10)
            );
        }
    }
}

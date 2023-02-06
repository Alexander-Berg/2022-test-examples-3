package ru.yandex.market.logistics.management.service.export.dynamic;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.Mockito;

import ru.yandex.market.logistics.Logistics;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerRelationDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.ProductRatingDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;

class RelationsBuilderProductRatingTest extends AbstractDynamicBuilderTest {
    private static final String FILE_PATH = PATH_PREFIX + "productratings/";

    @ParameterizedTest(name = "{index} : {1}")
    @ArgumentsSource(TestArgumentsProvider.class)
    void testBuildWithProductRatings(List<PartnerRelationDto> partnerRelations, String filename) {
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
                Arguments.arguments(createProductRatings(), "one_product_rating"),
                Arguments.arguments(createPartnerRelations(), "empty_product_ratings")
            );
        }

        private static List<PartnerRelationDto> createProductRatings() {
            List<PartnerRelationDto> pr = createPartnerRelations();
            pr.get(0).getProductRatings().addAll(
                Sets.newLinkedHashSet(Collections.singletonList(
                    new ProductRatingDto()
                        .setRating(11)
                        .setLocationId(10 * 20)
                )));
            return pr;
        }
    }
}

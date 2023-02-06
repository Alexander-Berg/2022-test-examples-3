package ru.yandex.market.logistics.front.library;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import ru.yandex.market.logistics.front.library.annotation.WithChild;
import ru.yandex.market.logistics.front.library.dto.Mode;
import ru.yandex.market.logistics.front.library.dto.ViewType;
import ru.yandex.market.logistics.front.library.dto.detail.DetailChild;
import ru.yandex.market.logistics.front.library.dto.detail.DetailData;
import ru.yandex.market.logistics.front.library.dto.detail.DetailMeta;
import ru.yandex.market.logistics.front.library.util.ViewUtils;

class WithChildrenTest {

    @ParameterizedTest(name = "{index}: {2}")
    @ArgumentsSource(WithChildrenArgumentsProviderImpl.class)
    void testFieldsSorted(WithChildrenTest.BaseMock mockDto, List<DetailChild> expectedChildren, String description) {

        DetailData detailView = ViewUtils.getDetail(mockDto, Mode.VIEW, false);

        Assertions.assertThat(detailView.getMeta())
            .extracting(DetailMeta::getChildren)
            .as("children has to be equal")
            .isEqualTo(expectedChildren);
    }

    private static class WithChildrenArgumentsProviderImpl implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                oneChildArgument(),
                twoChildrenArgument(),
                noChildArgument()
            );
        }

        private Arguments oneChildArgument() {
            List<DetailChild> expected = Collections.singletonList(
                DetailChild.builder()
                    .title("testTitle")
                    .slug("plugin/testSlug")
                    .parentSlug("parentSlug")
                    .type(ViewType.GRID)
                    .parentColumn("parentColumn")
                    .idFieldName("marketId")
                    .pageSize(15)
                    .isCrossPluginSlug(true)
                    .build()
            );

            return Arguments.of(new WithChildrenTest.MockWithOneChild(), expected, "one child");
        }

        private Arguments twoChildrenArgument() {
            List<DetailChild> expected = Arrays.asList(
                DetailChild.builder()
                    .title("testTitle")
                    .slug("testSlug")
                    .parentSlug("")
                    .type(ViewType.GRID)
                    .parentColumn("parentColumn")
                    .idFieldName("id")
                    .pageSize(5)
                    .isCrossPluginSlug(false)
                    .build(),
                DetailChild.builder()
                    .title("testTitle2")
                    .slug("testSlug2")
                    .parentSlug("")
                    .type(ViewType.DETAILS)
                    .parentColumn("parentColumn2")
                    .idFieldName("id")
                    .pageSize(5)
                    .isCrossPluginSlug(false)
                    .build()
            );

            return Arguments.of(new WithChildrenTest.MockWithTwoChildren(), expected, "two children");
        }

        private Arguments noChildArgument() {
            return Arguments.of(new WithChildrenTest.MockNoChildren(), Collections.emptyList(), "no children");
        }
    }

    @WithChild(
        slug = "plugin/testSlug",
        title = "testTitle",
        type = ViewType.GRID,
        mappedBy = "parentColumn",
        identifiedBy = "marketId",
        pageSize = 15,
        isCrossPluginSlug = true,
        parentSlug = "parentSlug"
    )
    private static class MockWithOneChild extends WithChildrenTest.BaseMock {
    }

    @WithChild(slug = "testSlug", title = "testTitle", type = ViewType.GRID, mappedBy = "parentColumn")
    @WithChild(slug = "testSlug2", title = "testTitle2", type = ViewType.DETAILS, mappedBy = "parentColumn2")
    private static class MockWithTwoChildren extends WithChildrenTest.BaseMock {
    }

    private static class MockNoChildren extends WithChildrenTest.BaseMock {
    }

    private static class BaseMock {
        private Long id = 1L;

        public Long getId() {
            return id;
        }
    }
}

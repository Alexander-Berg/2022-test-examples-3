package ru.yandex.market.logistics.front.library;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.front.library.annotation.ActionEnabled;
import ru.yandex.market.logistics.front.library.annotation.WithAction;
import ru.yandex.market.logistics.front.library.annotation.WithActions;
import ru.yandex.market.logistics.front.library.dto.Action;
import ru.yandex.market.logistics.front.library.dto.ActionMethod;
import ru.yandex.market.logistics.front.library.dto.ActionType;
import ru.yandex.market.logistics.front.library.dto.Mode;
import ru.yandex.market.logistics.front.library.dto.detail.DetailData;
import ru.yandex.market.logistics.front.library.util.ViewUtils;

import static org.assertj.core.api.Assertions.assertThat;

class WithActionTest {

    @MethodSource("singleActionArgumentsProvider")
    @ParameterizedTest(name = "{index} : {1}")
    void testSingleAction(@SuppressWarnings("unused") String displayName, MockDto dto, Action expectedAction) {
        DetailData detailView = ViewUtils.getDetail(dto, Mode.VIEW, false);
        assertThat(detailView.getMeta().getActions()).hasSize(1);
        assertThat(detailView.getMeta().getActions())
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(expectedAction);
    }

    private static Stream<Arguments> singleActionArgumentsProvider() {
        return Stream.of(
            Arguments.of(
                "Action с несколькими указанными идентификаторами",
                new WithOneAction(),
                Action.builder()
                    .title("testAction")
                    .description("Description")
                    .slug("testslug")
                    .identifiedBy(Arrays.asList("id", "name"))
                    .type(ActionType.ROW_ACTION)
                    .method(ActionMethod.POST_BODY)
                    .build()
            ),
            Arguments.of(
                "Action без указанных идентификаторов",
                new WithOneActionWithoutIdentifiers(),
                Action.builder()
                    .title("testAction1")
                    .description("Description1")
                    .slug("testslug1")
                    .identifiedBy(Collections.emptyList())
                    .type(ActionType.ROW_ACTION)
                    .method(ActionMethod.POST_BODY)
                    .build()
            ),
            Arguments.of(
                "Action без указанных идентификаторов",
                new WithTypeMethodAndIconAction(),
                Action.builder()
                    .title("testAction")
                    .description("Description")
                    .slug("slug")
                    .identifiedBy(Collections.singletonList("id"))
                    .type(ActionType.GRID_ACTION)
                    .method(ActionMethod.POST_MULTIPART)
                    .icon("cloud_upload")
                    .authorities(Arrays.asList("AUTHORITY_1", "AUTHORITY_2"))
                    .build()
            )
        );
    }

    @Test
    @DisplayName("Dto без Action")
    void testNoActions() {
        DetailData detailVew = ViewUtils.getDetail(new WithNoActionsImpl(), Mode.VIEW, false);
        assertThat(detailVew.getMeta().getActions()).isEmpty();
    }

    @MethodSource("multipleActionsArgumentsProvider")
    @ParameterizedTest(name = "{index} : {1}")
    void testMultipleActions(String displayName, MockDto dto) {
        DetailData detailView = ViewUtils.getDetail(dto, Mode.VIEW, false);
        List<Action> actions = detailView.getMeta().getActions();
        assertThat(actions).usingRecursiveFieldByFieldElementComparator().containsExactly(
            Action.builder()
                .title("testAction1")
                .description("Description1")
                .slug("slug1")
                .identifiedBy(Collections.singletonList("id"))
                .type(ActionType.ROW_ACTION)
                .method(ActionMethod.POST_BODY)
                .build(),
            Action.builder()
                .title("testAction2")
                .description("Description2")
                .slug("slug2")
                .identifiedBy(Collections.emptyList())
                .type(ActionType.ROW_ACTION)
                .method(ActionMethod.POST_BODY)
                .build()
        );
    }

    @MethodSource("actionsEnabledArgumentsProvider")
    @ParameterizedTest(name = "{index} : {0}")
    void testActionsEnabled(String displayName, WithNoActions dto, List<Action> expectedActions) {
        DetailData detailView = ViewUtils.getDetail(dto, Mode.VIEW, false);
        assertThat(detailView.getMeta().getActions()).containsExactlyInAnyOrderElementsOf(expectedActions);
    }

    private static Stream<Arguments> actionsEnabledArgumentsProvider() {
        return Stream.of(
            Arguments.of(
                "Action с неуказанным active",
                new WithOneAction(),
                ImmutableList.of(
                    Action.builder()
                        .title("testAction")
                        .description("Description")
                        .slug("testslug")
                        .identifiedBy(Arrays.asList("id", "name"))
                        .type(ActionType.ROW_ACTION)
                        .method(ActionMethod.POST_BODY)
                        .active(null)
                        .build()
                )
            ),
            Arguments.of(
                "Action с указанной активностью в мапе",
                new WithOneActivityMapActions(true),
                ImmutableList.of(
                    Action.builder()
                        .title("testAction1")
                        .description("Description1")
                        .slug("slug1")
                        .identifiedBy(Collections.emptyList())
                        .type(ActionType.ROW_ACTION)
                        .method(ActionMethod.POST_BODY)
                        .active(null)
                        .build(),
                    Action.builder()
                        .title("testAction2")
                        .description("Description2")
                        .slug("slug2")
                        .identifiedBy(Collections.emptyList())
                        .type(ActionType.ROW_ACTION)
                        .method(ActionMethod.POST_BODY)
                        .active(null)
                        .build()
                )
            ),
            Arguments.of(
                "Action с указанной неактивностью в мапе",
                new WithOneActivityMapActions(false),
                ImmutableList.of(
                    Action.builder()
                        .title("testAction1")
                        .description("Description1")
                        .slug("slug1")
                        .identifiedBy(Collections.emptyList())
                        .type(ActionType.ROW_ACTION)
                        .method(ActionMethod.POST_BODY)
                        .active(false)
                        .build(),
                    Action.builder()
                        .title("testAction2")
                        .description("Description2")
                        .slug("slug2")
                        .identifiedBy(Collections.emptyList())
                        .type(ActionType.ROW_ACTION)
                        .method(ActionMethod.POST_BODY)
                        .active(null)
                        .build()
                )
            ),
            Arguments.of(
                "Несколько Action с одним неактивным в мапе",
                new WithActivityMapActions(true, false),
                ImmutableList.of(
                    Action.builder()
                        .title("testAction1")
                        .description("Description1")
                        .slug("slug1")
                        .identifiedBy(Collections.emptyList())
                        .type(ActionType.ROW_ACTION)
                        .method(ActionMethod.POST_BODY)
                        .active(null)
                        .build(),
                    Action.builder()
                        .title("testAction2")
                        .description("Description2")
                        .slug("slug2")
                        .identifiedBy(Collections.emptyList())
                        .type(ActionType.ROW_ACTION)
                        .method(ActionMethod.POST_BODY)
                        .active(false)
                        .build()
                )
            ),
            Arguments.of(
                "Несколько Action с двумя неактивными в мапе",
                new WithActivityMapActions(false, false),
                ImmutableList.of(
                    Action.builder()
                        .title("testAction1")
                        .description("Description1")
                        .slug("slug1")
                        .identifiedBy(Collections.emptyList())
                        .type(ActionType.ROW_ACTION)
                        .method(ActionMethod.POST_BODY)
                        .active(false)
                        .build(),
                    Action.builder()
                        .title("testAction2")
                        .description("Description2")
                        .slug("slug2")
                        .identifiedBy(Collections.emptyList())
                        .type(ActionType.ROW_ACTION)
                        .method(ActionMethod.POST_BODY)
                        .active(false)
                        .build()
                )
            )

        );
    }

    private static Stream<Arguments> multipleActionsArgumentsProvider() {
        return Stream.of(
            Arguments.of(
                "Несколько Action",
                new WithTwoActions()
            ),
            Arguments.of(
                "Несколько Repeatable Action",
                new WithTwoRepeatableActions()
            )
        );
    }

    @WithAction(title = "testAction", description = "Description", slug = "testslug", identifiedBy = {"id", "name"})
    private static class WithOneAction extends MockDto implements WithNoActions {
        WithOneAction() {
            super(1L, "", false, 0, Collections.emptyList());
        }
    }

    @WithAction(title = "testAction1", description = "Description1", slug = "testslug1")
    private static class WithOneActionWithoutIdentifiers extends MockDto {
        WithOneActionWithoutIdentifiers() {
            super(1L, "", false, 0, Collections.emptyList());
        }
    }

    @WithActions({
        @WithAction(title = "testAction1", description = "Description1", slug = "slug1", identifiedBy = "id"),
        @WithAction(title = "testAction2", description = "Description2", slug = "slug2")
    })
    private static class WithTwoActions extends MockDto {
        WithTwoActions() {
            super(1L, "", false, 0, Collections.emptyList());
        }
    }

    @WithAction(title = "testAction1", description = "Description1", slug = "slug1", identifiedBy = "id")
    @WithAction(title = "testAction2", description = "Description2", slug = "slug2")
    private static class WithTwoRepeatableActions extends MockDto {
        WithTwoRepeatableActions() {
            super(1L, "", false, 0, Collections.emptyList());
        }
    }

    @WithAction(
        title = "testAction",
        description = "Description",
        slug = "slug",
        identifiedBy = "id",
        type = ActionType.GRID_ACTION,
        method = ActionMethod.POST_MULTIPART,
        icon = "cloud_upload",
        authorities = {"AUTHORITY_1", "AUTHORITY_2"}
    )
    private static class WithTypeMethodAndIconAction extends MockDto {
        WithTypeMethodAndIconAction() {
            super(1L, "", false, 0, Collections.emptyList());
        }
    }

    private interface WithNoActions {
    }

    public class WithNoActionsImpl implements WithNoActions {
        Long id = 1L;

        public Long getId() {
            return id;
        }
    }

    @WithAction(title = "testAction1", description = "Description1", slug = "slug1")
    @WithAction(title = "testAction2", description = "Description2", slug = "slug2")
    private static class WithActivityMapActions implements WithNoActions {
        Long id = 1L;

        @ActionEnabled(slug = "slug1")
        boolean slug1;

        @ActionEnabled(slug = "slug2")
        boolean slug2;

        WithActivityMapActions(boolean slug1, boolean slug2) {
            this.slug1 = slug1;
            this.slug2 = slug2;
        }

        public Long getId() {
            return id;
        }

        public boolean isSlug1() {
            return slug1;
        }

        public boolean isSlug2() {
            return slug2;
        }
    }

    @WithAction(title = "testAction1", description = "Description1", slug = "slug1")
    @WithAction(title = "testAction2", description = "Description2", slug = "slug2")
    private static class WithOneActivityMapActions implements WithNoActions {
        Long id = 1L;

        @ActionEnabled(slug = "slug1")
        boolean slug1;

        WithOneActivityMapActions(boolean slug1) {
            this.slug1 = slug1;
        }

        public Long getId() {
            return id;
        }

        public boolean getSlug1() {
            return slug1;
        }
    }
}

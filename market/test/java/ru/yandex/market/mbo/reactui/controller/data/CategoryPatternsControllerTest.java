package ru.yandex.market.mbo.reactui.controller.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.common.gwt.security.AccessControlManager;
import ru.yandex.common.gwt.shared.User;
import ru.yandex.market.mbo.db.TovarTreeForVisualService;
import ru.yandex.market.mbo.db.params.PatternService;
import ru.yandex.market.mbo.gwt.models.gurulight.GLPattern;
import ru.yandex.market.mbo.gwt.models.gurulight.GLPatternImpl;
import ru.yandex.market.mbo.gwt.models.params.CategoryPatterns;
import ru.yandex.market.mbo.gwt.models.visual.InheritedGLPattern;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryNode;
import ru.yandex.market.mbo.gwt.models.visual.TovarTree;
import ru.yandex.market.mbo.reactui.dto.ListDto;
import ru.yandex.market.mbo.reactui.dto.ResponseDto;
import ru.yandex.market.mbo.reactui.dto.glpatterns.GLPatternDto;
import ru.yandex.market.mbo.reactui.dto.glpatterns.GLPatternRequestDto;

/**
 * @author nikita-stenin
 * @created 05.11.2020
 */
public class CategoryPatternsControllerTest {
    private static final long DEFAULT_HID = -1L;
    private static final long ROOT_HID = 1L;
    private static final long CHILD1_HID = 2L;
    private static final Integer ROOT_TOVAR_ID = 1;
    private static final Integer CHILD1_TOVAR_ID = 2;

    private static final long PATTERN1_ID = 1L;
    private static final String PATTERN1_NAME = "pattern-test-1";

    private static final long PATTERN2_ID = 2L;
    private static final String PATTERN2_NAME = "pattern-test-2";
    private static final int PATTERN2_NEW_WEIGHT = 101;

    private static final long INHERITED_PATTERN_ID = 3L;
    private static final String INHERITED_PATTERN_NAME = "pattern-test-3";

    private static final long NEW_PATTERN_ID = 4L;
    private static final String NEW_PATTERN_NAME = "pattern-test-4";
    private static final int NEW_PATTERN_WEIGHT = 123;

    private CategoryPatternsController categoryPatternsController;
    private PatternService patternService;
    private ObjectMapper objectMapper;

    @Before
    public void setup() {
        patternService = Mockito.mock(PatternService.class);
        objectMapper = new ObjectMapper();

        final AccessControlManager accessControlManager = Mockito.mock(AccessControlManager.class);
        final TovarTreeForVisualService visualTovarTreeService = Mockito.mock(TovarTreeForVisualService.class);

        categoryPatternsController = new CategoryPatternsController(
            patternService, accessControlManager, visualTovarTreeService
        );

        Mockito.when(accessControlManager.getCachedUser()).thenReturn(new User());
        Mockito.when(visualTovarTreeService.loadCachedWholeTree()).thenReturn(getTovarTree());
        Mockito.when(patternService.loadCategoryPatternsByHid(CHILD1_HID)).thenReturn(getCategoryPatterns());
    }

    @Test
    public void testGetAllCategoryPatterns() {
        ResponseDto<ListDto<GLPatternDto>> response = categoryPatternsController.getPatterns(CHILD1_TOVAR_ID);
        final int size = 3;

        Assertions.assertThat(response.getData().getItems())
            .hasSize(size)
            .extracting(GLPatternDto::getId, GLPatternDto::getName)
            .containsExactlyInAnyOrder(
                Tuple.tuple(PATTERN1_ID, PATTERN1_NAME),
                Tuple.tuple(PATTERN2_ID, PATTERN2_NAME),
                Tuple.tuple(INHERITED_PATTERN_ID, INHERITED_PATTERN_NAME)
            );
    }

    @Test
    public void testGetCategoryPattern() {
        ResponseDto<GLPatternDto> response = categoryPatternsController.getPattern(CHILD1_TOVAR_ID, PATTERN2_ID);

        Assertions.assertThat(response.getData())
            .extracting(GLPatternDto::getId, GLPatternDto::getName)
            .containsExactly(PATTERN2_ID, PATTERN2_NAME);
    }

    @Test
    public void testCreateCategoryPattern() throws Exception {
        Mockito.when(patternService.addPattern(Mockito.any()))
            .thenAnswer(invocation -> {
                GLPattern p = invocation.getArgument(0);
                p.setId(NEW_PATTERN_ID);

                return p;
            });

        String content = "{\"name\":\"pattern-test-4\",\"weight\":123,\"samples\":[]}";
        GLPatternRequestDto body = objectMapper.readValue(content, GLPatternRequestDto.class);
        ResponseDto<GLPatternDto> response = categoryPatternsController.createPattern(CHILD1_TOVAR_ID, body);

        Assertions.assertThat(response.getData())
            .extracting(GLPatternDto::getId, GLPatternDto::getName, GLPatternDto::getWeight)
            .containsExactly(NEW_PATTERN_ID, NEW_PATTERN_NAME, NEW_PATTERN_WEIGHT);
    }

    @Test
    public void testUpdateCategoryPattern() throws Exception {
        String content = "{\"name\":\"pattern-test-2-changed\",\"weight\":101,\"samples\":[]}";
        GLPatternRequestDto body = objectMapper.readValue(content, GLPatternRequestDto.class);
        ResponseDto<GLPatternDto> response = categoryPatternsController.updatePattern(
            CHILD1_TOVAR_ID, PATTERN2_ID, body
        );

        Assertions.assertThat(response.getData())
            .extracting(GLPatternDto::getId, GLPatternDto::getName, GLPatternDto::getWeight)
            .containsExactly(PATTERN2_ID, "pattern-test-2-changed", PATTERN2_NEW_WEIGHT);
    }

    @Test
    public void testPublishCategoryPattern() {
        ResponseDto<GLPatternDto> response = categoryPatternsController.publishPattern(CHILD1_TOVAR_ID, PATTERN2_ID);

        Assertions.assertThat(response.getData())
            .extracting(GLPatternDto::isPublished)
            .isEqualTo(true);
    }

    @Test
    public void testUnpublishCategoryPattern() {
        ResponseDto<GLPatternDto> response = categoryPatternsController.unpublishPattern(CHILD1_TOVAR_ID, PATTERN1_ID);

        Assertions.assertThat(response.getData())
            .extracting(GLPatternDto::isPublished)
            .isEqualTo(false);
    }

    @Test
    public void testRemoveCategoryPattern() {
        categoryPatternsController.removePattern(CHILD1_TOVAR_ID, PATTERN1_ID);

        Mockito.verify(patternService, Mockito.times(0))
            .disablePatternInheritance(Mockito.anyLong(), Mockito.any(), Mockito.anyLong());
        Mockito.verify(patternService, Mockito.times(1)).removePattern(Mockito.any());
    }

    @Test
    public void testRemoveInheritedCategoryPattern() {
        categoryPatternsController.removePattern(CHILD1_TOVAR_ID, INHERITED_PATTERN_ID);

        Mockito.verify(patternService, Mockito.times(1))
            .disablePatternInheritance(Mockito.anyLong(), Mockito.any(), Mockito.anyLong());
        Mockito.verify(patternService, Mockito.times(0)).removePattern(Mockito.any());
    }

    private static CategoryPatterns getCategoryPatterns() {
        CategoryPatterns categoryPatterns = new CategoryPatterns();

        GLPattern pattern1 = new GLPatternImpl();
        pattern1.setId(PATTERN1_ID);
        pattern1.setName(PATTERN1_NAME);
        pattern1.setPublished(true);

        GLPattern pattern2 = new GLPatternImpl();
        pattern2.setId(PATTERN2_ID);
        pattern2.setName(PATTERN2_NAME);
        pattern2.setPublished(false);

        GLPattern pattern3 = new GLPatternImpl();
        pattern3.setId(INHERITED_PATTERN_ID);
        pattern3.setName(INHERITED_PATTERN_NAME);
        pattern3.setPublished(false);

        categoryPatterns.getPatterns().add(pattern1);
        categoryPatterns.getPatterns().add(pattern2);
        categoryPatterns.getPatterns().add(new InheritedGLPattern(pattern3));

        return categoryPatterns;
    }

    @NotNull
    private static TovarCategoryNode getTovarCategoryNode(TovarCategory goodItem) {
        TovarCategoryNode node = new TovarCategoryNode(goodItem);
        node.getData().setVisual(true);
        node.getData().setNotUsed(false);
        node.getData().setPublished(true);
        return node;
    }

    @NotNull
    private static TovarTree getTovarTree() {
        TovarCategory rootTovar = new TovarCategory("Root", ROOT_HID, DEFAULT_HID);
        rootTovar.setTovarId(ROOT_TOVAR_ID);

        TovarCategory child1Tovar = new TovarCategory("Child", CHILD1_HID, ROOT_HID);
        child1Tovar.setTovarId(CHILD1_TOVAR_ID);

        TovarCategoryNode rootNode = getTovarCategoryNode(rootTovar);
        TovarCategoryNode child1Node = getTovarCategoryNode(child1Tovar);
        rootNode.addChild(child1Node);

        return new TovarTree(rootNode);
    }
}

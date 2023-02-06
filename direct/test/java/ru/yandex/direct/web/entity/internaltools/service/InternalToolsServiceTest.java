package ru.yandex.direct.web.entity.internaltools.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.web.multipart.MultipartFile;

import ru.yandex.direct.common.lettuce.LettuceConnectionProvider;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.internaltools.core.InternalToolProxy;
import ru.yandex.direct.internaltools.core.InternalToolsRegistry;
import ru.yandex.direct.internaltools.core.bootstrap.InternalToolsRegistryBootstrap;
import ru.yandex.direct.internaltools.core.container.InternalToolMassResult;
import ru.yandex.direct.internaltools.core.container.InternalToolResult;
import ru.yandex.direct.internaltools.core.enrich.InternalToolEnrichProcessorFactory;
import ru.yandex.direct.internaltools.core.enums.InternalToolAction;
import ru.yandex.direct.internaltools.core.enums.InternalToolCategory;
import ru.yandex.direct.internaltools.core.exception.InternalToolAccessDeniedException;
import ru.yandex.direct.internaltools.core.exception.InternalToolNotFoundException;
import ru.yandex.direct.internaltools.core.exception.InternalToolValidationException;
import ru.yandex.direct.internaltools.core.input.InternalToolInputType;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;
import ru.yandex.direct.web.entity.internaltools.model.InternalToolBasicDescription;
import ru.yandex.direct.web.entity.internaltools.model.InternalToolCategoryDescription;
import ru.yandex.direct.web.entity.internaltools.model.InternalToolExtendedDescription;
import ru.yandex.direct.web.entity.internaltools.model.InternalToolInputGroupRepresentation;
import ru.yandex.direct.web.entity.internaltools.model.InternalToolInputRepresentation;
import ru.yandex.direct.web.entity.internaltools.model.InternalToolsFileDescription;
import ru.yandex.direct.web.entity.internaltools.service.testtool.ControllerTestTool;
import ru.yandex.direct.web.entity.internaltools.service.testtool.TestToolResultItem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.web.entity.internaltools.service.InternalToolsService.MAX_FILE_LIFE_DURATION;

public class InternalToolsServiceTest {
    private static final String TEST_TOOL_LABEL = "_controller_test_tool";
    private static final long TEST_UID = 100L;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DirectWebAuthenticationSource authenticationSource;

    private LettuceConnectionProvider lettuce;
    private RedisAdvancedClusterCommands commandsLettuce;

    private InternalToolsService service;
    private FeatureService featureService;
    private User user;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        lettuce = mock(LettuceConnectionProvider.class);
        commandsLettuce = mock(RedisAdvancedClusterCommands.class);

        when(lettuce.call(anyString(), any()))
                .then(invocation -> {
                    Function cmd = (Function) invocation.getArgument(1);
                    return cmd.apply(commandsLettuce);
                });
        when(lettuce.callBinary(anyString(), any()))
                .then(invocation -> {
                    Function cmd = (Function) invocation.getArgument(1);
                    return cmd.apply(commandsLettuce);
                });

        user = new User()
                .withLogin("test_user")
                .withUid(TEST_UID);
        when(authenticationSource.getAuthentication().getOperator()).thenReturn(user);

        InternalToolsRegistry toolsRegistry =
                InternalToolsRegistryBootstrap.generateRegistry(Collections.singleton(new ControllerTestTool()),
                        new InternalToolEnrichProcessorFactory(Collections.emptyMap()), Collections.emptyList(),
                        featureService, 12);
        service = new InternalToolsService(toolsRegistry, authenticationSource, lettuce);
    }

    @Test
    public void testGetInternalToolsList() {
        user.setRole(RbacRole.MANAGER);

        List<InternalToolCategoryDescription> categories = service.getToolsCategories();

        InternalToolBasicDescription description = new InternalToolBasicDescription()
                .withName("Тестовый инструмент")
                .withDescription("Тестовый инструмент для проверки всего на свете")
                .withLabel(TEST_TOOL_LABEL);

        InternalToolCategoryDescription categoryDescription = new InternalToolCategoryDescription()
                .withName(InternalToolCategory.OTHER.getName())
                .withItems(Collections.singletonList(description));

        assertThat(categories)
                .is(matchedBy(beanDiffer(Collections.singletonList(categoryDescription))));
    }

    @Test
    public void testGetInternalToolsListEmpty() {
        user.setRole(RbacRole.SUPPORT);

        List<InternalToolCategoryDescription> categories = service.getToolsCategories();

        assertThat(categories)
                .isEmpty();
    }

    @Test
    public void testGetInternalToolDescription() {
        user.setRole(RbacRole.SUPER);

        InternalToolExtendedDescription testTool = service.getToolDescription(TEST_TOOL_LABEL);

        List<InternalToolInputGroupRepresentation> inputGroups = Collections.singletonList(
                new InternalToolInputGroupRepresentation()
                        .withName("").withInputs(Arrays.asList(
                        new InternalToolInputRepresentation()
                                .withName("cb")
                                .withLabel("Галочка")
                                .withDescription("")
                                .withDefaultValue(true)
                                .withInputType(InternalToolInputType.CHECKBOX.toString().toLowerCase())
                                .withArgs(Collections.emptyMap())
                                .withRequired(true),
                        new InternalToolInputRepresentation()
                                .withName("text")
                                .withLabel("Текст")
                                .withDescription("Описание")
                                .withInputType(InternalToolInputType.TEXT.toString().toLowerCase())
                                .withArgs(Collections.singletonMap("max_value_len", 20))
                                .withRequired(false),
                        new InternalToolInputRepresentation()
                                .withName("fileNotRequired")
                                .withLabel("Необязательный файл")
                                .withDescription("")
                                .withInputType(InternalToolInputType.FILE.toString().toLowerCase())
                                .withArgs(Collections.emptyMap())
                                .withRequired(false)
                        )
                ));
        InternalToolExtendedDescription description = new InternalToolExtendedDescription()
                .withAction(InternalToolAction.REFRESH.getName())
                .withDisclaimers(Collections.emptyList())
                .withMethod(HttpMethod.GET)
                .withInputGroups(inputGroups)
                .withName("Тестовый инструмент")
                .withLabel(TEST_TOOL_LABEL)
                .withDescription("Тестовый инструмент для проверки всего на свете");

        assertThat(testTool)
                .is(matchedBy(beanDiffer(description)));
    }

    @Test(expected = InternalToolAccessDeniedException.class)
    public void testGetInternalToolDescriptionError() {
        user.setRole(RbacRole.SUPERREADER);
        user.setDeveloper(true);

        service.getToolDescription(TEST_TOOL_LABEL);
    }

    @Test(expected = InternalToolNotFoundException.class)
    public void testGetInternalToolDescriptionNotFoundError() {
        user.setRole(RbacRole.SUPER);
        user.setDeveloper(true);

        service.getToolDescription("unknown_label");
    }

    @Test
    public void testReadInternalTool() {
        user.setRole(RbacRole.MANAGER);

        Map<String, Object> params = ImmutableMap.<String, Object>builder()
                .put("cb", false)
                .put("text", "some text")
                .build();
        InternalToolResult result = service.processReadingTool(TEST_TOOL_LABEL, params);

        assertThat(result.getMessage())
                .isEqualTo("Your params list is here");
        assertThat(result)
                .isInstanceOf(InternalToolMassResult.class);

        @SuppressWarnings("unchecked")
        InternalToolMassResult<TestToolResultItem> massResult = (InternalToolMassResult<TestToolResultItem>) result;

        List<TestToolResultItem> items = Arrays.asList(
                new TestToolResultItem().withKey("cb").withValue("false"),
                new TestToolResultItem().withKey("text").withValue("some text"),
                new TestToolResultItem().withKey("fileNotRequired").withValue("")
        );
        assertThat(massResult.getData())
                .is(matchedBy(beanDiffer(items)));

        verify(commandsLettuce, never()).get(anyString());
    }

    @Test
    public void testReadInternalToolWithFile() {
        user.setRole(RbacRole.MANAGER);

        Map<String, Object> params = ImmutableMap.<String, Object>builder()
                .put("cb", false)
                .put("text", "some text")
                .put("fileNotRequired", "KEY_FOR_FILE")
                .build();
        doReturn(String.valueOf(TEST_UID))
                .when(commandsLettuce).get(eq("KEY_FOR_FILE/owner"));
        doReturn("file contents".getBytes())
                .when(commandsLettuce).get(eq("KEY_FOR_FILE".getBytes()));

        InternalToolResult result = service.processReadingTool(TEST_TOOL_LABEL, params);

        assertThat(result.getMessage())
                .isEqualTo("Your params list is here");
        assertThat(result)
                .isInstanceOf(InternalToolMassResult.class);

        @SuppressWarnings("unchecked")
        InternalToolMassResult<TestToolResultItem> massResult = (InternalToolMassResult<TestToolResultItem>) result;

        List<String> bytes = new ArrayList<>();
        for (byte b : "file contents".getBytes()) {
            bytes.add(String.valueOf(b));
        }
        List<TestToolResultItem> items = Arrays.asList(
                new TestToolResultItem().withKey("cb").withValue("false"),
                new TestToolResultItem().withKey("text").withValue("some text"),
                new TestToolResultItem().withKey("fileNotRequired").withValue(String.join(";", bytes))
        );
        assertThat(massResult.getData())
                .is(matchedBy(beanDiffer(items)));
    }

    @Test(expected = InternalToolAccessDeniedException.class)
    public void testReadInternalToolWithFileAccessError() {
        user.setRole(RbacRole.MANAGER);

        Map<String, Object> params = ImmutableMap.<String, Object>builder()
                .put("cb", false)
                .put("text", "some text")
                .put("fileNotRequired", "KEY_FOR_FILE")
                .build();
        doReturn(String.valueOf(TEST_UID + 2L))
                .when(commandsLettuce).get(eq("KEY_FOR_FILE/owner"));

        service.processReadingTool(TEST_TOOL_LABEL, params);
    }

    @Test(expected = InternalToolAccessDeniedException.class)
    public void testReadInternalToolAccessError() {
        user.setRole(RbacRole.SUPPORT);

        service.processReadingTool(TEST_TOOL_LABEL, Collections.emptyMap());
    }

    @Test(expected = InternalToolValidationException.class)
    public void testReadInternalToolProcessingError() {
        user.setRole(RbacRole.MANAGER);

        service.processReadingTool(TEST_TOOL_LABEL, Collections.emptyMap());
    }

    @Test(expected = InternalToolAccessDeniedException.class)
    public void testWriteInternalToolAccessError() {
        user.setRole(RbacRole.SUPER);

        service.processWritingTool(TEST_TOOL_LABEL, Collections.emptyMap());
    }

    @Test
    public void testUploadFile() throws IOException {
        user.setRole(RbacRole.MANAGER);

        MultipartFile file = mock(MultipartFile.class);
        doReturn("file contents".getBytes())
                .when(file).getBytes();

        InternalToolsFileDescription description = service.saveFile(TEST_TOOL_LABEL, file);
        assertThat(description)
                .isNotNull();
        assertThat(description.getKey())
                .startsWith(String.format("ITF/%s", TEST_TOOL_LABEL));

        verify(commandsLettuce)
                .setex(eq(description.getKey() + "/owner"),
                        eq(MAX_FILE_LIFE_DURATION.getSeconds()),
                        eq(String.valueOf(TEST_UID)));
        verify(commandsLettuce)
                .setex(eq(description.getKey().getBytes()),
                        eq(MAX_FILE_LIFE_DURATION.getSeconds()),
                        eq("file contents".getBytes()));
    }

    @Test(expected = InternalToolAccessDeniedException.class)
    public void testUploadFileAccessError() {
        user.setRole(RbacRole.SUPPORT);

        service.saveFile(TEST_TOOL_LABEL, mock(MultipartFile.class));
    }

    @Test
    public void extractOrderedDescriptions_returnOrderedList() {
        List<InternalToolProxy> input = Arrays.asList(
                initToolProxyMock("name 2"),
                initToolProxyMock("name 3"),
                initToolProxyMock("name 1"));

        List<InternalToolBasicDescription> actual = InternalToolsService.extractOrderedDescriptions(input);

        assertThat(actual).extracting("name").isSorted();
    }

    private static InternalToolProxy initToolProxyMock(String name) {
        InternalToolProxy mock = mock(InternalToolProxy.class, String.format("Tool named \"%s\"", name));
        when(mock.getDescription())
                .thenReturn(name);
        when(mock.getName())
                .thenReturn(name);
        when(mock.getLabel())
                .thenReturn(name);
        return mock;
    }
}

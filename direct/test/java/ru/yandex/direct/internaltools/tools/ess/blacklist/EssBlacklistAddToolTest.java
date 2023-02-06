package ru.yandex.direct.internaltools.tools.ess.blacklist;

import java.time.Duration;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.essblacklist.EssLogicObjectsBlacklistRepository;
import ru.yandex.direct.core.entity.essblacklist.model.EssBlacklistItem;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.ess.client.EssClient;
import ru.yandex.direct.ess.client.repository.EssAdditionalObjectsRepository;
import ru.yandex.direct.ess.common.models.BaseEssConfig;
import ru.yandex.direct.ess.common.models.BaseLogicObject;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.core.container.InternalToolMassResult;
import ru.yandex.direct.validation.defect.StringDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestUsers.generateRandomLogin;
import static ru.yandex.direct.dbschema.ppcdict.tables.EssLogicObjectsBlacklist.ESS_LOGIC_OBJECTS_BLACKLIST;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@InternalToolsTest
@RunWith(SpringRunner.class)
public class EssBlacklistAddToolTest {
    public static final String TEST_ESS_PROCESSOR_NAME = "test_ess_processor";

    @Autowired
    private EssLogicObjectsBlacklistRepository essBlacklistRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private EssAdditionalObjectsRepository essAdditionalObjectsRepository;

    private EssBlacklistAddTool blacklistTool;
    private User user;

    @Before
    public void setUp() {
        user = new User().withDomainLogin(generateRandomLogin());
        blacklistTool = new EssBlacklistAddTool(essBlacklistRepository,
                new EssClient(essAdditionalObjectsRepository, this.getClass().getPackageName()));
    }

    @After
    public void tearDown() {
        cleanupBlacklist();
    }

    private void cleanupBlacklist() {
        dslContextProvider.ppcdict().deleteQuery(ESS_LOGIC_OBJECTS_BLACKLIST).execute();
    }

    @Test
    public void startToolWhenBlacklistEmpty() {
        cleanupBlacklist();

        InternalToolMassResult<EssBlacklistItem> result = blacklistTool.processWithoutInput();

        assertThat(result, nullValue());
    }

    @Test
    public void startTool() {
        var filterSpec = "{\"bar\": \"baz\"}";
        essBlacklistRepository.addItems(TEST_ESS_PROCESSOR_NAME, Set.of(filterSpec), user.getDomainLogin());

        InternalToolMassResult<EssBlacklistItem> result = blacklistTool.processWithoutInput();

        assertThat(result.getData(), hasSize(1));
        assertThat(result.getData(), contains(
                essBlacklistItemMatcher(TEST_ESS_PROCESSOR_NAME, user.getDomainLogin(), filterSpec)
        ));
    }

    @Test
    public void addLogicObjectsToBlacklist() {
        var filter1 = "{\"foo\": 42}";
        var filter2 = "{\"bar\": 100500}";
        var input = new EssBlacklistAddParameters()
                .withEssProcessName(TEST_ESS_PROCESSOR_NAME)
                .withFilters(filter1 + ", " + filter2);
        input.setOperator(user);

        InternalToolMassResult<EssBlacklistItem> result = blacklistTool.process(input);

        assertThat(result.getData(), containsInAnyOrder(
                essBlacklistItemMatcher(TEST_ESS_PROCESSOR_NAME, user.getDomainLogin(), filter1),
                essBlacklistItemMatcher(TEST_ESS_PROCESSOR_NAME, user.getDomainLogin(), filter2)
        ));
        assertThat(result.getData(), hasSize(2));
    }

    @Test
    public void validation_emptyFilters() {
        var filters = "";
        var input = new EssBlacklistAddParameters()
                .withEssProcessName(TEST_ESS_PROCESSOR_NAME)
                .withFilters(filters);
        input.setOperator(user);

        ValidationResult<EssBlacklistAddParameters, Defect> result = blacklistTool.validate(input);

        assertThat(result, hasDefectDefinitionWith(validationError(
                path(field(EssBlacklistAddParameters.FILTERS)),
                StringDefects.notEmptyString()
        )));
    }

    private Matcher<EssBlacklistItem> essBlacklistItemMatcher(String logicProcessName, String domainLogin,
                                                              String filterSpec) {
        var expectedItem = new EssBlacklistItem()
                .withAuthorLogin(domainLogin)
                .withLogicProcessName(logicProcessName)
                .withFilterSpec(filterSpec);
        return beanDiffer(expectedItem).useCompareStrategy(onlyExpectedFields());
    }

    public static class TestConfig extends BaseEssConfig {
        @Override
        public String getLogicProcessName() {
            return TEST_ESS_PROCESSOR_NAME;
        }

        @Override
        public String getTopic() {
            return "test_topic";
        }

        @Override
        public Class<? extends BaseLogicObject> getLogicObject() {
            return EssBlacklistAddToolTest.TestObject.class;
        }

        @Override
        public Duration getCriticalEssProcessTime() {
            return null;
        }

        @Override
        public Duration getTimeToReadThreshold() {
            return null;
        }

        @Override
        public int getRowsThreshold() {
            return 0;
        }

        @Override
        public boolean processReshardingEvents() {
            // выставлено явно при замене умолчания в базовом классе: DIRECT-171006
            // необязательно означает, что для этого процесса нужно обрабатывать события от решардинга, просто настройку добавили позже: DIRECT-122901
            return true;
        }
    }

    @SuppressWarnings("unused")
    public static class TestObject extends BaseLogicObject {
        @JsonProperty("id")
        private Long id;

        @JsonCreator
        TestObject(@JsonProperty(value = "id", required = true) long id) {
            this.id = id;
        }
    }
}

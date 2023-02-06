package ru.yandex.direct.internaltools.tools.ess.blacklist;

import java.util.List;
import java.util.Set;

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
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.core.container.InternalToolMassResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestUsers.generateRandomLogin;
import static ru.yandex.direct.dbschema.ppcdict.tables.EssLogicObjectsBlacklist.ESS_LOGIC_OBJECTS_BLACKLIST;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@InternalToolsTest
@RunWith(SpringRunner.class)
public class EssBlacklistRemoveToolTest {
    public static final String ESS_PROCESSS_NAME = "1";

    @Autowired
    private EssLogicObjectsBlacklistRepository essBlacklistRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private EssBlacklistRemoveTool blacklistTool;

    private User user;

    @Before
    public void setUp() {
        user = new User().withDomainLogin(generateRandomLogin());
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
        essBlacklistRepository.addItems(ESS_PROCESSS_NAME, Set.of(filterSpec), user.getDomainLogin());

        InternalToolMassResult<EssBlacklistItem> result = blacklistTool.processWithoutInput();

        assertThat(result.getData(), hasSize(1));
        assertThat(result.getData(), contains(
                essBlacklistItemMatcher(ESS_PROCESSS_NAME, user.getDomainLogin(), filterSpec)
        ));
    }

    @Test
    public void removeLogicObjectsFromBlacklist() {
        var filter1 = "{\"foo\": 42}";
        var filter2 = "{\"bar\": 100500}";
        essBlacklistRepository.addItems(ESS_PROCESSS_NAME, Set.of(filter1, filter2), user.getDomainLogin());
        List<Long> ids = mapList(essBlacklistRepository.getAll(), EssBlacklistItem::getId);

        EssBlacklistRemoveParameters input = new EssBlacklistRemoveParameters()
                .withEssProcessName(ESS_PROCESSS_NAME)
                .withIds(String.join(",", mapList(ids, Object::toString)));
        input.setOperator(user);

        InternalToolMassResult<EssBlacklistItem> result = blacklistTool.process(input);

        assertThat(result.getData(), empty());
    }

    private Matcher<EssBlacklistItem> essBlacklistItemMatcher(String logicProcessName, String domainLogin,
                                                              String filterSpec) {
        var expectedItem = new EssBlacklistItem()
                .withAuthorLogin(domainLogin)
                .withLogicProcessName(logicProcessName)
                .withFilterSpec(filterSpec);
        return beanDiffer(expectedItem).useCompareStrategy(onlyExpectedFields());
    }

}

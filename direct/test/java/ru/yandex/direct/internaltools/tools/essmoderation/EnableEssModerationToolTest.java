package ru.yandex.direct.internaltools.tools.essmoderation;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.moderation.model.ObjectWithEnabledEssModeration;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.core.container.InternalToolMassResult;
import ru.yandex.direct.internaltools.tools.essmoderation.model.EnableEssModerationObjectType;
import ru.yandex.direct.internaltools.tools.essmoderation.model.EnableEssModerationParameter;
import ru.yandex.direct.utils.JsonUtils;

import static java.time.LocalDateTime.now;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.common.db.PpcPropertyNames.LIST_OF_OBJECTS_WITH_ENABLED_ESS_MODERATION;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;

@InternalToolsTest
@RunWith(SpringRunner.class)
public class EnableEssModerationToolTest {

    private static final CompareStrategy COMPARE_STRATEGY = DefaultCompareStrategies.onlyExpectedFields()
            .forFields(newPath(ObjectWithEnabledEssModeration.LAST_CHANGE.name())).useMatcher(approximatelyNow());

    @Autowired
    private EnableEssModerationTool enableEssModerationTool;

    @Autowired
    private PpcPropertiesSupport ppcProperties;

    @Before
    public void clear() {
        ppcProperties.get(LIST_OF_OBJECTS_WITH_ENABLED_ESS_MODERATION).remove();
    }

    @Test
    public void addCampaign() {
        EnableEssModerationParameter params = new EnableEssModerationParameter()
                .withToRemove(false)
                .withObjectId(101L)
                .withObjectType(EnableEssModerationObjectType.CAMPAIGN);
        InternalToolMassResult<ObjectWithEnabledEssModeration> processResult = enableEssModerationTool.process(params);

        assertThat(processResult.getData(), hasSize(1));
        assertThat(processResult.getData().get(0), beanDiffer(new ObjectWithEnabledEssModeration()
                .withCampaignId(101L))
                .useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void addBanner() {
        EnableEssModerationParameter params = new EnableEssModerationParameter()
                .withToRemove(false)
                .withObjectId(101L)
                .withObjectType(EnableEssModerationObjectType.BANNER);
        InternalToolMassResult<ObjectWithEnabledEssModeration> processResult = enableEssModerationTool.process(params);

        assertThat(processResult.getData(), hasSize(1));
        assertThat(processResult.getData().get(0), beanDiffer(new ObjectWithEnabledEssModeration()
                .withBannerId(101L))
                .useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void deleteCampaign() {
        ppcProperties.get(LIST_OF_OBJECTS_WITH_ENABLED_ESS_MODERATION)
                .set(JsonUtils.toJson(List.of(
                        new ObjectWithEnabledEssModeration()
                                .withCampaignId(101L)
                                .withLastChange(now())
                )));

        EnableEssModerationParameter params = new EnableEssModerationParameter()
                .withToRemove(true)
                .withObjectId(101L)
                .withObjectType(EnableEssModerationObjectType.CAMPAIGN);
        InternalToolMassResult<ObjectWithEnabledEssModeration> processResult = enableEssModerationTool.process(params);

        assertThat(processResult.getData(), empty());
    }

    @Test
    public void deleteBanner() {
        ppcProperties.get(LIST_OF_OBJECTS_WITH_ENABLED_ESS_MODERATION)
                .set(JsonUtils.toJson(List.of(
                        new ObjectWithEnabledEssModeration()
                                .withBannerId(101L)
                                .withLastChange(now())
                )));

        EnableEssModerationParameter params = new EnableEssModerationParameter()
                .withToRemove(true)
                .withObjectId(101L)
                .withObjectType(EnableEssModerationObjectType.BANNER);
        InternalToolMassResult<ObjectWithEnabledEssModeration> processResult = enableEssModerationTool.process(params);

        assertThat(processResult.getData(), empty());
    }

    @Test
    public void noDuplicates() {
        ppcProperties.get(LIST_OF_OBJECTS_WITH_ENABLED_ESS_MODERATION)
                .set(JsonUtils.toJson(List.of(
                        new ObjectWithEnabledEssModeration()
                                .withCampaignId(101L)
                                .withLastChange(now())
                )));

        EnableEssModerationParameter params = new EnableEssModerationParameter()
                .withToRemove(false)
                .withObjectId(101L)
                .withObjectType(EnableEssModerationObjectType.CAMPAIGN);
        InternalToolMassResult<ObjectWithEnabledEssModeration> processResult = enableEssModerationTool.process(params);

        assertThat(processResult.getData(), hasSize(1));
        assertThat(processResult.getData().get(0), beanDiffer(new ObjectWithEnabledEssModeration()
                .withCampaignId(101L))
                .useCompareStrategy(COMPARE_STRATEGY));
    }
}

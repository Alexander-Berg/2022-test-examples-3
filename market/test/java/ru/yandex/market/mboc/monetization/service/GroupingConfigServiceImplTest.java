package ru.yandex.market.mboc.monetization.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.mboc.app.controller.web.DisplayGroupingConfig;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.ConfigParameter;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.ConfigValidationError;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.GroupingConfig;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.monetization.config.MonetizationJooqConfig;
import ru.yandex.market.mboc.monetization.repository.ConfigParameterRepository;
import ru.yandex.market.mboc.monetization.repository.ConfigValidationErrorRepository;
import ru.yandex.market.mboc.monetization.repository.GroupingConfigRepository;
import ru.yandex.market.mboc.monetization.repository.filters.GroupingConfigFilter;

/**
 * @author eremeevvo
 * @since 25.10.2019
 */
@ContextConfiguration(classes = {MonetizationJooqConfig.class})
public class GroupingConfigServiceImplTest extends BaseDbTestClass {

    private static final int SEED = 254658;

    private GroupingConfigService groupingConfigService;

    private EnhancedRandom random;

    private String[] ignoreFields = new String[]{"modifiedAt", "createdAt"};

    @Autowired
    private GroupingConfigRepository groupingConfigRepository;

    @Autowired
    private ConfigParameterRepository configParameterRepository;

    @Autowired
    private ConfigValidationErrorRepository configValidationErrorRepository;

    @Before
    public void setUp() {
        random = new EnhancedRandomBuilder().seed(SEED).build();

        groupingConfigService = new GroupingConfigServiceImpl(
            groupingConfigRepository,
            configParameterRepository,
            configValidationErrorRepository
        );
    }

    @Test
    @SuppressWarnings("checkstyle:magicNumber")
    public void testFindDisplayGroupingConfigWithValidationErrors() {
        GroupingConfig config = random.nextObject(GroupingConfig.class);

        ConfigValidationError configValidationError = new ConfigValidationError(
            1L, config.getId(), "message", LocalDateTime.of(2019, 12, 5, 15, 0),
            "createdLogin", LocalDateTime.of(2019, 12, 5, 16, 0),
            "modifiedLogin", false
        );

        groupingConfigRepository.save(config);

        configValidationErrorRepository.save(configValidationError);

        List<DisplayGroupingConfig> found = groupingConfigService.find(
            new GroupingConfigFilter().setCategoryIds(Collections.singletonList(config.getCategoryId()))
        );

        Assertions.assertThat(found.size()).isEqualTo(1);
        Assertions.assertThat(found.get(0).getValidationErrors().size()).isEqualTo(1);
        Assertions.assertThat(
            found.get(0).getValidationErrors().get(0)
        ).isEqualToIgnoringGivenFields(configValidationError, "createdAt", "modifiedAt");
    }

    @Test
    public void testSaveAndFind() {
        GroupingConfig config = random.nextObject(GroupingConfig.class);
        ConfigParameter param = random.nextObject(ConfigParameter.class);

        DisplayGroupingConfig displayGroupingConfig = new DisplayGroupingConfig()
            .setGroupingConfig(config)
            .setConfigParameters(Collections.singletonList(param));

        DisplayGroupingConfig saved = groupingConfigService.save(displayGroupingConfig);
        List<DisplayGroupingConfig> found = groupingConfigService.find(new GroupingConfigFilter()
            .setCategoryIds(Collections.singletonList(
                saved.getGroupingConfig().getCategoryId())));

        Assertions.assertThat(saved.getGroupingConfig())
            .isEqualToIgnoringGivenFields(config, ignoreFields);

        Assertions.assertThat(saved.getConfigParameters().get(0))
            .isEqualToIgnoringGivenFields(param, ignoreFields);

        Assertions.assertThat(found.size()).isEqualTo(1);
        Assertions.assertThat(found.get(0).getGroupingConfig())
            .isEqualToIgnoringGivenFields(saved.getGroupingConfig());

        Assertions.assertThat(found.get(0).getConfigParameters().size()).isEqualTo(1);
        Assertions.assertThat(found.get(0).getConfigParameters().get(0))
            .isEqualToIgnoringGivenFields(saved.getConfigParameters().get(0));
    }

    @Test
    public void testModify() {
        GroupingConfig config = random.nextObject(GroupingConfig.class).setComment("test");
        ConfigParameter param = random.nextObject(ConfigParameter.class);

        DisplayGroupingConfig displayGroupingConfig = new DisplayGroupingConfig()
            .setGroupingConfig(config)
            .setConfigParameters(Collections.singletonList(param));

        DisplayGroupingConfig saved = groupingConfigService.save(displayGroupingConfig);

        saved.getGroupingConfig().setComment("modified");

        DisplayGroupingConfig modified = groupingConfigService.save(saved);

        Assertions.assertThat(saved.getGroupingConfig())
            .isEqualToIgnoringGivenFields(modified.getGroupingConfig(), ignoreFields);

        Assertions.assertThat(saved.getConfigParameters().get(0))
            .isEqualToIgnoringGivenFields(modified.getConfigParameters().get(0), ignoreFields);
    }

    @Test
    public void testModifyAddParam() {
        GroupingConfig config = random.nextObject(GroupingConfig.class);
        ConfigParameter param1 = random.nextObject(ConfigParameter.class);
        ConfigParameter param2 = random.nextObject(ConfigParameter.class);

        DisplayGroupingConfig displayGroupingConfig = new DisplayGroupingConfig()
            .setGroupingConfig(config)
            .setConfigParameters(Collections.singletonList(param1));

        DisplayGroupingConfig saved = groupingConfigService.save(displayGroupingConfig);

        final List<ConfigParameter> configParameters = new ArrayList<>(saved.getConfigParameters());
        configParameters.add(param2);
        saved = new DisplayGroupingConfig(saved).setConfigParameters(configParameters);

        DisplayGroupingConfig modified = groupingConfigService.save(saved);

        Assertions.assertThat(saved.getGroupingConfig())
            .isEqualToIgnoringGivenFields(modified.getGroupingConfig(), ignoreFields);

        Assertions.assertThat(saved.getConfigParameters().size()).isEqualTo(2);
        Assertions.assertThat(saved.getConfigParameters())
            .usingElementComparatorIgnoringFields(ignoreFields)
            .containsExactlyInAnyOrder(param1, param2);
    }

    @Test
    public void testModifyRemoveParam() {
        GroupingConfig config = random.nextObject(GroupingConfig.class);
        ConfigParameter param1 = random.nextObject(ConfigParameter.class);
        ConfigParameter param2 = random.nextObject(ConfigParameter.class);

        DisplayGroupingConfig displayGroupingConfig = new DisplayGroupingConfig()
            .setGroupingConfig(config)
            .setConfigParameters(Arrays.asList(param1, param2));

        DisplayGroupingConfig saved = groupingConfigService.save(displayGroupingConfig);

        final List<ConfigParameter> configParameters = new ArrayList<>(saved.getConfigParameters());
        configParameters.remove(0);
        saved = new DisplayGroupingConfig(saved).setConfigParameters(configParameters);

        DisplayGroupingConfig modified = groupingConfigService.save(saved);

        List<DisplayGroupingConfig> found = groupingConfigService.find(new GroupingConfigFilter()
            .setCategoryIds(Collections.singletonList(
                saved.getGroupingConfig().getCategoryId())));

        Assertions.assertThat(saved.getGroupingConfig())
            .isEqualToIgnoringGivenFields(modified.getGroupingConfig(), ignoreFields);

        Assertions.assertThat(saved.getConfigParameters().size()).isEqualTo(1);
        Assertions.assertThat(found.get(0).getConfigParameters().size()).isEqualTo(1);
    }

    @Test
    public void testDelete() {
        GroupingConfig config = random.nextObject(GroupingConfig.class);
        ConfigParameter param1 = random.nextObject(ConfigParameter.class);
        ConfigParameter param2 = random.nextObject(ConfigParameter.class);

        DisplayGroupingConfig displayGroupingConfig = new DisplayGroupingConfig()
            .setGroupingConfig(config)
            .setConfigParameters(Arrays.asList(param1, param2));

        DisplayGroupingConfig saved = groupingConfigService.save(displayGroupingConfig);

        groupingConfigService.delete(saved.getGroupingConfig().getId());

        List<DisplayGroupingConfig> found = groupingConfigService.find(new GroupingConfigFilter()
            .setCategoryIds(Collections.singletonList(
                saved.getGroupingConfig().getCategoryId())));

        Assertions.assertThat(found).isEmpty();
    }
}

package ru.yandex.direct.core.entity.campaign.repository;

import java.util.Collection;
import java.util.EnumSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.campaign.model.CampaignOpts;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.core.entity.campaign.model.CampaignOpts.ENABLE_CPC_HOLD;
import static ru.yandex.direct.core.entity.campaign.model.CampaignOpts.HAS_TURBO_APP;
import static ru.yandex.direct.core.entity.campaign.model.CampaignOpts.HAS_TURBO_SMARTS;
import static ru.yandex.direct.core.entity.campaign.model.CampaignOpts.HIDE_PERMALINK_INFO;
import static ru.yandex.direct.core.entity.campaign.model.CampaignOpts.IS_SIMPLIFIED_STRATEGY_VIEW_ENABLED;
import static ru.yandex.direct.core.entity.campaign.model.CampaignOpts.IS_TOUCH;
import static ru.yandex.direct.core.entity.campaign.model.CampaignOpts.NO_EXTENDED_GEOTARGETING;
import static ru.yandex.direct.core.entity.campaign.model.CampaignOpts.NO_TITLE_SUBSTITUTE;
import static ru.yandex.direct.core.entity.campaign.model.CampaignOpts.USE_CURRENT_REGION;
import static ru.yandex.direct.core.entity.campaign.model.CampaignOpts.USE_REGULAR_REGION;

/**
 * Тест првоеряет успешность преобразования {@link EnumSet}'а в строковое представление для сохранения в {@code SET}
 */
@RunWith(Parameterized.class)
public class CampaignMappingOptsTest {

    @Parameterized.Parameters(name = "model format: {0}, db format: {1}")
    public static Collection<Object[]> params() {
        return asList(new Object[][]{
                {EnumSet.of(ENABLE_CPC_HOLD), "enable_cpc_hold"},
                {EnumSet.of(ENABLE_CPC_HOLD, ENABLE_CPC_HOLD), "enable_cpc_hold"},
                {EnumSet.of(ENABLE_CPC_HOLD, NO_TITLE_SUBSTITUTE),
                        "no_title_substitute,enable_cpc_hold"},
                {EnumSet.of(NO_EXTENDED_GEOTARGETING, ENABLE_CPC_HOLD, NO_TITLE_SUBSTITUTE),
                        "no_title_substitute,enable_cpc_hold,no_extended_geotargeting"},
                {EnumSet.of(HIDE_PERMALINK_INFO, NO_EXTENDED_GEOTARGETING, ENABLE_CPC_HOLD, NO_TITLE_SUBSTITUTE),
                        "no_title_substitute,enable_cpc_hold,no_extended_geotargeting,hide_permalink_info"},
                {EnumSet.of(HAS_TURBO_SMARTS), "has_turbo_smarts"},
                {EnumSet.noneOf(CampaignOpts.class), ""},
                {EnumSet.of(IS_TOUCH), "is_touch"},
                {EnumSet.of(HAS_TURBO_APP), "has_turbo_app"},
                {EnumSet.of(USE_CURRENT_REGION), "use_current_region"},
                {EnumSet.of(USE_REGULAR_REGION), "use_regular_region"},
                {EnumSet.of(IS_SIMPLIFIED_STRATEGY_VIEW_ENABLED), "is_simplified_strategy_view_enabled"},
                {null, null}}
        );
    }

    private EnumSet<CampaignOpts> modelOpts;
    private String dbOpts;

    public CampaignMappingOptsTest(EnumSet<CampaignOpts> modelOpts, String dbOpts) {
        this.modelOpts = modelOpts;
        this.dbOpts = dbOpts;
    }

    @Test
    public void testToDbFormat() {
        assertThat("Конвертация типа в формат базы должна быть однозначной",
                CampaignMappings.optsToDb(modelOpts),
                is(dbOpts));
    }

    @Test
    public void testFromDbFormat() {
        assertThat("Конвертация типа в формат модели должна быть однозначной",
                CampaignMappings.optsFromDb(dbOpts),
                is(modelOpts));
    }
}

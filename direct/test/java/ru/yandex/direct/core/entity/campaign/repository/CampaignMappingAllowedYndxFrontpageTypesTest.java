package ru.yandex.direct.core.entity.campaign.repository;

import java.util.Collection;
import java.util.EnumSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


/**
 * Тест проверяет успешность преобразования {@link EnumSet}'а в строковое представление для сохранения в {@code SET}
 */
@RunWith(Parameterized.class)
public class CampaignMappingAllowedYndxFrontpageTypesTest {

    @Parameterized.Parameters(name = "model format: {0}, db format: {1}")
    public static Collection<Object[]> params() {
        return asList(new Object[][]{
                {EnumSet.of(FrontpageCampaignShowType.FRONTPAGE), "frontpage"},
                {EnumSet.of(FrontpageCampaignShowType.FRONTPAGE_MOBILE), "frontpage_mobile"},
                {EnumSet.of(FrontpageCampaignShowType.FRONTPAGE,
                        FrontpageCampaignShowType.FRONTPAGE_MOBILE), "frontpage,frontpage_mobile"},
                {EnumSet.noneOf(FrontpageCampaignShowType.class), ""}}
        );
    }

    private EnumSet<FrontpageCampaignShowType> modelOpts;
    private String dbOpts;

    public CampaignMappingAllowedYndxFrontpageTypesTest(EnumSet<FrontpageCampaignShowType> modelOpts, String dbOpts) {
        this.modelOpts = modelOpts;
        this.dbOpts = dbOpts;
    }

    @Test
    public void testToDbFormat() {
        assertThat("Конвертация типа в формат базы должна быть однозначной",
                CampaignMappings.allowedYndxFrontpageTypesToDb(modelOpts),
                is(dbOpts));
    }

    @Test
    public void testFromDbFormat() {
        assertThat("Конвертация типа в формат модели должна быть однозначной",
                CampaignMappings.allowedYndxFrontpageTypesFromDb(dbOpts),
                is(modelOpts));
    }
}

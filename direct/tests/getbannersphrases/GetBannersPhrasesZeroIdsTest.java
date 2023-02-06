package ru.yandex.autotests.directintapi.tests.getbannersphrases;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.request.GetBannersPhrasesRequest;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.darkside.steps.GetBannersPhrasesSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;

/**
 * @author xy6er
 * https://st.yandex-team.ru/DIRECT-35007
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.TRUNK_BUG + "DIRECT-35007")
public class GetBannersPhrasesZeroIdsTest {
    DarkSideSteps darkSideSteps = new DarkSideSteps();

    @Test
    public void getBannersPhrasesZeroIdsTest() {
        Long invalidCid = 0L;
        darkSideSteps.getBannersPhrasesSteps().executeMethodExpectError(
                GetBannersPhrasesSteps.GET_BANNERS_PHRASES,
                new GetBannersPhrasesRequest()
                        .withBanner(invalidCid, 0L),
                400,
                "\"code\":\"BAD_PARAM\""
        );
    }

}

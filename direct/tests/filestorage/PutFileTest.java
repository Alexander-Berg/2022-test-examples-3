package ru.yandex.autotests.directintapi.tests.filestorage;

import ru.yandex.qatools.Tag;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.allure.annotations.Features;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;


/**
 * User: omaz
 * Date: 15.08.13
 */
@Aqua.Test(title = "PutFile")
@Tag(TagDictionary.RELEASE)
@Features(FeatureNames.FILESTORAGE)
public class PutFileTest {

    protected LogSteps log = LogSteps.getLogger(this.getClass());

    private static DarkSideSteps darkSideSteps = new DarkSideSteps();

    @Test
    public void putFileTest() {
        String fileID = darkSideSteps.getFileStorageSteps().putDefaultFile(123L, 123L);
        assertThat("вернулся корректный идентификатор файла", fileID, not(isEmptyOrNullString()));
    }

}

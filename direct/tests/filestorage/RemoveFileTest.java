package ru.yandex.autotests.directintapi.tests.filestorage;

import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.allure.annotations.Features;


/**
 * User: omaz
 * Date: 16.08.13
 */
//@Aqua.Test(title = "RemoveFile")
@Features(FeatureNames.FILESTORAGE)
public class RemoveFileTest {
    protected LogSteps log = LogSteps.getLogger(this.getClass());
    private static DarkSideSteps darkSideSteps = new DarkSideSteps();


    //@Test
    public void removeFileTest() {
        String response = darkSideSteps.getFileStorageSteps().removeFile("2885", "documents");
        log.info("Ответ сервера: " + response);
    }

    //@Test
    public void removeFileNonexistIdTest() {
        String response = darkSideSteps.getFileStorageSteps().removeFile("1111", "documents");
        log.info("Ответ сервера: " + response);
    }

}

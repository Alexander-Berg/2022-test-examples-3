package ru.yandex.autotests.directintapi.tests.filestorage;

import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.allure.annotations.Features;


/**
 * User: omaz
 * Date: 15.08.13
 */
//@Aqua.Test(title = "GetFile")
@Features(FeatureNames.FILESTORAGE)
public class GetFileTest {

    private static DarkSideSteps darkSideSteps = new DarkSideSteps();
    protected LogSteps log = LogSteps.getLogger(this.getClass());


    //@Test
    public void getFileTest() {
        String response = darkSideSteps.getFileStorageSteps().getFile("2885", "documents");
        log.info("Содержимое полученного файла: " + response);

    }

    //@Test
    public void getFileNonexistIdTest() {
        String response = darkSideSteps.getFileStorageSteps().getFile("1", "documents");
        log.info("Содержимое полученного файла: " + response);
    }
}

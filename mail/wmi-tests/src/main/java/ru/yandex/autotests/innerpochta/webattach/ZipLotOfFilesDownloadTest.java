package ru.yandex.autotests.innerpochta.webattach;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.innerpochta.webattach.ZipDownloadTest.NAME;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.downloadFile;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

/**
 * Created with IntelliJ IDEA.
 * User: lanwen
 * Date: 29.01.13
 * Time: 20:34
 */
@Aqua.Test
@Title("Проверка скачивания зипом большого количества файлов")
@Description("[DARIA-23443]")
@Features(MyFeatures.WEBATTACH)
@Stories(MyStories.ATTACH)
@Credentials(loginGroup = "RetrieverZipLotOfFilesDownloadTest")
public class ZipLotOfFilesDownloadTest extends BaseWebattachTest {
    private String mid;

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Before
    public void prepare() throws Exception {
        mid = sendWith(authClient).viaProd()
            .addAttaches(generateAttas(300))
            .send()
            .waitDeliver()
            .getMid();
    }

    @Test
    public void downloadXSL() throws Exception {
        String url = urlOfAllAttachesZipArchive(mid, NAME);
        logger.info("Урл для скачивания: " + url);

        File zipFile = downloadFile(url + "&archive=zip", NAME, authClient.authHC());

        ZipFile zip = new ZipFile(zipFile, "UTF-8");
        List<ZipEntry> enries = getFiles(zip);

        assertThat(enries, hasSize(300));
    }

    private File[] generateAttas(int cnt) throws Exception {
        List<File> files = new ArrayList<>();
        for (int i = 0; i < cnt; i++) {

            String filename = RandomStringUtils.random(30, "sйцукенгшщзхэждлорпавыфячсмитьбю. qwertyuiopasdfghjklzx1");
            File file = Util.generateRandomShortFile(filename, 64);
            file.deleteOnExit();
            files.add(file);
        }

        return files.toArray(new File[files.size()]);
    }

    private List<ZipEntry> getFiles(ZipFile zip) {
        List<ZipEntry> entryList = new ArrayList<>();
        Enumeration entries = zip.getEntries();
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry) entries.nextElement();
            logger.info("File name: " + zipEntry.getName());
            entryList.add(zipEntry);
        }

        return entryList;
    }
}

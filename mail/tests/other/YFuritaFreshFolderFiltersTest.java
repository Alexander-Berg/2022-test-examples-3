package ru.yandex.autotests.innerpochta.yfurita.tests.other;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.util.rules.RestAssuredLogger;
import ru.yandex.autotests.innerpochta.yfurita.util.Condition;
import ru.yandex.autotests.innerpochta.yfurita.util.FilterUser;
import ru.yandex.autotests.innerpochta.yfurita.util.YFuritaPreviewResponse;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.innerpochta.yfurita.util.FuritaConsts.PG_FOLDER_DRAFT;
import static ru.yandex.autotests.innerpochta.yfurita.util.FilterSettings.CLIKER_MOVE;
import static ru.yandex.autotests.innerpochta.yfurita.util.FilterSettings.FiltersParams.*;
import static ru.yandex.autotests.innerpochta.yfurita.util.YfuritaProperties.chooseUser;

/**
 * User: stassiak
 * Date: 01.02.12
 * Time: 11:59
 */
@Aqua.Test(title = "Тестирование создания и работы фильтров на раскладку по папкам",
        description = "Тестирование создания и работы фильтров на папки: preview и apply запросы")
@RunWith(value = Parameterized.class)
@Feature("Yfurita.Base")
public class YFuritaFreshFolderFiltersTest {
    private static final long INDEXING_TIMEOUT = 15000;
    private static final User TEST_USER = chooseUser(new User("yfurita.fresh.folder.filters@ya.ru", "testqa"),
            new User("mxtest-15@yandex-team.ru", "OqKEt7R9sr1v"),
            new User("robbitter-6967771629@ya.ru", "simple123456"));
    private static FilterUser fUser;
    private String filterId;
    private HashMap<String, String> params = new HashMap<String, String>();

    @Parameterized.Parameter(0)
    public Folder targetFolder;
    @Parameterized.Parameter(1)
    public Condition condition;
    @Parameterized.Parameter(2)
    public String folderSubj;

    @Rule
    public LogConfigRule logRule = new LogConfigRule();
    @ClassRule
    public static RestAssuredLogger raLog = new RestAssuredLogger();

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        Collection<Object[]> data = new LinkedList<Object[]>();
        fUser = new FilterUser(TEST_USER);
        Folder redFolder = new Folder("red", fUser.createFolder("red"));
        Folder blackFolder = new Folder("blackFolder123|black", fUser.createFolder("blackFolder123|black"));
        String draftsFolderName = PG_FOLDER_DRAFT;
        Folder draftsFolder = new Folder(draftsFolderName, fUser.createFolder(draftsFolderName));

        data.add(new Object[]{blackFolder, new Condition("0", "subject", "1", "crot1"), "crot1"});
        data.add(new Object[]{blackFolder, new Condition("0", "subject", "2", "crot"), "crot2"});
        data.add(new Object[]{redFolder, new Condition("1", "subject", "3", "dog"), "dogcat"});

        data.add(new Object[]{blackFolder, new Condition("1", "subject", "4", "dog"), "do2gcat"});
        data.add(new Object[]{draftsFolder, new Condition("0", "subject", "4", "dog"), "do2gcat"});
        data.add(new Object[]{draftsFolder, new Condition("0", "from", "4", "dog"), "1223311233322"});
        return data;
    }

    @Before
    public void sendMsgs() throws Exception {
        fUser.removeAllFilters();
        fUser.clearAll();
        params.put(NAME.getName(), String.format("%s_%s_%s_%s",
                condition.field1, condition.field2, condition.field3, randomAlphanumeric(20)));
        params.put(LOGIC.getName(), condition.logic);
        params.put(FIELD1.getName(), condition.field1);
        params.put(FIELD2.getName(), condition.field2);
        params.put(FIELD3.getName(), condition.field3);
        params.put(CLICKER.getName(), CLIKER_MOVE);
        params.put(MOVE_FOLDER.getName(), targetFolder.getFid());
        filterId = fUser.createFilter(params);

        TestMessage msg = new TestMessage();
        msg.setRecipient(TEST_USER.getLogin());
        msg.setSubject(folderSubj);
        msg.setFrom("devnull@yandex.ru");
        msg.setText("Message for " + folderSubj);
        msg.saveChanges();

        fUser.disableFilter(filterId);
        fUser.sendMessageWithFilterOff(msg);
        Thread.sleep(INDEXING_TIMEOUT);
    }

    @Test
    public void testPreviewFolderFilter() throws Exception {
        YFuritaPreviewResponse response = new YFuritaPreviewResponse(fUser.previewFilter(filterId));
        YFuritaPreviewResponse respEtalon =
                new YFuritaPreviewResponse(new String[]{fUser.getAllMidsInMailbox().get(0)});

        assertThat("Expected:\n" + respEtalon.print() + "Actual:\n" + response.print(),
                respEtalon, equalTo(response));
    }

    @Test
    public void testApplyFolderFilter() throws Exception {
        fUser.applyFilter(filterId);
        fUser.inFolder(targetFolder.getName()).shouldSeeLetterWithSubject(folderSubj, 60);
    }

    public static class Folder {
        private String name;
        private String fid;

        private Folder(String name, String fid) {
            this.name = name;
            this.fid = fid;
        }

        public String getFid() {
            return fid;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
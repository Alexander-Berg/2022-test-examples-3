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
import ru.yandex.autotests.innerpochta.yfurita.util.FilterSettings;
import ru.yandex.autotests.innerpochta.yfurita.util.FilterUser;
import ru.yandex.autotests.innerpochta.yfurita.util.YFuritaPreviewResponse;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.innerpochta.yfurita.util.FilterSettings.FiltersParams.*;
import static ru.yandex.autotests.innerpochta.yfurita.util.FuritaConsts.PG_FOLDER_DEFAULT;
import static ru.yandex.autotests.innerpochta.yfurita.util.YfuritaProperties.chooseUser;

/**
 * User: stassiak
 * Date: 01.02.12
 * Time: 11:59
 */
@Aqua.Test(title = "Тестирование создания и работы фильтров с проставлением меток",
        description = "Тестирование создания и работы фильтров на метки: preview и apply запросы")
@RunWith(value = Parameterized.class)
@Feature("Yfurita.Base")
public class YFuritaFreshLabelFiltersTest {
    private static final long MOPS_TIMEOUT = 20000;
    private static final long INDEXING_TIMEOUT = 15000;
    private static final long BEFORE_PREVIEW_TIMEOUT = 5000;
    private static User testUser = chooseUser(new User("yfurita.fresh.label.filters@ya.ru", "testqa"),
            new User("mxtest-01@mail.yandex-team.ru", "H@pl@x4-01"),
            new User("robbitter-9165967352@ya.ru", "simple123456"));
    private static FilterUser fUser;
    private String filterId;
    private HashMap<String, String> params = new HashMap<String, String>();
    private String mid;
    @Parameterized.Parameter(0)
    public Label label;
    @Parameterized.Parameter(1)
    public Condition condition;
    @Parameterized.Parameter(2)
    public String labelSubj;

    @Rule
    public LogConfigRule logRule = new LogConfigRule();
    @ClassRule
    public static RestAssuredLogger raLog = new RestAssuredLogger();

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        Collection<Object[]> data = new LinkedList<Object[]>();
        fUser = new FilterUser(testUser);
        Label redLabel = new Label("red_label", fUser.createLabel("red_label"));
        Label blackLabel = new Label("black_label", fUser.createLabel("black_label"));

        data.add(new Object[]{redLabel, new Condition("0", "subject", "1", "crot1"), "crot1"});
        data.add(new Object[]{blackLabel, new Condition("0", "subject", "1", "crot2"), "crot2"});
        data.add(new Object[]{blackLabel, new Condition("0", "subject", "2", "crot"), "crot2"});
        data.add(new Object[]{redLabel, new Condition("0", "subject", "3", "dog"), "dogcat"});
        data.add(new Object[]{redLabel, new Condition("0", "subject", "4", "dog"), "do2gcat"});
        return data;
    }

    @Before
    public void sendMsgs() throws Exception {
        fUser.clearAll();
        fUser.removeAllFilters();
        params.put(NAME.getName(), String.format("%s_%s_%s_%s",
                condition.field1, condition.field2, condition.field3, randomAlphanumeric(20)));
        params.put(LOGIC.getName(), condition.logic);
        params.put(FIELD1.getName(), condition.field1);
        params.put(FIELD2.getName(), condition.field2);
        params.put(FIELD3.getName(), condition.field3);
        params.put(CLICKER.getName(), FilterSettings.CLIKER_MOVEL);
        params.put(MOVE_LABEL.getName(), label.getLid());
        filterId = fUser.createFilter(params);

        TestMessage labelMessage = new TestMessage();
        labelMessage.setRecipient(testUser.getLogin());
        labelMessage.setSubject(labelSubj);
        labelMessage.setFrom("devnull@yandex.ru");
        labelMessage.setText("Message for label " + labelSubj);
        labelMessage.saveChanges();
        fUser.disableFilter(filterId);
        fUser.sendMessageWithFilterOff(labelMessage);
        mid = fUser.inFolder(PG_FOLDER_DEFAULT).getMidOfMessageWithSubject(labelMessage.getSubject());
        Thread.sleep(INDEXING_TIMEOUT);
    }

    @Test
    public void testPreviewFolderFilter() throws Exception {
        Thread.sleep(BEFORE_PREVIEW_TIMEOUT);
        YFuritaPreviewResponse respEtalon = new YFuritaPreviewResponse(new String[]{mid});
        YFuritaPreviewResponse response = new YFuritaPreviewResponse(fUser.previewFilter(filterId));

        assertThat("Expected:\n" + respEtalon.print() + "Actual:\n" + response.print(),
                respEtalon, equalTo(response));
    }

    @Test
    public void testApplyFolderFilter() throws Exception {
        fUser.applyFilter(filterId);
        Thread.sleep(MOPS_TIMEOUT);
        fUser.shouldSeeLetterWithSubjectAndLabelWithLid(labelSubj, label.getLid());
    }

    public static class Label {
        private String name;
        private String lid;

        private Label(String name, String lid) {
            this.name = name;
            this.lid = lid;
        }

        public String getLid() {
            return lid;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
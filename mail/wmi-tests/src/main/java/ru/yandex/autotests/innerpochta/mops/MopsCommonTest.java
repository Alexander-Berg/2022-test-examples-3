package ru.yandex.autotests.innerpochta.mops;

import lombok.val;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.mops.mark.ApiMark.StatusParam;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmicommon.WmiConsts;
import ru.yandex.qatools.allure.annotations.*;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidsWithLabelMatcher.*;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.invalidRequest;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 14.10.14
 * Time: 19:43
 */
@Aqua.Test
@Title("[MOPS] Общие тесты на mops")
@Description("Проверяем mops с невалидными данными, на основе стандартных комманд")
@Features(MyFeatures.MOPS)
@Stories(MyStories.MOPS)
@Issue("AUTOTESTPERS-142")
@Credentials(loginGroup = "CommonMopsTest")
public class MopsCommonTest extends MopsBaseTest {
    public static final String NOT_EXIST_MID = "6666666666666666666";
    public static final String NOT_EXIST_TID = "6666666666666666666";
    public static final String NOT_EXIST_FID = "6666666666666666666";
    public static final String NOT_EXIST_LID = "6666666666666666666";

    public static final String INVALID_MID = "qwdUY7y02&Y*)";
    public static final String INVALID_TID = "qwdUY7y02&Y*)";
    public static final String INVALID_FID = "qwdUY7y02&Y*)";

    public static final String INVALID_DATE = "qwdUY7y02&Y*)";

    public static final String NEGATIVE_MID = "-1";

    private static final int COUNT_OF_LETTERS = 2;

    private enum EmptyMidPosition {
        Begin,
        Middle,
        End
    }

    private List<String> prepareIncorrectMids(List<String> mids, EmptyMidPosition emptyMidPos) throws Exception {
        val midsCount = mids.size();
        if (midsCount < 2) {
            throw new IllegalStateException("Mids count needs to be greater than one");
        }

        val resultMids = new ArrayList<String>(mids);
        switch (emptyMidPos) {
            case Begin:
                resultMids.add(0, "");
                break;
            case Middle:
                resultMids.add(midsCount / 2, "");
                break;
            case End:
                resultMids.add("");
                break;
        }
        return resultMids;
    }

    @Test
    @Title("Запрос к mops с пустым мидом в вначале")
    @Description("Делаем запрос к мопсу c mids=,2490000002650654922,2490000002650654818& Ожидаем, что нет 500-ок")
    public void testMopsWithStartEmptyParamShouldSeeInvalidRequest() throws Exception {
        val mids = sendMail(COUNT_OF_LETTERS).mids();
        val midsWithIncorrectItem = prepareIncorrectMids(mids, EmptyMidPosition.Begin);

        mark(new MidsSource(midsWithIncorrectItem), StatusParam.READ).post(shouldBe(invalidRequest()));

        assertThat("Ожидалось что письма будут помечены как непрочитанные", authClient,
                not(hasMsgsWithLid(mids, WmiConsts.FAKE_SEEN_LBL)));
    }

    @Test
    @Issue("DARIA-37269")
    @Title("Запрос к mops с пустым мидом в середине")
    @Description("Делаем запрос с mids=2490000002650634659,2490000002650634554,,2490000002650635114. " +
            "Проверяем, что нет 500к")
    public void mopsWithEmptyParamShouldSeeInvalidRequest() throws Exception {
        val mids = sendMail(COUNT_OF_LETTERS).mids();
        val midsWithIncorrectItem = prepareIncorrectMids(mids, EmptyMidPosition.Middle);

        mark(new MidsSource(midsWithIncorrectItem), StatusParam.READ).post(shouldBe(invalidRequest()));
        assertThat("Ожидалось что письма будут помечены как непрочитанные", authClient,
            not(hasMsgsWithLid(mids, WmiConsts.FAKE_SEEN_LBL)));
    }

    @Test
    @Title("Запрос к mops с пустым мидом в конце")
    @Description("Делаем запрос с mids=2490000002650622999,2490000002650622910,&" +
            "(где миды корректны). Проверяем, что нет 500к")
    public void testMopsWithEndEmptyParamShouldSeeInvalidRequest() throws Exception {
        val mids = sendMail(COUNT_OF_LETTERS).mids();
        val midsWithIncorrectItem = prepareIncorrectMids(mids, EmptyMidPosition.End);

        mark(new MidsSource(midsWithIncorrectItem), StatusParam.READ).post(shouldBe(invalidRequest()));

        assertThat("Ожидалось что письма будут помечены как непрочитанные", authClient,
            not(hasMsgsWithLid(mids, WmiConsts.FAKE_SEEN_LBL)));
    }

    @Test
    @Title("Запрос к mops с отрицательным mid")
    @Description("Проверяем, что запрос с отрицательным mid возвращает 400")
    @Issue("MAILPG-3716")
    public void testMopsWithNegativeMidShouldSeeInvalidRequest() throws Exception {
        remove(new MidsSource(NEGATIVE_MID)).post(shouldBe(invalidRequest()));
    }
}

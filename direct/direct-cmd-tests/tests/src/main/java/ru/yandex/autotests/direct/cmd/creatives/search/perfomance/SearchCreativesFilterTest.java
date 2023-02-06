package ru.yandex.autotests.direct.cmd.creatives.search.perfomance;

import org.apache.commons.lang.time.DateUtils;
import org.junit.*;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.creatives.Creative;
import ru.yandex.autotests.direct.cmd.data.creatives.SearchCreativesFilterEnum;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.PerfCreativesBusinessType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.PerfCreativesRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

@Aqua.Test
@Description("Поиск креативов по фильтрам через searchCreative (страница креативов)")
@Stories(TestFeatures.Creatives.FILTERS)
@Features(TestFeatures.CREATIVES)
@Tag(CmdTag.SEARCH_CREATIVES)
@Tag(ObjectTag.CREATIVE)
@Tag(TrunkTag.YES)
public class SearchCreativesFilterTest {

    private static final String CLIENT = "at-direct-search-cr5";
    private static final PerfCreativesBusinessType BUSINESS_TYPE = PerfCreativesBusinessType.hotels;
    private static final Long THEME_ID = 5L;
    private static final Long LAYOUT_ID = 5L;
    private static final Long HEIGHT = 500L;
    private static final Long GROUP_ID = 5L;

    @ClassRule
    public static DirectCmdRule cmdRule = DirectCmdRule.defaultClassRule();

    private PerfCreativesRecord firstPerfCreative;
    private PerfCreativesRecord secondPerfCreative;

    @Before
    public void before() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(CLIENT));
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps()
                .deletePerfCreativesByClientId(Long.valueOf(User.get(CLIENT).getClientID()));
        firstPerfCreative = createCreative();
        secondPerfCreative = createCreative();
    }

    @After
    public void after() {
        if (firstPerfCreative != null) {
            TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps()
                    .deletePerfCreatives(firstPerfCreative.getCreativeId());
        }

        if (secondPerfCreative != null) {
            TestEnvironment.newDbSteps().useShardForLogin(CLIENT).perfCreativesSteps()
                    .deletePerfCreatives(secondPerfCreative.getCreativeId());
        }
    }

    @Test
    @Description("Поиск креативов по business type")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10832")
    public void testSearchCreativesFilterBusiness() {
        updateCreative(firstPerfCreative.setBusinessType(BUSINESS_TYPE));
        check(SearchCreativesFilterEnum.BUSINESS_TYPE, BUSINESS_TYPE.getLiteral());
    }

    @Test
    @Description("Поиск креативов по layout")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10830")
    public void testSearchCreativesFilterLayout() {
        updateCreative(firstPerfCreative.setLayoutId(LAYOUT_ID));
        check(SearchCreativesFilterEnum.LAYOUT, LAYOUT_ID.toString());
    }

    @Test
    @Description("Поиск креативов по size")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10831")
    public void testSearchCreativesFilterSize() {
        updateCreative(firstPerfCreative.setHeight(HEIGHT.shortValue()));
        check(SearchCreativesFilterEnum.SIZE, firstPerfCreative.getWidth() + "x" + HEIGHT);
    }

    @Test
    @Description("Поиск креативов по theme id")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10833")
    public void testSearchCreativesFilterTheme() {
        updateCreative(firstPerfCreative.setThemeId(THEME_ID));
        check(SearchCreativesFilterEnum.THEME, THEME_ID.toString());
    }

    @Test
    @Description("Поиск креативов по group create date")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10834")
    public void testSearchCreativesFilterCreateTime() {
        Date date = new Date();
        date = DateUtils.round(date, Calendar.SECOND);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        updateCreative(firstPerfCreative.setGroupCreateTime(new Timestamp(date.getTime())));
        check(SearchCreativesFilterEnum.CREATE_TIME, format.format(date));
    }

    @Test
    @Description("Поиск креативов по group create date")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10835")
    public void testSearchCreativesFilterGroupId() {
        updateCreative(firstPerfCreative.setCreativeGroupId(GROUP_ID));
        check(SearchCreativesFilterEnum.GROUP_ID, GROUP_ID.toString());
    }

    private void check(SearchCreativesFilterEnum filterEnum, String value) {
        List<Long> creativesIds = cmdRule.cmdSteps().creativesSteps()
                .searchCreatives(filterEnum, value).stream()
                .map(Creative::getCreativeId)
                .collect(Collectors.toList());
        assertThat("найденный креатив в ответе ручки searchCreatives соответствует ожидаемому",
                creativesIds, contains(firstPerfCreative.getCreativeId()));
    }

    private PerfCreativesRecord createCreative() {
        Long creativeId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                .perfCreativesSteps().saveDefaultPerfCreative(User.get(CLIENT).getClientID());
        return TestEnvironment.newDbSteps().perfCreativesSteps().getPerfCreatives(creativeId);
    }

    private void updateCreative(PerfCreativesRecord perfCreative) {
        TestEnvironment.newDbSteps().perfCreativesSteps().updatePerfCreatives(perfCreative);
    }
}

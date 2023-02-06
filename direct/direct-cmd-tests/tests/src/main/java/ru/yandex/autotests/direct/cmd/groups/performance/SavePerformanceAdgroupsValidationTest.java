package ru.yandex.autotests.direct.cmd.groups.performance;

import com.google.gson.Gson;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.CommonErrorsResource;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorData;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Condition;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.common.CreativeBanner;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.performancefilters.PerformanceFilter;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.savePerformanceAdGroups.GroupErrorsResponse;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.textresources.banners.PerformanceGroupsErrorTexts;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasItem;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка валидации контроллера savePerformanceAdGroups")
@Stories(TestFeatures.Groups.SAVE_PERFORMANCE_AD_GROUPS)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_PERFORMANCE_AD_GROUPS)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.PERFORMANCE)
@Ignore("DIRECT-59019")
public class SavePerformanceAdgroupsValidationTest extends SavePerformanceAdgroupsTestBase {

    @Test
    @Description("Проверка ошибки при вызове контроллера сохранения группы без имени группы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9845")
    public void emptyGroupNameTest() {
        expectedGroup.setAdGroupName("");
        GroupErrorsResponse actualError = saveInvalidGroup();
        assertThat("Ошибка соответстует ожиданию", actualError.getErrors().getGroupErrors().getArrayErrors().get(0)
                .getObjectErrors().getAdgroupNameErrors().stream().map(ErrorData::getDescription)
                .collect(Collectors.toList()), hasItem(PerformanceGroupsErrorTexts.EMPTY_GROUP_NAME.toString()));
    }

    @Test
    @Description("Проверка ошибки при вызове контроллера сохранения группы без баннеров")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9846")
    public void groupWithoutBannersTest() {
        expectedGroup.setBanners(new ArrayList<>());
        GroupErrorsResponse actualError = saveInvalidGroup();
        assertThat("Ошибка соответстует ожиданию", actualError.getErrors().getGroupErrors().getGenericErrors().stream()
                        .map(ErrorData::getText).collect(Collectors.toList()),
                hasItem(PerformanceGroupsErrorTexts.BANNERS_NOT_FOUND.toString()));
    }

    @Test
    @Description("Проверка ошибки при вызове контроллера сохранения группы без креатива")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9850")
    public void bannerWithoutCreativeTest() {
        expectedGroup.getBanners().get(0).withCreativeBanner(null);
        GroupErrorsResponse actualError = saveInvalidGroup();
        assertThat("Ошибка соответстует ожиданию", actualError.getErrors().getGroupErrors().getGenericErrors().stream()
                        .map(ErrorData::getText).collect(Collectors.toList()),
                hasItem(CommonErrorsResource.WRONG_INPUT_DATA.toString()));
    }

    @Test
    @Description("Проверка ошибки при вызове контроллера сохранения группы без фильтров")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9847")
    public void bannerWithoutFiltersTest() {
        expectedGroup.setPerformanceFilters(null);
        GroupErrorsResponse actualError = saveInvalidGroup();
        assertThat("Ошибка соответстует ожиданию", actualError.getErrors().getGroupErrors().getGenericErrors().stream()
                        .map(ErrorData::getText).collect(Collectors.toList()),
                hasItem(PerformanceGroupsErrorTexts.FILTERS_NOT_FOUND.toString()));
    }

    @Test
    @Description("Проверка ошибки при вызове контроллера сохранения группы с кол-вом фильтров > 50")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9848")
    public void groupTooManyFiltersTest() {
        addFilters(50);
        GroupErrorsResponse actualError = saveInvalidGroup();
        assertThat("Ошибка соответстует ожиданию", actualError.getErrors().getGroupErrors().getArrayErrors()
                        .get(0).getObjectErrors().getFiltersErrors().getGenericErrors()
                        .stream().map(ErrorData::getDescription).collect(Collectors.toList()),
                hasItem(PerformanceGroupsErrorTexts.TOO_MANY_FILTERS.toString()));
    }

    @Test
    @Description("Проверка ошибки при вызове контроллера сохранения группы с кол-вом баннеров > 50")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9849")
    public void groupTooManyBannersTest() {
        addBanners(50);
        GroupErrorsResponse actualError = saveInvalidGroup();
        assertThat("Ошибка соответстует ожиданию", actualError.getErrors().getGroupErrors().getArrayErrors()
                        .get(0).getObjectErrors().getBannersErrors().getGenericErrors()
                        .stream().map(ErrorData::getDescription).collect(Collectors.toList()),
                hasItem(PerformanceGroupsErrorTexts.TOO_MANY_BANNERS.toString()));
    }

    private void addFilters(int count) {
        List<PerformanceFilter> performanceFilters = expectedGroup.getPerformanceFilters();
        String filterString = new Gson().toJson(performanceFilters.get(0));
        for (int i = 0; i < count; i++) {
            PerformanceFilter filter = new Gson().fromJson(filterString, PerformanceFilter.class);
            Condition condition = filter.getConditions().get(0);

            filter.setFilterName(filter.getFilterName() + " " + i);
            condition.withValue(singletonList(RandomUtils.getString(7)));
            performanceFilters.add(filter);
        }
    }

    private void addBanners(int count) {
        List<Long> creativeIdList = createCreative(count);
        List<Banner> banners = expectedGroup.getBanners();
        String bannerString = new Gson().toJson(banners.get(0));
        for (int i = 0; i < count; i++) {
            Banner banner = new Gson().fromJson(bannerString, Banner.class);
            CreativeBanner creativeBanner = banner.getCreativeBanner();

            creativeBanner.withCreativeId(creativeIdList.get(i));
            banners.add(banner);
        }
        creativeIds.addAll(creativeIdList);
    }
}

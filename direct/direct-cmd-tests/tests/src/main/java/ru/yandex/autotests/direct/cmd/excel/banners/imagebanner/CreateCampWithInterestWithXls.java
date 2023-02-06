package ru.yandex.autotests.direct.cmd.excel.banners.imagebanner;

import java.io.File;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.excel.ExcelColumnsEnum;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.interest.TargetInterests;
import ru.yandex.autotests.direct.cmd.data.interest.TargetInterestsFactory;
import ru.yandex.autotests.direct.cmd.data.redirect.LocationParam;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.excel.ExcelUtils;
import ru.yandex.autotests.direct.cmd.steps.retargeting.RetargetingHelper;
import ru.yandex.autotests.direct.cmd.tags.BusinessProcessTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppcdict.tables.records.TargetingCategoriesRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.irt.testutils.ResourceUtils;
import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.direct.cmd.steps.images.BannerImagesSteps.IMAGE_UPLOAD_CONDITION;
import static ru.yandex.autotests.direct.cmd.steps.images.BannerImagesSteps.allImageUploadTasksProcessed;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Создание новой кампании с интересами через excel")
@Stories(TestFeatures.Excel.EXCEL_UPLOAD)
@Feature(TestFeatures.EXCEL)
@Tag(ObjectTag.TARGET_INTERESTS)
@Tag(BusinessProcessTag.EXCEL)
@Tag(TrunkTag.YES)
public class CreateCampWithInterestWithXls {
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    private static final String CLIENT = Logins.CLIENT_WITH_INTERESTS;
    private static final Long clientId = Long.valueOf(User.get(CLIENT).getClientID());
    private String xlsTemplatePath = "excel/interest/one_interest.xls";
    private File excelToUpload;
    private TargetInterests expectedInterests;
    private Long newCampaignId;

    @Before
    public void before() {
        TargetingCategoriesRecord targetingCategory = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).interestSteps()
                .getTargetingCategoriesRecords(RetargetingHelper.getRandomTargetCategoryId());
        expectedInterests = TargetInterestsFactory.defaultTargetInterest(targetingCategory.getCategoryId())
                .withPriceContext(6D)
                .withAutobudgetPriority(null);
        excelToUpload = new File("excelWithCategory" + targetingCategory.getCategoryId() + ".xls");
        File templateCampFile = ResourceUtils.getResourceAsFile(xlsTemplatePath);
        ExcelUtils.setCellValue(templateCampFile, excelToUpload, ExcelColumnsEnum.PHARASE_WITH_MINUS_WORDS, 0,
                "interest: " + targetingCategory.getName() + "(" + targetingCategory.getCategoryId() + ")");
    }

    @Test
    @Description("Создаем кампанию с интересом  через xls")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10649")
    public void createCampWithInterest() {
        newCampaignId = cmdRule.cmdSteps().excelSteps().safeImportCampaignFromXls(excelToUpload, CLIENT, "",
                ImportCampXlsRequest.DestinationCamp.NEW).getLocationParamAsLong(LocationParam.CID);
        IMAGE_UPLOAD_CONDITION.until(allImageUploadTasksProcessed(cmdRule, clientId));
        List<Banner> banners = cmdRule.cmdSteps().groupsSteps().getBanners(CLIENT, newCampaignId);

        assumeThat("баннеры успешно создались", banners, Matchers.hasSize(1));
        ShowCampResponse showCamp = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT,
                newCampaignId.toString());
        List<TargetInterests> actualInterests = showCamp.getGroups().get(0).getTargetInterests();
        assumeThat("Сохранилось ождаемое число интересов", actualInterests, hasSize(1));

        DefaultCompareStrategy strategy = DefaultCompareStrategies.onlyExpectedFields();
        strategy.forFields(BeanFieldPath.newPath("retId")).useMatcher(greaterThan(0));

        assertThat("Таргетинг на интересы соответсвует ожиданием",
                actualInterests.get(0),
                beanDiffer(expectedInterests)
                        .useCompareStrategy(strategy));
    }

    @After
    public void delete() {
        if (newCampaignId != null) {
            cmdRule.cmdSteps().campaignSteps().deleteCampaign(CLIENT, newCampaignId);
        }
        if (excelToUpload != null) {
            excelToUpload.delete();
        }
    }

}


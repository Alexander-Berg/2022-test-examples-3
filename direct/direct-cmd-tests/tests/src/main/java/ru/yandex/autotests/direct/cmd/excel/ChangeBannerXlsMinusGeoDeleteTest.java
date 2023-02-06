package ru.yandex.autotests.direct.cmd.excel;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.excel.ExcelColumnsEnum;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.excel.ExcelUtils;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersMinusGeoType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannersMinusGeoRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.contains;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Удаление минус регионов при изменении баннера через эксель")
@Stories(TestFeatures.Excel.EXCEL_UPLOAD)
@Feature(TestFeatures.EXCEL)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class ChangeBannerXlsMinusGeoDeleteTest {

    private static final String CLIENT = "at-direct-backend-c";
    private static final String NEW_TEXT = "new text";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule;

    @Rule
    public DirectCmdRule cmdRule;

    private File tempExcel;
    private File excelToUpload;

    @Parameterized.Parameters(name = "Удаление минус регионов при изменении баннера через эксель. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE},
        });
    }

    public ChangeBannerXlsMinusGeoDeleteTest(CampaignTypeEnum campaignType) {
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType)
                .overrideGroupTemplate(new Group().withGeo(Geo.RUSSIA.getGeo() + "," + Geo.UKRAINE.getGeo()))
                .withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }


    @Before
    public void before() {
        BsSyncedHelper.moderateCamp(cmdRule, bannersRule.getCampaignId());
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersSteps()
                .saveBannersMinusGeo(bannersRule.getBannerId(), BannersMinusGeoType.current, Geo.UKRAINE.getGeo());
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersSteps()
                .saveBannersMinusGeo(bannersRule.getBannerId(), BannersMinusGeoType.bs_synced, Geo.AUSTRIA.getGeo());

        tempExcel = cmdRule.cmdSteps().excelSteps().exportXlsCampaign(bannersRule.getCampaignId(), CLIENT);
        try {
            excelToUpload = File.createTempFile(RandomUtils.getString(10), ".xls");
        } catch (Exception e) {
            throw new IllegalStateException("ошибка создания временного excel-файла", e);
        }
    }

    @After
    public void after() {
        FileUtils.deleteQuietly(tempExcel);
    }

    @Test
    @Description("удаление минус регионов при изменении баннера через эксель")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10845")
    public void deleteMinusGeoChangeBannerTest() {
        ExcelUtils.setCellValue(tempExcel, excelToUpload, ExcelColumnsEnum.TEXT, 0, NEW_TEXT);

        cmdRule.cmdSteps().excelSteps()
                .safeImportCampaignFromXls(excelToUpload, CLIENT, bannersRule.getCampaignId().toString(),
                        ImportCampXlsRequest.DestinationCamp.OLD);

        List<Map<String, Object>> actualRecords = TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                .bannersSteps().getBannersMinusGeo(bannersRule.getBannerId()).stream()
                .map(r -> r.intoMap())
                .collect(Collectors.toList());

        assertThat("минус гео скопировалось", actualRecords,
                contains(beanDiffer(getExpectedRecordMap(bannersRule.getBannerId(), BannersMinusGeoType.bs_synced,
                        Geo.AUSTRIA.getGeo()))));
    }

    private Map<String, Object> getExpectedRecordMap(Long bid, BannersMinusGeoType type, String minusGeo) {
        return new BannersMinusGeoRecord()
                .setBid(bid)
                .setType(type)
                .setMinusGeo(minusGeo).intoMap();
    }

}

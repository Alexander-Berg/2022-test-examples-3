package ru.yandex.autotests.direct.cmd.images.upload;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.DirectCmdSteps;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.images.BannerImageFormats;
import ru.yandex.autotests.direct.cmd.data.images.Formats;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.images.AbstractImageUploadHelper;
import ru.yandex.autotests.direct.cmd.steps.images.ImageParams;
import ru.yandex.autotests.direct.cmd.steps.images.NewImagesUploadHelper;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.ImageUtils;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannerImagesFormatsAvatarsHost;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannerImagesFormatsImageType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannerImagesFormatsNamespace;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.BannerImagesFormats;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.BannerImagesPool;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannerImagesFormatsRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannerImagesPoolRecord;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.AllOf.allOf;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;

@Aqua.Test
@Description("Проверка базы после загрузки картинки (cmd uploadImage)")
@Stories(TestFeatures.BannerImages.UPLOAD_IMAGE)
@Features(TestFeatures.BANNER_IMAGES)
@Tag(CmdTag.UPLOAD_IMAGE)
@Tag(ObjectTag.IMAGE)
@RunWith(Parameterized.class)
public class UploadImageDataBaseRecordsTest {

    public static final String CLIENT = Logins.DEFAULT_CLIENT;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule ruleChain = DirectCmdRule.defaultRule().as(CLIENT);
    @Parameterized.Parameter(0)
    public AbstractImageUploadHelper.UploadType uploadType;
    public ImageUtils.ImageFormat format = ImageUtils.ImageFormat.GIF;
    public Short width = 300;
    public Short height = 250;
    protected DirectCmdSteps directCmdSteps;
    protected NewImagesUploadHelper imageUploadHelper;
    private DirectJooqDbSteps dbSteps = TestEnvironment.newDbSteps();

    @Parameterized.Parameters(name = "Загрузка картинки из {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {AbstractImageUploadHelper.UploadType.URL},
                {AbstractImageUploadHelper.UploadType.FILE}
        });
    }

    @Before
    public void before() {
        dbSteps.useShardForLogin(CLIENT);
        directCmdSteps = ruleChain.cmdSteps();
        imageUploadHelper = (NewImagesUploadHelper) new NewImagesUploadHelper().
                withBannerImageSteps(directCmdSteps.bannerImagesSteps()).
                withUploadType(uploadType).
                withClient(CLIENT).
                withImageParams(new ImageParams().
                        withFormat(format).
                        withWidth(width.intValue()).
                        withHeight(height.intValue()));

        imageUploadHelper.upload();
        assumeThat("картинка загрузилась", imageUploadHelper.getUploadResponse().getHash(), notNullValue());
    }

    @Test
    @Description("Проверяем правильность записи в таблице banner_images_formats после загрузки картинки")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9891")
    public void checkBannerImageFormats() {
        BannerImagesFormatsRecord record = dbSteps.imagesSteps().
                getBannerImagesFormatsRecords(imageUploadHelper.getUploadResponse().getHash());
        assumeThat("В таблице banner_images_formats есть запись", record, notNullValue());
        Map<String, Object> values = new HashMap<>();
        values.put(BannerImagesFormats.BANNER_IMAGES_FORMATS.NAMESPACE.getName(),
                BannerImagesFormatsNamespace.direct_picture);
        values.put(BannerImagesFormats.BANNER_IMAGES_FORMATS.AVATARS_HOST.getName(),
                BannerImagesFormatsAvatarsHost.avatars_mdst_yandex_net);
        values.put(BannerImagesFormats.BANNER_IMAGES_FORMATS.IMAGE_HASH.getName(),
                imageUploadHelper.getUploadResponse().getHash());
        values.put(BannerImagesFormats.BANNER_IMAGES_FORMATS.MDS_META.getName(), null);
        values.put(BannerImagesFormats.BANNER_IMAGES_FORMATS.MDS_GROUP_ID.getName(),
                Long.valueOf(imageUploadHelper.getUploadResponse().getGroupId()));
        values.put(BannerImagesFormats.BANNER_IMAGES_FORMATS.IMAGE_TYPE.getName(),
                BannerImagesFormatsImageType.image_ad);
        values.put(BannerImagesFormats.BANNER_IMAGES_FORMATS.HEIGHT.getName(), height);
        values.put(BannerImagesFormats.BANNER_IMAGES_FORMATS.WIDTH.getName(), width);

        Map<String, Object> actualData = record.intoMap();
        prepareImagesFormatData(values, record, actualData);

        assertThat("Запись в таблице banner_images_formats соответствует ожиданию",
                actualData, beanDiffer(values).useCompareStrategy(
                        allFieldsExcept(newPath(BannerImagesFormats.BANNER_IMAGES_FORMATS.MDS_META.getName()))));
    }

    @Test
    @Description("Проверяем правильность записи в таблице banner_images_pool после загрузки картинки")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9892")
    public void checkBannerImagesPool() {
        List<BannerImagesPoolRecord> records =
                dbSteps.imagesSteps().getBannerImagesPoolRecords(imageUploadHelper.getUploadResponse().getHash());
        assumeThat("В таблице banner_images_pool одна запись", records, hasSize(1));
        Map<String, Object> values = new HashMap<>();
        values.put(BannerImagesPool.BANNER_IMAGES_POOL.CLIENTID.getName(),
                dbSteps.shardingSteps().getClientIdByLogin(CLIENT));
        values.put(BannerImagesPool.BANNER_IMAGES_POOL.NAME.getName(),
                imageUploadHelper.getUploadResponse().getName());
        values.put(BannerImagesPool.BANNER_IMAGES_POOL.IMAGE_HASH.getName(),
                imageUploadHelper.getUploadResponse().getHash());

        prepareImagesPoolData(values, records.get(0));

        assertThat("Запись в таблице banner_images_pool соответствует ожиданию",
                records.get(0).intoMap(), beanDiffer(values).useCompareStrategy(
                        allFieldsExcept(newPath(BannerImagesPool.BANNER_IMAGES_POOL.MDS_META_USER_OVERRIDE.getName()))));
        assertThat("Время создания в таблице +- 1 минута", records.get(0).getCreateTime(),
                allOf(greaterThan(DateTime.now().minusMinutes(1).toDate()), lessThan(DateTime.now().toDate())));
    }

    private void prepareImagesFormatData(Map<String, Object> expectedData,
            BannerImagesFormatsRecord actualRecord, Map<String, Object> actualData)
    {
        actualData.put(BannerImagesFormats.BANNER_IMAGES_FORMATS.FORMATS.getName(),
                new Gson().fromJson(actualRecord.getFormats(), BannerImageFormats.class));
        expectedData.put(BannerImagesFormats.BANNER_IMAGES_FORMATS.FORMATS.getName(),
                new BannerImageFormats().withOrig(new Formats()
                        .withHeight(imageUploadHelper.getUploadResponse().getHeight())
                        .withWidth(imageUploadHelper.getUploadResponse().getWidth())
                        .withPath("/get-direct-picture/" + imageUploadHelper.getUploadResponse().getGroupId()
                                + "/" + imageUploadHelper.getUploadResponse().getHash() + "/orig")));
    }

    private void prepareImagesPoolData(Map<String, Object> expectedData, BannerImagesPoolRecord actualData) {
        expectedData.put(BannerImagesPool.BANNER_IMAGES_POOL.IMP_ID.getName(),
                actualData.getImpId());
        expectedData.put(BannerImagesPool.BANNER_IMAGES_POOL.NAME.getName(),
                actualData.getName());
        expectedData.put(BannerImagesPool.BANNER_IMAGES_POOL.CREATE_TIME.getName(),
                actualData.getCreateTime());
        expectedData.put(BannerImagesPool.BANNER_IMAGES_POOL.SOURCE.getName(),
                actualData.getSource());
    }
}

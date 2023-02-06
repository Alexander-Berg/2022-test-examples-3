package ru.yandex.autotests.directintapi.tests.images;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.BannerFakeInfo;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.enums.ImageType;
import ru.yandex.autotests.directapi.logic.ppc.BannerImages;
import ru.yandex.autotests.directapi.model.common.Value;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by chicos on 21.04.2015.
 */
@Aqua.Test
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.REMOVE_BANNER_IMAGES)
@Issue("https://st.yandex-team.ru/DIRECT-41165")
@Description("Проверка удаления отвязанных от баннера и не отправленных в БК картинок. " +
        "После доработки скрипта в DIRECT-42328, при первом запуске на на 'холодной' базе таймаутов быть не должно")
public class RemoveBannerImagesTest {
    protected static LogSteps log = LogSteps.getLogger(RemoveBannerImagesTest.class);
    private static DarkSideSteps darkSideSteps = new DarkSideSteps();

    private static final String login = Logins.LOGIN_MAIN_IMAGES;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(login);

    @Rule
    public Trashman trasher = new Trashman(api);

    private static Long bannerID1;
    private static Long bannerID2;
    private static Long bannerID3;

    private static int userShard;
    private static final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");


    @BeforeClass
    public static void prepareImages() {
        userShard = api.userSteps.clientFakeSteps().getUserShard(login);

        log.info("Создадим кампанию и объявления");
        Long campaignId = api.userSteps.campaignSteps().addDefaultTextCampaign();
        Long pid = api.userSteps.adGroupsSteps().addDefaultGroup(campaignId);
        bannerID1 = api.userSteps.adsSteps().addDefaultTextAd(pid);
        bannerID2 = api.userSteps.adsSteps().addDefaultTextAd(pid);
        bannerID3 = api.userSteps.adsSteps().addDefaultTextAd(pid);

        log.info("Добавим баннерам картинки");
        String[] hashes = api.userSteps.imagesSteps().configLoginImages(login, ImageType.REGULAR, 1, 2, 3);
        api.userSteps.imagesSteps().setAdImageAssociation(bannerID1, hashes[0]);
        api.userSteps.imagesSteps().setAdImageAssociation(bannerID2, hashes[1]);
        api.userSteps.imagesSteps().setAdImageAssociation(bannerID3, hashes[2]);

        log.info("Имитируем отправку одного баннера c картинкой в БК");
        api.userSteps.bannersFakeSteps().makeBannerFullyModerated(bannerID1);
        api.userSteps.bannersFakeSteps().setImageStatusModerate(bannerID1, Value.YES);
        api.userSteps.bannersFakeSteps().setBannerRandomFakeBannerID(bannerID1);
        api.userSteps.bannersFakeSteps().setBannerFakeImageBannerIDRandom(bannerID1);

        log.info("Отвязка картинок от объявлений");
        api.userSteps.imagesSteps().setAdImageAssociation(bannerID1, null);
        api.userSteps.imagesSteps().setAdImageAssociation(bannerID2, null);
        api.userSteps.imagesSteps().setAdImageAssociation(bannerID3, null);

        log.info("Фейково изменим время последней модификации картинок");
        BannerFakeInfo bannerInfo1 = api.userSteps.bannersFakeSteps().getBannerParams(bannerID1);
        darkSideSteps.getDBSteps().getBannerImagesSteps().setBannerImagesDateAdded(
                bannerInfo1.getImageImageId(), formatter.print(DateTime.now().minusHours(2)), userShard);
        //darkSideSteps.getDBSteps().getBannerImagesUploadsSteps().setBannerImagesUploadsDateAdded(campaignId, newDate, userShard);

        BannerFakeInfo bannerInfo2 = api.userSteps.bannersFakeSteps().getBannerParams(bannerID2);
        darkSideSteps.getDBSteps().getBannerImagesSteps().setBannerImagesDateAdded(
                bannerInfo2.getImageImageId(), formatter.print(DateTime.now().minusHours(2)), userShard);

        BannerFakeInfo bannerInfo3 = api.userSteps.bannersFakeSteps().getBannerParams(bannerID3);
        darkSideSteps.getDBSteps().getBannerImagesSteps().setBannerImagesDateAdded(
                bannerInfo3.getImageImageId(), formatter.print(DateTime.now().minusMinutes(30)), userShard);

        log.info("Вызываем скрипт ppcRemoveBannerImagesPPC.pl - удаление картинок не отправленных в БК");
        darkSideSteps.getRunScriptSteps().runPpcRemoveBannerImages(userShard, campaignId,
                bannerInfo1.getImageImageId(),
                bannerInfo2.getImageImageId(),
                bannerInfo3.getImageImageId());
    }

    @Test
    @Description("Картинка отправленная в БК и затем отвзяанная от объявления не должна удалиться")
    public void imageSentToBSNotRemovedTest() {
        BannerImages images1 =
                darkSideSteps.getDBSteps().getBannerImagesSteps().getBannerImagesByBid(bannerID1, userShard);
        assertThat("картинка не удалилась", images1, notNullValue());
    }

    @Test
    @Description("Картинка не отправленная в БК и отвзяанная от объявления более часа назад должна быть удалена")
    public void imageNotSentToBSRemovedTest() {
        BannerImages images2 =
                darkSideSteps.getDBSteps().getBannerImagesSteps().getBannerImagesByBid(bannerID2, userShard);
        assertThat("картинка удалена", images2, nullValue());
    }

    @Test
    @Description("Картинка не отправленная в БК и отвзяанная от объявления менее часа назад не должна удалиться")
    public void imageNotSentToBSNotRemovedTest() {
        BannerImages images3 =
                darkSideSteps.getDBSteps().getBannerImagesSteps().getBannerImagesByBid(bannerID3, userShard);
        assertThat("картинка не удалилась", images3, notNullValue());
    }
}

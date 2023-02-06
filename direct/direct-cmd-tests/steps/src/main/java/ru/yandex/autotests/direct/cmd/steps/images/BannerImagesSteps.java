package ru.yandex.autotests.direct.cmd.steps.images;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;
import com.jayway.awaitility.core.ConditionFactory;
import org.apache.commons.io.FileUtils;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.images.AjaxResizeBannerImageRequest;
import ru.yandex.autotests.direct.cmd.data.images.AjaxResizeBannerImageResponse;
import ru.yandex.autotests.direct.cmd.data.images.UploadBannerImageRequest;
import ru.yandex.autotests.direct.cmd.data.images.UploadBannerImageResponse;
import ru.yandex.autotests.direct.cmd.data.images.UploadImageRequest;
import ru.yandex.autotests.direct.cmd.data.images.UploadImageResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.cmd.util.ImageUtils;
import ru.yandex.autotests.direct.db.steps.DBQueueSteps;
import ru.yandex.autotests.directapi.beans.images.ImageFormat;
import ru.yandex.autotests.httpclientlite.core.RequestBuilder;
import ru.yandex.autotests.httpclientlite.core.request.multipart.MultipartRequestBuilder;
import ru.yandex.autotests.irt.testutils.allure.AllureUtils;
import ru.yandex.qatools.allure.annotations.Step;

import static ru.yandex.autotests.direct.cmd.data.Headers.ACCEPT_JSON_HEADER;
import static ru.yandex.autotests.direct.cmd.data.Headers.X_REQUESTED_WITH_HEADER;

public class BannerImagesSteps extends DirectBackEndSteps {

    private final static ImageFormat DEFAULT_BANNER_IMAGE_FORMAT = ImageFormat.X_450;

    /**
     * Условие ожидания загрузки картинки, при загрузке кампании через excel
     */
    public static final ConditionFactory IMAGE_UPLOAD_CONDITION = Awaitility.
            with().timeout(new Duration(12L, TimeUnit.MINUTES)).
            and().with().pollDelay(Duration.TWO_SECONDS).
            and().with().pollInterval(new Duration(20L, TimeUnit.SECONDS)).
            await("ppcProcessImageQueue handled all tasks");

    /**
     * Подопнуть и дождаться, что все задачи по загрузке картинок будут обработаны
     * На ТС картинки штатно обрабатывает скрипт, запущенный в фоне. на бетах его нужно запускать вручную.
     * в редких случаях - скрипт на ТС успевает взять задачи в работу, и запуск "сбоку" тут же выходит т.к. задач нет,
     * при этом картинки еще продолжают грузится. дожидаемся пока все будет обработано (не важно успешно или нет).
     * <p>
     * NB: будет плохо работать на активных логинах, где задачи активно добавляются
     */
    public static Callable<Boolean> allImageUploadTasksProcessed(DirectCmdRule cmdRule, Long clientID) {
        Integer shard = cmdRule.dbSteps().shardingSteps().getShardByClientID(clientID);
        return () -> {
            cmdRule.darkSideSteps().getRunScriptSteps().runProcessImageQueue(shard, clientID.toString(), 42);
            return cmdRule.dbSteps().useShard(shard).dbqueueSteps()
                    .getDbqueueJobsRecords(clientID, DBQueueSteps.TYPE_BANNER_IMAGES)
                    .isEmpty();
        };
    }

    @Override
    protected RequestBuilder buildRequestBuilder() {
        MultipartRequestBuilder requestBuilder = new MultipartRequestBuilder();
        requestBuilder.setHeaders(ACCEPT_JSON_HEADER, X_REQUESTED_WITH_HEADER);
        return requestBuilder;
    }

    @Step("Загрузка картинки для баннера на сервер (из файла)")
    public UploadBannerImageResponse uploadBannerImage(long uid, long campaignId, File image) {
        try {
            AllureUtils.addPngAttachment("Картинка", FileUtils.readFileToByteArray(image));
        } catch (IOException e) {
            AllureUtils.addTextAttachment("не удалось прочитать файл с картинкой" + image.getAbsolutePath(), "");
        }
        UploadBannerImageRequest request = new UploadBannerImageRequest().
                withCampaignId(campaignId).
                withUid(uid).
                withImagePath(image.getAbsolutePath());
        return postUploadBannerImage(request);
    }

    @Step("Загрузка картинки для баннера на сервер (по ссылке)")
    public UploadBannerImageResponse uploadBannerImage(long uid, long campaignId, String imageUrl) {
        UploadBannerImageRequest request = new UploadBannerImageRequest().
                withCampaignId(campaignId).
                withUid(uid).
                withUrl(imageUrl);
        return postUploadBannerImage(request);
    }

    @Step("POST cmd = uploadBannerImage")
    public UploadBannerImageResponse postUploadBannerImage(UploadBannerImageRequest request) {
        return post(CMD.UPLOAD_BANNER_IMAGE, request, UploadBannerImageResponse.class);
    }

    @Step("POST cmd = uploadImage")
    public UploadImageResponse postUploadImage(UploadImageRequest request) {
        return post(CMD.UPLOAD_IMAGE, request, UploadImageResponse.class);
    }

    @Step("Ресайз картинки")
    public AjaxResizeBannerImageResponse resizeBannerImage(UploadBannerImageResponse uploadImageResponse,
                                                           String ulogin, long campaignId, int x1, int x2, int y1,
                                                           int y2) {
        AjaxResizeBannerImageRequest request = AjaxResizeBannerImageRequest.
                forUploadedImage(uploadImageResponse).
                withCampaignId(campaignId).
                withX1(x1).
                withX2(x2).
                withY1(y1).
                withY2(y2).
                withUlogin(ulogin);
        return postAjaxResizeBannerImage(request);
    }

    @Step("POST cmd = ajaxResizeBannerImage")
    public AjaxResizeBannerImageResponse postAjaxResizeBannerImage(AjaxResizeBannerImageRequest request) {
        return post(CMD.AJAX_RESIZE_BANNER_IMAGE, request, AjaxResizeBannerImageResponse.class);
    }

    @Step("Загружаем картинку для баннера")
    public void uploadSomeNewImageForBanner(Banner banner) {
        NewImagesUploadHelper imagesUploadHelper = (NewImagesUploadHelper)
                new NewImagesUploadHelper().withBannerImageSteps(this);
        imagesUploadHelper.upload();
        imagesUploadHelper.fillBannerByUploadedImage(banner);
    }

    @Step("Загружаем картинку для баннера")
    public void uploadSomeImageForBanner(String ulogin, Long cid, Banner banner) {
        ImageUploadHelper imagesUploadHelper = (ImageUploadHelper) new ImageUploadHelper()
                .forCampaign(cid)
                .withImageParams(
                        new ImageParams()
                                .withFormat(ImageUtils.ImageFormat.JPG)
                                .withWidth(Integer.valueOf(DEFAULT_BANNER_IMAGE_FORMAT.getWidth()))
                                .withHeight(Integer.valueOf(DEFAULT_BANNER_IMAGE_FORMAT.getWidth()))

                )
                .withUploadType(AbstractImageUploadHelper.UploadType.FILE)
                .withBannerImageSteps(this)
                .withClient(ulogin);

        imagesUploadHelper.upload();
        imagesUploadHelper.fillBannerByUploadedImage(banner);
    }
}

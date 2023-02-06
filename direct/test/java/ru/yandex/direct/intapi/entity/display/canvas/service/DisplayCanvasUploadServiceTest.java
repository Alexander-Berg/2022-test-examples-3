package ru.yandex.direct.intapi.entity.display.canvas.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.creative.model.AdditionalData;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.StatusModerate;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.display.canvas.model.CreativeUploadData;
import ru.yandex.direct.intapi.entity.display.canvas.model.CreativeUploadResponse;
import ru.yandex.direct.intapi.entity.display.canvas.model.CreativeUploadResult;
import ru.yandex.direct.intapi.entity.display.canvas.model.CreativeUploadType;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFields;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCanvas;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmOutdoorVideoAddition;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultHtml5;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;

@IntApiTest
@RunWith(SpringRunner.class)
public class DisplayCanvasUploadServiceTest {
    private static final DefaultCompareStrategy COMPARE_STRATEGY = allFields()
            .forFields(newPath(Creative.ADDITIONAL_DATA.name(), AdditionalData.DURATION.name()))
            .useDiffer(new BigDecimalDiffer());

    @Autowired
    private DisplayCanvasAuthService displayCanvasAuthService;
    @Autowired
    private DisplayCanvasUploadService displayCanvasUploadService;
    @Autowired
    private CreativeRepository creativeRepository;
    @Autowired
    private Steps steps;

    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        displayCanvasAuthService.auth(clientInfo.getUid(), clientInfo.getClientId());
    }

    @Test
    public void uploadCreatives_CpmOutdoorCreative() {
        Long creativeId = steps.creativeSteps().getNextCreativeId();
        Creative creative = defaultCpmOutdoorVideoAddition(clientInfo.getClientId(), creativeId);
        CreativeUploadResponse creativeUploadResponse = displayCanvasUploadService
                .uploadCreatives(clientInfo.getClientId(),
                        singletonList(buildCreativeUploadData(creative, CreativeUploadType.VIDEO_ADDITION)));

        assertThat(creativeUploadResponse.getUploadResults().get(0), beanDiffer(CreativeUploadResult.ok(creativeId)));

        Creative actualCreative =
                creativeRepository.getCreatives(clientInfo.getShard(), singletonList(creativeId)).get(0);
        assertThat(actualCreative, beanDiffer(creative).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void uploadCreatives_imageCreative() {
        Long creativeId = steps.creativeSteps().getNextCreativeId();
        Creative creative = defaultCanvas(clientInfo.getClientId(), creativeId);
        CreativeUploadResponse creativeUploadResponse = displayCanvasUploadService
                .uploadCreatives(clientInfo.getClientId(),
                        singletonList(buildCreativeUploadData(creative, CreativeUploadType.IMAGE)));

        assertThat(creativeUploadResponse.getUploadResults().get(0), beanDiffer(CreativeUploadResult.ok(creativeId)));

        Creative actualCreative =
                creativeRepository.getCreatives(clientInfo.getShard(), singletonList(creativeId)).get(0);
        assertThat(actualCreative, beanDiffer(creative).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void uploadCreatives_html5Creative() {
        Long creativeId = steps.creativeSteps().getNextCreativeId();
        Creative creative = defaultHtml5(clientInfo.getClientId(), creativeId)
                .withExpandedPreviewUrl("http://exp.com");
        CreativeUploadResponse creativeUploadResponse = displayCanvasUploadService
                .uploadCreatives(clientInfo.getClientId(),
                        singletonList(buildCreativeUploadData(creative, CreativeUploadType.HTML5_CREATIVE)));

        assertThat(creativeUploadResponse.getUploadResults().get(0), beanDiffer(CreativeUploadResult.ok(creativeId)));

        Creative actualCreative =
                creativeRepository.getCreatives(clientInfo.getShard(), singletonList(creativeId)).get(0);
        assertThat(actualCreative, beanDiffer(creative).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void uploadCreatives_html5CreativeLarger() {
        Long creativeId = steps.creativeSteps().getNextCreativeId();
        var originalHeight = 602;
        var originalWidth = 500;
        Creative creative = defaultHtml5(clientInfo.getClientId(), creativeId)
                .withExpandedPreviewUrl("http://exp.com")
                .withAdditionalData(
                        new AdditionalData()
                                .withOriginalHeight(originalHeight)
                                .withOriginalWidth(originalWidth)
                );

        CreativeUploadResponse creativeUploadResponse = displayCanvasUploadService
                .uploadCreatives(
                        clientInfo.getClientId(),
                        singletonList(buildCreativeUploadData(creative, CreativeUploadType.HTML5_CREATIVE))
                );

        assertThat(creativeUploadResponse.getUploadResults().get(0), beanDiffer(CreativeUploadResult.ok(creativeId)));

        Creative actualCreative = creativeRepository
                .getCreatives(clientInfo.getShard(), singletonList(creativeId))
                .get(0);

        assertThat(actualCreative, beanDiffer(creative).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void uploadCreatives_imageCreative_adminReject() {
        Long creativeId = steps.creativeSteps().getNextCreativeId();
        Creative creative = defaultCanvas(clientInfo.getClientId(), creativeId);
        creative.getModerationInfo().setAdminRejectReason("someReason");
        creative.withStatusModerate(StatusModerate.ADMINREJECT);
        CreativeUploadResponse creativeUploadResponse = displayCanvasUploadService
                .uploadCreatives(clientInfo.getClientId(),
                        singletonList(buildCreativeUploadData(creative, CreativeUploadType.IMAGE)));

        assertThat(creativeUploadResponse.getUploadResults().get(0), beanDiffer(CreativeUploadResult.ok(creativeId)));

        Creative actualCreative =
                creativeRepository.getCreatives(clientInfo.getShard(), singletonList(creativeId)).get(0);
        assertThat(actualCreative, beanDiffer(creative).useCompareStrategy(COMPARE_STRATEGY));
    }

    private static CreativeUploadData buildCreativeUploadData(Creative creative, CreativeUploadType creativeType) {
        CreativeUploadData creativeUploadData = new CreativeUploadData();
        creativeUploadData.setCreativeId(creative.getId());
        creativeUploadData.setCreativeType(creativeType);
        creativeUploadData.setCreativeName(creative.getName());
        creativeUploadData.setPreviewUrl(creative.getPreviewUrl());
        creativeUploadData.setLivePreviewUrl(creative.getLivePreviewUrl());
        creativeUploadData.setExpandedPreviewUrl(creative.getExpandedPreviewUrl());
        creativeUploadData.setArchiveUrl(creative.getArchiveUrl());
        creativeUploadData.setWidth(ifNotNull(creative.getWidth(), Long::intValue));
        creativeUploadData.setHeight(ifNotNull(creative.getHeight(), Long::intValue));
        creativeUploadData.setStockCreativeId(creative.getStockCreativeId());
        creativeUploadData.setModerationInfo(creative.getModerationInfo());
        creativeUploadData.setYabsData(creative.getYabsData());
        creativeUploadData.setDuration(ifNotNull(creative.getDuration(), Long::doubleValue));
        creativeUploadData.setPresetId(ifNotNull(creative.getLayoutId(), Long::intValue));
        creativeUploadData.setAdditionalData(creative.getAdditionalData());
        creativeUploadData.setHasPackshot(creative.getHasPackshot());
        return creativeUploadData;
    }
}

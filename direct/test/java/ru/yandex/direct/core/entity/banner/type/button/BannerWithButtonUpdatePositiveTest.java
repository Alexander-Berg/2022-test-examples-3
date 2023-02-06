package ru.yandex.direct.core.entity.banner.type.button;


import java.time.LocalDateTime;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.container.BannerRepositoryContainer;
import ru.yandex.direct.core.entity.banner.model.BannerButtonStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithButton;
import ru.yandex.direct.core.entity.banner.model.ButtonAction;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerModifyRepository;
import ru.yandex.direct.core.entity.banner.service.BannersAddOperationFactory;
import ru.yandex.direct.core.entity.banner.type.BannerClientInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.i18n.I18NBundle;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.fullCpmBanner;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithButtonUpdatePositiveTest extends BannerClientInfoUpdateOperationTestBase {
    private static final String HREF = "https://ya.ru";
    private static final String HREF_1 = "https://yandex.ru";
    private static final String CAPTION_CUSTOM_TEXT = "Купить зайчиков";
    private static final String CAPTION_CUSTOM_TEXT_REGEXP = "Abc Абв 123";

    private LocalDateTime someTime;
    private Long creativeId1;
    private Long creativeId2;
    private AdGroupInfo adGroupInfo;
    private Integer shard;
    private BannerRepositoryContainer repositoryContainer;

    @Autowired
    public BannersAddOperationFactory addOperationFactory;

    @Autowired
    private BannerModifyRepository modifyRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public CpmBanner oldBanner;

    @Parameterized.Parameter(2)
    public BannerWithButton changes;

    @Parameterized.Parameter(3)
    public CpmBanner newBanner;

    //Изменение креатива для смены статуса модерации
    @Parameterized.Parameter(4)
    public Boolean changeCreative;

    @Parameterized.Parameter(5)
    public StatusBsSynced statusBsSynced;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "null -> null",
                        fullCpmBanner(null),
                        new CpmBanner(),
                        new CpmBanner(),
                        false,
                        StatusBsSynced.YES
                },
                {
                        "null -> download",
                        fullCpmBanner(null),
                        new CpmBanner().withButtonAction(ButtonAction.DOWNLOAD)
                                .withButtonHref(HREF),
                        new CpmBanner().withButtonAction(ButtonAction.DOWNLOAD)
                                .withButtonCaption("Скачать")
                                .withButtonHref(HREF)
                                .withButtonStatusModerate(BannerButtonStatusModerate.READY),
                        false,
                        StatusBsSynced.NO
                },
                {
                        "download -> null",
                        fullCpmBanner(null).withButtonAction(ButtonAction.DOWNLOAD)
                                .withButtonCaption("Скачать")
                                .withButtonHref(HREF)
                                .withButtonStatusModerate(BannerButtonStatusModerate.YES),
                        new CpmBanner(),
                        new CpmBanner(),
                        false,
                        StatusBsSynced.NO
                },
                {
                        "download -> download",
                        fullCpmBanner(null).withButtonAction(ButtonAction.DOWNLOAD)
                                .withButtonCaption("Скачать")
                                .withButtonHref(HREF)
                                .withButtonStatusModerate(BannerButtonStatusModerate.YES),
                        new CpmBanner().withButtonAction(ButtonAction.DOWNLOAD)
                                .withButtonHref(HREF),
                        new CpmBanner().withButtonAction(ButtonAction.DOWNLOAD)
                                .withButtonCaption("Скачать")
                                .withButtonHref(HREF)
                                .withButtonStatusModerate(BannerButtonStatusModerate.YES),
                        false,
                        StatusBsSynced.YES
                },
                {
                        "download -> download со сменой урла",
                        fullCpmBanner(null).withButtonAction(ButtonAction.DOWNLOAD)
                                .withButtonCaption("Скачать")
                                .withButtonHref(HREF)
                                .withButtonStatusModerate(BannerButtonStatusModerate.YES),
                        new CpmBanner().withButtonAction(ButtonAction.DOWNLOAD)
                                .withButtonHref(HREF_1),
                        new CpmBanner().withButtonAction(ButtonAction.DOWNLOAD)
                                .withButtonCaption("Скачать")
                                .withButtonHref(HREF_1)
                                .withButtonStatusModerate(BannerButtonStatusModerate.READY),
                        false,
                        StatusBsSynced.NO
                },
                {
                        "download -> download с перемодерацией баннера",
                        fullCpmBanner(null).withButtonAction(ButtonAction.DOWNLOAD)
                                .withButtonCaption("Скачать")
                                .withButtonHref(HREF)
                                .withButtonStatusModerate(BannerButtonStatusModerate.YES),
                        new CpmBanner().withButtonAction(ButtonAction.DOWNLOAD)
                                .withButtonHref(HREF),
                        new CpmBanner().withButtonAction(ButtonAction.DOWNLOAD)
                                .withButtonCaption("Скачать")
                                .withButtonHref(HREF)
                                .withButtonStatusModerate(BannerButtonStatusModerate.READY),
                        true,
                        StatusBsSynced.NO
                },
                {
                        "download -> buy",
                        fullCpmBanner(null).withButtonAction(ButtonAction.DOWNLOAD)
                                .withButtonCaption("Скачать")
                                .withButtonHref(HREF)
                                .withButtonStatusModerate(BannerButtonStatusModerate.YES),
                        new CpmBanner().withButtonAction(ButtonAction.BUY)
                                .withButtonHref(HREF_1),
                        new CpmBanner().withButtonAction(ButtonAction.BUY)
                                .withButtonCaption("Купить")
                                .withButtonHref(HREF_1)
                                .withButtonStatusModerate(BannerButtonStatusModerate.READY),
                        false,
                        StatusBsSynced.NO
                },
                {
                        "null -> custom",
                        fullCpmBanner(null),
                        new CpmBanner().withButtonAction(ButtonAction.CUSTOM_TEXT)
                                .withButtonCaption(CAPTION_CUSTOM_TEXT)
                                .withButtonHref(HREF),
                        new CpmBanner().withButtonAction(ButtonAction.CUSTOM_TEXT)
                                .withButtonCaption(CAPTION_CUSTOM_TEXT)
                                .withButtonHref(HREF)
                                .withButtonStatusModerate(BannerButtonStatusModerate.READY),
                        false,
                        StatusBsSynced.NO
                },
                {
                        "download -> custom",
                        fullCpmBanner(null).withButtonAction(ButtonAction.DOWNLOAD)
                                .withButtonCaption("Скачать")
                                .withButtonHref(HREF)
                                .withButtonStatusModerate(BannerButtonStatusModerate.YES),
                        new CpmBanner().withButtonAction(ButtonAction.CUSTOM_TEXT)
                                .withButtonCaption(CAPTION_CUSTOM_TEXT)
                                .withButtonHref(HREF_1),
                        new CpmBanner().withButtonAction(ButtonAction.CUSTOM_TEXT)
                                .withButtonCaption(CAPTION_CUSTOM_TEXT)
                                .withButtonHref(HREF_1)
                                .withButtonStatusModerate(BannerButtonStatusModerate.READY),
                        false,
                        StatusBsSynced.NO
                },
                {
                        "custom -> download",
                        fullCpmBanner(null).withButtonAction(ButtonAction.CUSTOM_TEXT)
                                .withButtonCaption(CAPTION_CUSTOM_TEXT)
                                .withButtonHref(HREF)
                                .withButtonStatusModerate(BannerButtonStatusModerate.YES),
                        new CpmBanner().withButtonAction(ButtonAction.DOWNLOAD)
                                .withButtonHref(HREF_1),
                        new CpmBanner().withButtonAction(ButtonAction.DOWNLOAD).withButtonCaption("Скачать")
                                .withButtonHref(HREF_1)
                                .withButtonStatusModerate(BannerButtonStatusModerate.READY),
                        false,
                        StatusBsSynced.NO
                },
                {
                        "custom -> null",
                        fullCpmBanner(null).withButtonAction(ButtonAction.CUSTOM_TEXT)
                                .withButtonCaption(CAPTION_CUSTOM_TEXT)
                                .withButtonHref(HREF)
                                .withButtonStatusModerate(BannerButtonStatusModerate.READY),
                        new CpmBanner(),
                        new CpmBanner(),
                        false,
                        StatusBsSynced.NO
                },
                {
                        "custom -> custom",
                        fullCpmBanner(null).withButtonAction(ButtonAction.CUSTOM_TEXT)
                                .withButtonCaption(CAPTION_CUSTOM_TEXT)
                                .withButtonHref(HREF)
                                .withButtonStatusModerate(BannerButtonStatusModerate.YES),
                        new CpmBanner().withButtonAction(ButtonAction.CUSTOM_TEXT)
                                .withButtonCaption(CAPTION_CUSTOM_TEXT)
                                .withButtonHref(HREF),
                        new CpmBanner().withButtonAction(ButtonAction.CUSTOM_TEXT)
                                .withButtonCaption(CAPTION_CUSTOM_TEXT)
                                .withButtonHref(HREF)
                                .withButtonStatusModerate(BannerButtonStatusModerate.YES),
                        false,
                        StatusBsSynced.YES
                },
                {
                        "custom -> custom со сменой урла",
                        fullCpmBanner(null).withButtonAction(ButtonAction.CUSTOM_TEXT)
                                .withButtonCaption(CAPTION_CUSTOM_TEXT)
                                .withButtonHref(HREF)
                                .withButtonStatusModerate(BannerButtonStatusModerate.YES),
                        new CpmBanner().withButtonAction(ButtonAction.CUSTOM_TEXT)
                                .withButtonCaption(CAPTION_CUSTOM_TEXT)
                                .withButtonHref(HREF_1),
                        new CpmBanner().withButtonAction(ButtonAction.CUSTOM_TEXT)
                                .withButtonCaption(CAPTION_CUSTOM_TEXT)
                                .withButtonHref(HREF_1)
                                .withButtonStatusModerate(BannerButtonStatusModerate.READY),
                        false,
                        StatusBsSynced.NO
                },
                {
                        "custom -> custom со сменой заголовка",
                        fullCpmBanner(null).withButtonAction(ButtonAction.CUSTOM_TEXT)
                                .withButtonCaption(CAPTION_CUSTOM_TEXT)
                                .withButtonHref(HREF)
                                .withButtonStatusModerate(BannerButtonStatusModerate.YES),
                        new CpmBanner().withButtonAction(ButtonAction.CUSTOM_TEXT)
                                .withButtonCaption(CAPTION_CUSTOM_TEXT_REGEXP)
                                .withButtonHref(HREF_1),
                        new CpmBanner().withButtonAction(ButtonAction.CUSTOM_TEXT)
                                .withButtonCaption(CAPTION_CUSTOM_TEXT_REGEXP)
                                .withButtonHref(HREF_1)
                                .withButtonStatusModerate(BannerButtonStatusModerate.READY),
                        false,
                        StatusBsSynced.NO
                },
        });
    }

    @Before
    public void before() throws Exception {
        LocaleContextHolder.setLocale(I18NBundle.RU);
        new TestContextManager(getClass()).prepareTestInstance(this);
        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        clientInfo = adGroupInfo.getClientInfo();
        creativeId1 = steps.creativeSteps().getNextCreativeId();
        creativeId2 = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultHtml5Creative(clientInfo, creativeId1);
        steps.creativeSteps().addDefaultHtml5Creative(clientInfo, creativeId2);
        someTime = LocalDateTime.now().minusMinutes(7).withNano(0);
        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(), FeatureName.ASSET_BUTTON_CUSTOM_TEXT, true);
        shard = adGroupInfo.getShard();
        repositoryContainer = new BannerRepositoryContainer(shard);
    }

    @Test
    public void update() {
        oldBanner
                .withCampaignId(adGroupInfo.getCampaignId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCreativeId(creativeId1)
                .withStatusModerate(BannerStatusModerate.YES)
                .withLastChange(someTime);

        Long bannerId = modifyRepository.add(dslContextProvider.ppc(shard), repositoryContainer,
                singletonList(oldBanner)).get(0);

        ModelChanges<CpmBanner> modelChanges = new ModelChanges<>(bannerId, CpmBanner.class);
        modelChanges.process(changes.getButtonAction(), CpmBanner.BUTTON_ACTION);
        modelChanges.process(changes.getButtonCaption(), CpmBanner.BUTTON_CAPTION);
        modelChanges.process(changes.getButtonHref(), CpmBanner.BUTTON_HREF);

        if (changeCreative) {
            modelChanges.process(creativeId2, CpmBanner.CREATIVE_ID);
        }

        prepareAndApplyValid(modelChanges);

        CpmBanner actualBanner = getBanner(bannerId);
        assertThat(actualBanner.getButtonAction()).isEqualTo(newBanner.getButtonAction());
        assertThat(actualBanner.getButtonCaption()).isEqualTo(newBanner.getButtonCaption());
        assertThat(actualBanner.getButtonHref()).isEqualTo(newBanner.getButtonHref());
        assertThat(actualBanner.getButtonStatusModerate()).isEqualTo(newBanner.getButtonStatusModerate());
        assertThat(actualBanner.getStatusModerate())
                .isEqualTo(changeCreative ? BannerStatusModerate.READY : BannerStatusModerate.YES);
        assertThat(actualBanner.getStatusBsSynced()).isEqualTo(statusBsSynced);
        if (statusBsSynced == StatusBsSynced.YES) {
            assertThat(actualBanner.getLastChange()).isEqualTo(someTime);
        } else {
            assertThat(actualBanner.getLastChange()).isNotEqualTo(someTime);
        }
    }
}

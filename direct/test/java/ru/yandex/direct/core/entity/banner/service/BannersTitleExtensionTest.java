package ru.yandex.direct.core.entity.banner.service;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannersTitleExtensionTest {
    @Autowired
    private Steps steps;

    @Autowired
    private BannersUpdateOperationFactory bannersUpdateOperationFactory;

    @Autowired
    private BannerService bannerService;

    private long operatorUid;
    private ClientId clientId;
    private Long bannerId;

    @Before
    public void before() {
        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(activeTextBanner().withTitleExtension(""));
        ClientInfo clientInfo = bannerInfo.getClientInfo();
        bannerId = bannerInfo.getBannerId();

        operatorUid = clientInfo.getUid();
        clientId = clientInfo.getClientId();
    }

    @Test
    public void titleExtensionIsEmptyStringInDb_updateOfTheOtherFieldSucceeds() {
        ModelChanges<BannerWithSystemFields> modelChanges =
                ModelChanges.build(bannerId, TextBanner.class, TextBanner.TITLE, "New title")
                        .castModelUp(BannerWithSystemFields.class);
        MassResult<Long> result = createUpdateOperation(singletonList(modelChanges), operatorUid).prepareAndApply();

        assertThat(result, isSuccessful(true));
    }

    @Test
    public void titleExtensionIsEmptyStringInDb_getReturnsNullAsTitleExtension() {
        List<BannerWithSystemFields> banners = bannerService.getBannersByIds(singletonList(bannerId));
        checkState(banners.size() == 1, "Должен вернуться один баннер");
        TextBanner banner = (TextBanner) banners.get(0);

        assertThat(banner.getTitleExtension(), equalTo(null));
    }

    private BannersUpdateOperation createUpdateOperation(List<ModelChanges<BannerWithSystemFields>> modelChangesList,
                                                         long operatorUid) {

        return bannersUpdateOperationFactory.createPartialUpdateOperation(modelChangesList, operatorUid, clientId);
    }
}

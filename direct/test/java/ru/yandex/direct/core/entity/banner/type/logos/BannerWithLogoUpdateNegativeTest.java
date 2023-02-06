package ru.yandex.direct.core.entity.banner.type.logos;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithLogo;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.service.BannersAddOperation;
import ru.yandex.direct.core.entity.banner.service.BannersAddOperationFactory;
import ru.yandex.direct.core.entity.banner.type.BannerClientInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;

import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.imageNotFound;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithLogoUpdateNegativeTest extends BannerClientInfoUpdateOperationTestBase {
    @Autowired
    public BannersAddOperationFactory addOperationFactory;

    private CreativeInfo creativeInfo;
    private AdGroupInfo adGroupInfo;

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        clientInfo = adGroupInfo.getClientInfo();
        creativeInfo = steps.creativeSteps().addDefaultHtml5Creative(clientInfo,
                steps.creativeSteps().getNextCreativeId());
    }

    @Test
    public void update() {
        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId());

        BannersAddOperation operation = addOperationFactory.createAddOperation(Applicability.FULL,
                false, List.of(banner),
                adGroupInfo.getShard(), adGroupInfo.getClientId(), adGroupInfo.getUid(), false, false, false, false,
                featureService.getEnabledForClientId(adGroupInfo.getClientId()));

        Long bannerId = operation.prepareAndApply().get(0).getResult();

        ModelChanges<CpmBanner> modelChanges = ModelChanges.build(bannerId, CpmBanner.class,
                CpmBanner.LOGO_IMAGE_HASH, "123");

        var validationResult = prepareAndApplyInvalid(modelChanges);

        Assert.assertThat(validationResult, hasDefectWithDefinition(validationError(
                path(field(BannerWithLogo.LOGO_IMAGE_HASH.name())),
                imageNotFound())));
    }
}

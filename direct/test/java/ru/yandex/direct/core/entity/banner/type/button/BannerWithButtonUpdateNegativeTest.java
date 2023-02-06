package ru.yandex.direct.core.entity.banner.type.button;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.service.BannersAddOperation;
import ru.yandex.direct.core.entity.banner.service.BannersAddOperationFactory;
import ru.yandex.direct.core.entity.banner.type.BannerClientInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectInfo;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithButtonUpdateNegativeTest extends BannerClientInfoUpdateOperationTestBase {
    private CreativeInfo creativeInfo;
    private AdGroupInfo adGroupInfo;

    @Autowired
    public BannersAddOperationFactory addOperationFactory;

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public Map<ModelProperty, Object> properties;

    @Parameterized.Parameter(2)
    public Boolean customTextAllowed;

    @Parameterized.Parameter(3)
    public Set<Matcher<DefectInfo<Defect>>> errors;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return BannerWithButtonAddNegativeTest.parameters();
    }

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        clientInfo = adGroupInfo.getClientInfo();
        creativeInfo = steps.creativeSteps().addDefaultHtml5Creative(adGroupInfo.getClientInfo(),
                steps.creativeSteps().getNextCreativeId());
        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(), FeatureName.ASSET_BUTTON_CUSTOM_TEXT,
                customTextAllowed);
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

        ModelChanges<CpmBanner> modelChanges = new ModelChanges<>(bannerId, CpmBanner.class);
        for (Map.Entry<ModelProperty, Object> property : properties.entrySet()) {
            modelChanges.process(property.getValue(), property.getKey());
        }

        var vr = prepareAndApplyInvalid(modelChanges);

        assertThat(vr.flattenErrors(), hasSize(errors.size()));
        for (Matcher<DefectInfo<Defect>> matcher : errors) {
            Assert.assertThat(vr, hasDefectWithDefinition(matcher));
        }
    }
}

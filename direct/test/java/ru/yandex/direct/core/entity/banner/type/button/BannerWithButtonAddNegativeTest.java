package ru.yandex.direct.core.entity.banner.type.button;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.banner.model.ButtonAction;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectInfo;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.hasSize;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.invalidHref;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.restrictedCharsInField;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.unsupportedButtonAction;
import static ru.yandex.direct.core.entity.banner.type.button.BannerWithButtonConstants.CAPTION_MAX_LENGTH;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxStringLength;
import static ru.yandex.direct.validation.defect.CommonDefects.isNull;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.StringDefects.notEmptyString;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithButtonAddNegativeTest extends BannerAdGroupInfoAddOperationTestBase {
    private static final String HREF = "https://yandex.ru";
    private static final String CAPTION_CUSTOM_TEXT = "Купить зайчиков";

    private CreativeInfo creativeInfo;

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public Map<ModelProperty, Object> properties;

    @Parameterized.Parameter(2)
    public Boolean customTextAllowed;

    @Parameterized.Parameter(3)
    public Set<Matcher<DefectInfo<Defect>>> errors;

    // BannerWithButtonUpdateNegativeTest тоже берет параметры из этого метода
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "empty href",
                        Map.of(
                                CpmBanner.BUTTON_ACTION, ButtonAction.DOWNLOAD
                        ),
                        true,
                        singleton(validationError(path(field(CpmBanner.BUTTON_HREF.name())), notNull()))
                },
                {
                        "invalid href",
                        Map.of(
                                CpmBanner.BUTTON_ACTION, ButtonAction.DOWNLOAD,
                                CpmBanner.BUTTON_HREF, "abc"
                        ),
                        false,
                        singleton(validationError(path(field(CpmBanner.BUTTON_HREF.name())), invalidHref()))
                },
                {
                        "caption for common actions is not allowed",
                        Map.of(
                                CpmBanner.BUTTON_ACTION, ButtonAction.DOWNLOAD,
                                CpmBanner.BUTTON_CAPTION, CAPTION_CUSTOM_TEXT,
                                CpmBanner.BUTTON_HREF, HREF
                        ),
                        true,
                        singleton(validationError(path(field(CpmBanner.BUTTON_CAPTION.name())), isNull()))
                },
                {
                        "custom text is not allowed",
                        Map.of(
                                CpmBanner.BUTTON_ACTION, ButtonAction.CUSTOM_TEXT,
                                CpmBanner.BUTTON_CAPTION, CAPTION_CUSTOM_TEXT,
                                CpmBanner.BUTTON_HREF, HREF
                        ),
                        false,
                        singleton(validationError(path(field(CpmBanner.BUTTON_ACTION.name())),
                                unsupportedButtonAction()))
                },
                {
                        "caption for custom text should not be null",
                        Map.of(
                                CpmBanner.BUTTON_ACTION, ButtonAction.CUSTOM_TEXT,
                                CpmBanner.BUTTON_HREF, HREF
                        ),
                        true,
                        singleton(validationError(path(field(CpmBanner.BUTTON_CAPTION.name())), notNull()))
                },
                {
                        "caption for custom text should not be blank",
                        Map.of(
                                CpmBanner.BUTTON_ACTION, ButtonAction.CUSTOM_TEXT,
                                CpmBanner.BUTTON_CAPTION, " ",
                                CpmBanner.BUTTON_HREF, HREF
                        ),
                        true,
                        singleton(validationError(path(field(CpmBanner.BUTTON_CAPTION.name())), notEmptyString()))
                },
                {
                        "restricted chars in caption",
                        Map.of(
                                CpmBanner.BUTTON_ACTION, ButtonAction.CUSTOM_TEXT,
                                CpmBanner.BUTTON_CAPTION, "Купить лису + зайца",
                                CpmBanner.BUTTON_HREF, HREF
                        ),
                        true,
                        singleton(validationError(path(field(CpmBanner.BUTTON_CAPTION.name())),
                                restrictedCharsInField()))
                },
                {
                        "too many chars in caption",
                        Map.of(
                                CpmBanner.BUTTON_ACTION, ButtonAction.CUSTOM_TEXT,
                                CpmBanner.BUTTON_CAPTION, "Купить очень много лисичек",
                                CpmBanner.BUTTON_HREF, HREF
                        ),
                        true,
                        singleton(validationError(path(field(CpmBanner.BUTTON_CAPTION.name())),
                                maxStringLength(CAPTION_MAX_LENGTH)))
                },
                {
                        "several errors",
                        Map.of(
                                CpmBanner.BUTTON_ACTION, ButtonAction.CUSTOM_TEXT,
                                CpmBanner.BUTTON_CAPTION, "_____________________",
                                CpmBanner.BUTTON_HREF, "abc"
                        ),
                        true,
                        asSet(
                                validationError(path(field(CpmBanner.BUTTON_CAPTION.name())),
                                        restrictedCharsInField()),
                                validationError(path(field(CpmBanner.BUTTON_CAPTION.name())),
                                        maxStringLength(CAPTION_MAX_LENGTH)),
                                validationError(path(field(CpmBanner.BUTTON_HREF.name())), invalidHref())
                        )
                },
        });
    }

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        creativeInfo = steps.creativeSteps().addDefaultHtml5Creative(adGroupInfo.getClientInfo(),
                steps.creativeSteps().getNextCreativeId());
        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(), FeatureName.ASSET_BUTTON_CUSTOM_TEXT,
                customTextAllowed);
    }

    @Test
    public void add() {
        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId());

        for (Map.Entry<ModelProperty, Object> property : properties.entrySet()) {
            property.getKey().set(banner, property.getValue());
        }

        var vr = prepareAndApplyInvalid(banner);

        assertThat(vr.flattenErrors(), hasSize(errors.size()));
        for (Matcher<DefectInfo<Defect>> matcher : errors) {
            Assert.assertThat(vr, hasDefectWithDefinition(matcher));
        }
    }
}

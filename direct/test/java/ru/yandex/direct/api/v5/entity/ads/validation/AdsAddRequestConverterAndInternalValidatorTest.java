package ru.yandex.direct.api.v5.entity.ads.validation;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.ads.AdAddItem;
import com.yandex.direct.api.v5.ads.AddRequest;
import com.yandex.direct.api.v5.ads.VideoExtensionAddItem;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.api.v5.entity.ads.converter.AdsAddRequestConverter;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.core.entity.adgroup.container.AccessibleAdGroupTypes;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.api.v5.entity.ads.AdsAddTestData.filledDynamicAd;
import static ru.yandex.direct.api.v5.entity.ads.AdsAddTestData.filledTextAd;
import static ru.yandex.direct.api.v5.validation.Matchers.defectTypeWith;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class AdsAddRequestConverterAndInternalValidatorTest {

    private static final ClientId CLIENT_ID = ClientId.fromLong(1L);

    private AdsAddRequestConverter converter;
    private AdsAddRequestValidator validator;

    @Before
    public void before() {
        validator = new AdsAddRequestValidator(mock(AdGroupService.class), mock(BannerTypedRepository.class),
                mock(ShardHelper.class));
        converter = new AdsAddRequestConverter();
    }

    @Test
    @Parameters(method = "params")
    @TestCaseName("{method}: {0}, {2}")
    public void converterAndInternalValidatorIntegration(String desc, AdAddItem adAddItem, int expectedErrorCode) {
        assertThat(convertAndValidateInternal(new AddRequest().withAds(adAddItem)))
                .has(defectTypeWith(validationError(path(index(0)), expectedErrorCode)));
    }

    private ValidationResult<List<BannerWithAdGroupId>, DefectType> convertAndValidateInternal(AddRequest request) {
        String allowedAdTypes = AdTypeNames.getApi5AllowedAdTypes();
        return validator.validateInternalRequest(CLIENT_ID, converter.convert(request),
                AccessibleAdGroupTypes.API5_ALLOWED_AD_GROUP_TYPES, allowedAdTypes);
    }

    Iterable<Object[]> params() {
        return asList(new Object[][]{
                {"VideoExtension without ID",
                        new AdAddItem().withTextAd(filledTextAd().withVideoExtension(new VideoExtensionAddItem())),
                        5008},
                {"No ad type specified",
                        new AdAddItem(),
                        5008},
                {"Ambiguous ad type",
                        new AdAddItem().withTextAd(filledTextAd()).withDynamicTextAd(filledDynamicAd()),
                        5009},
        });
    }

}

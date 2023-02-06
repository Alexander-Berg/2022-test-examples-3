package ru.yandex.direct.excel.processing.service.internalad;

import java.util.Collections;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.banner.model.InternalBanner;
import ru.yandex.direct.excel.processing.model.internalad.InternalBannerRepresentation;
import ru.yandex.direct.excel.processing.model.internalad.SheetDescriptor;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.excel.processing.utils.InternalTestDataKt.getDefaultSheetDescriptor;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@ParametersAreNonnullByDefault
public class InternalAdExcelImportServiceMethodTest {

    private SheetDescriptor sheetDescriptor;
    private InternalBannerRepresentation representation;

    @Before
    public void initTestData() {
        sheetDescriptor = getDefaultSheetDescriptor();
        representation = new InternalBannerRepresentation()
                .setBanner(new InternalBanner()
                        .withAdGroupId(RandomNumberUtils.nextPositiveLong())
                        .withStatusShow(true));
    }


    @Test
    public void checkExtractAndEnrichInternalBanners() {
        InternalBanner banner = InternalAdExcelImportService
                .extractAndEnrichInternalBanner(sheetDescriptor, representation);
        InternalBanner expectedBanner = getExpectedBanner(sheetDescriptor, representation);

        assertThat(banner)
                .is(matchedBy(beanDiffer(expectedBanner)));
    }

    private static InternalBanner getExpectedBanner(SheetDescriptor sheetDescriptor,
                                                    InternalBannerRepresentation representation) {
        return new InternalBanner()
                .withId(representation.getBanner().getId())
                .withAdGroupId(representation.getAdGroupId())
                .withCampaignId(sheetDescriptor.getCampaignId())
                .withTemplateId(sheetDescriptor.getTemplateId())
                .withTemplateVariables(Collections.emptyList())
                .withStatusShow(true);
    }

}

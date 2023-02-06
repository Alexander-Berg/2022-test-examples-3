package ru.yandex.direct.grid.processing.service.banner;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAdsMassAction;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAdsMassActionPayload;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdCopyAdsInput;

import static java.util.Collections.emptyList;

@GridProcessingTest
@RunWith(SpringRunner.class)
public class AdMassActionsServiceTest {

    @Autowired
    Steps steps;

    @Autowired
    AdMassActionsService serviceUnderTest;

    private ClientId clientId;
    private Long operatorUid;

    public final static GdAdsMassActionPayload EMPTY_MASS_ACTION_PAYLOAD =
            new GdAdsMassActionPayload()
                    .withProcessedAdIds(emptyList())
                    .withSkippedAdIds(emptyList())
                    .withSuccessCount(0)
                    .withTotalCount(0)
                    .withValidationResult(null);

    @Before
    public void setUp() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        operatorUid = clientInfo.getUid();
    }

    @Test
    public void deleteBanners_success_onEmptyItemList() {
        GdAdsMassAction input = new GdAdsMassAction()
                .withAdIds(emptyList());

        GdAdsMassActionPayload result = serviceUnderTest.deleteBanners(clientId, operatorUid, input);
        checkEmpty(result);
    }

    @Test
    public void copyBanners_success_onEmptyItemList() {
        GdCopyAdsInput input = new GdCopyAdsInput()
                .withAdIds(emptyList());

        checkEmpty(serviceUnderTest.copyAds(clientId, operatorUid, input));
    }

    public static void checkEmpty(GdAdsMassActionPayload result) {
        checkResult(result, EMPTY_MASS_ACTION_PAYLOAD);
    }

    public static void checkResult(GdAdsMassActionPayload result, GdAdsMassActionPayload expected) {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult())
                    .as("validationResult")
                    .isEqualTo(expected.getValidationResult());
            softly.assertThat(result.getProcessedAdIds())
                    .as("processedAdIds")
                    .isNotNull()
                    .isEqualTo(expected.getProcessedAdIds());
            softly.assertThat(result.getSkippedAdIds())
                    .as("skippedAdIds")
                    .isNotNull()
                    .isEqualTo(expected.getSkippedAdIds());
            softly.assertThat(result.getSuccessCount())
                    .as("successCount")
                    .isEqualTo(expected.getSuccessCount());
            softly.assertThat(result.getTotalCount())
                    .as("totalCount")
                    .isEqualTo(expected.getTotalCount());
        });
    }
}

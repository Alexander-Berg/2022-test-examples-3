package ru.yandex.direct.grid.processing.service.feed;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.feed.model.UpdateStatus;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.direct.grid.processing.service.feed.FeedConverter.convertToGdFeedAccess;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class FeedConverterAccessTest {


    @SuppressWarnings("unused")
    private Object[] parameters() {
        return new Object[][]{
                {"Can write, no campaigns, status DONE", UpdateStatus.DONE, true, true, true, true},
                {"Can write, no campaigns, status ERROR", UpdateStatus.ERROR, true, true, true, true},
                {"Can write, no campaigns, status NEW", UpdateStatus.NEW, true, true, true, false},
                {"Can write, no campaigns, status UPDATING", UpdateStatus.UPDATING, true, true, true, false},
                {"Can write, no campaigns, status OUTDATED", UpdateStatus.OUTDATED, true, true, true, false},
                {"Can't write, no campaigns, status DONE", UpdateStatus.DONE, true, false, false, false},
                {"Can write, has campaigns, status DONE", UpdateStatus.DONE, false, true, true, false},
                {"Can write, has campaigns, status NEW", UpdateStatus.NEW, false, true, true, false},
        };
    }

    @Test
    @Parameters(method = "parameters")
    @TestCaseName("{0}, expectedCanEdit = {4}, expectedCanDelete = {5}")
    public void checkGetCampaignFeatures(@SuppressWarnings("unused") String description,
                                         UpdateStatus updateStatus, boolean notUsedInCampaigns,
                                         boolean operatorCanWrite, boolean expectedCanEdit, boolean expectedCanDelete) {
        var actual = convertToGdFeedAccess(updateStatus, notUsedInCampaigns, operatorCanWrite);

        assertSoftly(softly -> {
            softly.assertThat(actual.getCanEdit()).isEqualTo(expectedCanEdit);
            softly.assertThat(actual.getCanDelete()).isEqualTo(expectedCanDelete);
        });
    }

}

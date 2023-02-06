package ru.yandex.direct.jobs.bannersystem.export.service;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.testing.info.MobileContentInfo;
import ru.yandex.direct.core.testing.steps.MobileContentSteps;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestMobileContents.defaultMobileContent;

@JobsTest
@ExtendWith(SpringExtension.class)
class BsExportMobileContentServiceTest {
    @Autowired
    private MobileContentSteps mobileContentSteps;

    @Autowired
    private BsExportMobileContentService bsExportMobileContentService;

    /**
     * Проверяем, что не получаем в списке id к отправке мобильный контент с StatusBsSynced.Yes
     */
    @Test
    void testGetMobileContentIdsForBsExport() {
        MobileContentInfo mobileContentInfoYes = mobileContentSteps.createMobileContent(
                new MobileContentInfo()
                        .withMobileContent(defaultMobileContent()
                                .withStatusBsSynced(StatusBsSynced.YES))
        );
        MobileContentInfo mobileContentInfoSending = mobileContentSteps.createMobileContent(
                new MobileContentInfo()
                        .withMobileContent(defaultMobileContent()
                                .withStatusBsSynced(StatusBsSynced.SENDING))
        );
        MobileContentInfo mobileContentInfoNo = mobileContentSteps.createMobileContent(
                new MobileContentInfo()
                        .withMobileContent(defaultMobileContent()
                                .withStatusBsSynced(StatusBsSynced.NO))
        );

        List<Long> mobileContentIds =
                bsExportMobileContentService.getMobileContentIdsForBsExport(mobileContentInfoYes.getShard());

        assertThat(mobileContentIds)
                .as("Не получили нужный контент")
                .contains(mobileContentInfoNo.getMobileContentId(), mobileContentInfoSending.getMobileContentId())
                .doesNotContain(mobileContentInfoYes.getMobileContentId());
    }
}

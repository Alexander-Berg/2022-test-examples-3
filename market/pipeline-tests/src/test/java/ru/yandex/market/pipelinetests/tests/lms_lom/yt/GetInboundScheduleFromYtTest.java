package ru.yandex.market.pipelinetests.tests.lms_lom.yt;

import java.util.List;

import io.qameta.allure.Epic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;

import ru.yandex.market.logistics.management.entity.request.schedule.LogisticSegmentInboundScheduleFilter;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.pipelinetests.tests.lms_lom.GetInboundScheduleAbstractTest;
import ru.yandex.market.pipelinetests.tests.lms_lom.utils.ScheduleDayCompareUtils;

@Epic("Lms Lom Redis")
@Tag("LmsLomRedisSyncTest")
@DisplayName("Синхронизация данных LMS в YT")
public class GetInboundScheduleFromYtTest extends GetInboundScheduleAbstractTest {

    @Override
    public void sendRequestsAndCompareResponses(LogisticSegmentInboundScheduleFilter filter) {
        List<ScheduleDayResponse> lmsSchedules = LMS_STEPS.getInboundSchedule(filter).getEntities();
        List<ScheduleDayResponse> ytSchedules = LOM_LMS_YT_STEPS.searchInboundSchedule(filter);

        ScheduleDayCompareUtils.compareSchedules(softly, lmsSchedules, ytSchedules);
    }

}

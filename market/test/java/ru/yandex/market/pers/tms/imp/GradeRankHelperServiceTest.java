package ru.yandex.market.pers.tms.imp;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.pers.grade.core.model.vote.GradeVoteRate;
import ru.yandex.market.pers.grade.core.service.GradeRankHelperService;
import ru.yandex.market.pers.tms.yt.dumper.dumper.YtExportHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

public class GradeRankHelperServiceTest extends MockedPersTmsTest {
    @Autowired
    private YtExportHelper ytExportHelper;
    @Autowired
    private YtImportProcessor ytImportProcessor;
    @Autowired
    private GradeRankHelperService gradeRankHelperService;

    @Test
    public void test() {
        List<GradeVoteRate> expectedRates = Arrays.asList(
            new GradeVoteRate(1, 1, 2),
            new GradeVoteRate(2, 2, 1),
            new GradeVoteRate(3, 2, 4),
            new GradeVoteRate(4, 4, 2),
            new GradeVoteRate(5, 8, 4)
        );
        doReturn(expectedRates).when(ytExportHelper.getHahnYtClient()).read(any(), any());

        ytImportProcessor.updateVoteRates();

        Map<Integer, GradeVoteRate> actualRates = gradeRankHelperService.getGradeVoteRates();

        Assert.assertArrayEquals(expectedRates.toArray(), actualRates.values().toArray(new GradeVoteRate[0]));
    }
}

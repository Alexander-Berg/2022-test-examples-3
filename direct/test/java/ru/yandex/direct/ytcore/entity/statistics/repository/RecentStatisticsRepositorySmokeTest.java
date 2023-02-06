package ru.yandex.direct.ytcore.entity.statistics.repository;

import java.time.LocalDate;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.ytcomponents.statistics.model.DateRange;
import ru.yandex.direct.ytcomponents.statistics.model.PhraseStatisticsRequest;
import ru.yandex.direct.ytcomponents.statistics.model.PhraseStatisticsResponse;
import ru.yandex.direct.ytcore.spring.YtCoreConfiguration;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.types.Unsigned.ulong;

@Ignore("For manual run")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = YtCoreConfiguration.class)
public class RecentStatisticsRepositorySmokeTest {

    @Autowired
    private RecentStatisticsRepository recentStatisticsRepository;

    @Test
    public void recentStatSmokeTest_withEmptyResult() {
        List<PhraseStatisticsRequest> requests =
                singletonList(new PhraseStatisticsRequest.Builder()
                        .withCampaignId(1L)
                        .withAdGroupId(1L)
                        .withBsPhraseId(ulong(1L))
                        .build());
        List<PhraseStatisticsResponse> actual =
                recentStatisticsRepository.getPhraseStatistics(requests, getDateRange());
        assertThat(actual).hasSize(0);
    }

    @Test
    public void recentStatSmokeTest_withNonEmptyResult() {
        /*
        YQL запрос для полученя фраз со статистикой:
        select distinct S.ExportID, S.GroupExportID, PhraseID
        from zeno.[home/yabs/stat/DirectPhraseStat] as S
        where S.UpdateTime > 1517788800
        limit 100

        UpdateTime можно взять на неделю раньше текущей даты, например, так:
        $ WEEK_EARLIER=$(echo "($(date +%s) / (3600 * 24) - 7) * 3600 * 24" | bc)

        Запускать следует на кластере zeno, так как именно туда смотрит dev-окружение.
        $ YT_PROXY=zeno.yt.yandex.net YT_TOKEN_PATH="$HOME/.yt/token" time yt select --format json --input-row-limit 100000000 --output-row-limit 1000000000 \
        "S.ExportID, S.GroupExportID, S.PhraseID
        from [//home/yabs/stat/DirectPhraseStat] S
        where S.UpdateTime > ${WEEK_EARLIER} limit 1000" | sort -u | head -n 100
         */
        List<PhraseStatisticsRequest> requests =
                asList(new PhraseStatisticsRequest.Builder().withCampaignId(273L).withAdGroupId(448770L)
                                .withBsPhraseId(ulong(1504368)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(273L).withAdGroupId(448770L)
                                .withBsPhraseId(ulong(1504381)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(273L).withAdGroupId(448770L)
                                .withBsPhraseId(ulong(1504398)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(273L).withAdGroupId(448770L)
                                .withBsPhraseId(ulong(13590592)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(273L).withAdGroupId(448770L)
                                .withBsPhraseId(ulong(13590601)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(273L).withAdGroupId(448770L)
                                .withBsPhraseId(ulong(57063692)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(273L).withAdGroupId(448770L)
                                .withBsPhraseId(ulong(1111573636)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(378L).withAdGroupId(525L)
                                .withBsPhraseId(ulong(126540)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(619L).withAdGroupId(744244L)
                                .withBsPhraseId(ulong(2904832)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(619L).withAdGroupId(744248L)
                                .withBsPhraseId(ulong(1494838)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(619L).withAdGroupId(744251L)
                                .withBsPhraseId(ulong(1494842)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(619L).withAdGroupId(744254L)
                                .withBsPhraseId(ulong(1494837)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(619L).withAdGroupId(744255L)
                                .withBsPhraseId(ulong(1716292)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(619L).withAdGroupId(968764L)
                                .withBsPhraseId(ulong(3995231)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(620L).withAdGroupId(97507L)
                                .withBsPhraseId(ulong(448450)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(904L).withAdGroupId(1490L)
                                .withBsPhraseId(ulong(8725620)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(904L).withAdGroupId(11772L)
                                .withBsPhraseId(ulong(1328693270)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(904L).withAdGroupId(1042052L)
                                .withBsPhraseId(ulong(12112332)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(904L).withAdGroupId(1042053L)
                                .withBsPhraseId(ulong(23617521)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(904L).withAdGroupId(40258338L)
                                .withBsPhraseId(ulong(118256015)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(904L).withAdGroupId(40258338L)
                                .withBsPhraseId(ulong(1328682624)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(904L).withAdGroupId(40258338L)
                                .withBsPhraseId(ulong(1328682631)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(904L).withAdGroupId(111343327L)
                                .withBsPhraseId(ulong(1461210288)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(904L).withAdGroupId(2710212720L)
                                .withBsPhraseId(ulong(3963668)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(904L).withAdGroupId(2710212720L)
                                .withBsPhraseId(ulong(39368606)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(904L).withAdGroupId(2710212720L)
                                .withBsPhraseId(ulong(1394547083)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(904L).withAdGroupId(2790382164L)
                                .withBsPhraseId(ulong(96489)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(904L).withAdGroupId(2790382165L)
                                .withBsPhraseId(ulong(1022597)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(904L).withAdGroupId(2790382165L)
                                .withBsPhraseId(ulong(4227893)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(904L).withAdGroupId(2790382165L)
                                .withBsPhraseId(ulong(23103566)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(904L).withAdGroupId(2790382167L)
                                .withBsPhraseId(ulong(76743302)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(904L).withAdGroupId(2790382167L)
                                .withBsPhraseId(ulong(238451135)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(904L).withAdGroupId(2790382168L)
                                .withBsPhraseId(ulong(18168620)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(929L).withAdGroupId(1507L)
                                .withBsPhraseId(ulong(510970238)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(929L).withAdGroupId(1508L)
                                .withBsPhraseId(ulong(1836449287)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(929L).withAdGroupId(1508L)
                                .withBsPhraseId(ulong(1836449301)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(929L).withAdGroupId(679759L)
                                .withBsPhraseId(ulong(464306)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(929L).withAdGroupId(679759L)
                                .withBsPhraseId(ulong(165135275)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(929L).withAdGroupId(679759L)
                                .withBsPhraseId(ulong(403048939)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(929L).withAdGroupId(18582654L)
                                .withBsPhraseId(ulong(1845168789)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(929L).withAdGroupId(919269188L)
                                .withBsPhraseId(ulong(1)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(929L).withAdGroupId(919269188L)
                                .withBsPhraseId(ulong(428879410)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(929L).withAdGroupId(919269188L)
                                .withBsPhraseId(ulong(673533001)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(929L).withAdGroupId(919269188L)
                                .withBsPhraseId(ulong(1505102337)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1263L).withAdGroupId(79990L)
                                .withBsPhraseId(ulong(226804926)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1263L).withAdGroupId(150967751L)
                                .withBsPhraseId(ulong(62138047)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2401506969L)
                                .withBsPhraseId(ulong(1639919892)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2401593747L)
                                .withBsPhraseId(ulong(1639919892)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2401593750L)
                                .withBsPhraseId(ulong(1639919893)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2401642159L)
                                .withBsPhraseId(ulong(1639919893)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2401642160L)
                                .withBsPhraseId(ulong(1639919892)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2401642168L)
                                .withBsPhraseId(ulong(1639919893)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2401642174L)
                                .withBsPhraseId(ulong(1639919893)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408008311L)
                                .withBsPhraseId(ulong(1639919893)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408070694L)
                                .withBsPhraseId(ulong(1639931491)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408070696L)
                                .withBsPhraseId(ulong(1639859293)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408070697L)
                                .withBsPhraseId(ulong(1639931491)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408070701L)
                                .withBsPhraseId(ulong(1639859293)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408070703L)
                                .withBsPhraseId(ulong(1639931491)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408070704L)
                                .withBsPhraseId(ulong(1653946879)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408070707L)
                                .withBsPhraseId(ulong(1639931494)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408070710L)
                                .withBsPhraseId(ulong(1653946871)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408070716L)
                                .withBsPhraseId(ulong(1639931491)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408070717L)
                                .withBsPhraseId(ulong(1653946870)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408070719L)
                                .withBsPhraseId(ulong(1639931494)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408070721L)
                                .withBsPhraseId(ulong(1639931491)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408070723L)
                                .withBsPhraseId(ulong(1639931494)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408140484L)
                                .withBsPhraseId(ulong(1639937345)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408140485L)
                                .withBsPhraseId(ulong(1639937346)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408140486L)
                                .withBsPhraseId(ulong(1639937346)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408182030L)
                                .withBsPhraseId(ulong(1640037855)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408182035L)
                                .withBsPhraseId(ulong(1639937346)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408182035L)
                                .withBsPhraseId(ulong(1640037855)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408182035L)
                                .withBsPhraseId(ulong(1640037862)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408182038L)
                                .withBsPhraseId(ulong(1639937346)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408182038L)
                                .withBsPhraseId(ulong(1640037862)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408182038L)
                                .withBsPhraseId(ulong(1640037864)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408182041L)
                                .withBsPhraseId(ulong(1640037855)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408182042L)
                                .withBsPhraseId(ulong(1640037855)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408182048L)
                                .withBsPhraseId(ulong(1640037862)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408182049L)
                                .withBsPhraseId(ulong(1640037862)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408182050L)
                                .withBsPhraseId(ulong(1639937345)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408182050L)
                                .withBsPhraseId(ulong(1640037865)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408182052L)
                                .withBsPhraseId(ulong(1640037855)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408182053L)
                                .withBsPhraseId(ulong(1639937346)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408182054L)
                                .withBsPhraseId(ulong(1639937346)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408182055L)
                                .withBsPhraseId(ulong(1640037862)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408182057L)
                                .withBsPhraseId(ulong(1640037862)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408182057L)
                                .withBsPhraseId(ulong(1640037865)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408182059L)
                                .withBsPhraseId(ulong(1639937346)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408182059L)
                                .withBsPhraseId(ulong(1640037855)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1573L).withAdGroupId(2408182059L)
                                .withBsPhraseId(ulong(1640037865)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1629L).withAdGroupId(2728L)
                                .withBsPhraseId(ulong(999191049)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1629L).withAdGroupId(2728L)
                                .withBsPhraseId(ulong(999191056)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1629L).withAdGroupId(2729L)
                                .withBsPhraseId(ulong(999191056)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1629L).withAdGroupId(2741L)
                                .withBsPhraseId(ulong(999117848)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1629L).withAdGroupId(2741L)
                                .withBsPhraseId(ulong(999117849)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1629L).withAdGroupId(24282487L)
                                .withBsPhraseId(ulong(1)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1629L).withAdGroupId(24282487L)
                                .withBsPhraseId(ulong(999191048)).build(),
                        new PhraseStatisticsRequest.Builder().withCampaignId(1629L).withAdGroupId(24539486L)
                                .withBsPhraseId(ulong(1001669964)).build());
        List<PhraseStatisticsResponse> actual =
                recentStatisticsRepository.getPhraseStatistics(requests, getDateRange());
        assertThat(actual.size()).isGreaterThan(100);
    }

    private DateRange getDateRange() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(28);
        return new DateRange()
                .withFromInclusive(startDate)
                .withToInclusive(endDate);
    }
}

package ru.yandex.market.tsum.core.notify.common.startrek;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 24.05.17
 */
public class StartrekUtilsTest {

    @Test
    public void extractIssues() throws Exception {
        Assert.assertEquals(
            Arrays.asList("MI-111", "MBO-42"),
            StartrekUtils.extractIssues("some text MI-111\nMBo-42 another text")
        );

        Assert.assertEquals(
            Arrays.asList("MI-111", "MBO-42"),
            StartrekUtils.extractIssues("some text MI-111 MBo-42 another text")
        );
    }

    @Test
    public void extractIssueWithUrl() {

        Assert.assertEquals(
            Arrays.asList(
                "MBI-35160", "MBI-32099", "MBI-35148"
            ),
            StartrekUtils.extractIssues(
                "Тикет создан автоматически с помощью релизного пайплайна.\n" +
                    "Ссылка на пайплайн: https://tsum.yandex-team.ru/pipe/projects/mbi/delivery-dashboard/mbi-stages/release/5c6e9124ea67aec395763b75\n" +
                    "Ссылка на задачу: https://tsum.yandex-team.ru/pipe/projects/mbi/delivery-dashboard/mbi-stages/release/5c6e9124ea67aec395763b75/job/ConductorDeployJob67/1\n" +
                    "\n" +
                    "Релиз: 2019.1.225\n" +
                    "Тикет: https://st.yandex-team.ru/MBI-35160\n" +
                    "\n" +
                    "Изменения:\n" +
                    "MBI-32099 при апдейте CPA-заказов обновляем также и подстатус (#6425)\n" +
                    "\n" +
                    "* MBI-32099: при апдейте CPA-заказов обновляем также и подстатус\n" +
                    "\n" +
                    "* MBI-32099: fix checkstyle\n" +
                    "\n" +
                    "* MBI-32099: избавляемся от Optional\n" +
                    "\n" +
                    "* MBI-32099: рефакторинг\n" +
                    "\n" +
                    "* MBI-32099: рефакторинг\n" +
                    "\n" +
                    "* MBI-32099: fix\n" +
                    "MBI-35148 Try to fix failing tests (#6432)\n" +
                    "\n" +
                    "\n" +
                    "Pipe job id: 5c6e9123ea67aec395763b32:ConductorDeployJob67:1\n" +
                    "Pipeline id: mbi-main-pipeline-cd"
            )
        );
    }
}

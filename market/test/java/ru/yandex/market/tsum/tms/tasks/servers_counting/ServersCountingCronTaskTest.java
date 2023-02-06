package ru.yandex.market.tsum.tms.tasks.servers_counting;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Strakhov Artem <a href="mailto:dukeartem@yandex-team.ru"></a>
 * @date 19.09.17
 */
public class ServersCountingCronTaskTest {
    @Test
    public void createTskvRecord() throws Exception {
        List<String> groups = Arrays.asList("group1", "group2");
        ServerDescription serverDescription = new ServerDescription("server3", Environment.UNSTABLE, groups, 90);
        Pattern example = Pattern.compile("tskv\tdate=.*\tcatalog_name=Conductor\t" +
            "server_name=server3\tconductor_groups=group1,group2\tenvironment=UNSTABLE\t" +
            "dc=UNKNOWN\tdaily_cpu_usage=90");

        Assert.assertTrue(example.matcher(
            ServersCountingCronTask.createTskvRecord("Conductor", serverDescription)).find());
    }

    @Test
    public void createTskvRecordWithEmptyGroup() throws Exception {
        ServerDescription serverDescription = new ServerDescription("server3", Environment.UNSTABLE,
            Collections.emptyList(), 90);
        Pattern example = Pattern.compile("tskv\tdate=.*\tcatalog_name=Conductor\t" +
            "server_name=server3\tconductor_groups=\tenvironment=UNSTABLE\t" +
            "dc=UNKNOWN\tdaily_cpu_usage=90");

        Assert.assertTrue(example.matcher(
            ServersCountingCronTask.createTskvRecord("Conductor", serverDescription)).find());
    }
}

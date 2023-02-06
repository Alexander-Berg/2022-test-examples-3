package ru.yandex.direct.common.net;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitParamsRunner.class)
public class ProjectNetRangeTest {
    @Test
    @Parameters(method = "parametersForCorrectNetworks")
    public void correctNetwork(String str, ProjectNetRange expected) {
        assertThat(ProjectNetRange.parse(str))
                .isEqualTo(expected);
    }

    Object[] parametersForCorrectNetworks() {
        return new Object[]{
                new Object[]{"697@2a02:6b8:c00::/40", new ProjectNetRange(
                        Long.parseLong("697", 16),
                        NetRangeParser.parseSingleNetwork("2a02:6b8:c00::/40"))},
                new Object[]{"10cf499@2a02:6b8:c00::/40", new ProjectNetRange(
                        Long.parseLong("10cf499", 16),
                        NetRangeParser.parseSingleNetwork("2a02:6b8:c00::/40"))},
        };
    }

    @Test
    @Parameters(value = {
            "2a02:6b8:c00:2588::697:3e4c:ec2, 697",
            "2a02:6b8:c00:2588:10c:f499:3e4c:ec25, 10cf499"
    })
    public void extractProjectIdWorks(String ip, String proj) {
        assertThat(
                ProjectNetRange.extractProjectId(IpUtils.address2BigInteger(ip))
        ).isEqualTo(Long.parseLong(proj, 16));
    }
}

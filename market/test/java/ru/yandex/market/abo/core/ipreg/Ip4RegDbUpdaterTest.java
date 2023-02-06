package ru.yandex.market.abo.core.ipreg;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import lombok.Value;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;

/**
 * @author komarovns
 * @date 23.11.2020
 */
class Ip4RegDbUpdaterTest extends EmptyTest {
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    Ip4RegDbUpdater ip4RegDbUpdater;

    private static final String IP_REG_FILENAME = "ip_reg_abo";
    private static final String IP_REG = "" +
            "1 2 3\n" +
            "3 7 1\n" +
            "3758096384 4294967295 99999";

    @AfterEach
    public void tearDown() {
        FileUtils.deleteQuietly(new File(IP_REG_FILENAME));
    }

    @Test
    void testUpdateIpv4Table() throws IOException {
        var ipRegFile = Path.of(IP_REG_FILENAME);
        Files.writeString(ipRegFile, IP_REG, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        ip4RegDbUpdater.updateIpv4Table(ipRegFile);
        var dbRows = jdbcTemplate.query(
                "SELECT * FROM ip4_reg ORDER BY left_bound",
                (rs, rowNum) -> new IpRange(rs.getLong("left_bound"), rs.getLong("right_bound"), rs.getInt("region"))
        );
        Assertions.assertEquals(List.of(
                new IpRange(1, 2, 3),
                new IpRange(3, 7, 1),
                new IpRange(3758096384L, 4294967295L, 99999)
        ), dbRows);
    }

    @Value
    private static class IpRange {
        long left;
        long write;
        int region;
    }
}

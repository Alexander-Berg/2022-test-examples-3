package ru.yandex.market.vmid.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.vmid.AbstractIntegrationTest;
import ru.yandex.market.vmid.repositories.dto.Vmid;

import static org.junit.Assert.assertEquals;

public class VmidCleanerImplTest extends AbstractIntegrationTest {

    @Autowired
    private VmidCleanerImpl vmidCleaner;

    @Before
    public void setUp() {
        jdbcTemplate.update("INSERT INTO IDS (OFFER_ID, FEED_ID, LAST_REQUEST) VALUES ('1', 1, ?), ('2', 2, ?)",
                LocalDate.now().minusDays(5), LocalDate.now());
    }

    @Test
    public void testClean() {
        vmidCleaner.clean();

        final List<Vmid> result = new ArrayList<>();
        jdbcTemplate.query("SELECT * FROM IDS",
                rs -> {
                    Vmid vmid = new Vmid(
                            rs.getString("OFFER_ID"),
                            rs.getLong("FEED_ID"),
                            rs.getLong("VMID"),
                            rs.getDate("LAST_REQUEST").toLocalDate());
                    result.add(vmid);
                });
        assertEquals(1, result.size());
        Vmid vmid = result.get(0);
        assertEquals("2", vmid.getOfferId());
        assertEquals(2, vmid.getFeedId());

    }
}

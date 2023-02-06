package ru.yandex.market.vmid.repositories;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.vmid.AbstractIntegrationTest;
import ru.yandex.market.vmid.repositories.dto.SequenceStat;
import ru.yandex.market.vmid.repositories.dto.Vmid;

import static org.junit.Assert.assertEquals;

public class IdsRepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private IdsRepository idsRepository;

    @Before
    public void setUp() {
        jdbcTemplate.execute("INSERT INTO IDS (FEED_ID, OFFER_ID) VALUES (1, '1'), (2, '2')");
    }

    @Test
    public void findAllTest() {
        List<Vmid> result = new ArrayList<>();
        idsRepository.findAll(result::add);
        assertEquals(2, result.size());

        final Vmid vmid1 = result.get(0);
        assertEquals("1", vmid1.getOfferId());
        assertEquals(1, vmid1.getFeedId());

        final Vmid vmid2 = result.get(1);
        assertEquals("2", vmid2.getOfferId());
        assertEquals(2, vmid2.getFeedId());
    }

    @Test
    public void getSequenceStatTest() {
        final SequenceStat sequenceStat = idsRepository.getSequenceStat();
        assertEquals(1000000000000000L, sequenceStat.getMax());
        assertEquals(2000000000001L, sequenceStat.getCurr());
    }
}

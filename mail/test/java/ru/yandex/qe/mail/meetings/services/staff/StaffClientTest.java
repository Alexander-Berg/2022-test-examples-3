package ru.yandex.qe.mail.meetings.services.staff;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MockConfiguration.class})
public class StaffClientTest {
    @Autowired
    private StaffClient client;

    @Test
    public void getAll() {
        assertEquals(MockConfiguration.TOTAL, client.getAll().size());
    }

    @Test
    public void getExternal() {
        StaffClient staffClient = new StaffClient(new MockStaff(Collections.emptySet()));
        assertNull(staffClient.getByLogin(MockStaff.EXTERNAL));
    }
}

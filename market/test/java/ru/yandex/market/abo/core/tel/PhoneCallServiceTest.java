package ru.yandex.market.abo.core.tel;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author imelnikov
 */
class PhoneCallServiceTest extends EmptyTest {

    @Autowired
    private PhoneCallService phoneCallService;
    @Autowired
    private TelPhonebookRepo telPhonebookRepo;
    @Autowired
    private TelCallRepo telCallRepo;
    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @Test
    void testPhones() {
        long ticketId = 2L;
        PhoneCall call = newCall(ticketId);
        PhoneCall saved = phoneCallService.storeIfNotExists(call);
        assertEquals(saved.getId(), phoneCallService.storeIfNotExists(call).getId());

        List<PhoneCall> list = phoneCallService.list(ticketId, CallSourceType.LITE);
        assertEquals(1, list.size());

        assertNotEquals(saved.getId(), phoneCallService.storeIfNotExists(newCall(ticketId)).getId());
    }

    @Test
    void testPhonebook() {
        Long newUserId = RND.nextLong();
        TelPhonebook phone = telPhonebookRepo.saveAndFlush(new TelPhonebook("name"));
        pgJdbcTemplate.update("UPDATE tel_phonebook SET user_id = ? WHERE user_id = ?",
                newUserId, phone.getUserId());

        TelPhonebook phone2 = telPhonebookRepo.saveAndFlush(new TelPhonebook("name2"));
        pgJdbcTemplate.update("UPDATE tel_phonebook SET user_id = ? WHERE user_id = ?",
                newUserId, phone2.getUserId());

        Map<String, Long> name2Uid = phoneCallService.loadPhonebook();
        assertEquals(newUserId, name2Uid.get("name"));
        assertEquals(newUserId, name2Uid.get("name2"));
    }

    @Test
    void testCalls() {
        TelCall call = genTelCall();
        phoneCallService.store(Collections.singletonList(call));
        assertNotNull(call.getId());

        assertEquals(call.getCreationTime(), phoneCallService.getLastCallDate());
    }

    @Test
    void testStoreCallAndPhonebook() {
        TelCall call = genTelCall();
        Map<String, Long> name2Uid = phoneCallService.loadPhonebook();
        phoneCallService.store(Collections.singletonList(call), name2Uid);

        List<TelCall> calls = telCallRepo.findAll();
        assertTrue(calls.stream().anyMatch(c -> c.getName().equals(call.getName())));
        assertTrue(name2Uid.containsKey(call.getName()));
    }

    @Test
    void testBind() {
        TelCall call = genTelCall();
        phoneCallService.store(Collections.singletonList(call));

        List<TelCall> callsToBind = phoneCallService.getCallsToBind();
        assertTrue(callsToBind.stream().anyMatch(c -> c.getId().equals(call.getId())));

        call.setEntityId(RND.nextLong());
        phoneCallService.store(Collections.singletonList(call));

        callsToBind = phoneCallService.getCallsToBind();
        assertTrue(callsToBind.stream().noneMatch(c -> c.getId().equals(call.getId())));

        assertFalse(telCallRepo.findByEntityIdOrderByCreationTime(call.getEntityId()).isEmpty());
    }

    private static TelCall genTelCall() {
        return new TelCall(UUID.randomUUID().toString(), "+8923452345", new Date(), "http://ya.ru");
    }

    private static PhoneCall newCall(long ticketId) {
        return PhoneCall.newBuilder(41L, CallSourceType.LITE, ticketId, "8-926-123-8765")
                .callDirection(CallDirection.OUT)
                .callerType(CallerType.COURIER)
                .creationTime(new Date())
                .build();
    }
}

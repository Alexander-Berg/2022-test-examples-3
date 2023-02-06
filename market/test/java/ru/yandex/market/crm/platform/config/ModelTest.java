package ru.yandex.market.crm.platform.config;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.crm.platform.ConfigRepository;
import ru.yandex.market.crm.platform.TestConfiguration;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.exceptions.UnprocessableEntityException;
import ru.yandex.market.crm.platform.models.Push;
import ru.yandex.market.crm.util.Randoms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class ModelTest {

    @Autowired
    ConfigRepository repository;

    /**
     * Проверяем получение значения id-поля.
     */
    @Test
    public void getId() {
        String factId = UUID.randomUUID().toString();
        Push push = Push.newBuilder().setFactId(factId).build();

        var pushConfig = getConfig();
        String result = pushConfig.getModel().getId(push);

        assertEquals("Должны получить значение поля factId т.к. оно помечено crm.platform.commons.id", factId,
                result);
    }

    /**
     * Проверяем валидацию значения id-поля.
     * <p>
     * должны получить ошибку т.к. значение id не может быть пустым.
     */
    @Test(expected = UnprocessableEntityException.class)
    public void validateAndGetId_empty() {
        Push push = Push.newBuilder().build();

        var pushConfig = getConfig();
        pushConfig.getModel().validateAndGetId(push);
    }

    /**
     * Проверяем валидацию значения id-поля.
     * <p>
     * должны получить ошибку т.к. значение id не может быть пустым.
     */
    @Test
    public void validateAndGetId_notEmpty() {
        Push push = Push.newBuilder()
                .setFactId(UUID.randomUUID().toString())
                .build();

        var pushConfig = getConfig();
        String result = pushConfig.getModel().validateAndGetId(push);

        assertEquals(push.getFactId(), result);
    }

    /**
     * Проверяем получение значения time-поля.
     */
    @Test
    public void getTime() {
        long time = ThreadLocalRandom.current().nextLong();
        Push push = Push.newBuilder().setTimestamp(time).build();

        var pushConfig = getConfig();
        long result = pushConfig.getModel().getTime(push);

        assertEquals(
                "Должны получиь значение поля sendTime т.к. оно помечено crm.platform.commons.time",
                time,
                result
        );
    }

    /**
     * Проверяем получение значения uid-поля.
     */
    @Test
    public void getUid() {
        long puid = Randoms.unsignedLongValue();
        Uid uid = Uids.create(UidType.PUID, puid);
        Push push = Push.newBuilder().setUid(uid).build();

        var pushConfig = getConfig();
        Uid result = pushConfig.getModel().getUid(push);
        assertNotNull(result);
        assertEquals(puid, result.getIntValue());
    }

    @Test
    public void hasId() {
        var pushConfig = getConfig();
        assertTrue(pushConfig.getModel().hasId());
    }

    @Test
    public void hasTime() {
        var pushConfig = getConfig();
        assertTrue(pushConfig.getModel().hasTime());
    }

    @Test
    public void hasUid() {
        var pushConfig = getConfig();
        assertTrue(pushConfig.getModel().hasUid());
    }

    private FactInfo getConfig() {
        var config = repository.getFact("Push");
        assertNotNull(config);
        return config;
    }
}

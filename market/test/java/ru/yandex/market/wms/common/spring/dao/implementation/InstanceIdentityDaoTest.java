package ru.yandex.market.wms.common.spring.dao.implementation;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.context.TestConstructor;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.IdentityType;
import ru.yandex.market.wms.common.spring.dao.entity.InstanceIdentity;
import ru.yandex.market.wms.common.spring.dao.entity.SerialInventory;
import ru.yandex.market.wms.common.spring.enums.TypeOfIdentity;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InstanceIdentityDaoTest extends IntegrationTest {

    private final InstanceIdentityDAO instanceIdentityDAO;
    private final SerialInventoryDao serialInventoryDao;
    private final Clock clock;
    private final Random random = new Random();
    private final String defaultType = "IMEI";

    InstanceIdentityDaoTest(IdentityDao identityDao,
                            InstanceIdentityDAO instanceIdentityDAO,
                            SerialInventoryDao serialInventoryDao,
                            Clock clock) {
        this.instanceIdentityDAO = instanceIdentityDAO;
        this.serialInventoryDao = serialInventoryDao;
        this.clock = clock;

        identityDao.upsertIdentityType(Arrays.asList(
                IdentityType.builder()
                        .type(defaultType)
                        .regex("123")
                        .description("IMEI 1")
                        .addWho(getClass().getName())
                        .editWho(getClass().getName())
                        .build(),
                IdentityType.builder()
                        .type("CIS")
                        .regex("1a2b")
                        .description("CIS")
                        .addWho(getClass().getName())
                        .editWho(getClass().getName())
                        .build()
        ));
    }

    @Test
    void create() {
        final InstanceIdentity instanceIdentity = InstanceIdentity.builder()
                .pk(InstanceIdentity.PK.builder()
                        .instance(addSerialInventory().getSerialNumber())
                        .identity("abc")
                        .type(defaultType)
                        .build())
                .process("test")
                .addWho(getClass().getName())
                .editWho(getClass().getName())
                .build();

        assertNotNull(instanceIdentityDAO.create(instanceIdentity));

        final Collection<InstanceIdentity> actualSet = instanceIdentityDAO.findByInstance(
                instanceIdentity.getPk().getInstance()
        );
        assertNotNull(actualSet);
        assertEquals(1, actualSet.size());

        final InstanceIdentity actual = actualSet.iterator().next();
        assertEquals(withRoundedNanos(instanceIdentity), actual);
        assertNotNull(actual.getSerialKey());
    }

    @Test
    void batch() {
        SerialInventory serialInventory = addSerialInventory();
        final InstanceIdentity itemIdentity1 = InstanceIdentity.builder()
                .pk(InstanceIdentity.PK.builder()
                        .instance(serialInventory.getSerialNumber())
                        .identity("abc1")
                        .type(defaultType)
                        .build())
                .process("test")
                .addWho(getClass().getName())
                .editWho(getClass().getName())
                .build();

        final InstanceIdentity itemIdentity2 = InstanceIdentity.builder()
                .pk(InstanceIdentity.PK.builder()
                        .instance(serialInventory.getSerialNumber())
                        .identity("abc2")
                        .type(defaultType)
                        .build())
                .process("test")
                .addWho(getClass().getName())
                .editWho(getClass().getName())
                .build();

        assertNotNull(instanceIdentityDAO.createBatch(Lists.newArrayList(itemIdentity1, itemIdentity2)));

        final Collection<InstanceIdentity> actualSet = instanceIdentityDAO.findByInstanceList(
                Collections.singletonList(serialInventory.getSerialNumber()));
        assertNotNull(actualSet);
        assertEquals(2, actualSet.size());
        assertions.assertThat(actualSet).contains(withRoundedNanos(itemIdentity1, itemIdentity2));
    }

    @Test
    void findEmpty() {
        assertEquals(0, instanceIdentityDAO.findByInstance(randomId()).size());
    }

    @Test
    void update() {
        final String item = addSerialInventory().getSerialNumber();
        instanceIdentityDAO.create(InstanceIdentity.builder()
                .pk(InstanceIdentity.PK.builder()
                        .instance(item)
                        .identity("abc")
                        .type(defaultType)
                        .build())
                .process("test")
                .addWho("Grom Hellscream")
                .editWho("Grom Hellscream")
                .build()
        );
        InstanceIdentity base = instanceIdentityDAO.findByInstance(item).iterator().next();
        final InstanceIdentity expected = InstanceIdentity.builder()
                .pk(InstanceIdentity.PK.builder()
                        .instance(base.getPk().getInstance())
                        .identity("xyz")
                        .type(base.getPk().getType())
                        .build())
                .serialKey(base.getSerialKey())
                .process(base.getProcess())
                .addDate(base.getAddDate())
                .addWho(base.getAddWho())
                .editWho("Thrall")
                .build();
        instanceIdentityDAO.update(expected);

        final InstanceIdentity actual =
                instanceIdentityDAO.findByInstance(expected.getPk().getInstance()).iterator().next();
        assertEquals(withRoundedNanos(expected), actual);
    }

    @Test
    void delete() {
        final InstanceIdentity instanceIdentity = InstanceIdentity.builder()
                .pk(InstanceIdentity.PK.builder()
                        .instance(addSerialInventory().getSerialNumber())
                        .identity("abc")
                        .type(defaultType)
                        .build())
                .process("test")
                .addWho(getClass().getName())
                .editWho(getClass().getName())
                .build();

        instanceIdentityDAO.create(instanceIdentity);
        assertEquals(1, instanceIdentityDAO.findByInstance(instanceIdentity.getPk().getInstance()).size());

        instanceIdentityDAO.deleteByPk(instanceIdentity.getPk());
        assertEquals(0, instanceIdentityDAO.findByInstance(instanceIdentity.getPk().getInstance()).size());
    }

    @Test
    void multiTimeZone() {
        final InstanceIdentity origin = InstanceIdentity.builder()
                .pk(InstanceIdentity.PK.builder()
                        .instance(addSerialInventory().getSerialNumber())
                        .identity("abc")
                        .type(defaultType)
                        .build())
                .process("test")
                .addWho(getClass().getName())
                .editWho(getClass().getName())
                .build();

        executeInTimeZone("PST", () -> instanceIdentityDAO.create(origin));

        final InstanceIdentity created =
                instanceIdentityDAO.findByInstance(origin.getPk().getInstance()).iterator().next();
        assertEquals(withRoundedNanos(origin), created);

        final InstanceIdentity newVersion = InstanceIdentity.builder()
                .pk(InstanceIdentity.PK.builder()
                        .instance(created.getPk().getInstance())
                        .identity("xyz")
                        .type(created.getPk().getType())
                        .build())
                .serialKey(created.getSerialKey())
                .process(created.getProcess())
                .addDate(created.getAddDate())
                .addWho(created.getAddWho())
                .editWho(created.getEditWho())
                .build();
        instanceIdentityDAO.update(newVersion);

        executeInTimeZone("CET", () -> {
                    final InstanceIdentity updated = instanceIdentityDAO.findByInstance(origin.getPk().getInstance())
                            .iterator().next();
                    assertEquals(withRoundedNanos(newVersion), updated);
                    return updated;
                }
        );
    }

    @Test
    void findByItemListAndType() {
        final List<InstanceIdentity> identities = Stream.of(1, 2, 3, 4, 5)
                .map(it -> InstanceIdentity.builder()
                        .pk(InstanceIdentity.PK.builder()
                                .instance(addSerialInventory().getSerialNumber())
                                .identity(randomId())
                                .type(it % 2 == 0 ? "CIS" : "IMEI")
                                .build())
                        .process("test")
                        .addWho(getClass().getName())
                        .editWho(getClass().getName())
                        .build()
                )
                .collect(toList());
        instanceIdentityDAO.createBatch(identities);

        final List<String> items = identities.stream()
                .map(id -> id.getPk().getInstance())
                .collect(toList());
        final List<InstanceIdentity> cisIdentities = instanceIdentityDAO.findByInstanceListAndType(items,
                TypeOfIdentity.CIS);
        assertThat(cisIdentities, hasSize(2));

        final List<String> cisTypes = cisIdentities.stream().map(id -> id.getPk().getType()).collect(toList());
        assertThat(cisTypes, contains("CIS", "CIS"));
    }

    @Test
    void empty() {
        assertThrows(NullPointerException.class, () -> instanceIdentityDAO.create(null));

        assertNotNull(instanceIdentityDAO.createBatch(null));

        assertNotNull(instanceIdentityDAO.findByInstance(null));

        assertNotNull(instanceIdentityDAO.findByIdentityByTemplate(null, null));

        assertNotNull(instanceIdentityDAO.findByInstanceListAndType(null, null));
        assertNotNull(instanceIdentityDAO.findByInstanceListAndType(Collections.emptyList(), TypeOfIdentity.CIS));

        assertThrows(NullPointerException.class, () -> instanceIdentityDAO.update(null));

        instanceIdentityDAO.deleteByPk(null);
    }

    private SerialInventory addSerialInventory() {
        final SerialInventory serialInventory = SerialInventory.builder()
                .serialNumber(randomId())
                .storerKey("storer")
                .sku("sku")
                .id("id")
                .loc("loc")
                .lot("lot")
                .isFake(false)
                .quantity(BigDecimal.ONE)
                .addWho(getClass().getName())
                .editWho(getClass().getName())
                .build();
        serialInventoryDao.insert(Collections.singletonList(serialInventory), LocalDateTime.now(clock));
        return serialInventory;
    }

    private String randomId() {
        return Integer.toString(random.nextInt(Integer.MAX_VALUE));
    }

    private LocalDateTime localInDefaultTimeZone(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private <V> V executeInTimeZone(String timeZone, Callable<V> callable) {
        final TimeZone originTimeZone = TimeZone.getDefault();
        final Instant instant = Instant.now();
        final LocalDateTime originLocalDateTime = localInDefaultTimeZone(instant);
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(timeZone));
            assertNotEquals(originLocalDateTime, localInDefaultTimeZone(instant));
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            TimeZone.setDefault(originTimeZone);
            assertEquals(originLocalDateTime, localInDefaultTimeZone(instant));
        }
    }

    private InstanceIdentity[] withRoundedNanos(InstanceIdentity... iis) {
        return Arrays.stream(iis).map(this::withRoundedNanos).toArray(InstanceIdentity[]::new);
    }

    private InstanceIdentity withRoundedNanos(InstanceIdentity ii) {
        if (ii == null) {
            return null;
        }
        return InstanceIdentity.builder()
                .pk(ii.getPk())
                .process(ii.getProcess())
                .addDate(roundNanos(ii.getAddDate()))
                .addWho(ii.getAddWho())
                .editDate(roundNanos(ii.getEditDate()))
                .editWho(ii.getEditWho())
                .serialKey(ii.getSerialKey())
                .noIdentities(ii.isNoIdentities())
                .locStatus(ii.getLocStatus())
                .build();
    }

    private Instant roundNanos(Instant val) {
        if (val == null) {
            return null;
        }
        return val.plusNanos(500).truncatedTo(ChronoUnit.MICROS);
    }
}

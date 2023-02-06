package ru.yandex.market.wms.common.spring.dao.implementation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.context.TestConstructor;

import ru.yandex.market.wms.common.model.enums.MarkHandleMode;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.IdentityType;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.entity.SkuIdentity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdentityDaoTest extends IntegrationTest {

    private final IdentityDao identityDao;

    IdentityDaoTest(IdentityDao identityDao) {
        this.identityDao = identityDao;
    }

    @Test
    void upsertIdentityType() {
        final IdentityType first = IdentityType.builder()
                .type(generateType())
                .regex("11")
                .description("Serial Number")
                .addWho(getClass().getName())
                .editWho(getClass().getName())
                .build();
        identityDao.upsertIdentityType(Collections.singleton(first));

        List<IdentityType> actualList = identityDao.findByType(Collections.singleton(first.getType()));
        assertEquals(1, actualList.size());
        assertEquals(withRoundedNanos(first), actualList.iterator().next());

        final IdentityType firstUpdated = IdentityType.builder()
                .type(first.getType())
                .regex("22")
                .description(first.getDescription())
                .addDate(first.getAddDate())
                .addWho(first.getAddWho())
                .editWho(first.getEditWho())
                .build();
        final IdentityType second = IdentityType.builder()
                .type(String.valueOf(new Random().nextInt()))
                .regex("abc")
                .description("Serial Number")
                .addWho(getClass().getName())
                .editWho(getClass().getName())
                .build();
        identityDao.upsertIdentityType(Lists.list(firstUpdated, second));

        actualList = identityDao.findByType(Lists.list(firstUpdated.getType(), second.getType()));
        assertEquals(2, actualList.size());
        assertThat(actualList, containsInAnyOrder(withRoundedNanos(firstUpdated, second)));
    }

    @Test
    void upsertMultipleTypes() {
        Collection<IdentityType> identityTypes = new ArrayList<>();
        identityTypes.add(
                IdentityType.builder()
                        .type(generateType())
                        .regex("gg")
                        .description("Unique Type")
                        .addWho(getClass().getName())
                        .editWho(getClass().getName())
                        .build()
        );
        final String oneType = generateType();
        for (int i = 0; i < 9; i++) {
            identityTypes.add(
                    IdentityType.builder()
                            .type(oneType)
                            .regex("gg")
                            .description("One Type " + i)
                            .addWho(getClass().getName())
                            .editWho(getClass().getName())
                            .build()
            );
        }
        identityDao.upsertIdentityType(identityTypes);

        final Collection<String> expected = identityTypes
                .stream()
                .map(IdentityType::getType)
                .collect(Collectors.toSet());
        final List<String> actual = identityDao.findByType(expected)
                .stream()
                .map(IdentityType::getType)
                .collect(Collectors.toList());
        assertEquals(2, actual.size());
        assertThat(actual, containsInAnyOrder(expected.toArray()));
    }

    @Test
    void upsertEmpty() {
        identityDao.upsertIdentityType(Collections.emptySet());
        identityDao.upsertIdentityType(null);

        assertNotNull(identityDao.getIdentityType(null));

        assertNotNull(identityDao.findByType(Collections.emptySet()));
        assertNotNull(identityDao.findByType(null));

        identityDao.upsertSkuIdentities(Collections.emptySet());
        identityDao.upsertSkuIdentities(null);

        assertNotNull(identityDao.getSkuIdentities(Collections.emptyList()));
        assertNotNull(identityDao.getSkuIdentities(null));
    }

    @Test
    void upsertSkuIdentityRegex() {
        String idType = generateType();
        final IdentityType identityType = IdentityType.builder()
                .type(idType)
                .regex("11")
                .description("Serial Number")
                .addWho(getClass().getName())
                .editWho(getClass().getName())
                .serialKey(1)
                .build();
        identityDao.upsertIdentityType(Collections.singleton(identityType));

        final IdentityType identityTypeSkuFirst = IdentityType.builder()
                .type(idType)
                .regex("11")
                .description("Serial Number")
                .addWho(getClass().getName())
                .editWho(getClass().getName())
                .serialKey(1)
                .build();
        final SkuId skuIdFirst = new SkuId("123", "SKU123");
        final SkuIdentity skuIdentityFirst = SkuIdentity.builder()
                .pk(SkuIdentity.PK.builder()
                        .skuId(skuIdFirst)
                        .type(idType)
                        .build())
                .identityType(identityTypeSkuFirst)
                .requirements(0)
                .markHandleMode(MarkHandleMode.NOT_DEFINED)
                .addWho(getClass().getName())
                .editWho(getClass().getName())
                .build();

        final IdentityType identityTypeSkuSecond = IdentityType.builder()
                .type(idType)
                .regex("111")
                .description("Serial Number")
                .addWho(getClass().getName())
                .editWho(getClass().getName())
                .serialKey(1)
                .build();
        final SkuId skuIdSecond = new SkuId("234", "SKU234");
        final SkuIdentity skuIdentitySecond = SkuIdentity.builder()
                .pk(SkuIdentity.PK.builder()
                        .skuId(skuIdSecond)
                        .type(idType)
                        .build())
                .identityType(identityTypeSkuSecond)
                .requirements(0)
                .markHandleMode(MarkHandleMode.NOT_DEFINED)
                .addWho(getClass().getName())
                .editWho(getClass().getName())
                .build();

        identityDao.upsertSkuIdentities(List.of(skuIdentityFirst, skuIdentitySecond));

        Map<SkuId, List<SkuIdentity>> skuIdentities = identityDao.getSkuIdentities(List.of(skuIdFirst, skuIdSecond));
        SkuIdentity skuIdentityActualFirst = skuIdentities.get(skuIdFirst).get(0);
        SkuIdentity skuIdentityActualSecond = skuIdentities.get(skuIdSecond).get(0);

        assertEquals(skuIdentityFirst.getIdentityType().getRegex(),
                skuIdentityActualFirst.getIdentityType().getRegex());
        assertEquals(skuIdentitySecond.getIdentityType().getRegex(),
                skuIdentityActualSecond.getIdentityType().getRegex());
    }

    private String generateType() {
        return String.valueOf(new Random().nextInt());
    }

    private IdentityType[] withRoundedNanos(IdentityType... iis) {
        return Arrays.stream(iis).map(this::withRoundedNanos).toArray(IdentityType[]::new);
    }

    private IdentityType withRoundedNanos(IdentityType it) {
        if (it == null) {
            return null;
        }
        return IdentityType.builder()
                .type(it.getType())
                .regex(it.getRegex())
                .description(it.getDescription())
                .addDate(roundNanos(it.getAddDate()))
                .addWho(it.getAddWho())
                .editDate(roundNanos(it.getEditDate()))
                .editWho(it.getEditWho())
                .serialKey(it.getSerialKey())
                .build();
    }

    private Instant roundNanos(Instant val) {
        if (val == null) {
            return null;
        }
        return val.plusNanos(500).truncatedTo(ChronoUnit.MICROS);
    }
}

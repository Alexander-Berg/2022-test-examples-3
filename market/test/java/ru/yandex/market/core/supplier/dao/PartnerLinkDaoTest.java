package ru.yandex.market.core.supplier.dao;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.partner.PartnerLinkDao;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Проверяем проверку дочернего магазина в {@link PartnerLinkDao}.
 */
@DbUnitDataSet(before = "PartnerLinkServiceTest.before.csv")
public class PartnerLinkDaoTest extends FunctionalTest {
    @Autowired
    private PartnerLinkDao partnerLinkDao;

    private static Stream<Arguments> argsCheckIsChildPartner() {
        return Stream.of(
                Arguments.of("просто ДБС партнер", 1L, false),
                Arguments.of("просто CPC партнер", 2L, false),
                Arguments.of("ДБС, реплика от донора CPC", 3L, true),
                Arguments.of("неизвестный партнер", 4L, false)
        );
    }

    @ParameterizedTest
    @MethodSource("argsCheckIsChildPartner")
    void checkIsChildPartner(String description, long partnerId, boolean isChildExpected) {
        boolean isChildPartner = partnerLinkDao.getDonorPartnerId(partnerId) != null;
        assertEquals(isChildExpected, isChildPartner);
    }

    @Test
    void checkAllIsChildPartner() {
        Map<Long, Long> actual = partnerLinkDao.getDonorPartnerIds(Set.of(1L, 2L, 3L, 4L));
        Map<Long, Long> expected = Map.of(3L, 2L);
        assertEquals(expected, actual);
    }
}

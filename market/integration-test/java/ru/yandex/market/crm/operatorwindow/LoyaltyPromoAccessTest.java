package ru.yandex.market.crm.operatorwindow;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.transaction.Transactional;

import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import ru.yandex.market.crm.operatorwindow.dao.BonusReasonDao;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.module.ou.security.Employee;
import ru.yandex.market.jmf.module.ou.security.EmployeeRole;
import ru.yandex.market.ocrm.module.loyalty.BonusReason;
import ru.yandex.market.ocrm.module.loyalty.LoyaltyPromo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@Disabled("FIXME")
public class LoyaltyPromoAccessTest extends AbstractModuleOwTest {

    @Inject
    private DbService dbService;
    @Inject
    private BonusReasonDao bonusReasonDao;

    @Mock
    private Employee employee;

    @BeforeEach
    public void init() {
        securityDataService.reset();

        // Загрузим реальные роли. Пусть пользователь будет иметь 2 из 3
        Query q = Query.of(EmployeeRole.FQN)
                .withFilters(Filters.in("code", List.of("employeeRoleOne", "employeeRoleTwo")));

        var emplRoles = new HashSet<EmployeeRole>(dbService.list(q));
        assertEquals(2, emplRoles.size(), "вероятно, неправильно заполнен справочник employeeRoles");

        when(employee.getRoles()).thenReturn(emplRoles);
        securityDataService.setCurrentEmployee(employee);
    }

    @Test
    @Transactional
    public void testBonusReasons() {
        List<BonusReason> bonusReasons = bonusReasonDao.getBonusReasons();

        Map<String, BonusReason> index = buildBonusReasonIndex(bonusReasons);

        var testFull = getReason(index, "TEST_FULL");
        var testOneDefault = getReason(index, "TEST_ONE_DEFAULT_PROMO");
        var testOneAdditional = getReason(index, "TEST_ONE_ADDITIONAL_PROMO");
        var testNoOne = getReason(index, "TEST_NO_ONE_PROMO");

        Collection<String> availablePromos = Collections2.transform(testFull.getAvailablePromos(),
                LoyaltyPromo::getCode);
        assertEquals(3, availablePromos.size(), testFull.getDescription());
        assertTrue(availablePromos.containsAll(List.of("BONUS_VISIBLE_1", "BONUS_VISIBLE_2", "BONUS_NO_ONE_ROLE")));

        assertEquals(1, testOneDefault.getAvailablePromos().size(), testOneDefault.getDescription());
        assertEquals(1, testOneAdditional.getAvailablePromos().size(), testOneAdditional.getDescription());
        assertEquals(0, testNoOne.getAvailablePromos().size(), testNoOne.getDescription());

        LoyaltyPromo promoVisibleOne = testOneDefault.getAvailablePromos().iterator().next();
        assertEquals(Long.valueOf(99991), promoVisibleOne.getPromoId());

        LoyaltyPromo promoVisibleTwo = testOneAdditional.getAvailablePromos().iterator().next();
        assertEquals(Long.valueOf(99992), promoVisibleTwo.getPromoId());
    }

    private BonusReason getReason(Map<String, BonusReason> index, String code) {
        var reason = index.get(code);
        assertNotNull(reason, "Причина " + code + " должна быть доступна");

        return reason;
    }

    private Map<String, BonusReason> buildBonusReasonIndex(List<BonusReason> bonusReasons) {
        return Maps.uniqueIndex(bonusReasons, BonusReason::getCode);
    }
}

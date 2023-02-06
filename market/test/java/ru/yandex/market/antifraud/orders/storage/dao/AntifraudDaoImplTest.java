package ru.yandex.market.antifraud.orders.storage.dao;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRule;
import ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRuleType;
import ru.yandex.market.antifraud.orders.service.Utils;
import ru.yandex.market.antifraud.orders.test.annotations.DaoLayerTest;
import ru.yandex.market.antifraud.orders.web.entity.LoyaltyVerdictType;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRuleType.ANY;
import static ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRuleType.PHONE;
import static ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRuleType.UID;

/**
 * @author dzvyagin
 */
@DaoLayerTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AntifraudDaoImplTest {

    private static final String CANCEL_ORDER_ACTION = Utils.getBlacklistAction(AntifraudAction.CANCEL_ORDER);

    @Autowired
    private NamedParameterJdbcOperations jdbcTemplate;

    private AntifraudDao antifraudDao;

    @Before
    public void init() {
        antifraudDao = new AntifraudDaoImpl(jdbcTemplate);
    }

    @Test
    public void getBlacklistRule() throws Exception {
        SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss z");
        AntifraudBlacklistRule rule1 = new AntifraudBlacklistRule(UID,
                "123", CANCEL_ORDER_ACTION, "some_reason_1", ft.parse("30-05-2045 00:00:00 +0000"), 555555555L);
        AntifraudBlacklistRule rule2 = new AntifraudBlacklistRule(PHONE,
                "+7111111111", CANCEL_ORDER_ACTION, "some_reason_2", ft.parse("30-05-2049 00:00:00 +0000"), 555555555L);
        Optional<AntifraudBlacklistRule> rule1O = antifraudDao.getBlacklistRuleO(UID, "123", CANCEL_ORDER_ACTION);
        assertThat(rule1O).isEmpty();
        antifraudDao.saveBlacklistRule(rule1);
        rule1O = antifraudDao.getBlacklistRuleO(UID, "123", CANCEL_ORDER_ACTION);
        assertThat(rule1O).isPresent();
        Optional<AntifraudBlacklistRule> rule2O = antifraudDao.getBlacklistRuleO(PHONE, "+7111111111", CANCEL_ORDER_ACTION);
        assertThat(rule2O).isEmpty();
        antifraudDao.saveBlacklistRule(rule2);
        rule2O = antifraudDao.getBlacklistRuleO(UID, "123", CANCEL_ORDER_ACTION);
        assertThat(rule2O).isPresent();
    }

    @Test
    public void getBlackListRules() throws Exception {
        SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss z");
        AntifraudBlacklistRule rule1 = new AntifraudBlacklistRule(UID,
                "5411", CANCEL_ORDER_ACTION, "some_reason_1", ft.parse("30-05-2045 00:00:00 +0000"), 555555555L);
        AntifraudBlacklistRule rule2 = new AntifraudBlacklistRule(UID,
                "5412", CANCEL_ORDER_ACTION, "some_reason_2", ft.parse("30-05-2045 00:00:00 +0000"), 555555555L);
        AntifraudBlacklistRule rule3 = new AntifraudBlacklistRule(UID,
                "5413", CANCEL_ORDER_ACTION, "some_reason_3", ft.parse("30-05-2045 00:00:00 +0000"), null);
        antifraudDao.saveBlacklistRule(rule1);
        antifraudDao.saveBlacklistRule(rule2);
        antifraudDao.saveBlacklistRule(rule3);
        List<AntifraudBlacklistRule> rules =
                antifraudDao.getBlacklistRules(UID, Arrays.asList("5411", "5412"), CANCEL_ORDER_ACTION);
        assertThat(rules).hasSize(2);
    }


    @Test
    public void getBlackListRulesByPairs() throws Exception {
        SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss z");
        AntifraudBlacklistRule rule1 = new AntifraudBlacklistRule(UID,
                "4411", CANCEL_ORDER_ACTION, "some_reason_1", ft.parse("30-05-2045 00:00:00 +0000"), 555555555L);
        AntifraudBlacklistRule rule2 = new AntifraudBlacklistRule(PHONE,
                "88005553535", CANCEL_ORDER_ACTION, "some_reason_2", ft.parse("30-05-2045 00:00:00 +0000"), 555555555L);
        AntifraudBlacklistRule rule3 = new AntifraudBlacklistRule(UID,
                "4413", CANCEL_ORDER_ACTION, "some_reason_3", ft.parse("30-05-2045 00:00:00 +0000"), 555555555L);
        antifraudDao.saveBlacklistRule(rule1);
        antifraudDao.saveBlacklistRule(rule2);
        antifraudDao.saveBlacklistRule(rule3);
        List<AntifraudBlacklistRule> rules = antifraudDao.getBlacklistRules(
                List.of(
                        new ImmutablePair<>(UID, "4411"),
                        new ImmutablePair<>(PHONE, "88005553535")
                ),
                CANCEL_ORDER_ACTION);
        assertThat(rules).hasSize(2);
    }

    @Test
    public void removeBlackListRule() throws Exception {
        SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss Z");
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Moscow"));
        AntifraudBlacklistRule rule1 = new AntifraudBlacklistRule(UID,
                "4411", CANCEL_ORDER_ACTION,"some_reason_1", ft.parse("30-05-2045 00:00:00 +0300"), 555555555L);
        AntifraudBlacklistRule rule2 = new AntifraudBlacklistRule(UID,
                "4412", CANCEL_ORDER_ACTION,"some_reason_2", ft.parse("30-05-2045 00:00:00 +0300"), 555555555L);
        AntifraudBlacklistRule rule3 = new AntifraudBlacklistRule(UID,
                "4413", CANCEL_ORDER_ACTION,"some_reason_3", ft.parse("30-05-2045 00:00:00 +0300"), null);
        antifraudDao.saveBlacklistRule(rule1);
        antifraudDao.saveBlacklistRule(rule2);
        antifraudDao.saveBlacklistRule(rule3);
        AntifraudBlacklistRule oldRule = antifraudDao.removeBlacklistRule(rule1);
        assertThat(oldRule).isEqualTo(rule1);

        AntifraudBlacklistRule rule = antifraudDao.getBlacklistRule(UID, "4411", CANCEL_ORDER_ACTION);
        assertThat(rule).isNull();
    }

    @Test
    public void getFullBlacklistRule() throws Exception {
        SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss z");
        AntifraudBlacklistRule rule1 = new AntifraudBlacklistRule(UID,
                "321", CANCEL_ORDER_ACTION, "some_reason_1", ft.parse("30-05-2045 00:00:00 +0000"), 555555555L);
        AntifraudBlacklistRule rule2 = new AntifraudBlacklistRule(UID,
                "321", CANCEL_ORDER_ACTION, "some_reason_2", ft.parse("30-05-2045 00:00:00 +0000"), 555555556L);
        AntifraudBlacklistRule rule3 = new AntifraudBlacklistRule(UID,
                "321", CANCEL_ORDER_ACTION, "some_reason_3", ft.parse("30-05-2045 00:00:00 +0000"), 555555557L);
        Optional<AntifraudBlacklistRule> rule1O = antifraudDao.getBlacklistRuleO(UID, "321", CANCEL_ORDER_ACTION);
        assertThat(rule1O).isEmpty();
        antifraudDao.saveBlacklistRule(rule1);
        antifraudDao.removeBlacklistRule(rule2);
        antifraudDao.saveBlacklistRule(rule3);
        AntifraudBlacklistRule rule1Full = antifraudDao.getFullBlacklistRule(UID, "321", CANCEL_ORDER_ACTION);

        String expectedFullReason = "ADD 555555555 some_reason_1\nREMOVE 555555556 some_reason_2";
        assertThat(rule1Full.getReason()).isEqualTo(expectedFullReason);
    }

    @Test
    public void findRules() throws Exception {
        SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss z");
        AntifraudBlacklistRule rule1 = new AntifraudBlacklistRule(UID,
                "4411111", CANCEL_ORDER_ACTION, "some_reason_1", ft.parse("30-05-2045 00:00:00 +0000"), 555555555L);
        AntifraudBlacklistRule rule2 = new AntifraudBlacklistRule(UID,
                "4411112", CANCEL_ORDER_ACTION, "some_reason_2", ft.parse("30-05-2045 00:00:00 +0000"), 555555555L);
        AntifraudBlacklistRule rule3 = new AntifraudBlacklistRule(UID,
                "4422223", CANCEL_ORDER_ACTION, "some_reason_3", ft.parse("30-05-2045 00:00:00 +0000"), null);
        antifraudDao.saveBlacklistRule(rule1);
        antifraudDao.saveBlacklistRule(rule2);
        antifraudDao.saveBlacklistRule(rule3);
        List<AntifraudBlacklistRule> rules = antifraudDao.findBlacklistRules(null, "441111", CANCEL_ORDER_ACTION);
        assertThat(rules).hasSize(2);
        rules = antifraudDao.findBlacklistRules(ANY, "441111", CANCEL_ORDER_ACTION);
        assertThat(rules).hasSize(2);
        rules = antifraudDao.findBlacklistRules(ANY, "442222", CANCEL_ORDER_ACTION);
        assertThat(rules).hasSize(1);
    }

    @Test
    public void checkRemoveRules() throws Exception {
        SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss z");
        AntifraudBlacklistRule rule1 = new AntifraudBlacklistRule(PHONE,
                "12345", CANCEL_ORDER_ACTION, "some_reason_1", ft.parse("30-05-2045 00:00:00 +0000"), 555555555L);

        antifraudDao.saveBlacklistRule(rule1);
        List<AntifraudBlacklistRule> rules =
                antifraudDao.getBlacklistRules(List.of(new ImmutablePair<>(UID, "3321"),
                        new ImmutablePair<>(PHONE, "12345")),
                        CANCEL_ORDER_ACTION);
        assertThat(rules).hasSize(1);
        antifraudDao.removeBlacklistRule(rule1);
        rules = antifraudDao.getBlacklistRules(List.of(new ImmutablePair<>(UID, "3321"),
                new ImmutablePair<>(PHONE, "12345")),
                CANCEL_ORDER_ACTION);
        assertThat(rules).isEmpty();
    }

    @Test
    public void getBlacklistRules() throws ParseException {
        SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy");
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Moscow"));
        var date = ft.parse("30-05-2045");
        var uid = 655555555L;
        var rules = List.of(
                new AntifraudBlacklistRule(ANY, "001", CANCEL_ORDER_ACTION, "comment1", date, uid),
                new AntifraudBlacklistRule(PHONE, "001", CANCEL_ORDER_ACTION, "comment 2", date, uid),
                new AntifraudBlacklistRule(UID, "002", CANCEL_ORDER_ACTION, "123", date, uid),
                new AntifraudBlacklistRule(ANY, "003", CANCEL_ORDER_ACTION, "comment", date, uid)
        );
        String actionName = CANCEL_ORDER_ACTION;
        //reverse, т.к. в запросе ORDER BY id DESC
        Lists.reverse(rules).forEach(rule -> antifraudDao.saveBlacklistRule(rule));
        var allTypes = Arrays.asList(AntifraudBlacklistRuleType.values());

        var result = antifraudDao.getBlacklistRules(allTypes, null, actionName, null, null, null, null);
        assertThat(result).containsAll(rules);

        result = antifraudDao.getBlacklistRules(allTypes, null, actionName, null, uid, null, null);
        assertThat(result).isEqualTo(rules);

        result = antifraudDao.getBlacklistRules(allTypes, null, actionName, null, uid, 0, 3);
        assertThat(result).isEqualTo(rules.subList(0, 3));

        result = antifraudDao.getBlacklistRules(allTypes, null, actionName, null, uid, 1, 3);
        assertThat(result).isEqualTo(List.of(rules.get(3)));

        result = antifraudDao.getBlacklistRules(List.of(ANY, UID), null, actionName, null, uid, null, null);
        assertThat(result).isEqualTo(List.of(rules.get(0), rules.get(2), rules.get(3)));

        result = antifraudDao.getBlacklistRules(allTypes, "001", actionName, null, uid, null, null);
        assertThat(result).isEqualTo(rules.subList(0, 2));

        result = antifraudDao.getBlacklistRules(allTypes, null, actionName, "comment", uid, null, null);
        assertThat(result).isEqualTo(List.of(rules.get(0), rules.get(1), rules.get(3)));
    }

    @Test
    public void getAllRestrictions() throws ParseException {
        SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss z");
        AntifraudBlacklistRule rule1 = new AntifraudBlacklistRule(UID,
                "6411", CANCEL_ORDER_ACTION, "some_reason_1", ft.parse("30-05-2045 00:00:00 +0000"), 555555555L);
        AntifraudBlacklistRule rule2 = new AntifraudBlacklistRule(UID,
                "6412", AntifraudAction.PREPAID_ONLY.name(), "some_reason_2", ft.parse("30-05-2045 00:00:00 +0000"), 555555555L);
        AntifraudBlacklistRule rule3 = new AntifraudBlacklistRule(UID,
                "6413", Utils.getBlacklistAction(LoyaltyVerdictType.BLACKLIST), "some_reason_3", ft.parse("30-05-2045 00:00:00 +0000"), null);
        antifraudDao.saveBlacklistRule(rule1);
        antifraudDao.saveBlacklistRule(rule2);
        antifraudDao.saveBlacklistRule(rule3);
        List<AntifraudBlacklistRule> rules = antifraudDao.getAllRestrictions(UID, Arrays.asList("6411", "6412", "6413"));
        assertThat(rules).hasSize(3);
    }

    @Test
    public void getAntifraudRestrictions() throws ParseException {
        SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss z");
        AntifraudBlacklistRule rule1 = new AntifraudBlacklistRule(UID,
                "7411", CANCEL_ORDER_ACTION, "some_reason_1", ft.parse("30-05-2045 00:00:00 +0000"), 555555555L);
        AntifraudBlacklistRule rule2 = new AntifraudBlacklistRule(UID,
                "7412", AntifraudAction.PREPAID_ONLY.getActionName(), "some_reason_2", ft.parse("30-05-2045 00:00:00 +0000"), 555555555L);
        AntifraudBlacklistRule rule3 = new AntifraudBlacklistRule(UID,
                "7413", Utils.getBlacklistAction(LoyaltyVerdictType.BLACKLIST), "some_reason_3", ft.parse("30-05-2045 00:00:00 +0000"), null);
        antifraudDao.saveBlacklistRule(rule1);
        antifraudDao.saveBlacklistRule(rule2);
        antifraudDao.saveBlacklistRule(rule3);
        List<AntifraudBlacklistRule> rules = antifraudDao.getAntifraudRestrictions(UID, Arrays.asList("7411", "7412", "7413"));
        assertThat(rules).hasSize(2);
    }

    @Test
    public void getAntifraudRestrictionsByPairs() throws ParseException {
        SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss z");
        AntifraudBlacklistRule rule1 = new AntifraudBlacklistRule(UID,
                "8411", AntifraudAction.PREPAID_ONLY.getActionName(), "some_reason_1", ft.parse("30-05-2045 00:00:00 +0000"), 555555555L);
        AntifraudBlacklistRule rule2 = new AntifraudBlacklistRule(PHONE,
                "88005553536", CANCEL_ORDER_ACTION, "some_reason_2", ft.parse("30-05-2045 00:00:00 +0000"), 555555555L);
        AntifraudBlacklistRule rule3 = new AntifraudBlacklistRule(UID,
                "8411", Utils.getBlacklistAction(LoyaltyVerdictType.BLACKLIST), "some_reason_3", ft.parse("30-05-2045 00:00:00 +0000"), 555555555L);
        AntifraudBlacklistRule rule4 = new AntifraudBlacklistRule(UID,
                "8412", CANCEL_ORDER_ACTION, "some_reason_4", ft.parse("30-05-2045 00:00:00 +0000"), 555555555L);
        antifraudDao.saveBlacklistRule(rule1);
        antifraudDao.saveBlacklistRule(rule2);
        antifraudDao.saveBlacklistRule(rule3);
        antifraudDao.saveBlacklistRule(rule4);
        List<AntifraudBlacklistRule> rules = antifraudDao.getAntifraudRestrictions(
                List.of(
                        new ImmutablePair<>(UID, "8411"),
                        new ImmutablePair<>(PHONE, "88005553536")
                ));
        assertThat(rules).hasSize(2);
    }
}

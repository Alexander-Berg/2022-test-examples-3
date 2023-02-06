package ru.yandex.market.antifraud.orders.service.loyalty.detectors;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.node.BooleanNode;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRule;
import ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRuleType;
import ru.yandex.market.antifraud.orders.entity.MarketUserId;
import ru.yandex.market.antifraud.orders.entity.loyalty.LoyaltyBonusContext;
import ru.yandex.market.antifraud.orders.service.ConfigurationService;
import ru.yandex.market.antifraud.orders.service.Utils;
import ru.yandex.market.antifraud.orders.service.loyalty.LoyaltyBonusDetector;
import ru.yandex.market.antifraud.orders.storage.dao.AntifraudDao;
import ru.yandex.market.antifraud.orders.storage.dao.MarketUserIdDao;
import ru.yandex.market.antifraud.orders.storage.entity.configuration.ConfigEnum;
import ru.yandex.market.antifraud.orders.storage.entity.configuration.ConfigurationEntity;
import ru.yandex.market.antifraud.orders.test.annotations.IntegrationTest;
import ru.yandex.market.antifraud.orders.web.dto.loyalty.BonusState;
import ru.yandex.market.antifraud.orders.web.dto.loyalty.LoyaltyBonusInfoRequestDto;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@RunWith(SpringJUnit4ClassRunner.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BonusDisableMarkerDetectorTest {
    @Autowired
    MarketUserIdDao userIdDao;

    @Autowired
    AntifraudDao antifraudDao;

    @Autowired
    ConfigurationService configurationService;

    @Autowired
    LoyaltyBonusDetector bonusDisableMarkerDetector;

    @Before
    public void init() {
        configurationService.save(ConfigurationEntity.builder()
            .parameter(ConfigEnum.VOLVA_CHECK)
            .config(BooleanNode.FALSE)
            .build());
    }

    @Test
    public void notBlacklisted() {
        assertThat(bonusDisableMarkerDetector.checkBonuses(getContext(10001))).isEqualTo(BonusState.ENABLED);
    }

    @Test
    public void blacklisted() {
        arrangeIsBlacklisted(1002, 1003);
        assertThat(bonusDisableMarkerDetector.checkBonuses(getContext(1002))).isEqualTo(BonusState.DISABLED);
    }

    private void arrangeIsBlacklisted(long uid1, long uid2) {
        userIdDao.insertNewGlues(List.of(MarketUserId.fromUid(uid1), MarketUserId.fromUid(uid2)));
        antifraudDao.saveBlacklistRule(AntifraudBlacklistRule.builder()
                .type(AntifraudBlacklistRuleType.UID)
                .value(String.valueOf(uid2))
                .action(Utils.getBlacklistAction(BonusState.DISABLED))
                .expiryAt(Date.valueOf(LocalDate.now().plusDays(1)))
                .reason("")
                .build());
        // cache warmup for timeout
        antifraudDao.getBlacklistRules(AntifraudBlacklistRuleType.UID, Set.of(String.valueOf(uid1)), BonusState.DISABLED.name());
    }

    private LoyaltyBonusContext getContext(long uid) {
        return LoyaltyBonusContext.builder()
            .request(LoyaltyBonusInfoRequestDto.builder().uid(uid).build())
            .build();
    }
}

package ru.yandex.market.loyalty.core.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.programs.AddUserLoyaltyProgramResponse;
import ru.yandex.market.loyalty.api.model.programs.SetPerkError;
import ru.yandex.market.loyalty.core.dao.hash.ActionHashDao;
import ru.yandex.market.loyalty.core.model.hash.ActionHash;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.coin.SourceKey;
import ru.yandex.market.loyalty.core.service.hash.UserLoyaltyProgramService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static ru.yandex.market.loyalty.core.dao.hash.ActionHashDao.ACTION_HASH_TABLE;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.ANOTHER_UID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

/**
 * @author : poluektov
 * date: 2019-09-12.
 */
public class UserLoyaltyProgramServiceTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private UserLoyaltyProgramService userLoyaltyProgramService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private ActionHashDao actionHashDao;

    private Promo promo;
    private ActionHash generatedHash;

    @Before
    public void createPromo() {
        promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());
        userLoyaltyProgramService.insertActionHashes(
                promo.getId(), Stream.of("sourceKey-1").map(SourceKey::new).collect(Collectors.toSet()));
        generatedHash = actionHashDao.selectHashes(ACTION_HASH_TABLE.promoId.eqTo(promo.getId()), 1)
                .iterator().next();
    }

    @Test
    public void testBindingHashToUser() {
        AddUserLoyaltyProgramResponse response = userLoyaltyProgramService.addLoyaltyProgram(
                DEFAULT_UID, generatedHash.getUniqueKey(), promo.getPromoKey());
        assertThat(response.getErrors(), nullValue());
    }

    @Test
    public void testBindingHashWithWrongKey() {
        AddUserLoyaltyProgramResponse response = userLoyaltyProgramService.addLoyaltyProgram(
                DEFAULT_UID, "randomwronghash", String.valueOf(promo.getId()));
        assertThat(response.getErrors(), contains(SetPerkError.INVALID_CREDENTIALS));
    }

    @Test
    public void testBindingHashThatAlreadyUsed() {
        userLoyaltyProgramService.addLoyaltyProgram(
                DEFAULT_UID, generatedHash.getUniqueKey(), promo.getPromoKey());
        AddUserLoyaltyProgramResponse response = userLoyaltyProgramService.addLoyaltyProgram(
                ANOTHER_UID, generatedHash.getUniqueKey(), promo.getPromoKey());
        assertThat(response.getErrors(), contains(SetPerkError.ALREADY_REGISTERED_TO_ANOTHER_UID));
    }

    @Test
    public void testBindingHashWithWrongActionId() {
        AddUserLoyaltyProgramResponse response = userLoyaltyProgramService.addLoyaltyProgram(DEFAULT_UID,
                generatedHash.getUniqueKey(), "11186");
        assertThat(response.getErrors(), contains(SetPerkError.SALE_NOT_FOUND));
    }

}

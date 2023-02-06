package ru.yandex.market.loyalty.core.service.promogroup;

import java.time.Clock;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.promogroup.PromoGroupTokenAndType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promogroup.PromoGroup;
import ru.yandex.market.loyalty.core.model.promogroup.PromoGroupImpl;
import ru.yandex.market.loyalty.core.model.promogroup.PromoGroupPromo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
public class PromoGroupServiceTest extends MarketLoyaltyCoreMockedDbTestBase {

    @Autowired
    private PromoGroupService promoGroupService;
    @Autowired
    private Clock clock;
    @Autowired
    private PromoManager promoManager;

    private Promo promo;
    private Promo anotherPromo;

    @Before
    public void init() {
        promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );
        anotherPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed()
        );
    }

    @Test
    public void shouldSavePromoGroupAndGetById() {
        PromoGroup promoGroup = PromoGroupUtils.createDefaultPromoGroup(clock);
        long promoGroupId = promoGroupService.insertPromoGroupAndGetPromoGroupId(promoGroup);
        PromoGroup actualPromoGroup = promoGroupService.getPromoGroupById(promoGroupId)
                .orElseThrow(AssertionError::new);
        assertEquals((Long) promoGroupId, actualPromoGroup.getId());
        assertThat(actualPromoGroup, samePropertyValuesAs(promoGroup, "id"));
    }

    @Test
    public void shouldSavePromoGroupAndGetByToken() {
        PromoGroup promoGroup = PromoGroupUtils.createDefaultPromoGroup(clock);
        promoGroupService.insertPromoGroupAndGetPromoGroupId(promoGroup);
        PromoGroup actualPromoGroup = promoGroupService.getPromoGroupByTypeAndToken(
                PromoGroupUtils.DEFAULT_PROMO_GROUP_TYPE, PromoGroupUtils.DEFAULT_TOKEN
        )
                .orElseThrow(AssertionError::new);
        assertThat(actualPromoGroup, samePropertyValuesAs(promoGroup, "id"));
    }

    @Test
    public void shouldUpdatePromoGroup() {
        PromoGroup promoGroup = PromoGroupUtils.createDefaultPromoGroup(clock);
        long promoGroupId = promoGroupService.insertPromoGroupAndGetPromoGroupId(promoGroup);
        PromoGroupImpl savedPromoGroup = promoGroupService.getPromoGroupById(promoGroupId)
                .orElseThrow(AssertionError::new);
        PromoGroup modifiedPromoGroup = savedPromoGroup.from()
                .setName(PromoGroupUtils.ANOTHER_NAME)
                .build();
        assertTrue(promoGroupService.updatePromoGroup(modifiedPromoGroup));
        PromoGroup actualPromoGroup = promoGroupService.getPromoGroupById(promoGroupId)
                .orElseThrow(AssertionError::new);
        assertThat(actualPromoGroup, samePropertyValuesAs(modifiedPromoGroup));
    }

    @Test
    public void shouldSavePromoGroupPromos() {
        PromoGroup promoGroup = PromoGroupUtils.createDefaultPromoGroup(clock);
        long promoGroupId = promoGroupService.insertPromoGroupAndGetPromoGroupId(promoGroup);
        List<PromoGroupPromo> promoGroupPromos = PromoGroupUtils.createDefaultPromoGroupPromos(promoGroupId, promo);
        promoGroupService.replacePromoGroupPromos(promoGroupId, promoGroupPromos);
        List<PromoGroupPromo> actualPromoGroupPromos = promoGroupService.getPromoGroupPromosByPromoGroupId(
                promoGroupId
        );
        assertThat(actualPromoGroupPromos, containsInAnyOrder(
                promoGroupPromos.stream()
                        .map(promoGroupPromo -> samePropertyValuesAs(promoGroupPromo, "id"))
                        .collect(ImmutableList.toImmutableList())
        ));
    }

    @Test
    public void shouldUpdatePromoGroupPromos() {
        PromoGroup promoGroup = PromoGroupUtils.createDefaultPromoGroup(clock);
        long promoGroupId = promoGroupService.insertPromoGroupAndGetPromoGroupId(promoGroup);
        promoGroupService.replacePromoGroupPromos(promoGroupId,
                PromoGroupUtils.createDefaultPromoGroupPromos(promoGroupId, promo));
        List<PromoGroupPromo> savedPromoGroupPromos = promoGroupService.getPromoGroupPromosByPromoGroupId(promoGroupId);
        List<PromoGroupPromo> modifiedPromoGroupPromos = ImmutableList.of(
                savedPromoGroupPromos.get(0)
                        .from()
                        .setPromoId(anotherPromo.getId())
                        .build()
        );
        promoGroupService.replacePromoGroupPromos(promoGroupId, modifiedPromoGroupPromos);
        List<PromoGroupPromo> actualPromoGroupPromos = promoGroupService.getPromoGroupPromosByPromoGroupId(
                promoGroupId
        );
        assertThat(actualPromoGroupPromos, containsInAnyOrder(
                modifiedPromoGroupPromos.stream()
                        .map(promoGroupPromo -> samePropertyValuesAs(promoGroupPromo, "id"))
                        .collect(ImmutableList.toImmutableList())
        ));
    }

    @Test
    public void shouldGetPromoGroupWithPromosById() {
        PromoGroup promoGroup = PromoGroupUtils.createDefaultPromoGroup(clock);
        long promoGroupId = promoGroupService.insertPromoGroupAndGetPromoGroupId(promoGroup);
        promoGroupService.replacePromoGroupPromos(promoGroupId,
                PromoGroupUtils.createDefaultPromoGroupPromos(promoGroupId, promo));
        PromoGroupImpl.WithPromos expectedPromoGroupWithPromos = promoGroupService.getPromoGroupById(promoGroupId)
                .orElseThrow(AssertionError::new)
                .withPromos(promoGroupService.getPromoGroupPromosByPromoGroupId(promoGroupId));
        PromoGroupImpl.WithPromos promoGroupWithPromos = promoGroupService.getPromoGroupWithPromosById(promoGroupId)
                .orElseThrow(AssertionError::new);

        assertThat(promoGroupWithPromos, samePropertyValuesAs(expectedPromoGroupWithPromos, "promoGroupPromos"));
        assertThat(promoGroupWithPromos.getPromoGroupPromos(), containsInAnyOrder(
                expectedPromoGroupWithPromos.getPromoGroupPromos()
                        .stream()
                        .map(Matchers::samePropertyValuesAs)
                        .collect(ImmutableList.toImmutableList())
        ));
    }

    @Test
    public void shouldGetPromoGroupWithPromosByToken() {
        PromoGroup promoGroup = PromoGroupUtils.createDefaultPromoGroup(clock);
        long promoGroupId = promoGroupService.insertPromoGroupAndGetPromoGroupId(promoGroup);
        promoGroupService.replacePromoGroupPromos(promoGroupId,
                PromoGroupUtils.createDefaultPromoGroupPromos(promoGroupId, promo));
        PromoGroupImpl.WithPromos expectedPromoGroupWithPromos = promoGroupService.getPromoGroupById(promoGroupId)
                .orElseThrow(AssertionError::new)
                .withPromos(promoGroupService.getPromoGroupPromosByPromoGroupId(promoGroupId));
        PromoGroupTokenAndType tokenAndType = PromoGroupTokenAndType.Builder
                .builder()
                .promoGroupType(PromoGroupUtils.DEFAULT_PROMO_GROUP_TYPE)
                .token(PromoGroupUtils.DEFAULT_TOKEN)
                .build();
        PromoGroup.WithPromos promoGroupWithPromos = promoGroupService.getPromoGroupWithPromosByTypeAndToken(
                Collections.singletonList(tokenAndType)
        ).get(tokenAndType);

        assertThat(promoGroupWithPromos, samePropertyValuesAs(expectedPromoGroupWithPromos, "promoGroupPromos"));
        assertThat(promoGroupWithPromos.getPromoGroupPromos(), containsInAnyOrder(
                expectedPromoGroupWithPromos.getPromoGroupPromos()
                        .stream()
                        .map(Matchers::samePropertyValuesAs)
                        .collect(ImmutableList.toImmutableList())
        ));
    }

}

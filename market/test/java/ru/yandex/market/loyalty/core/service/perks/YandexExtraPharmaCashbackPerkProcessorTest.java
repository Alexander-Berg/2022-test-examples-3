package ru.yandex.market.loyalty.core.service.perks;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.perks.impl.YandexExtraPharmaCashbackPerkProcessor;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PerkAdditionalParamsKey;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.YANDEX_EXTRA_PHARMA_CASHBACK;
import static ru.yandex.market.loyalty.core.model.promo.RuleParameterName.PERK_TYPE;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.ALCOHOL_CATEGORY;
import static ru.yandex.market.loyalty.core.test.SupplementaryDataLoader.PHARMA_ROOT_CATEGORY_ID;

/**
 * @author <a href="mailto:khamitov-rail@yandex-team.ru">Rail Khamitov</a>
 * @date 18.10.2021
 */

public class YandexExtraPharmaCashbackPerkProcessorTest extends MarketLoyaltyCoreMockedDbTestBase {

    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private YandexExtraPharmaCashbackPerkProcessor processor;
    @Autowired
    private PromoManager promoManager;

    private Set<Integer> excludedCategoriesForPharmaCashback = Set.of(16345094, 15043354, 14414114, 14404643, 16339727,
            14401832, 16353290, 16345110, 16631570, 16039965, 13120303, 16039955, 16339999, 13742135, 15756589,
            15756581, 15043377, 16618540,
            17542340, 15185185, 16344890, 16345156, 14456422, 16344897, 15756357, 16631625, 14382443, 90540,
            16345172, 15756892, 90538, 90539, 90536, 16631636, 90537, 90534, 15756887, 90535, 16345180, 90532, 90533,
            15024705, 90530, 90531, 90528, 90529, 16747104, 90526, 90527, 15756910, 90524, 90525, 14947448, 15756907,
            90523, 90521, 90518, 15756903, 966040, 24627171, 15756900, 16737902, 15756897, 17730975, 6290384, 16344944,
            15756921, 17836952, 15756919, 15756915, 15756914, 14456486,
            16065421, 6498837, 16631698, 6296079, 16065434, 14728079, 16074651, 13458336, 16074648, 6296075, 14424250,
            7869470, 16065425, 16074646, 989037, 16051350, 14456451, 1009488,
            1009490, 1009491, 6091783, 13077405, 6203657, 6203656, 16074656, 16065440, 6203658, 6203660, 16089018,
            13005962, 16739763, 278351, 1009481, 1009482, 16345021, 1009483, 1009484,
            1009485, 8475840, 1009486, 16283321, 15754673, 1009487, 6290273, 6290276,
            6290278, 14456555, 16669131, 14404075, 15756510, 16345044, 15756506, 15806939, 15756503, 13744375,
            15756502, 15758037, 454690, 16345063,
            15756525, 15756517, 15315708, 16650220, 15756513, 14456524, 15988990, 16739316, 6290261, 15756538,
            6290262, 14695400, 16080381, 16650233, 818955, 6290266, 6290267, 6290268, 6290271);

    /*
     * Условия похода в YDB (метод userDontHaveMaxCountExtraPharmaCashbackOrders)
     * 1) пользователь с фармой (false true = 1) = идем в YDB
     * 2) пользователь без фармы (false false = 0) = НЕ идем в YDB
     * 3) пользователь не калк (true true = 1) = идем в YDB
     * */

    @Before
    public void initConfig() {
        Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultPercent(BigDecimal.valueOf(20))
                        .addCashbackRule(RuleType.PERKS_ALLOWED_CUTTING_RULE, PERK_TYPE, YANDEX_EXTRA_PHARMA_CASHBACK)
                        .addCashbackRule(RuleType.CATEGORY_FILTER_RULE, RuleParameterName.CATEGORY_ID,
                                excludedCategoriesForPharmaCashback)
                        .setPriority(0)
        );
        cashbackCacheService.reloadCashbackPromos();
        cashbackCacheService.reloadExtraCashbackPromoList();
        configurationService.set(ConfigurationService.YANDEX_EXTRA_PHARMA_CASHBACK_PROMO_KEY,
                cashbackPromo.getPromoKey());
    }

    @Test
    public void case_1_shouldGoToYdbHavePharmaCategoryItem() {
        long pharmaItemCount = processor.getPharmaItemCount(Map.of(PerkAdditionalParamsKey.ITEM_CATEGORIES,
                List.of(PHARMA_ROOT_CATEGORY_ID)));
        assertTrue(pharmaItemCount > 0);
    }

    @Test
    public void case_2_shouldNoGoToYdbIfNoPharmaCategoryItem() {
        long pharmaItemCount = processor.getPharmaItemCount(Map.of(PerkAdditionalParamsKey.ITEM_CATEGORIES,
                List.of(ALCOHOL_CATEGORY)));
        assertFalse(pharmaItemCount > 0);
    }
}

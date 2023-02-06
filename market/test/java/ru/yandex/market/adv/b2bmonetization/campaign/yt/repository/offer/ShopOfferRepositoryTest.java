package ru.yandex.market.adv.b2bmonetization.campaign.yt.repository.offer;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.adv.b2bmonetization.campaign.model.campaign.Color;
import ru.yandex.market.adv.b2bmonetization.campaign.yt.entity.ShopOffer;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;

public class ShopOfferRepositoryTest extends AbstractMonetizationTest {

    @Autowired
    private ShopOfferRepository repository;

    private static final long PARTNER_ID = 1L;

    private static final List<String> OFFER_IDS = List.of("K1", "K2", "K3", "K4", "K5");
    private static final List<String> NOT_EXISTED_OFFER_IDS = List.of("A1", "A2", "A3", "A4", "A5");

    @DisplayName("Получение списка предложений, прошедших проверку")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ShopOffer.class,
                    path = "//tmp/adv_unittest/shop_offer"
            ),
            before = "CampaignRepository/json/shopOffer/getCheckedOfferIds_someIdsExist_checkedIdsList.before.json"
    )
    public void getCheckedOfferIds_someIdsExist_checkedIdsList() {
        Assertions.assertThat(repository.getCheckedOfferIds(PARTNER_ID, OFFER_IDS, Color.WHITE))
                .containsOnlyKeys("K1", "K3", "K4");
    }

    @DisplayName("Получение списка предложений, прошедших проверку с пустым ответом")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ShopOffer.class,
                    path = "//tmp/adv_unittest/shop_offer"
            ),
            before = "CampaignRepository/json/shopOffer/getCheckedOfferIds_notExistedIds_emptyList.before.json"
    )
    public void getCheckedOfferIds_notExistedIds_emptyList() {
        Assertions.assertThat(repository.getCheckedOfferIds(PARTNER_ID, NOT_EXISTED_OFFER_IDS, Color.WHITE))
                .isEmpty();
    }
}

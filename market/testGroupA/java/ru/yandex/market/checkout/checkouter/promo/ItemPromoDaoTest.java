package ru.yandex.market.checkout.checkouter.promo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.storage.promo.OrderItemPromoDao;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkouter.entities.OrderItemPromoEntity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

/**
 * @author : poluektov
 * date: 2020-09-04.
 */
public class ItemPromoDaoTest extends AbstractWebTestBase {

    private static final BigDecimal PRECISE_ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);

    @Autowired
    OrderItemPromoDao orderItemPromoDao;
    @Autowired
    OrderCreateHelper orderCreateHelper;
    @Autowired
    TransactionTemplate transactionTemplate;

    @Test
    public void testInsertCashbackValue() {
        BigDecimal expectedCashbackAccrualAmount = BigDecimal.valueOf(123L);
        BigDecimal expectedCashbackSpendLimit = BigDecimal.valueOf(321L);
        BigDecimal expectedCashbackPercentMarket = PRECISE_ZERO;
        BigDecimal expectedCashbackPercentPartner = new BigDecimal("100");
        String expectedCmsDescriptionSemanticId = "partner-default-cashback";
        List<String> expectedUiPromoFlags = List.of("ui1, ui2");
        String expectedDetailsGroupName = "details-group-name";
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        OrderItemPromoEntity entity = new OrderItemPromoEntity();
        OrderItem item = order.getItems().iterator().next();
        entity.setItemId(item.getId());
        entity.setBuyerDiscount(BigDecimal.ZERO);
        entity.setBuyerSubsidy(BigDecimal.ZERO);
        entity.setSubsidy(BigDecimal.ZERO);
        entity.setCashbackAccrualAmount(expectedCashbackAccrualAmount);
        entity.setCashbackSpendLimit(expectedCashbackSpendLimit);
        entity.setGiftCount(BigDecimal.ZERO);
        entity.setMarketCashbackPercent(expectedCashbackPercentMarket);
        entity.setPartnerCashbackPercent(expectedCashbackPercentPartner);
        entity.setPartnerId(1L);
        entity.setCmsDescriptionSemanticId(expectedCmsDescriptionSemanticId);
        entity.setUiPromoFlags(expectedUiPromoFlags);
        entity.setDetailsGroupNameId(expectedDetailsGroupName);
        entity.setNominal(BigDecimal.TEN);
        entity.setPriority(50);
        entity.setPromoBucketName("promo_bucket_name");
        entity.setMarketTariffsVersionId(1L);
        List<String> expectedThresholds = List.of("1", "2");
        entity.setThresholds(expectedThresholds);
        transactionTemplate.execute(ts -> {
            orderItemPromoDao.insertItemsPromos(List.of(entity));
            return null;
        });

        List<OrderItemPromoEntity> promosFromDb = orderItemPromoDao.getItemPromos(Set.of(item.getId()));
        assertThat(promosFromDb, hasSize(1));
        OrderItemPromoEntity actualEntity = Iterables.getOnlyElement(promosFromDb);
        assertThat(actualEntity.getPartnerId(), equalTo(1L));
        assertThat(actualEntity.getBuyerDiscount(), equalTo(PRECISE_ZERO));
        assertThat(actualEntity.getBuyerSubsidy(), equalTo(PRECISE_ZERO));
        assertThat(actualEntity.getSubsidy(), equalTo(PRECISE_ZERO));
        assertThat(actualEntity.getCashbackAccrualAmount(),
                equalTo(expectedCashbackAccrualAmount.setScale(2, RoundingMode.UNNECESSARY)));
        assertThat(actualEntity.getCashbackSpendLimit(),
                equalTo(expectedCashbackSpendLimit.setScale(2, RoundingMode.UNNECESSARY)));
        assertThat(actualEntity.getGiftCount(), equalTo(BigDecimal.ZERO));
        assertThat(actualEntity.getMarketCashbackPercent(), equalTo(expectedCashbackPercentMarket));
        assertThat(actualEntity.getPartnerCashbackPercent(),
                equalTo(expectedCashbackPercentPartner.setScale(2, RoundingMode.UNNECESSARY)));
        assertThat(actualEntity.getCmsDescriptionSemanticId(), equalTo(expectedCmsDescriptionSemanticId));
        assertThat(actualEntity.getDetailsGroupNameId(), equalTo(expectedDetailsGroupName));
        assertThat(actualEntity.getUiPromoFlags(), equalTo(expectedUiPromoFlags));
        assertThat(actualEntity.getNominal(), comparesEqualTo(BigDecimal.TEN));
        assertThat(actualEntity.getPriority(), comparesEqualTo(50));
        assertThat(actualEntity.getPromoBucketName(), equalTo("promo_bucket_name"));
        assertThat(actualEntity.getMarketTariffsVersionId(), comparesEqualTo(1L));
        assertThat(actualEntity.getThresholds(), equalTo(expectedThresholds));
    }
}

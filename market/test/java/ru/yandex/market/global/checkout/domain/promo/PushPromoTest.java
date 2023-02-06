package ru.yandex.market.global.checkout.domain.promo;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.factory.TestPromoFactory;
import ru.yandex.market.global.db.jooq.enums.EPromoCommunicationType;
import ru.yandex.market.global.db.jooq.tables.pojos.Promo;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PushPromoTest extends BaseFunctionalTest {

    private final TestPromoFactory testPromoFactory;

    public final PromoQueryService promoQueryService;

    @Test
    public void testPushBlockMissing() {
        Promo promo = testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupCommunicationTypes(it -> new EPromoCommunicationType[]{
                        EPromoCommunicationType.CHECKOUT,
                        EPromoCommunicationType.INFORMER
                })
                .setupCommunicationArgs(it ->
                        it.push(null)
                )
                .build());

        Promo actPromo = promoQueryService.getPromoById(promo.getId());

        Assertions.assertThat(actPromo).usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                .withComparedFields("communicationTypes",
                        "communicationArgs").build()).isEqualTo(promo);
    }

}

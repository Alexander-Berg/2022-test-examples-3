package ru.yandex.market.loyalty.core.service.promogroup;

import org.junit.Test;

import ru.yandex.market.loyalty.core.model.promogroup.PromoGroupImpl;
import ru.yandex.market.loyalty.core.model.promogroup.PromoGroupPromo;
import ru.yandex.market.loyalty.api.model.promogroup.PromoGroupType;

import java.time.LocalDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
public class PromoGroupModelTest {
    @Test
    public void promoGroupFromShouldCopy() {
        PromoGroupImpl promoGroup = new PromoGroupImpl(
                0L, PromoGroupType.EFIM, "a", LocalDateTime.now(), LocalDateTime.now().plusDays(1), "b",
                null);
        assertThat(promoGroup.from().build(), samePropertyValuesAs(promoGroup));
    }

    @Test
    public void promoGroupPromoFromShouldCopy() {
        PromoGroupPromo promoGroupPromo = new PromoGroupPromo(0L, 1L, 2L, 3);
        assertThat(promoGroupPromo.from().build(), samePropertyValuesAs(promoGroupPromo));
    }
}

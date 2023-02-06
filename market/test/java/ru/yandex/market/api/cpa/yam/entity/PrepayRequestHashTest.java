package ru.yandex.market.api.cpa.yam.entity;

import org.junit.Test;

import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.core.application.PartnerApplicationStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

/**
 * Unit тест для проверки хэша на изменение в {@link PrepayRequest}.
 *
 * @author avetokhin 10/04/17.
 */
public class PrepayRequestHashTest {

    @Test
    public void test() {
        final PrepayRequest r1 = createPrepayRequest();
        final PrepayRequest r2 = createPrepayRequest();

        assertThat(r1.updatingHash(), equalTo(r2.updatingHash()));

        // Обновим незначимое поле
        r1.setComment("new comment");
        assertThat(r1.updatingHash(), equalTo(r2.updatingHash()));

        // Обновим значимое поле
        r1.setBankName("new bank name");
        assertThat(r1.updatingHash(), not(equalTo(r2.updatingHash())));
    }

    private PrepayRequest createPrepayRequest() {
        final PrepayRequest request = new PrepayRequest(1L, PrepayType.YANDEX_MARKET, PartnerApplicationStatus.INIT, 2L);
        request.setBankName("bank name");
        request.setComment("comment");
        request.setOrganizationName("org name");
        return request;
    }

}

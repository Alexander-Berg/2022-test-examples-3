package ru.yandex.market.common.report.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class ConsolidateRequestUrlBuilderTest {

    @Test
    public void shouldProvideClientAndCoFromIfSpecified() {
        ConsolidateRequestUrlBuilder builder = new ConsolidateRequestUrlBuilder("blablabla.ru");
        builder.setClient("checkout");
        builder.setCoFrom("checkouter");
        String build = builder.build(new ConsolidateRequest(213L, Color.BLUE, prepareOffersList()));

        assertThat(build, containsString("place=consolidate"));
        assertThat(build, containsString("rids=213"));
        assertThat(build, containsString("rgb=blue"));
        assertThat(build, containsString("RandomWareMd5_1:2;msku:sku-1"));
        assertThat(build, containsString("RandomWareMd5_2:3;msku:sku-2"));
        assertThat(build, containsString("RandomWareMd5_3:4;msku:sku-3;bundle_id:b3;promo_id:pkey3;promo_type:ptype3"));
        assertThat(build, containsString("client=checkout"));
        assertThat(build, containsString("co-from=checkouter"));
        assertThat(build, containsString("adult=1"));

        assertThat(build, not(containsString("uid")));
        assertThat(build, not(containsString("uid-type")));
    }

    @Test
    public void shouldProvideUidAndUidTypeIfSpecified() {
        ConsolidateRequestUrlBuilder builder = new ConsolidateRequestUrlBuilder("blablabla.ru");
        String build = builder.build(new ConsolidateRequest(213L, Color.BLUE, prepareOffersList(),
                true, "test-uid-123", UserIdType.UUID));
        assertThat(build, containsString("uid=test-uid-123"));
        assertThat(build, containsString("uid-type=uuid"));
    }

    private List<CartOffer> prepareOffersList() {
        return Arrays.asList(
                new CartOffer("RandomWareMd5_1", 2, "sku-1"),
                new CartOffer("RandomWareMd5_2", 3, "sku-2"),
                new CartOffer("RandomWareMd5_3", 4, "sku-3", "b3", "pkey3", "ptype3", null)
        );
    }
}

package ru.yandex.travel.commons.experimental;

import java.util.EnumMap;

import io.grpc.Context;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.junit.Test;

import ru.yandex.travel.commons.experiments.ExperimentDataProvider;
import ru.yandex.travel.commons.experiments.ExperimentFlag;
import ru.yandex.travel.commons.experiments.OrderExperiments;
import ru.yandex.travel.commons.http.CommonHttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;

public class ExperimentDataProviderTest {
    ExperimentDataProvider provider = new ExperimentDataProvider();

    @Test
    public void testGetExpsFromHeaders() throws Exception {
        var headers = new EnumMap<CommonHttpHeaders.HeaderType, String>(CommonHttpHeaders.HeaderType.class);

        String h = "{\"HOTELS_boymeta_page_is\":\"enabled\",\"HOTELS_use_default_deferred_payments\":\"enabled\"," +
                "\"HOTELS_deferred_payments\":\"enabled\",\"TRAINS_bandit_type\":\"neural-bandit-default\"," +
                "\"back_flags\":\"BOY_AFL_ENABLE=1,BOY_DESKTOP_AFL_ENABLE=1,BOY_TOUCH_AFL_ENABLE=1\"," +
                "\"AVIA_PAYMENT\":\"redesign\",\"AVIA_happy_page_is\":\"enabled\"}";

        headers.put(CommonHttpHeaders.HeaderType.EXPERIMENTS, h);
        CommonHttpHeaders commonHttpHeaders = new CommonHttpHeaders(headers);

        TestExperimentData d = Context.current().withValue(CommonHttpHeaders.KEY, commonHttpHeaders).call(
                () -> provider.getInstance(TestExperimentData.class, CommonHttpHeaders.get()));
        assertThat(d.isBoyMetaPage()).isTrue();
        assertThat(d.isDeferredPayments()).isTrue();
        assertThat(d.getBanditType()).isEqualTo("neural-bandit-default");
    }

    @Test
    public void testNoHeaders() throws Exception {
        var d = provider.getInstance(TestExperimentData.class, CommonHttpHeaders.get());
        assertThat(d).isNotNull();
        assertThat(d.isDeferredPayments()).isFalse();
        assertThat(d.isBoyMetaPage()).isFalse();
        assertThat(d.getBanditType()).isNull();
    }

    @Test
    public void testEmptyHeaders() throws Exception {
        var headers = new EnumMap<CommonHttpHeaders.HeaderType, String>(CommonHttpHeaders.HeaderType.class);
        headers.put(CommonHttpHeaders.HeaderType.EXPERIMENTS, "");
        CommonHttpHeaders commonHttpHeaders = new CommonHttpHeaders(headers);

        TestExperimentData d = Context.current().withValue(CommonHttpHeaders.KEY, commonHttpHeaders).call(
                () -> provider.getInstance(TestExperimentData.class, CommonHttpHeaders.get()));
        assertThat(d).isNotNull();
        assertThat(d.isDeferredPayments()).isFalse();
        assertThat(d.isBoyMetaPage()).isFalse();
        assertThat(d.getBanditType()).isNull();
    }

    @Test
    public void testOrderExperiments() throws Exception {
        String h = "{\"HOTELS_boymeta_page_is\":\"enabled\",\"HOTELS_use_default_deferred_payments\":\"enabled\"," +
                "\"HOTELS_deferred_payments\":\"enabled\",\"TRAINS_bandit_type\":\"neural-bandit-default\"," +
                "\"back_flags\":\"BOY_AFL_ENABLE=1,BOY_DESKTOP_AFL_ENABLE=1,BOY_TOUCH_AFL_ENABLE=1\"," +
                "\"AVIA_PAYMENT\":\"redesign\",\"AVIA_happy_page_is\":\"enabled\"}";
        var headers = new EnumMap<CommonHttpHeaders.HeaderType, String>(CommonHttpHeaders.HeaderType.class);
        headers.put(CommonHttpHeaders.HeaderType.EXPERIMENTS, h);
        CommonHttpHeaders commonHttpHeaders = new CommonHttpHeaders(headers);

        OrderExperiments d = Context.current().withValue(CommonHttpHeaders.KEY, commonHttpHeaders).call(
                () -> provider.getInstance(OrderExperiments.class, CommonHttpHeaders.get()));
        assertThat(d).isNotNull();
    }


    @Getter
    @Setter
    @NoArgsConstructor
    public static class TestExperimentData {
        @ExperimentFlag("HOTELS_boymeta_page_is")
        private boolean boyMetaPage;
        @ExperimentFlag("HOTELS_deferred_payments")
        private boolean deferredPayments;
        @ExperimentFlag("TRAINS_bandit_type")
        private String banditType;
    }
}

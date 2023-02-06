package ru.yandex.market.shopadminstub.stub;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.UriUtils;
import ru.yandex.market.helpers.CartHelper;
import ru.yandex.market.helpers.CartParameters;
import ru.yandex.market.providers.CartRequestProvider;
import ru.yandex.market.shopadminstub.application.AbstractTestBase;
import ru.yandex.market.shopadminstub.model.CartRequest;
import ru.yandex.market.util.report.ReportConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class StubRearrFactorsTest extends AbstractTestBase {

    @Autowired
    private CartHelper cartHelper;
    @Autowired
    private ReportConfigurer reportConfigurer;

    @Test
    public void shouldPassExperimentsToReport() throws Exception {
        String experiments = "market_blue_ignore_supplier_filter=1";

        CartRequest request = CartRequestProvider.buildCartRequest();
        request.setExperiments(experiments);

        cartHelper.cart(new CartParameters(request));

        List<ServeEvent> reportEvents = reportConfigurer.getReportEvents();

        assertThat(reportEvents, hasSize(3));

        List<ServeEvent> offerInfoRequests = reportEvents.stream()
                .filter(se -> se.getRequest().getUrl().contains("place=offerinfo"))
                .collect(Collectors.toList());

        ServeEvent event = offerInfoRequests.get(0);
        assertThat(
                event.getRequest().getUrl(),
                CoreMatchers.containsString("rearr-factors=" + UriUtils.encode(experiments, StandardCharsets.UTF_8))
        );
    }
}

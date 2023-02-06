package ru.yandex.market.api.internal.report;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.market.api.common.client.rules.BlueRule;
import ru.yandex.market.api.common.client.rules.GreenWithBlueReportRule;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.common.GenericParamsBuilder;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;

/**
 * @author Ural Yulmukhametov <a href="mailto:ural@yandex-team.ru"></a>
 * @date 15.10.2020
 */
@WithMocks
public class ReportRootUrlResolverTest extends BaseTest {

    private static final String WHITE_URL = "WHITE_REPORT";
    private static final String BLUE_URL = "BLUE_REPORT";

    private static final Client MOBILE = new Client() {{
        setType(Type.MOBILE);
        setShowShopUrl(true);
    }};

    @Mock
    private BlueRule blueRule;

    @Mock
    private GreenWithBlueReportRule greenWithBlueReportRule;

    private ReportRootUrlResolver resolver;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.resolver = new ReportRootUrlResolver(blueRule, greenWithBlueReportRule, WHITE_URL, BLUE_URL);
    }

    private void mockBlueRule(boolean blue) {
        Mockito.when(blueRule.test(Mockito.any())).thenReturn(blue);
    }

    private void mockGreenWithBlueRule(boolean greenWithBlueReport) {
        Mockito.when(greenWithBlueReportRule.test(Mockito.any())).thenReturn(greenWithBlueReport);
    }

    private void checkResolver(String url) {
        checkResolver(url, null);
    }

    private void checkResolver(String url, CommonReportOptions.Place place) {
        Assert.assertEquals(url, resolver.resolve(context, place));
    }

    @Test
    public void whiteUrlByDefault() {
        mockBlueRule(false);
        mockGreenWithBlueRule(false);
        checkResolver(WHITE_URL);
    }

    @Test
    public void whiteUrlWithoutContext() {
        mockBlueRule(false);
        mockGreenWithBlueRule(false);
        Assert.assertEquals(WHITE_URL, resolver.resolve(null, null));
    }

    @Test
    public void blueUrlIfRule() {
        mockBlueRule(true);
        mockGreenWithBlueRule(false);
        checkResolver(BLUE_URL);
    }

    @Test
    public void blueUrlIfNotBlueRule() {
        mockBlueRule(false);
        mockGreenWithBlueRule(true);
        checkResolver(WHITE_URL, CommonReportOptions.Place.OFFERINFO);
        checkResolver(WHITE_URL, CommonReportOptions.Place.PRIME);
        checkResolver(WHITE_URL, null);
    }

    @Test
    public void greenWithBlueRuleOverrideBlueRuleForOfferInfo() {
        mockBlueRule(true);
        mockGreenWithBlueRule(true);
        checkResolver(WHITE_URL, CommonReportOptions.Place.OFFERINFO);
        checkResolver(BLUE_URL, CommonReportOptions.Place.PRIME);
        checkResolver(BLUE_URL, null);
    }

    @Test
    public void whiteUrlIfForced() {
        mockBlueRule(true);
        mockGreenWithBlueRule(false);
        context.setClient(MOBILE);
        context.setGenericParams(
                new GenericParamsBuilder()
                        .setRearrFactors("beru_use_white_report=1")
                        .build()
        );
        checkResolver(WHITE_URL);
    }

    @Test
    public void ignoreForcedRearrIfNotMobile() {
        mockBlueRule(true);
        mockGreenWithBlueRule(false);
        context.setGenericParams(
                new GenericParamsBuilder()
                        .setRearrFactors("beru_use_white_report=1")
                        .build()
        );
        checkResolver(BLUE_URL);
    }

}

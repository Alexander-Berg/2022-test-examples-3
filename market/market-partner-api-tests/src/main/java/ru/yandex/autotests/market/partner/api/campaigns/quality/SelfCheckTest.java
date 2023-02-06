package ru.yandex.autotests.market.partner.api.campaigns.quality;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.market.billing.backend.core.dao.billing.BillingDaoFactory;
import ru.yandex.autotests.market.partner.api.ProjectConfig;
import ru.yandex.autotests.market.abo.AboSteps;
import ru.yandex.autotests.market.partner.api.steps.QualitySteps;
import ru.yandex.autotests.market.partner.beans.Format;
import ru.yandex.autotests.market.partner.beans.PartnerApiRequestData;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;

import java.io.IOException;
import java.util.Arrays;

import static ru.yandex.autotests.market.partner.api.data.QualityRequests.selfCheckRequest;

/**
 * @author m-bazhenov
 * @date 10/02/2017
 */
@Aqua.Test(title = "Проверка ручки /self-check")
@Features("Quality")
@Stories("Self-check")
@Issue("https://st.yandex-team.ru/AUTOTESTMARKET-5272")
@RunWith(Parameterized.class)
public class SelfCheckTest {

    private static final ProjectConfig CONFIG = new ProjectConfig();
    private static final AboSteps aboSteps = new AboSteps();
    private static final String CHECK_ORDER_ALREADY_STARTED = "Check order already started";

    private final QualitySteps qualitySteps = new QualitySteps();

    private final PartnerApiRequestData request;

    public SelfCheckTest(final Format format) {
        this.request = selfCheckRequest(CONFIG.getToken(), CONFIG.getClientId(), CONFIG.getCampaignId(), format);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> parameterize() {
        return Arrays.asList(
                new Object[] {Format.XML},
                new Object[] {Format.JSON}
        );
    }

    @Step("Останавливаем самопроверку, начатую до теста")
    @Before
    public void stopPreviousSelfCheck() throws IOException {
        int shopId = BillingDaoFactory.getBillingDao().takeShopIdByCampaignId(request.getCampaignId());
        aboSteps.cancelSelfCheck(shopId);
    }

    @Test
    public void shouldNotSelfCheckTwiceInARow() {
        qualitySteps.requestSelfCheck(request);
        qualitySteps.checkResponseHasError(request, CHECK_ORDER_ALREADY_STARTED);
    }
}

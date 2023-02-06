package ru.yandex.market.mbi.api.testing;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ru.yandex.market.core.billing.BillingService;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.ds.DatasourceTransactionTemplate;
import ru.yandex.market.core.protocol.model.ActionType;
import ru.yandex.market.core.protocol.model.SystemActionContext;
import ru.yandex.market.mbi.api.client.entity.ApiResponse;

/**
 * @author zoom
 */
@Profile({"testing", "development"})
@Controller
public class TestingPaymentController {
    private static final int SERVICE_ID = 11;

    private final DatasourceTransactionTemplate template;
    private final CampaignService campaignService;
    private final BillingService billingService;

    @Autowired
    public TestingPaymentController(DatasourceTransactionTemplate template,
                                    CampaignService campaignService,
                                    BillingService billingService) {
        this.template = template;
        this.campaignService = campaignService;
        this.billingService = billingService;
    }

    @ResponseBody
    @RequestMapping(value = "/testing/shops/{shopId}/payment", method = RequestMethod.POST)
    public Object registerPayment(@PathVariable("shopId") long shopId, @RequestParam("amount") int amount) {
        template.execute(shopId, new SystemActionContext(ActionType.TEST_ACTION), c -> {
            CampaignInfo info = campaignService.getCampaignByDatasource(shopId);
            BigDecimal paidTotal = new BigDecimal(amount);
            billingService.registerPayment(SERVICE_ID, info.getId(), paidTotal, paidTotal.toBigInteger(),
                    0, c.getActionId());
        });
        return ApiResponse.OK;
    }
}

package ru.yandex.market.api.util.httpclient.clients;

import org.springframework.stereotype.Service;

@Service
public class PartnerStatTestClient extends AbstractFixedConfigurationTestClient {

    protected PartnerStatTestClient() {
        super("PartnerStat");
    }

    public void getDistributionOrders(String clid, String total,
                                      String expectedResponseFileName) {
        configure(b -> b
                .param("clids", clid)
                .param("total", total))
                .ok().body(expectedResponseFileName);
    }

    public void getDistributionOrderById(String clid, String orderId, String total,
                                      String expectedResponseFileName) {
        configure(b -> b
                .param("clids", clid)
                .param("orderId", orderId)
                .param("total", total))
                .ok().body(expectedResponseFileName);
    }

    public void getDistributionStatsClicks(String clid, String dateStart, String dateEnd,
                                           String expectedResponseFileName
    ) {
        configure(b -> b
                .param("clid", clid)
                .param("dateStart", dateStart)
                .param("dateEnd", dateEnd))
                .ok().body(expectedResponseFileName);
    }
}

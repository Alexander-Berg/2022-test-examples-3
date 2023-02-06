package ru.yandex.market.api.util.httpclient.clients;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import ru.yandex.market.api.domain.PageInfo;

@Service
public class AffiliatePromoTestClient extends AbstractFixedConfigurationTestClient {

    protected AffiliatePromoTestClient() {
        super("AffiliatePromo");
    }

    public void getCatalogPromos(PageInfo pageInfo, String expectedResponseFileName) {
        configure(b -> b
                    .param("numResults", String.valueOf(pageInfo.getCount()))
                    .param("pageNum", String.valueOf(pageInfo.getPage())))
                .ok().body(expectedResponseFileName);
    }

    public void getCatalogPromosFull(PageInfo pageInfo, List<Integer> categories, String expectedResponseFileName) {
        configure(b -> b
                    .param("numResults", String.valueOf(pageInfo.getCount()))
                    .param("pageNum", String.valueOf(pageInfo.getPage()))
                    .param("categories", StringUtils.join(categories, ',')))
                .ok().body(expectedResponseFileName);
    }
}

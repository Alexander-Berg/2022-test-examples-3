package ru.yandex.autotests.direct.httpclient.data.banners.searchbanners;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.List;

/**
 * Created by shmykov on 16.06.15.
 */
public class SearchBannersResponseBean {

    @JsonPath(responsePath = "banners/bid")
    private List<String> bids;

    public List<String> getBids() {
        return bids;
    }

    public void setBids(List<String> bids) {
        this.bids = bids;
    }
}

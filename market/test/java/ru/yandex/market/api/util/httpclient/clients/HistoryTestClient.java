package ru.yandex.market.api.util.httpclient.clients;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import ru.yandex.market.api.domain.PageInfo;

@Service
public class HistoryTestClient extends AbstractFixedConfigurationTestClient {

    @Inject
    public HistoryTestClient() {
        super("PersHistory");
    }

    public void getHistory(long uid, PageInfo pageInfo, String expectedFileName) {
        configure(x -> x.serverMethod("/history/UID/" + uid)
            .param("limit", String.valueOf(pageInfo.getCount())))
            .body(expectedFileName)
            .ok();
    }

    public void merge(String userFrom, long userTo, int limit) {
        configure(
                x -> x.patch()
                        .serverMethod("history/UUID/" + userFrom)
                        .param("idTo", String.valueOf(userTo))
                        .param("limit", String.valueOf(limit))
        ).ok().emptyResponse();
    }
}

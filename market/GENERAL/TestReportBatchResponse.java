package ru.yandex.market.tsum.clients.kombat;

import java.util.List;

public class TestReportBatchResponse {
    private final List<Battle> battles;

    TestReportBatchResponse(List<Battle> battles) {
        this.battles = battles;
    }

    public List<Battle> getBattles() {
        return battles;
    }
}

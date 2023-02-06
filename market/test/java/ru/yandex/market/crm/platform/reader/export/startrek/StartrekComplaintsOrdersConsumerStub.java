package ru.yandex.market.crm.platform.reader.export.startrek;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ru.yandex.market.crm.platform.config.FactConfig;
import ru.yandex.market.crm.platform.reader.export.startrek.parsers.StartrekComplaintOrderParser;
import ru.yandex.market.crm.platform.services.facts.impl.FactsServiceImpl;
import ru.yandex.market.mcrm.startrek.support.StartrekResult;

public class StartrekComplaintsOrdersConsumerStub extends StartrekComplaintsOrdersConsumer {
    private final BlockingQueue<StartrekResult> accepted = new LinkedBlockingQueue<>();

    public StartrekComplaintsOrdersConsumerStub(
        FactConfig config,
        StartrekComplaintOrderParser parser,
        FactsServiceImpl factsService
    ) {
        super(config, parser, factsService);
    }

    @Override
    public void accept(StartrekResult startrekResult) {
        accepted.offer(startrekResult);
    }

    public BlockingQueue<StartrekResult> getAccepted() {
        return accepted;
    }
}

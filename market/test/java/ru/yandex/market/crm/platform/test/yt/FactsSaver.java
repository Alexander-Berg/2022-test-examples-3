package ru.yandex.market.crm.platform.test.yt;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.protobuf.Message;
import org.springframework.stereotype.Component;

import ru.yandex.market.crm.platform.common.FactContainer;
import ru.yandex.market.crm.platform.config.FactConfig;
import ru.yandex.market.crm.platform.services.facts.StorageService;
import ru.yandex.market.crm.platform.services.facts.impl.FactsWrapper;
import ru.yandex.market.crm.platform.test.utils.YtSchemaTestUtils;

/**
 * @author apershukov
 */
@Component
public class FactsSaver {

    private final StorageService storageService;
    private final FactsWrapper factsWrapper;
    private final YtSchemaTestUtils schemaTestUtils;

    public FactsSaver(StorageService storageService,
                      FactsWrapper factsWrapper,
                      YtSchemaTestUtils schemaTestUtils) {
        this.storageService = storageService;
        this.factsWrapper = factsWrapper;
        this.schemaTestUtils = schemaTestUtils;
    }

    public void save(FactConfig config, Message... facts) {
        schemaTestUtils.prepareFactTable(config);

        List<FactContainer> containers = Stream.of(facts)
                .map(fact -> factsWrapper.wrap(config, fact))
                .collect(Collectors.toList());

        storageService.add(config, containers).join();
    }
}

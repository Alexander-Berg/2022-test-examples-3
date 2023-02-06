package ru.yandex.market.crm.platform.reader.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.protobuf.Message;

import ru.yandex.market.crm.platform.config.FactConfig;
import ru.yandex.market.crm.platform.services.facts.FactsService;

public class FactsServiceStub implements FactsService {

    private final Map<String, List<Message>> facts = new HashMap<>();

    @Override
    public CompletableFuture<?> add(String config, List<? extends Message> facts) {
        this.facts.computeIfAbsent(config, k -> new ArrayList<>()).addAll(facts);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<?> add(FactConfig config, List<? extends Message> facts) {
        return add(config.getId(), facts);
    }

    @Override
    public void validate(FactConfig config, Message fact) {
        // empty
    }

    public void clear() {
        this.facts.clear();
    }

    public List<? extends Message> get(String config) {
        return facts.getOrDefault(config, Collections.emptyList());
    }
}

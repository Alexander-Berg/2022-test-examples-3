package ru.yandex.market.jmf.script.storage;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import ru.yandex.market.crm.util.CrmCollections;
import ru.yandex.market.jmf.script.Script;
import ru.yandex.market.jmf.script.ScriptType;
import ru.yandex.market.jmf.script.storage.impl.ScriptImpl;

public class InMemoryScriptStorageService implements ScriptStorageService {

    private final Map<String, Script> scripts;

    public static Builder builder() {
        return new Builder();
    }

    public InMemoryScriptStorageService(List<Script> scripts) {
        this.scripts = CrmCollections.index(scripts, Script::getCode);
    }

    @Override
    public Script getScriptOrError(String code) {
        return getScript(code)
                .orElseThrow(() -> new IllegalArgumentException("There is no script with code " + code));
    }

    @Override
    public Optional<Script> getScript(String code) {
        return Optional.ofNullable(scripts.get(code));
    }

    public static class Builder {

        private final List<Script> scripts = new ArrayList<>();

        public Builder withScript(String code, String body) {
            scripts.add(new ScriptImpl(code, Map.of(), body, OffsetDateTime.now(), ScriptType.DEFAULT, Set.of()));
            return this;
        }

        public Builder withModule(String code, String body) {
            scripts.add(new ScriptImpl(code, Map.of(), body, OffsetDateTime.now(), ScriptType.MODULE, Set.of()));
            return this;
        }

        public InMemoryScriptStorageService build() {
            return new InMemoryScriptStorageService(scripts);
        }

    }

}

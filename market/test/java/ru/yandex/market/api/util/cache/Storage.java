package ru.yandex.market.api.util.cache;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Эмулирует дисковое хранилише
 *
 * Created by apershukov on 10.01.17.
 */
class Storage extends FileMover implements BiConsumer<Path, String>, Function<Path, String> {
    Map<String, String> value = new HashMap<>();

    @Override
    public void accept(Path fileName, String value) {
        this.value.put(fileName.toString(), value);
    }

    @Override
    public String apply(Path fileName) {
        return value.get(fileName.toString());
    }

    @Override
    public void moveFile(Path from, Path to) {
        value.put(to.toString(), value.get(from.toString()));
    }
}

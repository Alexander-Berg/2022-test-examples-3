package ru.yandex.mbo.tool.jira.MBO14232;

import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.mbo.tool.dump.AutoCloseableIterator;
import ru.yandex.mbo.tool.dump.ModelDumpReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author ayratgdl
 * @date 02.02.18
 */
public class DumpReaderMock implements ModelDumpReader {

    private Map<Long, List<ModelStorage.Model>> data  = new HashMap<>();

    @Override
    public String getSessionId() {
        return "mock-session-id";
    }

    @Override
    public List<Long> readAllCategoryIds() {
        return new ArrayList<>(data.keySet());
    }

    @Override
    public boolean containsCategory(Long categoryId) {
        return data.containsKey(categoryId);
    }

    @Override
    public AutoCloseableIterator<ModelStorage.Model> getModelsIterator(Long categoryId) {
        return new AutoClosableWrapper<>(data.get(categoryId).iterator());
    }

    public DumpReaderMock addModel(Long categoryId, ModelStorage.Model model) {
        List<ModelStorage.Model> categoryModels = data.computeIfAbsent(categoryId, key -> new ArrayList<>());
        categoryModels.add(model);
        return this;
    }

    private static class AutoClosableWrapper<T> implements AutoCloseableIterator<T> {
        private Iterator<T> iterator;

        AutoClosableWrapper(Iterator<T> iterator) {
            this.iterator = iterator;
        }

        @Override
        public void close() throws Exception {
            // nothing
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public T next() {
            return iterator.next();
        }
    }
}

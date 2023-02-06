package ru.yandex.market.mbo.tms.model;

import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class YqlModelJdbcTemplateMock extends JdbcTemplate {
    List<Map<String, Object>> objects;
    private int batchSize = 5;
    private int lastIndex = 0;

    //Mock for queryForList
    @NotNull
    @Override
    public List<Map<String, Object>> queryForList(@NotNull String sql) throws DataAccessException {
        List<Map<String, Object>> result;
        try {
            int rightBoundary = Math.min(lastIndex + batchSize, objects.size());
            result = objects.subList(lastIndex, rightBoundary);
            lastIndex += batchSize;
        } catch (IndexOutOfBoundsException | IllegalArgumentException ex) {
            // If there is no more batches returns empty list
            return new ArrayList<>();
        }
        return result;
    }

    public void setLastIndex(int lastIndex) {
        this.lastIndex = lastIndex;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public List<Map<String, Object>> getObjects() {
        return objects;
    }

    public void setObjects(List<Map<String, Object>> objects) {
        this.objects = objects;
    }

}

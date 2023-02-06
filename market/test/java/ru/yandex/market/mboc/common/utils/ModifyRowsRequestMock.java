package ru.yandex.market.mboc.common.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mockito.Mockito;

import ru.yandex.yt.ytclient.proxy.ModifyRowsRequest;
import ru.yandex.yt.ytclient.tables.TableSchema;

/**
 * @author kravchenko-aa
 * @date 18.09.2020
 */
public class ModifyRowsRequestMock extends ModifyRowsRequest {
    private static TableSchema tableSchema;

    static {
        tableSchema = Mockito.mock(TableSchema.class);
        Mockito.when(tableSchema.isWriteSchema()).thenReturn(true);
    }

    private List<Map<String, ?>> insertion = new ArrayList<>();
    private List<Map<String, ?>> deletion = new ArrayList<>();
    private List<Map<String, ?>> updation = new ArrayList<>();

    public ModifyRowsRequestMock() {
        super("mock", tableSchema);
    }

    @Override
    public ModifyRowsRequest addInsert(Map<String, ?> map) {
        insertion.add(map);
        return this;
    }

    @Override
    public ModifyRowsRequest addUpdate(Map<String, ?> map) {
        updation.add(map);
        return this;
    }

    @Override
    public ModifyRowsRequest addDelete(Map<String, ?> map) {
        deletion.add(map);
        return this;
    }

    public List<Map<String, ?>> getInsertion() {
        return insertion;
    }

    public List<Map<String, ?>> getDeletion() {
        return deletion;
    }

    public List<Map<String, ?>> getUpdation() {
        return updation;
    }

    public boolean isEmpty() {
        return insertion.isEmpty() && deletion.isEmpty() && updation.isEmpty();
    }

    public void clear() {
        insertion.clear();
        updation.clear();
        deletion.clear();
    }
}

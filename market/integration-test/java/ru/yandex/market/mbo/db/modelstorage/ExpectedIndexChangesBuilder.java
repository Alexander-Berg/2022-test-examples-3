package ru.yandex.market.mbo.db.modelstorage;

import ru.yandex.ir.parser.matcher.tokenizers.StringValuesTokenizer;
import ru.yandex.market.mbo.db.modelstorage.yt.YtModelColumns;
import ru.yandex.yt.ytclient.proxy.ModifyRowsRequest;

import java.util.HashMap;
import java.util.Map;

/*
 * @author Nur-Magomed Dzhamiev <a href="mailto:n-mago@yandex-team.ru"></a>
 * @date 30.3.2020
 */
public class ExpectedIndexChangesBuilder {
    private ModifyRowsRequest request;

    enum RowType {
        DELETE, UPDATE
    }

    ExpectedIndexChangesBuilder(ModifyRowsRequest prototype) {
        this.request = new ModifyRowsRequest(prototype.getPath(), prototype.getSchema());
    }

    public ExpectedIndexChangesBuilder addExpectedBarcodeChanges(RowType rowType, long modelId, long categoryId,
                                                                 String... barcodes) {
        switch (rowType) {
            case UPDATE:
                for (String barcode : barcodes) {
                    request.addUpdate(makeBarcodeRowMap(barcode, modelId, categoryId));
                }
                break;
            case DELETE:
                for (String barcode : barcodes) {
                    request.addDelete(makeBarcodeRowMap(barcode, modelId, categoryId));
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
        return this;
    }

    public ExpectedIndexChangesBuilder addExpectedVendorCodeChanges(RowType rowType, long modelId, long vendorId,
                                                                    long categoryId, String... vendorCodes) {
        switch (rowType) {
            case UPDATE:
                for (String vendorCode : vendorCodes) {
                    request.addUpdate(makeVendorCodeRowMap(vendorCode, vendorId, categoryId, modelId));
                }
                break;
            case DELETE:
                for (String vendorCode : vendorCodes) {
                    request.addDelete(makeVendorCodeRowMap(vendorCode, vendorId, categoryId, modelId));
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
        return this;
    }

    public ExpectedIndexChangesBuilder addExpectedAliasChanges(RowType rowType, long modelId, long vendorId,
                                                                    long categoryId, long parentId,
                                                               String... aliases) {
        switch (rowType) {
            case UPDATE:
                for (String alias : aliases) {
                    request.addUpdate(makeAliasRowMap(alias, vendorId, categoryId, modelId, parentId));
                }
                break;
            case DELETE:
                for (String alias : aliases) {
                    request.addDelete(makeAliasRowMap(alias, vendorId, categoryId, modelId, parentId));
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
        return this;
    }

    public ModifyRowsRequest build() {
        return request;
    }

    private Map<String, Object> makeBarcodeRowMap(String barcode, long modelId, long categoryId) {
        Map<String, Object> map = new HashMap<>();
        map.put(YtModelColumns.BARCODE, barcode);
        map.put(YtModelColumns.MODEL_ID, modelId);
        map.put(YtModelColumns.CATEGORY_ID, categoryId);
        return map;
    }

    private Map<String, Object> makeVendorCodeRowMap(String vendorCode, long vendorId, long categoryId,
                                                     long modelId) {
        Map<String, Object> map = new HashMap<>();
        map.put(YtModelColumns.VENDOR_CODE, vendorCode);
        map.put(YtModelColumns.VENDOR_ID, vendorId);
        map.put(YtModelColumns.CATEGORY_ID, categoryId);
        map.put(YtModelColumns.MODEL_ID, modelId);
        return map;
    }

    private Map<String, Object> makeAliasRowMap(String alias, long vendorId, long categoryId,
                                                    long modelId, long parentId) {
        Map<String, Object> map = new HashMap<>();
        map.put(YtModelColumns.ALIAS_TOKEN, StringValuesTokenizer.tokenize(alias).toSearchValue());
        map.put(YtModelColumns.VENDOR_ID, vendorId);
        map.put(YtModelColumns.CATEGORY_ID, categoryId);
        map.put(YtModelColumns.MODEL_ID, modelId);
        map.put(YtModelColumns.PARENT_ID, parentId);
        return map;
    }
}

package ru.yandex.market.billing.tlogreport.marketplace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import one.util.streamex.StreamEx;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.core.OperatingUnit;
import ru.yandex.market.billing.model.billing.ProductId;
import ru.yandex.market.billing.model.tlog.Column;
import ru.yandex.market.billing.model.tlog.ColumnsForTransactionLogItem;
import ru.yandex.market.billing.model.tlog.TransactionLogServiceTypeParameters;
import ru.yandex.market.billing.tlog.config.TransactionLogConfig;
import ru.yandex.market.billing.tlogreport.marketplace.mappers.TransactionLogItemMapper;

import static ru.yandex.market.billing.model.billing.ProductId.FULFILLMENT_STORING_OLD;
import static ru.yandex.market.billing.model.billing.ProductId.GLOBAL_AGENCY_COMMISSION;
import static ru.yandex.market.billing.model.billing.ProductId.GLOBAL_DELIVERY;
import static ru.yandex.market.billing.model.billing.ProductId.GLOBAL_PLACEMENT;
import static ru.yandex.market.billing.model.billing.ProductId.OUTCOME_ACT_PRODUCT;
import static ru.yandex.market.billing.model.billing.ProductId.REWARD;

public class MarketplaceReportSheetsTest extends FunctionalTest {

    private static final Map<String, ProductId> PRODUCT_MAP = new HashMap<>();
    // НЕ МЕНЯТЬ БЕЗ СОГЛАСОВАНИЯ С @voznyuk-da
    private static final List<ProductId> PRODUCT_TO_INGORE_LIST = List.of(OUTCOME_ACT_PRODUCT, REWARD, GLOBAL_PLACEMENT,
            GLOBAL_DELIVERY, GLOBAL_AGENCY_COMMISSION, FULFILLMENT_STORING_OLD);
    private ObjectMapper mapper = new ObjectMapper();


    @Autowired
    private List<TransactionLogItemMapper> mappers;

    @BeforeEach
    void initProductMap() {
        for (ProductId pid : ProductId.values()) {
            if (PRODUCT_TO_INGORE_LIST.contains(pid)) {
                continue;
            }
            PRODUCT_MAP.put(pid.getCode(), pid);
        }
    }


    @Test
    @DisplayName("Проверка, что все ключи из записей тлога обрабатываются соответствующими мапперами")
    void checkKeysForBillingAndMarketplaceReport() throws JsonProcessingException {
        // Параметры конфига тлога
        List<TransactionLogServiceTypeParameters> parameters =
                new ArrayList<>(TransactionLogConfig.getTransactionLogConfig().values());
        //тип записи тлога входит в лист мапы, основанной на ProductId
        Map<ProductId, List<List<Column>>> keysMap = StreamEx.of(parameters)
                .filter(p -> Objects.equals(OperatingUnit.YANDEX_MARKET, p.getOperatingUnit()))
                // фильтруем пропущенные product_id чтобы не было нпе
                .filter(p -> !PRODUCT_TO_INGORE_LIST.stream().map(ProductId::getCode).collect(Collectors.toSet())
                        .contains(p.getColumnsMapping().get(ColumnsForTransactionLogItem.PRODUCT.getColumnName())
                                .getDefaultValue()))
                .toMap(
                        v -> PRODUCT_MAP.get(
                                v.getColumnsMapping()
                                        .get(ColumnsForTransactionLogItem.PRODUCT.getColumnName())
                                        .getDefaultValue()
                        ),
                        v -> List.of(v.getPrimaryKey()), (x, y) -> List.of(x.get(0), y.get(0))
                );
        // получаем карту для мапперов ключей листов отчета по коду ProductId
        Map<String, TransactionLogItemMapper> mapperMap = StreamEx.of(mappers)
                .toMap(i -> i.getMappedProduct().getCode(), i -> i);
        for (ProductId productId : keysMap.keySet()) {
            TransactionLogItemMapper currMapper = mapperMap.get(productId.getCode());
            Assertions.assertNotNull(currMapper, "Mapper with product " + productId + " from tlog config doesn't " +
                    "exists");
            Object keyDto = currMapper.getKeyDto();
            // список json-полей класса KeyDto
            Set<String> jsonKeys = new JSONObject(mapper.writeValueAsString(keyDto)).keySet();
            List<List<Column>> columns = keysMap.get(productId);
            List<String> columnStrings = new ArrayList<>();
            for (List<Column> columnList : columns) {
                columnStrings.addAll(columnList.stream().map(Column::getName).collect(Collectors.toList()));
            }
            Assertions.assertTrue(jsonKeys.containsAll(columnStrings), "Mapper with ProductId " + productId + " doesn" +
                    "'t contain column with the same ProductId");
        }
    }

    @Test
    @DisplayName("Проверка, что наличия defaultValue у колонки Product для всех типов записей в тлоге.")
    public void testTlogProductDefaultValue() {
        List<TransactionLogServiceTypeParameters> tlogTypes =
                new ArrayList<>(TransactionLogConfig.getTransactionLogConfig().values());
        tlogTypes.forEach(tlog -> {
            Assertions.assertNotNull(
                    tlog.getColumnsMapping()
                            .get(ColumnsForTransactionLogItem.PRODUCT.getColumnName())
                            .getDefaultValue(), "Tlog config for table " + tlog.getTableName() + " doesn't have " +
                            "default value for field Prouct.");
        });

    }
}

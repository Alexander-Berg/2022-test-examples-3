package ru.yandex.market.billing.tasks.shopdata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.tasks.shopdata.processor.ShopDataEnabledPostProcessor;
import ru.yandex.market.billing.tasks.shopdata.processor.ShopDataEnabledPostProcessorData;
import ru.yandex.market.billing.tasks.shopdata.processor.ShopDataTagShopProcessor;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.expimp.storage.export.exception.Trouble;
import ru.yandex.market.core.expimp.storage.export.processor.RowProcessorContext;
import ru.yandex.market.core.expimp.storage.export.processor.RowProcessorResult;
import ru.yandex.market.mbi.environment.EnvironmentService;

class ShopDataEnabledTest extends FunctionalTest {

    @Autowired
    private EnvironmentService environmentService;

    private ShopDataTagShopProcessor tagRowProcessor;
    private ShopDataEnabledPostProcessor shopPostProcessor;
    private ShopDataEnabledPostProcessor supplierPostProcessor;

    @BeforeEach
    void setUp() {
        tagRowProcessor = new ShopDataTagShopProcessor();
        shopPostProcessor = new ShopDataEnabledPostProcessor(environmentService, CampaignType.SHOP);
        supplierPostProcessor = new ShopDataEnabledPostProcessor(environmentService, CampaignType.SUPPLIER);
    }

    @Test
    void checkGoodSupplierData() {
        Assertions.assertNull(
                process(
                        List.of(
                                thirdParty(),
                                dropship(),
                                clickCollect()
                        ), supplierPostProcessor));
    }

    @Test
    void withoutThirdParty() {
        Assertions.assertEquals(
                process(
                        List.of(
                                disabled(thirdParty()),
                                dropship(),
                                clickCollect()
                        ), supplierPostProcessor).toString(),
                "В shop_data отсутствуют включенные магазины типа: 3P supplier");
    }

    @Test
    void withoutDropship() {
        Assertions.assertEquals(
                process(
                        List.of(
                                thirdParty(),
                                disabled(dropship()),
                                clickCollect()
                        ),
                        supplierPostProcessor).toString(),
                "В shop_data отсутствуют включенные магазины типа: Dropship");
    }

    @Test
    void withoutMultiply() {
        String result = process(
                List.of(
                        thirdParty(),
                        disabled(dropship()),
                        clickCollect()
                ),
                supplierPostProcessor).toString();

        Assertions.assertTrue(result.contains("В shop_data отсутствуют включенные магазины типа:"));
        Assertions.assertTrue(result.contains("Dropship"));
    }

    @Test
    void withoutClickCollect() {
        Assertions.assertEquals(
                process(
                        List.of(
                                thirdParty(),
                                dropship(),
                                disabled(clickCollect())
                        ),
                        supplierPostProcessor).toString(),
                "В shop_data отсутствуют включенные магазины типа: Click & Collect");
    }

    @Test
    void checkShop() {
        Assertions.assertNull(process(List.of(shop()), shopPostProcessor));
        Assertions.assertEquals(
                process(
                        List.of(disabled(shop())),
                        shopPostProcessor).toString(),
                "В shop_data отсутствуют включенные магазины");
    }

    private Trouble process(List<Map<String, Object>> rows, ShopDataEnabledPostProcessor postProcessor) {
        List<RowProcessorContext> contexts =
                rows.stream()
                        .map(entry -> new RowProcessorContext(null, entry))
                        .collect(Collectors.toList());

        List<ShopDataEnabledPostProcessorData> processed =
                contexts.stream()
                        .map(tagRowProcessor::process)
                        .map(RowProcessorResult::getPostProcessorData)
                        .map(data -> (ShopDataEnabledPostProcessorData) data)
                        .collect(Collectors.toList());

        return postProcessor.result(processed);
    }

    private Map<String, Object> thirdParty() {
        return Map.ofEntries(
                Map.entry("shop_id", "234"),
                Map.entry("supplier_type", "3"),
                Map.entry("direct_shipping", "true"),
                Map.entry("is_supplier", "true")
        );
    }

    private Map<String, Object> dropship() {
        return Map.ofEntries(
                Map.entry("shop_id", "567"),
                Map.entry("ff_program", "NO"),
                Map.entry("direct_shipping", "true"),
                Map.entry("is_supplier", "true")
        );
    }

    private Map<String, Object> clickCollect() {
        return Map.ofEntries(
                Map.entry("shop_id", "678"),
                Map.entry("ff_program", "NO"),
                Map.entry("direct_shipping", "true"),
                Map.entry("ignore_stocks", "true"),
                Map.entry("is_supplier", "true")
        );
    }

    private Map<String, Object> shop() {
        return Map.ofEntries(
                Map.entry("shop_id", "789")
        );
    }

    private Map<String, Object> disabled(Map<String, Object> row) {
        HashMap<String, Object> convertion = new HashMap<>(row);
        convertion.put("#shop_id", row.get("shop_id"));
        convertion.remove("shop_id");
        return convertion;
    }
}

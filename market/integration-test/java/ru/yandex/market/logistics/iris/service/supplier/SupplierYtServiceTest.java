package ru.yandex.market.logistics.iris.service.supplier;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.model.SupplierYtInfo;
import ru.yandex.market.request.trace.RequestContextHolder;

public class SupplierYtServiceTest extends AbstractContextualTest {

    @Autowired
    private SupplierYtService supplierYtService;
    private Function<JsonNode, SupplierYtInfo> mapper;
    private Consumer<List<SupplierYtInfo>> consumer;

    private ObjectMapper jsonMapper;

    @Before
    public void init() {
        jsonMapper = new ObjectMapper();
        RequestContextHolder.clearContext();

        mapper = supplierYtService.nodeToSupplierMapper();
        consumer = supplierYtService.supplierListConsumer();
    }

    @Test
    @DatabaseSetup("/fixtures/setup/sync/supplier/2.xml")
    @ExpectedDatabase(
            value = "/fixtures/expected/sync/supplier/2.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void syncYtSuppliersSuccess() throws Exception {
        String jsonString = extractFileContent("fixtures/setup/sync/supplier/yt_data.json");
        JsonNode jsonArray = jsonMapper.readTree(jsonString);
        List<JsonNode> rows = StreamSupport.stream(jsonArray.spliterator(), false)
                .collect(Collectors.toList());

        List<SupplierYtInfo> suppliers = rows.stream().map(e -> mapper.apply(e)).collect(Collectors.toList());
        consumer.accept(suppliers);
    }
}

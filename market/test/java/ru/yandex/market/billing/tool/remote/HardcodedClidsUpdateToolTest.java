package ru.yandex.market.billing.tool.remote;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.distribution.imports.HardcodedClidService;
import ru.yandex.market.billing.distribution.imports.dao.HardcodedClidDao;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.util.tool.ToolRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HardcodedClidsUpdateToolTest extends FunctionalTest {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private HardcodedClidService service;

    @BeforeEach
    void setup() {
        service = new HardcodedClidService(new HardcodedClidDao(namedParameterJdbcTemplate), transactionTemplate);
    }

    @Test
    @DbUnitDataSet(
            before = "db/HardcodedClidsUpdateTool.before.csv",
            after = "db/HardcodedClidsUpdateTool.fromParams.after.csv")
    public void testLoadFromParam() {
        HardcodedClidsUpdateTool tool = new HardcodedClidsUpdateTool(
                service, null);

        ToolRequest request = mock(ToolRequest.class);
        when(request.getParam(eq("clids"), any())).thenReturn("1:marketing,2:,7:closer");

        tool.doToolAction(request);
    }

    @Test
    @DbUnitDataSet(
            before = "db/HardcodedClidsUpdateTool.before.csv",
            after = "db/HardcodedClidsUpdateTool.fromResource.after.csv")
    public void testLoadFromCsv() {
        HardcodedClidsUpdateTool tool = new HardcodedClidsUpdateTool(
                service, "hardcoded_clids.csv");

        ToolRequest request = mock(ToolRequest.class);
        when(request.getParam(eq("clids"), any())).thenReturn(null);

        tool.doToolAction(request);
    }
}
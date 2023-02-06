package ru.yandex.market.pers.tms.imp;

import java.util.List;
import java.util.function.Consumer;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.pers.tms.cpc.CpcBusinessInfo;
import ru.yandex.market.pers.tms.yt.dumper.dumper.YtExportHelper;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;

public class CpcBusinessMappingTest extends MockedPersTmsTest {

    @Autowired
    private MbiYtImportProcessor processor;

    @Autowired
    private YtExportHelper ytExportHelper;

    @Test
    public void testImport() {
        mockYtCall(List.of(
            new CpcBusinessInfo(123L),
            new CpcBusinessInfo(124L),
            new CpcBusinessInfo(142L))
        );

        processor.updateCpcBusinessMapping();
        assertEquals(3, pgJdbcTemplate.queryForList("select * from grade.ext_mbi_cpc_mapping").size());

        mockYtCall(List.of(new CpcBusinessInfo(123L),
            new CpcBusinessInfo(124L),
            new CpcBusinessInfo(142L),
            new CpcBusinessInfo(149L)));
        processor.updateCpcBusinessMapping();
        assertEquals(4, pgJdbcTemplate.queryForList("select * from grade.ext_mbi_cpc_mapping").size());
    }

    private void mockYtCall(List<CpcBusinessInfo> source) {
        doAnswer(invocation -> {
            final Consumer<List<CpcBusinessInfo>> consumer = invocation.getArgument(3);
            consumer.accept(source);
            return null;
        }).when(ytExportHelper.getHahnYtClient()).consumeTableBatched(any(), anyInt(), any(), any());
    }
}

package ru.yandex.market.ff.repository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.bo.CRMExportSupplierInfo;
import ru.yandex.market.ff.model.bo.CRMExportSupplyInfo;
import ru.yandex.market.ff.repository.implementation.SupplierCrmExportRepositoryImpl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author otedikova
 */
class SupplierCrmExportRepositoryTest extends IntegrationTest {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private SupplierCrmExportRepositoryImpl customSupplierRepository;

    @Test
    @DatabaseSetup("classpath:repository/custom-supplier/crm_export.xml")
    void testGetSupplierCrmExport() {
        List<CRMExportSupplierInfo> exportInfo = new ArrayList<>();
        customSupplierRepository.provideSupplierExportIteratorTo(iterator -> {
            while (iterator.hasNext()) {
                exportInfo.add(iterator.next());
            }
            assertThat(exportInfo.size(), equalTo(2));
        });
        assertThat(exportInfo.size(), equalTo(2));
        Map<Long, CRMExportSupplierInfo> crmExportInfoMap = exportInfo.stream()
                .collect(Collectors.toMap(CRMExportSupplierInfo::getId, Function.identity()));
        CRMExportSupplierInfo exportInfo1 = crmExportInfoMap.get(1L);
        CRMExportSupplierInfo exportInfo2 = crmExportInfoMap.get(2L);
        assertThat(LocalDateTime.ofInstant(exportInfo1.getLastSupplyRequest().getCreatedDate(), ZoneId.systemDefault()),
                equalTo(LocalDateTime.parse("2018-05-08 12:13:01", DATE_TIME_FORMAT)));
        CRMExportSupplyInfo lastSupply = exportInfo1.getLastSupply();
        assertThat(LocalDateTime.ofInstant(lastSupply.getFinishedDate(), ZoneId.systemDefault()),
                equalTo(LocalDateTime.parse("2018-05-04 12:15:01", DATE_TIME_FORMAT)));
        assertThat(lastSupply.getItemsCount(), equalTo(5));
        assertThat(lastSupply.getSkuCount(), equalTo(2));
        assertThat(LocalDateTime.ofInstant(exportInfo2.getLastSupplyRequest().getCreatedDate(), ZoneId.systemDefault()),
                equalTo(LocalDateTime.parse("2018-05-02 22:13:01", DATE_TIME_FORMAT)));

    }
}

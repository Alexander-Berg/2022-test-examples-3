package ru.yandex.market.mbo.mdm.common.rsl;

import java.io.IOException;
import java.util.List;

import com.google.common.io.ByteStreams;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.rsl.CategoryRslRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.rsl.MskuRslRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.rsl.SskuRslRepository;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.services.category.CategoryCachingService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.MdmProperties;
import ru.yandex.market.mboc.common.utils.TaskQueueRegistratorMock;

/**
 * @author dmserebr
 * @date 28/11/2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public abstract class RslExcelTestBase extends MdmBaseDbTestClass {
    protected RslExcelExportService excelExportService;
    protected RslExcelImportService importExcelService;

    @Autowired
    protected CategoryRslRepository categoryRslRepository;

    @Autowired
    protected MskuRslRepository mskuRslRepository;

    @Autowired
    protected SskuRslRepository sskuRslRepository;

    @Autowired
    private TransactionHelper transactionHelper;

    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;

    protected CategoryCachingService categoryCachingService;
    protected StorageKeyValueServiceMock keyValueService;

    protected static void assertExcelFile(ExcelFile file, List<List<String>> sheet) {
        Assertions.assertThat(file.getLastLine()).isEqualTo(sheet.size());
        if (!sheet.isEmpty()) {
            Assertions.assertThat(file.getHeadersSize()).isEqualTo(sheet.get(0).size());
        }
        for (int i = 0; i < sheet.size(); ++i) {
            List<String> expectedLine = sheet.get(i);
            for (int j = 0; j < expectedLine.size(); ++j) {
                String expected = expectedLine.get(j);
                String actual = file.getLines().get(i + 1).get(j);
                if (!expected.isEmpty()) {
                    Assertions.assertThat(actual).isEqualTo(expected);
                } else {
                    Assertions.assertThat(actual).isNullOrEmpty();
                }
            }
        }
    }

    @Before
    public void before() {
        categoryCachingService = Mockito.mock(CategoryCachingService.class);
        keyValueService = new StorageKeyValueServiceMock();
        keyValueService.putValue(MdmProperties.RSL_MULTIDATES_ENABLED_KEY, true);

        excelExportService = new RslExcelExportService(categoryRslRepository, mskuRslRepository, sskuRslRepository,
            categoryCachingService, mappingsCacheRepository);

        importExcelService = new RslExcelImportService(
            categoryRslRepository,
            mskuRslRepository,
            sskuRslRepository,
            transactionHelper,
            new TaskQueueRegistratorMock(),
            keyValueService
        );
    }

    protected byte[] readResource(String fileName) throws IOException {
        return ByteStreams.toByteArray(getClass().getClassLoader().getResourceAsStream(fileName));
    }

    protected void prepareMapping(int categoryId, long modelId, int supplierId, String shopSku) {
        mappingsCacheRepository.insert(new MappingCacheDao()
            .setCategoryId(categoryId)
            .setMskuId(modelId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
        );
    }
}

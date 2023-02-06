package ru.yandex.market.mbo.mdm.tms.executors;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import ru.yandex.market.mbo.common.ZooKeeper.ZooKeeperService;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CustomsCommCodeRepository;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;

@Import(
    CustomsCommCodeImportExecutor.class
)
public class CustomsCommCodeImportExecutorTest extends MdmBaseDbTestClass {

    @MockBean
    private ZooKeeperService zooKeeperService;

    @Autowired
    private CustomsCommCodeRepository customsCommCodeRepository;

    @Autowired
    private CustomsCommCodeImportExecutor executor;

    @Test
    @Ignore("Долгий тест, нужен только для ручной проверки джобы")
    public void checkThatToolLoadDictionaryWithoutErrorsOnTestData() {
        var existedCodes = customsCommCodeRepository.findAllLite(true).stream().count();

        // when
        executor.execute();

        // then
        var loadedCodes = customsCommCodeRepository.findAllLite(true).stream().count();
        assertThat(loadedCodes).isEqualTo(21073);
        assertThat(existedCodes).isEqualTo(250);
    }
}

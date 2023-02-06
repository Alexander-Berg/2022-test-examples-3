package ru.yandex.market.mbisfintegration.importer;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.mbisfintegration.AbstractFunctionalTest;
import ru.yandex.market.mbisfintegration.converters.Converter;
import ru.yandex.market.mbisfintegration.dao.EntityService;
import ru.yandex.market.mbisfintegration.datapreparation.DataPreparationService;
import ru.yandex.market.mbisfintegration.generated.sf.model.SObject;
import ru.yandex.market.mbisfintegration.generated.sf.model.Soap;
import ru.yandex.market.mbisfintegration.importer.mbi.ImportEntityType;
import ru.yandex.market.mbisfintegration.salesforce.SoapHolder;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 23.03.2022
 */
public abstract class AbstractImportTest extends AbstractFunctionalTest {
    protected ImportConfiguration importConfig;
    protected Class<? extends SObject> entityClass;
    protected ImportEntityType entityType;
    protected Soap soap;
    protected Converter converter;
    @Autowired
    protected ResourceLoader resourceLoader;
    @Autowired
    protected EntityService entityService;
    @Autowired
    protected SoapHolder soapHolder;
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    protected void setUp() throws Exception {
        soap = soapHolder.getSoap();
    }

    @AfterEach
    protected void tearDown() {
        Mockito.clearInvocations(soap);
        jdbcTemplate.update("TRUNCATE TABLE queue CASCADE");
        jdbcTemplate.update("TRUNCATE TABLE entities CASCADE");
    }

    abstract public void doImport(DataPreparationService preparationService, String filePath);

    protected <T extends SObject> T findEntityData(Long id) {
        return (T) entityService.find(id, entityType, entityClass).getData();
    }

    protected <T extends SObject> List<T> findEntityDataList(Long id) {
        return (List<T>) entityService.find(id, entityType, entityClass, List.class).getData();
    }
}

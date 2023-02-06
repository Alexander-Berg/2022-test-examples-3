package ru.yandex.market.mbisfintegration.importer.yt;

import org.mockito.Mock;

import ru.yandex.market.mbisfintegration.dao.ConfigurationService;
import ru.yandex.market.mbisfintegration.datapreparation.DataPreparationService;
import ru.yandex.market.mbisfintegration.importer.AbstractImportTest;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 24.03.2022
 */
public class AbstractYtImportTest extends AbstractImportTest {

    protected YtImporter ytImporter;

    @Mock
    protected ConfigurationService configurationService;

    @Override
    public void doImport(DataPreparationService preparationService, String filePath) {

    }
}

package ru.yandex.market.mbisfintegration.importer.mbi;

import javax.xml.parsers.SAXParserFactory;

import lombok.SneakyThrows;

import ru.yandex.market.mbisfintegration.datapreparation.DataPreparationService;
import ru.yandex.market.mbisfintegration.importer.AbstractImportTest;

public abstract class AbstractMbiImportTest extends AbstractImportTest {

    @SneakyThrows
    @Override
    public void doImport(DataPreparationService preparationService, String filePath) {
        var context = new ImportExecutionContext(importConfig, preparationService, converter);
        var is = resourceLoader.getResource(filePath).getInputStream();
        SAXParserFactory.newInstance()
                .newSAXParser()
                .parse(is, new MbiImportSaxHandler(context));
    }

}

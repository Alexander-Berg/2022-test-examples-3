package ru.yandex.market.mbo.synchronizer.export;

import ru.yandex.market.mbo.core.kdepot.saver.XmlDataGenerator;

/**
 * @author ayratgdl
 * @date 29.03.18
 */
public abstract class XmlSingleFileExtractorTest extends ExtractorBaseTestClass {
    @Override
    protected final BaseExtractor createExtractor() {
        XmlSingleFileExtractor actualExtractor = new XmlSingleFileExtractor();
        actualExtractor.setEncoding("utf-8");
        actualExtractor.setOutputFileName("result.xml");
        actualExtractor.setXmlDataGenerator(createDataGenerator());
        ExtractorWriterService extractorWriterService = new ExtractorWriterService();
        actualExtractor.setExtractorWriterService(extractorWriterService);

        WithXsdValidator validator = new WithXsdValidator();
        validator.setSchemaPath(getSchemaPath());
        actualExtractor.setValidator(validator);

        return actualExtractor;
    }

    protected abstract XmlDataGenerator createDataGenerator();

    protected String getSchemaPath() {
        return null;
    }

    protected String getResultXml() {
        return new String(getExtractContent());
    }
}

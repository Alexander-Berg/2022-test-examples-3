package ru.yandex.market.common.test.db;

import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ReplacementDataSet;
import org.dbunit.dataset.csv.CsvDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.dataset.xml.XmlDataSet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import static ru.yandex.market.common.test.db.ddl.FlatXmlObjectReplacements.REPLACEMENTS;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public enum DataSetType {

    XML(new XmlDataSetProducer(), ".xml"),
    FLAT_XML(new FlatXmlDataSetProducer(), ".xml"),
    CSV(new CsvDataSetProducer(), ""),
    SINGLE_CSV(new SingleCsvDataSetProducer(), ".csv");

    private final DataSetProducer producer;
    private final String defaultSuffix;

    DataSetType(DataSetProducer producer, String defaultSuffix) {
        this.producer = producer;
        this.defaultSuffix = defaultSuffix;
    }

    public IDataSet createDataSet(Path path) throws DataSetException, IOException {
        return producer.createDataSet(path);
    }

    public IDataSet createDataSet(InputStream inputStream) throws DataSetException, IOException {
        return producer.createDataSet(inputStream);
    }

    public String getDefaultSuffix() {
        return defaultSuffix;
    }

    public static DataSetType parseFromDataSetName(String dataSetName) {
        if (dataSetName != null) {
            for (DataSetType dataSetType : values()) {
                if (!dataSetType.getDefaultSuffix().isEmpty() && dataSetName.endsWith(dataSetType.getDefaultSuffix())) {
                    return dataSetType;
                }
            }
        }
        throw new RuntimeException(String.format("Can not find DataSetType for dataSet %s", dataSetName));
    }

    private interface DataSetProducer {
        IDataSet createDataSet(Path dataSetPath) throws DataSetException, IOException;
        IDataSet createDataSet(InputStream inputStream) throws DataSetException, IOException;
    }

    private static class XmlDataSetProducer implements DataSetProducer {
        @Override
        public IDataSet createDataSet(Path dataSetPath) throws DataSetException, IOException {
            InputStream is = Files.newInputStream(dataSetPath);
            return new XmlDataSet(is);
        }

        @Override
        public IDataSet createDataSet(InputStream inputStream) throws DataSetException, IOException {
            if (inputStream != null) {
                return new XmlDataSet(inputStream);
            }
            return null;
        }
    }

    private static class FlatXmlDataSetProducer implements DataSetProducer {
        private final FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder()
                .setDtdMetadata(false)
                .setColumnSensing(true);

        @Override
        public IDataSet createDataSet(Path dataSetPath) throws DataSetException, IOException {
            InputStream is = Files.newInputStream(dataSetPath);
            return withReplacementsDataSet(is);
        }

        @Override
        public IDataSet createDataSet(InputStream inputStream) throws DataSetException, IOException {
            return withReplacementsDataSet(inputStream);
        }

        private IDataSet withReplacementsDataSet(InputStream inputStream) throws DataSetException, IOException {
            if (inputStream != null) {
                ReplacementDataSet dataSet = new ReplacementDataSet(builder.build(inputStream));
                REPLACEMENTS.forEach(dataSet::addReplacementObject);
                return dataSet;
            }
            return null;
        }
    }

    private static class CsvDataSetProducer implements DataSetProducer {
        @Override
        public IDataSet createDataSet(Path dataSetPath) throws DataSetException, IOException {
            File file = dataSetPath.toFile();
            if (!file.exists()) {
                file = dataSetPath.toAbsolutePath().toFile();
            }
            if (file.exists() && file.isDirectory()) {
                return new CsvDataSet(file);
            }
            return null;
        }

        @Override
        public IDataSet createDataSet(InputStream inputStream) throws DataSetException, IOException {
            throw new UnsupportedOperationException("path required");
        }
    }

    private static class SingleCsvDataSetProducer implements DataSetProducer {
        @Override
        public IDataSet createDataSet(Path path) throws DataSetException, IOException {
            InputStream is = Files.newInputStream(path);
            return new SingleFileCsvDataSet(is);
        }
        @Override
        public IDataSet createDataSet(InputStream inputStream) throws DataSetException, IOException {
            if (inputStream != null) {
                return new SingleFileCsvDataSet(inputStream);
            }
            return null;
        }
    }
}

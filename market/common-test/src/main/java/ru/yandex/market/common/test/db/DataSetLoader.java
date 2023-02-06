package ru.yandex.market.common.test.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.dbunit.dataset.CompositeDataSet;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassRelativeResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * @author jkt on 13.04.17.
 */
class DataSetLoader {

    private static final Logger log = LoggerFactory.getLogger(DataSetLoader.class);

    private final Class<?> testClass;

    DataSetLoader(Class<?> testClass) {
        this.testClass = testClass;
    }


    IDataSet getBeforeDataSets(List<DbUnitDataSet> dbUnitDataSets) {
        DataSetNameResolver nameResolver = DataSetNameResolver.beforeFor();
        List<IDataSet> dataSets = getDataSets(nameResolver, dbUnitDataSets);
        return compositeDataSet(dataSets);
    }

    @Nullable
    IDataSet getAfterDataSets(List<DbUnitDataSet> dbUnitDataSets) {
        DataSetNameResolver nameResolver = DataSetNameResolver.afterFor();
        List<IDataSet> dataSets = getDataSets(nameResolver, dbUnitDataSets);
        return dataSets.isEmpty() ? null : compositeDataSet(dataSets);
    }

    private IDataSet compositeDataSet(List<IDataSet> dataSets) {
        try {
            return new CompositeDataSet(dataSets.toArray(new IDataSet[0]));
        } catch (DataSetException e) {
            throw new RuntimeException("Error building composite data set", e);
        }
    }

    private List<IDataSet> getDataSets(DataSetNameResolver nameResolver, List<DbUnitDataSet> dataSets) {
        List<IDataSet> result = new ArrayList<>();
        for (DbUnitDataSet dataSet : dataSets) {
            List<String> classDataSetNames = extractDataSetNames(dataSet, nameResolver);
            result.addAll(loadDataSets(dataSet.type(), classDataSetNames));
        }
        return result;
    }

    private List<String> extractDataSetNames(DbUnitDataSet annotation, DataSetNameResolver nameResolver) {
        return Stream.of(annotation)
                .filter(Objects::nonNull)
                .map(nameResolver.getNamesMapper())
                .flatMap(Arrays::stream)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    List<IDataSet> loadDataSets(DataSetType dataSetType, List<String> locations) {
        List<IDataSet> dataSets = new ArrayList<>();
        for (String location : locations) {
            Optional<IDataSet> optionalDataSet = loadDataSet(dataSetType, location);
            if (optionalDataSet.isPresent()) {
                dataSets.add(optionalDataSet.get());
            } else {
                throw new IllegalArgumentException("Can not load dataset for file " + location);
            }
        }
        return dataSets;
    }

    private Optional<IDataSet> loadDataSet(DataSetType dataSetType, String location) {
        ResourceLoader resourceLoader = new ClassRelativeResourceLoader(testClass);
        try {
            Resource resource = resourceLoader.getResource(location);
            if (resource.exists()) {
                if (resource.isFile()) {
                    return loadDataSet(dataSetType, resource.getFile().toPath());
                } else {
                    return loadDataSet(dataSetType, resource.getInputStream());
                }
            }
        } catch (IOException e) {
            log.error(
                    "Failed to load resource {} by relative path. Trying to load resource by absolute path...",
                    location, e
            );
            try (InputStream is = testClass.getResourceAsStream(location)) {
                return Optional.of(dataSetType.createDataSet(is));
            } catch (DataSetException | IOException e2) {
                throw new RuntimeException("Fallback didn't help for resource: " + location, e2);
            }
        }
        return Optional.empty();
    }

    private static Optional<IDataSet> loadDataSet(DataSetType dataSetType, Path dataSetFile) throws IOException {
        try {
            log.info("Loading dataset {}", dataSetFile);
            return Optional.ofNullable(dataSetType.createDataSet(dataSetFile));
        } catch (DataSetException e) {
            throw new IOException(String.format("Error loading dataset file %s", dataSetFile), e);
        }
    }

    private static Optional<IDataSet> loadDataSet(DataSetType dataSetType, InputStream input) throws IOException {
        try {
            return Optional.ofNullable(dataSetType.createDataSet(input));
        } catch (DataSetException e) {
            throw new IOException(e);
        }
    }


    private static class DataSetNameResolver {
        private final Function<? super DbUnitDataSet, String[]> namesMapper;

        private DataSetNameResolver(Function<? super DbUnitDataSet, String[]> namesMapper) {
            this.namesMapper = namesMapper;
        }

        static DataSetNameResolver beforeFor() {
            return new DataSetNameResolver(DbUnitDataSet::before);
        }

        static DataSetNameResolver afterFor() {
            return new DataSetNameResolver(DbUnitDataSet::after);
        }

        private Function<? super DbUnitDataSet, String[]> getNamesMapper() {
            return namesMapper;
        }
    }
}

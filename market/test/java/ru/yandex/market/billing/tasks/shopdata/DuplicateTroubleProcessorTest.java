package ru.yandex.market.billing.tasks.shopdata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.expimp.storage.QueryToStorageExtractor;
import ru.yandex.market.core.expimp.storage.export.processor.RowProcessor;
import ru.yandex.market.core.expimp.storage.export.processor.common.FieldChangeProcessor;
import ru.yandex.market.core.expimp.storage.export.processor.common.NameCaseFormatProcessor;
import ru.yandex.market.core.expimp.storage.export.processor.common.NumberToBooleanProcessor;
import ru.yandex.market.core.expimp.storage.export.processor.common.UniquePostProcessor;
import ru.yandex.market.core.expimp.storage.export.processor.common.UniqueRowProcessor;
import ru.yandex.market.core.expimp.storage.export.worker.QueryToStorageWorkerImplFactory;
import ru.yandex.market.sqb.service.config.reader.ClasspathConfigurationReader;

import static com.google.common.base.CaseFormat.LOWER_UNDERSCORE;

@DbUnitDataSet(before = {
        "ShopDataWithoutDuplicates.csv",
})
class DuplicateTroubleProcessorTest extends FunctionalTest {
    private static final List<String> LOOKING_COLUMNS_UNIQUE_ROW_PROCESSOR = ImmutableList.of("datafeed_id");
    private static final Map<String, String> TEST_SHOP_SYMPTOM = ImmutableMap.of("shop_id", "774");
    private static final Map<String, Object> TEST_SHOP_REPLACEMENT = ImmutableMap.of("datasource_is_enabled", true);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void withoutDuplicates() {
        DuplicateTroubleProcessor duplicateTroubleProcessor = new DuplicateTroubleProcessor(
                new ClasspathConfigurationReader(getClass(),"shops_data_test.xml"),
                List.of(),
                queryToStorageExtractor(true),
                LOOKING_COLUMNS_UNIQUE_ROW_PROCESSOR

        );
        Assertions.assertEquals(
                "There are have been duplicates, but algorithm couldn't find it",
                duplicateTroubleProcessor.process()
        );
    }

    @Test
    @DbUnitDataSet(before = {
            "ShopDataWithDuplicates.csv",
    })
    public void withDuplicates() {
        DuplicateTroubleProcessor duplicateTroubleProcessor = new DuplicateTroubleProcessor(
                new ClasspathConfigurationReader(getClass(),"shops_data_test.xml"),
                List.of(),
                queryToStorageExtractor(true),
                LOOKING_COLUMNS_UNIQUE_ROW_PROCESSOR

        );
        Assertions.assertEquals("Duplicates in PREPAY_REQUIRES_VAT", duplicateTroubleProcessor.process());
    }

    @Test
    @DbUnitDataSet(before = {
            "ShopDataBrokenOrderOfRowProcessors.csv",
    })
    public void checkOrderOfRowProcessors() {
        DuplicateTroubleProcessor duplicateTroubleProcessor = new DuplicateTroubleProcessor(
                new ClasspathConfigurationReader(getClass(),"shops_data_test.xml"),
                List.of(),
                queryToStorageExtractor(false),
                LOOKING_COLUMNS_UNIQUE_ROW_PROCESSOR
        );
        Assertions.assertThrows(RuntimeException.class, duplicateTroubleProcessor::process);
    }

    private QueryToStorageWorkerImplFactory workerFactory(boolean isValid) {
        QueryToStorageWorkerImplFactory workerFactory = new QueryToStorageWorkerImplFactory(serializerFactory());
        workerFactory.setRowProcessors(getRowProcessors(isValid));
        workerFactory.setPostProcessors(List.of(new UniquePostProcessor()));
        return workerFactory;
    }

    private List<RowProcessor> getRowProcessors(boolean isValid) {
        List<RowProcessor> rowProcessors = new ArrayList<>();
        rowProcessors.add(nameCaseFormatProcessor());
        rowProcessors.add(new FieldChangeProcessor(TEST_SHOP_SYMPTOM, TEST_SHOP_REPLACEMENT));
        rowProcessors.add(uniqueRowProcessor());
        rowProcessors.add(isValid ? 0 : rowProcessors.size(), new NumberToBooleanProcessor());
        return rowProcessors;
    }

    private QueryToStorageExtractor queryToStorageExtractor(boolean isValid) {
        return new QueryToStorageExtractor(jdbcTemplate, workerFactory(isValid));
    }

    private ShopDataSerializer.ShopDataSerializerFactory serializerFactory() {
        return new ShopDataSerializer.ShopDataSerializerFactory();
    }

    private NameCaseFormatProcessor nameCaseFormatProcessor() {
        return new NameCaseFormatProcessor(LOWER_UNDERSCORE);
    }

    private UniqueRowProcessor uniqueRowProcessor() {
        return new UniqueRowProcessor(LOOKING_COLUMNS_UNIQUE_ROW_PROCESSOR);
    }
}

package ru.yandex.market.core.offer.mapping;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.feed.supplier.tanker.SupplierTankerService;
import ru.yandex.market.core.indexer.model.IndexerError;
import ru.yandex.market.core.offer.mapping.offerlist.Emergency;
import ru.yandex.market.core.offer.mapping.offerlist.LogMessage;
import ru.yandex.market.core.tanker.model.UserMessage;
import ru.yandex.market.mbi.util.MbiMatchers;

@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "MboMappingXlsHelperTest.before.csv")
class MboMappingXlsHelperTest extends FunctionalTest {

    @Autowired
    MboMappingXlsHelper mboMappingXlsHelper;

    @Autowired
    SupplierTankerService supplierTankerService;

    @Test
    void toIndexerErrorsTest() {
        Map<Integer, List<LogMessage>> errors = new HashMap<>();
        errors.put(0, Collections.singletonList(LogMessage.of(Emergency.WARNING, new UserMessage.Builder()
                .setDefaultTranslation("Error0 {{error}}")
                .setMessageCode("34d")
                .setMustacheArguments(/*language=json*/ "{\"error\": \"xyx\"}")
                .build())));
        errors.put(1, Collections.singletonList(LogMessage.of(Emergency.OK, new UserMessage.Builder()
                .setDefaultTranslation("Error1 {{offerId}}")
                .setMessageCode("34f")
                .setMustacheArguments(/*language=json*/ "{\"offerId\": \"xyx\"}")
                .build())));
        errors.put(2,
                Arrays.asList(
                        LogMessage.of(
                                Emergency.ERROR,
                                new UserMessage.Builder().setDefaultTranslation("Error21").setMessageCode("34f").build()
                        ),
                        LogMessage.of(
                                Emergency.ERROR,
                                new UserMessage.Builder().setDefaultTranslation("Error22").setMessageCode("34h").build()
                        )
                ));

        List<IndexerError> indexerErrors = MboMappingXlsHelper.toIndexerErrors(errors);

        MatcherAssert.assertThat(indexerErrors, Matchers.contains(Arrays.asList(
                MbiMatchers.<IndexerError>newAllOfBuilder()
                        .add(IndexerError::getPosition, "2")
                        .add(IndexerError::getCode, "34d")
                        .add(IndexerError::getText, "Error0 {{error}}")
                        .add(IndexerError::getDetails, /*language=json*/ "{\"error\": \"xyx\"}")
                        .build(),
                MbiMatchers.<IndexerError>newAllOfBuilder()
                        .add(IndexerError::getPosition, "4")
                        .add(IndexerError::getCode, "34f")
                        .add(IndexerError::getText, "Error21")
                        .add(IndexerError::getDetails, /*language=json*/ "{}")
                        .build(),
                MbiMatchers.<IndexerError>newAllOfBuilder()
                        .add(IndexerError::getPosition, "4")
                        .add(IndexerError::getCode, "34h")
                        .add(IndexerError::getText, "Error22")
                        .add(IndexerError::getDetails, /*language=json*/ "{}")
                        .build()
        )));
    }
}

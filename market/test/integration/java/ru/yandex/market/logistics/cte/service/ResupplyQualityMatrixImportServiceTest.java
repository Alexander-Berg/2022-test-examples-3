package ru.yandex.market.logistics.cte.service;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.logistics.cte.base.IntegrationTest;
import ru.yandex.market.logistics.cte.converters.SupplyDtoToSupplyConverter;
import ru.yandex.market.logistics.cte.converters.SupplyItemDtoToSupplyItemConverter;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@ContextConfiguration(classes = {SupplyDtoToSupplyConverter.class, SupplyItemDtoToSupplyItemConverter.class})
class ResupplyQualityMatrixImportServiceTest extends IntegrationTest {

    @Autowired
    ResupplyQualityMatrixImportService resupplyQualityMatrixImportService;

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:repository/qattribute.xml"),
            @DatabaseSetup("classpath:service/before.xml")
    })
    @ExpectedDatabase(
            table = "result",
            query = "select cc.id, cc.name as category_name, " +
                    "qa.name as quality_attribute, qmai.util as is_util\n" +
                    "from quality_matrix_attr_inclusion qmai\n" +
                    "left join quality_matrix qm on qmai.matrix_id = qm.id\n" +
                    "left join category_canonical cc on qm.category_id = cc.id " +
                    "left join qattribute qa on qmai.qattribute_id = qa.id",
            value = "classpath:service/canonical_category_join_quality_matrix_after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void updateQualityMatrices_happyPath() throws URISyntaxException {
        URL input = this.getClass().getClassLoader().getResource("test-resupply-quality-attributes.csv");

        resupplyQualityMatrixImportService.updateQualityMatrices(new File(Objects.requireNonNull(input).toURI()));
    }
}

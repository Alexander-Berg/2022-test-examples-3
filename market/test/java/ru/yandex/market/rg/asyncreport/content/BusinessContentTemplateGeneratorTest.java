package ru.yandex.market.rg.asyncreport.content;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersRequest;

/**
 * Тесты для {@link BusinessContentTemplateGenerator}
 */
public class BusinessContentTemplateGeneratorTest extends AbstractContentTemplateGeneratorTest {

    @Autowired
    private BusinessContentTemplateGenerator businessContentTemplateGenerator;

    @Test
    @DisplayName("Успешная генерация отчета")
    @DbUnitDataSet(before = "PartnerContentTemplateGeneratorTest/csv/testSuccess.before.csv")
    void testSuccess() {
        SearchBusinessOffersRequest actualRequest = testSuccessAndGetRequest(businessContentTemplateGenerator, 2001L);

        Assertions.assertThat(actualRequest.getPartnerId())
                .isNull();
        Assertions.assertThat(actualRequest.getBusinessId())
                .isNotNull();
    }

    @Test
    @DisplayName("Фейл во время генерации отчета. МБО вернул ошибку")
    @DbUnitDataSet(before = "PartnerContentTemplateGeneratorTest/csv/testSuccess.before.csv")
    void testMboFail() {
        testMboFail(businessContentTemplateGenerator, 2001L);
    }
}

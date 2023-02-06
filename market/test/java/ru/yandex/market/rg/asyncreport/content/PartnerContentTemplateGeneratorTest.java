package ru.yandex.market.rg.asyncreport.content;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersRequest;

/**
 * Тесты для {@link PartnerContentTemplateGenerator}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class PartnerContentTemplateGeneratorTest extends AbstractContentTemplateGeneratorTest {

    @Autowired
    private PartnerContentTemplateGenerator partnerContentTemplateGenerator;

    @Test
    @DisplayName("Успешная генерация отчета")
    @DbUnitDataSet(before = "PartnerContentTemplateGeneratorTest/csv/testSuccess.before.csv")
    void testSuccess() {
        SearchBusinessOffersRequest actualRequest = testSuccessAndGetRequest(partnerContentTemplateGenerator, 1001L);

        Assertions.assertThat(actualRequest.getPartnerId())
                .isNotNull();
        Assertions.assertThat(actualRequest.getBusinessId())
                .isNotNull();
    }

    @Test
    @DisplayName("Фейл во время генерации отчета. МБО вернул ошибку")
    @DbUnitDataSet(before = "PartnerContentTemplateGeneratorTest/csv/testSuccess.before.csv")
    void testMboFail() {
        testMboFail(partnerContentTemplateGenerator, 1001L);
    }
}

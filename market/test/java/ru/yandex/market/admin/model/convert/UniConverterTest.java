package ru.yandex.market.admin.model.convert;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.admin.ui.model.feed.UIFeedSiteType;
import ru.yandex.market.core.feed.model.FeedSiteType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Тест проверяет успешность конвертации моделей, из core типов в UI, определенных в
 * конфигурации {@code classpath:admin/admin-models.xml}.
 * <p>
 * Это необходимо для корректной передачи данных между бекендом и фронтом админки.
 */
class UniConverterTest extends FunctionalTest {

    @Autowired
    private UniConverter uniConverter;

    /**
     * Тест проверяет, что все элементы энума {@link FeedSiteType} успешно конвертируются в
     * UI тип {@link UIFeedSiteType}.
     */
    @Test
    void testFeedSiteTypeConversionToUI() {
        for (FeedSiteType coreType : FeedSiteType.values()) {
            UIFeedSiteType uiType = uniConverter.fromCoreToUI(coreType);

            assertThat("Expected value: " + coreType, uiType, notNullValue());
            assertThat(uiType.getStringId(), equalTo(coreType.name()));
        }
    }

}

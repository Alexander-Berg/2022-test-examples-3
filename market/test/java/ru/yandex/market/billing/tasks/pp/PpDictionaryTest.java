package ru.yandex.market.billing.tasks.pp;

import java.util.Arrays;

import org.junit.Test;

import ru.yandex.market.core.billing.pp.PpDictionary;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

/**
 * @author vbudnev
 */
public class PpDictionaryTest {

    /**
     * Тест уведомление о том, что расширение {@link PpDictionary} должно быть явно обработано в задачах иморта
     * в {@link PpDictImportService}.
     */
    @Test
    public void test_extensionMustBeExplicitlyHandled() {
        assertThat(
                Arrays.asList(PpDictionary.values()),
                containsInAnyOrder(
                        PpDictionary.PP,
                        PpDictionary.STATISTICS,
                        PpDictionary.PLACEMENT,
                        PpDictionary.PLATFORMS,
                        PpDictionary.UNIVERSAL_REPORTS,
                        PpDictionary.MARKET_AND_PARTNERS,
                        PpDictionary.PP_BLUE
                )
        );
    }

}

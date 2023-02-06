package ru.yandex.market.mboc.common.utils;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mboc.common.randomizers.LocalizedStringRandomizer;
import ru.yandex.market.mboc.common.services.modelstorage.models.LocalizedString;

/**
 * @author s-ermakov
 */
public class LocalizedStringConverterTest {
    private static final long SEED = "MBO-15933".hashCode();
    private LocalizedStringRandomizer localizedStringRandomizer;

    @Before
    public void setUp() throws Exception {
        localizedStringRandomizer = new LocalizedStringRandomizer(SEED);
    }

    @Test
    public void testDoubleConvention() {
        for (int i = 0; i < 100; i++) {
            LocalizedString localizedString = localizedStringRandomizer.getRandomValue();

            ModelStorage.LocalizedString proto = LocalizedStringUtils.reverseConvert(localizedString);
            LocalizedString converted = LocalizedStringUtils.convert(proto);

            Assertions.assertThat(converted).isEqualToComparingFieldByField(localizedString);
        }
    }
}

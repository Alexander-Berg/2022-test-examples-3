package ru.yandex.direct.core.entity.adgroup.service.complex.text.update.keyword;

import ru.yandex.direct.core.entity.adgroup.service.complex.text.update.ComplexAdGroupUpdateOperationTestBase;
import ru.yandex.direct.core.entity.keyword.model.Keyword;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultClientKeyword;

public class ComplexUpdateKeywordTestBase extends ComplexAdGroupUpdateOperationTestBase {

    protected Keyword randomKeyword() {
        return randomKeyword(null);
    }

    protected Keyword randomKeyword(Long id) {
        return defaultClientKeyword()
                .withId(id)
                .withPhrase(randomAlphabetic(10));
    }
}

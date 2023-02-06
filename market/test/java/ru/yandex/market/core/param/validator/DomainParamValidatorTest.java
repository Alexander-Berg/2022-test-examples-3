package ru.yandex.market.core.param.validator;

import java.util.HashMap;
import java.util.Map;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;

import ru.yandex.market.core.language.model.Language;
import ru.yandex.market.core.tanker.dao.TankerDao;
import ru.yandex.market.core.tanker.model.MessageSet;
import ru.yandex.market.core.tanker.model.TankerKeySets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DomainParamValidatorTest {
    @Mock
    private TankerDao tankerDao;

    private DomainParamValidator domainParamValidator;

    @BeforeEach
    void setUp() {
        domainParamValidator = new DomainParamValidator(tankerDao);

        Map<String, String> hashMap = new HashMap<>();
        hashMap.put(TankerKeySets.FORBIDDEN_DOMAIN_LIST, "vk.com");
        when(tankerDao.getMessageSet(TankerKeySets.FORBIDDEN_DOMAIN_LIST, Language.RUSSIAN))
                .thenReturn(new MessageSet(Language.RUSSIAN, hashMap));
    }

    /**
     * Тест проверяет, что проверка не пропускает популярные домены.
     * Домены подкачиваются из танкера.
     */
    @ParameterizedTest
    @ValueSource(strings = {"http://vk.com", "https://vk.com", "https://www.vk.com", "www.vk.com", "vk.com",
            "https://vk.com/ "})
    void testCheckPopularDomains(String url) {
        assertFalse(domainParamValidator.isValid(url));
        assertFalse(domainParamValidator.isValid(url, true));
        assertFalse(domainParamValidator.isValid(url, false));
    }

    /**
     * Проверяет корректную валидацию доменов не из черного списка.
     */
    @ParameterizedTest
    @ValueSource(strings = {"http://avk.com", "https://avk.com", "https://www.avk.com", "www.avk.com", "avk.com",
            "http://avk.com/ "})
    void testCheckValidDomains(String url) {
        assertTrue(domainParamValidator.isValid(url));
        assertTrue(domainParamValidator.isValid(url, false));
        assertTrue(domainParamValidator.isValid(url, true));
    }

    /**
     * Проверяет корректную валидацию произвольных ссылок, в том числе на популярные сайты.
     */
    @ParameterizedTest
    @ValueSource(strings = {"http://avk.com:123/parashop", "https://avk.com/parashop?q=1",
            "https://www.instagram.com/parashop"})
    void testCheckValidUrls(String url) {
        assertTrue(domainParamValidator.isValid(url));
        assertTrue(domainParamValidator.isValid(url, true));
        assertFalse(domainParamValidator.isValid(url, false));
    }

    /**
     * Проверяет, что произвольный текст не проходит валидацию.
     */
    @ParameterizedTest
    @ValueSource(strings = {"www.vk.com/smb", "avk.com/pupkin?name=vasily", // это не урлы, отсутствует протокол http
            "text", "https://www.instagram.com/ space"})
    void testCheckInvalidUrls(String url) {
        assertFalse(domainParamValidator.isValid(url));
        assertFalse(domainParamValidator.isValid(url, false));
        assertFalse(domainParamValidator.isValid(url, true));
    }
}

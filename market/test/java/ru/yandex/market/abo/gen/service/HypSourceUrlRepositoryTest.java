package ru.yandex.market.abo.gen.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.gen.manual.HypSourceUrl;
import ru.yandex.market.abo.gen.manual.HypSourceUrlStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author agavrikov
 * @date 30.04.18
 */
public class HypSourceUrlRepositoryTest extends EmptyTest {

    @Autowired
    HypSourceUrlRepository hypSourceUrlRepository;

    @Test
    public void testRepo() {
        HypSourceUrl hypSourceUrl = initHypSourceUrl();
        hypSourceUrlRepository.save(hypSourceUrl);
        HypSourceUrl dbHypSourceUrl = hypSourceUrlRepository.findByIdOrNull(hypSourceUrl.getId());
        assertEquals(hypSourceUrl, dbHypSourceUrl);
    }

    @Test
    public void testFindAllByStatus() {
        HypSourceUrl hypSourceUrl = initHypSourceUrl();
        hypSourceUrlRepository.save(hypSourceUrl);
        assertEquals(1,
                hypSourceUrlRepository.findAllByStatus(HypSourceUrlStatus.OPEN.getId()).size()
        );
        assertEquals(hypSourceUrl,
                hypSourceUrlRepository.findAllByStatus(HypSourceUrlStatus.OPEN.getId()).get(0)
        );
    }

    private HypSourceUrl initHypSourceUrl() {
        String url = "http://market.yandex.ru/offers.xml?hyperid=119641&hid=90575&modelid=119641&grhow=shop";
        String userComment = "проверка наиболее популярных водонагревателей";
        String[] wareMD5Array = {"guisB5mT9ZfLSDo9XqsZyQ", "YCRJSiS04YJrKO6uHIDjeA", "CQmjmr54CBba-6ay7AptfQ"};
        return new HypSourceUrl(1L, url, userComment, false, false, wareMD5Array);
    }
}

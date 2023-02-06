package ru.yandex.market.fulfillment.wrap.marschroute.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.Multimap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.fulfillment.wrap.core.api.RequestType;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.RepositoryTest;
import ru.yandex.market.logistic.api.model.common.request.Token;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllowedTokenRepositoryTest extends RepositoryTest {
    @Autowired
    private AllowedTokenRepository allowedTokenRepository;

    @Test
    @DatabaseSetup("classpath:repository/allowed_token_setup.xml")
    void mapFromRepositoryIsValid() {
        Multimap<Token, RequestType> allowedTokenMultimap = allowedTokenRepository.getAllowedTokenMultimap();
        assertEquals(2, allowedTokenMultimap.asMap().size());
        assertEquals(2,
            allowedTokenMultimap.get(
                new Token("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")).size());
        assertTrue(allowedTokenMultimap.get(
            new Token("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"))
            .containsAll(Arrays.asList(RequestType.GET_ORDER, RequestType.GET_STOCKS)));


        assertEquals(1,
            allowedTokenMultimap.get(
                new Token("yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy")).size());
        assertTrue(allowedTokenMultimap.get(
            new Token("yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy"))
            .contains(RequestType.GET_STOCKS));

    }

}

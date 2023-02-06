package ru.yandex.market.wrap.infor.repository;

import java.util.Arrays;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.Multimap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.wrap.core.api.RequestType;
import ru.yandex.market.logistic.api.model.common.request.Token;
import ru.yandex.market.wrap.infor.configuration.AbstractContextualTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllowedTokenRepositoryTest extends AbstractContextualTest {
    @Autowired
    private AllowedTokenRepository allowedTokenRepository;

    @Test
    @DatabaseSetup(connection = "wrapConnection",
        value = "classpath:fixtures/integration/repository/allowed_token_setup.xml")
    void mapFromRepositoryIsValid() {
        Multimap<Token, RequestType> allowedTokenMultimap = allowedTokenRepository.getAllowedTokenMultimap();
        assertEquals(2, allowedTokenMultimap.asMap().size());
        Token tokenX = new Token("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        Token tokenY = new Token("yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy");

        assertEquals(2, allowedTokenMultimap.get(tokenX).size());
        assertTrue(allowedTokenMultimap.get(tokenX)
            .containsAll(Arrays.asList(RequestType.GET_ORDER, RequestType.GET_STOCKS)));


        assertEquals(1, allowedTokenMultimap.get(tokenY).size());
        assertTrue(allowedTokenMultimap.get(tokenY).contains(RequestType.GET_STOCKS));
    }
}

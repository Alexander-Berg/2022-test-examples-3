package ru.yandex.market.loyalty.core.model.coin;

import org.hamcrest.Matchers;
import org.junit.Test;

import ru.yandex.market.loyalty.api.model.coin.CoinType;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
public class CoreCoinTypeTest {

    @Test
    public void shouldAllApiCoinTypeValuesMapped() {
        assertThat(
                Arrays.stream(CoreCoinType.values())
                        .map(CoreCoinType::getApiCoinType)
                        .collect(Collectors.toList()),
                containsInAnyOrder(
                        Arrays.stream(CoinType.values())
                                .filter(t -> t != CoinType.UNKNOWN)
                                .map(Matchers::equalTo)
                                .collect(Collectors.toList())
                )
        );
    }
}

package ru.yandex.market.core.eats_and_lavka;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.mbi.yt.YtTemplate;
import ru.yandex.market.mbi.yt.YtUtil;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;

/**
 * Тесты для {@link  EatsAndLavkaPartnersYtDao}
 */
@Disabled("Проверка интеграции с YT - чтения. dev отладка")
public class EatsAndLavkaPartnersYtDaoIntegrationTest {
    private EatsAndLavkaPartnersYtDao eatsAndLavkaPartnersYtDao;

    @BeforeEach
    void setUp() {
        eatsAndLavkaPartnersYtDao = new EatsAndLavkaPartnersYtDao(
                new YtTemplate(YtUtil.buildYtClusters(
                        List.of("hahn.yt.yandex.net", "arnold.yt.yandex.net"),
                        getToken())),
                "//home/taxi/testing/replica/postgres/eats_retail_market_integration/places_info");
    }

    @DisplayName("Проверяем, что запрос выполняется корректно")
    @Test
    void testImport() {
        var result = eatsAndLavkaPartnersYtDao.getFromYt();
        Map<String, Set<?>> allData = new HashMap<>();
        for (Method method : EatsAndLavkaPartnerYtInfo.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers()) || method.getParameters().length != 0) {
                continue;
            }
            Set<?> columnData = result.stream().map(p -> {
                try {
                    return method.invoke(p);
                } catch (Exception e) {
                    System.out.println(method);
                    throw new RuntimeException(e);
                }
            }).filter(Objects::nonNull).collect(Collectors.toSet());
            // хотя бы одна колонка содержит данные
            MatcherAssert.assertThat(method.getName(), columnData.size(), greaterThan(0));
            // разные данные в разных колонках
            for (var entity : allData.entrySet()) {
                // тут пока одинаковые данные с getUpdatedAt
                if (method.getName().equals("getCreatedAt") || method.getName().equals("getUpdatedAt")) {
                    continue;
                }
                MatcherAssert.assertThat(method.getName() + " equals " + entity.getKey(),
                        entity.getValue(), not(equalTo(columnData)));
            }
            allData.put(method.getName(), columnData);
        }
    }


    /**
     * Для запуска теста надо положить продовый токен (не забыть стереть, чтоб не закомитить)
     */
    private String getToken() {
        return "${mbi.robot.yt.token}";
    }
}

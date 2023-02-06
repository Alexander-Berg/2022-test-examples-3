package ru.yandex.market.wrap.infor.functional;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.common.ping.CheckResult;
import ru.yandex.market.wrap.infor.entity.MonrunLevel;
import ru.yandex.market.wrap.infor.util.CheckResultUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CheckUnknownSkusTest extends AbstractFunctionalTest {

    /**
     * Сценарий #1: Проверяем, что будет возвращен ОК на пустой БД.
     */
    @Test
    void okOnEmptyDatabase() throws Exception {
        assertChecking(MonrunLevel.OK);
    }

    /**
     * Сценарий #2: Проверяем, что будет возвращен ОК на БД,
     * которая содержит только зарезолвленные SKU.
     */
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/functional/check_unknown_skus/2/state.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @Test
    void okOnDatabaseThatContainsOnlyResolvedSkus() throws Exception {
        assertChecking(MonrunLevel.OK);
    }

    /**
     * Сценарий #3: Проверяем, что будет возвращен WARN на БД,
     * которая содержит максимальный статус - WARN.
     */
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/functional/check_unknown_skus/3/state.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @Test
    void warnOnDatabaseThatContainsMaxWarnStatus() throws Exception {
        assertChecking(MonrunLevel.WARN);
    }

    /**
     * Сценарий #4: Проверяем, что будет возвращен CRIT на БД,
     * которая содержит максимальный статус - CRIT.
     */
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/functional/check_unknown_skus/4/state.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @Test
    void critOnDatabaseThatContainsMaxCritStatus() throws Exception {
        assertChecking(MonrunLevel.CRIT);
    }


    private void assertChecking(MonrunLevel expectedLevel) throws Exception {
        mockMvc.perform(get("/health/checkUnknownSkus")
            .contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(content().string(CheckResultUtils.toMonrunString(
                new CheckResult(expectedLevel.toCheckResultLevel(), "")))
            );
    }
}

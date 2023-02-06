package ru.yandex.market.adv.shop.integration.api.metrika.AddGrantsBusinessMetrika;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.adv.shop.integration.AbstractShopIntegrationTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Тесты на endpoint POST /businesses/metrika/grants/add.")
public class AddGrantsBusinessMetrikaTest extends AbstractShopIntegrationTest {

    private static final String LOGIN = "login4";

    @Autowired
    private MockMvc mvc;

    @DisplayName("Добавили логин в список дополнительных логинов, на которые выдается доступ.")
    @DbUnitDataSet(
            before = {
                    "csv/addGrantsBusinessMetrika_exist_success.before.csv",
                    "csv/addGrantsBusinessMetrika_Updater_empty.before.csv"
            },
            after = {
                    "csv/addGrantsBusinessMetrika_exist_success.after.csv",
                    "csv/addGrantsBusinessMetrika_Updater_successTwoRows.after.csv"
            }
    )
    @Test
    void addGrantsBusinessMetrika_exist_success() throws Exception {

        List<Long> counterList = List.of(222L, 333L);
        check(LOGIN, counterList, status().isNoContent());
    }


    @DisplayName("Добавили логин в список дополнительных логинов с учетом дубликатов счетчиков в запросе.")
    @DbUnitDataSet(
            before = {
                    "csv/addGrantsBusinessMetrika_exist_success.before.csv",
                    "csv/addGrantsBusinessMetrika_Updater_empty.before.csv"
            },
            after = {
                    "csv/addGrantsBusinessMetrika_exist_success.after.csv",
                    "csv/addGrantsBusinessMetrika_Updater_successTwoRows.after.csv"
            }
    )
    @Test
    void addGrantsBusinessMetrika_withDuplicateCounters_success() throws Exception {

        List<Long> counterList = List.of(222L, 222L, 222L, 333L);
        check(LOGIN, counterList, status().isNoContent());
    }


    @DisplayName("Добавили логин в список дополнительных логинов с учетом дубликатов логинов.")
    @DbUnitDataSet(
            before = {
                    "csv/addGrantsBusinessMetrika_exist_success.before.csv",
                    "csv/addGrantsBusinessMetrika_Updater_empty.before.csv"
            },
            after = {
                    "csv/addGrantsBusinessMetrika_exist_success.after.csv",
                    "csv/addGrantsBusinessMetrika_Updater_successThreeRows.after.csv"
            }
    )
    @Test
    void addGrantsBusinessMetrika_withDuplicateLogins_success() throws Exception {

        List<Long> counterList = List.of(222L, 333L, 444L);
        check(LOGIN, counterList, status().isNoContent());
    }


    @DisplayName("Добавили логин в список дополнительных логинов при пустом поле additionalLogins.")
    @DbUnitDataSet(
            before = {
                    "csv/addGrantsBusinessMetrika_additionalLoginsEmpty_success.before.csv",
                    "csv/addGrantsBusinessMetrika_Updater_empty.before.csv"
            },
            after = {
                    "csv/addGrantsBusinessMetrika_additionalLoginsEmpty_success.after.csv",
                    "csv/addGrantsBusinessMetrika_Updater_successOneRow.after.csv"
            }
    )
    @Test
    void addGrantsBusinessMetrika_additionalLoginsEmpty_success() throws Exception {

        List<Long> counterList = List.of(222L);
        check(LOGIN, counterList, status().isNoContent());
    }


    @DisplayName("Логин длины 0 в запросе на добавление логина. Ошибка 400.")
    @Test
    void addGrantsBusinessMetrika_loginZeroLength_badRequest() throws Exception {

        check("", List.of(222L, 333L, 444L), status().isBadRequest());
    }


    @DisplayName("Пустой список счетчиков в запросе на добавление логина. Ошибка 400.")
    @Test
    void addGrantsBusinessMetrika_emptyCounterIds_badRequest() throws Exception {

        check(LOGIN, List.of(), status().isBadRequest());
    }


    private void check(String login, List<Long> counterIds, ResultMatcher resultMatcher) throws Exception {
        String path = UriComponentsBuilder.fromUriString("/businesses/metrika/grants/add")
                .queryParam("login", login)
                .queryParam("counter_ids", counterIds)
                .build()
                .toString();

        mvc.perform(
                        post(path).contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(resultMatcher);
    }
}

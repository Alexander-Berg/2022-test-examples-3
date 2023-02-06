package ru.yandex.market.sc.core.domain.place.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import ru.yandex.market.sc.core.domain.user.repository.User;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Для упрощения вызова. Можно использовать только один экземпляр одновременно.
 */
@Service
public class PlaceHistoryTestHelper {
    @Autowired
    JdbcTemplate jdbcTemplate;

    List<Map<String, Object>> before;
    List<Map<String, Object>> after;

    public void deleteHistory() {
        jdbcTemplate.update("delete from place_history");
    }

    public void startPlaceHistoryCollection() {
        before = jdbcTemplate.queryForList("select * from place_history");
    }

    public void validateThatOneRecordWithUserCollected(User user) {
        validateThatNRecordsWithUserCollected(1, user);
    }

    public void validateThatNRecordsWithUserCollected(int numberOfRecords, User user) {
        after = jdbcTemplate.queryForList("select * from place_history order by created_at");
        ArrayList<Map<String, Object>> result = new ArrayList<>(after);
        result.removeAll(before);

        assertThat(result).hasSize(numberOfRecords);
        validateResultWithUser(result, user);
    }

    public void validateThatNRecordsWithUserCollected(int numberOfRecords) {
        after = jdbcTemplate.queryForList("select * from place_history");
        ArrayList<Map<String, Object>> result = new ArrayList<>(after);
        result.removeAll(before);

        assertThat(result).hasSize(numberOfRecords);
        validateResultWithNotNullUser(result);
    }

    public ArrayList<Map<String, Object>> viewCollected() {
        var actualRecords = jdbcTemplate.queryForList("select * from place_history");
        ArrayList<Map<String, Object>> result = new ArrayList<>(actualRecords);
        result.removeAll(before);

        return result;
    }

        private void validateResultWithUser(ArrayList<Map<String, Object>> result, User user) {
        result.forEach(
                row -> {

                    Object persistedUserId = row.get("user_id");
                    String expectedUserId = user.getId().toString();
                    if (!expectedUserId.equals(persistedUserId + "")) {
                        System.out.println(
                            "Найден неподходящий пользователь. \nОжидание:" + expectedUserId + " \nРеальность:" + row);
                    }
                     assertThat(expectedUserId).isEqualTo(persistedUserId + "");
                }
        );
    }

    private void validateResultWithNotNullUser(ArrayList<Map<String, Object>> result) {
        result.forEach(
                row -> {
                    Object persistedUserId = row.get("user_id");
                    if (persistedUserId == null) {
                        System.out.println(
                                "Найден неподходящий пользователь. \nОжидание: пользователь сущнствует \nРеальность:" + row);
                    }
                    assertThat(persistedUserId).isNotNull();
                }
        );
    }

}

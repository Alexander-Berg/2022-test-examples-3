package ru.yandex.market.abo.core.premod;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.common.util.collections.MultiMap;
import ru.yandex.common.util.db.RowMappers;

/**
 * @author mixey
 *         @date 25.11.2008
 *         Time: 20:36:30
 */
public class PremodRecommendationTest extends EmptyTest {

    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    private String getTemplatePart(final String partName, final int id) {
        return jdbcTemplate.queryForObject("select " + partName + " from message_template where id = ?",
                RowMappers.stringAt(1), id);
    }

    /**
     * not a test
     * It's a tool for doc preparation (https://wiki.yandex-team.ru/Market/Abo/Premoderation2/RecommendationTemplates)
     */
    @Test
    @Disabled
    public void testGetAllRecommendationTemplates() {
        String sql = "SELECT recommendation, name, item_type_id FROM premod_problem_type ORDER BY id";

        MultiMap<Integer, String> itemTypeIdToRecommendations = new MultiMap<>();
        MultiMap<Integer, String> itemTypeIdToNames = new MultiMap<>();

        jdbcTemplate.query(sql, rs -> {
            itemTypeIdToRecommendations.append(rs.getInt("item_type_id"), rs.getString("recommendation"));
            itemTypeIdToNames.append(rs.getInt("item_type_id"), rs.getString("name"));
        });

        StringBuffer recommendationBody = new StringBuffer();

        jdbcTemplate.query(
                "SELECT id, recommendation_header FROM premod_item_type ORDER BY order_in_recommendation",
                rs -> {
                    final int itemTypeId = rs.getInt(1);
                    final String header = rs.getString(2);
                    final List<String> recommendations = itemTypeIdToRecommendations.get(itemTypeId);
                    final List<String> names = itemTypeIdToNames.get(itemTypeId);

                    if (recommendations != null && !recommendations.isEmpty()) {
                        recommendationBody.append("++ЗАГОЛОВОК:++ ").append(" **" + header + "**").append("\n\n");
                        for (int i = 0; i < recommendations.size(); i++) {
                            recommendationBody.append("++НАЗВАНИЕ:++ ").append(" !!" + names.get(i) + "!!")
                                    .append("\n");
                            recommendationBody.append("++РЕКОМЕНДАЦИЯ:++ ").append(recommendations.get(i))
                                    .append("\n\n");
                        }
                        recommendationBody.append("\n");
                    }
                });

        System.out.println("==Шаблоны рекомендаций магазину по результатам премодерации\n");
        System.out.println("===Типы проблем и соответствующие им рекомендации\n");

        System.out.println(recommendationBody.toString());

        System.out.println("\n===Шаблон письма магазиу\n");
        System.out.println("++subject:++ " + getTemplatePart("subject", 48) + "\n");
        System.out.println(getTemplatePart("message", 48) + "\n");

        System.out.println("\n===Шаблон письма магазиу в случае проблем с клоновостью\n");
        System.out.println("++subject:++ " + getTemplatePart("subject", 49) + "\n");
        System.out.println(getTemplatePart("message", 49) + "\n");

    }

}

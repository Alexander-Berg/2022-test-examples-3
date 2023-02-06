package ru.yandex.market.abo.mm.gen;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.common.util.collections.MultiMap;
import ru.yandex.market.abo.mm.db.DbMailGeneratorService;
import ru.yandex.market.abo.mm.model.Suggestion;
import ru.yandex.market.abo.mm.model.SuggestionEntityType;

/**
 * @author Olga Bolshakova (obolshakova@yandex-team.ru)
 *         @date 11.11.2008
 */
public class SimpleSuggestionSummatorTest extends EmptyTest {

    @Autowired
    private SimpleSuggestionSummator suggestionSummator;

    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @Autowired
    private DbMailGeneratorService dbMailGeneratorService;

    @Autowired
    private SuggestionGeneratorManager suggestionGeneratorManager;


    @Test
    public void testSumm() {

        final MultiMap<Long, Suggestion> sugg = new MultiMap<>();
        pgJdbcTemplate.query(
                "SELECT * FROM mm_suggestion WHERE message_id = 4404",
                rs -> {
                    final long entityId = rs.getLong("entity_id");
                    final SuggestionEntityType type = SuggestionEntityType.byCode(rs.getInt("entity_type"));
                    final float weight = rs.getFloat("weight");
                    final int genId = rs.getInt("gen_id");
                    final Suggestion suggestion = new Suggestion(11628, entityId, type, weight);
                    suggestion.setGenId(genId);
                    final int id = rs.getInt("id");
                    suggestion.setId(id);

                    final long messageId = rs.getLong("message_id");

                    sugg.append(messageId, suggestion);
                }
        );

        final Map<Integer, Float> genWeights = dbMailGeneratorService.loadGenWeights();
        final SuggestionContext context = suggestionGeneratorManager.loadSuggestionContext();

        for (final Long messageId : sugg.keySet()) {
            final List<Suggestion> suggestions = sugg.get(messageId);

            suggestionSummator.processMessageSuggestions(suggestions, genWeights, context);
            for (final Suggestion suggestion : suggestions) {
                System.out.println(suggestion);
            }
        }

    }

}

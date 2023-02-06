package ru.yandex.market.abo.gen.common;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.gen.GeneratorConfigurationException;
import ru.yandex.market.abo.gen.model.GeneratorProfile;
import ru.yandex.market.abo.gen.model.Hypothesis;

/**
 * @author Olga Bolshakova (obolshakova@yandex-team.ru)
 * @date 15.07.2008
 */
public class DbByModelGeneratorTest extends EmptyTest {

    @Autowired
    private DbByModelGenerator generator;

    @Autowired
    JdbcTemplate pgJdbcTemplate;

    @Test
    public void testGenerate() throws GeneratorConfigurationException {
        final GeneratorProfile generatorProfile = new GeneratorProfile(
                21, 50, "3"
        );
        generatorProfile.putParam("size_limit", "10");
        generatorProfile.putParam("query_id", "4");
        generatorProfile.putParam("comment", "comment");
        generatorProfile.putParam("check_method", "1");

        generator.configure(generatorProfile);
        final List<Hypothesis> hypothesis = generator.generate();
        for (final Hypothesis hyp : hypothesis) {
            System.out.println("hypothesis: " + hyp);
            System.out.println(generator.buildTicket(hyp, 1));
        }
    }

    @Test
    public void queryWithRegion() throws GeneratorConfigurationException {

        pgJdbcTemplate.update("insert into common_gen (id, gen_type, view_query) " +
                "values (1000, 'WARE_MD5', 'select 1 source_id, 774 shop_id, ''2u3c42nu'' ware_md5, 1 as weight, 213 region_id from dual')");

        GeneratorProfile profile = new GeneratorProfile(58, 1, "3");
        profile.putParam("size_limit", "30");
        profile.putParam("query_id", "1000");
        profile.putParam("comment", "comment");
        profile.putParam("check_method", "1");

        generator.configure(profile);

        generator.generate()
                .stream()
                .filter(hyp -> 213L == hyp.getRegionId())
                .map(hyp -> generator.buildTicket(hyp, 1L))
                .findFirst().orElseThrow(() -> new IllegalStateException("Should build at least one ticket"));
    }

    @Test
    public void allQueries() {
        pgJdbcTemplate.queryForList("select value::integer from hyp_gen_param p\n" +
                "  join hyp_gen g on g.id = p.gen_id\n" +
                "  where g.active = true\n" +
                "    and p.name = 'query_id'",
                Integer.class)
                .forEach(id -> {
                    GeneratorProfile profile = new GeneratorProfile();
                    profile.putParam("query_id", Integer.toString(id));
                    profile.setId(1);
                    generator.configure(profile);
                    generator.generate();
                });
    }
}

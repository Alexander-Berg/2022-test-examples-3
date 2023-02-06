package ru.yandex.market.partner.content.common.db;

import liquibase.integration.spring.SpringLiquibase;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.Sequence;
import org.springframework.beans.factory.InitializingBean;
import ru.yandex.market.partner.content.common.db.jooq.PartnerContent;

import java.util.List;
import java.util.Random;

/**
 * @author nkondratyeva
 */
public class SequenceStartRandomizer implements InitializingBean {
    private static final Random RANDOM = new Random(867492315);

    private final SpringLiquibase liquibase; //needed for the process to start after liquibase was initialized
    private final Configuration configuration;

    public SequenceStartRandomizer(SpringLiquibase liquibase, Configuration configuration) {
        this.liquibase = liquibase;
        this.configuration = configuration;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        List<Sequence<?>> sequences = PartnerContent.PARTNER_CONTENT.getSequences();

        DSLContext dsl = configuration.dsl();

        for (Sequence<?> sequence : sequences) {
            int randomPositiveInt = RANDOM.nextInt(1000) + 1;

            dsl.execute("select setval('partner_content." + sequence.getName() + "'," +  randomPositiveInt + ")");
        }
    }
}

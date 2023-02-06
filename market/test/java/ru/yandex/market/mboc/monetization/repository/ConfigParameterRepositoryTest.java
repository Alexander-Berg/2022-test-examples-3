package ru.yandex.market.mboc.monetization.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.mbo.jooq.repo.JooqWithDeletedFieldRepository;
import ru.yandex.market.mboc.common.BaseJooqWithDeletedFieldRepositoryTestClass;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.ConfigParameter;
import ru.yandex.market.mboc.monetization.config.MonetizationJooqConfig;

/**
 * @author eremeevvo
 * @since 17.10.2019
 */
@ContextConfiguration(classes = {MonetizationJooqConfig.class})
public class ConfigParameterRepositoryTest
    extends BaseJooqWithDeletedFieldRepositoryTestClass<ConfigParameter, Long> {

    @Autowired
    private ConfigParameterRepository repository;

    public ConfigParameterRepositoryTest() {
        super(ConfigParameter.class, ConfigParameter::getId);
        generatedFields = new String[]{"modifiedAt", "createdAt", "deleted"};
    }

    @Override
    protected JooqWithDeletedFieldRepository<ConfigParameter, ?, Long, ?, ?> repository() {
        return repository;
    }
}

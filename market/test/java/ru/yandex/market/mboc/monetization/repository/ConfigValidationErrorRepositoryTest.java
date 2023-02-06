package ru.yandex.market.mboc.monetization.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.mbo.jooq.repo.JooqWithDeletedFieldRepository;
import ru.yandex.market.mboc.common.BaseJooqWithDeletedFieldRepositoryTestClass;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.ConfigValidationError;
import ru.yandex.market.mboc.monetization.config.MonetizationJooqConfig;

/**
 * @author eremeevvo
 * @since 22.10.2019
 */
@ContextConfiguration(classes = {MonetizationJooqConfig.class})
public class ConfigValidationErrorRepositoryTest
    extends BaseJooqWithDeletedFieldRepositoryTestClass<ConfigValidationError, Long> {

    @Autowired
    private ConfigValidationErrorRepository repository;

    public ConfigValidationErrorRepositoryTest() {
        super(ConfigValidationError.class, ConfigValidationError::getId);
        generatedFields = new String[]{"modifiedAt", "createdAt", "deleted"};
    }

    @Override
    protected JooqWithDeletedFieldRepository<ConfigValidationError, ?, Long, ?, ?> repository() {
        return repository;
    }
}

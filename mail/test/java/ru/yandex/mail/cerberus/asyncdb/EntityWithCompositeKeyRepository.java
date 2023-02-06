package ru.yandex.mail.cerberus.asyncdb;

import ru.yandex.mail.cerberus.asyncdb.annotations.CompositeId;
import ru.yandex.mail.cerberus.asyncdb.annotations.ConfigureCrudRepository;

@CompositeId
@ConfigureCrudRepository(table = "composite_key_table")
public interface EntityWithCompositeKeyRepository extends CrudRepository<CompositeKey, EntityWithCompositeKey> {
}

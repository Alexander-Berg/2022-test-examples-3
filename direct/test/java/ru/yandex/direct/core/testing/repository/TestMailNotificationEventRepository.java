package ru.yandex.direct.core.testing.repository;

import java.util.Collection;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ru.yandex.direct.core.entity.mailnotification.model.MailNotificationEvent;
import ru.yandex.direct.core.entity.mailnotification.repository.MailNotificationEventRepository;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapper.JooqMapperWithSupplier;

import static ru.yandex.direct.dbschema.ppc.tables.Events.EVENTS;

@Repository
@ParametersAreNonnullByDefault
public class TestMailNotificationEventRepository {

    private final JooqMapperWithSupplier<MailNotificationEvent> jooqMapper;
    private final DslContextProvider dslContextProvider;

    @Autowired
    public TestMailNotificationEventRepository(DslContextProvider dslContextProvider,
                                               MailNotificationEventRepository mailNotificationEventRepository) {
        this.dslContextProvider = dslContextProvider;
        this.jooqMapper = mailNotificationEventRepository.jooqMapper;
    }

    /**
     * Возвращает события {@link MailNotificationEvent} для указанных {@code ownerUids}
     */
    public List<MailNotificationEvent> getEventsByOwnerUids(int shard, Collection<Long> ownerUids) {
        return dslContextProvider.ppc(shard)
                .select(jooqMapper.getFieldsToRead())
                .from(EVENTS)
                .where(EVENTS.OBJECTUID.in(ownerUids))
                .fetch(jooqMapper::fromDb);
    }
}

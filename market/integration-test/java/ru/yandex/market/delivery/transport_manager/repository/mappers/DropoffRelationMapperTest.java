package ru.yandex.market.delivery.transport_manager.repository.mappers;

import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.DropoffPair;
import ru.yandex.market.delivery.transport_manager.domain.entity.DropoffRelation;

public class DropoffRelationMapperTest extends AbstractContextualTest {
    @Autowired
    private DropoffRelationMapper mapper;

    private static final DropoffRelation FIRST = new DropoffRelation()
        .setId(1L)
        .setDropoffPointId(2L)
        .setScPointId(1L)
        .setScPartnerId(111L)
        .setDropoffPartnerId(112L)
        .setDeliveryId(3L);

    private static final DropoffRelation SECOND = new DropoffRelation()
        .setId(2L)
        .setDropoffPointId(6L)
        .setScPointId(12L)
        .setScPartnerId(101L)
        .setDropoffPartnerId(102L)
        .setDeliveryId(33L);

    @Test
    @ExpectedDatabase(
        value = "/repository/dropoff/after/after_insert.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void insert() {
        mapper.insert(List.of(FIRST, SECOND));
    }

    @Test
    @DatabaseSetup("/repository/dropoff/after/after_insert.xml")
    @ExpectedDatabase(
        value = "/repository/dropoff/after/after_insert_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void update() {
        DropoffRelation updated = new DropoffRelation()
            .setId(2L)
            .setDropoffPointId(6L)
            .setScPointId(12L)
            .setScPartnerId(101L)
            .setDropoffPartnerId(102L)
            .setDeliveryId(35L);
        mapper.insert(Set.of(updated));
    }

    @Test
    @DatabaseSetup("/repository/dropoff/dropoff_relations.xml")
    @ExpectedDatabase(
        value = "/repository/dropoff/after/after_delete.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void delete() {
        mapper.delete(Set.of(2L, 3L));
    }

    @Test
    void deleteEmptyNoExcept() {
        mapper.delete(Set.of());
    }

    @Test
    @DatabaseSetup("/repository/dropoff/after/after_insert.xml")
    void get() {
        Set<DropoffRelation> dropoffRelations = mapper.get();
        softly.assertThat(dropoffRelations).containsExactlyInAnyOrder(FIRST, SECOND);
    }

    @Test
    @DatabaseSetup("/repository/dropoff/dropoff_relations.xml")
    void getByPoints() {
        Set<DropoffRelation> relations = mapper.getByPoints(Set.of(
            new DropoffPair(1L, 10L),
            new DropoffPair(6L, 7L)
        ));

        softly.assertThat(relations).containsExactlyInAnyOrder(
            new DropoffRelation()
                .setId(2L)
                .setDeliveryId(33L)
                .setScPointId(1L)
                .setScPartnerId(103L)
                .setDropoffPartnerId(104L)
                .setDropoffPointId(10L),
            new DropoffRelation()
                .setId(4L)
                .setDeliveryId(31L)
                .setScPointId(6L)
                .setScPartnerId(107L)
                .setDropoffPartnerId(108L)
                .setDropoffPointId(7L)
        );
    }
}

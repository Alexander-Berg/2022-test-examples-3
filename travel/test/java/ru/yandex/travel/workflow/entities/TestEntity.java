package ru.yandex.travel.workflow.entities;

import java.time.Instant;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import lombok.Getter;
import lombok.Setter;

import ru.yandex.travel.test.fake.proto.TTestState;

@Entity
@Table(name = "test_entities")
@Getter
@Setter
public class TestEntity implements WorkflowEntity<TTestState> {
    @Id
    private UUID id;

    @OneToOne
    private Workflow workflow;

    @Enumerated(EnumType.STRING)
    private TTestState state;

    private Instant lastTransitionAt;

    @Version
    private Integer version;

    @Override
    public String getEntityType() {
        return "TEST_ENTITY";
    }

}

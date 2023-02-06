package ru.yandex.market.logistics.cs.domain.entity;


import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter(AccessLevel.PRIVATE)
@Builder
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Entity
@Table(name = "queue_task")
public class QueueTask {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "queue_name")
    private String queueName;

    @Column(name = "task")
    private String task;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "process_time")
    private LocalDateTime processTime;

    @Column(name = "attempt")
    private Integer attempt;

    @Column(name = "actor")
    private String actor;

    @Column(name = "log_timestamp")
    private String logTimestamp;

}

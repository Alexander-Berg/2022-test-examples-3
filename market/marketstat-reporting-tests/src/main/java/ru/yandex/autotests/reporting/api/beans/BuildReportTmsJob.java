package ru.yandex.autotests.reporting.api.beans;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.yandex.autotests.market.stat.mappers.Column;

import java.time.LocalDateTime;

/**
 * Created by kateleb on 17.11.16.
 */
@ToString
@Getter
@Setter
public class BuildReportTmsJob {
    @Column
    private String id;
    @Column
    private String name;
    @Column(name = "submitted_by")
    private String submittedBy;
    @Column
    private String profile;
    @Column
    private String params;
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
    @Column
    private String status;
    @Column
    private String result;

    public BuildReportTmsJob() {
    }
}

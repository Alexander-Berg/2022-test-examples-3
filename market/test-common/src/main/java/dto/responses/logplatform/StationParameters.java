package dto.responses.logplatform;

import javax.annotation.Nullable;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class StationParameters {
    private final Long limit;
    private final String dump;
    private final String capacity;

    private final String stationId;
    private final String operatorId;

    private StationParameters(
        Long limit,
        String dump,
        String capacity,
        @Nullable String stationId,
        @Nullable String operatorId
    ) {
        this.limit = limit != null ? limit : 10000;
        this.dump = dump != null ? dump : "eventlog";
        this.capacity = capacity != null ? capacity : "true";
        this.stationId = stationId;
        this.operatorId = operatorId;
    }
}

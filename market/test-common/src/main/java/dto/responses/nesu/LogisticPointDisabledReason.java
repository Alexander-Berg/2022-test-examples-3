package dto.responses.nesu;

import java.util.Set;

import lombok.Data;

@Data
public class LogisticPointDisabledReason {
    private String type;
    private Set<Integer> unsupportedCargoTypes;
}

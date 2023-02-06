package dto.responses.nesu;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import lombok.Data;

@Data
public class ShipmentLogisticPoint {
    private Long id;
    private String shipmentType;
    private String pointType;
    private Set<Integer> forbiddenCargoTypes;
    private Set<LogisticPointDisabledReason> disabledReasons;
    private List<LocalDate> dayoffs;
    private Object returnSortingCenter;
}

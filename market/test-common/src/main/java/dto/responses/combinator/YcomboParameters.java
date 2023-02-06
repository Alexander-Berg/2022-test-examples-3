package dto.responses.combinator;

import java.math.BigDecimal;
import java.util.EnumSet;

import dto.requests.checkouter.RearrFactor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class YcomboParameters {
    private Long warehouse;
    private Integer region;
    private Integer weight;
    private Integer[] dimensions;
    private BigDecimal latitude;
    private BigDecimal longitude;
    @Builder.Default
    private EnumSet<RearrFactor> experiment = EnumSet.noneOf(RearrFactor.class);
}

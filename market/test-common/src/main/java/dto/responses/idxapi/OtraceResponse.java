package dto.responses.idxapi;

import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OtraceResponse {

    @JsonProperty("offer")
    private Offer offer;

    @JsonProperty("urls")
    private Urls urls;

    @JsonProperty("Тарифы от калькулятора доставки, которые неприменимы к офферу")
    private HashMap<String, List<IdxBucket>> unavailableTariffs;

    @JsonProperty("Доступные тарифы от калькулятора доставки")
    private HashMap<String, List<IdxBucket>> availableTariffs;

    public List<IdxBucket> getUnavailableTariffs(IdxTarif idxTarif) {
        return unavailableTariffs.get(idxTarif.returnName());
    }

    public List<IdxBucket> getAvailableTariffs(IdxTarif idxTarif) {
        return availableTariffs.get(idxTarif.returnName());
    }
}

package dto.responses.inbounds;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Info {

    @JsonProperty("boxes")
    private List<Object> boxes;

    @JsonProperty("unsortedPallets")
    private List<Object> unsortedPallets;

    @JsonProperty("unpackedBoxes")
    private List<Object> unpackedBoxes;

    @JsonProperty("pallets")
    private List<Object> pallets;

    public List<Object> getBoxes() {
        return boxes;
    }

    public List<Object> getUnsortedPallets() {
        return unsortedPallets;
    }

    public List<Object> getUnpackedBoxes() {
        return unpackedBoxes;
    }

    public List<Object> getPallets() {
        return pallets;
    }
}

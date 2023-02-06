package dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CargoType {

    BULKY_CARGO_20_KG(310),
    POWDERS(470),
    CHEMICALS(480);

    private final int cargoTypeCode;
}

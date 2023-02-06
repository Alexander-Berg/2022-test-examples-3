package ru.yandex.market.tpl.api.advice;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

import ru.yandex.market.tpl.common.personal.client.HasPersonalAddress;
import ru.yandex.market.tpl.common.personal.client.HasPersonalEmail;
import ru.yandex.market.tpl.common.personal.client.HasPersonalFio;
import ru.yandex.market.tpl.common.personal.client.HasPersonalGpsCoords;
import ru.yandex.market.tpl.common.personal.client.HasPersonalPhone;
import ru.yandex.market.tpl.common.personal.client.model.GpsCoord;
import ru.yandex.market.tpl.common.personal.client.model.PersonalAddressKeys;

@Data
@NoArgsConstructor
public class HasPersonalFieldTestImpl implements HasPersonalPhone, HasPersonalEmail, HasPersonalFio, HasPersonalAddress,
        HasPersonalGpsCoords {

    public HasPersonalFieldTestImpl(String personalPhoneId, String personalEmailId, String personalNameId,
                                    String personalAddressId, String personalGpsId) {
        this.personalPhoneId = personalPhoneId;
        this.personalEmailId = personalEmailId;
        this.personalNameId = personalNameId;
        this.personalAddressId = personalAddressId;
        this.personalGpsId = personalGpsId;
    }

    private String personalPhoneId;
    private String phone;

    private String personalEmailId;
    private String email;

    private String personalNameId;
    private String name;

    private String personalAddressId;
    private String address;

    private String personalGpsId;
    private BigDecimal longitude;
    private BigDecimal latitude;


    @Override
    public void setRecipientEmail(String email) {
        this.email = email;
    }

    @Override
    public String getPersonalEmailId() {
        return this.personalEmailId;
    }

    @Override
    public void setRecipientFio(String fio) {
        this.name = fio;
    }

    @Override
    public String getPersonalFioId() {
        return this.personalNameId;
    }


    @Override
    public void setRecipientPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public String getPersonalPhoneId() {
        return this.personalPhoneId;
    }

    @Override
    public void setGpsCoords(GpsCoord gpsCoord) {
        this.longitude = gpsCoord.getLongitude();
        this.latitude = gpsCoord.getLatitude();
    }

    @JsonIgnore
    @Override
    public List<PersonalAddressKeys> getAvailableAddressKeys() {
        return List.of(PersonalAddressKeys.LOCALITY);
    }
}

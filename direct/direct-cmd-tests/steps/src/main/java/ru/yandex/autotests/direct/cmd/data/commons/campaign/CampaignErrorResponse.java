package ru.yandex.autotests.direct.cmd.data.commons.campaign;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.savecamp.GeoCharacteristic;

public class CampaignErrorResponse extends ErrorResponse {

    @SerializedName("campaign")
    private CampaignErrors campaignErrors;


    public CampaignErrors getCampaignErrors() {
        return campaignErrors;
    }

    public class CampaignErrors {
        @SerializedName("geo_changes")
        private Map<String, GeoCharacteristic> geoChanges;

        @SerializedName("error")
        private String error;

        public String getError() {
            return error;
        }

        public Map<String, GeoCharacteristic> getGeoChanges() {
            return geoChanges;
        }
    }
}

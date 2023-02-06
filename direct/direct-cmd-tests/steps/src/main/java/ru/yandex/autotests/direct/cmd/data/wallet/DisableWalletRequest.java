package ru.yandex.autotests.direct.cmd.data.wallet;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

/**
 * Created by aleran on 18.11.2015.
 */
public class DisableWalletRequest extends BasicDirectRequest {

    @SerializeKey("media_camps")
    private String mediaCamps;

    @SerializeKey("AgencyID")
    private String agencyId;

    public String getMediaCamps() {
        return mediaCamps;
    }

    public void setMediaCamps(String mediaCamps) {
        this.mediaCamps = mediaCamps;
    }

    public String getAgencyId() {
        return agencyId;
    }

    public void setAgencyId(String agencyId) {
        this.agencyId = agencyId;
    }
}

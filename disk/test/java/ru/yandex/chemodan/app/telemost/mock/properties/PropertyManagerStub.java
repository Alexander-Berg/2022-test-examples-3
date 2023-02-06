package ru.yandex.chemodan.app.telemost.mock.properties;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.chemodan.app.telemost.services.PropertyManager;

public class PropertyManagerStub implements PropertyManager {

    MapF<PropertyNames, Boolean> values = Cf.hashMap();

    public PropertyManagerStub() {
        reset();
    }

    public void reset() {
        values.clear();
        values.put(PropertyNames.LOCAL_RECORDING_ALLOWED, true);
        values.put(PropertyNames.CLOUD_RECORDING_ALLOWED, true);
        values.put(PropertyNames.NON_YATEAM_CHATS_ALLOWED, true);
        values.put(PropertyNames.YATEAM_CHATS_ALLOWED, true);
        values.put(PropertyNames.NON_YATEAM_CONTROL_ALLOWED, true);
        values.put(PropertyNames.YATEAM_CONTROL_ALLOWED, true);
        values.put(PropertyNames.NON_YATEAM_BROADCAST_ALLOWED, true);
        values.put(PropertyNames.YATEAM_BROADCAST_ALLOWED, true);
    }

    public void setValue(PropertyNames property, boolean value) {
        values.put(property, value);
    }

    @Override
    public Long getConferenceTtl() {
        return DEFAULT_CONFERENCE_TTL_SECS;
    }

    @Override
    public Long getConferenceTbl() {
        return DEFAULT_CONFERENCE_TBL_SECS;
    }

    @Override
    public boolean getUseLocalRecordingAllowed() {
        return values.getO(PropertyNames.LOCAL_RECORDING_ALLOWED).get();
    }

    @Override
    public boolean getUseCloudRecordingAllowed() {
        return values.getO(PropertyNames.CLOUD_RECORDING_ALLOWED).get();
    }

    @Override
    public boolean getUseNonYaTeamChatsAllowed() {
        return values.getO(PropertyNames.NON_YATEAM_CHATS_ALLOWED).get();
    }

    @Override
    public boolean getUseYaTeamChatsAllowed() {
        return values.getO(PropertyNames.YATEAM_CHATS_ALLOWED).get();
    }

    @Override
    public boolean getUseNonYaTeamControlAllowed() {
        return values.getO(PropertyNames.NON_YATEAM_CONTROL_ALLOWED).get();
    }

    @Override
    public boolean getUseYaTeamControlAllowed() {
        return values.getO(PropertyNames.YATEAM_CONTROL_ALLOWED).get();
    }

    @Override
    public boolean getUseNonYaTeamBroadcastAllowed() {
        return values.getO(PropertyNames.NON_YATEAM_BROADCAST_ALLOWED).get();
    }

    @Override
    public boolean getUseYaTeamBroadcastAllowed() {
        return values.getO(PropertyNames.YATEAM_BROADCAST_ALLOWED).get();
    }

}

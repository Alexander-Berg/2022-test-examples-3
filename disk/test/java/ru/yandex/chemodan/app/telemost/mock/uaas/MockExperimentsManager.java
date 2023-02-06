package ru.yandex.chemodan.app.telemost.mock.uaas;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.chemodan.app.uaas.client.UaasClient;
import ru.yandex.chemodan.app.uaas.experiments.ExperimentsManager;
import ru.yandex.chemodan.app.uaas.parser.UaasConditionParser;
import ru.yandex.chemodan.app.uaas.zk.UaasOverrideController;
import ru.yandex.inside.passport.PassportUid;

public class MockExperimentsManager extends ExperimentsManager {

    private final MapF<Long, ListF<String>> userFlags = Cf.hashMap();

    public MockExperimentsManager(UaasConditionParser uaasConditionParser, UaasOverrideController uaasOverrideController,
            UaasClient uaasClient) {
        super(uaasConditionParser, uaasOverrideController, uaasClient);
    }

    public void addFlagsForUser(PassportUid uid, ListF<String> flags) {
        Long key = uid.toUidOrZero().getUid();
        ListF<String> currentFlags = getFlags(key);
        userFlags.put(key, currentFlags.plus(flags).stableUnique());
    }

    public void removeFlagForUser(PassportUid uid, String flag) {
        Long key = uid.toUidOrZero().getUid();
        ListF<String> currentFlags = getFlags(key);
        userFlags.put(key, currentFlags.filter(flagValue -> !flagValue.equals(flag)));
    }

    @Override
    public ListF<String> getFlags(long uid) {
        return userFlags.getO(uid).getOrElse(Cf.list());
    }
}

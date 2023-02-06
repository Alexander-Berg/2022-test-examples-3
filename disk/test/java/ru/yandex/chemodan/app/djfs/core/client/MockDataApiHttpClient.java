package ru.yandex.chemodan.app.djfs.core.client;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;

public class MockDataApiHttpClient extends DataApiHttpClient {
    private DataapiMordaBlockList blocks;

    public void addNostalgyDataapiBlock(DataapiMordaBlock block) {
        if (blocks == null) {
            blocks = new DataapiMordaBlockList(Cf.list());
        }
        blocks = new DataapiMordaBlockList(blocks.getItems().plus(Cf.list(block)));
    }

    public void resetBlocks() {
        blocks = new DataapiMordaBlockList(Cf.list());
    }

    public MockDataApiHttpClient() {
        super(null, null);
    }

    @Override
    public DataapiMordaBlockList fetchNostalgyBlocks(DjfsUid uid, int limit) {
        if (blocks == null) {
            return new DataapiMordaBlockList(Cf.list());
        }
        return blocks;
    }

    @Override
    public Option<ProfileAddress> fetchAddress(DjfsUid uid, ProfileAddressType addressType) {
        return Option.empty();
    }
}

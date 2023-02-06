package ru.yandex.chemodan.app.djfs.core.client;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.internal.NotImplementedException;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsFileId;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;

public class MockDiskSearchHttpClient extends DiskSearchHttpClient {
    private ListF<DiskSearchResponseItem> items;
    private FacesDeltas facesDeltas;

    public MockDiskSearchHttpClient() {
        super(null, null, null, null);
        items = Cf.arrayList();
    }

    public void addItem(DiskSearchResponseItem item) {
        items.add(item);
    }

    public void resetItems() {
        items.clear();
    }

    @Override
    public DiskSearchResponse getExtractedData(DjfsUid uid, ListF<DjfsFileId> fileIds, boolean isFastMovedUser) {
        return new DiskSearchResponse(items.length(), items);
    }

    @Override
    public FaceClustersSnapshot getFaceClusters(DjfsUid uid) {
        if (uid.asLong() != 31337)
            throw new NotImplementedException();
        return FaceClustersSnapshot.jsonParser.parseJson(
            "{\n" +
            "    \"version\": 1,\n" +
            "    \"items\": [\n" +
            "        {\n" +
            "            \"id\": \"31337_1_0\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"id\": \"31337_1_1\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"id\": \"31337_1_2\"\n" +
            "        }\n" +
            "    ]\n" +
            "}"
        );
    }

    @Override
    public FaceClusterPhotos getClusterPhotos(DjfsUid uid, String cluster) {
        if (uid.asLong() != 31337)
            throw new NotImplementedException("uid = " + uid + ", cluster = " + cluster);
        String json;
        if (cluster.equals("31337_1_0")) {
            json = "{" +
                    "   \"cluster_id\": \"" + cluster + "\", " +
                    "   \"items\": [" +
                    "       {" +
                    "           \"face_coord_x\": \"0.58\", " +
                    "           \"face_coord_y\": \"0.58\", " +
                    "           \"face_height\": \"0.60\", " +
                    "           \"face_width\": \"0.61\", " +
                    "           \"height\": \"66\", " +
                    "           \"width\": \"67\", " +
                    "           \"resource_id\": \"31337:cab43abb3ae65359313949b3a146c2f9129fb1c4a3c115eefe672a21890f3fa3\"" +
                    "       }" +
                    "   ]" +
                    "}";
        } else if (cluster.equals("31337_1_1")) {
            json =
                "{\n" +
                "    \"cluster_id\": \"" + cluster + "\",\n" +
                "    \"items\": [\n" +
                "        {\n" +
                "            \"face_height\": \"0.1026368696\",\n" +
                "            \"face_coord_x\": \"0.4761917114\",\n" +
                "            \"face_coord_y\": \"0.1296961454\",\n" +
                "            \"face_cluster_id\": \"31337_1_1\",\n" +
                "            \"type\": \"face\",\n" +
                "            \"face_confidence\": \"0.909399\",\n" +
                "            \"resource_id\": \"31337:27ccb2a4bc8d274186fb8b006e6a0690745b5f7a788de909c53f204b928c7767\",\n" +
                "            \"face_width\": \"0.05893343608\",\n" +
                "            \"cost_disk_aethetic_0\": \"0.826851\",\n" +
                "            \"face_gender\": \"0.0837462\",\n" +
                "            \"width\": \"1200\",\n" +
                "            \"face_id\": \"31337:27ccb2a4bc8d274186fb8b006e6a0690745b5f7a788de909c53f204b928c7767_2\",\n" +
                "            \"id\": \"face_31337:27ccb2a4bc8d274186fb8b006e6a0690745b5f7a788de909c53f204b928c7767_2\",\n" +
                "            \"face_age\": \"33.4693\",\n" +
                "            \"height\": \"799\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"face_height\": \"0.1142490755\",\n" +
                "            \"face_coord_x\": \"0.6345787048\",\n" +
                "            \"face_coord_y\": \"0.1598756812\",\n" +
                "            \"face_cluster_id\": \"31337_1_1\",\n" +
                "            \"type\": \"face\",\n" +
                "            \"face_confidence\": \"0.82245\",\n" +
                "            \"resource_id\": \"31337:27ccb2a4bc8d274186fb8b006e6a0690745b5f7a788de909c53f204b928c7767\",\n" +
                "            \"face_width\": \"0.06409645081\",\n" +
                "            \"cost_disk_aethetic_0\": \"0.826851\",\n" +
                "            \"face_gender\": \"0.0301332\",\n" +
                "            \"width\": \"1200\",\n" +
                "            \"face_id\": \"31337:27ccb2a4bc8d274186fb8b006e6a0690745b5f7a788de909c53f204b928c7767_3\",\n" +
                "            \"id\": \"face_31337:27ccb2a4bc8d274186fb8b006e6a0690745b5f7a788de909c53f204b928c7767_3\",\n" +
                "            \"face_age\": \"27.4327\",\n" +
                "            \"height\": \"799\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"face_height\": \"0.09660942832\",\n" +
                "            \"face_coord_x\": \"0.309209493\",\n" +
                "            \"face_coord_y\": \"0.4322328341\",\n" +
                "            \"face_cluster_id\": \"31337_1_1\",\n" +
                "            \"type\": \"face\",\n" +
                "            \"face_confidence\": \"0.960988\",\n" +
                "            \"resource_id\": \"31337:927daf4d8b10336d27bcf14d487ca781e1cbfe40f8517b6d7b07d36d43a45b71\",\n" +
                "            \"face_width\": \"0.05015030543\",\n" +
                "            \"cost_disk_aethetic_0\": \"2.73072\",\n" +
                "            \"face_gender\": \"0.0149385\",\n" +
                "            \"width\": \"1200\",\n" +
                "            \"face_id\": \"31337:927daf4d8b10336d27bcf14d487ca781e1cbfe40f8517b6d7b07d36d43a45b71_0\",\n" +
                "            \"id\": \"face_31337:927daf4d8b10336d27bcf14d487ca781e1cbfe40f8517b6d7b07d36d43a45b71_0\",\n" +
                "            \"face_age\": \"37.5297\",\n" +
                "            \"height\": \"799\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"face_height\": \"0.09660942832\",\n" +
                "            \"face_coord_x\": \"0.309209493\",\n" +
                "            \"face_coord_y\": \"0.4322328341\",\n" +
                "            \"face_cluster_id\": \"31337_1_1\",\n" +
                "            \"type\": \"face\",\n" +
                "            \"face_confidence\": \"0.960988\",\n" +
                "            \"resource_id\": \"47806:43f754eccd1b3c6987498c0ba7a21dbe49764a6d9fe73918d0ed5433502c2880\",\n" +
                "            \"face_width\": \"0.05015030543\",\n" +
                "            \"cost_disk_aethetic_0\": null,\n" + // null for purpose
                "            \"face_gender\": \"0.0149385\",\n" +
                "            \"width\": \"1200\",\n" +
                "            \"face_id\": \"47806:43f754eccd1b3c6987498c0ba7a21dbe49764a6d9fe73918d0ed5433502c2880_0\",\n" +
                "            \"id\": \"face_47806:43f754eccd1b3c6987498c0ba7a21dbe49764a6d9fe73918d0ed5433502c2880_0\",\n" +
                "            \"face_age\": \"37.5297\",\n" +
                "            \"height\": \"799\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"face_height\": \"0.1091610523\",\n" +
                "            \"face_coord_x\": \"0.7697073364\",\n" +
                "            \"face_coord_y\": \"0.2218144194\",\n" +
                "            \"face_cluster_id\": \"31337_1_1\",\n" +
                "            \"type\": \"face\",\n" +
                "            \"face_confidence\": \"0.95887\",\n" +
                "            \"resource_id\": \"31337:e81455a5fe5879dbc8b10ef7cb7b56d56ab4d5e67f81e3fd2987721f74c86b84\",\n" +
                "            \"face_width\": \"0.06462818146\",\n" +
                // removed "cost_disk_aethetic_0" for purpose
                "            \"face_gender\": \"0.996191\",\n" +
                "            \"width\": \"1200\",\n" +
                "            \"face_id\": \"31337:e81455a5fe5879dbc8b10ef7cb7b56d56ab4d5e67f81e3fd2987721f74c86b84_0\",\n" +
                "            \"id\": \"face_31337:e81455a5fe5879dbc8b10ef7cb7b56d56ab4d5e67f81e3fd2987721f74c86b84_0\",\n" +
                "            \"face_age\": \"30.8363\",\n" +
                "            \"height\": \"799\"\n" +
                "        }\n" +
                "    ]\n" +
                "}";
        } else {
            json = "{\"cluster_id\": \"" + cluster + "\", \"items\": []}";
        }
        return FaceClusterPhotos.jsonParser.parseJson(json);
    }

    public void setFacesDeltas(long version, FacesDelta... facesDeltas) {
        this.facesDeltas = new FacesDeltas(version, Cf.list(facesDeltas), false);
    }

    @Override
    public FacesDeltas getFacesDeltas(DjfsUid uid, long knownVersion) {
        if (facesDeltas != null) {
            final FacesDeltas t = facesDeltas;
            facesDeltas = null;
            return t;
        } else {
            throw new NotImplementedException("no data");
        }
    }

    @Override
    public void reindexFaces(DjfsUid uid) {
        /* skip */
    }
}

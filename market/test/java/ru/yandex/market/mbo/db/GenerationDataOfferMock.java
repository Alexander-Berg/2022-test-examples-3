package ru.yandex.market.mbo.db;

import com.google.protobuf.ByteString;
import ru.yandex.market.mbo.http.OffersStorage;
import ru.yandex.market.proto.indexer.v2.Pictures;

public class GenerationDataOfferMock {

    private static final float CLOTHES_BIN = 0.1F;

    private GenerationDataOfferMock() {
    }

    public static OffersStorage.GenerationDataOffer createOffer() {
        OffersStorage.GenerationDataOffer.Builder offerBuilder = OffersStorage.GenerationDataOffer.newBuilder();

        OffersStorage.PictureMeta.Builder pictureMetaBuilder = OffersStorage.PictureMeta.newBuilder();

        Pictures.Picture.Builder pictureBuilder = Pictures.Picture.newBuilder();

        Pictures.VersionedSignature.Builder signatureBuilder = Pictures.VersionedSignature.newBuilder();
        signatureBuilder.setClothesBin(CLOTHES_BIN);
        signatureBuilder.setClothes(ByteString.copyFromUtf8("bs1"));
        signatureBuilder.setSimilar(ByteString.copyFromUtf8("bs2"));
        signatureBuilder.setVersion(1);

        pictureBuilder.addSignatures(signatureBuilder.build());
        pictureMetaBuilder.addPicture(pictureBuilder.build());

        offerBuilder.setPictureMeta(pictureMetaBuilder.build());

        return offerBuilder.build();
    }
}

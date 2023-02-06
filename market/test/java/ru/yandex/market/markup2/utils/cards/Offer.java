package ru.yandex.market.markup2.utils.cards;

/**
 * @author inenakhov
 */
public class Offer {
    private String id;
    private String wareMd5;
    private long feedId;
    private long modelId;
    private long clusterId;
    private String description;
    private String picUrl;


    public Offer(String id, String description, String picUrl,
                 long feedId, long modelId, long clusterId, String wareMd5) {
        this.id = id;
        this.feedId = feedId;
        this.modelId = modelId;
        this.clusterId = clusterId;
        this.description = description;
        this.picUrl = picUrl;
        this.wareMd5 = wareMd5;
    }

    public String getId() {
        return id;
    }

    public long getModelId() {
        return modelId;
    }

    public long getClusterId() {
        return clusterId;
    }

    public String getDescription() {
        return description;
    }

    public String getPicUrl() {
        return picUrl;
    }

    public long getFeedId() {
        return feedId;
    }

    public String getWareMd5() {
        return wareMd5;
    }

    public ru.yandex.market.markup2.utils.offer.Offer toYTOffer() {
        ru.yandex.market.markup2.utils.offer.Offer.OfferBuilder offerBuilder =
            ru.yandex.market.markup2.utils.offer.Offer.newBuilder();

        offerBuilder.setClusterId(this.clusterId);
        offerBuilder.setDescription(this.description);
        offerBuilder.setMatchedId(this.modelId);
        offerBuilder.setWareMd5(this.wareMd5);
        offerBuilder.setPicturesUrls(new String[]{this.picUrl});
        offerBuilder.setOfferId(this.id);
        offerBuilder.setYmlParams(new ru.yandex.market.ir.http.Offer.YmlParam[]{});

        return offerBuilder.build();
    }

    @Override
    public String toString() {
        return "Offer{" +
            "id='" + id + '\'' +
            ", wareMd5='" + wareMd5 + '\'' +
            ", feedId=" + feedId +
            ", modelId=" + modelId +
            ", clusterId=" + clusterId +
            ", description='" + description + '\'' +
            ", picUrl='" + picUrl + '\'' +
            '}';
    }
}

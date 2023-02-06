package ru.yandex.market.markup2.utils.cards;

import com.google.common.collect.Lists;
import org.datanucleus.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author inenakhov
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class StorageData {
    private final int categoryId = 1;
    private static final String BAD_URL = "http://bad";
    private final ArrayList<Offer> allOffers = new ArrayList<>();
    {
        //Offers from first cluster (id 11)
        allOffers.add(createOffer("1", BAD_URL, 5, 0, 11L));
        allOffers.add(createOffer("2", BAD_URL, 4, 0, 11L));
        allOffers.add(createOffer("3", ClusterImageFinderMock.GOOD_IMAGE_URL, 3, 0, 11L));
        allOffers.add(createOffer("4", BAD_URL, 2, 0, 11L));
        allOffers.add(createOffer("5", BAD_URL, 1, 0, 11L));

        //Offers from second cluster (id 12)
        allOffers.add(createOffer("6", BAD_URL, 1, 0, 12L));
        allOffers.add(createOffer("7", ClusterImageFinderMock.GOOD_IMAGE_URL, 2, 0, 12L));
        allOffers.add(createOffer("8", ClusterImageFinderMock.GOOD_IMAGE_URL, 3, 0, 12L));
        allOffers.add(createOffer("9", BAD_URL, 4, 0, 12L));

        //Matched offers (Offers matched to cards 1, 2, 3)
        allOffers.add(createOffer("10", BAD_URL, 1, 1, 0));
        allOffers.add(createOffer("11", BAD_URL, 2, 2, 0));
        allOffers.add(createOffer("12", ClusterImageFinderMock.GOOD_IMAGE_URL, 3, 3, 10));
        allOffers.add(createOffer("13", BAD_URL, 4, 4, 10));

        //Offers from third no title cluster. Offers also are matched to guru cards 1, 2, 3, 4
        allOffers.add(createOffer("14", ClusterImageFinderMock.GOOD_IMAGE_URL, 4, 1, 13L));
        allOffers.add(createOffer("15", BAD_URL, 4, 2, 13L));
        allOffers.add(createOffer("16", BAD_URL, 4, 3, 13L));
        allOffers.add(createOffer("17", BAD_URL, 4, 4, 13L));

        //Offers from fourth unpublished cluster
        allOffers.add(createOffer("18", ClusterImageFinderMock.GOOD_IMAGE_URL, 4, 0, 14L));
        allOffers.add(createOffer("19", BAD_URL, 4, 0, 14L));
    }

    private final Set<String> allOfferIds = allOffers.stream().map(Offer::getId).collect(Collectors.toSet());

    /**
        Do not use equal ids for guru cards and clusters.
     **/
    private final ArrayList<InStorageCard> guruCardsInStorage =
        Lists.newArrayList(createInStorageGuru(1L, true),
                           createInStorageGuru(2L, true),
                           createInStorageGuru(3L, true),
                           //card with no image, should be ignored.
                           createInStorageGuruNoImage(4L, true),
                           createInStorageGuru(5L, false));

    private final ArrayList<InStorageCard> clustersInStorage = Lists.newArrayList(
        createInStorageCluster(11L, true),
        createInStorageCluster(12L, true),
        createInStorageCluster(13L, InStorageCardsFinder.NO_TITLE_CLUSTER_TITLE, false),
        createInStorageCluster(14L, false));

    public StorageData() {
    }

    public List<Offer> getOffers() {
        return Collections.unmodifiableList(allOffers);
    }

    public Set<String> getOffersIds() {
        return Collections.unmodifiableSet(allOfferIds);
    }

    public List<InStorageCard> getGuruCardsInStorage() {
        return Collections.unmodifiableList(guruCardsInStorage);
    }

    public List<Card> getGuruCards() {
        return guruCardsInStorage.stream().map(InStorageCard::toCard).collect(Collectors.toList());
    }

    public List<Card> getMboPublishedGuruCardsWithPics() {
        return guruCardsInStorage.stream()
            .filter(InStorageCard::isMboPublished)
            .filter(card -> !StringUtils.isEmpty(card.getImageUrl()))
            .map(InStorageCard::toCard).collect(Collectors.toList());
    }

    public List<Card> getMboClusterCards() {
        return clustersInStorage.stream().map(InStorageCard::toCard).collect(Collectors.toList());
    }


    public List<Card> getMboPublishedClusterCards() {
        return clustersInStorage.stream()
            .filter(InStorageCard::isMboPublished)
            .map(InStorageCard::toCard).collect(Collectors.toList());
    }

    public List<Offer> getOffersOnCard(long cardId) {
        ArrayList<Offer> offers = new ArrayList<>();
        for (Offer offer : allOffers) {
            if (offer.getModelId() == cardId || offer.getClusterId() == cardId) {
                offers.add(offer);
            }
        }

        return offers;
    }

    public List<Card> getMboPublishedCards() {
        ArrayList<Card> cards = new ArrayList<>(getMboClusterCards());
        cards.addAll(getMboPublishedClusterCards());
        return cards;
    }

    public List<InStorageCard> getClusterCardsInStorage() {
        return Collections.unmodifiableList(clustersInStorage);
    }

    public int getCategoryId() {
        return categoryId;
    }

    private Offer createOffer(String id, String url, int feedId, int modelId, long clusterId) {
        return new Offer(id, "|Описание: офер " + id, url, feedId, modelId, clusterId, id);
    }

    private InStorageCard createInStorageGuru(long id, boolean published) {
        return new InStorageCard(id, "title" + id,
                                 "http://modelUrl" + id, "description" + id,
                                 categoryId, CardType.MODEL, published);
    }

    private InStorageCard createInStorageGuruNoImage(long id, boolean published) {
        return new InStorageCard(id, "title" + id,
                                 "", "description" + id,
                                 categoryId, CardType.MODEL, published);
    }

    private InStorageCard createInStorageCluster(long id, boolean published) {
        return new InStorageCard(id, "title" + id,
                                 "", "",
                                 categoryId, CardType.CLUSTER, published);
    }

    private InStorageCard createInStorageCluster(long id, String title, boolean published) {
        return new InStorageCard(id, title,
                                 "", "",
                                 categoryId, CardType.CLUSTER, published);
    }
}

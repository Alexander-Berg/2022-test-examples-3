package ru.yandex.autotests.direct.cmd.data.forecast;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.directapi.common.api45.BannerPhraseInfo;
import ru.yandex.autotests.directapi.common.api45.GetForecastInfo;
import ru.yandex.autotests.directapi.common.api45.PhraseAuctionBids;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by aleran on 29.09.2015.
 */
public class AjaxDataForBudgetForecastResponse extends BasicDirectRequest {

    @SerializedName("phrase2key")
    HashMap<String, String> phrase2key;

    @SerializedName("data_by_positions")
    List<PhraseForecastPositions> phraseForecastByPositionList;

    public HashMap<String, String> getPhrase2key() {
        return phrase2key;
    }

    public void setPhrase2key(HashMap<String, String> phrase2key) {
        this.phrase2key = phrase2key;
    }

    public List<PhraseForecastPositions> getPhraseForecastByPositionList() {
        return phraseForecastByPositionList;
    }

    public void setPhraseForecastByPositionList(List<PhraseForecastPositions> phraseForecastByPositionList) {
        this.phraseForecastByPositionList = phraseForecastByPositionList;
    }

    public PhraseForecastPositions getPhraseForecastPositionsByPhrase(String phrase) {
        return phraseForecastByPositionList.stream().filter(t -> t.getMd5().equals(getPhraseKey(phrase))).findFirst().orElse(null);
    }

    public List<String> getPhrases() {
        return new ArrayList<String>(phrase2key.keySet());
    }

    public String getPhraseKey(String phrase) {
        return phrase2key.get(phrase);
    }

    public GetForecastInfo getForecastInfo() {
        int phraseCount = phrase2key.size();
        int positionCount = 5;
        GetForecastInfo getForecastInfo = new GetForecastInfo();
        BannerPhraseInfo[] bannerPhraseInfos = new BannerPhraseInfo[phraseCount];

        for (int i = 0; i < phraseCount; i++) {
            BannerPhraseInfo bannerPhraseInfo = new BannerPhraseInfo();
            String phrase = getPhrases().get(i);
            PhraseAuctionBids[] phraseAuctionBids = new PhraseAuctionBids[positionCount];
            PhraseForecastPositions currentPhrasePositions = getPhraseForecastPositionsByPhrase(phrase);
            bannerPhraseInfo.setPhrase(phrase);
            bannerPhraseInfo.setClicks(currentPhrasePositions.getYandexStdPosition().getClicks());
            bannerPhraseInfo.setFirstPlaceClicks(currentPhrasePositions
                    .getYandexFirstPlacePosition().getClicks());
            bannerPhraseInfo.setPremiumClicks(currentPhrasePositions
                    .getYandexFirstPremiumPosition().getClicks());

            for (PhrasePositionEnum positionEnum : PhrasePositionEnum.values()) {
                PhraseAuctionBids phraseAuctionBid = new PhraseAuctionBids();
                phraseAuctionBid.setPosition(positionEnum.getPositionIndex());
                switch (positionEnum) {
                    case FIRST_PREMIUM:
                        phraseAuctionBid.setBid(currentPhrasePositions.getYandexFirstPremiumPosition().getBidPrice());
                        phraseAuctionBid.setPrice(currentPhrasePositions.getYandexFirstPremiumPosition().getAmnestyPrice());
                        ;
                        break;
                    case FISRT_PLACE:
                        phraseAuctionBid.setBid(currentPhrasePositions.getYandexFirstPlacePosition().getBidPrice());
                        phraseAuctionBid.setPrice(currentPhrasePositions.getYandexFirstPlacePosition().getAmnestyPrice());
                        break;
                    case PREMIUM:
                        phraseAuctionBid.setBid(currentPhrasePositions.getYandexPremiumPosition().getBidPrice());
                        phraseAuctionBid.setPrice(currentPhrasePositions.getYandexPremiumPosition().getAmnestyPrice());
                        break;
                    case SECOND_PREMIUM:
                        phraseAuctionBid.setBid(currentPhrasePositions.getYandexSecondPremiumPosition().getBidPrice());
                        phraseAuctionBid.setPrice(currentPhrasePositions.getYandexSecondPremiumPosition().getAmnestyPrice());
                        break;
                    case STD:
                        phraseAuctionBid.setBid(currentPhrasePositions.getYandexStdPosition().getBidPrice());
                        phraseAuctionBid.setPrice(currentPhrasePositions.getYandexStdPosition().getAmnestyPrice());
                        break;
                }
                phraseAuctionBids[positionEnum.ordinal()] = phraseAuctionBid;
            }
            bannerPhraseInfo.setAuctionBids(phraseAuctionBids);
            bannerPhraseInfos[i] = bannerPhraseInfo;
        }
        getForecastInfo.setPhrases(bannerPhraseInfos);
        return getForecastInfo;
    }

    public String toJson() {
        return new Gson().toJson(this, AjaxDataForBudgetForecastResponse.class);
    }

    public String toString() {
        return toJson();
    }
}

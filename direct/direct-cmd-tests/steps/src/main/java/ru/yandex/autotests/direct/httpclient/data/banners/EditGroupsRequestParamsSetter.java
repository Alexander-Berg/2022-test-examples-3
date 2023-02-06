package ru.yandex.autotests.direct.httpclient.data.banners;

import com.google.gson.JsonObject;

import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.directapi.common.api45.BannerInfo;

/**
 * Created by shmykov on 04.03.15.
 */
public class EditGroupsRequestParamsSetter {


    public static void setJsonGroupParamsFromBanners(JsonObject requestJsonGroup, BannerInfo... banners) {

        requestJsonGroup.addProperty("adgroup_id", banners[0].getAdGroupID());
        requestJsonGroup.addProperty("bid", banners[0].getBannerID());

        for (int bInd = 0; bInd < banners.length; bInd++) {

            JsonObject banner = requestJsonGroup.get("banners").getAsJsonArray().get(bInd).getAsJsonObject();
            banner.addProperty("bid", banners[bInd].getBannerID());

            for (int pInd = 0; pInd < banners[bInd].getPhrases().length; pInd++) {

                JsonObject phrase = requestJsonGroup.get("phrases").getAsJsonArray().get(pInd).getAsJsonObject();
                phrase.addProperty("phrase", banners[bInd].getPhrases()[pInd].getPhrase());
                phrase.addProperty("id", banners[bInd].getPhrases()[pInd].getPhraseID());
            }
        }
    }

    public static void setJsonGroupParamsFromBanners(JsonObject requestJsonGroup, Group group) {

        requestJsonGroup.addProperty("adgroup_id", group.getAdGroupID());
        requestJsonGroup.addProperty("bid", group.getBanners().get(0).getBid());

        for (int bInd = 0; bInd < group.getBanners().size(); bInd++) {

            JsonObject banner = requestJsonGroup.get("banners").getAsJsonArray().get(bInd).getAsJsonObject();
            banner.addProperty("bid", group.getBanners().get(bInd).getBid());

            for (int pInd = 0; pInd < group.getPhrases().size(); pInd++) {

                JsonObject phrase = requestJsonGroup.get("phrases").getAsJsonArray().get(pInd).getAsJsonObject();
                phrase.addProperty("phrase", group.getPhrases().get(pInd).getPhrase());
                phrase.addProperty("id", group.getPhrases().get(pInd).getId());
            }
        }
    }
}

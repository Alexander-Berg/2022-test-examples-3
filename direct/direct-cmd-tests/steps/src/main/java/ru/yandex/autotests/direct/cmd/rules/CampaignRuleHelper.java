package ru.yandex.autotests.direct.cmd.rules;

import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.MobileContentOsType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.MobileAppsRecord;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.MobileContentRecord;
import ru.yandex.autotests.direct.db.steps.MobileAppSteps;

import static ru.yandex.autotests.direct.httpclient.TestEnvironment.newDbSteps;
import static ru.yandex.autotests.directapi.darkside.model.MobileContentUtils.getDefaultMobileContent;

class CampaignRuleHelper {

    private CampaignRuleHelper() {
    }

    static MobileAppData createDefaultMobileApp(String uLogin) {
        Long clientId = newDbSteps(uLogin).usersSteps().getUser(uLogin).getClientid();

        MobileContentRecord mobileContentRecord = getDefaultMobileContent(clientId.toString());

        // здесь костыль: getDefaultMobileContent на момент написания создавал невозможную запись
        // в mobile_content с os_type = ios, content_id = ru.yandex.<...>
        mobileContentRecord.setOsType(MobileContentOsType.Android);

        Long mobileContentId = newDbSteps(uLogin).mobileContentSteps()
                .saveMobileContent(mobileContentRecord);

        // и здесь костыль: полагаемся на то, что getDefaultMobileContent делает приложение в US
        // (а что он делает android, мы сами подкостылили выше)
        String storeContentHref = "https://play.google.com/store/apps/details?id=" +
                mobileContentRecord.getStoreContentId() + "&gl=US";

        MobileAppsRecord mobileAppsRecord = MobileAppSteps.getDefaultMobileAppsRecord(clientId, mobileContentId);
        mobileAppsRecord.setStoreHref(storeContentHref);

        Long mobileAppId = newDbSteps(uLogin).mobileAppsSteps()
                .createMobileApp(mobileAppsRecord);

        return new MobileAppData()
                .withMobileContentId(mobileContentId)
                .withMobileAppId(mobileAppId)
                .withStoreContentHref(storeContentHref);
    }

    static void clearMobileApp(MobileAppData appData, String uLogin) {
        if (appData == null) {
            return;
        }

        newDbSteps(uLogin).mobileAppsSteps().deleteMobileApp(appData.getMobileAppId());
        newDbSteps(uLogin).mobileContentSteps().deleteMobileContent(appData.getMobileContentId());
    }

    static class MobileAppData {
        private Long mobileContentId;
        private Long mobileAppId;
        private String storeContentHref;

        Long getMobileContentId() {
            return mobileContentId;
        }

        MobileAppData withMobileContentId(Long mobileContentId) {
            this.mobileContentId = mobileContentId;
            return this;
        }

        Long getMobileAppId() {
            return mobileAppId;
        }

        MobileAppData withMobileAppId(Long mobileAppId) {
            this.mobileAppId = mobileAppId;
            return this;
        }

        String getStoreContentHref() {
            return storeContentHref;
        }

        MobileAppData withStoreContentHref(String storeContentHref) {
            this.storeContentHref = storeContentHref;
            return this;
        }
    }
}

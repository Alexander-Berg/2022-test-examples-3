package ru.yandex.market.crm.core.test.utils;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ru.yandex.inside.yt.kosher.cypress.YPath;

/**
 * Пути которые подгружаются из конфигов, нужны в тестах, но в
 * продакшн-коде они лишние
 *
 * @author apershukov
 */
@Component
public class YtTestTables {

    private final YPath passportProfiles;
    private final YPath modelInfo;
    private final YPath modelStat;
    private final YPath emailOwnership;
    private final YPath metrikaMobileAppFacts;
    private final YPath mobileAppInfoFacts;
    private final YPath genericSubscriptionFacts;
    private final YPath plusDataTablePath;
    private final YPath fapiAccessDataTablePath;
    private final YPath capiAccessDataTablePath;
    private final YPath cryptaMatchingDir;
    private final YPath executedActions;
    private final YPath chytPassportEmails;
    private final YPath chytPassportUuids;
    private final YPath chytUuidsWithTokens;
    private final YPath platformPuidToEmail;
    private final YPath emailsGeoInfo;
    private final YPath chytUuidsWithSubscriptions;

    public YtTestTables(@Value("${var.passport_userdata_dir}") String passportProfilesDir,
                        @Value("${var.table_models_info}") String modelInfo,
                        @Value("${var.table_models_stat}") String modelStat,
                        @Value("${var.platform_email_ownership}") String emailOwnership,
                        @Value("${var.platform_metrika_mobile_app}") String metrikaMobileAppFacts,
                        @Value("${var.platform_mobile_app_info}") String mobileAppInfoFacts,
                        @Value("${var.ya_plus_profiles_last}") String plusDataTable,
                        @Value("${var.fapi_access_segment_data_dir}") String fapiAccessDataDir,
                        @Value("${var.capi_access_segment_data_dir}") String capiAccessDataDir,
                        @Value("${var.crypta_matching_by_id_dir}") String cryptaMatchingDir,
                        @Value("${var.platform_executed_actions}") String executedActions,
                        @Value("${var.chyt_passport_emails}") String chytPassportEmails,
                        @Value("${var.chyt_passport_uuids}") String chytPassportUuids,
                        @Value("${var.platform_puid_to_email}") String platformPuidToEmail,
                        @Value("${var.users_emails_geo_info}") String usersEmailsGeoInfo,
                        @Value("${var.chyt_uuids_with_tokens}") String chytUuidsWithTokens,
                        @Value("${var.chyt_uuids_with_subscriptions}") String chytUuidsWithSubscriptions,
                        @Value("${var.platform_generic_subscription}") String genericSubscriptionFacts) {
        this.passportProfiles = YPath.simple(passportProfilesDir).child(LocalDate.now().toString());
        this.modelInfo = YPath.simple(modelInfo);
        this.modelStat = YPath.simple(modelStat);
        this.emailOwnership = YPath.simple(emailOwnership);
        this.metrikaMobileAppFacts = YPath.simple(metrikaMobileAppFacts);
        this.mobileAppInfoFacts = YPath.simple(mobileAppInfoFacts);
        this.plusDataTablePath = YPath.simple(plusDataTable);
        this.fapiAccessDataTablePath = YPath.simple(fapiAccessDataDir).child(LocalDate.now().toString());
        this.capiAccessDataTablePath = YPath.simple(capiAccessDataDir).child(LocalDate.now().toString());
        this.cryptaMatchingDir = YPath.simple(cryptaMatchingDir);
        this.executedActions = YPath.simple(executedActions);
        this.chytPassportEmails = YPath.simple(chytPassportEmails);
        this.chytPassportUuids = YPath.simple(chytPassportUuids);
        this.chytUuidsWithTokens = YPath.simple(chytUuidsWithTokens);
        this.chytUuidsWithSubscriptions = YPath.simple(chytUuidsWithSubscriptions);
        this.platformPuidToEmail = YPath.simple(platformPuidToEmail);
        this.emailsGeoInfo = YPath.simple(usersEmailsGeoInfo);
        this.genericSubscriptionFacts = YPath.simple(genericSubscriptionFacts);
    }

    public YPath getModelInfo() {
        return modelInfo;
    }

    public YPath getModelStat() {
        return modelStat;
    }

    YPath getPassportProfiles() {
        return passportProfiles;
    }

    public YPath getEmailOwnership() {
        return emailOwnership;
    }

    public YPath getMertikaAppFacts() {
        return metrikaMobileAppFacts;
    }

    public YPath getMobileAppInfoFacts() {
        return mobileAppInfoFacts;
    }

    public YPath getGenericSubscriptionFacts() {
        return genericSubscriptionFacts;
    }

    public YPath getPlusDataTablePath() {
        return plusDataTablePath;
    }

    public YPath getFapiAccessDataTablePath() {
        return fapiAccessDataTablePath;
    }

    public YPath getCapiAccessDataTablePath() {
        return capiAccessDataTablePath;
    }

    public YPath getCryptaMatchingDir() {
        return cryptaMatchingDir;
    }

    public YPath getExecutedActions() {
        return executedActions;
    }

    public YPath getChytPassportEmails() {
        return chytPassportEmails;
    }

    public YPath getChytPassportUuids() {
        return chytPassportUuids;
    }

    public YPath getChytUuidsWithSubscriptions() {
        return chytUuidsWithSubscriptions;
    }

    public YPath getPlatformPuidToEmail() {
        return platformPuidToEmail;
    }

    public YPath getEmailsGeoInfo() {
        return emailsGeoInfo;
    }

    public YPath getChytUuidsWithTokens() {
        return chytUuidsWithTokens;
    }
}

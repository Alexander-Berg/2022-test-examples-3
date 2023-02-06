package ru.yandex.direct.core.testing.steps.campaign.repository0;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignsDayBudgetShowMode;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.testing.steps.campaign.model0.BroadmatchFlag;
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate;
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusPostModerate;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.DayBudgetShowMode;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbschema.ppc.enums.CampOptionsBroadMatchFlag;
import ru.yandex.direct.dbschema.ppc.enums.CampOptionsStatuspostmoderate;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsArchived;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsCurrency;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsPaidByCertificate;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStatusactive;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStatusbssynced;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStatusempty;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStatusmoderate;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStatusnopay;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStatusshow;
import ru.yandex.direct.utils.JsonUtils;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static ru.yandex.direct.utils.JsonUtils.fromJson;
import static ru.yandex.direct.utils.JsonUtils.toJson;

public class CampaignMappings {

    private static final String METRIKA_COUNTERS_DELIMITER = ",";

    public static CampaignsCurrency currencyCodeToDb(CurrencyCode currencyCode) {
        return currencyCode != null ? CampaignsCurrency.valueOf(currencyCode.name()) : null;
    }

    public static CurrencyCode currencyCodeFromDb(CampaignsCurrency currency) {
        return currency != null ? CurrencyCode.valueOf(currency.name()) : null;
    }

    public static CampaignsDayBudgetShowMode dayBudgetShowModeToDb(DayBudgetShowMode mode) {
        if (mode == null) {
            return null;
        }
        if (mode == DayBudgetShowMode.DEFAULT) {
            return CampaignsDayBudgetShowMode.DEFAULT_;
        }
        return CampaignsDayBudgetShowMode.valueOf(mode.name().toUpperCase());
    }

    public static DayBudgetShowMode dayBudgetShowModeFromDb(CampaignsDayBudgetShowMode mode) {
        if (mode == null) {
            return null;
        }
        if (mode == CampaignsDayBudgetShowMode.DEFAULT_) {
            return DayBudgetShowMode.DEFAULT;
        }
        return DayBudgetShowMode.valueOf(mode.name().toUpperCase());
    }

    public static CampaignsPaidByCertificate paidByCertificateToDb(Boolean paid) {
        return paid != null ? (paid ? CampaignsPaidByCertificate.Yes : CampaignsPaidByCertificate.No) : null;
    }

    public static Boolean paidByCertificateFromDb(CampaignsPaidByCertificate paid) {
        return paid != null ? paid == CampaignsPaidByCertificate.Yes : null;
    }

    public static CampaignsStatusbssynced statusBsSyncedToDb(StatusBsSynced status) {
        return status != null ? CampaignsStatusbssynced.valueOf(capitalize(status.name().toLowerCase())) : null;
    }

    public static StatusBsSynced statusBsSyncedFromDb(CampaignsStatusbssynced status) {
        return status != null ? StatusBsSynced.valueOf(status.name().toUpperCase()) : null;
    }

    public static CampaignsStatusactive statusActiveToDb(Boolean statusActive) {
        return statusActive != null ? (statusActive ? CampaignsStatusactive.Yes : CampaignsStatusactive.No) : null;
    }

    public static Boolean statusActiveFromDb(CampaignsStatusactive campaignsStatusactive) {
        return campaignsStatusactive != null ? (campaignsStatusactive == CampaignsStatusactive.Yes) : null;
    }

    public static CampaignsStatusempty statusEmptyToDb(Boolean statusEmpty) {
        return statusEmpty != null ? (statusEmpty ? CampaignsStatusempty.Yes : CampaignsStatusempty.No) : null;
    }

    public static Boolean statusEmptyFromDb(CampaignsStatusempty campaignsStatusempty) {
        return campaignsStatusempty != null ? (campaignsStatusempty == CampaignsStatusempty.Yes) : null;
    }

    public static CampaignsStatusmoderate statusModerateToDb(StatusModerate status) {
        return status != null ? CampaignsStatusmoderate.valueOf(capitalize(status.name().toLowerCase())) : null;
    }

    public static StatusModerate statusModerateFromDb(CampaignsStatusmoderate status) {
        return status != null ? StatusModerate.valueOf(status.name().toUpperCase()) : null;
    }

    public static CampaignsStatusnopay statusNoPayToDb(Boolean status) {
        return status != null ? (status ? CampaignsStatusnopay.Yes : CampaignsStatusnopay.No) : null;
    }

    public static Boolean statusNoPayFromDb(CampaignsStatusnopay status) {
        return status != null ? status == CampaignsStatusnopay.Yes : null;
    }

    public static CampOptionsStatuspostmoderate statusPostModerateToDb(StatusPostModerate status) {
        return status != null ? CampOptionsStatuspostmoderate.valueOf(capitalize(status.name().toLowerCase())) : null;
    }

    public static StatusPostModerate statusPostModerateFromDb(CampOptionsStatuspostmoderate status) {
        return status != null ? StatusPostModerate.valueOf(status.name().toUpperCase()) : null;
    }

    public static CampaignsStatusshow statusShowToDb(Boolean statusShow) {
        return statusShow != null ? (statusShow ? CampaignsStatusshow.Yes : CampaignsStatusshow.No) : null;
    }

    public static Boolean statusShowFromDb(CampaignsStatusshow campaignsStatusshow) {
        return campaignsStatusshow != null ? (campaignsStatusshow == CampaignsStatusshow.Yes) : null;
    }

    public static CampaignsArchived archivedToDb(Boolean archived) {
        return archived != null ? (archived ? CampaignsArchived.Yes : CampaignsArchived.No) : null;
    }

    public static Boolean archivedFromDb(CampaignsArchived campaignsArchived) {
        return campaignsArchived != null ? (campaignsArchived == CampaignsArchived.Yes) : null;
    }

    public static String strategyDataToDb(StrategyData strategyData) {
        return strategyData != null ? toJson(strategyData) : null;
    }

    public static StrategyData strategyDataFromDb(Object strategyData) {
        return strategyData != null ? fromJson((String) (strategyData), StrategyData.class) : null;
    }

    public static List<Long> metrikaCountersFromDb(String counters) {
        return ru.yandex.direct.core.entity.campaign.repository.CampaignMappings.metrikaCountersFromDbToListOfCounters(counters);
    }

    public static String metrikaCountersToDb(List<Long> counters) {
        return ru.yandex.direct.core.entity.campaign.repository.CampaignMappings.metrikaCountersToDb(counters);
    }

    public static CampOptionsBroadMatchFlag broadMatchFlagToDb(BroadmatchFlag flag) {
        return flag != null ? CampOptionsBroadMatchFlag.valueOf(capitalize(flag.name().toLowerCase())) : null;
    }

    public static BroadmatchFlag broadMatchFlagFromDb(CampOptionsBroadMatchFlag flag) {
        return flag != null ? BroadmatchFlag.valueOf(flag.name().toUpperCase()) : null;
    }

    @Nullable
    public static String stringCollectionToDb(@Nullable Collection<String> domains) {
        return domains == null || domains.isEmpty() ? null : domains.stream()
                .sorted()
                .collect(joining(","));
    }

    @Nullable
    public static Set<String> stringSetFromDb(@Nullable String domains) {
        return domains == null ? null : Arrays.stream(domains.split(","))
                .collect(toSet());
    }

    @Nullable
    public static String stringListToJson(@Nullable List<String> disabledVideoPlacements) {
        return JsonUtils.toJsonNullable(disabledVideoPlacements);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static List<String> stringListFromJson(@Nullable String disabledVideoPlacementsJson) {
        return org.apache.commons.lang3.StringUtils.isNotEmpty(disabledVideoPlacementsJson) ? JsonUtils
                .fromJson(disabledVideoPlacementsJson, List.class) : null;
    }

    public static Long orderIdToDb(@Nullable Long orderId) {
        return orderId == null ? 0 : orderId;
    }

    public static Long orderIdFromDb(Long orderId) {
        return orderId == 0 ? null : orderId;
    }

    @Nullable
    public static Set<Integer> geoFromDb(@Nullable String geo) {
        return geo == null ? null : Arrays.stream(geo.split(","))
                .map(Integer::valueOf)
                .collect(toSet());
    }

    @Nullable
    public static String geoToDb(@Nullable Set<Integer> geo) {
        return geo == null ? null : geo.stream()
                .map(Object::toString)
                .collect(joining(","));
    }
}

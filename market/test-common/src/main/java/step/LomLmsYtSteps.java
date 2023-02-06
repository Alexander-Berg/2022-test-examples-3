package step;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import client.LomLmsYtClient;
import io.qameta.allure.Step;
import toolkit.Retrier;

import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.request.schedule.LogisticSegmentInboundScheduleFilter;
import ru.yandex.market.logistics.management.entity.request.settings.SettingsMethodFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParamGroup;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodDto;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;

/**
 * Обращения к данным LMS в yt LOM.
 */
public class LomLmsYtSteps {

    private static final LomLmsYtClient LOM_LMS_YT_CLIENT = new LomLmsYtClient();

    @Nullable
    @Step("Получение логистической точки по идентификатору")
    public LogisticsPointResponse getLogisticsPoint(Long logisticsPointId) {
        return Retrier.clientRetry(
            () -> LOM_LMS_YT_CLIENT.getLogisticsPoint(logisticsPointId)
        );
    }

    @Nonnull
    @Step("Получение логистических точкек по идентификатору")
    public List<LogisticsPointResponse> getLogisticsPoints(LogisticsPointFilter filter) {
        return Retrier.clientRetry(
            () -> LOM_LMS_YT_CLIENT.getLogisticsPoints(filter)
        );
    }

    @Nullable
    @Step("Получение интервала доставки по идентификатору")
    public ScheduleDayResponse getScheduleDayById(Long scheduleDayId) {
        return Retrier.clientRetry(
            () -> LOM_LMS_YT_CLIENT.getScheduleDayById(scheduleDayId)
        );
    }

    @Nonnull
    @Step("Получение параметров партнеров по типам параметров")
    public List<PartnerExternalParamGroup> getPartnerExternalParamValues(
        Set<PartnerExternalParamType> paramTypes
    ) {
        return Retrier.clientRetry(
            () -> LOM_LMS_YT_CLIENT.getPartnerExternalParamValues(paramTypes)
        );
    }

    @Nonnull
    @Step("Получить партнера по id из yt")
    public PartnerResponse getPartner(Long id) {
        return Retrier.clientRetry(
            () -> LOM_LMS_YT_CLIENT.getPartner(id)
        );
    }

    @Nonnull
    @Step("Получить партнеров по нескольким id из yt")
    public List<PartnerResponse> getPartners(Set<Long> ids) {
        return Retrier.clientRetry(
            () -> LOM_LMS_YT_CLIENT.getPartners(ids)
        );
    }

    @Nonnull
    @Step("Получение методов партнеров по фильтру из yt")
    public List<SettingsMethodDto> searchPartnerSettingsMethods(SettingsMethodFilter filter) {
        return Retrier.clientRetry(
            () -> LOM_LMS_YT_CLIENT.searchPartnerSettingsMethods(filter)
        );
    }

    @Nonnull
    @Step("Получить расписание заборов по фильтру из yt")
    public List<ScheduleDayResponse> searchInboundSchedule(LogisticSegmentInboundScheduleFilter filter) {
        return Retrier.clientRetry(
            () -> LOM_LMS_YT_CLIENT.searchInboundSchedule(filter)
        );
    }

    @Nonnull
    @Step("Получение связок партнеров с катоффами по фильтру из yt")
    public List<PartnerRelationEntityDto> searchPartnerRelationWithCutoffs(PartnerRelationFilter filter) {
        return Retrier.clientRetry(
            () -> LOM_LMS_YT_CLIENT.searchPartnerRelationWithCutoffs(filter)
        );
    }

    @Nonnull
    @Step("Получение связок партнеров с возвратными партнерами по фильтру из yt")
    public List<PartnerRelationEntityDto> searchPartnerRelationWithReturnPartners(PartnerRelationFilter filter) {
        return Retrier.clientRetry(
            () -> LOM_LMS_YT_CLIENT.searchPartnerRelationWithReturnPartners(filter)
        );
    }

    @Nonnull
    @Step("Получение актуальной версии данных")
    public Instant getActualDataVersion() {
        return Retrier.clientRetry(
            LOM_LMS_YT_CLIENT::getActualDataVersion
        );
    }
}

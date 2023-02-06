package step;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import client.LomRedisClient;
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

public class LomRedisSteps {

    private static final LomRedisClient LOM_REDIS_CLIENT = new LomRedisClient();
    private static final int REDIS_RETRIES_AMOUNT = 5;

    @Nonnull
    @Step("Получить логистическую точку по id из редиса")
    public LogisticsPointResponse getLogisticsPointFromRedis(Long id) {
        return Retrier.retry(
            () -> LOM_REDIS_CLIENT.getLogisticsPointFromRedis(id),
            REDIS_RETRIES_AMOUNT
        );
    }

    @Nonnull
    @Step("Получить логистические точки по фильтру из редиса")
    public List<LogisticsPointResponse> getLogisticsPointsFromRedis(LogisticsPointFilter filter) {
        return Retrier.retry(
            () -> LOM_REDIS_CLIENT.getLogisticsPointsFromRedis(filter),
            REDIS_RETRIES_AMOUNT
        );
    }

    @Nonnull
    @Step("Получить партнера по id из редиса")
    public PartnerResponse getPartnerFromRedis(Long id) {
        return Retrier.retry(
            () -> LOM_REDIS_CLIENT.getPartnerFromRedis(id),
            REDIS_RETRIES_AMOUNT
        );
    }

    @Nonnull
    @Step("Получить партнеров по нескольким id из редиса")
    public List<PartnerResponse> getPartnersFromRedis(Set<Long> ids) {
        return Retrier.retry(
            () -> LOM_REDIS_CLIENT.getPartnersFromRedis(ids),
            REDIS_RETRIES_AMOUNT
        );
    }

    @Nonnull
    @Step("Получить расписание заборов по фильтру из редиса")
    public List<ScheduleDayResponse> searchInboundSchedule(LogisticSegmentInboundScheduleFilter filter) {
        return Retrier.retry(
            () -> LOM_REDIS_CLIENT.searchInboundSchedule(filter),
            REDIS_RETRIES_AMOUNT
        );
    }

    @Nonnull
    @Step("Получение связки партнеров с катоффами по фильтру из редиса")
    public List<PartnerRelationEntityDto> searchPartnerRelationWithCutoffsFromRedis(PartnerRelationFilter filter) {
        return Retrier.retry(
            () -> LOM_REDIS_CLIENT.searchPartnerRelationWithCutoffsFromRedis(filter),
            REDIS_RETRIES_AMOUNT
        );
    }

    @Nonnull
    @Step("Получение связки с возвратными партнерами по фильтру из редиса")
    public List<PartnerRelationEntityDto> searchPartnerRelationWithReturnPartnersFromRedis(
        PartnerRelationFilter filter
    ) {
        return Retrier.retry(
            () -> LOM_REDIS_CLIENT.searchPartnerRelationWithReturnPartnersFromRedis(filter),
            REDIS_RETRIES_AMOUNT
        );
    }

    @Nonnull
    @Step("Получение параметров партнеров по типу из редиса")
    public List<PartnerExternalParamGroup> getPartnerExternalParamValues(
        Set<PartnerExternalParamType> paramTypes
    ) {
        return Retrier.retry(
            () -> LOM_REDIS_CLIENT.getPartnerExternalParamValues(paramTypes),
            REDIS_RETRIES_AMOUNT
        );
    }

    @Nonnull
    @Step("Получение методов партнеров по фильтру из редиса")
    public List<SettingsMethodDto> searchPartnerSettingsMethods(SettingsMethodFilter filter) {
        return Retrier.retry(
            () -> LOM_REDIS_CLIENT.searchPartnerSettingsMethods(filter),
            REDIS_RETRIES_AMOUNT
        );
    }

    @Nonnull
    @Step("Получение актуальной версии данных")
    public Instant getActualDataVersion() {
        return Retrier.retry(
            LOM_REDIS_CLIENT::getActualDataVersion,
            REDIS_RETRIES_AMOUNT
        );
    }
}

package client;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import api.LomLmsYtApi;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

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

import static toolkit.Retrofits.RETROFIT_LMS;

@Resource.Classpath("delivery/lom.properties")
public class LomLmsYtClient {

    private final LomLmsYtApi lomLmsYtApi;
    @Property("lom.host")
    private String host;

    public LomLmsYtClient() {
        PropertyLoader.newInstance().populate(this);
        lomLmsYtApi = RETROFIT_LMS.getRetrofit(host).create(LomLmsYtApi.class);
    }

    @Nullable
    @SneakyThrows
    public LogisticsPointResponse getLogisticsPoint(Long logisticsPointId) {
        Response<LogisticsPointResponse> bodyResponse = lomLmsYtApi.getLogisticsPoint(logisticsPointId).execute();
        Assertions.assertTrue(
            bodyResponse.isSuccessful(),
            "Запрос получения логистической точки по id из yt неуспешен"
        );

        return bodyResponse.body();
    }

    @Nonnull
    @SneakyThrows
    public List<LogisticsPointResponse> getLogisticsPoints(LogisticsPointFilter filter) {
        Response<List<LogisticsPointResponse>> bodyResponse = lomLmsYtApi.getLogisticsPoints(filter).execute();
        Assertions.assertTrue(
            bodyResponse.isSuccessful(),
            "Запрос получения логистических точек по фильтру из yt неуспешен"
        );
        Assertions.assertNotNull(
            bodyResponse.body(),
            "Пустое тело ответа при получении логистических точек по фильтру из yt"
        );

        return bodyResponse.body();
    }

    @Nullable
    @SneakyThrows
    public ScheduleDayResponse getScheduleDayById(Long scheduleDayId) {
        Response<ScheduleDayResponse> bodyResponse = lomLmsYtApi.getScheduleDayById(scheduleDayId).execute();
        Assertions.assertTrue(
            bodyResponse.isSuccessful(),
            "Запрос получения интервала доставки по id из yt неуспешен"
        );

        return bodyResponse.body();
    }

    @Nonnull
    @SneakyThrows
    public List<PartnerExternalParamGroup> getPartnerExternalParamValues(
        Set<PartnerExternalParamType> paramTypes
    ) {
        Response<List<PartnerExternalParamGroup>> bodyResponse =
            lomLmsYtApi.getPartnerExternalParamValues(paramTypes).execute();

        Assertions.assertTrue(
            bodyResponse.isSuccessful(),
            "Запрос поиска параметров партнеров по типу из yt неуспешен"
        );
        Assertions.assertNotNull(
            bodyResponse.body(),
            "Пустое тело ответа при поиске параметров партнеров по типу из yt"
        );
        return bodyResponse.body();
    }

    @SneakyThrows
    public PartnerResponse getPartner(Long id) {
        Response<PartnerResponse> bodyResponse = lomLmsYtApi.getPartner(id).execute();
        Assertions.assertTrue(bodyResponse.isSuccessful(), "Запрос получения партнера по id из yt неуспешен");
        Assertions.assertNotNull(bodyResponse.body(), "Пустое тело ответа получения партнера по id из yt");
        return bodyResponse.body();
    }

    @SneakyThrows
    public List<PartnerResponse> getPartners(Set<Long> ids) {
        Response<List<PartnerResponse>> bodyResponse = lomLmsYtApi.getPartners(ids).execute();
        Assertions.assertTrue(
            bodyResponse.isSuccessful(),
            "Запрос получения партнеров по нескольким id из yt неуспешен"
        );
        Assertions.assertNotNull(
            bodyResponse.body(),
            "Пустое тело ответа получения партнеров по нескольким id из yt"
        );
        return bodyResponse.body();
    }

    @SneakyThrows
    public List<SettingsMethodDto> searchPartnerSettingsMethods(SettingsMethodFilter filter) {
        Response<List<SettingsMethodDto>> bodyResponse = lomLmsYtApi.searchPartnerSettingsMethods(filter).execute();

        Assertions.assertTrue(
            bodyResponse.isSuccessful(),
            "Запрос поиска методов партнеров по фильтру из yt неуспешен"
        );
        Assertions.assertNotNull(
            bodyResponse.body(),
            "Пустое тело ответа при поиске методов партнеров по фильтру из yt"
        );
        return bodyResponse.body();
    }

    @SneakyThrows
    public List<ScheduleDayResponse> searchInboundSchedule(LogisticSegmentInboundScheduleFilter filter) {
        Response<List<ScheduleDayResponse>> bodyResponse = lomLmsYtApi.searchInboundSchedule(filter).execute();
        Assertions.assertTrue(
            bodyResponse.isSuccessful(),
            "Запрос получения расписания заборов по фильтру из yt неуспешен"
        );
        Assertions.assertNotNull(
            bodyResponse.body(),
            "Пустое тело ответа получения расписания заборов по фильтру из yt"
        );
        return bodyResponse.body();
    }

    @SneakyThrows
    public List<PartnerRelationEntityDto> searchPartnerRelationWithCutoffs(PartnerRelationFilter filter) {
        Response<List<PartnerRelationEntityDto>> bodyResponse =
            lomLmsYtApi.searchPartnerRelationWithCutoffs(filter).execute();

        Assertions.assertTrue(
            bodyResponse.isSuccessful(),
            "Запрос поиска связок партнеров с катоффами по фильтру из yt неуспешен"
        );
        Assertions.assertNotNull(
            bodyResponse.body(),
            "Пустое тело ответа при поиске связок партнеров с катоффами по фильтру из yt"
        );
        return bodyResponse.body();
    }

    @SneakyThrows
    public List<PartnerRelationEntityDto> searchPartnerRelationWithReturnPartners(PartnerRelationFilter filter) {
        Response<List<PartnerRelationEntityDto>> bodyResponse =
            lomLmsYtApi.searchPartnerRelationWithReturnPartners(filter).execute();

        Assertions.assertTrue(
            bodyResponse.isSuccessful(),
            "Запрос поиска связок партнеров с возвратными партнерами по фильтру из yt неуспешен"
        );
        Assertions.assertNotNull(
            bodyResponse.body(),
            "Пустое тело ответа при поиске связок партнеров с возвратными партнерами по фильтру из yt"
        );
        return bodyResponse.body();
    }

    @Nonnull
    @SneakyThrows
    public Instant getActualDataVersion() {
        Response<String> bodyResponse = lomLmsYtApi.getActualDataVersion().execute();

        Assertions.assertTrue(
            bodyResponse.isSuccessful(),
            "Запрос получения текущей актуальной версии данных в yt неуспешен"
        );
        Assertions.assertNotNull(
            bodyResponse.body(),
            "Пустое тело ответа при получении актуальной версии данных в yt"
        );

        return Instant.parse(bodyResponse.body());
    }
}

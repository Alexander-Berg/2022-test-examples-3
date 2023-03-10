package client;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import api.LomRedisApi;
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
public class LomRedisClient {
    private final LomRedisApi lomRedisApi;
    @Property("lom.host")
    private String host;

    public LomRedisClient() {
        PropertyLoader.newInstance().populate(this);
        lomRedisApi = RETROFIT_LMS.getRetrofit(host).create(LomRedisApi.class);
    }

    @SneakyThrows
    public LogisticsPointResponse getLogisticsPointFromRedis(Long id) {
        Response<LogisticsPointResponse> bodyResponse = lomRedisApi.getLogisticsPointFromRedis(id).execute();
        Assertions.assertTrue(
            bodyResponse.isSuccessful(),
            "???????????? ?????????????????? ?????????????????????????? ?????????? ???? id ???? ???????????? ??????????????????"
        );
        Assertions.assertNotNull(
            bodyResponse.body(),
            "???????????? ???????? ???????????? ?????????????????? ?????????????????????????? ?????????? ???? id ???? ????????????"
        );
        return bodyResponse.body();
    }

    @SneakyThrows
    public List<LogisticsPointResponse> getLogisticsPointsFromRedis(LogisticsPointFilter filter) {
        Response<List<LogisticsPointResponse>> bodyResponse = lomRedisApi.getLogisticsPointsFromRedis(filter).execute();
        Assertions.assertTrue(
            bodyResponse.isSuccessful(),
            "???????????? ?????????????????? ?????????????????????????? ?????????? ???? ?????????????? ???? ???????????? ??????????????????"
        );
        Assertions.assertNotNull(
            bodyResponse.body(),
            "???????????? ???????? ???????????? ?????????????????? ?????????????????????????? ?????????? ???? ?????????????? ???? ????????????"
        );
        return bodyResponse.body();
    }

    @SneakyThrows
    public PartnerResponse getPartnerFromRedis(Long id) {
        Response<PartnerResponse> bodyResponse = lomRedisApi.getPartnerFromRedis(id).execute();
        Assertions.assertTrue(bodyResponse.isSuccessful(), "???????????? ?????????????????? ???????????????? ???? id ???? ???????????? ??????????????????");
        Assertions.assertNotNull(bodyResponse.body(), "???????????? ???????? ???????????? ?????????????????? ???????????????? ???? id ???? ????????????");
        return bodyResponse.body();
    }

    @SneakyThrows
    public List<PartnerResponse> getPartnersFromRedis(Set<Long> ids) {
        Response<List<PartnerResponse>> bodyResponse = lomRedisApi.getPartnersFromRedis(ids).execute();
        Assertions.assertTrue(
            bodyResponse.isSuccessful(),
            "???????????? ?????????????????? ?????????????????? ???? ???????????????????? id ???? ???????????? ??????????????????"
        );
        Assertions.assertNotNull(
            bodyResponse.body(),
            "???????????? ???????? ???????????? ?????????????????? ?????????????????? ???? ???????????????????? id ???? ????????????"
        );
        return bodyResponse.body();
    }

    @SneakyThrows
    public List<ScheduleDayResponse> searchInboundSchedule(LogisticSegmentInboundScheduleFilter filter) {
        Response<List<ScheduleDayResponse>> bodyResponse = lomRedisApi.searchInboundSchedule(filter).execute();
        Assertions.assertTrue(
            bodyResponse.isSuccessful(),
            "???????????? ?????????????????? ???????????????????? ?????????????? ???? ?????????????? ???? ???????????? ??????????????????"
        );
        Assertions.assertNotNull(
            bodyResponse.body(),
            "???????????? ???????? ???????????? ?????????????????? ???????????????????? ?????????????? ???? ?????????????? ???? ????????????"
        );
        return bodyResponse.body();
    }

    @SneakyThrows
    public List<PartnerRelationEntityDto> searchPartnerRelationWithCutoffsFromRedis(PartnerRelationFilter filter) {
        Response<List<PartnerRelationEntityDto>> bodyResponse =
            lomRedisApi.searchPartnerRelationWithCutoffsFromRedis(filter).execute();

        Assertions.assertTrue(
            bodyResponse.isSuccessful(),
            "???????????? ???????????? ???????????? ?????????????????? ?? ?????????????????? ???? ?????????????? ???? ???????????? ??????????????????"
        );
        Assertions.assertNotNull(
            bodyResponse.body(),
            "???????????? ???????? ???????????? ?????? ???????????? ???????????? ?????????????????? ?? ?????????????????? ???? ?????????????? ???? ????????????"
        );
        return bodyResponse.body();
    }

    @SneakyThrows
    public List<PartnerRelationEntityDto> searchPartnerRelationWithReturnPartnersFromRedis(
        PartnerRelationFilter filter
    ) {
        Response<List<PartnerRelationEntityDto>> bodyResponse =
            lomRedisApi.searchPartnerRelationWithReturnPartnersFromRedis(filter).execute();

        Assertions.assertTrue(
            bodyResponse.isSuccessful(),
            "???????????? ???????????? ???????????? ?? ?????????????????????? ???????????????????? ???? ?????????????? ???? ???????????? ??????????????????"
        );
        Assertions.assertNotNull(
            bodyResponse.body(),
            "???????????? ???????? ???????????? ?????? ???????????? ???????????? ?? ?????????????????????? ???????????????????? ???? ?????????????? ???? ????????????"
        );
        return bodyResponse.body();
    }

    @SneakyThrows
    public List<PartnerExternalParamGroup> getPartnerExternalParamValues(
        Set<PartnerExternalParamType> paramTypes
    ) {
        Response<List<PartnerExternalParamGroup>> bodyResponse =
            lomRedisApi.getPartnerExternalParamValues(paramTypes).execute();

        Assertions.assertTrue(
            bodyResponse.isSuccessful(),
            "???????????? ???????????? ???????????????????? ?????????????????? ???? ???????? ???? ???????????? ??????????????????"
        );
        Assertions.assertNotNull(
            bodyResponse.body(),
            "???????????? ???????? ???????????? ?????? ???????????? ???????????????????? ?????????????????? ???? ???????? ???? ????????????"
        );
        return bodyResponse.body();
    }

    @SneakyThrows
    public List<SettingsMethodDto> searchPartnerSettingsMethods(SettingsMethodFilter filter) {
        Response<List<SettingsMethodDto>> bodyResponse = lomRedisApi.searchPartnerSettingsMethods(filter).execute();

        Assertions.assertTrue(
            bodyResponse.isSuccessful(),
            "???????????? ???????????? ?????????????? ?????????????????? ???? ?????????????? ???? ???????????? ??????????????????"
        );
        Assertions.assertNotNull(
            bodyResponse.body(),
            "???????????? ???????? ???????????? ?????? ???????????? ?????????????? ?????????????????? ???? ?????????????? ???? ????????????"
        );
        return bodyResponse.body();
    }

    @Nonnull
    @SneakyThrows
    public Instant getActualDataVersion() {
        Response<String> bodyResponse = lomRedisApi.getActualDataVersion().execute();

        Assertions.assertTrue(
            bodyResponse.isSuccessful(),
            "???????????? ?????????????????? ?????????????? ???????????????????? ???????????? ???????????? ?? redis ??????????????????"
        );
        Assertions.assertNotNull(
            bodyResponse.body(),
            "???????????? ???????? ???????????? ?????? ?????????????????? ???????????????????? ???????????? ???????????? ?? redis"
        );

        return Instant.parse(bodyResponse.body());
    }
}

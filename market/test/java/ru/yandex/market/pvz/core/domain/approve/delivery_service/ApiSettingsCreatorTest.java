package ru.yandex.market.pvz.core.domain.approve.delivery_service;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.settings.SettingsApiFilter;
import ru.yandex.market.logistics.management.entity.response.settings.SettingsApiDto;
import ru.yandex.market.logistics.management.entity.response.settings.SettingsApiUpdateDto;
import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodCreateDto;
import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodDto;
import ru.yandex.market.logistics.management.entity.type.ApiType;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;
import ru.yandex.market.tpl.common.util.TplRandom;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.management.entity.type.ApiType.DELIVERY;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.ApiSettingsCreator.CRON_TEMPLATE;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.ApiSettingsCreator.MethodOptions;
import static ru.yandex.market.pvz.core.domain.approve.delivery_service.ApiSettingsCreator.SETTINGS_ACTIVE;
import static ru.yandex.market.tpl.common.util.StringFormatter.sf;

@ExtendWith(SpringExtension.class)
class ApiSettingsCreatorTest {

    private static final long DELIVERY_SERVICE_ID = 48455L;
    private static final String TOKEN = "3cefef9b51354dc9a4a7f0fe4754c5c1534b754f087e4038a3f826563b24d76c";
    private static final ApiType API_TYPE = DELIVERY;
    private static final String VERSION = "1";
    private static final String FORMAT = "XML";
    private static final String CRON = sf(CRON_TEMPLATE, 0, 1);
    private static final long DEFAULT_SETTINGS_API_ID = 101;
    private static final MethodOptions CRON_METHOD = new MethodOptions("update", "url", true);
    private static final MethodOptions NON_CRON_METHOD = new MethodOptions("create", "url", false);

    private static final SettingsApiFilter SETTINGS_API_FILTER = SettingsApiFilter.newBuilder()
            .partnerIds(Set.of(DELIVERY_SERVICE_ID))
            .apiType(API_TYPE)
            .build();

    private static final SettingsApiDto SETTINGS_API_DTO = SettingsApiDto.newBuilder()
            .partnerId(DELIVERY_SERVICE_ID)
            .apiType(API_TYPE)
            .token(TOKEN)
            .version(VERSION)
            .format(FORMAT)
            .build();

    private static final List<MethodOptions> METHODS = List.of(CRON_METHOD, NON_CRON_METHOD);

    @Mock
    private TplRandom tplRandom;

    @Mock
    private LMSClient lmsClient;

    private ApiSettingsCreator apiSettingsCreator;

    @BeforeEach
    void setupRandom() {
        when(tplRandom.nextInt(0, 60)).thenReturn(0);
        when(tplRandom.nextInt(0, 5)).thenReturn(1);
        apiSettingsCreator = new ApiSettingsCreator(tplRandom, lmsClient);
    }

    @Test
    void setupApiSettingsAndMethods() {
        when(lmsClient.searchPartnerApiSettings(SETTINGS_API_FILTER)).thenReturn(List.of());
        when(lmsClient.createApiSettings(DELIVERY_SERVICE_ID, createSettingsApiUpdateDto()))
                .thenReturn(createSettingsApiDto(API_TYPE));

        apiSettingsCreator.create(SETTINGS_API_DTO, METHODS);

        verify(lmsClient, times(1)).createApiSettings(DELIVERY_SERVICE_ID, createSettingsApiUpdateDto());
        verify(lmsClient, never()).getPartnerApiSettingsMethods(any());
        verify(lmsClient, times(1)).createPartnerApiMethods(DELIVERY_SERVICE_ID, createListOfSettingsMethodCreateDto());
    }

    @Test
    void apiSettingsAndMethodsAlreadyExists() {
        when(lmsClient.searchPartnerApiSettings(SETTINGS_API_FILTER))
                .thenReturn(List.of(createSettingsApiDto(API_TYPE)));
        when(lmsClient.getPartnerApiSettingsMethods(DELIVERY_SERVICE_ID))
                .thenReturn(createListOfSettingsMethodDto(DEFAULT_SETTINGS_API_ID));

        apiSettingsCreator.create(SETTINGS_API_DTO, METHODS);

        verify(lmsClient, never()).createApiSettings(any(), any());
        verify(lmsClient, times(1)).getPartnerApiSettingsMethods(DELIVERY_SERVICE_ID);
        verify(lmsClient, never()).createPartnerApiMethods(any(), any());
    }

    @Test
    void apiSettingsAlreadyExistsAndMethodsDoesNotWhole() {
        when(lmsClient.searchPartnerApiSettings(SETTINGS_API_FILTER))
                .thenReturn(List.of(createSettingsApiDto(API_TYPE)));
        when(lmsClient.getPartnerApiSettingsMethods(DELIVERY_SERVICE_ID)).thenReturn(List.of());

        apiSettingsCreator.create(SETTINGS_API_DTO, METHODS);

        verify(lmsClient, never()).createApiSettings(any(), any());
        verify(lmsClient, times(1)).getPartnerApiSettingsMethods(DELIVERY_SERVICE_ID);
        verify(lmsClient, times(1)).createPartnerApiMethods(DELIVERY_SERVICE_ID, createListOfSettingsMethodCreateDto());
    }

    @Test
    void apiSettingsAlreadyExistsAndMethodsDoesNotPartly() {
        var existing = createListOfSettingsMethodDto(DEFAULT_SETTINGS_API_ID).subList(0, 1);
        var dtos = createListOfSettingsMethodCreateDto();
        var created = dtos.subList(1, dtos.size());

        when(lmsClient.searchPartnerApiSettings(SETTINGS_API_FILTER))
                .thenReturn(List.of(createSettingsApiDto(API_TYPE)));
        when(lmsClient.getPartnerApiSettingsMethods(DELIVERY_SERVICE_ID)).thenReturn(existing);

        apiSettingsCreator.create(SETTINGS_API_DTO, METHODS);

        verify(lmsClient, never()).createApiSettings(any(), any());
        verify(lmsClient, times(1)).getPartnerApiSettingsMethods(DELIVERY_SERVICE_ID);
        verify(lmsClient, times(1)).createPartnerApiMethods(DELIVERY_SERVICE_ID, created);
    }

    @Test
    void lmsInternalErrorOnMethodSettings() {
        when(lmsClient.searchPartnerApiSettings(SETTINGS_API_FILTER)).thenThrow(new HttpTemplateException(500, ""));

        assertThatThrownBy(() -> apiSettingsCreator.create(SETTINGS_API_DTO, METHODS))
                .isExactlyInstanceOf(HttpTemplateException.class);
    }

    @Test
    void lmsInternalErrorOnApiSettings() {
        when(lmsClient.searchPartnerApiSettings(SETTINGS_API_FILTER)).thenReturn(List.of());
        when(lmsClient.createApiSettings(DELIVERY_SERVICE_ID, createSettingsApiUpdateDto()))
                .thenReturn(createSettingsApiDto(API_TYPE));
        when(lmsClient.createPartnerApiMethods(any(), any())).thenThrow(new HttpTemplateException(500, ""));

        assertThatThrownBy(() -> apiSettingsCreator.create(SETTINGS_API_DTO, METHODS))
                .isExactlyInstanceOf(HttpTemplateException.class);

        verify(lmsClient, times(1)).createApiSettings(DELIVERY_SERVICE_ID, createSettingsApiUpdateDto());
        verify(lmsClient, never()).getPartnerApiSettingsMethods(any());
    }

    private SettingsApiDto createSettingsApiDto(ApiType apiType) {
        return SettingsApiDto.newBuilder()
                .id(DEFAULT_SETTINGS_API_ID)
                .apiType(apiType)
                .token(TOKEN)
                .format(FORMAT)
                .version(VERSION)
                .build();
    }

    private SettingsApiUpdateDto createSettingsApiUpdateDto() {
        return SettingsApiUpdateDto.newBuilder()
                .apiType(API_TYPE)
                .token(TOKEN)
                .format(FORMAT)
                .version(VERSION)
                .build();
    }

    private List<SettingsMethodCreateDto> createListOfSettingsMethodCreateDto() {
        return List.of(
                getSettingsMethodCreateDto(CRON_METHOD.getName(), CRON_METHOD.getUrl(), CRON),
                getSettingsMethodCreateDto(NON_CRON_METHOD.getName(), NON_CRON_METHOD.getUrl(), null)
        );
    }

    private List<SettingsMethodDto> createListOfSettingsMethodDto(long settingsApiId) {
        return List.of(
                getSettingsMethodDto(settingsApiId, CRON_METHOD.getName(), CRON_METHOD.getUrl(), CRON),
                getSettingsMethodDto(settingsApiId, NON_CRON_METHOD.getName(), NON_CRON_METHOD.getUrl(), null)
        );
    }

    private SettingsMethodDto getSettingsMethodDto(long id, String methodName, String url, String cronExpression) {
        return SettingsMethodDto.newBuilder()
                .settingsApiId(id)
                .method(methodName)
                .active(SETTINGS_ACTIVE)
                .url(url)
                .cronExpression(cronExpression)
                .build();
    }

    private SettingsMethodCreateDto getSettingsMethodCreateDto(String methodName, String url, String cronExpression) {
        return SettingsMethodCreateDto.newBuilder()
                .method(methodName)
                .apiType(API_TYPE)
                .active(SETTINGS_ACTIVE)
                .url(url)
                .cronExpression(cronExpression)
                .build();
    }

}

package ru.yandex.direct.api.v5.entity.agencyclients.delegate;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.yandex.direct.api.v5.agencyclients.AddRequest;
import com.yandex.direct.api.v5.general.CurrencyEnum;
import com.yandex.direct.api.v5.general.LangEnum;
import com.yandex.direct.api.v5.general.YesNoEnum;
import com.yandex.direct.api.v5.generalclients.ClientSettingAddEnum;
import com.yandex.direct.api.v5.generalclients.ClientSettingAddItem;
import com.yandex.direct.api.v5.generalclients.EmailSubscriptionEnum;
import com.yandex.direct.api.v5.generalclients.EmailSubscriptionItem;
import com.yandex.direct.api.v5.generalclients.GrantItem;
import com.yandex.direct.api.v5.generalclients.NotificationAdd;
import com.yandex.direct.api.v5.generalclients.PrivilegeEnum;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.core.entity.client.model.AddAgencyClientRequest;
import ru.yandex.direct.core.entity.client.service.AddAgencyClientService;
import ru.yandex.direct.core.service.RequestInfoProvider;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.i18n.Language;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(Parameterized.class)
public class AddAgencyClientsDelegateConvertRequestTest {
    private static final String LOGIN = "login";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final CurrencyEnum SOURCE_CURRENCY = CurrencyEnum.RUB;
    private static final CurrencyCode RESULT_CURRENCY = CurrencyCode.RUB;
    private static final String NOTIFICATION_EMAIL = "login@yandex.ru";
    private static final LangEnum SOURCE_NOTIFICATION_LANG = LangEnum.RU;
    private static final Language RESULT_NOTIFICATION_LANG = Language.RU;

    private static final Map<EmailSubscriptionEnum, Boolean> SUBSCRIPTION_MAP = ImmutableMap.of(
            EmailSubscriptionEnum.RECEIVE_RECOMMENDATIONS, true,
            EmailSubscriptionEnum.TRACK_MANAGED_CAMPAIGNS, true,
            EmailSubscriptionEnum.TRACK_POSITION_CHANGES, true);

    private static final Map<ClientSettingAddEnum, Boolean> SETTING_MAP = ImmutableMap.of(
            ClientSettingAddEnum.DISPLAY_STORE_RATING, false,
            ClientSettingAddEnum.CORRECT_TYPOS_AUTOMATICALLY, false,
            ClientSettingAddEnum.SHARED_ACCOUNT_ENABLED, true
    );


    private static final Map<PrivilegeEnum, Boolean> GRANT_MAP = ImmutableMap.of(
            PrivilegeEnum.EDIT_CAMPAIGNS, true,
            PrivilegeEnum.IMPORT_XLS, true,
            PrivilegeEnum.TRANSFER_MONEY, true);

    @Parameterized.Parameter
    public Set<EmailSubscriptionEnum> subscriptionSubset;
    @Parameterized.Parameter(value = 1)
    public Set<ClientSettingAddEnum> settingSubset;
    @Parameterized.Parameter(value = 2)
    public Set<PrivilegeEnum> grantSubset;

    private AddAgencyClientsDelegate delegate;

    @Parameterized.Parameters
    @SuppressWarnings("unchecked")
    public static List<Object[]> getParameters() {
        return Sets.cartesianProduct(
                        Sets.powerSet(EnumSet.allOf(EmailSubscriptionEnum.class)),
                        Sets.powerSet(EnumSet.allOf(ClientSettingAddEnum.class)),
                        Sets.powerSet(EnumSet.allOf(PrivilegeEnum.class)))
                .stream()
                .map(List::toArray)
                .collect(toList());
    }

    private static <T> boolean getFlagValue(Map<T, Boolean> mapping, Set<T> subset, T flagKey) {
        return subset.contains(flagKey) == mapping.get(flagKey);
    }

    private static <T> Optional<Boolean> getOptionalFlagValue(Map<T, Boolean> mapping, Set<T> subset, T flagkey) {
        boolean result = mapping.get(flagkey);
        if (subset.contains(flagkey)) {
            return Optional.of(result);
        }
        return Optional.empty();
    }

    @Before
    public void setUp() {
        delegate = new AddAgencyClientsDelegate(
                mock(ApiAuthenticationSource.class),
                mock(AddAgencyClientService.class),
                mock(RequestInfoProvider.class),
                mock(ResultConverter.class));
    }

    @Test
    public void testConvertFromRequest() {
        AddRequest externalRequest = new AddRequest()
                .withLogin(LOGIN)
                .withFirstName(FIRST_NAME)
                .withLastName(LAST_NAME)
                .withCurrency(SOURCE_CURRENCY)
                .withNotification(
                        new NotificationAdd()
                                .withEmail(NOTIFICATION_EMAIL)
                                .withLang(SOURCE_NOTIFICATION_LANG)
                                .withEmailSubscriptions(
                                        subscriptionSubset.stream()
                                                .map(e -> new EmailSubscriptionItem()
                                                        .withOption(e)
                                                        .withValue(YesNoEnum.YES))
                                                .collect(toList()))
                )
                .withSettings(
                        settingSubset.stream()
                                .map(e -> new ClientSettingAddItem()
                                        .withOption(e)
                                        .withValue(YesNoEnum.YES))
                                .collect(toList()))
                .withGrants(
                        grantSubset.stream()
                                .map(e -> new GrantItem()
                                        .withPrivilege(e)
                                        .withValue(YesNoEnum.YES))
                                .collect(toList()));

        AddAgencyClientRequest internalRequest = delegate.convertRequest(externalRequest);

        assertThat(internalRequest)
                .usingRecursiveComparison()
                .isEqualTo(
                        new AddAgencyClientRequest()
                                .withLogin(LOGIN)
                                .withFirstName(FIRST_NAME)
                                .withLastName(LAST_NAME)
                                .withCurrency(RESULT_CURRENCY)
                                .withNotificationEmail(NOTIFICATION_EMAIL)
                                .withNotificationLang(RESULT_NOTIFICATION_LANG)
                                .withSendNews(
                                        getFlagValue(
                                                SUBSCRIPTION_MAP,
                                                subscriptionSubset,
                                                EmailSubscriptionEnum.RECEIVE_RECOMMENDATIONS))
                                .withSendAccNews(
                                        getFlagValue(
                                                SUBSCRIPTION_MAP,
                                                subscriptionSubset,
                                                EmailSubscriptionEnum.TRACK_MANAGED_CAMPAIGNS))
                                .withSendWarn(
                                        getFlagValue(
                                                SUBSCRIPTION_MAP,
                                                subscriptionSubset,
                                                EmailSubscriptionEnum.TRACK_POSITION_CHANGES))
                                .withHideMarketRating(
                                        getFlagValue(
                                                SETTING_MAP,
                                                settingSubset,
                                                ClientSettingAddEnum.DISPLAY_STORE_RATING))
                                .withNoTextAutocorrection(
                                        getFlagValue(
                                                SETTING_MAP,
                                                settingSubset,
                                                ClientSettingAddEnum.CORRECT_TYPOS_AUTOMATICALLY))
                                .withAllowEditCampaigns(
                                        getFlagValue(
                                                GRANT_MAP,
                                                grantSubset,
                                                PrivilegeEnum.EDIT_CAMPAIGNS))
                                .withAllowImportXls(
                                        getFlagValue(
                                                GRANT_MAP,
                                                grantSubset,
                                                PrivilegeEnum.IMPORT_XLS))
                                .withAllowTransferMoney(
                                        getFlagValue(
                                                GRANT_MAP,
                                                grantSubset,
                                                PrivilegeEnum.TRANSFER_MONEY))
                                .withSharedAccountEnabled(
                                        getOptionalFlagValue(
                                                SETTING_MAP,
                                                settingSubset,
                                                ClientSettingAddEnum.SHARED_ACCOUNT_ENABLED).orElse(null))
                );
    }
}

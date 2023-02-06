package ru.yandex.market.partner.notification.api;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.notification.AbstractFunctionalTest;
import ru.yandex.mj.generated.client.self.api.UserSendingSettingsApiClient;
import ru.yandex.mj.generated.client.self.model.PartnerToSettingsDTO;
import ru.yandex.mj.generated.client.self.model.TransportDTO;
import ru.yandex.mj.generated.client.self.model.UserBusinessSettingsDTO;
import ru.yandex.mj.generated.client.self.model.UserPartnerSettingsDTO;
import ru.yandex.mj.generated.client.self.model.UserThemeSettingsDTO;
import ru.yandex.mj.generated.client.self.model.UserTransportSettingsDTO;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@DbUnitDataSet(before = "UserSendingSettingsApiServiceTest.before.csv")
class UserSendingSettingsApiServiceTest extends AbstractFunctionalTest {

    @Autowired
    UserSendingSettingsApiClient userSendingSettingsApiClient;

    @Test
    void getDefaultUserBusinessSettings() throws ExecutionException, InterruptedException {
        var result = userSendingSettingsApiClient.getUserBusinessSettings(111L, 222L).schedule().get();
        assertThat(result, equalTo(getDefaultBusinessSettings()));
    }

    @Test
    void getUserBusinessSettings() throws ExecutionException, InterruptedException {
        var result = userSendingSettingsApiClient.getUserBusinessSettings(100L, 200L).schedule().get();
        assertThat(result, equalTo(getBusinessSettingsSample1()));
    }

    @Test
    void setUserBusinessSettings() throws ExecutionException, InterruptedException {
        userSendingSettingsApiClient.setUserBusinessSettings(111L, 222L, getBusinessSettingsSample1())
                .scheduleVoid().get();

        var stored = userSendingSettingsApiClient.getUserBusinessSettings(111L, 222L).schedule().get();
        assertThat(stored, equalTo(getBusinessSettingsSample1()));
    }

    @Test
    void resetUserBusinessSettings() throws ExecutionException, InterruptedException {
        userSendingSettingsApiClient.resetUserBusinessSettings(1L, 2L).schedule().get();

        var result = userSendingSettingsApiClient.resetUserBusinessSettings(100L, 200L).schedule().get();
        assertThat(result, equalTo(getDefaultBusinessSettings()));

        var stored = userSendingSettingsApiClient.getUserBusinessSettings(100L, 200L).schedule().get();
        assertThat(stored, equalTo(getDefaultBusinessSettings()));
    }

    @Test
    void getDefaultUserPartnersSettings() throws ExecutionException, InterruptedException {
        var result = userSendingSettingsApiClient.getUserPartnersSettings(
                100L,
                List.of(203L, 202L)
        ).schedule().get();

        var expected = List.of(
                new PartnerToSettingsDTO()
                        .partnerId(202L)
                        .settings(new UserPartnerSettingsDTO().enabled(true)),
                new PartnerToSettingsDTO()
                        .partnerId(203L)
                        .settings(new UserPartnerSettingsDTO().enabled(true))
        );

        assertThat(result, equalTo(expected));
    }

    @Test
    void getUserPartnersSettings() throws ExecutionException, InterruptedException {
        var result = userSendingSettingsApiClient.getUserPartnersSettings(
                100L,
                List.of(200L, 201L, 202L)
        ).schedule().get();

        var expected = List.of(
                new PartnerToSettingsDTO()
                        .partnerId(200L)
                        .settings(new UserPartnerSettingsDTO().enabled(false)),
                new PartnerToSettingsDTO()
                        .partnerId(201L)
                        .settings(new UserPartnerSettingsDTO().enabled(false)),
                new PartnerToSettingsDTO()
                        .partnerId(202L)
                        .settings(new UserPartnerSettingsDTO().enabled(true))
        );

        assertThat(result, equalTo(expected));
    }

    @Test
    void setUserPartnerSettings() throws ExecutionException, InterruptedException {
        userSendingSettingsApiClient.setUserPartnerSettings(
                111L,
                222L,
                new UserPartnerSettingsDTO().enabled(false)
        ).scheduleVoid().get();

        var stored = userSendingSettingsApiClient.getUserPartnersSettings(111L, List.of(222L)).schedule().get();

        var expected = List.of(
                new PartnerToSettingsDTO()
                        .partnerId(222L)
                        .settings(new UserPartnerSettingsDTO().enabled(false))
        );

        assertThat(stored, equalTo(expected));
    }

    @Nonnull
    private UserBusinessSettingsDTO getBusinessSettingsSample1() {
        var themes = List.of(1, 3, 5, 7, 8, 10, 11, 12, 13).stream()
                .mapToLong(i -> i)
                .mapToObj(themeId -> new UserThemeSettingsDTO()
                        .themeId(themeId)
                        .enabled(themeId % 2 == 0)
                        .transports(List.of(
                                new UserTransportSettingsDTO()
                                        .transport(TransportDTO.EMAIL)
                                        .enabled(themeId % 3 == 0),
                                new UserTransportSettingsDTO()
                                        .transport(TransportDTO.TELEGRAM)
                                        .enabled(themeId % 4 == 0)
                        ))).collect(Collectors.toList());

        return new UserBusinessSettingsDTO().themes(themes);
    }

    private UserBusinessSettingsDTO getDefaultBusinessSettings() {
        var themes = List.of(1, 3, 5, 7, 8, 10, 11, 12, 13).stream()
                .mapToLong(i -> i)
                .mapToObj(themeId -> new UserThemeSettingsDTO()
                        .themeId(themeId)
                        .enabled(true)
                        .transports(List.of(
                                new UserTransportSettingsDTO()
                                        .transport(TransportDTO.EMAIL)
                                        .enabled(true),
                                new UserTransportSettingsDTO()
                                        .transport(TransportDTO.TELEGRAM)
                                        .enabled(true)
                        ))).collect(Collectors.toList());

        return new UserBusinessSettingsDTO().themes(themes);
    }
}

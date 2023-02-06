package ru.yandex.market.core.samovar.validation;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.feed.validation.FeedValidationTestUtils;
import ru.yandex.market.core.logbroker.samovar.SamovarEvent;
import ru.yandex.market.core.misc.resource.RemoteResource;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.request.trace.RequestContextHolder;

import static ru.yandex.market.core.feed.validation.FeedValidationTestUtils.createUnitedValidationInfoWrapper;
import static ru.yandex.market.core.samovar.SamovarTestUtils.assertSamovarEvent;

/**
 * Date: 26.10.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
class SamovarValidationTaskImplTest extends FunctionalTest {

    @Autowired
    private SamovarValidationTask samovarValidationTask;
    @Autowired
    @Qualifier("samovarLogbrokerService")
    private LogbrokerService logbrokerService;

    @BeforeEach
    void beforeEach() {
        RequestContextHolder.createNewContext();
    }

    @DisplayName("Отправка события через самовар для ресурса без логина и пароля")
    @Test
    void validate_valueWithoutCredentials_correctResult() throws URISyntaxException, IOException {
        var viw = FeedValidationTestUtils.createUnitedValidationInfoWrapper(
                43L, 1001L, RemoteResource.of("https://aga.ru"), CampaignType.SHOP);

        samovarValidationTask.validate(viw);

        ArgumentCaptor<SamovarEvent> eventCaptor = ArgumentCaptor.forClass(SamovarEvent.class);
        Mockito.verify(logbrokerService, Mockito.times(1))
                .publishEvent(eventCaptor.capture());

        assertSamovarEvent(eventCaptor.getValue(), viw);
    }

    @DisplayName("Отправка события через самовар для ресурса с логином и паролем")
    @Test
    void validate_valueWithCredentials_correctResult() throws URISyntaxException, IOException {
        var viw = createUnitedValidationInfoWrapper(42L, 1001L,
                RemoteResource.of("https://aga.ru", "login", "pass"), CampaignType.SUPPLIER);

        samovarValidationTask.validate(viw);

        ArgumentCaptor<SamovarEvent> eventCaptor = ArgumentCaptor.forClass(SamovarEvent.class);
        Mockito.verify(logbrokerService, Mockito.times(1))
                .publishEvent(eventCaptor.capture());

        assertSamovarEvent(eventCaptor.getValue(), viw);
    }
}

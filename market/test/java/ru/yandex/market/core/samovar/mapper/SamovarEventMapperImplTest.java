package ru.yandex.market.core.samovar.mapper;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.application.EnvironmentType;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.feed.validation.FeedValidationTestUtils;
import ru.yandex.market.core.feed.validation.model.UnitedValidationInfoWrapper;
import ru.yandex.market.core.logbroker.samovar.SamovarEvent;
import ru.yandex.market.core.misc.resource.RemoteResource;
import ru.yandex.market.request.trace.RequestContextHolder;

import static ru.yandex.market.core.samovar.SamovarTestUtils.assertSamovarEvent;

/**
 * Date: 26.10.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
class SamovarEventMapperImplTest extends FunctionalTest {

    @Autowired
    private SamovarEventMapper samovarEventMapper;

    @BeforeEach
    void beforeEach() {
        RequestContextHolder.createNewContext();
    }

    @DisplayName("Маппинг данных для ресурса с логином и паролем")
    @Test
    void map_valueWithCredentials_correctMapping() throws IOException, URISyntaxException {
        var viw = createUnitedValidationInfoWrapper(CampaignType.SUPPLIER);

        SamovarEvent samovarEvent = samovarEventMapper.map(viw, EnvironmentType.DEVELOPMENT,
                "test-feed", RequestContextHolder.getContext().getRequestId());

        assertSamovarEvent(samovarEvent, viw);
    }

    @DisplayName("Маппинг данных для ресурса с логином и паролем, без типа поставщика")
    @Test
    void map_valueWithCredentialsAndNullType_correctMapping() throws IOException, URISyntaxException {
        var viw = createUnitedValidationInfoWrapper(CampaignType.SUPPLIER);

        SamovarEvent samovarEvent = samovarEventMapper.map(viw, EnvironmentType.DEVELOPMENT,
                "test-feed", RequestContextHolder.getContext().getRequestId());

        assertSamovarEvent(samovarEvent, viw);
    }

    @DisplayName("Маппинг данных для ресурса без логина и пароля")
    @Test
    void map_valueWithoutCredentials_correctMapping() throws IOException, URISyntaxException {
        var viw = FeedValidationTestUtils.createUnitedValidationInfoWrapper(
                43L, 1001L, RemoteResource.of("http://ya"), CampaignType.SHOP);

        SamovarEvent samovarEvent = samovarEventMapper.map(viw, EnvironmentType.DEVELOPMENT,
                "test-feed", RequestContextHolder.getContext().getRequestId());

        assertSamovarEvent(samovarEvent, viw);
    }

    @Nonnull
    private UnitedValidationInfoWrapper createUnitedValidationInfoWrapper(CampaignType type) {
        return FeedValidationTestUtils.createUnitedValidationInfoWrapper(42L, 1001L,
                RemoteResource.of("https://aga.ru", "ya", "qwe"), type);
    }
}

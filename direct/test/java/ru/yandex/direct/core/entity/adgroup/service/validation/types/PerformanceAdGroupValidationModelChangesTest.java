package ru.yandex.direct.core.entity.adgroup.service.validation.types;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.client.service.ClientGeoService;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.service.CreativeService;
import ru.yandex.direct.core.entity.feed.service.FeedService;
import ru.yandex.direct.core.entity.feed.validation.FeedDefects;
import ru.yandex.direct.core.testing.configuration.CoreTestingConfiguration;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeLoader;
import ru.yandex.direct.regions.GeoTreeType;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.PathHelper;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.inconsistentCreativeGeoToAdGroupGeo;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrorsAndWarnings;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.path;

@ContextConfiguration(classes = CoreTestingConfiguration.class)
@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class PerformanceAdGroupValidationModelChangesTest {

    @Test
    public void validateModelChanges_success() throws IOException {
        //Создаём модель изменений
        long adGroupId = 33L;
        ModelChanges<PerformanceAdGroup> modelChanges = new ModelChanges<>(adGroupId, PerformanceAdGroup.class)
                .process("new_body_field", PerformanceAdGroup.FIELD_TO_USE_AS_BODY);

        //Валидируем
        ValidationResult<List<ModelChanges<PerformanceAdGroup>>, Defect> actual =
                validate(ClientId.fromLong(11L), adGroupId, modelChanges, emptyList());

        //Сверяем ожидания и реальность
        assertThat(actual).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    public void validateModelChanges_failureWhenTryToChangeFeed() throws IOException {
        //Создаём модель изменений
        ClientId clientId = ClientId.fromLong(11L);
        long adGroupId = 33L;
        ModelChanges<PerformanceAdGroup> modelChanges = new ModelChanges<>(adGroupId, PerformanceAdGroup.class)
                .process(22L, PerformanceAdGroup.FEED_ID);

        //Валидируем
        ValidationResult<List<ModelChanges<PerformanceAdGroup>>, Defect> actual =
                validate(clientId, adGroupId, modelChanges, emptyList());

        //Сверяем ожидания и реальность
        Matcher<ValidationResult<Object, Defect>> expectedValidationResult = hasDefectWithDefinition(validationError(
                path(PathHelper.index(0), PathHelper.field(PerformanceAdGroup.FEED_ID)), FeedDefects.feedNotExist()));
        assertThat(actual).is(matchedBy(expectedValidationResult));
    }

    @Test
    public void validateModelChanges_successWhenCorrespondCreativeGeo() throws IOException {
        //Исходные данные
        ClientId clientId = ClientId.fromLong(11L);
        long adGroupId = 33L;
        ModelChanges<PerformanceAdGroup> modelChanges = new ModelChanges<>(adGroupId, PerformanceAdGroup.class)
                .process(singletonList(Region.MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID), PerformanceAdGroup.GEO);
        Creative creative = new Creative()
                .withSumGeo(singletonList(Region.RUSSIA_REGION_ID));

        //Валидируем
        ValidationResult<List<ModelChanges<PerformanceAdGroup>>, Defect> actual =
                validate(clientId, adGroupId, modelChanges, singletonList(creative));

        //Сверяем ожидания и реальность
        assertThat(actual).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    public void validateModelChanges_failureWhenNotCorrespondCreativeGeo() throws IOException {
        //Исходные данные
        ClientId clientId = ClientId.fromLong(11L);
        long adGroupId = 33L;
        ModelChanges<PerformanceAdGroup> modelChanges = new ModelChanges<>(adGroupId, PerformanceAdGroup.class)
                .process(singletonList(Region.ISTANBUL_REGION_ID), PerformanceAdGroup.GEO);
        Creative creative = new Creative()
                .withSumGeo(singletonList(Region.RUSSIA_REGION_ID));

        //Валидируем
        ValidationResult<List<ModelChanges<PerformanceAdGroup>>, Defect> actual =
                validate(clientId, adGroupId, modelChanges, singletonList(creative));

        //Сверяем ожидания и реальность
        Matcher<ValidationResult<Object, Defect>> expectedValidationResult = hasDefectWithDefinition(validationError(
                path(PathHelper.index(0), PathHelper.field(PerformanceAdGroup.GEO)),
                inconsistentCreativeGeoToAdGroupGeo()));
        assertThat(actual).is(matchedBy(expectedValidationResult));
    }


    @Test
    public void validateModelChanges_successWhenCorrespondTranslocationRegion() throws IOException {
        //Исходные данные
        ClientId clientId = ClientId.fromLong(11L);
        long adGroupId = 33L;
        ModelChanges<PerformanceAdGroup> modelChanges = new ModelChanges<>(adGroupId, PerformanceAdGroup.class)
                .process(singletonList(Region.SIMFEROPOL_REGION_ID), PerformanceAdGroup.GEO);
        Creative creative = new Creative()
                .withSumGeo(singletonList(Region.RUSSIA_REGION_ID));

        //Валидируем
        ValidationResult<List<ModelChanges<PerformanceAdGroup>>, Defect> actual =
                validate(clientId, adGroupId, modelChanges, singletonList(creative));

        //Сверяем ожидания и реальность
        assertThat(actual).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    public void validateModelChanges_failureWhenNotCorrespondTranslocationRegion() throws IOException {
        //Исходные данные
        ClientId clientId = ClientId.fromLong(11L);
        long adGroupId = 33L;
        ModelChanges<PerformanceAdGroup> modelChanges = new ModelChanges<>(adGroupId, PerformanceAdGroup.class)
                .process(singletonList(Region.SIMFEROPOL_REGION_ID), PerformanceAdGroup.GEO);
        Creative creative = new Creative()
                .withSumGeo(singletonList(Region.UKRAINE_REGION_ID));

        //Валидируем
        ValidationResult<List<ModelChanges<PerformanceAdGroup>>, Defect> actual =
                validate(clientId, adGroupId, modelChanges, singletonList(creative));

        //Сверяем ожидания и реальность
        Matcher<ValidationResult<Object, Defect>> expectedValidationResult = hasDefectWithDefinition(validationError(
                path(PathHelper.index(0), PathHelper.field(PerformanceAdGroup.GEO)),
                inconsistentCreativeGeoToAdGroupGeo()));
        assertThat(actual).is(matchedBy(expectedValidationResult));
    }

    private ValidationResult<List<ModelChanges<PerformanceAdGroup>>, Defect> validate(ClientId clientId, long adGroupId,
                                                                                      ModelChanges<PerformanceAdGroup> modelChanges, List<Creative> creatives) throws IOException {
        FeedService feedService = mock(FeedService.class);
        CreativeService creativeService = mock(CreativeService.class);
        Map<Long, List<Creative>> creativesByPerformanceAdGroup = singletonMap(adGroupId, creatives);
        when(creativeService.getCreativesByPerformanceAdGroups(any(ClientId.class), anyCollection()))
                .thenReturn(creativesByPerformanceAdGroup);
        String json = IOUtils.toString(this.getClass().getResourceAsStream("/externalData/regions_russian.json"),
                StandardCharsets.UTF_8);
        GeoTree geoTree = GeoTreeLoader.build(json, GeoTreeType.RUSSIAN);
        ClientGeoService clientGeoService = mock(ClientGeoService.class);
        when(clientGeoService.getClientTranslocalGeoTree(any(ClientId.class))).thenReturn(geoTree);
        PerformanceAdGroupValidation mockedPerformanceAdGroupValidation =
                new PerformanceAdGroupValidation(feedService, clientGeoService, creativeService);
        return mockedPerformanceAdGroupValidation.validateModelChanges(clientId, singletonList(modelChanges));
    }

}

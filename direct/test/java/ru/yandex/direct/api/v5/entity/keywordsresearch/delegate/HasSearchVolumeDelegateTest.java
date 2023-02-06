package ru.yandex.direct.api.v5.entity.keywordsresearch.delegate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.yandex.direct.api.v5.keywordsresearch.HasSearchVolumeFieldEnum;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.advq.AdvqClient;
import ru.yandex.direct.advq.CheckMinHitsResult;
import ru.yandex.direct.api.v5.common.validation.DefaultApiPresentations;
import ru.yandex.direct.api.v5.common.validation.DefectPresentationService;
import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.keywordsresearch.service.HasSearchVolumeInnerRequest;
import ru.yandex.direct.api.v5.entity.keywordsresearch.service.KeywordSearchVolumes;
import ru.yandex.direct.api.v5.result.ApiResult;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.core.entity.region.validation.RegionIdsValidator;
import ru.yandex.direct.core.entity.stopword.repository.StopWordRepository;
import ru.yandex.direct.core.entity.stopword.service.StopWordService;
import ru.yandex.direct.libs.keywordutils.inclusion.model.KeywordWithLemmasFactory;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeLoader;
import ru.yandex.direct.regions.GeoTreeType;
import ru.yandex.direct.regions.SimpleGeoTreeFactory;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.nCopies;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.api.v5.validation.DefectTypes.absent;
import static ru.yandex.direct.api.v5.validation.DefectTypes.duplicatedElement;
import static ru.yandex.direct.api.v5.validation.DefectTypes.maxElementsPerRequest;
import static ru.yandex.direct.api.v5.validation.DefectTypes.requiredButEmpty;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@Api5Test
@RunWith(SpringRunner.class)
public class HasSearchVolumeDelegateTest {

    @Autowired
    private KeywordWithLemmasFactory keywordFactory;

    private HasSearchVolumeDelegate delegate;

    private static HasSearchVolumeInnerRequest request(List<String> keywords, List<Long> regionIds) {
        return new HasSearchVolumeInnerRequest(keywords, regionIds,
                ImmutableSet.of(HasSearchVolumeFieldEnum.DESKTOPS));
    }

    @Before
    public void setup() throws IOException {
        AdvqClient client = mock(AdvqClient.class);
        when(client.checkMinHits(anyList(), anyList(), anyList()))
                .thenReturn(CheckMinHitsResult.success(emptyMap()));

        String json = IOUtils.toString(this.getClass().getResourceAsStream("/externalData/regions.json"),
                StandardCharsets.UTF_8);
        GeoTree geoTree = GeoTreeLoader.build(json, GeoTreeType.GLOBAL);
        DefectPresentationService defectPresentationService =
                new DefectPresentationService(DefaultApiPresentations.HOLDER);

        StopWordRepository stopWordRepositoryMock = mock(StopWordRepository.class);
        when(stopWordRepositoryMock.getStopWords()).thenReturn(ImmutableSet.of("stop", "стоп"));
        StopWordService stopWordService = new StopWordService(stopWordRepositoryMock);

        delegate = new HasSearchVolumeDelegate(client,
                new SimpleGeoTreeFactory(ImmutableMap.<GeoTreeType, GeoTree>builder()
                        .put(GeoTreeType.API, geoTree)
                        .build()),
                new RegionIdsValidator(),
                new ResultConverter(mock(TranslationService.class), defectPresentationService),
                mock(ApiAuthenticationSource.class),
                stopWordService,
                keywordFactory);
    }

    @Test
    public void validate_emptyListOfKeywords() {
        HasSearchVolumeInnerRequest request = request(emptyList(), singletonList(0L));
        ApiResult<List<KeywordSearchVolumes>> result = delegate.processRequest(request);
        assertThat(result.getErrors(), hasItem(validationError(path(field("Keywords")), absent())));
    }

    @Test
    public void validate_listLongerThan10000() {
        HasSearchVolumeInnerRequest request = request(nCopies(10001, "сапоги"), singletonList(0L));
        ApiResult<List<KeywordSearchVolumes>> result = delegate.processRequest(request);
        assertThat(result.getErrors(), hasItem(validationError(path(field("Keywords")),
                maxElementsPerRequest(10000))));
    }

    @Test
    public void validate_nonUniqueKeywords() {
        HasSearchVolumeInnerRequest request = request(asList("сапог", "сапог"), singletonList(1L));
        ApiResult<List<KeywordSearchVolumes>> result = delegate.processRequest(request);
        assertThat(result.getErrors(), hasItem(validationError(path(field("Keywords"), index(0)),
                duplicatedElement())));
    }

    @Test
    public void validate_keywordsIsNull() {
        HasSearchVolumeInnerRequest request = request(null, singletonList(1L));
        ApiResult<List<KeywordSearchVolumes>> result = delegate.processRequest(request);
        assertThat(result.getErrors(), hasItem(validationError(path(field("Keywords")), requiredButEmpty())));
    }

    @Test
    public void validate_regionIdsIsNull() {
        HasSearchVolumeInnerRequest request = request(singletonList("сапоги"), null);
        ApiResult<List<KeywordSearchVolumes>> result = delegate.processRequest(request);
        assertThat(result.getErrors(), hasItem(validationError(path(field("RegionIds")), requiredButEmpty())));
    }
}

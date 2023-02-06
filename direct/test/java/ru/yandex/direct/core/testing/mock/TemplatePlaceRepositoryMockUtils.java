package ru.yandex.direct.core.testing.mock;

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import ru.yandex.direct.core.entity.internalads.Constants;
import ru.yandex.direct.core.entity.internalads.model.TemplatePlace;
import ru.yandex.direct.core.entity.internalads.repository.InternalYaBsClusterChooser;
import ru.yandex.direct.core.entity.internalads.repository.TemplatePlaceRepository;
import ru.yandex.direct.core.entity.internalads.repository.TemplatePlaceYtRepository;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.ytwrapper.client.YtProvider;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.entity.internalads.Constants.MEDIA_BANNER_TEMPLATE_ID;
import static ru.yandex.direct.core.entity.internalads.Constants.TEASER_INLINE_BROWSER_TEMPLATE_ID;
import static ru.yandex.direct.core.entity.internalads.Constants.TEASER_TEMPLATE_ID;

public class TemplatePlaceRepositoryMockUtils {
    public static final long PLACE_1 = 1L;
    public static final long PLACE_2 = 2L;

    public static final Long MODERATED_PLACE_ID = new TreeSet<>(Constants.MODERATED_PLACES).first();

    public static final long PLACE_1_TEMPLATE_1 = 11L;
    public static final long PLACE_1_TEMPLATE_2_WITHOUT_RESOURCES = 12;
    public static final long PLACE_1_TEMPLATE_3_WITH_IMAGE = 13;

    public static final long PLACE_1_TEMPLATE_4_URL_IMG = 14;

    public static final long PLACE_1_TEMPLATE_WITH_AGE_VARIABLE = 16;

    public static final long PLACE_1_TEMPLATE_5_WITH_MANY_RESOURCES = 17;

    public static final long PLACE_2_TEMPLATE_1 = 15;

    // id шаблона взят из TemplateInfoOverrides, чтобы проставилось свойство hidden для нужного ресурса
    public static final long PLACE_2_TEMPLATE_2_WITH_HIDDEN_RESOURCE = 642;

    // id шаблонов взяты из Constants.MODERATED_TEMPLATES_RESOURCES_MAPPER, чтобы шаблоны были модерирумые
    public static final long PLACE_3_TEMPLATE_1 = MEDIA_BANNER_TEMPLATE_ID;
    public static final long PLACE_3_TEMPLATE_2 = TEASER_TEMPLATE_ID;
    public static final long PLACE_3_TEMPLATE_3 = TEASER_INLINE_BROWSER_TEMPLATE_ID;
    public static final long MODERATED_TEMPLATE_ID = PLACE_3_TEMPLATE_1;


    private static final TemplatePlace TEMPLATE_PLACE_1 =
            new TemplatePlace().withPlaceId(PLACE_1).withTemplateId(PLACE_1_TEMPLATE_1);

    private static final TemplatePlace TEMPLATE_PLACE_2 =
            new TemplatePlace().withPlaceId(PLACE_1).withTemplateId(PLACE_1_TEMPLATE_2_WITHOUT_RESOURCES);

    private static final TemplatePlace TEMPLATE_PLACE_3 =
            new TemplatePlace().withPlaceId(PLACE_1).withTemplateId(PLACE_1_TEMPLATE_3_WITH_IMAGE);

    private static final TemplatePlace TEMPLATE_PLACE_4 =
            new TemplatePlace().withPlaceId(PLACE_1).withTemplateId(PLACE_1_TEMPLATE_4_URL_IMG);

    private static final TemplatePlace TEMPLATE_PLACE_5 =
            new TemplatePlace().withPlaceId(PLACE_2).withTemplateId(PLACE_2_TEMPLATE_1);

    private static final TemplatePlace TEMPLATE_PLACE_6 =
            new TemplatePlace().withPlaceId(PLACE_2).withTemplateId(PLACE_2_TEMPLATE_2_WITH_HIDDEN_RESOURCE);

    private static final TemplatePlace TEMPLATE_PLACE_7 =
            new TemplatePlace().withPlaceId(PLACE_1).withTemplateId(PLACE_1_TEMPLATE_WITH_AGE_VARIABLE);

    private static final TemplatePlace TEMPLATE_PLACE_8 =
            new TemplatePlace().withPlaceId(MODERATED_PLACE_ID).withTemplateId(PLACE_3_TEMPLATE_1);

    private static final TemplatePlace TEMPLATE_PLACE_9 =
            new TemplatePlace().withPlaceId(MODERATED_PLACE_ID).withTemplateId(PLACE_3_TEMPLATE_2);

    private static final TemplatePlace TEMPLATE_PLACE_10 =
            new TemplatePlace().withPlaceId(PLACE_1).withTemplateId(PLACE_1_TEMPLATE_5_WITH_MANY_RESOURCES);

    private static final TemplatePlace TEMPLATE_PLACE_11 =
            new TemplatePlace().withPlaceId(MODERATED_PLACE_ID).withTemplateId(PLACE_3_TEMPLATE_3);

    public static TemplatePlaceYtRepository createYtRepositoryMock() {
        var all = asList(TEMPLATE_PLACE_1, TEMPLATE_PLACE_2, TEMPLATE_PLACE_3, TEMPLATE_PLACE_4);
        return new TemplatePlaceYtRepository(mock(YtProvider.class), mock(InternalYaBsClusterChooser.class)) {
            @Override
            public List<TemplatePlace> getAll() {
                return all;
            }
        };
    }

    public static TemplatePlaceRepository createMySqlRepositoryMock() {
        List<TemplatePlace> templatePlaces = asList(
                TEMPLATE_PLACE_1, TEMPLATE_PLACE_2, TEMPLATE_PLACE_3, TEMPLATE_PLACE_4,
                TEMPLATE_PLACE_5, TEMPLATE_PLACE_6, TEMPLATE_PLACE_7, TEMPLATE_PLACE_8,
                TEMPLATE_PLACE_9, TEMPLATE_PLACE_10, TEMPLATE_PLACE_11);
        return new TemplatePlaceRepository(mock(DslContextProvider.class)) {
            @Override
            public List<TemplatePlace> getAll() {
                return templatePlaces;
            }

            @Override
            public List<Long> getPlaces() {
                return templatePlaces.stream().map(TemplatePlace::getPlaceId).distinct().collect(Collectors.toList());
            }

            @Override
            public List<TemplatePlace> getByPlaceIds(Collection<Long> placeIds) {
                return templatePlaces.stream()
                        .filter(t -> placeIds.contains(t.getPlaceId()))
                        .collect(Collectors.toList());
            }

            @Override
            public List<TemplatePlace> getByTemplateId(Long templateId) {
                return templatePlaces.stream()
                        .filter(t -> t.getTemplateId().equals(templateId))
                        .collect(Collectors.toList());
            }
        };
    }
}

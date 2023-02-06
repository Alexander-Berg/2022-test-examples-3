package ru.yandex.direct.core.testing.mock;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;

import ru.yandex.direct.core.entity.internalads.model.TemplateResource;
import ru.yandex.direct.core.entity.internalads.model.TemplateResourceOption;
import ru.yandex.direct.core.entity.internalads.repository.InternalYaBsClusterChooser;
import ru.yandex.direct.core.entity.internalads.repository.TemplateResourceRepository;
import ru.yandex.direct.core.entity.internalads.repository.TemplateResourceYtRepository;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.ytwrapper.client.YtProvider;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_4_URL_IMG;
import static ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_5_WITH_MANY_RESOURCES;
import static ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_WITH_AGE_VARIABLE;
import static ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils.PLACE_2_TEMPLATE_2_WITH_HIDDEN_RESOURCE;
import static ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils.PLACE_3_TEMPLATE_1;
import static ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils.PLACE_3_TEMPLATE_2;
import static ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils.PLACE_3_TEMPLATE_3;

public class TemplateResourceRepositoryMockUtils {
    public static final long TEMPLATE_RESOURCE_RESOURCE_NO = 7;

    public static final long TEMPLATE_1_RESOURCE_1_REQUIRED = 111;
    public static final long TEMPLATE_3_RESOURCE_1_REQUIRED_IMAGE = 131;

    public static final long TEMPLATE_4_RESOURCE_1_IMAGE = 141;
    public static final long TEMPLATE_4_RESOURCE_2_REQUIRED_URL = 142;

    public static final long TEMPLATE_5_RESOURCE_1_REQUIRED = 143;
    // id ресурса взят из TemplateInfoOverrides, чтобы проставилось свойство hidden для нужного ресурса
    public static final long TEMPLATE_5_RESOURCE_2_HIDDEN = 2774;

    public static final long TEMPLATE_6_RESOURCE_AGE = 144;

    // id ресурса взят из InternalBannerSender, для модерируемого шаблона PLACE_3_TEMPLATE_1
    public static final long TEMPLATE_7_RESOURCE = 3338;

    public static final long TEMPLATE_8_RESOURCE_1 = 145;
    public static final long TEMPLATE_8_RESOURCE_2 = 146;
    public static final long TEMPLATE_8_RESOURCE_3 = 147;
    public static final long TEMPLATE_8_RESOURCE_4 = 148;

    // id ресурсов взяты из Constants.MODERATED_TEMPLATES_RESOURCES_MAPPER, для модерируемого шаблона PLACE_3_TEMPLATE_2
    public static final long TEMPLATE_9_RESOURCE_LINK = 3278;
    public static final long TEMPLATE_9_RESOURCE_IMAGE = 3279;
    public static final long TEMPLATE_9_RESOURCE_TITLE1 = 3280;

    // id ресурсов взяты из Constants.MODERATED_TEMPLATES_RESOURCES_MAPPER, для модерируемого шаблона PLACE_3_TEMPLATE_3
    public static final long TEMPLATE_10_RESOURCE_TITLE1 = 3675;
    public static final long TEMPLATE_10_RESOURCE_TITLE2 = 3676;

    public static final String RESOURCE_DESCRIPTION = "ddd";

    private static final TemplateResource TEMPLATE_RESOURCE_1 =
            createResource(TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_1, TEMPLATE_1_RESOURCE_1_REQUIRED, 1L,
                    true, false, false, RESOURCE_DESCRIPTION);

    private static final TemplateResource TEMPLATE_RESOURCE_2 =
            createResource(TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_3_WITH_IMAGE,
                    TEMPLATE_3_RESOURCE_1_REQUIRED_IMAGE, 1L, true, true, false, RESOURCE_DESCRIPTION);

    private static final TemplateResource TEMPLATE_RESOURCE_3 =
            createResource(PLACE_1_TEMPLATE_4_URL_IMG, TEMPLATE_4_RESOURCE_1_IMAGE, 2L, false, true, false,
                    RESOURCE_DESCRIPTION);
    private static final TemplateResource TEMPLATE_RESOURCE_4 =
            createResource(PLACE_1_TEMPLATE_4_URL_IMG, TEMPLATE_4_RESOURCE_2_REQUIRED_URL, 1L, true, false, true,
                    RESOURCE_DESCRIPTION);

    private static final TemplateResource TEMPLATE_RESOURCE_5 =
            createResource(PLACE_2_TEMPLATE_2_WITH_HIDDEN_RESOURCE, TEMPLATE_5_RESOURCE_1_REQUIRED, 1L, true, false,
                    true, RESOURCE_DESCRIPTION);
    private static final TemplateResource TEMPLATE_RESOURCE_6 =
            createResource(PLACE_2_TEMPLATE_2_WITH_HIDDEN_RESOURCE, TEMPLATE_5_RESOURCE_2_HIDDEN, 50L, false, false,
                    false, "hiddenResource1");

    private static final TemplateResource TEMPLATE_RESOURCE_7 =
            createResource(PLACE_1_TEMPLATE_WITH_AGE_VARIABLE, TEMPLATE_6_RESOURCE_AGE, 1L, false, false,
                    false, "Возрастная метка");

    private static final TemplateResource TEMPLATE_RESOURCE_8 =
            createResource(PLACE_3_TEMPLATE_1, TEMPLATE_7_RESOURCE, 1L, false, false,
                    false, RESOURCE_DESCRIPTION);

    private static final TemplateResource TEMPLATE_RESOURCE_9 =
            createResource(PLACE_3_TEMPLATE_2, TEMPLATE_9_RESOURCE_LINK, 1L, true, false,
                    true, "link");
    private static final TemplateResource TEMPLATE_RESOURCE_10 =
            createResource(PLACE_3_TEMPLATE_2, TEMPLATE_9_RESOURCE_IMAGE, 1L, true, true,
                    false, "image");
    private static final TemplateResource TEMPLATE_RESOURCE_11 =
            createResource(PLACE_3_TEMPLATE_2, TEMPLATE_9_RESOURCE_TITLE1, 1L, true, false,
                    false, "title1");

    private static final TemplateResource TEMPLATE_RESOURCE_12 =
            createResource(PLACE_1_TEMPLATE_5_WITH_MANY_RESOURCES, TEMPLATE_8_RESOURCE_1, 1L, true, false,
                    false, String.valueOf(TEMPLATE_8_RESOURCE_1));
    private static final TemplateResource TEMPLATE_RESOURCE_13 =
            createResource(PLACE_1_TEMPLATE_5_WITH_MANY_RESOURCES, TEMPLATE_8_RESOURCE_2, 1L, false, false,
                    false, String.valueOf(TEMPLATE_8_RESOURCE_2));
    private static final TemplateResource TEMPLATE_RESOURCE_14 =
            createResource(PLACE_1_TEMPLATE_5_WITH_MANY_RESOURCES, TEMPLATE_8_RESOURCE_3, 1L, false, false,
                    false, String.valueOf(TEMPLATE_8_RESOURCE_3));
    private static final TemplateResource TEMPLATE_RESOURCE_15 =
            createResource(PLACE_1_TEMPLATE_5_WITH_MANY_RESOURCES, TEMPLATE_8_RESOURCE_4, 1L, false, false,
                    false, String.valueOf(TEMPLATE_8_RESOURCE_4));

    private static final TemplateResource TEMPLATE_RESOURCE_16 =
            createResource(PLACE_3_TEMPLATE_3, TEMPLATE_10_RESOURCE_TITLE1, 1L, false, false,
                    false, String.valueOf(TEMPLATE_10_RESOURCE_TITLE1));
    private static final TemplateResource TEMPLATE_RESOURCE_17 =
            createResource(PLACE_3_TEMPLATE_3, TEMPLATE_10_RESOURCE_TITLE2, 1L, false, false,
                    false, String.valueOf(TEMPLATE_10_RESOURCE_TITLE2));

    // Блок Unified template ресурсов
    private static final long UNIFIED_TEMPLATE_ID = 3350; // Константа
    private static final long TEMPLATE_RESOURCE_NO_7 = 7;
    private static final long TEMPLATE_RESOURCE_NO_17 = 17;
    private static final long TEMPLATE_RESOURCE_NO_67 = 67;
    private static final long TEMPLATE_RESOURCE_NO_73 = 73;
    private static final long UNIFIED_TEMPLATE_RESOURCE_ID_1 = 5970; // Взято у resourceNo=7
    private static final long UNIFIED_TEMPLATE_RESOURCE_ID_2 = 5980; // Взято у resourceNo=17
    private static final long UNIFIED_TEMPLATE_RESOURCE_ID_3 = 6030; // Взято у resourceNo=67
    private static final long UNIFIED_TEMPLATE_RESOURCE_ID_4 = 6036; // Взято у resourceNo=73
    private static final String UNIFIED_TEMPLATE_RESOURCE_DESCRIPTION_1 = "Заголовок (2 строка)"; //Взято у resourceNo=7
    private static final String UNIFIED_TEMPLATE_RESOURCE_DESCRIPTION_2 = "Картинка"; //Взято у resourceNo=17
    private static final String UNIFIED_TEMPLATE_RESOURCE_DESCRIPTION_3 = "Трекинговая ссылка"; //Взято у resourceNo=67
    private static final String UNIFIED_TEMPLATE_RESOURCE_DESCRIPTION_4 = "Платформа"; //Взято у resourceNo=67
    private static final TemplateResource UNIFIED_TEMPLATE_RESOURCE_NO_7 =
            createResource(UNIFIED_TEMPLATE_ID, UNIFIED_TEMPLATE_RESOURCE_ID_1, 70, false, false,
                    false, UNIFIED_TEMPLATE_RESOURCE_DESCRIPTION_1)
                    .withResourceNo(TEMPLATE_RESOURCE_NO_7);
    private static final TemplateResource UNIFIED_TEMPLATE_RESOURCE_NO_17 =
            createResource(UNIFIED_TEMPLATE_ID, UNIFIED_TEMPLATE_RESOURCE_ID_2, 170, false, true,
                    false, UNIFIED_TEMPLATE_RESOURCE_DESCRIPTION_2)
                    .withResourceNo(TEMPLATE_RESOURCE_NO_17);
    private static final TemplateResource UNIFIED_TEMPLATE_RESOURCE_NO_67 =
            createResource(UNIFIED_TEMPLATE_ID, UNIFIED_TEMPLATE_RESOURCE_ID_3, 670, false, false,
                    true, UNIFIED_TEMPLATE_RESOURCE_DESCRIPTION_3)
                    .withResourceNo(TEMPLATE_RESOURCE_NO_67);
    private static final TemplateResource UNIFIED_TEMPLATE_RESOURCE_NO_73 =
            createResource(UNIFIED_TEMPLATE_ID, UNIFIED_TEMPLATE_RESOURCE_ID_4, 730, false, false,
                    false, UNIFIED_TEMPLATE_RESOURCE_DESCRIPTION_4)
                    .withResourceNo(TEMPLATE_RESOURCE_NO_73);

    public static TemplateResourceYtRepository createYtRepositoryMock() {
        var all = asList(TEMPLATE_RESOURCE_1, TEMPLATE_RESOURCE_2, TEMPLATE_RESOURCE_3, TEMPLATE_RESOURCE_4);
        return new TemplateResourceYtRepository(mock(YtProvider.class), mock(InternalYaBsClusterChooser.class)) {
            @Override
            public List<TemplateResource> getAll() {
                return all;
            }
        };
    }

    public static TemplateResourceRepository createMySqlRepositoryMock() {
        List<TemplateResource> templateResources = asList(TEMPLATE_RESOURCE_1, TEMPLATE_RESOURCE_2,
                TEMPLATE_RESOURCE_3, TEMPLATE_RESOURCE_4, TEMPLATE_RESOURCE_5, TEMPLATE_RESOURCE_6,
                TEMPLATE_RESOURCE_7, TEMPLATE_RESOURCE_8, TEMPLATE_RESOURCE_9, TEMPLATE_RESOURCE_10,
                TEMPLATE_RESOURCE_11, TEMPLATE_RESOURCE_12, TEMPLATE_RESOURCE_13, TEMPLATE_RESOURCE_14,
                TEMPLATE_RESOURCE_15, TEMPLATE_RESOURCE_16, TEMPLATE_RESOURCE_17,
                UNIFIED_TEMPLATE_RESOURCE_NO_7, UNIFIED_TEMPLATE_RESOURCE_NO_17, UNIFIED_TEMPLATE_RESOURCE_NO_67,
                UNIFIED_TEMPLATE_RESOURCE_NO_73);

        return new TemplateResourceRepository(mock(DslContextProvider.class)) {
            @Override
            public List<TemplateResource> getAll() {
                return templateResources;
            }

            @Override
            public List<TemplateResource> getByIds(Collection<Long> ids) {
                return templateResources.stream().filter(t -> ids.contains(t.getId())).collect(Collectors.toList());
            }

            @Override
            public List<TemplateResource> getByTemplateIds(Collection<Long> templateIds) {
                return templateResources.stream()
                        .filter(t -> templateIds.contains(t.getTemplateId())).collect(Collectors.toList());
            }
        };
    }

    public static TemplateResource createResource(long templateId, long resourceId, long position, boolean isRequired,
                                                  boolean isImage, boolean isUrl, String description) {
        Set<TemplateResourceOption> options = new HashSet<>();
        if (isImage) {
            options.add(TemplateResourceOption.BANANA_IMAGE);
        }
        if (isUrl) {
            options.add(TemplateResourceOption.BANANA_URL);
        }
        if (isRequired) {
            options.add(TemplateResourceOption.REQUIRED);
        }
        return new TemplateResource()
                .withId(resourceId)
                .withTemplateId(templateId)
                .withTemplateCounterType(3L)
                .withTemplatePartNo(0L)
                .withDescription(description)
                .withResourceType(6L)
                .withResourceNo(TEMPLATE_RESOURCE_RESOURCE_NO)
                .withOptions(ImmutableSet.copyOf(options))
                .withPosition(position);
    }
}

package ru.yandex.direct.grid.processing.service.validation.presentation;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.direct.grid.processing.model.campaign.facelift.GdAddUpdateCampaignAdditionalData;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddAbstractCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddContentPromotionCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddDynamicCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddSmartCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddTextCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignEmailSettingsRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignNotificationRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignSmsSettingsRequest;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateAbstractCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateContentPromotionCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateDynamicCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateSmartCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateTextCampaign;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.PathNode;
import ru.yandex.direct.validation.result.PathNodeConverter;

/**
 * Тест, проверящий, что в {@link CampaignPathConverters} есть конвертация ошибок для всех полей, которые можно вернуть.
 */
@ParametersAreNonnullByDefault
public class CampaignPathConvertersTest {
    @Test
    public void checkTextCampaignPathConverter() throws NoSuchFieldException, IllegalAccessException {
        Set<String> fieldsAbsentInText = Set.of("brandSafety");
        PathNodeConverter converter = CampaignPathConverters.ADD_AND_UPDATE_TEXT_CAMPAIGN_PATH_CONVERTER;

        Field singleItemDictField = converter.getClass().getDeclaredField("singleItemDict");
        singleItemDictField.setAccessible(true);
        Map<PathNode.Field, Path> singleItemDict = (Map<PathNode.Field, Path>) singleItemDictField.get(converter);
        Set<String> allConverterValues = extractConverterValues(singleItemDict);

        Set<String> allGdFields = StreamEx.of(GdUpdateAbstractCampaign.class.getDeclaredFields())
                .append(GdAddAbstractCampaign.class.getDeclaredFields())
                .append(GdUpdateTextCampaign.class.getDeclaredFields())
                .append(GdAddTextCampaign.class.getDeclaredFields())
                .append(GdCampaignNotificationRequest.class.getDeclaredFields())
                .append(GdCampaignEmailSettingsRequest.class.getDeclaredFields())
                .append(GdCampaignSmsSettingsRequest.class.getDeclaredFields())
                .append(GdAddUpdateCampaignAdditionalData.class.getDeclaredFields())
                .remove(field -> Modifier.isStatic(field.getModifiers()))
                .remove(field -> fieldsAbsentInText.contains(field.getName()))
                .map(Field::getName)
                .toSet();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(allConverterValues).containsAll(allGdFields);
            // добавлено для того, чтобы не забыть какой-то новый Gd класс, появившийся в маппере
            softly.assertThat(allGdFields).containsAll(allConverterValues);
        });
    }

    @Test
    public void checkDynamicCampaignPathConverter() throws NoSuchFieldException, IllegalAccessException {
        Set<String> fieldsAbsentInDynamic = Set.of("companyName", "businessCategory", "brandSafety");
        PathNodeConverter converter = CampaignPathConverters.ADD_AND_UPDATE_DYNAMIC_CAMPAIGN_PATH_CONVERTER;

        Field singleItemDictField = converter.getClass().getDeclaredField("singleItemDict");
        singleItemDictField.setAccessible(true);
        Map<PathNode.Field, Path> singleItemDict = (Map<PathNode.Field, Path>) singleItemDictField.get(converter);
        Set<String> allConverterValues = extractConverterValues(singleItemDict);

        Set<String> allGdFields = StreamEx.of(GdUpdateAbstractCampaign.class.getDeclaredFields())
                .append(GdAddAbstractCampaign.class.getDeclaredFields())
                .append(GdUpdateDynamicCampaign.class.getDeclaredFields())
                .append(GdAddDynamicCampaign.class.getDeclaredFields())
                .append(GdCampaignNotificationRequest.class.getDeclaredFields())
                .append(GdCampaignEmailSettingsRequest.class.getDeclaredFields())
                .append(GdCampaignSmsSettingsRequest.class.getDeclaredFields())
                .append(GdAddUpdateCampaignAdditionalData.class.getDeclaredFields())
                .remove(field -> Modifier.isStatic(field.getModifiers()))
                .remove(field -> fieldsAbsentInDynamic.contains(field.getName()))
                .map(Field::getName)
                .toSet();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(allConverterValues).containsAll(allGdFields);
            // добавлено для того, чтобы не забыть какой-то новый Gd класс, появившийся в маппере
            softly.assertThat(allGdFields).containsAll(allConverterValues);
        });
    }

    @Test
    public void checkSmartCampaignPathConverter() throws NoSuchFieldException, IllegalAccessException {
        Set<String> fieldsAbsentInSmart = Set.of("checkPositionInterval", "companyName", "businessCategory",
                "brandSafety");
        PathNodeConverter converter = CampaignPathConverters.ADD_AND_UPDATE_SMART_CAMPAIGN_PATH_CONVERTER;

        Field singleItemDictField = converter.getClass().getDeclaredField("singleItemDict");
        singleItemDictField.setAccessible(true);
        Map<PathNode.Field, Path> singleItemDict = (Map<PathNode.Field, Path>) singleItemDictField.get(converter);
        Set<String> allConverterValues = extractConverterValues(singleItemDict);

        Set<String> allGdFields = StreamEx.of(GdUpdateAbstractCampaign.class.getDeclaredFields())
                .append(GdAddAbstractCampaign.class.getDeclaredFields())
                .append(GdUpdateSmartCampaign.class.getDeclaredFields())
                .append(GdAddSmartCampaign.class.getDeclaredFields())
                .append(GdCampaignNotificationRequest.class.getDeclaredFields())
                .append(GdCampaignEmailSettingsRequest.class.getDeclaredFields())
                .append(GdCampaignSmsSettingsRequest.class.getDeclaredFields())
                .append(GdAddUpdateCampaignAdditionalData.class.getDeclaredFields())
                .remove(field -> Modifier.isStatic(field.getModifiers()))
                .remove(field -> fieldsAbsentInSmart.contains(field.getName()))
                .map(Field::getName)
                .toSet();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(allConverterValues).containsAll(allGdFields);
            // добавлено для того, чтобы не забыть какой-то новый Gd класс, появившийся в маппере
            softly.assertThat(allGdFields).containsAll(allConverterValues);
        });
    }

    @Test
    public void checkContentPromotionCampaignPathConverter() throws NoSuchFieldException, IllegalAccessException {
        Set<String> fieldsAbsentInContentPromotion = Set.of("checkPositionInterval", "xlsReady",
                "companyName", "businessCategory", "brandSafety");
        PathNodeConverter converter = CampaignPathConverters.ADD_AND_UPDATE_CONTENT_PROMOTION_CAMPAIGN_PATH_CONVERTER;

        Field singleItemDictField = converter.getClass().getDeclaredField("singleItemDict");
        singleItemDictField.setAccessible(true);
        Map<PathNode.Field, Path> singleItemDict = (Map<PathNode.Field, Path>) singleItemDictField.get(converter);
        Set<String> allConverterValues = extractConverterValues(singleItemDict);

        Set<String> allGdFields = StreamEx.of(GdUpdateAbstractCampaign.class.getDeclaredFields())
                .append(GdAddAbstractCampaign.class.getDeclaredFields())
                .append(GdUpdateContentPromotionCampaign.class.getDeclaredFields())
                .append(GdAddContentPromotionCampaign.class.getDeclaredFields())
                .append(GdCampaignNotificationRequest.class.getDeclaredFields())
                .append(GdCampaignEmailSettingsRequest.class.getDeclaredFields())
                .append(GdCampaignSmsSettingsRequest.class.getDeclaredFields())
                .append(GdAddUpdateCampaignAdditionalData.class.getDeclaredFields())
                .remove(field -> Modifier.isStatic(field.getModifiers()))
                .remove(field -> fieldsAbsentInContentPromotion.contains(field.getName()))
                .map(Field::getName)
                .toSet();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(allConverterValues).containsAll(allGdFields);
            // добавлено для того, чтобы не забыть какой-то новый Gd класс, появившийся в маппере
            softly.assertThat(allGdFields).containsAll(allConverterValues);
        });
    }

    private static Set<String> extractConverterValues(Map<PathNode.Field, Path> singleItemDict) {
        return StreamEx.of(singleItemDict.values())
                .map(Path::getNodes)
                .flatMap(StreamEx::of)
                .select(PathNode.Field.class)
                .map(PathNode.Field::toString)
                .map(string -> string.split("\\."))
                .flatMap(StreamEx::of)
                .toSet();
    }
}

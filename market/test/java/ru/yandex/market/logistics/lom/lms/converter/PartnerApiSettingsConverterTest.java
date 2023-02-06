package ru.yandex.market.logistics.lom.lms.converter;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.service.yt.dto.YtPartnerApiSettings;
import ru.yandex.market.logistics.lom.utils.YtTestUtils;
import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodDto;

@ParametersAreNonnullByDefault
@DisplayName("Конвертация API параметров партнера из моделей yt в модели lms")
class PartnerApiSettingsConverterTest extends AbstractTest {

    private final PartnerApiSettingsConverter partnerApiSettingsConverter = new PartnerApiSettingsConverter();

    private static final String METHOD_NAME = "saveTheWorld";

    @Test
    @SneakyThrows
    @DisplayName("Конвертация методов партнера с частично заполненными yt полями")
    void convertPartialModel() {
        YtPartnerApiSettings ytPartnerApiSettings = buildPartnerApiSettings(1L).setMethod(null);

        SettingsMethodDto settingsMethodDto = partnerApiSettingsConverter.convert(ytPartnerApiSettings);

        softly.assertThat(settingsMethodDto).isNotNull();
        softly.assertThat(settingsMethodDto.getPartnerId()).isEqualTo(ytPartnerApiSettings.getPartnerId());
        softly.assertThat(settingsMethodDto.getActive()).isEqualTo(ytPartnerApiSettings.getActive());
        softly.assertThat(settingsMethodDto.getMethod()).isNull();
    }

    @Test
    @SneakyThrows
    @DisplayName("Конвертация методов партнера со всеми заполненными в yt полями")
    void convertFullModel() {
        YtPartnerApiSettings ytPartnerApiSettings = buildPartnerApiSettings(1L).setMethod(null);

        SettingsMethodDto settingsMethodDto = partnerApiSettingsConverter.convert(ytPartnerApiSettings);

        softly.assertThat(settingsMethodDto).isNotNull();
        softly.assertThat(settingsMethodDto.getPartnerId()).isEqualTo(ytPartnerApiSettings.getPartnerId());
        softly.assertThat(settingsMethodDto.getActive()).isEqualTo(ytPartnerApiSettings.getActive());
        softly.assertThat(settingsMethodDto.getMethod()).isEqualTo(ytPartnerApiSettings.getMethod());
    }

    @Test
    @SneakyThrows
    @DisplayName("Конвертация партнеров из итератора YT со всеми заполненными полями")
    void convertModelsFromYt() {
        List<Long> partnerIds = List.of(1L, 2L, 3L);

        Iterator<YTreeMapNode> iterator = YtTestUtils.getIterator(
            partnerIds.stream()
                .map(partnerId -> YtTestUtils.buildMapNode(buildPartnerApiSettingsMap(partnerId)))
                .collect(Collectors.toList())
        );

        List<SettingsMethodDto> settingsMethodDtos = partnerApiSettingsConverter.extractFromRows(iterator);

        for (int i = 0; i < partnerIds.size(); i++) {
            Long partnerId = partnerIds.get(i);
            SettingsMethodDto partnerLightModel = settingsMethodDtos.get(i);

            softly.assertThat(partnerLightModel).isNotNull();
            softly.assertThat(partnerLightModel.getPartnerId()).isEqualTo(partnerId);
            softly.assertThat(partnerLightModel.getMethod()).isEqualTo(METHOD_NAME);
            softly.assertThat(partnerLightModel.getActive()).isEqualTo(true);
        }
    }

    @Nonnull
    public static YtPartnerApiSettings buildPartnerApiSettings(long id) {
        return new YtPartnerApiSettings()
            .setPartnerId(id)
            .setMethod("saveTheWorld")
            .setActive(true);
    }

    @Nonnull
    public static Map<String, ?> buildPartnerApiSettingsMap(long id) {
        return Map.ofEntries(
            Map.entry("partner_id", id),
            Map.entry("method", "saveTheWorld"),
            Map.entry("active", true)
        );
    }
}

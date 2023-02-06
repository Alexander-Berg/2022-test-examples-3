package ru.yandex.market.logistics.lom.lms.converter;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.lms.model.PartnerLightModel;
import ru.yandex.market.logistics.lom.service.yt.dto.YtPartner;
import ru.yandex.market.logistics.lom.utils.YtTestUtils;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

@ParametersAreNonnullByDefault
@DisplayName("Конвертация партнера из моделей yt в модели lms")
class PartnerYtToLmsConverterTest extends AbstractTest {

    private final PartnerYtToLmsConverter partnerYtToLmsConverter = new PartnerYtToLmsConverter(objectMapper);

    private static final String PARTNER_NAME = "Name";
    private static final String PARTNER_READABLE_NAME = "Partner name";
    private static final String PARTNER_DOMAIN = "Domain";
    private static final String FIRST_PARAM_KEY = "IS_COMMON";
    private static final String SECOND_PARAM_KEY = "IS_GLOBAL";
    private static final String THIRD_PARAM_KEY = "AVAILABLE_TO_ALL";
    private static final String TRUE_PARAM_VALUE = "1";
    private static final String FALSE_PARAM_VALUE = "0";

    @Test
    @SneakyThrows
    @DisplayName("Конвертация партнера с частично заполненными yt полями")
    void convertPartialModel() {
        long partnerId = 1L;
        YtPartner partner = new YtPartner()
            .setId(partnerId)
            .setMarketId(Optional.empty())
            .setName(Optional.empty())
            .setReadableName(Optional.empty())
            .setSubtypeId(Optional.empty())
            .setBillingClientId(Optional.empty())
            .setDomain(Optional.empty())
            .setExternalParams("{\"external_params\":[]}");

        PartnerLightModel partnerLightModel = partnerYtToLmsConverter.convert(partner);

        softly.assertThat(partnerLightModel).isNotNull();
        softly.assertThat(partnerLightModel.getId()).isEqualTo(partnerId);
        softly.assertThat(partnerLightModel.getMarketId()).isNull();
        softly.assertThat(partnerLightModel.getName()).isNull();
        softly.assertThat(partnerLightModel.getReadableName()).isNull();
        softly.assertThat(partnerLightModel.getSubtype()).isNull();
        softly.assertThat(partnerLightModel.getBillingClientId()).isNull();
        softly.assertThat(partnerLightModel.getDomain()).isNull();
        softly.assertThat(partnerLightModel.getParams()).isEmpty();
    }

    @Test
    @SneakyThrows
    @DisplayName("Конвертация партнера со всеми заполненными в yt полями")
    void convertFullModel() {
        long partnerId = 1L;
        YtPartner partner = buildDeliveryPartner(partnerId);

        PartnerLightModel partnerLightModel = partnerYtToLmsConverter.convert(partner);

        softly.assertThat(partnerLightModel).isNotNull();
        softly.assertThat(partnerLightModel.getId()).isEqualTo(partnerId);
        softly.assertThat(partnerLightModel.getMarketId()).isEqualTo(partnerId + 1);
        softly.assertThat(partnerLightModel.getPartnerType()).isEqualTo(PartnerType.DELIVERY);
        softly.assertThat(partnerLightModel.getSubtype().getId()).isEqualTo(partnerId);
        softly.assertThat(partnerLightModel.getName()).isEqualTo(PARTNER_NAME);
        softly.assertThat(partnerLightModel.getReadableName()).isEqualTo(PARTNER_READABLE_NAME);
        softly.assertThat(partnerLightModel.getBillingClientId()).isEqualTo(partnerId);
        softly.assertThat(partnerLightModel.getDomain()).isEqualTo(PARTNER_DOMAIN);
        softly.assertThat(partnerLightModel.getParams()).isEqualTo(List.of(
            new PartnerExternalParam(FIRST_PARAM_KEY, null, TRUE_PARAM_VALUE),
            new PartnerExternalParam(SECOND_PARAM_KEY, null, TRUE_PARAM_VALUE),
            new PartnerExternalParam(THIRD_PARAM_KEY, null, FALSE_PARAM_VALUE)
        ));
    }

    @Test
    @SneakyThrows
    @DisplayName("Конвертация партнера из итератора YT со всеми заполненными полями")
    void convertModelFromYtByIterator() {
        Long partnerId = 1L;

        Iterator<YTreeMapNode> iterator =
            YtTestUtils.getIterator(List.of(YtTestUtils.buildMapNode(buildPartnerMap(partnerId))));

        Optional<PartnerLightModel> partnerLightModelOptional = partnerYtToLmsConverter.extractFromRow(iterator);

        softly.assertThat(partnerLightModelOptional).isPresent();
        PartnerLightModel partnerLightModel = partnerLightModelOptional.get();

        softly.assertThat(partnerLightModel.getId()).isEqualTo(partnerId);
        softly.assertThat(partnerLightModel.getMarketId()).isEqualTo(partnerId);
        softly.assertThat(partnerLightModel.getPartnerType()).isEqualTo(PartnerType.DELIVERY);
        softly.assertThat(partnerLightModel.getSubtype().getId()).isEqualTo(partnerId);
        softly.assertThat(partnerLightModel.getName()).isEqualTo(PARTNER_NAME);
        softly.assertThat(partnerLightModel.getReadableName()).isEqualTo(PARTNER_READABLE_NAME);
        softly.assertThat(partnerLightModel.getBillingClientId()).isEqualTo(partnerId);
        softly.assertThat(partnerLightModel.getDomain()).isEqualTo(PARTNER_DOMAIN);
        softly.assertThat(partnerLightModel.getParams()).containsExactlyInAnyOrderElementsOf(List.of(
            new PartnerExternalParam(FIRST_PARAM_KEY, null, TRUE_PARAM_VALUE),
            new PartnerExternalParam(SECOND_PARAM_KEY, null, TRUE_PARAM_VALUE),
            new PartnerExternalParam(THIRD_PARAM_KEY, null, FALSE_PARAM_VALUE)
        ));
    }

    @Test
    @SneakyThrows
    @DisplayName("Конвертация партнера из пустого итератора YT")
    void convertModelFromYtFromEmptyIterator() {
        Iterator<YTreeMapNode> iterator = YtTestUtils.getIterator(List.of());
        softly.assertThat(partnerYtToLmsConverter.extractFromRow(iterator)).isEmpty();
    }

    @Test
    @SneakyThrows
    @DisplayName("Конвертация партнеров из итератора YT со всеми заполненными полями")
    void convertModelsFromYt() {
        List<Long> partnerIds = List.of(1L, 2L, 3L);

        Iterator<YTreeMapNode> iterator = YtTestUtils.getIterator(
            partnerIds.stream()
                .map(partnerId -> YtTestUtils.buildMapNode(buildPartnerMap(partnerId)))
                .collect(Collectors.toList())
        );

        List<PartnerLightModel> partnerLightModels = partnerYtToLmsConverter.extractFromRows(iterator);

        for (int i = 0; i < partnerIds.size(); i++) {
            Long partnerId = partnerIds.get(i);
            PartnerLightModel partnerLightModel = partnerLightModels.get(i);

            softly.assertThat(partnerLightModel).isNotNull();
            softly.assertThat(partnerLightModel.getId()).isEqualTo(partnerId);
            softly.assertThat(partnerLightModel.getMarketId()).isEqualTo(partnerId);
            softly.assertThat(partnerLightModel.getPartnerType()).isEqualTo(PartnerType.DELIVERY);
            softly.assertThat(partnerLightModel.getSubtype().getId()).isEqualTo(partnerId);
            softly.assertThat(partnerLightModel.getName()).isEqualTo(PARTNER_NAME);
            softly.assertThat(partnerLightModel.getReadableName()).isEqualTo(PARTNER_READABLE_NAME);
            softly.assertThat(partnerLightModel.getBillingClientId()).isEqualTo(partnerId);
            softly.assertThat(partnerLightModel.getDomain()).isEqualTo(PARTNER_DOMAIN);
            softly.assertThat(partnerLightModel.getParams()).containsExactlyInAnyOrderElementsOf(List.of(
                new PartnerExternalParam(FIRST_PARAM_KEY, null, TRUE_PARAM_VALUE),
                new PartnerExternalParam(SECOND_PARAM_KEY, null, TRUE_PARAM_VALUE),
                new PartnerExternalParam(THIRD_PARAM_KEY, null, FALSE_PARAM_VALUE)
            ));
        }
    }

    @Nonnull
    private YtPartner buildDeliveryPartner(long id) {
        return new YtPartner()
            .setId(id)
            .setMarketId(Optional.of(id + 1))
            .setName(Optional.of(PARTNER_NAME))
            .setReadableName(Optional.of(PARTNER_READABLE_NAME))
            .setType(PartnerType.DELIVERY)
            .setSubtypeId(Optional.of(id))
            .setBillingClientId(Optional.of(id))
            .setDomain(Optional.of(PARTNER_DOMAIN))
            .setExternalParams(buildExternalParamsJson());
    }

    @Nonnull
    public static Map<String, ?> buildPartnerMap(long id) {
        return Map.ofEntries(
            Map.entry("id", id),
            Map.entry("market_id", id),
            Map.entry("name", "Name"),
            Map.entry("readable_name", "Partner name"),
            Map.entry("type", PartnerType.DELIVERY.name()),
            Map.entry("subtype_id", id),
            Map.entry("billing_client_id", id),
            Map.entry("domain", "Domain"),
            Map.entry("external_params", buildExternalParamsJson())
        );
    }

    @Nonnull
    private static String buildExternalParamsJson() {
        return "{\"external_params\":["
            + "{\"key\":\"IS_COMMON\",\"value\":\"1\"},"
            + "{\"key\":\"IS_GLOBAL\",\"value\":\"1\"},"
            + "{\"key\":\"AVAILABLE_TO_ALL\",\"value\":\"0\"}"
            + "]}";
    }
}

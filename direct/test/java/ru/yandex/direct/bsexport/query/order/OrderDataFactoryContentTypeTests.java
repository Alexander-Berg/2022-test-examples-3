package ru.yandex.direct.bsexport.query.order;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.direct.bsexport.model.ContentType;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithCampaignType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.direct.bsexport.query.order.OrderDataFactory.contentTypeMapper;
import static ru.yandex.direct.bsexport.query.order.OrderDataFactory.getContentType;
import static ru.yandex.direct.core.entity.campaign.model.CampaignType.CONTENT_PROMOTION;
import static ru.yandex.direct.core.entity.campaign.model.CampaignType.MCB;

class OrderDataFactoryContentTypeTests {

    /**
     * Проверяем что любой тип кампании имеет отправляемое в БК значение ContentType.
     * <p>
     * При появлении новых значений - следует добавить тип в enum ContentType
     * https://a.yandex-team.ru/arc/trunk/arcadia/direct/libs/bstransport/proto/order_data.proto
     * затем перегенерировать проект и добавить его (тип) в маппер.
     * <p>
     * Про исключения комментарий записан в маппере.
     */
    @ParameterizedTest
    @EnumSource(value = CampaignType.class, mode = EnumSource.Mode.EXCLUDE, names = {
            "MCB", "BILLING_AGGREGATE"
    })
    void allAttributionModelHasMappedValue(CampaignType campaignType) {
        assertDoesNotThrow(() -> contentTypeMapper(campaignType),
                "должен быть определен ContentType, соответствующий типу кампании");
    }

    @Test
    void contentPromotionTypeMappedToText() {
        assertThat(contentTypeMapper(CONTENT_PROMOTION)).isEqualTo(ContentType.text);
    }

    @ParameterizedTest
    @EnumSource(value = CampaignType.class, mode = EnumSource.Mode.MATCH_ALL, names = {
            "^(?!MCB|BILLING_AGGREGATE|CONTENT_PROMOTION).*", "^(?!CPM_).*"
    })
    void mappedContentTypeDbValue(CampaignType campaignType) {
        String dbCampaignType = CampaignType.toSource(campaignType).getLiteral();
        assertThat(contentTypeMapper(campaignType).name())
                .describedAs("отправляемое в БК значение соответствует хранимому в базе Директа")
                .isEqualTo(dbCampaignType);
    }

    /**
     * Исторически сложилось, что все охватные продукты отправляются как reach.
     * Но это вовсе не означает, что новые cpm-типы обязательно должны отправляться именно так.
     * Правильное значение - определяет БК.
     */
    @ParameterizedTest
    @EnumSource(value = CampaignType.class, mode = EnumSource.Mode.MATCH_ANY, names = "^CPM_.*")
    void cpmTypesMappedToReach(CampaignType campaignType) {
        assertThat(contentTypeMapper(campaignType).name())
                .describedAs("для охватных продуктов в БК отправляется тип reach")
                .isEqualTo("reach");
    }

    @ParameterizedTest
    @EnumSource(value = CampaignType.class, mode = EnumSource.Mode.EXCLUDE, names = {
            "MCB", "BILLING_AGGREGATE"
    })
    void getContentType_sameAsContentTypeMapper(CampaignType campaignType) {
        CampaignWithCampaignType campaign = new CampaignWithTypeImpl().withType(campaignType);
        assertThat(getContentType(campaign))
                .describedAs("getContentType дает такой же результат, что и contentTypeMapper")
                .isEqualByComparingTo(contentTypeMapper(campaignType));
    }

    @Test
    void contentTypeMapper_throwsExceptionForUnsupportedTypes() {
        CampaignWithCampaignType campaign = new CampaignWithTypeImpl().withType(MCB);
        assertThrows(IllegalStateException.class, () -> getContentType(campaign));
    }
}

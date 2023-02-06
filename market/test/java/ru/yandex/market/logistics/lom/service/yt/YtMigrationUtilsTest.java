package ru.yandex.market.logistics.lom.service.yt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.configuration.properties.LmsYtProperties;
import ru.yandex.market.logistics.lom.service.yt.dto.YtMigrationModel;
import ru.yandex.market.logistics.lom.service.yt.util.YtMigrationUtils;

@DisplayName("Получение класса модели в yt по названию таблицы")
class YtMigrationUtilsTest extends AbstractTest {

    @Test
    @DisplayName("Все проперти не заполнены")
    void createFromEmptyProperties() {
        softly.assertThat(YtMigrationUtils.getAvailableTablesForMigration(new LmsYtProperties(), null))
            .isEmpty();
    }

    @Test
    @DisplayName("Все проперти - пустые строки")
    void createFromBlankProperties() {
        LmsYtProperties lmsYtProperties = new LmsYtProperties();
        lmsYtProperties.setCommonPath("");
        lmsYtProperties.setLogisticsPointsAggPath("");
        lmsYtProperties.setSchedulePath("");
        lmsYtProperties.setPhonesPath("");
        lmsYtProperties.setLogisticsPointPath("");
        lmsYtProperties.setPartnerPath("");
        lmsYtProperties.setPartnerExternalParamPath("");
        lmsYtProperties.setPartnerRelationPath("");
        lmsYtProperties.setPartnerRelationToPath("");
        lmsYtProperties.setInboundSchedulePath("");
        lmsYtProperties.setScheduleDayByIdPath("");

        softly.assertThat(YtMigrationUtils.getAvailableTablesForMigration(lmsYtProperties, "version"))
            .isEmpty();
    }

    @Test
    @DisplayName("Заполнены все проперти")
    void createFromFullProperties() {
        LmsYtProperties lmsYtProperties = new LmsYtProperties();
        lmsYtProperties.setCommonPath("//home/common/");
        lmsYtProperties.setLogisticsPointPath("logisticsPoints");
        lmsYtProperties.setPhonesPath("phones");
        lmsYtProperties.setLogisticsPointsAggPath("logisticsPointsAgg");
        lmsYtProperties.setSchedulePath("schedulePath");
        lmsYtProperties.setPartnerPath("partnerPath");
        lmsYtProperties.setPartnerExternalParamPath("partnerExternalParamPath");
        lmsYtProperties.setPartnerRelationPath("partnerRelationPath");
        lmsYtProperties.setPartnerRelationToPath("partnerRelationToPath");
        lmsYtProperties.setInboundSchedulePath("inboundSchedulePath");
        lmsYtProperties.setScheduleDayByIdPath("scheduleDayByIdPath");

        String version = "version";

        softly.assertThat(YtMigrationUtils.getAvailableTablesForMigration(lmsYtProperties, version))
            .hasSize(10)
            .containsOnlyKeys(
                lmsYtProperties.getLogisticsPointPath(version),
                lmsYtProperties.getPhonesPath(version),
                lmsYtProperties.getSchedulePath(version),
                lmsYtProperties.getLogisticsPointsAggPath(version),
                lmsYtProperties.getPartnerPath(version),
                lmsYtProperties.getPartnerExternalParamPath(version),
                lmsYtProperties.getPartnerRelationPath(version),
                lmsYtProperties.getPartnerRelationToPath(version),
                lmsYtProperties.getInboundSchedulePath(version),
                lmsYtProperties.getDynamicScheduleDayByIdPath(version)
            );
    }

    @Test
    @DisplayName("Возвращаемая мапа не изменяется")
    void mapIsUnmodifiable() {
        softly.assertThatCode(
            () -> YtMigrationUtils.getAvailableTablesForMigration(new LmsYtProperties(), "version")
                .put("new key", ((YtMigrationModel) () -> "some class").getClass())
        )
            .isInstanceOf(UnsupportedOperationException.class);
    }
}

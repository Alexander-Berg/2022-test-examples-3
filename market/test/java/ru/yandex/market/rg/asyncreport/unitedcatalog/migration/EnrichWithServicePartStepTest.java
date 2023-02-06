package ru.yandex.market.rg.asyncreport.unitedcatalog.migration;

import java.util.List;
import java.util.stream.Stream;

import Market.DataCamp.SyncAPI.SyncGetOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.business.migration.BusinessMigration;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.asyncreport.model.unitedcatalog.CopyOffersParams;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.DataCampStrollerConversions;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;

/**
 * Тесты для {@link EnrichWithServicePartStep}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class EnrichWithServicePartStepTest extends FunctionalTest {

    private static final long BUSINESS_ID = 2001;
    private static final long PARTNER_ID = 1001;
    private static final CopyOffersParams PARAMS = new CopyOffersParams(BUSINESS_ID, BUSINESS_ID, PARTNER_ID);

    @Autowired
    @Qualifier("dataCampMigrationClient")
    private DataCampClient dataCampClient;

    private EnrichWithServicePartStep enrichWithServicePartStep;

    @BeforeEach
    void init() {
        enrichWithServicePartStep = new EnrichWithServicePartStep(dataCampClient, PARAMS);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testData")
    @DisplayName("Проверка обогащения офферов данными из ЕОХ")
    void test(String name, String beforeProto, String datacampResponseProto, String afterProto) {
        BusinessMigration.MergeOffersRequestItem before = ProtoTestUtil.getProtoMessageByJson(
                BusinessMigration.MergeOffersRequestItem.class,
                "proto/" + beforeProto,
                getClass()
        );
        SyncGetOffer.GetUnitedOffersResponse datacampResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/" + datacampResponseProto,
                getClass()
        );
        BusinessMigration.MergeOffersRequestItem after = ProtoTestUtil.getProtoMessageByJson(
                BusinessMigration.MergeOffersRequestItem.class,
                "proto/" + afterProto,
                getClass()
        );

        Mockito.when(dataCampClient.searchBusinessOffers(any()))
                .thenReturn(DataCampStrollerConversions.fromStrollerResponse(datacampResponse));

        BusinessMigration.MergeOffersRequestItem.Builder toUpdate = before.toBuilder();
        enrichWithServicePartStep.accept(List.of(toUpdate));

        ProtoTestUtil.assertThat(toUpdate.build())
                .isEqualTo(after);
    }

    private static Stream<Arguments> testData() {
        return Stream.of(
                Arguments.of(
                        "Поля сервисной части в приоритете берутся из КИ",
                        "EnrichWithServicePartStepTest.withSupplyPlan.before.json",
                        "EnrichWithServicePartStepTest.withSupplyPlan.datacamp.json",
                        "EnrichWithServicePartStepTest.withSupplyPlan.after.json"
                ),
                Arguments.of(
                        "В ЕОХ сервисной части нет. Данные из КИ не меняются",
                        "EnrichWithServicePartStepTest.withSupplyPlan.before.json",
                        "EnrichWithServicePartStepTest.empty.datacamp.json",
                        "EnrichWithServicePartStepTest.withSupplyPlan.before.json"
                ),
                Arguments.of(
                        "Поля актуальной сервисной части в приоритете берутся из КИ",
                        "EnrichWithServicePartStepTest.withSupplyPlan.actual.before.json",
                        "EnrichWithServicePartStepTest.withSupplyPlan.actual.datacamp.json",
                        "EnrichWithServicePartStepTest.withSupplyPlan.actual.after.json"
                ),
                Arguments.of(
                        "В ЕОХ актуальной сервисной части нет. Данные из КИ не меняются",
                        "EnrichWithServicePartStepTest.withSupplyPlan.actual.before.json",
                        "EnrichWithServicePartStepTest.empty.datacamp.json",
                        "EnrichWithServicePartStepTest.withSupplyPlan.actual.before.json"
                ),
                Arguments.of(
                        "VAT копируется в новое поле",
                        "EnrichWithServicePartStepTest.withSupplyPlan.before.json",
                        "EnrichWithServicePartStepTest.withOldVat.datacamp.json",
                        "EnrichWithServicePartStepTest.withOldVat.after.json"
                )

        );
    }
}

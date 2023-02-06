package ru.yandex.market.adv.b2bmonetization.programs.interactor;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationMockServerTest;
import ru.yandex.market.adv.b2bmonetization.bonus.yt.entity.AdvProgramBonus;
import ru.yandex.market.adv.b2bmonetization.bonus.yt.entity.AdvProgramMultiBonus;
import ru.yandex.market.adv.b2bmonetization.campaign.yt.entity.FeeRecommendation;
import ru.yandex.market.adv.b2bmonetization.programs.yt.entity.TestCategory;
import ru.yandex.market.adv.b2bmonetization.programs.yt.entity.TestOffer;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.open.api.exception.MbiOpenApiClientResponseException;

@MockServerSettings(ports = 12233)
class ProgramActivateInteractorTest extends AbstractMonetizationMockServerTest {

    @Autowired
    private ProgramActivateInteractor programActivateInteractor;

    ProgramActivateInteractorTest(MockServerClient server) {
        super(server);
    }

    @DisplayName("Активация программ у партнеров, готовых к активации.")
    @DbUnitDataSet(
            before = "ProgramActivateInteractor/csv/programActivate_existReady_doActivate.before.csv",
            after = "ProgramActivateInteractor/csv/programActivate_existReady_doActivate.after.csv"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = AdvProgramBonus.class,
                    path = "//tmp/adv_unittest/programActivate_existReady_doActivate_adv_program_bonus"
            ),
            before = "ProgramActivateInteractor/json/yt/Bonus/" +
                    "programActivate_existReady_doActivate.before.json",
            after = "ProgramActivateInteractor/json/yt/Bonus/" +
                    "programActivate_existReady_doActivate.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = AdvProgramMultiBonus.class,
                    path = "//tmp/adv_unittest/programActivate_existReady_doActivate_adv_program_multi_bonus",
                    ignoreColumns = "bonusId"
            ),
            before = "ProgramActivateInteractor/json/yt/MultiBonus/" +
                    "programActivate_existReady_doActivate.before.json",
            after = "ProgramActivateInteractor/json/yt/MultiBonus/" +
                    "programActivate_existReady_doActivate.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TestOffer.class,
                    path = "//tmp/adv_unittest/programActivate_existReady_doActivate_shop_offer"
            ),
            before = "ProgramActivateInteractor/json/yt/Offer/" +
                    "programActivate_existReady_doActivate.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TestCategory.class,
                    path = "//tmp/adv_unittest/programActivate_existReady_doActivate_shop_category"
            ),
            before = "ProgramActivateInteractor/json/yt/Category/" +
                    "programActivate_existReady_doActivate.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = FeeRecommendation.class,
                    path = "//tmp/adv_unittest/programActivate_existReady_doActivate_fee_recommendation"
            )
    )
    @Test
    public void programActivate_existReady_doActivate() {
        run("adv_unittest/programActivate_existReady_doActivate_", () -> {
            mockServerPath(
                    "POST",
                    "/api/v1/public/autostrategy/batch",
                    "ProgramActivateInteractor/json/request/programActivate_existReady_doActivate.json",
                    Map.of(
                            "shopId", List.of("1527"),
                            "autoStrategyTarget", List.of("white"),
                            "uid", List.of("523"),
                            "lastOrder", List.of("true")
                    ),
                    200,
                    "ProgramActivateInteractor/json/response/programActivate_existReady_doActivate.json"
            );
            mockServerPath("POST",
                    "/notification/partner",
                    "ProgramActivateInteractor/json/request/notificationPartnerActivated_1527.json",
                    Map.of(),
                    200,
                    "ProgramActivateInteractor/json/response/notificationPartnerActivated_1527.json"
            );
            mockServerPath("POST",
                    "/notification/partner",
                    "ProgramActivateInteractor/json/request/notificationPartnerActivated_85.json",
                    Map.of(),
                    200,
                    "ProgramActivateInteractor/json/response/notificationPartnerActivated_85.json"
            );
            mockServerPath("POST",
                    "/notification/partner",
                    "ProgramActivateInteractor/json/request/notificationPartnerActivated_95.json",
                    Map.of(),
                    200,
                    "ProgramActivateInteractor/json/response/notificationPartnerActivated_95.json"
            );

            Assertions.assertThatThrownBy(() -> programActivateInteractor.activateProgram())
                    .isInstanceOf(MbiOpenApiClientResponseException.class);

            mockServerVerify(
                    "POST",
                    "/notification/partner",
                    "ProgramActivateInteractor/json/request/notificationPartnerActivated_1527.json"
            );
            mockServerVerify(
                    "POST",
                    "/notification/partner",
                    "ProgramActivateInteractor/json/request/notificationPartnerActivated_85.json"
            );
            mockServerVerify(
                    "POST",
                    "/notification/partner",
                    "ProgramActivateInteractor/json/request/notificationPartnerActivated_95.json"
            );
        });
    }
}

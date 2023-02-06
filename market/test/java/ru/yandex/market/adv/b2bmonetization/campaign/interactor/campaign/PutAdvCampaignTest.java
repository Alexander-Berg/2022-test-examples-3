package ru.yandex.market.adv.b2bmonetization.campaign.interactor.campaign;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.adv.b2bmonetization.campaign.database.repository.offer.OfferFileTaskRepository;
import ru.yandex.market.adv.b2bmonetization.campaign.yt.entity.Campaign;
import ru.yandex.market.adv.b2bmonetization.campaign.yt.entity.CampaignHistory;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Date: 18.03.2022
 * Project: b2bmarketmonetization
 *
 * @author alexminakov
 */
@DisplayName("Тесты на endpoint PUT /v1/adv/campaign/{campaign_id}.")
@ParametersAreNonnullByDefault
class PutAdvCampaignTest extends AbstractMonetizationTest {

    @Autowired
    private OfferFileTaskRepository repository;

    @DisplayName("При неверных входных параметрах, вернулось 400.")
    @Test
    void v1AdvCampaignActivationCampaignIdPut_wrongParameters_badRequest() {
        run("v1AdvCampaignCampaignIdDelete_wrongParameters_badRequest_",
                () -> put("10", "gs", 400, "WHITE",
                        "v1AdvCampaignActivationCampaignIdPut_wrongParameters_badRequest")
        );
    }

    @DisplayName("При отсутствии excel файла, вернулось 404.")
    @Test
    @DbUnitDataSet(
            before = "PutAdvCampaign/csv/" +
                    "v1AdvCampaignActivationCampaignIdPut_excelFileNotFound_badRequest.csv",
            after = "PutAdvCampaign/csv/" +
                    "v1AdvCampaignActivationCampaignIdPut_excelFileNotFound_badRequest.csv"
    )
    void v1AdvCampaignActivationCampaignIdPut_excelFileNotFound_badRequest() {
        run("v1AdvCampaignActivationCampaignIdPut_excelFileNotFound_badRequest_",
                () -> put("412", "532", 404, "BLUE",
                        "v1AdvCampaignActivationCampaignIdPut_excelFileNotFound_badRequest")
        );
    }

    @DisplayName("Если excel файл уже привязан к кампании, вернулось 404.")
    @Test
    @DbUnitDataSet(
            before = "PutAdvCampaign/csv/" +
                    "v1AdvCampaignActivationCampaignIdPut_excelFileWithCampaign_notFound.csv",
            after = "PutAdvCampaign/csv/" +
                    "v1AdvCampaignActivationCampaignIdPut_excelFileWithCampaign_notFound.csv"
    )
    void v1AdvCampaignActivationCampaignIdPut_excelFileWithCampaign_notFound() {
        run("v1AdvCampaignActivationCampaignIdPut_excelFileWithCampaign_notFound_",
                () -> put("512", "532", 404, "BLUE",
                        "v1AdvCampaignActivationCampaignIdPut_excelFileWithCampaign_notFound")
        );
    }

    @DisplayName("Создаем новую компанию.")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Campaign.class,
                    path = "//tmp/v1AdvCampaignActivationCampaignIdPut_createCampaign_ok_" +
                            "adv_campaign"
            ),
            before = "PutAdvCampaign/json/yt/Campaign/" +
                    "v1AdvCampaignActivationCampaignIdPut_createCampaign_ok.before.json",
            after = "PutAdvCampaign/json/yt/Campaign/" +
                    "v1AdvCampaignActivationCampaignIdPut_createCampaign_ok.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = CampaignHistory.class,
                    path = "//tmp/v1AdvCampaignActivationCampaignIdPut_createCampaign_ok_" +
                            "adv_campaign_history",
                    ignoreColumns = {"actionId"}
            ),
            before = "PutAdvCampaign/json/yt/CampaignHistory/" +
                    "v1AdvCampaignActivationCampaignIdPut_createCampaign_ok.before.json",
            after = "PutAdvCampaign/json/yt/CampaignHistory/" +
                    "v1AdvCampaignActivationCampaignIdPut_createCampaign_ok.after.json"
    )
    @DbUnitDataSet(
            before = "PutAdvCampaign/csv/" +
                    "v1AdvCampaignActivationCampaignIdPut_createCampaign_ok.before.csv",
            after = "PutAdvCampaign/csv/" +
                    "v1AdvCampaignActivationCampaignIdPut_createCampaign_ok.after.csv"
    )
    void v1AdvCampaignActivationCampaignIdPut_createCampaign_ok() {
        run("v1AdvCampaignActivationCampaignIdPut_createCampaign_ok_",
                () -> {
                    try {
                        put("953", "543", 200, "WHITE",
                                "v1AdvCampaignActivationCampaignIdPut_createCampaign_ok")
                                .andExpect(
                                        jsonPath("$.data.file.url")
                                                .value(Matchers.endsWith("ru/yandex/market/adv/b2bmonetization/1.xlsm"))
                                );
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    Assertions.assertThat(
                                    repository.findActiveTasks(1L)
                                            .get(0)
                                            .getCampaignId()
                            )
                            .isEqualTo(953L);
                }
        );
    }

    @DisplayName("Обновляем существующую кампанию.")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Campaign.class,
                    path = "//tmp/v1AdvCampaignActivationCampaignIdPut_updateCampaign_ok_" +
                            "adv_campaign"
            ),
            before = "PutAdvCampaign/json/yt/Campaign/" +
                    "v1AdvCampaignActivationCampaignIdPut_updateCampaign_ok.before.json",
            after = "PutAdvCampaign/json/yt/Campaign/" +
                    "v1AdvCampaignActivationCampaignIdPut_updateCampaign_ok.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = CampaignHistory.class,
                    path = "//tmp/v1AdvCampaignActivationCampaignIdPut_updateCampaign_ok_" +
                            "adv_campaign_history",
                    ignoreColumns = {"actionId"}
            ),
            before = "PutAdvCampaign/json/yt/CampaignHistory/" +
                    "v1AdvCampaignActivationCampaignIdPut_updateCampaign_ok.before.json",
            after = "PutAdvCampaign/json/yt/CampaignHistory/" +
                    "v1AdvCampaignActivationCampaignIdPut_updateCampaign_ok.after.json"
    )
    @DbUnitDataSet(
            before = "PutAdvCampaign/csv/" +
                    "v1AdvCampaignActivationCampaignIdPut_updateCampaign_ok.before.csv",
            after = "PutAdvCampaign/csv/" +
                    "v1AdvCampaignActivationCampaignIdPut_updateCampaign_ok.after.csv"
    )
    void v1AdvCampaignActivationCampaignIdPut_updateCampaign_ok() {
        run("v1AdvCampaignActivationCampaignIdPut_updateCampaign_ok_",
                () -> put("421", "523", 200, "BLUE",
                        "v1AdvCampaignActivationCampaignIdPut_updateCampaign_ok")
        );
    }

    @DisplayName("Обновляем существующую кампанию если excel файл уже привязан к ней.")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Campaign.class,
                    path = "//tmp/v1AdvCampaignActivationCampaignIdPut_updateCampaignWithCurrentExcel_ok_" +
                            "adv_campaign"
            ),
            before = "PutAdvCampaign/json/yt/Campaign/" +
                    "v1AdvCampaignActivationCampaignIdPut_updateCampaignWithCurrentExcel_ok.before.json",
            after = "PutAdvCampaign/json/yt/Campaign/" +
                    "v1AdvCampaignActivationCampaignIdPut_updateCampaignWithCurrentExcel_ok.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = CampaignHistory.class,
                    path = "//tmp/v1AdvCampaignActivationCampaignIdPut_updateCampaignWithCurrentExcel_ok_" +
                            "adv_campaign_history",
                    ignoreColumns = {"actionId"}
            ),
            before = "PutAdvCampaign/json/yt/CampaignHistory/" +
                    "v1AdvCampaignActivationCampaignIdPut_updateCampaignWithCurrentExcel_ok.before.json",
            after = "PutAdvCampaign/json/yt/CampaignHistory/" +
                    "v1AdvCampaignActivationCampaignIdPut_updateCampaignWithCurrentExcel_ok.after.json"
    )
    @DbUnitDataSet(
            before = "PutAdvCampaign/csv/" +
                    "v1AdvCampaignActivationCampaignIdPut_updateCampaignWithCurrentExcel_ok.before.csv",
            after = "PutAdvCampaign/csv/" +
                    "v1AdvCampaignActivationCampaignIdPut_updateCampaignWithCurrentExcel_ok.after.csv"
    )
    void v1AdvCampaignActivationCampaignIdPut_updateCampaignWithCurrentExcel_ok() {
        run("v1AdvCampaignActivationCampaignIdPut_updateCampaignWithCurrentExcel_ok_",
                () -> put("849", "523", 200, "BLUE",
                        "v1AdvCampaignActivationCampaignIdPut_updateCampaignWithCurrentExcel_ok")
        );
    }

    @DisplayName("Обновляем существующую кампанию в статусе DEACTIVATING.")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Campaign.class,
                    path = "//tmp/v1AdvCampaignActivationCampaignIdPut_updateDeactivatingCampaign_ok_" +
                            "adv_campaign"
            ),
            before = "PutAdvCampaign/json/yt/Campaign/" +
                    "v1AdvCampaignActivationCampaignIdPut_updateDeactivatingCampaign_ok.before.json",
            after = "PutAdvCampaign/json/yt/Campaign/" +
                    "v1AdvCampaignActivationCampaignIdPut_updateDeactivatingCampaign_ok.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = CampaignHistory.class,
                    path = "//tmp/v1AdvCampaignActivationCampaignIdPut_updateDeactivatingCampaign_ok_" +
                            "adv_campaign_history",
                    ignoreColumns = {"actionId"}
            ),
            before = "PutAdvCampaign/json/yt/CampaignHistory/" +
                    "v1AdvCampaignActivationCampaignIdPut_updateDeactivatingCampaign_ok.before.json",
            after = "PutAdvCampaign/json/yt/CampaignHistory/" +
                    "v1AdvCampaignActivationCampaignIdPut_updateDeactivatingCampaign_ok.after.json"
    )
    @DbUnitDataSet(
            before = "PutAdvCampaign/csv/" +
                    "v1AdvCampaignActivationCampaignIdPut_updateDeactivatingCampaign_ok.before.csv",
            after = "PutAdvCampaign/csv/" +
                    "v1AdvCampaignActivationCampaignIdPut_updateDeactivatingCampaign_ok.after.csv"
    )
    void v1AdvCampaignActivationCampaignIdPut_updateDeactivatingCampaign_ok() {
        run("v1AdvCampaignActivationCampaignIdPut_updateDeactivatingCampaign_ok_",
                () -> put("4789", "523", 200, "BLUE",
                        "v1AdvCampaignActivationCampaignIdPut_updateDeactivatingCampaign_ok")
        );
    }

    @DisplayName("Обновляем существующую кампанию в статусе INACTIVE.")
    @Test
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Campaign.class,
                    path = "//tmp/v1AdvCampaignActivationCampaignIdPut_updateInactiveCampaign_ok_" +
                            "adv_campaign"
            ),
            before = "PutAdvCampaign/json/yt/Campaign/" +
                    "v1AdvCampaignActivationCampaignIdPut_updateInactiveCampaign_ok.before.json",
            after = "PutAdvCampaign/json/yt/Campaign/" +
                    "v1AdvCampaignActivationCampaignIdPut_updateInactiveCampaign_ok.after.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = CampaignHistory.class,
                    path = "//tmp/v1AdvCampaignActivationCampaignIdPut_updateInactiveCampaign_ok_" +
                            "adv_campaign_history",
                    ignoreColumns = {"actionId"}
            ),
            before = "PutAdvCampaign/json/yt/CampaignHistory/" +
                    "v1AdvCampaignActivationCampaignIdPut_updateInactiveCampaign_ok.before.json",
            after = "PutAdvCampaign/json/yt/CampaignHistory/" +
                    "v1AdvCampaignActivationCampaignIdPut_updateInactiveCampaign_ok.after.json"
    )
    @DbUnitDataSet(
            before = "PutAdvCampaign/csv/" +
                    "v1AdvCampaignActivationCampaignIdPut_updateInactiveCampaign_ok.before.csv",
            after = "PutAdvCampaign/csv/" +
                    "v1AdvCampaignActivationCampaignIdPut_updateInactiveCampaign_ok.after.csv"
    )
    void v1AdvCampaignActivationCampaignIdPut_updateInactiveCampaign_ok() {
        run("v1AdvCampaignActivationCampaignIdPut_updateInactiveCampaign_ok_",
                () -> put("1458", "523", 200, "BLUE",
                        "v1AdvCampaignActivationCampaignIdPut_updateInactiveCampaign_ok")
        );
    }

    private ResultActions put(String campaignId, String partnerId, int status, String color, String testName) {
        try {
            return mvcPerform(
                    HttpMethod.PUT,
                    new URIBuilder()
                            .setPath("/v1/adv/campaign/" + campaignId)
                            .addParameters(
                                    List.of(
                                            new BasicNameValuePair("uid", "4182"),
                                            new BasicNameValuePair("partner_id", partnerId),
                                            new BasicNameValuePair("color", color)
                                    )
                            )
                            .build()
                            .toString(),
                    status,
                    "PutAdvCampaign/json/response/" + testName + ".json",
                    "PutAdvCampaign/json/request/" + testName + ".json",
                    true
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

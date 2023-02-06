package ru.yandex.market.adv.b2bmonetization.campaign.interactor.campaign;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.hamcrest.Matchers;
import org.hamcrest.number.OrderingComparison;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.adv.b2bmonetization.campaign.yt.entity.ShopOffer;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Date: 16.03.2022
 * Project: b2bmarketmonetization
 *
 * @author alexminakov
 */
@DisplayName("Тесты на endpoint POST /v1/adv/campaign/file.")
@ParametersAreNonnullByDefault
class PostAdvCampaignFileTest extends AbstractMonetizationTest {

    @DisplayName("При попытке загрузить не excel файл, вернулось 400.")
    @Test
    void v1AdvCampaignFilePost_notExcelFile_badRequest() {
        run("v1AdvCampaignFilePost_notExcelFile_badRequest_",
                () -> post("6533", "412", "WHITE", "v1AdvCampaignFilePost_notExcelFile_badRequest", 400)
        );
    }

    @DisplayName("При попытке загрузить excel файл большого размера, вернулось 400.")
    @Test
    void v1AdvCampaignFilePost_oversizeFile_badRequest() {
        run("v1AdvCampaignFilePost_oversizeFile_badRequest_",
                () -> post("541", "417", "BLUE", "v1AdvCampaignFilePost_oversizeFile_badRequest", 400)
        );
    }

    @DisplayName("При неверных входных параметрах, вернулось 400.")
    @Test
    void v1AdvCampaignFilePost_wrongParameters_badRequest() {
        run("v1AdvCampaignFilePost_wrongParameters_badRequest_",
                () -> post("10", "gs", "BLUE", "v1AdvCampaignFilePost_wrongParameters_badRequest", 400)
        );
    }

    @DisplayName("При неверном формате excel файла ничего не удалось прочитать. Сохранили пустой файл.")
    @Test
    @DbUnitDataSet(after = "PostAdvCampaignFile/csv/v1AdvCampaignFilePost_wrongExcelFormat_nothing.after.csv")
    void v1AdvCampaignFilePost_wrongExcelFormat_nothing() {
        postOk("503", "543", "BLUE", "v1AdvCampaignFilePost_wrongExcelFormat_nothing");
    }

    @Test
    @DisplayName("При верном формате excel файла вычитали все данные. DBS. Сохранили файл.")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ShopOffer.class,
                    path = "//tmp/v1AdvCampaignFilePost_correctFile_allOffer_dbs_shop_offer"
            ),
            before = "PostAdvCampaignFile/json/yt/v1AdvCampaignFilePost_correctFile_allOffer_dbs.before.json"
    )
    @DbUnitDataSet(after = "PostAdvCampaignFile/csv/v1AdvCampaignFilePost_correctFile_allOffer_dbs.after.csv")
    void v1AdvCampaignFilePost_correctFile_allOffer_dbs() {
        postOk("553", "452", "WHITE", "v1AdvCampaignFilePost_correctFile_allOffer_dbs");
    }

    @Test
    @DisplayName("При верном формате excel файла вычитали все данные. FB. Сохранили файл.")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ShopOffer.class,
                    path = "//tmp/v1AdvCampaignFilePost_correctFile_allOffer_fb_blue_shop_offer"
            ),
            before = "PostAdvCampaignFile/json/yt/v1AdvCampaignFilePost_correctFile_allOffer_fb.before.json"
    )
    @DbUnitDataSet(after = "PostAdvCampaignFile/csv/v1AdvCampaignFilePost_correctFile_allOffer_fb.after.csv")
    void v1AdvCampaignFilePost_correctFile_allOffer_fb() {
        postOk("554", "453", "BLUE", "v1AdvCampaignFilePost_correctFile_allOffer_fb");
    }

    private void postOk(String uid, String partnerId, String color, String testName) {
        run(testName + "_",
                () -> {
                    try {
                        post(uid, partnerId, color, testName, 200)
                                .andExpect(
                                        jsonPath("$.file.id")
                                                .value(OrderingComparison.greaterThanOrEqualTo(1))
                                )
                                .andExpect(
                                        jsonPath("$.file.url")
                                                .value(Matchers.endsWith("ru/yandex/market/adv/b2bmonetization/1.xlsm"))
                                );
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    @Nonnull
    private ResultActions post(String uid, String partnerId, String color, String testName, int status) {
        try {
            return mvc.perform(
                            MockMvcRequestBuilders.multipart(
                                            new URIBuilder()
                                                    .setPath("/v1/adv/campaign/file")
                                                    .addParameters(
                                                            List.of(
                                                                    new BasicNameValuePair("uid", uid),
                                                                    new BasicNameValuePair("partner_id", partnerId),
                                                                    new BasicNameValuePair("color", color)
                                                            )
                                                    )
                                                    .build()
                                                    .toString()
                                    )
                                    .file(
                                            new MockMultipartFile(
                                                    "file",
                                                    "test.xlsm",
                                                    "application/vnd.ms-excel",
                                                    loadFileBinary("PostAdvCampaignFile/excel/" + testName + ".xlsm")
                                            )
                                    )
                    )
                    .andExpect(status().is(status))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(content().json(loadFile("PostAdvCampaignFile/json/response/" + testName + ".json")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

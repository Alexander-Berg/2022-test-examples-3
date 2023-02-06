package ru.yandex.market.adv.b2bmonetization.programs.interactor.newbie;

import java.net.URISyntaxException;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

@DisplayName("Тесты на endpoint GET /v1/programs")
@ParametersAreNonnullByDefault
class GetNewbieProgramApiServiceTest extends AbstractMonetizationTest {

    @SuppressWarnings("unused")
    @DisplayName("Партнер не участвовал в программе, не новый и поэтому не может ее принять.")
    @DbUnitDataSet(
            before = "Get/csv/v1ProgramGet_oldPartnerWithoutProgram_allFalse.before.csv"
    )
    @ParameterizedTest(name = "{2}")
    @CsvSource(value = {
            "773234,876543,FULFILLMENT - Not newbie",
            "778634,876543,DBS - Not newbie",
            "732634,876543,ADV",
            "543634,876543,CPC",
            "573634,876543,FULFILLMENT - DISABLED",
            "583634,876543,FULFILLMENT - FAIL",
            "593634,876543,UNKNOWN",
            "67541,1124,NEW in business",
            "67542,1125,ACTIVATED in business",
            "67543,1126,ENQUEUED in business"
    })
    void v1ProgramGet_oldPartnerWithoutProgram_allFalse(String shopId, String businessId,
                                                        String testName) {
        run("v1ProgramGet_oldPartnerWithoutProgram_allFalse_",
                () -> {
                    try {
                        getOk(shopId, businessId, "v1ProgramGet_oldPartnerWithoutProgram_allFalse.json");
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    @SuppressWarnings("unused")
    @DisplayName("Партнер не участвовал в программе, новый и может ее принять.")
    @DbUnitDataSet(
            before = "Get/csv/v1ProgramGet_newbie_canParticipate.before.csv"
    )
    @ParameterizedTest(name = "{2}")
    @CsvSource(value = {
            "96123,532316,Not existed",
            "773234,876543,Existed"
    })
    void v1ProgramGet_newbie_canParticipate(String shopId, String businessId,
                                            String testName) {
        run("v1ProgramGet_newbie_canParticipate_",
                () -> {
                    try {
                        getOk(shopId, businessId, "v1ProgramGet_newbie_canParticipate.json");
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    @DisplayName("Программа у партнера в статусе REFUSED, может участвовать")
    @DbUnitDataSet(
            before = "Get/csv/v1ProgramGet_inRefused_canParticipate.before.csv"
    )
    @Test
    void v1ProgramGet_inRefused_canParticipate() {
        run("v1ProgramGet_inRefused_canParticipate_",
                () -> {
                    try {
                        getOk("53415", "876543",
                                "v1ProgramGet_inRefused_canParticipate.json");
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    @DisplayName("Программа у партнера в статусе RESET, может участвовать")
    @DbUnitDataSet(
            before = "Get/csv/v1ProgramGet_inReset_canParticipate.before.csv"
    )
    @Test
    void v1ProgramGet_inReset_canParticipate() {
        run("v1ProgramGet_inReset_canParticipate_",
                () -> {
                    try {
                        getOk("53415", "876543", "v1ProgramGet_inReset_canParticipate.json");
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    @DisplayName("Программа у партнера в статусе ACTIVATED, не может участвовать")
    @DbUnitDataSet(
            before = "Get/csv/v1ProgramGet_inActivated_false.before.csv"
    )
    @Test
    void v1ProgramGet_inActivated_false() {
        run("v1ProgramGet_inActivated_false_",
                () -> {
                    try {
                        getOk("53415", "876543", "v1ProgramGet_inActivated_false.json");
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    @DisplayName("Программа у партнера в статусе NEW, не может участвовать")
    @DbUnitDataSet(
            before = "Get/csv/v1ProgramGet_inNew_processed.before.csv"
    )
    @Test
    void v1ProgramGet_inNew_processed() {
        run("v1ProgramGet_inNew_processed_",
                () -> {
                    try {
                        getOk("52935", "876543", "v1ProgramGet_inNew_processed.json");
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    @DisplayName("Программа у партнера в статусе ENQUEUED")
    @DbUnitDataSet(
            before = "Get/csv/v1ProgramGet_inEnqueued_processed.before.csv"
    )
    @Test
    void v1ProgramGet_inEnqueued_processed() {
        run("v1ProgramGet_inEnqueued_processed_",
                () -> {
                    try {
                        getOk("52454", "876543", "v1ProgramGet_inEnqueued_processed.json");
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    @DisplayName("Программа у партнера в рамках бизнеса в статусе READY_RESET, не может участвовать")
    @DbUnitDataSet(
            before = "Get/csv/v1ProgramGet_inReadyResetInBusiness_false.before.csv"
    )
    @Test
    void v1ProgramGet_inReadyResetInBusiness_false() {
        run("v1ProgramGet_inReadyResetInBusiness_false_",
                () -> {
                    try {
                        getOk("852", "876543",
                                "v1ProgramGet_inReadyResetInBusiness_false.json");
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    @DisplayName("Программа у партнера в рамках бизнеса в статусе READY_CREATION, не может участвовать")
    @DbUnitDataSet(
            before = "Get/csv/v1ProgramGet_inReadyCreationInBusiness_false.before.csv"
    )
    @Test
    void v1ProgramGet_inReadyCreationInBusiness_false() {
        run("v1ProgramGet_inReadyCreationInBusiness_false_",
                () -> {
                    try {
                        getOk("8522", "855543",
                                "v1ProgramGet_inReadyCreationInBusiness_false.json");
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    private void getOk(String shopId, String businessId, String fileName) throws URISyntaxException {
        mvcPerform(
                HttpMethod.GET,
                new URIBuilder()
                        .setPath("/v1/program")
                        .addParameter("programType", "NEWBIE")
                        .addParameter("color", "WHITE")
                        .addParameter("business_id", businessId)
                        .addParameter("partner_id", shopId)
                        .build()
                        .toString(),
                HttpStatus.OK.value(),
                "Get/json/response/" + fileName,
                null,
                true
        );
    }
}

package ru.yandex.market.replenishment.autoorder.validation;

import org.junit.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.model.DemandType;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@WithMockLogin
public class DoesntWorkDuringImportAspectTest extends ControllerTest {

    @Test
    @DbUnitDataSet(before = "DoesntWorkDuringImportAspectTest_loadStarted.before.csv")
    public void getDemandsReturnsErrorIfLoaderStarted_1p() throws Exception {
        getDemandsReturnsErrorIfLoaderStarted(DemandType.TYPE_1P);
    }

    @Test
    @DbUnitDataSet(before = "DoesntWorkDuringImportAspectTest_loadStarted3p.before.csv")
    public void getDemandsReturnsErrorIfLoaderStarted_3p() throws Exception {
        getDemandsReturnsErrorIfLoaderStarted(DemandType.TYPE_3P);
    }


    @Test
    @DbUnitDataSet(before = "DoesntWorkDuringImportAspectTest_loadStartedTender.before.csv")
    public void getDemandsReturnsErrorIfLoaderStarted_tender() throws Exception {
        getDemandsReturnsErrorIfLoaderStarted(DemandType.TENDER);
    }

    @Test
    @DbUnitDataSet(before = "DoesntWorkDuringImportAspectTest_loadDidiNotStart.before.csv")
    public void getDemandsReturnsOkIfLoaderDidNotStart_default() throws Exception {
        mockMvc.perform(get("/demands?demandType=TYPE_1P&dateType=DELIVERY&dateFrom=2019-03-15&dateTo=2019-03-23")
                .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "DoesntWorkDuringImportAspectTest_loadDidiNotStart.before.csv")
    public void getDemandsReturnsOkIfLoaderDidNotStart_1p() throws Exception {
        mockMvc.perform(get("/demands?demandType=TYPE_1P&dateType=DELIVERY&dateFrom=2019-03-15&dateTo=2019-03-23")
                .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "DoesntWorkDuringImportAspectTest_loadDidiNotStart.before.csv")
    public void getDemandsReturnsOkIfLoaderDidNotStart_3p() throws Exception {
        mockMvc.perform(get("/demands?demandType=TYPE_3P&dateType=DELIVERY&dateFrom=2019-03-15&dateTo=2019-03-23")
                .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "DoesntWorkDuringImportAspectTest_loadDidiNotStart.before.csv")
    public void getDemandsReturnsOkIfLoaderDidNotStart_tender() throws Exception {
        mockMvc.perform(get("/demands?demandType=TENDER&dateType=DELIVERY&dateFrom=2019-03-15&dateTo=2019-03-23")
                .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "DoesntWorkDuringImportAspectTest_loadStarted.before.csv")
    public void testMethodWithDemandTypeReturnsError_default() throws Exception {
        testMethodWithDemandTypeReturnsError(null);
    }

    @Test
    @DbUnitDataSet(before = "DoesntWorkDuringImportAspectTest_loadStarted.before.csv")
    public void testMethodWithDemandTypeReturnsError_1p() throws Exception {
        testMethodWithDemandTypeReturnsError(DemandType.TYPE_1P);
    }

    @Test
    @DbUnitDataSet(before = "DoesntWorkDuringImportAspectTest_loadStarted3p.before.csv")
    public void testMethodWithDemandTypeReturnsError_3p() throws Exception {
        testMethodWithDemandTypeReturnsError(DemandType.TYPE_3P);
    }

    @Test
    @DbUnitDataSet(before = "DoesntWorkDuringImportAspectTest_loadStartedTender.before.csv")
    public void testMethodWithDemandTypeReturnsError_tender() throws Exception {
        testMethodWithDemandTypeReturnsError(DemandType.TENDER);
    }

    private void testMethodWithDemandTypeReturnsError(DemandType demandType) throws Exception {
        String[] args = getParamAndTypeInMessage(demandType);

        String url = "/demands/1/delivery-date?" + args[0];
        String content = "{\"deliveryDate\":\"2020-09-12\"}";

        mockMvc.perform(put(url).contentType(APPLICATION_JSON_UTF8)
                .content(content))
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message").value("В 16:29:00 начался блокирующий работу этап импорта " +
                        "рекомендаций типа " +
                        args[1] +
                        ", пожалуйста, попробуйте позже (как правило блокирующий работу этап занимает несколько " +
                        "минут)"));
    }

    private void getDemandsReturnsErrorIfLoaderStarted(DemandType demandType) throws Exception {
        String[] args = getParamAndTypeInMessage(demandType);
        mockMvc.perform(get("/demands?dateType=DELIVERY&dateFrom=2019-03-15&dateTo=2019-03-23" + args[0])
                .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message").value("В 16:29:00 начался блокирующий работу этап импорта " +
                        "рекомендаций типа " +
                        args[1] +
                        ", пожалуйста, попробуйте позже (как правило блокирующий работу этап занимает несколько " +
                        "минут)"));
    }

    private static String[] getParamAndTypeInMessage(DemandType demandType) {
        if (demandType == null) {
            return new String[]{"", "1P"};
        }
        switch (demandType) {
            case TYPE_1P:
                return new String[]{"&demandType=TYPE_1P", "1P"};
            case TYPE_3P:
                return new String[]{"&demandType=TYPE_3P", "3P"};
            case TENDER:
                return new String[]{"&demandType=TENDER", "TENDER"};
            default:
                return new String[]{"", "1P"};
        }
    }
}

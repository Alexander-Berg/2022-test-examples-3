package ru.yandex.market.partnertype;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.common.util.xmlrpc.XmlRPCServceExecutor;
import ru.yandex.market.billing.tasks.distribution.DistributionClient;
import ru.yandex.market.common.balance.xmlrpc.Balance2;
import ru.yandex.market.common.balance.xmlrpc.Balance2Operations;
import ru.yandex.market.common.balance.xmlrpc.Balance2XmlRPCServiceFactory;
import ru.yandex.market.common.balance.xmlrpc.model.PartnerContractStructure;
import ru.yandex.market.common.balance.xmlrpc.model.QueryCatalogStructure;
import ru.yandex.market.core.balance.BalanceService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

/**
 * Тест для {@link GetDistributionPartnerTypeService}
 */
@RunWith(MockitoJUnitRunner.class)
public class GetDistributionPartnerTypeServiceTest {
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private BalanceService balanceService;

    @Mock
    private DistributionClient distributionClient;

    @Test
    public void test() throws Exception {
        when(balanceService.queryCatalog(List.of("v_distribution_contract"), ""))
                .thenReturn(new QueryCatalogStructure(
                        List.of("v_distribution_contract.client_id",
                                "v_distribution_contract.person_id",
                                "v_distribution_contract.id"),
                        List.of(List.of("1", "1001", "11"),
                                List.of("2", "1002", "21"),
                                List.of("3", "1002", "31"),
                                List.of("3", "1002", "32"),
                                List.of("4", "1004", "10000"))));

        when(balanceService.getPartnerContracts(1L)).thenReturn(
                List.of(
                    contract(11, null, null, "ph", List.of()),
                    contract(101, 11, 51, null, List.of(
                            collateral(date(9), date(9), true),
                            collateral(date(10), null, false)
        ))));
        when(balanceService.getPartnerContracts(2L)).thenReturn(
                List.of(
                        contract(21, null, null, "ph", List.of()))
                );
        when(balanceService.getPartnerContracts(3L)).thenReturn(
                List.of(
                        contract(31, null, null, "j", List.of(
                                collateral(date(8), date(8), true))),
                        contract(32, null, null, "ph", List.of(
                                collateral(date(9), date(9), true),
                                collateral(date(12), date(9), false))),
                        contract(113, 31, 64, null, List.of()),
                        contract(114, 10000, 1000, null, List.of()),
                        contract(115, 31, 63, null, List.of()),
                        contract(116, 32, 64, null, List.of())));
        when(balanceService.getPartnerContracts(4L)).thenReturn(List.of());

        when(distributionClient.getData(any(), eq(distrFilters(51))))
                .thenReturn(responseWithClids(1));
        when(distributionClient.getData(any(), eq(distrFilters(63))))
                .thenReturn(responseWithClids(5, 6));
        when(distributionClient.getData(any(), eq(distrFilters(64))))
                .thenReturn(responseWithClids(7));

        GetDistributionPartnerTypeService service = new GetDistributionPartnerTypeService(balanceService, distributionClient);
        var result = service.getPartnerData();
        assertEquals(4, result.size());
        assertEquals(DistributionPartnerTypeData.PartnerType.NO_DATA, result.get(1L).getPartnerType());
        assertEquals(DistributionPartnerTypeData.PartnerType.INDIVIDUAL_SELF_EMPLOYED, result.get(5L).getPartnerType());
        assertEquals(DistributionPartnerTypeData.PartnerType.INDIVIDUAL_SELF_EMPLOYED, result.get(6L).getPartnerType());
        assertEquals(DistributionPartnerTypeData.PartnerType.OTHER, result.get(7L).getPartnerType());
        assertEquals(DistributionPartnerTypeData.ResidenceStatus.RESIDENT, result.get(1L).getResidenceStatus());
        assertEquals(DistributionPartnerTypeData.ResidenceStatus.NON_RESIDENT, result.get(5L).getResidenceStatus());
        assertEquals(DistributionPartnerTypeData.ResidenceStatus.NON_RESIDENT, result.get(6L).getResidenceStatus());
        assertEquals(DistributionPartnerTypeData.ResidenceStatus.RESIDENT, result.get(7L).getResidenceStatus());
    }

    private static Date date(int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(Calendar.MONTH, Calendar.APRIL);
        calendar.set(Calendar.YEAR, 2021);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        return calendar.getTime();
    }

    private static PartnerContractStructure contract(
            Integer id, Integer parentId, Integer tagId, String personType,
            List<PartnerContractStructure.CollateralsStructure> collaterals) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> person = new HashMap<>();
        Map<String, Object> contract = new HashMap<>();
        result.put(PartnerContractStructure.FIELD_PERSON, person);
        result.put(PartnerContractStructure.FIELD_CONTRACT, contract);
        Object[] collateralsArray = new Object[collaterals.size()];
        for (int i = 0; i < collateralsArray.length; ++i) {
            collateralsArray[i] = collaterals.get(i);
        }
        result.put(PartnerContractStructure.FIELD_COLLATERALS, collateralsArray);
        if (id != null) {
            contract.put(PartnerContractStructure.FIELD_CONTRACT_ID, id);
        }
        if (parentId != null) {
            contract.put(PartnerContractStructure.FIELD_PARENT_CONTRACT_ID, parentId);
        }
        if (tagId != null) {
            contract.put(PartnerContractStructure.FIELD_TAG_ID, tagId);
        }
        if (personType != null) {
            person.put(PartnerContractStructure.FIELD_TYPE, personType);
        }
        contract.put(PartnerContractStructure.FIELD_TYPE, GetDistributionPartnerTypeService.DISTRIBUTION_CONTRACT_TYPE);
        return new PartnerContractStructure(result);
    }

    private static PartnerContractStructure.CollateralsStructure collateral(Date date, Date signed, boolean selfemployed) {
        Map<String, Object> map = new HashMap<>();
        if (date != null) {
            map.put(PartnerContractStructure.FIELD_DATE, format(date));
        }
        if (signed != null) {
            map.put(PartnerContractStructure.FIELD_SIGNED, format(signed));
        }
        map.put(PartnerContractStructure.FIELD_SELFEMPLOYED, selfemployed ? 1 : 0);
        return new PartnerContractStructure.CollateralsStructure(map);
    }

    private static List<?> distrFilters(int tagId) {
        return List.of("AND", List.of(List.of("tag_id", "=", String.valueOf(tagId))));
    }

    private DistributionClient.Response responseWithClids(Integer ... clids) throws IOException {
        String clidsJson = Arrays.stream(clids)
            .map(clid -> "[{ \"name\": \"clid\", \"value\": " + clid + " }]")
            .collect(Collectors.joining(", "));
        String response =
                "{\"data\": {" +
                "        \"clids\": [" + clidsJson + "], " +
                "\"packs\": [], \"sets\": [] }, " +
                "\"result\": \"ok\"" +
                "}";
        return mapper.readValue(response.getBytes(StandardCharsets.UTF_8), DistributionClient.Response.class);
    }

    //@Test
    @Disabled("Этим кодом можно проверить ответ реального XMLRPC, запустив локально")
    public void tryRealQueryCatalog() throws Exception {
        var result = createExecutor().executeCommand(rpc -> rpc.QueryCatalog(List.of("v_distribution_contract"), "v_distribution_contract.client_id = 64621956"));
        var data = result.mapFromColumns(List.of(
                "v_distribution_contract.client_id",
                "v_distribution_contract.person_id",
                "v_distribution_contract.id"),
                l -> Long.parseLong(l.get(0)));
        assertFalse(data.isEmpty());
    }

    private static String format(Date date) {
        return DATE_FORMAT.format(date);
    }

    //@Test
    @Disabled("Этим кодом можно проверить ответ реального XMLRPC, запустив локально")
    public void tryRealPartnerContracts() throws Exception {
        var result = createExecutor().executeCommand(rpc -> rpc.GetPartnerContracts(64621956L));
        assertFalse(result.isEmpty());
    }

    private static XmlRPCServceExecutor<Balance2Operations> createExecutor() throws Exception {
        String myTvmTicket = "xxx";
        XmlRPCServceExecutor<Balance2Operations> executor = new XmlRPCServceExecutor<>();
        Balance2XmlRPCServiceFactory factory = new Balance2XmlRPCServiceFactory();
        factory.setServiceClass(Balance2.class);
        factory.setServerUrl("http://greed-ts.paysys.yandex.ru:8002/xmlrpc");
        factory.setTvmTicketProvider(() -> myTvmTicket);
        executor.setServiceFactory(factory);
        return executor;
    }

}

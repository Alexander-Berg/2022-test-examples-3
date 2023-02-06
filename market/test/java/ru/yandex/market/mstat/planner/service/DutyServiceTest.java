package ru.yandex.market.mstat.planner.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.collect.ImmutableList;
import junit.framework.TestCase;
import lombok.SneakyThrows;
import org.mockito.Mockito;
import ru.yandex.market.mstat.planner.client.AbcClient;
import ru.yandex.market.mstat.planner.dao.DutyDao;

import java.util.List;

import static ru.yandex.market.mstat.planner.util.RestUtil.parseLocalDateYMDFormat;

public class DutyServiceTest extends TestCase {
    JsonNodeFactory jsonFactory = new JsonNodeFactory(false);
    List<String> logins1 = ImmutableList.<String>builder().add("test1").add("test2").add("test3").build();
    List<String> logins2 = ImmutableList.<String>builder().add("test2").add("test3").add("test4").build();
    List<String> logins3 = ImmutableList.<String>builder().add("test1").build();

    String json1 = "[{\"id\":886776,\"person\":{\"login\":\"test1\",\"uid\":\"1120000000130370\"},\"schedule\":{\"id\":804,\"name\":\"Дежурство пользовательских бэков С++\"},\"is_approved\":true,\"start\":\"2020-12-07\",\"end\":\"2020-12-13\",\"start_datetime\":\"2020-12-07T00:00:00+03:00\",\"end_datetime\":\"2020-12-14T00:00:00+03:00\",\"replaces\":[]},{\"id\":752619,\"person\":{\"login\":\"test2\",\"uid\":\"1120000000020287\"},\"schedule\":{\"id\":1368,\"name\":\"Факап-дежурства\"},\"is_approved\":true,\"start\":\"2020-12-21\",\"end\":\"2020-12-28\",\"start_datetime\":\"2020-12-21T11:00:00+03:00\",\"end_datetime\":\"2020-12-28T11:00:00+03:00\",\"replaces\":[]},{\"id\":891426,\"person\":{\"login\":\"test3\",\"uid\":\"1120000000040704\"},\"schedule\":{\"id\":1453,\"name\":\"Поддержка\"},\"is_approved\":false,\"start\":\"2020-12-28\",\"end\":\"2021-01-04\",\"start_datetime\":\"2020-12-28T11:00:00+03:00\",\"end_datetime\":\"2021-01-04T11:00:00+03:00\",\"replaces\":[]}]";
    String json2 = "[{\"id\":886776,\"person\":{\"login\":\"test4\",\"uid\":\"1120000000130370\"},\"schedule\":{\"id\":804,\"name\":\"Дежурство пользовательских бэков С++\"},\"is_approved\":true,\"start\":\"2020-12-07\",\"end\":\"2020-12-13\",\"start_datetime\":\"2020-12-07T00:00:00+03:00\",\"end_datetime\":\"2020-12-14T00:00:00+03:00\",\"replaces\":[]}]";
    String json3 = "[{\"id\":752619,\"person\":{\"login\":\"test2\",\"uid\":\"1120000000020287\"},\"schedule\":{\"id\":1368,\"name\":\"Факап-дежурства\"},\"is_approved\":true,\"start\":\"2020-12-21\",\"end\":\"2020-12-28\",\"start_datetime\":\"2020-12-21T11:00:00+03:00\",\"end_datetime\":\"2020-12-28T11:00:00+03:00\",\"replaces\":[]},{\"id\":891426,\"person\":{\"login\":\"test3\",\"uid\":\"1120000000040704\"},\"schedule\":{\"id\":1453,\"name\":\"Поддержка\"},\"is_approved\":false,\"start\":\"2020-12-28\",\"end\":\"2021-01-04\",\"start_datetime\":\"2020-12-28T11:00:00+03:00\",\"end_datetime\":\"2021-01-04T11:00:00+03:00\",\"replaces\":[]},{\"id\":886776,\"person\":{\"login\":\"test4\",\"uid\":\"1120000000130370\"},\"schedule\":{\"id\":804,\"name\":\"Дежурство пользовательских бэков С++\"},\"is_approved\":true,\"start\":\"2020-12-07\",\"end\":\"2020-12-13\",\"start_datetime\":\"2020-12-07T00:00:00+03:00\",\"end_datetime\":\"2020-12-14T00:00:00+03:00\",\"replaces\":[]}]";
    String json4 = "[{\"id\":886776,\"person\":{\"login\":\"test1\",\"uid\":\"1120000000130370\"},\"schedule\":{\"id\":804,\"name\":\"Дежурство пользовательских бэков С++\"},\"is_approved\":true,\"start\":\"2020-11-07\",\"end\":\"2020-11-20\",\"start_datetime\":\"2020-12-07T00:00:00+03:00\",\"end_datetime\":\"2020-12-14T00:00:00+03:00\",\"replaces\":[]},{\"id\":752619,\"person\":{\"login\":\"test2\",\"uid\":\"1120000000020287\"},\"schedule\":{\"id\":1368,\"name\":\"Факап-дежурства\"},\"is_approved\":true,\"start\":\"2020-12-21\",\"end\":\"2020-12-28\",\"start_datetime\":\"2020-12-21T11:00:00+03:00\",\"end_datetime\":\"2020-12-28T11:00:00+03:00\",\"replaces\":[]},{\"id\":891426,\"person\":{\"login\":\"test3\",\"uid\":\"1120000000040704\"},\"schedule\":{\"id\":1453,\"name\":\"Поддержка\"},\"is_approved\":false,\"start\":\"2020-12-28\",\"end\":\"2021-01-04\",\"start_datetime\":\"2020-12-28T11:00:00+03:00\",\"end_datetime\":\"2021-01-04T11:00:00+03:00\",\"replaces\":[]}]";
    String json5 = "[{\"id\":886775,\"person\":{\"login\":\"test1\",\"uid\":\"1120000000130370\"},\"schedule\":{\"id\":805,\"name\":\"Дежурство пользовательских бэков С\"},\"is_approved\":true,\"start\":\"2020-12-05\",\"end\":\"2020-12-13\",\"start_datetime\":\"2020-12-07T00:00:00+03:00\",\"end_datetime\":\"2020-12-14T00:00:00+03:00\",\"replaces\":[]},{\"id\":886776,\"person\":{\"login\":\"test1\",\"uid\":\"1120000000130370\"},\"schedule\":{\"id\":805,\"name\":\"Дежурство пользовательских бэков С\"},\"is_approved\":true,\"start\":\"2020-12-05\",\"end\":\"2020-12-13\",\"start_datetime\":\"2020-12-07T00:00:00+03:00\",\"end_datetime\":\"2020-12-14T00:00:00+03:00\",\"replaces\":[]}]";
    String json6 = "[{\"id\":886775,\"person\":{\"login\":\"test1\",\"uid\":\"1120000000130370\"},\"schedule\":{\"id\":805,\"name\":\"Дежурство пользовательских бэков С\"},\"is_approved\":true,\"start\":\"2020-12-05\",\"end\":\"2020-12-13\",\"start_datetime\":\"2020-12-07T00:00:00+03:00\",\"end_datetime\":\"2020-12-14T00:00:00+03:00\",\"replaces\":[{\"id\":886776,\"person\":{\"login\":\"test1\",\"uid\":\"1120000000130370\"},\"schedule\":{\"id\":805,\"name\":\"Дежурство пользовательских бэков С\"},\"is_approved\":true,\"start\":\"2020-12-05\",\"end\":\"2020-12-13\",\"start_datetime\":\"2020-12-07T00:00:00+03:00\",\"end_datetime\":\"2020-12-14T00:00:00+03:00\"}]}]";

    String resjson6 = "[{\"id\":886775,\"person\":{\"login\":\"test1\",\"uid\":\"1120000000130370\"},\"schedule\":{\"id\":805,\"name\":\"Дежурство пользовательских бэков С (с заменами)\"},\"is_approved\":true,\"start\":\"2020-12-05\",\"end\":\"2020-12-13\",\"start_datetime\":\"2020-12-07T00:00:00+03:00\",\"end_datetime\":\"2020-12-14T00:00:00+03:00\",\"replaces\":[{\"id\":886776,\"person\":{\"login\":\"test1\",\"uid\":\"1120000000130370\"},\"schedule\":{\"id\":805,\"name\":\"Дежурство пользовательских бэков С (заменяет @test1)\"},\"is_approved\":true,\"start\":\"2020-12-05\",\"end\":\"2020-12-13\",\"start_datetime\":\"2020-12-07T00:00:00+03:00\",\"end_datetime\":\"2020-12-14T00:00:00+03:00\"}]},{\"id\":886776,\"person\":{\"login\":\"test1\",\"uid\":\"1120000000130370\"},\"schedule\":{\"id\":805,\"name\":\"Дежурство пользовательских бэков С (заменяет @test1)\"},\"is_approved\":true,\"start\":\"2020-12-05\",\"end\":\"2020-12-13\",\"start_datetime\":\"2020-12-07T00:00:00+03:00\",\"end_datetime\":\"2020-12-14T00:00:00+03:00\"}]";

    @SneakyThrows
    public void testGetDutyShifts() {
        AbcClient client = Mockito.mock(AbcClient.class);
        DutyDao dao = Mockito.mock(DutyDao.class);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode1 = mapper.readTree(json1);
        Mockito.when(client.getDutyShifts(parseLocalDateYMDFormat("2020-12-01"), parseLocalDateYMDFormat("2020-12-02"), logins1)).thenReturn(jsonNode1);

        JsonNode jsonNode2 = mapper.readTree(json2);
        Mockito.when(client.getDutyShifts(parseLocalDateYMDFormat("2020-12-01"), parseLocalDateYMDFormat("2020-12-02"),
                ImmutableList.<String>builder().add("test4").build())).thenReturn(jsonNode2);

        JsonNode jsonNode3 = mapper.readTree(json3);

        JsonNode jsonNode4 = mapper.readTree(json4);
        Mockito.when(client.getDutyShifts(parseLocalDateYMDFormat("2020-11-05"), parseLocalDateYMDFormat("2020-11-21"), logins1)).thenReturn(jsonNode4);
        JsonNode jsonNode5 = mapper.readTree(json5);
        Mockito.when(client.getDutyShifts(parseLocalDateYMDFormat("2020-11-04"), parseLocalDateYMDFormat("2020-11-21"), logins3)).thenReturn(jsonNode5);

        JsonNode jsonNode6 = mapper.readTree(json6);
        JsonNode resJsonNode6 = mapper.readTree(resjson6);
        Mockito.when(client.getDutyShifts(parseLocalDateYMDFormat("2020-11-06"), parseLocalDateYMDFormat("2020-11-21"), logins3)).thenReturn(jsonNode6);


        DutyService service = new DutyService(dao, client);

        assertEquals(jsonFactory.nullNode(), service.loadDutyShifts(parseLocalDateYMDFormat("2020-12-01"), parseLocalDateYMDFormat("2020-12-02"),null));
        assertEquals(jsonNode1,service.loadDutyShifts(parseLocalDateYMDFormat("2020-12-01"), parseLocalDateYMDFormat("2020-12-02"), logins1));
        assertEquals(jsonNode3,service.loadDutyShifts(parseLocalDateYMDFormat("2020-12-01"), parseLocalDateYMDFormat("2020-12-02"), logins2));
        assertEquals(jsonNode4,service.loadDutyShifts(parseLocalDateYMDFormat("2020-11-05"), parseLocalDateYMDFormat("2020-11-21"), logins1));
        assertEquals(jsonNode5,service.loadDutyShifts(parseLocalDateYMDFormat("2020-11-04"), parseLocalDateYMDFormat("2020-11-21"), logins3));
        assertEquals(resJsonNode6,service.loadDutyShifts(parseLocalDateYMDFormat("2020-11-06"), parseLocalDateYMDFormat("2020-11-21"), logins3));
    }
}

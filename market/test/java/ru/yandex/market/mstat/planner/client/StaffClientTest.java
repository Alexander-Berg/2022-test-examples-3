package ru.yandex.market.mstat.planner.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.TestCase;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.mockito.Mockito;

import java.io.OutputStream;
import java.net.HttpURLConnection;

import static org.mockito.Mockito.mock;

public class StaffClientTest extends TestCase {
    AbstractClient.URLFactory urlFactory = Mockito.mock(AbstractClient.URLFactory.class);
    StaffClient staffClient = new StaffClient(urlFactory, "");

    @SneakyThrows
    public void testGetDepartments() {
        String testDataWithId = "{\"links\": {}, \"page\": 1, \"limit\": 50, \"result\": [{\"is_deleted\": false, \"uid\": \"1120000000044878\", \"official\": {\"affiliation\": \"yandex\", \"is_dismissed\": false, \"is_homeworker\": false, \"is_robot\": false}, \"login\": \"s-alina\", \"id\": 22222, \"name\": {\"last\": {\"ru\": \"Смирнова\", \"en\": \"Smirnova\"}, \"first\": {\"ru\": \"Алина\", \"en\": \"Alina\"}}}], \"total\": 1, \"pages\": 1}";

        String testDataPaging= "{\"links\": {}, \"page\": 1, \"limit\": 50, \"result\": [{\"is_deleted\": false, \"uid\": \"1120000000044878\", \"official\": {\"affiliation\": \"yandex\", \"is_dismissed\": false, \"is_homeworker\": false, \"is_robot\": false}, \"login\": \"s-alina\", \"id\": 22222, \"name\": {\"last\": {\"ru\": \"Смирнова\", \"en\": \"Smirnova\"}, \"first\": {\"ru\": \"Алина\", \"en\": \"Alina\"}}}], \"total\": 1, \"pages\": 2}";
        String testDataPaging2 = "{\"links\": {}, \"page\": 2, \"limit\": 50, \"result\": [{\"is_deleted\": false, \"uid\": \"1120000000044878\", \"official\": {\"affiliation\": \"yandex\", \"is_dismissed\": false, \"is_homeworker\": false, \"is_robot\": false}, \"login\": \"s-alina\", \"id\": 22222, \"name\": {\"last\": {\"ru\": \"Смирнова\", \"en\": \"Smirnova\"}, \"first\": {\"ru\": \"Алина\", \"en\": \"Alina\"}}}], \"total\": 1, \"pages\": 2}";


        HttpURLConnection mockHttpConnectionFirst = Mockito.mock(HttpURLConnection.class);
        Mockito.when(mockHttpConnectionFirst.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        Mockito.when(mockHttpConnectionFirst.getInputStream()).thenReturn(IOUtils.toInputStream(testDataWithId));
        final AbstractClient.UrlWrapper mockURLFirst = mock(AbstractClient.UrlWrapper.class);
        Mockito.when(mockURLFirst.openConnection()).thenReturn(mockHttpConnectionFirst);

        HttpURLConnection mockHttpConnectionSecond = Mockito.mock(HttpURLConnection.class);
        Mockito.when(mockHttpConnectionSecond.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        Mockito.when(mockHttpConnectionSecond.getInputStream()).thenReturn(IOUtils.toInputStream(testDataWithId));
        final AbstractClient.UrlWrapper mockURLSecond = mock(AbstractClient.UrlWrapper.class);
        Mockito.when(mockURLSecond.openConnection()).thenReturn(mockHttpConnectionSecond);

        HttpURLConnection mockHttpConnectionThird = Mockito.mock(HttpURLConnection.class);
        Mockito.when(mockHttpConnectionThird.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        Mockito.when(mockHttpConnectionThird.getInputStream()).thenReturn(IOUtils.toInputStream(testDataPaging));
        final AbstractClient.UrlWrapper mockURLThird = mock(AbstractClient.UrlWrapper.class);
        Mockito.when(mockURLThird.openConnection()).thenReturn(mockHttpConnectionThird);

        HttpURLConnection mockHttpConnectionFourth = Mockito.mock(HttpURLConnection.class);
        Mockito.when(mockHttpConnectionFourth.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        Mockito.when(mockHttpConnectionFourth.getInputStream()).thenReturn(IOUtils.toInputStream(testDataPaging2));
        final AbstractClient.UrlWrapper mockURLFourth = mock(AbstractClient.UrlWrapper.class);
        Mockito.when(mockURLFourth.openConnection()).thenReturn(mockHttpConnectionFourth);

        Mockito.when(urlFactory.createUrl("https://staff-api.yandex-team.ru/v3/groups?type=department&_fields=name,id,department.heads.role,department.heads.person.login,parent.id&id=222&_limit=2000")).thenReturn(mockURLFirst);
        Mockito.when(urlFactory.createUrl("https://staff-api.yandex-team.ru/v3/groups?type=department&_fields=name,id,department.heads.role,department.heads.person.login,parent.id&id=222&parent.id=1&_limit=2000")).thenReturn(mockURLSecond);
        Mockito.when(urlFactory.createUrl("https://staff-api.yandex-team.ru/v3/groups?type=department&_fields=name,id,department.heads.role,department.heads.person.login,parent.id&_limit=2000")).thenReturn(mockURLThird);
        Mockito.when(urlFactory.createUrl("https://staff-api.yandex-team.ru/v3/groups?type=department&_fields=name,id,department.heads.role,department.heads.person.login,parent.id&_limit=2000&_page=2")).thenReturn(mockURLFourth);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(testDataWithId);
        assertEquals(jsonNode, staffClient.getDepartments("222", null));
        assertEquals(jsonNode, staffClient.getDepartments("222","1"));
        assertEquals((new ObjectMapper()).readTree("{\"links\":{},\"page\":1,\"limit\":50,\"result\":[{\"is_deleted\":false,\"uid\":\"1120000000044878\",\"official\":{\"affiliation\":\"yandex\",\"is_dismissed\":false,\"is_homeworker\":false,\"is_robot\":false},\"login\":\"s-alina\",\"id\":22222,\"name\":{\"last\":{\"ru\":\"Смирнова\",\"en\":\"Smirnova\"},\"first\":{\"ru\":\"Алина\",\"en\":\"Alina\"}}},{\"is_deleted\":false,\"uid\":\"1120000000044878\",\"official\":{\"affiliation\":\"yandex\",\"is_dismissed\":false,\"is_homeworker\":false,\"is_robot\":false},\"login\":\"s-alina\",\"id\":22222,\"name\":{\"last\":{\"ru\":\"Смирнова\",\"en\":\"Smirnova\"},\"first\":{\"ru\":\"Алина\",\"en\":\"Alina\"}}}],\"total\":1,\"pages\":2}"),staffClient.getDepartments(null, null));
    }

    @SneakyThrows
    public void testGetPersons() {
        String testDataWithAffiliation = "{\"links\": {}, \"page\": 1, \"limit\": 50, \"result\": [{\"is_deleted\": false, \"uid\": \"1120000000170546\", \"official\": {\"affiliation\": \"yamoney\", \"is_dismissed\": false, \"is_homeworker\": false, \"is_robot\": false}, \"login\": \"orphie\", \"id\": 64890, \"name\": {\"last\": {\"ru\": \"Емельянова\", \"en\": \"Emelianova\"}, \"first\": {\"ru\": \"Анастасия\", \"en\": \"Anastasiia\"}}}], \"total\": 1, \"pages\": 1}";
        String testDataWithRootDepId = "{\"links\": {}, \"page\": 1, \"limit\": 50, \"result\": [{\"is_deleted\": false, \"uid\": \"1120000000000951\", \"official\": {\"affiliation\": \"yandex\", \"is_dismissed\": true, \"is_homeworker\": false, \"is_robot\": false}, \"login\": \"gin\", \"id\": 9, \"name\": {\"last\": {\"ru\": \"Положинцев\", \"en\": \"Pologintsev\"}, \"first\": {\"ru\": \"Илья\", \"en\": \"Ilya\"}}}], \"total\": 1, \"pages\": 1}";
        String testDataWithLogin = "{\"links\": {}, \"page\": 1, \"limit\": 50, \"result\": [{\"is_deleted\": false, \"uid\": \"1120000000170545\", \"official\": {\"affiliation\": \"yandex\", \"is_dismissed\": false, \"is_homeworker\": false, \"is_robot\": false}, \"login\": \"orphie\", \"id\": 64890, \"name\": {\"last\": {\"ru\": \"Емельянова\", \"en\": \"Emelyanova\"}, \"first\": {\"ru\": \"Анастасия\", \"en\": \"Anastasia\"}}}], \"total\": 1, \"pages\": 1}";

        HttpURLConnection mockHttpConnectionFirst = Mockito.mock(HttpURLConnection.class);
        Mockito.when(mockHttpConnectionFirst.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        Mockito.when(mockHttpConnectionFirst.getInputStream()).thenReturn(IOUtils.toInputStream(testDataWithAffiliation));
        final AbstractClient.UrlWrapper mockURLSecond = mock(AbstractClient.UrlWrapper.class);
        Mockito.when(mockURLSecond.openConnection()).thenReturn(mockHttpConnectionFirst);

        HttpURLConnection mockHttpConnectionSecond = Mockito.mock(HttpURLConnection.class);
        Mockito.when(mockHttpConnectionSecond.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        Mockito.when(mockHttpConnectionSecond.getInputStream()).thenReturn(IOUtils.toInputStream(testDataWithRootDepId));
        final AbstractClient.UrlWrapper mockURLThird = mock(AbstractClient.UrlWrapper.class);
        Mockito.when(mockURLThird.openConnection()).thenReturn(mockHttpConnectionSecond);

        HttpURLConnection mockHttpConnectionThird = Mockito.mock(HttpURLConnection.class);
        Mockito.when(mockHttpConnectionThird.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        Mockito.when(mockHttpConnectionThird.getInputStream()).thenReturn(IOUtils.toInputStream(testDataWithLogin));
        final AbstractClient.UrlWrapper mockURLFourth = mock(AbstractClient.UrlWrapper.class);
        Mockito.when(mockURLFourth.openConnection()).thenReturn(mockHttpConnectionThird);

        Mockito.when(urlFactory.createUrl("https://staff-api.yandex-team.ru/v3/persons?official.is_robot=false&_fields=login,name.first.ru,name.last.ru,official.position,official.is_dismissed,official.contract_ended_at,department_group.id&official.affiliation=yandex&_limit=2000")).thenReturn(mockURLSecond);
        Mockito.when(urlFactory.createUrl("https://staff-api.yandex-team.ru/v3/persons?official.is_robot=false&_fields=login,name.first.ru,name.last.ru,official.position,official.is_dismissed,official.contract_ended_at,department_group.id&department_group.id=22&_limit=2000")).thenReturn(mockURLThird);
        Mockito.when(urlFactory.createUrl("https://staff-api.yandex-team.ru/v3/persons?official.is_robot=false&_fields=login,name.first.ru,name.last.ru,official.position,official.is_dismissed,official.contract_ended_at,department_group.id&_one=1&login=orphie&_limit=2000")).thenReturn(mockURLFourth);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(testDataWithLogin);
        assertEquals(jsonNode, staffClient.getPersons(null, null, "orphie"));

        jsonNode = mapper.readTree(testDataWithRootDepId);
        assertEquals(jsonNode, staffClient.getPersons(null,"22", null));

        jsonNode = mapper.readTree(testDataWithAffiliation);
        assertEquals(jsonNode,staffClient.getPersons("yandex", null, null));
    }
}

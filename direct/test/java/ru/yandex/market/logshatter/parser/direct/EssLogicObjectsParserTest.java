package ru.yandex.market.logshatter.parser.direct;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class EssLogicObjectsParserTest {

    private LogParserChecker checker = new LogParserChecker(new EssLogicObjectsParser());

    @Test
    public void testParse() throws Exception {
        checker.setLogBrokerTopic("test-topic");
        String jsonLine = "{"
            + "'reqId':1,"
            + "'utcTimestamp':1550534403,"
            + "'gtid':'2ec0b25f-e22f-385b-1993-cbfd97a2bba1:349965755',"
            + "'seqNo':123457,"
            + "'source':'ppc:1',"
            + "'logicObjectsList':["
            + "{'changed_table':'banner_images','primary_key':1,'id':2,'last_change':'2019-02-19T00:00:02'," +
            "'bool_field':true},"
            + "{'changed_table':'banner_images','primary_key':4,'id':5,'last_change':'2019-02-19T00:00:02'," +
            "'bool_field':false}"
            + "]," +
            "'isPingObject':false"
            + "}";

        List<String> logicObject1Col =
            asList("changed_table", "primary_key", "id", "last_change", "bool_field");
        List<String> logicObject1Val =
            asList("banner_images", "1", "2", "2019-02-19T00:00:02", "true");

        List<String> logicObject2Col =
            asList("changed_table", "primary_key", "id", "last_change", "bool_field");
        List<String> logicObject2Val =
            asList("banner_images", "4", "5", "2019-02-19T00:00:02", "false");

        Object[] res1 = new Object[]{
            1L,
            "2ec0b25f-e22f-385b-1993-cbfd97a2bba1:349965755",
            123457L,
            "ppc:1",
            "test-topic",
            logicObject1Col,
            logicObject1Val
        };

        Object[] res2 = new Object[]{
            1L,
            "2ec0b25f-e22f-385b-1993-cbfd97a2bba1:349965755",
            123457L,
            "ppc:1",
            "test-topic",
            logicObject2Col,
            logicObject2Val
        };

        Date date = Date.from(Instant.ofEpochSecond(1550534403L));
        checker.check(jsonLine,
            asList(date, date),
            asList(res1, res2));
    }

    @Test
    public void testParse_ObjectWithArray() throws Exception {
        checker.setLogBrokerTopic("test-topic");
        String jsonLine = "{"
            + "'reqId':1,"
            + "'utcTimestamp':1550534403,"
            + "'gtid':'2ec0b25f-e22f-385b-1993-cbfd97a2bba1:349965756',"
            + "'seqNo':123458,"
            + "'source':'ppc:1',"
            + "'logicObjectsList':["
            + "{'array_int':[1,2,3,4],'array_string':['1','2','3','4']}"
            + "]"
            + "}";

        List<String> logicObjectCol =
            asList("array_int", "array_string");
        List<String> logicObjectVal =
            asList("[1,2,3,4]", "[\"1\",\"2\",\"3\",\"4\"]");

        Object[] res = new Object[]{
            1L,
            "2ec0b25f-e22f-385b-1993-cbfd97a2bba1:349965756",
            123458L,
            "ppc:1",
            "test-topic",
            logicObjectCol,
            logicObjectVal
        };

        Date date = Date.from(Instant.ofEpochSecond(1550534403L));
        checker.check(jsonLine,
            singletonList(date),
            singletonList(res));
    }

    @Test
    public void testParse_NestedObject() throws Exception {
        checker.setLogBrokerTopic("test-topic");
        String jsonLine = "{"
            + "'reqId':1,"
            + "'utcTimestamp':1550534403,"
            + "'gtid':'2ec0b25f-e22f-385b-1993-cbfd97a2bba1:349965757',"
            + "'seqNo':123459,"
            + "'source':'ppc:1',"
            + "'logicObjectsList':["
            + "{'object1':{'nested_object':{'nested_attr1':'nested_val','nested_attr2':5}}," +
            "'object2':{'nested_object':{'nested_object':{'nested_attr1':2}}}}"
            + "]"
            + "}";

        List<String> logicObjectCol =
            asList("object1", "object2");
        List<String> logicObjectVal =
            asList("{\"nested_object\":{\"nested_attr1\":\"nested_val\",\"nested_attr2\":5}}",
                "{\"nested_object\":{\"nested_object\":{\"nested_attr1\":2}}}");

        Object[] res = new Object[]{
            1L,
            "2ec0b25f-e22f-385b-1993-cbfd97a2bba1:349965757",
            123459L,
            "ppc:1",
            "test-topic",
            logicObjectCol,
            logicObjectVal
        };

        Date date = Date.from(Instant.ofEpochSecond(1550534403L));
        checker.check(jsonLine,
            singletonList(date),
            singletonList(res));
    }

    @Test
    public void testParse_NullValues() throws Exception {
        checker.setLogBrokerTopic("test-topic");
        String jsonLine = "{"
            + "'reqId':null,"
            + "'utcTimestamp':1550534403,"
            + "'gtid':null,"
            + "'seqNo':null,"
            + "'source':null,"
            + "'logicObjectsList':["
            + "{'object1':{'nested_object':{'nested_attr1':'nested_val','nested_attr2':5}}," +
            "'object2':{'nested_object':{'nested_object':{'nested_attr1':2}}}}"
            + "]"
            + "}";

        List<String> logicObjectCol =
            asList("object1", "object2");
        List<String> logicObjectVal =
            asList("{\"nested_object\":{\"nested_attr1\":\"nested_val\",\"nested_attr2\":5}}",
                "{\"nested_object\":{\"nested_object\":{\"nested_attr1\":2}}}");

        Object[] res = new Object[]{
            0L,
            "",
            0L,
            "",
            "test-topic",
            logicObjectCol,
            logicObjectVal
        };

        Date date = Date.from(Instant.ofEpochSecond(1550534403L));
        checker.check(jsonLine,
            singletonList(date),
            singletonList(res));
    }

    @Test
    public void testParse_MissingValues() throws Exception {
        checker.setLogBrokerTopic("test-topic");
        String jsonLine = "{"
            + "'utcTimestamp':1550534403,"
            + "'logicObjectsList':["
            + "{'object1':{'nested_object':{'nested_attr1':'nested_val','nested_attr2':5}}," +
            "'object2':{'nested_object':{'nested_object':{'nested_attr1':2}}}}"
            + "]"
            + "}";

        List<String> logicObjectCol =
            asList("object1", "object2");
        List<String> logicObjectVal =
            asList("{\"nested_object\":{\"nested_attr1\":\"nested_val\",\"nested_attr2\":5}}",
                "{\"nested_object\":{\"nested_object\":{\"nested_attr1\":2}}}");

        Object[] res = new Object[]{
            0L,
            "",
            0L,
            "",
            "test-topic",
            logicObjectCol,
            logicObjectVal
        };

        Date date = Date.from(Instant.ofEpochSecond(1550534403L));
        checker.check(jsonLine,
            singletonList(date),
            singletonList(res));
    }

    @Test
    public void testParse_PingObject() throws Exception {
        checker.setLogBrokerTopic("test-topic");
        String jsonLine = "{"
            + "'reqId':0,"
            + "'utcTimestamp':1550534403,"
            + "'gtid':'',"
            + "'seqNo':123458,"
            + "'source':'',"
            + "'logicObjectsList':[],"
            + "'isPingObject':true"
            + "}";
        
        checker.checkEmpty(jsonLine);
    }
}


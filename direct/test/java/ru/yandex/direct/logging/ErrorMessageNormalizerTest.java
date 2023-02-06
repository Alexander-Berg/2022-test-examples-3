package ru.yandex.direct.logging;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class ErrorMessageNormalizerTest {
    private final ErrorMessageNormalizer errorMessageNormalizer;

    public ErrorMessageNormalizerTest() {
        errorMessageNormalizer = new ErrorMessageNormalizer(1_000_000);
    }

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public String messageBefore;

    @Parameterized.Parameter(2)
    public String messageAfter;

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {
                        "Cut out payload",
                        "Got error on response for DisplayCanvas request Request: " +
                                "POST https://turbo.yandex.ru/direct/submissions\npayload:\n" +
                                "{\"SelectionCriteria\":{\"TurboPageIds\":[10000000],\"ClientId\":11111111," +
                                "\"DateTimeFrom\":\"2018-10-04T12:30:06Z\"},\"Page\":{\"Limit\":1001,\"Offset\":0}}\n",
                        "Got error on response for DisplayCanvas request Request: " +
                                "POST https://turbo.yandex.ru/direct/submissions PAYLOAD\n"
                },
                {
                        "Cut out SQL requests",
                        "SQL [select `users`.`FIO`, `users`.`ClientID` from `users` where `users`.`uid` = ?]; ppc - " +
                                "Connection is not available, request timed out after ms." +
                                "\nSQL [delete from `banner_prices` where 1 = 0]; Communications link failure" +
                                "\nSQL [(select distinct `bids_base`.`cid` from `bids_base`)]; Query execution was " +
                                "interrupted" +
                                "\nSQL [select `campaigns`.`archived`, `campaigns`.`DontShow`, `camp_options`" +
                                ".`statusPostModerate`, `campaigns`.`statusActive` from `campaigns`]; Socket closed" +
                                "\nSQL [select `campaigns`.`archived` from `campaigns`]; Stream closed" +
                                "\nSQL [update bids` set `bids`.`price` = case `bids`.`id`]; Lock wait timeout " +
                                "exceeded; try restarting transaction",
                        "SQL ppc - Connection is not available, request timed out after ms." +
                                "\nSQL Communications link failure" +
                                "\nSQL Query execution was interrupted" +
                                "\nSQL Socket closed" +
                                "\nSQL Stream closed" +
                                "\nSQL Lock wait timeout exceeded; try restarting transaction"
                },
                // Second SQL test is needed because of fingerprint length restrictions
                {
                        "Cut out SQL requests #2",
                        "SQL [select `campaigns`.`archived` from `campaigns`]; Can not read response from server." +
                                "\n SQL [? from `users` limit ?]; Deadlock found when trying to get lock;" +
                                "\n SQL [delete from `banner_turbolandings` where 1 = 0]; No operations allowed after" +
                                " connection closed.",
                        "SQL Can not read response from server." +
                                "\n SQL Deadlock found when trying to get lock;" +
                                "\n SQL No operations allowed after connection closed."
                },
                {
                        "Cut out duplicate entry SQL request",
                        "SQL [insert into `banner_images_formats` (`formats`, `image_type`, `height`) values (?, ?, " +
                                "?)]; Duplicate entry 'opopopopopopopo_Kd1WhA' for key 'PRIMARY'" +
                                "\nDuplicate entry '123456789_abcde' for key 'PRIMARY'",
                        "SQL Duplicate entry" +
                                "\nDuplicate entry"
                },
                {
                        "Cut out 'uncategorized SQLException'",
                        "PreparedStatementCallback; uncategorized SQLException for SQL [SELECT `type`, `shard`, `id` " +
                                "FROM `dict` WHERE (`type` = ? AND `id` IN (?))]; SQL state [null];",
                        "PreparedStatementCallback; uncategorized SQLException for SQL; SQL state [null];",
                },
                {
                        "Cut out ADVQ response with errors for phrase",
                        "Got errors for phrase \u0433\u0430\u0437\u043e\u0431\u0435\u0442\u043e\u043d d600 " +
                                "-\u0434\u043e\u043c from ADVQ response: [assoc: timeout, assoc: timeout]",
                        "Got ADVQ response's errors for phrase"
                },
                {
                        "Cut out ADVQ query syntax error",
                        "ru.yandex.advq.query.IllegalQueryException: Syntax error at 1:18: extraneous input '<EOF>' " +
                                "expecting {SPACE, ']'} @ com.google.common.cache.LocalCache$Segment; " +
                                "Caused by Syntax error at 1:18: extraneous input '<EOF>' expecting {SPACE, ']'} " +
                                "@ ru.yandex.advq.query.QueryParserBuilder$SyntaxErrorReporter",
                        "ru.yandex.advq.query.IllegalQueryException: Syntax error at LINE:COLUMN: MESSAGE"
                },
                {
                        "Cut out 'Blackbox fatal error' exception",
                        "blackbox fatal error: DB_EXCEPTION: Fatal BlackBox error: dbpool exception: Can't get " +
                                "connection (DSN: host=22a2:6b8:c02:e08:1000:636::28:3306;dr=mysql;" +
                                "db=passportdbshard1). request_id=111111111111. method=oauth. host=blackbox" +
                                ".yandex.net. hostname=pass-s11.sezam.yandex.net. current_time=2019-10-02T09:34:04" +
                                ".421703+0300; url: http://blackbox.yandex.net/blackbox/",
                        "blackbox fatal error: DB_EXCEPTION; url: http://blackbox.yandex.net/blackbox/"
                },
                {
                        "Cut out 'Failed to call blackbox' exception",
                        "BlackboxHttpException : Failed to call blackbox: http://blackbox.yandex" +
                                ".net/blackbox/?method=oauth&userip=145.239.207.40&oauth_token=<cut>&get_user_ticket=1",
                        "BlackboxHttpException : Failed to call blackbox"
                },
                {
                        "Cut out 'RpcError' exception",
                        "Error 1: Internal RPC call failed {datetime=\"2019-10-03T14:18:17.747350Z\"; " +
                                "fid=111111111111111u; host=\"vla3-1000-407-vla-yt-seneca-rpc-d69-0000.gencfg-c" +
                                ".yandex.net\"; pid=50; span_id=111111111111111u; tid=111111111111111u; " +
                                "trace_id=\"aa702a9c-c96189c2-a2309ae1-50d82f9b\"}\n" +
                                "ru.yandex.yt.ytclient.rpc.RpcError: Error 105: Proxy cannot synchronize with " +
                                "cluster {datetime=\"2019-10-07T15:59:41.071230Z\"; fid=111111111111111u; " +
                                "host=\"vla3-1001-16b-vla-yt-seneca-rpc-d69-0009.gencfg-c.yandex.net\"; pid=50;}",
                        "Error 1: Internal RPC call failed\n" +
                                "ru.yandex.yt.ytclient.rpc.RpcError: Error 105: Proxy cannot synchronize with cluster"
                },
                {
                        "Cut out 'LogBroker message’s unknown verdictType' exception",
                        "LogBroker message contains unknown verdictType. Message: \t{\"service\": \"pythia\", " +
                                "\"unixtime\": 1570141251000, \"create_time\": \"2019-10-04 01:20:51\", \"meta\": " +
                                "{\"banner_id\": 0, \"client_id\": 0\"}, \"type\": \"brandlift_box\"}",
                        "LogBroker message contains unknown verdictType"
                },
                {
                        "Cut out 'Duplicate key' exception",
                        "Duplicate key Барбер Оренбург (attempted merging values 123456789 and 987654321)",
                        "Duplicate key"
                },
                {
                        "Cut out 'Duplicate entry for key' exception",
                        "Duplicate entry for key '!которые' (attempt to merge values 'ru.yandex.direct.core.entity" +
                                ".keyword.processing.NormalizedWord@11a12345' and 'ru.yandex.direct.core.entity" +
                                ".keyword.processing.NormalizedWord@11a12345')",
                        "Duplicate entry for key"
                },
                {
                        "Cut out Job's id in exceptions",
                        "Job PpcDataExportJob.bb111111 param_HAHN---classpath:/export/ppcdataexport/bs/bidsForBS.conf" +
                                " threw an unhandled Exception: " +
                                "\nJob RecommendationsMergeJob param_SENECA_SAS threw Exception: ",
                        "JOB threw an unhandled Exception" +
                                "\nJOB threw Exception"
                },
                {
                        "Cut out Advq's 'Interrupted while processing' exception",
                        "Interrupted while processing 4422 requests, the first 10 requests [SearchRequest{keywords" +
                                "=[lancome poeme \\u0442\\u0435\\u0441\\u0442], regionIds=[225, -1, -NUM, 977]}]" +
                                "\nInterrupted while processing 1453 requests [SearchRequest smthsmth]",
                        "Interrupted while processing NUM requests, the first 10 requests" +
                                "\nInterrupted while processing NUM requests"
                },
                {
                        "Cut out 'DefectInfo' exception",
                        "RuntimeException : DefectInfo{path=, value=testo-1@mail-ru, " +
                                "defect=Defect{defectId=INVALID_VALUE, params=null}}",
                        "RuntimeException : DefectInfo"
                },
                {
                        "Cut out 'stream closed with error' exception",
                        "Producer stream (sessionId: banner-moderationproduction:ppc:11cpm_banner|111f111f-111f111f" +
                                "-111f111f-111f111f) closed with error",
                        "Producer stream closed with error"
                },
                {
                        "Cut out syntax error position",
                        "Syntax error at 1:5:",
                        "Syntax error"
                },
                {
                        "Cut out syntax error's symbol details",
                        "Syntax error token recognition error at: '?'" +
                                "\nSyntax error missing WORD at '<EOF>'" +
                                "\nSyntax error extraneous input '\"' expecting WORD" +
                                "\nSyntax error mismatched input '<EOF>' expecting {SPACE, ']'}",
                        "Syntax error token recognition error at:" +
                                "\nSyntax error missing WORD at" +
                                "\nSyntax error extraneous input" +
                                "\nSyntax error mismatched input"
                },
                {
                        "Cut out 'Connection timed out/refused' address",
                        "Connection refused: sas2-1111-70d.seneca-sas.yt.gencfg-c.yandex" +
                                ".net/2a02:6b8:c16:718:10d:adbd:0:1111:1111",
                        "Connection refused"
                },
                {
                        "Cut out incorrect URIs",
                        "http:///kontakty could not be parsed into a proper Uri, missing host",
                        "could not be parsed into a proper Uri, missing host"
                },
                {
                        "Cut out 'BS auction response' errors",
                        "Can't get BS auction response for all phrases. Got errors in next bs-auction responses: " +
                                "[BsResponse{errors=[ru.yandex.direct.asynchttp.ErrorResponseWrapperException: Error " +
                                "during request], result=null}]",
                        "Can't get BS auction response for all phrases. Got errors in next bs-auction responses"
                },
                {
                        "Cut out transaction and cell numbers",
                        "YT error: Error committing transaction 18991-f8a25-3ee0001-672884c8 at cell " +
                                "63ca2d40-23f1484a-3ee0259-30cd7086",
                        "YT error: Error committing transaction at cell"
                },
                {
                        "Replace collection url with URL",
                        "CollectionUrl: https://yandex.ru/collections/api/links/redirect/?url=https%3A%2F%2Fi.pinimg" +
                                ".com%2F60ead55d1f9c932cc330b6141527c085.jpg doesn't match pattern",
                        "CollectionUrl: URL doesn't match pattern"
                },
                {
                        "Replace operator name with OPERATOR",
                        "Error while calling operator RUSS_OUTDOOR get_moderated for pageId: 410590.",
                        "Error while calling OPERATOR get_moderated for pageId: NUM."
                },
                {
                        "Replace user from 'access is denied for' with USER",
                        "access is denied for prgr-avon.",
                        "access is denied for USER."
                },
                {
                        "Replace cluster with CLUSTER",
                        "Can't get space usage for cluster SENECA_SAS, dir //home/direct/mysql-sync/current on " +
                                "medium = default" +
                                "\nCan't get remaining space for cluster SENECA_VLA on medium = default",
                        "Can't get space usage for CLUSTER, dir on MEDIUM" +
                                "\nCan't get remaining space for CLUSTER on MEDIUM"
                },
                {
                        "Replace number of attempts with NUM",
                        "Failed to create event reader, 13 attempts left",
                        "Failed to create event reader, NUM attempts left"
                },
                {
                        "Replace non-nullable type with TYPE",
                        "Cannot return null for non-nullable type: 'String'",
                        "Cannot return null for non-nullable TYPE"
                },
                {
                        "Replace required type and parameter name with TYPE PARAMETER",
                        "Required Long parameter 'reminder' is not present",
                        "Required TYPE PARAMETER is not present"
                },
                {
                        "Replace short message with MESSAGE",
                        "short message: Нет прав",
                        "MESSAGE"
                },
                {
                        "Replace path from NonNullableFieldWasNullError with PATH",
                        "NonNullableFieldWasNullError{message='Cannot return null for non-nullable type within parent" +
                                " 'GdSitelinkTurbolanding' (/client/id)', path=[client, turbolanding, id]}",
                        "NonNullableFieldWasNullError{message='Cannot return null for non-nullable type within parent" +
                                " 'GdSitelinkTurbolanding' (/client/id)', PATH}"
                },
                {
                        "Replace ppc name with PPC",
                        "ppc_11__26 - Connection is not available; Can't ping database ppc:1",
                        "PPC - Connection is not available; Can't ping database PPC"
                },
                {
                        "Replace ppcdict name with PPCDICT",
                        "ppcdict__73 - Connection is not available",
                        "PPCDICT - Connection is not available"
                },
                {
                        "Replace time in ms with MILLISECONDS",
                        "4000ms; The last packet successfully received from the server was 5 milliseconds ago; " +
                                "Was 1,282 milliseconds ago; 26,219,103 milliseconds",
                        "MILLISECONDS; The last packet successfully received from the server was MILLISECONDS ago; " +
                                "Was MILLISECONDS ago; MILLISECONDS"
                },
                {
                        "Replace bytes' value with BYTES",
                        "Expected to read 10,807 bytes, read 4,994 bytes before connection was unexpectedly lost.",
                        "Expected to read BYTES, read BYTES before connection was unexpectedly lost."
                },
                {
                        "Replace date with DATETIME #1",
                        "2019-09-27T08:57:36.827+0000",
                        "DATETIME"
                },
                {
                        "Replace date with DATETIME #2",
                        "2019-10-04 01:20:51",
                        "DATETIME"
                },
                {
                        "Replace date with DATETIME #3",
                        "Thu, 03 Oct 2019 13:57:14 GMT",
                        "DATETIME"
                },
                {
                        "Replace ip address with IPADDRESS",
                        "[blackbox.yandex.net/213.180.225.35]" +
                                "\nAcl check failed for ip: /2a02:6b8:c0c:820c:10d:9ea6:0:4762",
                        "[blackbox.yandex.net/IPADDRESS]" +
                                "\nAcl check failed for ip: /IPADDRESS"
                },
                {
                        "Replace port of ip address with PORT",
                        "2a02:6b8:b040:1a3c:feaa:14ff:fed9:386b:9013",
                        "IPADDRESS:PORT"
                },
                {
                        "Replace shard name with SHARD",
                        "shard_16; shard=18",
                        "SHARD; SHARD"
                },
                {
                        "Replace RbacRole with ROLE",
                        "role=EMPTY; role=AGENCY",
                        "ROLE; ROLE"
                },
                {
                        "Replace BigDecimal value with BIGDECIMAL",
                        "Money on wallet must be greater than or equal to zero. Current value = -26.770000" +
                                "\nCannot coerce a floating-point value ('202.5')",
                        "Money on wallet must be greater than or equal to zero. Current value = BIGDECIMAL" +
                                "\nCannot coerce a floating-point value ('BIGDECIMAL')"
                },
                {
                        "Replace block id with NUM",
                        "Block <454034,10> has validation errors",
                        "Block <NUM,NUM> has validation errors"
                },
                {
                        "Replace numerical uid|id with NUM",
                        "Operator with uid 12345678 cannot access client with id 345;" +
                                "\nImage with id: 2 failed",
                        "Operator with uid NUM cannot access client with id NUM;" +
                                "\nImage with id: NUM failed"
                },
                {
                        "Replace number in square brackets with NUM",
                        "rowset[0]/sitelinks[1]",
                        "rowset[NUM]/sitelinks[NUM]"
                },
                {
                        "Graphql permissions check",
                        "Operator is blocked for resolver adsResume.",
                        "Operator is blocked for resolver RESOLVER."
                },
                {
                        "Replace row|line|column|port number with NUM",
                        "at [Source: (PushbackInputStream); line: 1, column: 84]; Row: 6; port: 443; Code: 241; " +
                                "Last retry failed: 23; Processing of request fails for regionId: 213; pageId: 5",
                        "at [Source: (PushbackInputStream); line: NUM, column: NUM]; Row: NUM; port: NUM; Code: NUM; " +
                                "Last retry failed: NUM; Processing of request fails for regionId: NUM; pageId: NUM"
                },
                {
                        "Replace with HEX16",
                        "request_id=32c27b867d70d572",
                        "request_id=HEX16"
                },
                {
                        "Replace numbers over 3 digits with NUM",
                        "\"TurboPageIds\":[10000000]",
                        "\"TurboPageIds\":[NUM]"
                },
                {
                        "Datetimes",
                        "Failed to parse uaas condition: clientCreateDate > '2020-07-21T12:00:00'; Caused by EL1007E:",
                        "Failed to parse uaas condition: clientCreateDate > 'DATETIME'; Caused by EL1007E:"
                },
                {
                        "Balance PromoCode already reserved",
                        "Balance2.CreateRequest2 call fault on try 1: <error><msg>Invalid parameter for function: PromoCode H2ST59TPMZUKQNZY already reserved at date 2020-12-30 00:00:00</msg><wo-rollback>0</wo-rollback><method>Balance2.CreateRequest2</method><code>INVALID_PARAM</code><parent-codes><code>EXCEPTION</code></parent-codes><contents>Invalid parameter for function: PromoCode H2ST59TPMZUKQNZY already reserved at date 2020-12-30 00:00:00</contents></error>",
                        "Balance2.CreateRequest2 PromoCode already reserved"
                },
                {
                        "Balance Promocode already has reservation",
                        "Balance2.CreateRequest2 call fault on try 1: <error><msg>Promocode: Can't reserve promocode. Already has reservation for FRSVYPDCDSMVUCRH</msg><tanker-fields>['promocode']</tanker-fields><promocode>FRSVYPDCDSMVUCRH</promocode><wo-rollback>0</wo-rollback><method>Balance2.CreateRequest2</method><code>CANT_RESERVE_PROMOCODE</code><parent-codes><code>INVALID_PARAM</code><code>EXCEPTION</code></parent-codes><contents>Promocode: Can't reserve promocode. Already has reservation for FRSVYPDCDSMVUCRH</contents></error>",
                        "Balance2.CreateRequest2 Promocode already has reservation"
                },
                {
                        "Balance invalid promocode invalid period",
                        "Balance2.CreateRequest2 call fault on try 1: <error><msg>Invalid promo code: ID_PC_INVALID_PERIOD</msg><wo-rollback>0</wo-rollback><method>Balance2.CreateRequest2</method><code>INVALID_PROMO_CODE</code><parent-codes><code>EXCEPTION</code></parent-codes><contents>Invalid promo code: ID_PC_INVALID_PERIOD</contents></error>; Caused by <error><msg>Invalid promo code: ID_PC_INVALID_PERIOD</msg><wo-rollback>0</wo-rollback><method>Balance2.CreateRequest2</method><code>INVALID_PROMO_CODE</code><parent-codes><code>EXCEPTION</code></parent-codes><contents>Invalid promo code: ID_PC_INVALID_PERIOD</contents></error> @ org.apache.xmlrpc.client.XmlRpcStreamTransport",
                        "Balance2.CreateRequest2 Invalid Promocode: ID_PC_INVALID_PERIOD"
                },
                {
                        "Balance invalid promocode unknown",
                        "Balance2.CreateRequest2 call fault on try 1: <error><msg>Invalid promo code: ID_PC_UNKNOWN</msg><wo-rollback>0</wo-rollback><method>Balance2.CreateRequest2</method><code>PROMOCODE_NOT_FOUND</code><parent-codes><code>INVALID_PROMO_CODE</code><code>EXCEPTION</code></parent-codes><contents>Invalid promo code: ID_PC_UNKNOWN</contents></error>; Caused by <error><msg>Invalid promo code: ID_PC_UNKNOWN</msg><wo-rollback>0</wo-rollback><method>Balance2.CreateRequest2</method><code>PROMOCODE_NOT_FOUND</code><parent-codes><code>INVALID_PROMO_CODE</code><code>EXCEPTION</code></parent-codes><contents>Invalid promo code: ID_PC_UNKNOWN</contents></error> @ org.apache.xmlrpc.client.XmlRpcStreamTransport",
                        "Balance2.CreateRequest2 Invalid Promocode: ID_PC_UNKNOWN"
                },
                {
                        "Balance invalid promocode not unique urls",
                        "Balance2.CreateRequest2 call fault on try 1: <error><msg>Invalid promo code: ID_PC_NOT_UNIQUE_URLS</msg><wo-rollback>0</wo-rollback><method>Balance2.CreateRequest2</method><code>INVALID_PROMO_CODE</code><parent-codes><code>EXCEPTION</code></parent-codes><contents>Invalid promo code: ID_PC_NOT_UNIQUE_URLS</contents></error>; Caused by <error><msg>Invalid promo code: ID_PC_NOT_UNIQUE_URLS</msg><wo-rollback>0</wo-rollback><method>Balance2.CreateRequest2</method><code>INVALID_PROMO_CODE</code><parent-codes><code>EXCEPTION</code></parent-codes><contents>Invalid promo code: ID_PC_NOT_UNIQUE_URLS</contents></error> @ org.apache.xmlrpc.client.XmlRpcStreamTransport",
                        "Balance2.CreateRequest2 Invalid Promocode: ID_PC_NOT_UNIQUE_URLS"
                },
                {
                        "Balance invalid promocode used",
                        "Balance2.CreateRequest2 call fault on try 1: <error><msg>Invalid promo code: ID_PC_USED</msg><wo-rollback>0</wo-rollback><method>Balance2.CreateRequest2</method><code>INVALID_PROMO_CODE</code><parent-codes><code>EXCEPTION</code></parent-codes><contents>Invalid promo code: ID_PC_USED</contents></error>; Caused by <error><msg>Invalid promo code: ID_PC_USED</msg><wo-rollback>0</wo-rollback><method>Balance2.CreateRequest2</method><code>INVALID_PROMO_CODE</code><parent-codes><code>EXCEPTION</code></parent-codes><contents>Invalid promo code: ID_PC_USED</contents></error> @ org.apache.xmlrpc.client.XmlRpcStreamTransport",
                        "Balance2.CreateRequest2 Invalid Promocode: ID_PC_USED"
                },
                {
                        "Balance invalid promocode wrong client",
                        "Balance2.CreateRequest2 call fault on try 1: <error><code>RCV2453V92D3WMVJ</code><target-client>71436338</target-client><wo-rollback>0</wo-rollback><bound-client>86401636</bound-client><msg>Can't reserve promocode RCV2453V92D3WMVJ for client 71436338. Promocode is bound to client 86401636.</msg><tanker-fields>['code']</tanker-fields><method>Balance2.CreateRequest2</method><code>PROMOCODE_WRONG_CLIENT</code><parent-codes><code>EXCEPTION</code></parent-codes><contents>Can't reserve promocode RCV2453V92D3WMVJ for client 71436338. Promocode is bound to client 86401636.</contents></error>; Caused by <error><code>RCV2453V92D3WMVJ</code><target-client>71436338</target-client><wo-rollback>0</wo-rollback><bound-client>86401636</bound-client><msg>Can't reserve promocode RCV2453V92D3WMVJ for client 71436338. Promocode is bound to client 86401636.</msg><tanker-fields>['code']</tanker-fields><method>Balance2.CreateRequest2</method><code>PROMOCODE_WRONG_CLIENT</code><parent-codes><code>EXCEPTION</code></parent-codes><contents>Can't reserve promocode RCV2453V92D3WMVJ for client 71436338. Promocode is bound to client 86401636.</contents></error> @ org.apache.xmlrpc.client.XmlRpcStreamTransport",
                        "Balance2.CreateRequest2 Invalid Promocode: PROMOCODE_WRONG_CLIENT"
                },
                {
                        "Balance invalid promocode not new client",
                        "Balance2.CreateRequest2 call fault on try 1: <error><msg>Invalid promo code: ID_PC_NOT_NEW_CLIENT</msg><wo-rollback>0</wo-rollback><method>Balance2.CreateRequest2</method><code>INVALID_PROMO_CODE</code><parent-codes><code>EXCEPTION</code></parent-codes><contents>Invalid promo code: ID_PC_NOT_NEW_CLIENT</contents></error>; Caused by <error><msg>Invalid promo code: ID_PC_NOT_NEW_CLIENT</msg><wo-rollback>0</wo-rollback><method>Balance2.CreateRequest2</method><code>INVALID_PROMO_CODE</code><parent-codes><code>EXCEPTION</code></parent-codes><contents>Invalid promo code: ID_PC_NOT_NEW_CLIENT</contents></error> @ org.apache.xmlrpc.client.XmlRpcStreamTransport",
                        "Balance2.CreateRequest2 Invalid Promocode: ID_PC_NOT_NEW_CLIENT"
                },
                {
                        "Balance No payment options available",
                        "Balance2.PayRequest call fault on try 1: <error><msg>Invalid parameter for function: No payment options available</msg><wo-rollback>0</wo-rollback><method>Balance2.PayRequest</method><code>INVALID_PARAM</code><parent-codes><code>EXCEPTION</code></parent-codes><contents>Invalid parameter for function: No payment options available</contents></error> org.apache.xmlrpc.XmlRpcException: <error><msg>Invalid parameter for function: No payment options available</msg><wo-rollback>0</wo-rollback><method>Balance2.PayRequest</method><code>INVALID_PARAM</code><parent-codes><code>EXCEPTION</code></parent-codes><contents>Invalid parameter for function: No payment options available</contents></error>",
                        "Balance2.PayRequest No payment options available"
                },
                {
                        "Mask JSESSIONID",
                        "Set-Cookie: JSESSIONID=node01y05xqfnz1cma8tklqlf08c872974577.node0",
                        "Set-Cookie: JSESSIONID=..."
                },
                {
                        "can't get screenshot from Rotor (statusCode=502)",
                        "can't get screenshot from Rotor ([ru.yandex.direct.asynchttp.ErrorResponseWrapperException: Error during request (NettyResponse { statusCode=502 headers= Content-Type: text/plain Content-Length: 246 Connection: Close body= (yexception) yweb/robot/js/rotor/clientlib/addressresolver.cpp:140: Couldn't find server for url: https://storage.mds.yandex.net/get-bstor/3911536/f2c6b171-a6dd-4e80-b850-c865eb16ea73.txt, no alive servers;",
                        "can't get screenshot from Rotor (statusCode=502)"
                },
                {
                        "can't get screenshot from Rotor (statusCode=400)",
                        "can't get screenshot from Rotor ([ru.yandex.direct.asynchttp.ErrorResponseWrapperException: Error during request (NettyResponse { statusCode=400 headers= Content-Type: text/plain Content-Length: 34 Connection: Keep-Alive body= fail to fetch main frame (3): 1010 }), ru.yandex.direct.asynchttp.ErrorResponseWrapperException: Error during request (NettyResponse { statusCode=400 headers= Content-Type: text/plain Content-Length: 34 Con",
                        "can't get screenshot from Rotor (statusCode=400)"
                },
                {
                        "Hourglass job error",
                        "7de05ebe-92f5-4272-a61a-4e6664cb9983: Job ru.yandex.direct.logicprocessor.processors.uac" +
                                ".updatestatuses.UacUpdateStatusesProcessor",
                        "UUID: Job ru.yandex.direct.logicprocessor.processors.uac.updatestatuses" +
                                ".UacUpdateStatusesProcessor"
                },
                {
                        "bsclient error",
                        "[pid=67,reqid=2207396633074249774,uuid=f34d8d25-b486-4d08-b18b-d84e67b61fae," +
                                "data_type=deserialization_error]\tcan not deserialize object from json",
                        "[pid=NUM,reqid=NUM,uuid=UUID,data_type=deserialization_error]\tcan not deserialize object " +
                                "from json"
                },
                {
                        "alw error",
                        "Can't handle 390 rows due to absent dictionary data for CAMPAIGN_PATH{20232312, 21485450, " +
                                "21636627, 21693150, 21694414, 21694531, 21694907, 21695174, 21695283, 21695290, " +
                                "21695356, 21695761, 21695835, 21696017, 21696142}",
                        "Can't handle 390 rows due to absent dictionary data for CAMPAIGN_PATH{LIST_OF_NUMS}"
                }
        };
    }

    @Test
    public void processEventMessage() {
        assertThat(errorMessageNormalizer.normalize(messageBefore)).isEqualTo(messageAfter);
    }
}

package ru.yandex.direct.bsexport.messaging.soap;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import com.google.common.primitives.UnsignedLong;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.direct.bsexport.messaging.SoapResponseDeserializer;
import ru.yandex.direct.bsexport.model.BannerResponse;
import ru.yandex.direct.bsexport.model.ContextResponse;
import ru.yandex.direct.bsexport.model.DynamicResponse;
import ru.yandex.direct.bsexport.model.OrderResponse;
import ru.yandex.direct.bsexport.model.PhraseResponse;
import ru.yandex.direct.bsexport.model.RetargetingResponse;
import ru.yandex.direct.bsexport.model.SoapFault;
import ru.yandex.direct.bsexport.model.UpdateData2Response;
import ru.yandex.direct.bsexport.testing.Util;

import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class ResponseDeserializationTest {

    /**
     * Проверяем на двух дампах: один as/is, второй отформатирован pretty (так удобнее в тестах).
     * До теста оказалось, что разница в поведении десериализатора - есть (лишние пустые ноды в XML)
     */
    @ParameterizedTest
    @ValueSource(strings = {"soap/update_data2_sandbox_response.xml", "soap/update_data2_sandbox_response_pretty.xml"})
    void fakeBsSuccessResponse(String dumpPath) {
        var msg = getMessage(dumpPath);
        var response = deserialize(msg);

        var expectedResponse = UpdateData2Response.newBuilder()
                .setBACKENDHOST("direct-ts-1-2.man.yp-c.yandex.net")
                .putORDER("O_30752761_6080204", OrderResponse.newBuilder()
                        .setStop(false)
                        .setArchive(false)
                        .setEID(6080204)
                        .setID(30752761)
                        .putCONTEXT("C_0_144465739", ContextResponse.newBuilder()
                                .setID(65739)
                                .setEID(144465739)
                                .putPHRASE("P_0_1197262186", PhraseResponse.newBuilder()
                                        .setID(uLong("14610404315530459246"))
                                        .setEID(1197262186)
                                        .build())
                                .putPHRASE("P_0_1197262187", PhraseResponse.newBuilder()
                                        .setID(uLong("1983455406884274728"))
                                        .setEID(1197262187)
                                        .build())
                                .putBANNER("B_72057594184733972_146806036", BannerResponse.newBuilder()
                                        .setID(72057594184733972L)
                                        .setEID(146806036)
                                        .setStop(true)
                                        .build())
                                .build())
                        .putCONTEXT("C_0_151277834", ContextResponse.newBuilder()
                                .setID(77834)
                                .setEID(151277834)
                                .putPHRASE("P_0_1374917357", PhraseResponse.newBuilder()
                                        .setID(uLong("16599972141514856879"))
                                        .setEID(1374917357)
                                        .build())
                                .putBANNER("B_72057594191880311_153952375", BannerResponse.newBuilder()
                                        .setID(72057594191880311L)
                                        .setEID(153952375)
                                        .setStop(true)
                                        .build())
                                .putGOALCONTEXT("G_2685751_16250701", RetargetingResponse.newBuilder()
                                        .setID(2685751)
                                        .setEID(16250701)
                                        .build())
                                .putGOALCONTEXT("G_2685756_16250706", RetargetingResponse.newBuilder()
                                        .setID(2685756)
                                        .setEID(16250706)
                                        .build())
                                .build())
                        .build())
                .putORDER("O_30752766_14248907", OrderResponse.newBuilder()
                        .setStop(true)
                        .setArchive(false)
                        .setID(30752766)
                        .setEID(14248907)
                        .putCONTEXT("C_0_887983004", ContextResponse.newBuilder()
                                .setID(887983004)
                                .setEID(887983004)
                                .putPHRASE("P_0_20293246631", PhraseResponse.newBuilder()
                                        .setID(uLong("9469765645952675046"))
                                        .setEID(20293246631L)
                                        .build())
                                .putPHRASE("P_0_4160092016", PhraseResponse.newBuilder()
                                        .setID(uLong("9501645597100806969"))
                                        .setEID(4160092016L)
                                        .build())
                                .putBANNER("B_72057595226106708_1188178772", BannerResponse.newBuilder()
                                        .setID(72057595226106708L)
                                        .setEID(1188178772)
                                        .setStop(true)
                                        .build())
                                .putBANNER("B_72057603109238152_9071310216", BannerResponse.newBuilder()
                                        .setID(72057603109238152L)
                                        .setEID(9071310216L)
                                        .setStop(true)
                                        .build())
                                .build())
                        .build())
                .build();

        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    void faultResponseTest() {
        var msg = getMessage("soap/update_data2_fault_response.xml");
        var responseDeserializer = createParser(msg);

        Map<String, String> details = Map.of("Yabs__Logger__Exception", "ppalex test at " +
                "/usr/share/perl5/Yabs/Transport/API.pm line 336\n" +
                "Yabs::Transport::API::update_data('HASH(0x7f144b32ed30)', 1) called at /var/www/bssoap.yandex" +
                ".ru/utf8/YaBSSOAPExport.pm line 45\n" +
                "YaBSSOAPExport::UpdateData2('YaBSSOAPExport', 'HASH(0x7f144b32ed30)', 1) called at " +
                "/usr/share/perl5/SOAP/Lite.pm line 2179\n" +
                "eval {...} called at /usr/share/perl5/SOAP/Lite.pm line 2170\n" +
                "eval {...} called at /usr/share/perl5/SOAP/Lite.pm line 2142\n" +
                "SOAP::Server::handle('SOAP::Transport::HTTP2::Apache=HASH(0x7f144b2380f8)', '<?xml version=\"1.0\" " +
                "encoding=\"UTF-8\"?><SOAP-ENV:Envelope SOAP...') called at /usr/share/perl5/SOAP/Transport/HTTP.pm " +
                "line 258\n" +
                "SOAP::Transport::HTTP::Server::handle('SOAP::Transport::HTTP2::Apache=HASH(0x7f144b2380f8)') called " +
                "at /usr/share/perl5/SOAP/Transport/HTTP2.pm line 105\n" +
                "SOAP::Transport::HTTP2::Apache::handler('SOAP::Transport::HTTP2::Apache=HASH(0x7f144b2380f8)', " +
                "'Apache2::RequestRec=SCALAR(0x7f144485f298)') called at /var/www/bssoap.yandex" +
                ".ru/utf8//YaBSSOAPApache.pm line 36\n" +
                "YaBSSOAPApache::handler('Apache2::RequestRec=SCALAR(0x7f144485f298)') called at -e line 0\n" +
                "eval {...} called at -e line 0\n" +
                "\n" +
                "ppalex test\n" +
                "Yabs::Transport::API");
        var expected = new SoapFault()
                .withFaultCode("SOAP-ENV:Server")
                .withFaultString("Application error")
                .withFaultDetails(details);

        Assertions.assertThat(responseDeserializer.hasFault()).isTrue();
        Assertions.assertThat(responseDeserializer.getFault()).isEqualToComparingFieldByField(expected);
    }

    @Test
    void errorResponseTest() {
        var msg = getMessage("soap/update_data2_error_response.xml");
        var response = deserialize(msg);

        var expectedResponse = UpdateData2Response.newBuilder()
                .setError(true)
                .setErrorMessage("Service is not supported for EngineID: 9999")
                .build();

        assertThat(response).isEqualTo(expectedResponse);
    }

    /**
     * Пустой ответ (без данных) настоящего bssoap
     */
    @Test
    void emptyResponseTest1() {
        var msg = getMessage("soap/update_data2_empty_response1.xml");
        var response = deserialize(msg);

        var expectedResponse = UpdateData2Response.newBuilder()
                .setBACKENDHOST("bssoap02f")
                .build();

        assertThat(response).isEqualTo(expectedResponse);
    }

    /**
     * Еще один ответ (без данных) настоящего bssoap, отличается SOAP-заголовками
     */
    @Test
    void emptyResponseTest2() {
        var msg = getMessage("soap/update_data2_empty_response2.xml");
        var response = deserialize(msg);

        var expectedResponse = UpdateData2Response.newBuilder()
                .setBACKENDHOST("bssoap02i")
                .build();

        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    void singleOrderResponseTest() {
        var msg = getMessage("soap/update_data2_single_order_response.xml");
        var response = deserialize(msg);

        var expectedResponse = UpdateData2Response.newBuilder()
                .setBACKENDHOST("bssoap01i")
                .putORDER("O_28165270_45998453", OrderResponse.newBuilder()
                        .setStop(false)
                        .setArchive(false)
                        .setEID(45998453)
                        .setID(28165270)
                        .build())
                .build();

        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    void severalOrdersUnDoneResponseTest() {
        var msg = getMessage("soap/update_data2_response_with_order_undone.xml");
        var response = deserialize(msg);

        var expectedResponse = UpdateData2Response.newBuilder()
                .setBACKENDHOST("bssoap02e")
                .putORDER("O_30578035_48372868", OrderResponse.newBuilder()
                        .setID(30578035)
                        .setEID(48372868)
                        .setUnDone(true)
                        .build())
                .putORDER("O_30553172_48315231", OrderResponse.newBuilder()
                        .setID(30553172)
                        .setEID(48315231)
                        .setUnDone(true)
                        .build())
                .putORDER("O_30578050_48372861", OrderResponse.newBuilder()
                        .setID(30578050)
                        .setEID(48372861)
                        .setUnDone(true)
                        .build())
                .build();

        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    void severalBannersAndPhrasesResponseTest() {
        var msg = getMessage("soap/update_data2_repsonse_with_several_banners_and_phrases.xml");
        var response = deserialize(msg);

        var expectedResponse = UpdateData2Response.newBuilder()
                .setBACKENDHOST("bssoap04i")
                .putORDER("O_28533384_46115655", OrderResponse.newBuilder()
                        .setID(28533384)
                        .setEID(46115655)
                        .setArchive(false)
                        .setStop(true)
                        .putCONTEXT("C_1750517905_3966014975", ContextResponse.newBuilder()
                                .setID(1750517905)
                                .setEID(3966014975L)
                                .putPHRASE("P_464252430_18377983502", PhraseResponse.newBuilder()
                                        .setID(464252430)
                                        .setEID(18377983502L)
                                        .build())
                                .putPHRASE("P_5496855252522264931_18377983494", PhraseResponse.newBuilder()
                                        .setID(5496855252522264931L)
                                        .setEID(18377983494L)
                                        .build())
                                .putPHRASE("P_8146180308404837884_18377983499", PhraseResponse.newBuilder()
                                        .setID(8146180308404837884L)
                                        .setEID(18377983499L)
                                        .build())
                                .putPHRASE("P_17166161511921246889_18377983496", PhraseResponse.newBuilder()
                                        .setID(uLong("17166161511921246889"))
                                        .setEID(18377983496L)
                                        .build())
                                .putBANNER("B_7266476366_8043070205", BannerResponse.newBuilder()
                                        .setID(7266476366L)
                                        .setEID(8043070205L)
                                        .setStop(false)
                                        .build())
                                .putBANNER("B_7266476367_8043070206", BannerResponse.newBuilder()
                                        .setID(7266476367L)
                                        .setEID(8043070206L)
                                        .setStop(true)
                                        .build())
                                .putBANNER("B_72057602820735507_8782807571", BannerResponse.newBuilder()
                                        .setID(72057602820735507L)
                                        .setEID(8782807571L)
                                        .setStop(true)
                                        .build())
                                .build())
                        .build())
                .build();

        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    void goalContextResponseTest() {
        var msg = getMessage("soap/update_data2_response_with_goal_context.xml");
        var response = deserialize(msg);

        var expectedResponse = UpdateData2Response.newBuilder()
                .setBACKENDHOST("bssoap03e")
                .putORDER("O_31839340_50583481", OrderResponse.newBuilder()
                        .setID(31839340)
                        .setEID(50583481)
                        .setArchive(false)
                        .setStop(false)
                        .putCONTEXT("C_1292654011_4147382313", ContextResponse.newBuilder()
                                .setID(1292654011)
                                .setEID(4147382313L)
                                .putGOALCONTEXT("3160061", RetargetingResponse.newBuilder()
                                        .setID(3160061)
                                        .setEID(15690380)
                                        .build())
                                .putGOALCONTEXT("3107119", RetargetingResponse.newBuilder()
                                        .setID(3107119)
                                        .setEID(15628227)
                                        .build())
                                .putBANNER("B_72057602858632759_8820704823", BannerResponse.newBuilder()
                                        .setID(72057602858632759L)
                                        .setEID(8820704823L)
                                        .setStop(false)
                                        .build())
                                .build())
                        .build())
                .build();

        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    void dynamicResponseTest() {
        var msg = getMessage("soap/update_data2_response_with_dynamic.xml");
        var response = deserialize(msg);

        var expectedResponse = UpdateData2Response.newBuilder()
                .putORDER("O_32025856_50862441", OrderResponse.newBuilder()
                        .setID(32025856)
                        .setEID(50862441)
                        .setArchive(false)
                        .setStop(true)
                        .putCONTEXT("C_739115077_4156769079", ContextResponse.newBuilder()
                                .setID(739115077)
                                .setEID(4156769079L)
                                .putBANNER("B_72057602903803102_8865875166", BannerResponse.newBuilder()
                                        .setID(72057602903803102L)
                                        .setEID(8865875166L)
                                        .setStop(false)
                                        .build())
                                .putDYNAMIC("1077755", DynamicResponse.newBuilder()
                                        .setID(1077755)
                                        .setEID(1077755)
                                        .build())
                                .putDYNAMIC("1077754", DynamicResponse.newBuilder()
                                        .setID(1077754)
                                        .setEID(1077754)
                                        .build())
                                .build())
                        .build())
                .setBACKENDHOST("bssoap04i")
                .build();

        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    void dynamicFakeBsResponseTest() {
        var msg = getMessage("soap/update_data2_fakebs_response_with_dynamic.xml");
        var response = deserialize(msg);

        var expectedResponse = UpdateData2Response.newBuilder()
                .putORDER("O_11582729_22010666", OrderResponse.newBuilder()
                        .setStop(false)
                        .setID(11582729)
                        .setEID(22010666)
                        .putCONTEXT("C_1758888132_2023908093", ContextResponse.newBuilder()
                                .setID(1758888132)
                                .setEID(2023908093L)
                                .putBANNER("B_2668178307_3007183723", BannerResponse.newBuilder()
                                        .setID(2668178307L)
                                        .setEID(3007183723L)
                                        .setStop(false)
                                        .build())
                                .putDYNAMIC("D_356990_356990", DynamicResponse.newBuilder()
                                        .setID(356990)
                                        .setEID(356990)
                                        .build())
                                .build())
                        .putCONTEXT("C_1758888132_2023939629", ContextResponse.newBuilder()
                                .setID(1758888132)
                                .setEID(2023939629L)
                                .putBANNER("B_2668178302_3007257914", BannerResponse.newBuilder()
                                        .setID(2668178302L)
                                        .setEID(3007257914L)
                                        .setStop(false)
                                        .build())
                                .putDYNAMIC("D_1198996_1198996", DynamicResponse.newBuilder()
                                        .setID(1198996)
                                        .setEID(1198996)
                                        .build())
                                .putDYNAMIC("D_208157_208157", DynamicResponse.newBuilder()
                                        .setID(208157)
                                        .setEID(208157)
                                        .build())
                                .build())
                        .build())
                .setBACKENDHOST("direct-ts-1-vla-2.vla.yp-c.yandex.net")
                .build();
        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    void bannerErrorTest() {
        var msg = getMessage("soap/update_data2_response_with_banner_error.xml");
        var response = deserialize(msg);

        var expectedResponse = UpdateData2Response.newBuilder()
                .putORDER("O_17702790_30932983", OrderResponse.newBuilder()
                        .setID(17702790)
                        .setEID(30932983)
                        .setArchive(false)
                        .setStop(true)
                        .putCONTEXT("C_655032394_3006308918", ContextResponse.newBuilder()
                                .setID(655032394)
                                .setEID(3006308918L)
                                .putPHRASE("P_22069994_11388566158", PhraseResponse.newBuilder()
                                        .setID(22069994)
                                        .setEID(11388566158L)
                                        .build())
                                .putBANNER("B_5091527709_4946717546", BannerResponse.newBuilder()
                                        .setID(0)
                                        .setEID(4946717546L)
                                        .setStop(false)
                                        .setError(true)
                                        .setErrorMessage("Not enough resources for 100-days-archived banner" +
                                                ", BannerID=5091527709, ExportID=4946717546")
                                        .build())
                                .build())
                        .build())
                .setBACKENDHOST("bssoap03i")
                .build();

        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    void contextErrorTest() {
        var msg = getMessage("soap/update_data2_response_with_context_error.xml");
        var response = deserialize(msg);

        var expectedResponse = UpdateData2Response.newBuilder()
                .putORDER("O_10290590_20160695", OrderResponse.newBuilder()
                        .setID(10290590)
                        .setEID(20160695)
                        .setArchive(false)
                        .setStop(false)
                        .putCONTEXT("C_1310953453_1711913644", ContextResponse.newBuilder()
                                .setID(0)
                                .setEID(1711913644)
                                .putBANNER("B_1998715206_2486397870", BannerResponse.newBuilder()
                                        .setID(1998715206)
                                        .setEID(2486397870L)
                                        .setStop(false)
                                        .build())
                                .putBANNER("B_4701427098_4711845141", BannerResponse.newBuilder()
                                        .setID(4701427098L)
                                        .setEID(4711845141L)
                                        .setStop(false)
                                        .build())
                                .setError(true)
                                .setErrorMessage("Context has archived banner that is being activated" +
                                        ", but UpdateInfo hasn't been sent")
                                .build())
                        .build())
                .setBACKENDHOST("bssoap02i")
                .build();

        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    void orderErrorTest() {
        var msg = getMessage("soap/update_data2_response_with_order_error.xml");
        var response = deserialize(msg);

        var expectedResponse = UpdateData2Response.newBuilder()
                .putORDER("O_0_14248907", OrderResponse.newBuilder()
                        .setID(0)
                        .setEID(14248907)
                        .setErrorMessage("Not found joint account for GroupOrderID: 33333 on order EID: 14248907")
                        .setError(true)
                        .putCONTEXT("C_887983004_887983004", ContextResponse.newBuilder()
                                .setID(887983004)
                                .setEID(887983004)
                                .putPHRASE("P_9469765645952675046_20293246631", PhraseResponse.newBuilder()
                                        .setID(uLong("9469765645952675046"))
                                        .setEID(20293246631L)
                                        .build())
                                .putBANNER("B_72057603109238152_9071310216", BannerResponse.newBuilder()
                                        .setID(72057603109238152L)
                                        .setEID(9071310216L)
                                        .setStop(false)
                                        .build())
                                .build())
                        .build())
                .setBACKENDHOST("bssoap01g")
                .build();

        assertThat(response).isEqualTo(expectedResponse);
    }

    private static long uLong(String value) {
        return UnsignedLong.valueOf(value).longValue();
    }

    private static SoapResponseDeserializer createParser(SOAPMessage soapMessage) {
        try {
            return new SoapResponseDeserializer(soapMessage);
        } catch (SOAPException e) {
            return fail(e);
        }
    }

    private static UpdateData2Response deserialize(SOAPMessage soapMessage) {
        return createParser(soapMessage).deserialize();
    }

    private static SOAPMessage getMessage(String resourceName) {
        URL resource = Util.class.getClassLoader().getResource(resourceName);
        try {
            MessageFactory messageFactory = MessageFactory.newInstance();
            return messageFactory.createMessage(null, resource.openStream());
        } catch (SOAPException | IOException | NullPointerException e) {
            throw new RuntimeException(e);
        }
    }
}

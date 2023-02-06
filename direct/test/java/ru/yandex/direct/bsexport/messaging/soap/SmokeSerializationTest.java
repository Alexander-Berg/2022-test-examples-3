package ru.yandex.direct.bsexport.messaging.soap;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.bsexport.messaging.BaseSerializationTest;
import ru.yandex.direct.bsexport.messaging.SoapSerializer;
import ru.yandex.direct.bsexport.model.Order;
import ru.yandex.direct.bsexport.model.UpdateData2Request;
import ru.yandex.direct.bsexport.model.UpdateData2RequestSoapMessage;
import ru.yandex.direct.bsexport.testing.data.TestContext;
import ru.yandex.direct.bsexport.testing.data.TestOrder;
import ru.yandex.direct.bsexport.util.QueryComposer;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.bsexport.testing.Util.getFromClasspath;

/**
 * Тест на сериализацию запроса в XML.
 * Проверяется базовая структура запроса и типы некоторых полей.
 * <p>
 * Как получен ожидаемый результат: запрос из транспорта направлен в {@code netcat -lnp port > req.dump},
 * к файлу применено формматирование XML и удалены поля, которых в запросе не было.
 * Кроме этого учтены отличия в сериализации, описанные в {@link SoapSerializer}
 */
class SmokeSerializationTest extends BaseSerializationTest {

    @Test
    void test() {
        Order.Builder orderBuilder1 = TestOrder.textWithUpdateInfo1Full.toBuilder();
        QueryComposer.putContext(orderBuilder1, TestContext.baseWithUpdateInfo1);
        QueryComposer.putContext(orderBuilder1, TestContext.baseWithUpdateInfo2);

        Order.Builder orderBuilder2 = TestOrder.cpmBannerWithUpdateInfo1Full.toBuilder();
        QueryComposer.putContext(orderBuilder2, TestContext.cpmBannerWithUpdateInfo1);
        QueryComposer.putContext(orderBuilder2, TestContext.cpmBannerWithUpdateInfo2);

        UpdateData2Request.Builder updateData2RequestBuilder = UpdateData2Request.newBuilder()
                .setEngineID(7)
                .setRequestUUID("0076319C-0F98-11EA-A61D-62C3229A9442");
        QueryComposer.putOrder(updateData2RequestBuilder, orderBuilder1.build());
        QueryComposer.putOrder(updateData2RequestBuilder, orderBuilder2.build());

        UpdateData2RequestSoapMessage request = UpdateData2RequestSoapMessage.newBuilder()
                .setRequest(updateData2RequestBuilder)
                .setWorkerID(1001)
                .build();

        serialize(request);

        String expected = getFromClasspath("soap/update_data2_smoke.xml");
        assertThat(soap).isXmlEqualTo(expected);

    }
}

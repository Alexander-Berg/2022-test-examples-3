package ru.yandex.market.api.common;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Function;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;

import ru.yandex.market.api.MockClientHelper;
import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.server.context.Context;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.ClientHelper;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;

/**
 * Created by tesseract on 11.01.17.
 */
@WithMocks
public class FunctionsTest extends UnitTestBase {
    enum TestEnum {
        GOOD_VALUE,
        OTHER_VALUE
    }

    @Mock
    ClientHelper clientHelper;

    MockClientHelper mockClientHelper;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mockClientHelper = new MockClientHelper(clientHelper);
    }

    @Test
    public void extractUserIp_bixby() {
        Context ctx = getContext("12912", Client.Type.EXTERNAL);
        HttpServletRequest request = prepareRequest();

        mockClientHelper.is(ClientHelper.Type.BIXBY, true);

        String result = Functions.extractUserIp(ctx, request, clientHelper);

        Assert.assertEquals("3.3.3.3", result);
    }

    @Test
    public void extractUserIp_external() {
        Context ctx = getContext("123", Client.Type.EXTERNAL);
        HttpServletRequest request = prepareRequest();

        mockClientHelper.is(ClientHelper.Type.BIXBY, false);

        String result = Functions.extractUserIp(ctx, request, clientHelper);

        Assert.assertEquals("5.5.5.5", result);
    }

    @Test
    public void extractUserIp_internal() {
        Context ctx = getContext("123", Client.Type.INTERNAL);
        HttpServletRequest request = prepareRequest();

        mockClientHelper.is(ClientHelper.Type.BIXBY, false);

        String result = Functions.extractUserIp(ctx, request, clientHelper);

        Assert.assertEquals("2.2.2.2", result);
    }

    @Test
    public void extractUserIp_mobile() {
        Context ctx = getContext("101", Client.Type.MOBILE);

        HttpServletRequest request = prepareRequest();

        mockClientHelper.is(ClientHelper.Type.BIXBY, false);

        String result = Functions.extractUserIp(ctx, request, clientHelper);

        Assert.assertEquals("3.3.3.3", result);
    }

    @Test
    public void extractBlackboxIp_empty() {
        String ip = Functions.extractIpForBlackbox(MockRequestBuilder.start()
                .build(), false);
        Assert.assertNull(ip);
        ip = Functions.extractIpForBlackbox(MockRequestBuilder.start()
                .header("X-Forwarded-For-Y", "")
                .build(), false);
        Assert.assertNull(ip);
    }

    @Test
    public void extractBlackboxIp_first() {
        String ip = Functions.extractIpForBlackbox(MockRequestBuilder.start()
                .header("X-Forwarded-For-Y", "1.1.1.1,2.2.2.2")
                .build(), false);
        Assert.assertEquals("1.1.1.1", ip);
    }

    @Test
    public void extractBlackboxIp_only() {
        String ip = Functions.extractIpForBlackbox(MockRequestBuilder.start()
                .header("X-Forwarded-For-Y", "3.3.3.3")
                .build(), false);
        Assert.assertEquals("3.3.3.3", ip);
    }

    /**
     * ?????????????????? ???????????????????????? ?????????????????? Enum-???????????????? ?????? ?????????????????????????? ????????????????
     */
    @Test
    public void safeEnumParserGoodValue() {
        // ??????????????????
        Function<String, TestEnum> parser = CommonFunctions.safeEnumParser(TestEnum.class, null);
        // ??????????
        TestEnum result = parser.apply("good_value");
        // ????????????????
        Assert.assertEquals(TestEnum.GOOD_VALUE, result);
    }

    /**
     * ?????????????????? ???????????????????????? ?????????????????? Enum-???????????????? ?????? null-??
     */
    @Test
    public void safeEnumParserNullValue() {
        // ??????????????????
        Function<String, TestEnum> parser = CommonFunctions.safeEnumParser(TestEnum.class, TestEnum.OTHER_VALUE);
        // ??????????
        TestEnum result = parser.apply(null);
        // ????????????????
        Assert.assertEquals("???????????? ???????????????? ?????????????????? ???????????????? ???? ?????????????????? ?????????????? ??.??. ???????????????????? ???????????????? = null", TestEnum.OTHER_VALUE, result);
    }

    /**
     * ?????????????????? ???????????????????????? ?????????????????? Enum-???????????????? ?????? ?????????????????????????? ????????????????
     */
    @Test
    public void safeEnumParserWrongValue() {
        // ??????????????????
        Function<String, TestEnum> parser = CommonFunctions.safeEnumParser(TestEnum.class, TestEnum.OTHER_VALUE);
        // ??????????
        TestEnum result = parser.apply("wrong_value");
        // ????????????????
        Assert.assertEquals("???????????? ???????????????? ?????????????????? ???????????????? ???? ?????????????????? ?????????????? ??.??. ???????????????????? ???????????????? ???? ???????????????????? ?? ????????????????????????", TestEnum.OTHER_VALUE, result);
    }

    @NotNull
    private Context getContext(String id, Client.Type type) {
        Client client = new Client();
        client.setId(id);
        client.setType(type);

        Context ctx = new Context("");
        ctx.setClient(client);
        return ctx;
    }

    @NotNull
    private HttpServletRequest prepareRequest() {
        return MockRequestBuilder.start()
            .header("X-Forwarded-For", "1.1.1.1, 2.2.2.2, 3.3.3.3, 4.4.4.4")
            .header("X-Real-IP", "5.5.5.5")
            .remoteAddr("6.6.6.6")
            .build();
    }
}

package ru.yandex.market.api.common.client.rules;

import java.util.Collections;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.api.MockClientHelper;
import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.common.client.InternalClientVersionInfo;
import ru.yandex.market.api.common.client.KnownMobileClientVersionInfo;
import ru.yandex.market.api.common.client.SemanticVersion;
import ru.yandex.market.api.integration.ContainerTestBase;
import ru.yandex.market.api.internal.common.DeviceType;
import ru.yandex.market.api.internal.common.Platform;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.ClientHelper;
import ru.yandex.market.api.server.sec.client.internal.TestTariffs;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;

@WithContext
@ActiveProfiles(BlueRuleTest.PROFILE)
public class BlueRuleTest extends ContainerTestBase {
    static final String PROFILE = "BlueRuleTest";
    public static final String BLUE_METHOD_NAME = "V3/affiliate/beru/search";

    @Configuration
    @Profile(PROFILE)
    public static class Config {
        @Primary
        @Bean
        public ClientHelper localHelper() {
            return Mockito.mock(ClientHelper.class);
        }
    }

    private class RuleExpected {
        private Boolean isBlue;
        private Boolean isBlueMobile;
        private Boolean isBlueInternal;

        public RuleExpected setIsBlue(Boolean isBlue) {
            this.isBlue = isBlue;
            return this;
        }

        public RuleExpected setIsBlueMobile(Boolean isBlueMobile) {
            this.isBlueMobile = isBlueMobile;
            return this;
        }

        public RuleExpected setIsBlueInternal(Boolean isBlueInternal) {
            this.isBlueInternal = isBlueInternal;
            return this;
        }

        public void assertThat() {
            if (null != isBlue) {
                Assert.assertEquals(isBlue, blueRule.test());
            }
            if (null != isBlueMobile) {
                Assert.assertEquals(isBlueMobile, blueMobileApplicationRule.test());
            }
            if (null != isBlueInternal) {
                Assert.assertEquals(isBlueInternal, blueInternalRule.test());
            }
        }
    }

    @Inject
    private BlueRule blueRule;

    @Inject
    private BlueMobileApplicationRule blueMobileApplicationRule;

    @Inject
    private BlueInternalRule blueInternalRule;

    @Inject
    private ClientHelper clientHelper;

    private MockClientHelper mockClientHelper;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mockClientHelper = new MockClientHelper(clientHelper);
    }

    @Test
    public void mobileWithoutRules() {
        clientMobile();
        request();

        new RuleExpected()
            .setIsBlue(false)
            .setIsBlueMobile(false)
            .setIsBlueInternal(false)
            .assertThat();
    }

    @Test
    public void mobileWithRules() {
        clientMobile();
        request();

        new RuleExpected()
            .setIsBlue(false)
            .setIsBlueMobile(false)
            .setIsBlueInternal(false)
            .assertThat();
    }

    @Test
    public void blueMobile() {
        clientBlue();
        request();

        new RuleExpected()
            .setIsBlue(true)
            .setIsBlueMobile(true)
            .setIsBlueInternal(false)
            .assertThat();
    }

    @Test
    public void externalWithRules() {
        clientExternal();
        request(RulesHelper.Rules.BLUE);

        new RuleExpected()
            .setIsBlue(false)
            .setIsBlueMobile(false)
            .setIsBlueInternal(false)
            .assertThat();
    }

    @Test
    public void internalWithoutRules() {
        clientInternal();
        request();

        new RuleExpected()
            .setIsBlue(false)
            .setIsBlueMobile(false)
            .setIsBlueInternal(false)
            .assertThat();
    }

    @Test
    public void internalWithBlueRuleInUpperCase() {
        clientInternal();
        request("BLUE");

        new RuleExpected()
            .setIsBlue(true)
            .setIsBlueMobile(false)
            .setIsBlueInternal(true)
            .assertThat();
    }



    @Test
    public void internalWithBlueRuleInLowerCase() {
        clientInternal();
        request("blue");

        new RuleExpected()
            .setIsBlue(true)
            .setIsBlueMobile(false)
            .setIsBlueInternal(true)
            .assertThat();
    }

    @Test
    public void internalWithAnotherRule() {
        clientInternal();
        request("abra");

        new RuleExpected()
            .setIsBlue(false)
            .setIsBlueMobile(false)
            .setIsBlueInternal(false)
            .assertThat();
    }

    @Test
    public void internalWithEmptyRule() {
        clientInternal();
        request("");

        new RuleExpected()
            .setIsBlue(false)
            .setIsBlueMobile(false)
            .setIsBlueInternal(false)
            .assertThat();
    }


    @Test
    public void internalWithBlueRulesInList() {
        clientInternal();
        request("abra,blue,cab");

        new RuleExpected()
            .setIsBlue(true)
            .setIsBlueMobile(false)
            .setIsBlueInternal(true)
            .assertThat();
    }

    @Test
    public void internalWithBlueRuleInSubstring() {
        clientInternal();
        request("abluea");

        new RuleExpected()
            .setIsBlue(false)
            .setIsBlueMobile(false)
            .setIsBlueInternal(false)
            .assertThat();
    }

    @Test
    public void externalWithCustomTariffWithNoRule() {
        clientExternal();
        ContextHolder.update(ctx -> ctx.getClient().setTariff(TestTariffs.CUSTOM));

        request("", BLUE_METHOD_NAME);

        new RuleExpected()
            .setIsBlue(false)
            .setIsBlueMobile(false)
            .setIsBlueInternal(false)
            .assertThat();
    }

    @Test
    public void externalWithCustomTariffWithWrongMethod() {
        clientExternal();
        ContextHolder.update(ctx -> ctx.getClient().setTariff(TestTariffs.CUSTOM));

        request("abra,blue,cab", "/v2/redirect");

        new RuleExpected()
            .setIsBlue(false)
            .setIsBlueMobile(false)
            .setIsBlueInternal(false)
            .assertThat();
    }

    @Test
    public void internalWithBlueRuleInUpperCaseWithRgbWhiteField() {
        clientInternal();
        request("BLUE");
        requestWithRgbField("white");

        new RuleExpected()
                .setIsBlue(false)
                .setIsBlueMobile(false)
                .setIsBlueInternal(true)
                .assertThat();
    }

    @Test
    public void blueMobileWithRgbWhiteField() {
        clientBlue();
        request();
        requestWithRgbField("white");

        new RuleExpected()
                .setIsBlue(false)
                .setIsBlueMobile(true)
                .setIsBlueInternal(false)
                .assertThat();
    }

    @Test
    public void blueMobileWithRgbBlueField() {
        clientBlue();
        request();
        requestWithRgbField("blue");

        new RuleExpected()
                .setIsBlue(true)
                .setIsBlueMobile(true)
                .setIsBlueInternal(false)
                .assertThat();
    }

    @Test
    public void blueMobileWithRgbEmptyField() {
        clientBlue();
        request();
        requestWithRgbField("");

        new RuleExpected()
                .setIsBlue(true)
                .setIsBlueMobile(true)
                .setIsBlueInternal(false)
                .assertThat();
    }

    private static void clientInternal() {
        Client client = new Client();
        client.setType(Client.Type.INTERNAL);
        ContextHolder.update(ctx -> {
            ctx.setClient(client);
            ctx.setClientVersionInfo(InternalClientVersionInfo.INSTANCE);
        });
    }

    private static void clientExternal() {
        Client client = new Client();
        client.setType(Client.Type.EXTERNAL);
        ContextHolder.update(ctx -> ctx.setClient(client));
    }

    private static void clientMobile() {
        Client client = new Client();
        client.setType(Client.Type.MOBILE);
        ContextHolder.update(ctx -> ctx.setClient(client));
    }

    private void clientBlue() {
        Client client = new Client();
        client.setType(Client.Type.MOBILE);

        mockClientHelper.is(ClientHelper.Type.BLUE_APP, true);

        ContextHolder.update(ctx -> {
            ctx.setClient(client);
            ctx.setClientVersionInfo(
                new KnownMobileClientVersionInfo(
                    Platform.IOS,
                    DeviceType.TABLET,
                    new SemanticVersion(1, 0, 0)
                )
            );
        });
    }

    private static void request(String rules, String methodName) {
        ContextHolder.update(
            ctx ->
                ctx.setRequest(
                    MockRequestBuilder.start()
                        .param("rules", rules)
                            .methodName(methodName)
                        .build()
                )
        );
    }

    private static void request(String rules) {
        ContextHolder.update(
            ctx ->
                ctx.setRequest(
                    MockRequestBuilder.start()
                        .param("rules", rules)
                        .build()
                )
        );
    }

    private static void request() {
        ContextHolder.update(
            ctx ->
                ctx.setRequest(
                    MockRequestBuilder.start()
                        .build()
                )
        );
    }

    private static void requestWithRgbField(String rgbField){
        ContextHolder.update(ctx->ctx.setRgbs(Collections.singletonList(rgbField)));
    }
}

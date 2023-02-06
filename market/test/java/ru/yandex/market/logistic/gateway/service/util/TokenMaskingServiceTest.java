package ru.yandex.market.logistic.gateway.service.util;

import org.junit.Test;

import ru.yandex.market.logistic.gateway.BaseTest;

public class TokenMaskingServiceTest extends BaseTest {

    @Test
    public void stripTokenTagAsTextSuccess() {

        String nullXml = null;
        String nullXmlAfterProcessing = null;
        String nullResultOK = TokenMaskingService.stripTokenTagAsText(nullXml);
        assertions
            .assertThat(nullResultOK)
            .as("Null doesn't throw NPE")
            .isEqualTo(nullXmlAfterProcessing);

        String xmlWithToken = "<xml><token>super_secret_info</token></xml>";
        String xmlWithRemovedToken = "<xml><token>{token hidden}</token></xml>";
        String resultOK = TokenMaskingService.stripTokenTagAsText(xmlWithToken);
        assertions
            .assertThat(resultOK)
            .as("Token successfully removed")
            .isEqualTo(xmlWithRemovedToken);


        String xmlWith2Tokens = "<xml><token>super_secret_info</token><other>1</other><token>other_secret</token></xml>";
        String xmlWithRemovedTokens =
            "<xml><token>{token hidden}</token><other>1</other><token>{token hidden}</token></xml>";
        String resultOKBoth = TokenMaskingService.stripTokenTagAsText(xmlWith2Tokens);
        assertions
            .assertThat(resultOKBoth)
            .as("2 tokens successfully removed")
            .isEqualTo(xmlWithRemovedTokens);
    }
}

package ru.yandex.market.pers.notify.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.yandex.market.pers.notify.api.controller.VerificationController;
import ru.yandex.market.pers.notify.api.service.sk.SecretKeyManager;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.sk.SecretKeyData;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscription;
import ru.yandex.market.pers.notify.settings.SubscriptionAndIdentityService;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author semin-serg
 */
@Component
public class VerificationUtil {

    private final SecretKeyManager secretKeyManager;
    private final MockMvc mockMvc;
    private final SubscriptionAndIdentityService subscriptionAndIdentityService;
    private VerificationController verificationController;

    @Autowired
    public VerificationUtil(SecretKeyManager secretKeyManager, MockMvc mockMvc,
                            SubscriptionAndIdentityService subscriptionAndIdentityService,
                            VerificationController verificationController) {
        this.secretKeyManager = secretKeyManager;
        this.mockMvc = mockMvc;
        this.subscriptionAndIdentityService = subscriptionAndIdentityService;
        this.verificationController = verificationController;
    }

    public String confirmSubscription(long subscriptionId) {
        return confirmSubscription(subscriptionAndIdentityService.getEmailSubscription(subscriptionId));
    }

    public String confirmSubscription(EmailSubscription emailSubscription) {
        SecretKeyData secretKeyData = secretKeyManager.generateSecretKeyData(emailSubscription);
        String action = secretKeyManager.getAction(secretKeyData);
        String sk;
        try {
            sk = secretKeyManager.generateSk(action);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
        MockHttpServletRequestBuilder requestBuilder = post("/verification/")
            .contentType(MediaType.APPLICATION_JSON)
            .param("userAgent", "someAgent")
            .param("userIp", "someIp")
            .param("action", action)
            .param("sk", sk)
            .param("command", "subscribe");
        try {
            return mockMvc
                .perform(requestBuilder)
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        } catch (Exception e) {
            throw new RuntimeException("", e);
        }
    }

    public String generateActionAndSk(NotificationType type, Long subscriptionId, String email) {
        return verificationController.generate(type, null, email, subscriptionId);
    }

    public String unsubscribe(NotificationType type, Long subscriptionId, String email) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post("/verification/?" +
                generateActionAndSk(type, subscriptionId, email))
                .param("userIp", "127.0.0.1")
                .param("userAgent", "agent"))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
    }


}

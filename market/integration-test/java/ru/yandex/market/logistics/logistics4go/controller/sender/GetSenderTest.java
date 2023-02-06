package ru.yandex.market.logistics.logistics4go.controller.sender;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.logistics4go.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4go.client.api.SendersApi;
import ru.yandex.market.logistics.logistics4go.client.model.ErrorType;
import ru.yandex.market.logistics.logistics4go.client.model.GetSenderResponse;
import ru.yandex.market.logistics.logistics4go.client.model.NotFoundError;
import ru.yandex.market.logistics.logistics4go.client.model.PersonName;
import ru.yandex.market.logistics.logistics4go.client.model.Phone;
import ru.yandex.market.logistics.logistics4go.client.model.ResourceType;
import ru.yandex.market.logistics.logistics4go.client.model.SenderContactDto;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.yandex.market.logistics.logistics4go.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4go.client.ResponseSpecBuilders.validatedWith;

@DatabaseSetup("/controller/sender/get/before/sender.xml")
@DisplayName("Получение магазина")
@ParametersAreNonnullByDefault
class GetSenderTest extends AbstractIntegrationTest {
    private static final long SENDER_ID = 1;
    private static final long MISSING_SENDER_ID = 2;
    private static final long PARTNER_ID = 100;

    @Test
    @DisplayName("Получение магазина")
    void getSender() {
        GetSenderResponse expected = new GetSenderResponse()
            .externalId("sender-external-id")
            .name("sender-name")
            .siteUrl("sender-site-url")
            .contact(
                new SenderContactDto()
                    .name(
                        new PersonName()
                            .lastName("sender-last-name")
                            .firstName("sender-first-name")
                            .middleName("sender-middle-name")
                    )
                    .email("sender-email")
                    .phone(new Phone().number("sender-phone-number").extension("sender-additional-number"))
            )
            .partnerId(PARTNER_ID);

        GetSenderResponse response = getSender(SENDER_ID)
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("Получение несуществующего магазина")
    void senderNotFound() {
        NotFoundError error = getSender(MISSING_SENDER_ID)
            .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
            .as(NotFoundError.class);

        softly.assertThat(error).isEqualTo(
            new NotFoundError()
                .code(ErrorType.RESOURCE_NOT_FOUND)
                .addIdsItem(MISSING_SENDER_ID)
                .resourceType(ResourceType.SENDER)
                .message("Failed to find SENDER with ids [" + MISSING_SENDER_ID + "]")
        );
    }

    @Nonnull
    private SendersApi.GetSenderOper getSender(long senderId) {
        return apiClient.senders().getSender().senderIdPath(senderId);
    }
}

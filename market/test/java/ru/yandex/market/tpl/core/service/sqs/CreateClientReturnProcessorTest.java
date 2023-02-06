package ru.yandex.market.tpl.core.service.sqs;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.dto.TplDimensionsDto;
import ru.yandex.market.logistics.les.dto.TplRequestIntervalDto;
import ru.yandex.market.logistics.les.dto.TplReturnAtClientAddressItemDto;
import ru.yandex.market.logistics.les.dto.TplReturnAtClientAddressReasonType;
import ru.yandex.market.logistics.les.tpl.TplReturnAtClientAddressCreateResponseEvent;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.external.sqs.SqsQueueProperties;
import ru.yandex.market.tpl.core.service.sqs.processor.CreateClientReturnProcessor;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@RequiredArgsConstructor
public class CreateClientReturnProcessorTest extends TplAbstractTest {
    private final CreateClientReturnProcessor processor;
    private final ClientReturnRepository clientReturnRepository;
    private final JmsTemplate jmsTemplate;
    private final TransactionTemplate transactionTemplate;
    private final SqsQueueProperties sqsQueueProperties;
    private final Clock clock;


    @AfterEach
    void afterEach() {
        Mockito.clearInvocations(jmsTemplate);
    }

    @Test
    void testSuccessfulCreate() {
        var event = TplReturnAtClientAddressCreateRequestEventGenerateService.generateEvent(
                TplReturnAtClientAddressCreateRequestEventGenerateService.TplReturnAtClientAddressCreateRequestEventGenerateParam
                        .builder()
                        .interval(
                                new TplRequestIntervalDto(
                                        LocalDate.now(),
                                        LocalTime.now(),
                                        LocalDate.now().plusDays(1),
                                        LocalTime.now()
                                )
                        )
                        .build()
        );

        processor.process(event);

        var clientReturn = clientReturnRepository.findByExternalReturnId(event.getReturnId()).get();

        var lesEvent = new Event(
                sqsQueueProperties.getSource(),
                clientReturn.getId().toString(),
                Instant.now(clock).toEpochMilli(),
                TplReturnAtClientAddressCreateResponseEvent.EVENT_NAME,
                new TplReturnAtClientAddressCreateResponseEvent(
                        event.getId(),
                        event.getReturnId(),
                        List.of()
                ),
                "Клиентский возврат создан в курьерке"
        );

        var clientReturns = clientReturnRepository.findAll();
        assertThat(clientReturns).isNotEmpty();
        assertThat(clientReturns.get(0).getExternalReturnId()).isEqualTo(event.getReturnId());
        assertThat(clientReturns.get(0).getCheckouterReturnId()).isEqualTo(event.getReturnExternalId());

        Mockito.verify(jmsTemplate, Mockito.times(1)).convertAndSend(
                anyString(),
                eq(lesEvent)
        );
    }


    @Test
    void testCreationFailedWithValidation() {
        var event = TplReturnAtClientAddressCreateRequestEventGenerateService.generateEvent(
                TplReturnAtClientAddressCreateRequestEventGenerateService.TplReturnAtClientAddressCreateRequestEventGenerateParam
                        .builder()
                        .interval(null)
                        .client(null)
                        .build()
        );

        processor.process(event);

        var clientReturns = clientReturnRepository.findAll();
        assertThat(clientReturns).isEmpty();
        Mockito.verify(jmsTemplate, Mockito.times(1)).convertAndSend(anyString(), any(Event.class));
    }

    @Test
    void testSuccessfulCreateWithUrls() {
        var event = TplReturnAtClientAddressCreateRequestEventGenerateService.generateEvent(
                TplReturnAtClientAddressCreateRequestEventGenerateService.TplReturnAtClientAddressCreateRequestEventGenerateParam
                        .builder()
                        .interval(
                                new TplRequestIntervalDto(
                                        LocalDate.now(),
                                        LocalTime.now(),
                                        LocalDate.now().plusDays(1),
                                        LocalTime.now()
                                )
                        )
                        .items(List.of(
                                new TplReturnAtClientAddressItemDto(
                                        345L,
                                        "itemSku",
                                        "itemName",
                                        "CategoryName",
                                        "description",
                                        "photoUrl/50x50",
                                        "detailsUrl",
                                        new TplDimensionsDto(
                                                1L, 1L, 1L, 1L
                                        ),
                                        TplReturnAtClientAddressReasonType.BAD_QUALITY,
                                        "Bad quality",
                                        null,
                                        List.of("https://avatars.mds.yandex" +
                                                        ".net/get-market-ugc/6349684" +
                                                        "/2a0000017fd5c607ca78a36b3ca674d24fef/",
                                                "//avatars.mds.yandex" +
                                                        ".net/get-market-ugc/6349684" +
                                                        "/2a0000017fd5c607ca78a36b3ca674d24fef/orig",
                                                "https://avatars.mds.yandex" +
                                                        ".net/get-market-ugc/6349684" +
                                                        "/2a0000017fd5c607ca78a36b3ca674d24fef"
                                        )
                                )))
                        .build()
        );

        processor.process(event);

        transactionTemplate.executeWithoutResult(ts -> {
            var clientReturns = clientReturnRepository.findAll();
            assertThat(clientReturns).isNotEmpty();
            assertThat(clientReturns.get(0).getExternalReturnId()).isEqualTo(event.getReturnId());
            clientReturns.get(0).getItems().get(0).getClientPhotoUrls().forEach(url -> assertThat(url.endsWith("/")));
            clientReturns.get(0).getItems().get(0).getClientPhotoUrls().forEach(url -> assertThat(url.startsWith("//")));
            assertThat(clientReturns.get(0).getItems().get(0).getPreviewPhotoUrl()).endsWith("/");
        });
        Mockito.verify(jmsTemplate, Mockito.times(1)).convertAndSend(anyString(), any(Event.class));
    }
}

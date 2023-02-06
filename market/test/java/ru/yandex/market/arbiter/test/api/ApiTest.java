package ru.yandex.market.arbiter.test.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.businesschat.provider.api.client.BusinesschatProviderApi;
import ru.yandex.businesschat.provider.api.client.dto.BusinessChatMessageDto;
import ru.yandex.businesschat.provider.api.client.dto.MessageType;
import ru.yandex.businesschat.provider.api.client.dto.UserMessageDto;
import ru.yandex.businesschat.provider.api.client.dto.UserSenderDto;
import ru.yandex.market.arbiter.api.client.ArbiterApi;
import ru.yandex.market.arbiter.api.client.ServiceApi;
import ru.yandex.market.arbiter.api.client.dto.ArbiterConversationDto;
import ru.yandex.market.arbiter.api.client.dto.ArbiterConversationSummaryArrayDto;
import ru.yandex.market.arbiter.api.client.dto.ArbiterConversationSummaryDto;
import ru.yandex.market.arbiter.api.client.dto.BusinesschatParamsDto;
import ru.yandex.market.arbiter.api.client.dto.ConversationSide;
import ru.yandex.market.arbiter.api.client.dto.ConversationStatus;
import ru.yandex.market.arbiter.api.client.dto.CreateConversationRequestDto;
import ru.yandex.market.arbiter.api.client.dto.ErrorDto;
import ru.yandex.market.arbiter.api.client.dto.IdResponseDto;
import ru.yandex.market.arbiter.api.client.dto.MessageDto;
import ru.yandex.market.arbiter.api.client.dto.NotificationChannelDto;
import ru.yandex.market.arbiter.api.client.dto.NotificationChannelType;
import ru.yandex.market.arbiter.api.client.dto.VerdictDto;
import ru.yandex.market.arbiter.api.client.dto.VerdictType;
import ru.yandex.market.arbiter.test.BaseIntegrationTest;
import ru.yandex.market.arbiter.test.TestDataService;
import ru.yandex.market.arbiter.test.TestMapper;
import ru.yandex.market.arbiter.test.util.RandomDataGenerator;
import ru.yandex.market.arbiter.test.util.TestClock;

/**
 * @author moskovkin@yandex-team.ru
 * @since 21.05.2020
 */
@SuppressWarnings("ConstantConditions")
public class ApiTest extends BaseIntegrationTest {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(ApiTest.class).build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);


    @Autowired
    private ServiceApi serviceApi;

    @Autowired
    private ArbiterApi arbiterApi;

    @Autowired
    private BusinesschatProviderApi businesschatProviderApi;

    @Autowired
    private TestMapper testMapper;

    @Autowired
    private TestClock clock;

    @Test
    public void testAddBusinesschatMessage() {
        CreateConversationRequestDto createConversationRequest = RANDOM.nextObject(
                CreateConversationRequestDto.class
        ).notificationChannels(List.of(
                new NotificationChannelDto()
                        .conversationSide(ConversationSide.USER)
                        .type(NotificationChannelType.BUSINESSCHAT)
                        .businesschatParams(new BusinesschatParamsDto()
                                .chatId("USER_CHAT_ID")
                                .recipientId("USER_CHAT_RECIPIENT")
                        ),
                new NotificationChannelDto()
                        .conversationSide(ConversationSide.MERCHANT)
                        .type(NotificationChannelType.BUSINESSCHAT)
                        .businesschatParams(new BusinesschatParamsDto()
                                .chatId("MERCHANT_CHAT_ID")
                                .recipientId("MERCHANT_CHAT_RECIPIENT")
                        )
                )
        );

        Long conversationId = serviceApi.serviceConversationAddPost(createConversationRequest).getId();
        ArbiterConversationDto conversationBefore = arbiterApi.arbiterConversationGetGet(conversationId);

        businesschatProviderApi.businesschatProviderChatIdPost("USER_CHAT_ID", new UserMessageDto()
            .sender(new UserSenderDto()
                .id("USER_CHAT_RECIPIENT")
            )
            .message(new BusinessChatMessageDto()
                .type(MessageType.TEXT)
                .text("test user message")
            )
        );
        ArbiterConversationDto conversationAfterUser = arbiterApi.arbiterConversationGetGet(conversationId);

        businesschatProviderApi.businesschatProviderChatIdPost("MERCHANT_CHAT_ID", new UserMessageDto()
                .sender(new UserSenderDto()
                        .id("MERCHANT_CHAT_RECIPIENT")
                )
                .message(new BusinessChatMessageDto()
                        .type(MessageType.TEXT)
                        .text("test merchant message")
                )
        );
        ArbiterConversationDto conversationAfterMerchant = arbiterApi.arbiterConversationGetGet(conversationId);

        MessageDto expectedUserMessage = new MessageDto()
                .sender(ConversationSide.USER)
                .recipient(ConversationSide.ARBITER)
                .text("test user message");

        MessageDto expectedMerchantMessage = new MessageDto()
                .sender(ConversationSide.MERCHANT)
                .recipient(ConversationSide.ARBITER)
                .text("test merchant message");

        Assertions.assertThat(conversationBefore.getMessages()).usingRecursiveFieldByFieldElementComparator()
                .usingElementComparatorOnFields("sender", "recipient", "text")
                .doesNotContain(expectedUserMessage, expectedMerchantMessage);

        Assertions.assertThat(conversationAfterUser.getMessages()).usingRecursiveFieldByFieldElementComparator()
                .usingElementComparatorOnFields("sender", "recipient", "text")
                .contains(expectedUserMessage);

        Assertions.assertThat(conversationAfterMerchant.getMessages()).usingRecursiveFieldByFieldElementComparator()
                .usingElementComparatorOnFields("sender", "recipient", "text")
                .contains(expectedUserMessage, expectedMerchantMessage);
    }

    @Test
    public void testArbiterConversationSearch() {
        TestDataService.TestData testData = testDataService.saveTestData();
        Long arbiterUid = RANDOM.nextLong();

        arbiterApi.arbiterActionInprogressPost(arbiterUid, testData.someConversationId());

        OffsetDateTime beforeVerdict = OffsetDateTime.ofInstant(clock.instant(), clock.getZone());
        clock.setTime(clock.instant().plus(7, ChronoUnit.DAYS));
        arbiterApi.arbiterVerdictDeclinePost(arbiterUid, testData.someConversationId());
        OffsetDateTime afterVerdict = OffsetDateTime.ofInstant(clock.instant(), clock.getZone());

        ArbiterConversationSummaryArrayDto foundOld = arbiterApi.arbiterConversationSearchGet(
                null,
                List.of(ConversationStatus.VERDICT),
                null, null, null, null,
                beforeVerdict.plus(4, ChronoUnit.DAYS),
                null, null
        );

        ArbiterConversationSummaryArrayDto foundNew = arbiterApi.arbiterConversationSearchGet(
                null,
                List.of(ConversationStatus.VERDICT),
                null, null, null, null,
                afterVerdict,
                null, null
        );

        Assertions.assertThat(foundOld.getItems()).isEmpty();
        Assertions.assertThat(foundNew.getItems())
                .usingRecursiveFieldByFieldElementComparator()
                .usingElementComparatorOnFields("id", "status", "verdict")
                .containsOnly(new ArbiterConversationSummaryDto()
                        .id(testData.someConversationId())
                        .status(ConversationStatus.VERDICT)
                        .verdict(new VerdictDto()
                            .type(VerdictType.DECLINE)
                        )
                );
    }

    @Test
    public void testCreateConversation() {
        testDataService.saveTestData();

        CreateConversationRequestDto createConversationRequestDto =
                RANDOM.nextObject(CreateConversationRequestDto.class);

        IdResponseDto idResponse = serviceApi.serviceConversationAddPost(createConversationRequestDto);
        ArbiterConversationDto arbiterConversationDto = arbiterApi.arbiterConversationGetGet(idResponse.getId());

        Assertions.assertThat(createConversationRequestDto)
                .usingComparatorForType(Comparator.comparing(OffsetDateTime::toInstant), OffsetDateTime.class)
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .ignoringCollectionOrder()
                .ignoringFields("subject.id", "merchant.id", "messages.id", "messages.creationTime")
                .isEqualTo(arbiterConversationDto);
    }

    @Test
    public void testAddMessage() {
        TestDataService.TestData testData = testDataService.saveTestData();
        MessageDto newMessage = RANDOM.nextObject(MessageDto.class)
                .sender(ConversationSide.USER);

        ArbiterConversationDto conversationBefore =
                arbiterApi.arbiterConversationGetGet(testData.someConversation().getId());

        IdResponseDto idResponse =
                serviceApi.serviceMessageAddPost(testData.someConversation().getId(), newMessage);

        ArbiterConversationDto conversationAfter =
                arbiterApi.arbiterConversationGetGet(testData.someConversation().getId());

        Optional<MessageDto> messageAfter = conversationAfter.getMessages().stream()
                .filter(m -> m.getId().equals(idResponse.getId()))
                .findAny();

        Optional<MessageDto> messageBefore = conversationBefore.getMessages().stream()
                .filter(m -> m.getId().equals(idResponse.getId()))
                .findAny();

        Assertions.assertThat(messageBefore)
                .isEmpty();

        Assertions.assertThat(messageAfter)
                .isNotEmpty();

        Assertions.assertThat(messageAfter.get())
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .ignoringCollectionOrder()
                .ignoringFields("id", "creationTime", "recipient")
                .isEqualTo(newMessage);
    }

    @Test
    public void testErrorOnComplexValidation() throws IOException, InterruptedException {
        TestDataService.TestData testData = testDataService.saveTestData();

        testErrorResponse(
                "POST",
                "/service/message/add?" +
                        "conversationId=" + testData.someConversationId(),
                "{\"sender\": \"ARBITER\", \"recipient\": \"USER\", \"text\": \"some text\"}",
                400, "Bad Request", "message.sender"
        );
    }

    @Test
    public void testErrorOnForbiddenAction() throws IOException, InterruptedException {
        TestDataService.TestData testData = testDataService.saveTestData();

        testErrorResponse(
                "POST",
                "/arbiter/verdict/confirm?" +
                        "uid=" + RANDOM.nextLong() +
                        "&conversationId=" + testData.someConversationId(),
                null,
                400, "Bad Request", "forbidden for this conversation"
        );
    }

    @Test
    public void testErrorNoRequiredField() throws IOException, InterruptedException {
        testErrorResponse(
                "POST", "/service/conversation/add", "{\"serviceType\": \"SUPERAPP\"}",
                400, "Bad Request", "uid"
        );
    }

    @Test
    public void testErrorNullInRequiredField() throws IOException, InterruptedException {
        testErrorResponse(
                "POST", "/service/conversation/add", "{\"uid\": null, \"serviceType\": \"SUPERAPP\"}",
                400, "Bad Request", "uid"
        );
    }

    @Test
    public void testErrorUnableToParseJson() throws IOException, InterruptedException {
        testErrorResponse(
                "POST", "/service/conversation/add", "{\"serviceType\": \"BEBEBE\"}",
                400, "Bad Request", "BEBEBE"
        );
    }

    @Test
    public void testDuplicateNotificationChannelsDisallowed() {
        CreateConversationRequestDto createConversationRequest = RANDOM.nextObject(
                CreateConversationRequestDto.class
        ).notificationChannels(List.of(
                new NotificationChannelDto()
                        .conversationSide(ConversationSide.USER)
                        .type(NotificationChannelType.BUSINESSCHAT)
                        .businesschatParams(new BusinesschatParamsDto()
                                .chatId("USER_CHAT_ID")
                                .recipientId("USER_CHAT_RECIPIENT")
                        ),
                new NotificationChannelDto()
                        .conversationSide(ConversationSide.MERCHANT)
                        .type(NotificationChannelType.BUSINESSCHAT)
                        .businesschatParams(new BusinesschatParamsDto()
                                .chatId("MERCHANT_CHAT_ID")
                                .recipientId("MERCHANT_CHAT_RECIPIENT")
                        )
                )
        );

        CreateConversationRequestDto duplicatedChannelsConversationRequest = RANDOM.nextObject(
                CreateConversationRequestDto.class
        ).notificationChannels(
                createConversationRequest.getNotificationChannels()
        );

        serviceApi.serviceConversationAddPost(createConversationRequest);
        Assertions.assertThatThrownBy(
                () -> serviceApi.serviceConversationAddPost(duplicatedChannelsConversationRequest)
        )
        .isInstanceOf(HttpClientErrorException.BadRequest.class)
        .hasMessageContaining("400 Bad Request");
    }

    @Test
    public void testTypeinMessage() throws IOException, InterruptedException {
        testResponseStatus(
                "POST",
                "/businesschat/provider/1234_CHAT_ID/", "{\n" +
                        "  \"sender\": {\n" +
                        "    \"id\": 12345\n" +
                        "  },\n" +
                        "  \"message\": {\n" +
                        "    \"id\": 42,\n" +
                        "    \"type\": \"typein\",\n" +
                        "    \"date\": 1593604463,\n" +
                        "    \"typing\": true\n" +
                        "  }\n" +
                        "}",
                200
        );
    }

    public void testResponseStatus(
            String requestMethod,
            String requestString,
            String requestBody,

            Integer status
    ) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .build();

        HttpRequest.BodyPublisher bodyPublisher = requestBody != null
                ? HttpRequest.BodyPublishers.ofString(requestBody)
                : HttpRequest.BodyPublishers.noBody();

        URI uri = URI.create(arbiterApiClient.getBasePath() + requestString);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("content-type", "application/json")
                .method(requestMethod, bodyPublisher)
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Assertions.assertThat(response.statusCode())
                .isEqualTo(status);
    }

    public void testErrorResponse(
            String requestMethod,
            String requestString,
            String requestBody,

            Integer status,
            String error,
            String messageSubstring
    ) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .build();

        HttpRequest.BodyPublisher bodyPublisher = requestBody != null
                ? HttpRequest.BodyPublishers.ofString(requestBody)
                : HttpRequest.BodyPublishers.noBody();

        URI uri = URI.create(arbiterApiClient.getBasePath() + requestString);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("content-type", "application/json")
                .method(requestMethod, bodyPublisher)
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Assertions.assertThat(response.statusCode())
                .isEqualTo(status);

        ErrorDto errorDto = OBJECT_MAPPER.readValue(response.body(), ErrorDto.class);
        Assertions.assertThat(errorDto).isEqualToComparingOnlyGivenFields(new ErrorDto()
                .status(status)
                .path(uri.getPath())
                .error(error),
                "status", "path", "error"
        );

        Assertions.assertThat(response.body()).contains(messageSubstring);
    }

    @SuppressWarnings({"Convert2MethodRef", "OptionalGetWithoutIsPresent", "RedundantCollectionOperation"})
    @Test
    public void testFindMessages() {
//        TestDataService.TestData testData = testDataService.saveTestData();
//        Long arbiterUid = RANDOM.nextLong();
//
//        // Find message ID we will look messages from
//        Long fromId = testData.someConversation().getMessages().stream()
//                .mapToLong(m -> m.getId())
//                .max().getAsLong() + 1;
//
//        // Save more messages
//        RANDOM.objects(MessageDto.class, 20)
//                .peek(m -> m.setRecipient(
//                        RandomUtil.randomItem(RANDOM, List.of(ConversationSide.USER, ConversationSide.MERCHANT)))
//                )
//                .forEach(m -> arbiterApi.arbiterActionMessagePost(arbiterUid, testData.someConversation().getId(), m));
//
//        // Get conversation with full message list
//        ArbiterConversationDto conversation = arbiterApi.arbiterConversationGetGet(testData.someConversation().getId());
//        List<MessageDto> allMessages = conversation.getMessages();
//
//        List<MessageWithConversationDto> expectedMessages = allMessages.stream()
//                .map(m -> testMapper.mapToMessageWithConversationDto(m, conversation))
//                .filter(m -> m.getId() >= fromId)
//                .filter(m -> Set.of(ConversationSide.ARBITER).contains(m.getSender()))
//                .filter(m -> Set.of(ConversationSide.MERCHANT, ConversationSide.USER).contains(m.getRecipient()))
//                .sorted(Comparator.comparingLong(MessageWithConversationDto::getId))
//                .collect(Collectors.toUnmodifiableList());
//
//        List<MessageWithConversationDto> foundMessages = serviceApi.serviceMessageSearchGet(
//                testData.someConversation().getId(),
//                fromId,
//                List.of(ConversationSide.ARBITER),
//                List.of(ConversationSide.MERCHANT, ConversationSide.USER)
//        ).getItems();
//
//        // Minimum 2 messages should be found to check sorting
//        Assertions.assertThat(expectedMessages)
//                .hasSizeGreaterThanOrEqualTo(2);
//
//        // Some messages should NOT be found
//        Assertions.assertThat(expectedMessages)
//                .hasSizeLessThan(allMessages.size());
//
//        Assertions.assertThat(foundMessages).usingRecursiveFieldByFieldElementComparator()
//                .containsExactlyElementsOf(expectedMessages);
    }
}

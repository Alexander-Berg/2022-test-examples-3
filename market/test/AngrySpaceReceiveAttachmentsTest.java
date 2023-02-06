package ru.yandex.market.jmf.module.angry.test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.logic.def.Attachment;
import ru.yandex.market.jmf.module.angry.AngryAttachmentProcessingStrategy;
import ru.yandex.market.jmf.module.angry.AngryAttachmentUploadStrategy;
import ru.yandex.market.jmf.module.angry.AngrySpaceAttachmentsUploadService;
import ru.yandex.market.jmf.module.angry.AngrySpaceService;
import ru.yandex.market.jmf.module.angry.Ticket;
import ru.yandex.market.jmf.module.angry.controller.v1.model.Item;
import ru.yandex.market.jmf.module.angry.controller.v1.model.Message;
import ru.yandex.market.jmf.module.angry.controller.v1.model.SmmObjectBase;
import ru.yandex.market.jmf.module.angry.impl.AngrySpaceAttachmentServiceImpl;
import ru.yandex.market.jmf.module.angry.impl.SocialMessagingTestUtils;
import ru.yandex.market.jmf.module.angry.impl.strategy.DefaultAttachmentProcessingStrategy;
import ru.yandex.market.jmf.module.comment.CommentsService;
import ru.yandex.market.jmf.module.comment.InternalComment;
import ru.yandex.market.jmf.utils.LinkUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ContextConfiguration(classes = ModuleAngrySpaceTestConfiguration.class)
@Transactional
public class AngrySpaceReceiveAttachmentsTest {

    private static final String TICKET_GID = "ticket@777";
    private static final String DEFAULT_MIME_TYPE = "defaultMimeType";
    private static final String DEFAULT_UPLOAD_URL = "defaultUploadUrl";

    private AngrySpaceAttachmentServiceImpl attachmentService;
    @Inject
    private List<AngryAttachmentUploadStrategy> uploadStrategies;
    @Inject
    private List<AngryAttachmentProcessingStrategy> processingStrategies;
    @Inject
    private DefaultAttachmentProcessingStrategy defaultAttachmentProcessingStrategy;
    @Inject
    private LinkUtils linkUtils;
    @Inject
    private BcpService bcpService;
    @Inject
    private SocialMessagingTestUtils socialMessagingTestUtils;
    @Inject
    private AngrySpaceService angrySpaceService;
    @Inject
    private AngrySpaceAttachmentsUploadService attachmentsUploadService;
    @Mock
    private CommentsService commentsService;
    @Mock
    private Ticket ticket;


    @BeforeEach
    void setUp() {
        attachmentService = new AngrySpaceAttachmentServiceImpl(uploadStrategies, processingStrategies,
                defaultAttachmentProcessingStrategy, commentsService, linkUtils, bcpService);

        when(ticket.getGid()).thenReturn(TICKET_GID);
    }

    @AfterEach
    void tearDown() {
        reset(commentsService);
        reset(ticket);
        reset(attachmentsUploadService);
    }

    private void testCreateSingleAttachmentComment(SmmObjectBase smmObjectBase, String expectedBody) {
        var attachments = attachmentService.createAttachments(smmObjectBase);
        assertEquals(0, attachments.size());

        var offsetDateTime = OffsetDateTime.now();
        attachmentService.createAttachmentCommentIfNeeded(smmObjectBase, ticket, offsetDateTime);
        assertCreateOneAttachmentComment(offsetDateTime, TICKET_GID, expectedBody);
    }

    private void testCreateSingleAttachmentAsFile(SmmObjectBase smmObjectBase, String expectedMimeType,
                                                  String expectedUploadUrl, String expectedName) throws Exception {
        configureAttachmentUploadMock();
        var attachments = attachmentService.createAttachments(smmObjectBase);

        assertEquals(1, attachments.size());
        assertAttachment(attachments.get(0), expectedMimeType, expectedUploadUrl, expectedName);
        assertDontCreateAttachmentComment();
    }

    @Test
    public void testFbItemGif() throws Exception {
        String expectedBody = "К сообщению было прикреплено вложение (анимация).<br/>Просмотреть его можно в " +
                              "социальной сети <a rel=\"nofollow noopener noreferrer\" target=\"_blank\" " +
                              "href=\"https://www.facebook.com/102191528673262/posts/102272755331806/?comment_id" +
                              "=118457410380007&amp;reply_comment_id=122945716597843\">по ссылке</a>.";
        testCreateSingleAttachmentComment(getSmmItem("fbItemGif.json"), expectedBody);
    }

    @Test
    public void testFbItemPhoto() throws Exception {
        SmmObjectBase smmObjectBase = getSmmItem("fbItemPhoto.json");
        testCreateSingleAttachmentAsFile(smmObjectBase, DEFAULT_MIME_TYPE, DEFAULT_UPLOAD_URL, "изображение");
    }

    @Test
    public void testFbItemSticker() throws Exception {
        SmmObjectBase smmObjectBase = getSmmItem("fbItemSticker.json");
        String expectedUrl = "https://scontent-hel3-1.xx.fbcdn.net/v/t39" +
                             ".1997-6/106421800_953852561743768_2258502540470753610_n" +
                             ".png?_nc_cat=1&ccb=1-3&_nc_sid=ac3552&_nc_ohc=xh2gjCKzUfgAX-IFj2v&_nc_ht=scontent-hel3" +
                             "-1.xx&oh=612320559403109304857455d8834ced&oe=60D03E17";
        testCreateSingleAttachmentAsFile(smmObjectBase, "image/png", expectedUrl, "стикер");
    }

    @Test
    public void testFbItemVideo() throws Exception {
        String expectedBody = "К сообщению было прикреплено вложение (видеозапись).<br/>Просмотреть его можно в " +
                              "социальной сети <a rel=\"nofollow noopener noreferrer\" target=\"_blank\" " +
                              "href=\"https://www.facebook.com/102191528673262/posts/102272755331806/?comment_id" +
                              "=118457410380007&amp;reply_comment_id=122945876597827\">по ссылке</a>.";
        testCreateSingleAttachmentComment(getSmmItem("fbItemVideo.json"), expectedBody);
    }

    @Test
    public void testVkMessageSticker() throws Exception {
        SmmObjectBase smmObjectBase = getSmmMessage("vkMessageSticker.json");
        testCreateSingleAttachmentAsFile(smmObjectBase, "image/png", "https://vk.com/sticker/1-3463-512", "стикер");
    }

    @Test
    public void testVkMessageAudio() throws Exception {
        SmmObjectBase smmObjectBase = getSmmMessage("vkMessageAudio.json");
        String expectedBody = "К сообщению было прикреплено вложение (STILOVDAILY (аудиозапись)).<br/>Просмотреть его" +
                              " можно в социальной сети <a rel=\"nofollow noopener noreferrer\" target=\"_blank\" " +
                              "href=\"https://vk.com/wall-203323213_29?reply=91&amp;thread=88\">по ссылке</a>.";
        testCreateSingleAttachmentComment(smmObjectBase, expectedBody);
    }

    @Test
    public void testVkItemVideo() throws Exception {
        SmmObjectBase smmObjectBase = getSmmItem("vkItemVideo.json");
        String expectedBody = "К сообщению было прикреплено вложение (Без названия (видеозапись)).<br/>Просмотреть " +
                              "его можно в социальной сети <a rel=\"nofollow noopener noreferrer\" target=\"_blank\" " +
                              "href=\"https://vk.com/wall-203323213_29?reply=90&amp;thread=88\">по ссылке</a>.";
        testCreateSingleAttachmentComment(smmObjectBase, expectedBody);
    }

    @Test
    public void testVkItemPhoto() throws Exception {
        SmmObjectBase smmObjectBase = getSmmItem("vkItemPhoto.json");
        testCreateSingleAttachmentAsFile(smmObjectBase, DEFAULT_MIME_TYPE, DEFAULT_UPLOAD_URL, "изображение");
    }

    @Test
    public void testVkItemDocument() throws Exception {
        SmmObjectBase smmObjectBase = getSmmItem("vkItemDocument.json");
        testCreateSingleAttachmentAsFile(smmObjectBase, DEFAULT_MIME_TYPE, DEFAULT_UPLOAD_URL, "duke.png");
    }

    @Test
    public void testOkMessageFile() throws Exception {
        SmmObjectBase smmObjectBase = getSmmMessage("okMessageFile.json");
        testCreateSingleAttachmentAsFile(smmObjectBase, DEFAULT_MIME_TYPE, DEFAULT_UPLOAD_URL, "файл");
    }

    @Test
    public void testOkItemSticker() throws Exception {
        SmmObjectBase smmObjectBase = getSmmItem("okItemSticker.json");
        String expectedBody = "К сообщению было прикреплено вложение (стикер).<br/>Просмотреть его можно в социальной" +
                              " сети <a rel=\"nofollow noopener noreferrer\" target=\"_blank\" href=\"https://ok" +
                              ".ru/discussions/1/67179833393194/153271274582058\">по ссылке</a>.";
        testCreateSingleAttachmentComment(smmObjectBase, expectedBody);
    }

    @Test
    public void twoUploadedAttachmentsWithSameTypesShouldCreateTwoAttachments() throws Exception {
        SmmObjectBase smmObjectBase = getSmmItem("okItemTwoPhotos.json");

        configureAttachmentUploadMock();
        var attachments = attachmentService.createAttachments(smmObjectBase);

        assertEquals(2, attachments.size());
        assertAttachment(attachments.get(0), "image/jpeg", DEFAULT_UPLOAD_URL, "изображение");
        assertAttachment(attachments.get(1), "image/jpeg", DEFAULT_UPLOAD_URL, "изображение");
        assertDontCreateAttachmentComment();
    }

    @Test
    public void twoUploadedAttachmentsWithDifferentTypesShouldCreateTwoAttachments() throws Exception {
        SmmObjectBase smmObjectBase = getSmmItem("vkItemPhotoAndDocument.json");

        configureAttachmentUploadMock();
        var attachments = attachmentService.createAttachments(smmObjectBase);

        assertEquals(2, attachments.size());
        assertAttachment(attachments.get(0), DEFAULT_MIME_TYPE, DEFAULT_UPLOAD_URL, "изображение");
        assertAttachment(attachments.get(1), DEFAULT_MIME_TYPE, DEFAULT_UPLOAD_URL, "duke.png");
        assertDontCreateAttachmentComment();
    }

    @Test
    public void twoProcessedAttachmentsWithDifferentTypesShouldCreateOneComment() throws Exception {
        SmmObjectBase smmObjectBase = getSmmItem("vkMessageVideoAndAudio.json");

        var attachments = attachmentService.createAttachments(smmObjectBase);
        assertEquals(0, attachments.size());

        var offsetDateTime = OffsetDateTime.now();
        attachmentService.createAttachmentCommentIfNeeded(smmObjectBase, ticket, offsetDateTime);
        String expectedBody = "К сообщению было прикреплено вложение (STILOVDAILY (аудиозапись), Без названия " +
                              "(видеозапись)).<br/>Просмотреть его можно в социальной сети <a rel=\"nofollow noopener" +
                              " noreferrer\" target=\"_blank\" href=\"https://vk.com/wall-203323213_29?reply=91&amp;" +
                              "thread=88\">по ссылке</a>.";
        assertCreateOneAttachmentComment(offsetDateTime, TICKET_GID, expectedBody);
    }

    @Test
    public void oneProcessedAndOneUploadedAttachmentsShouldCreateOneCommentAndOneAttachment() throws Exception {
        SmmObjectBase smmObjectBase = getSmmItem("vkMessageStickerAndAudio.json");

        var attachments = attachmentService.createAttachments(smmObjectBase);
        assertEquals(1, attachments.size());
        assertAttachment(attachments.get(0), "image/png", "https://vk.com/sticker/1-3463-512", "стикер");

        var offsetDateTime = OffsetDateTime.now();
        attachmentService.createAttachmentCommentIfNeeded(smmObjectBase, ticket, offsetDateTime);
        String expectedBody = "К сообщению было прикреплено вложение (STILOVDAILY (аудиозапись)).<br/>Просмотреть его" +
                              " можно в социальной сети <a rel=\"nofollow noopener noreferrer\" target=\"_blank\" " +
                              "href=\"https://vk.com/wall-203323213_29?reply=95&amp;thread=88\">по ссылке</a>.";
        assertCreateOneAttachmentComment(offsetDateTime, TICKET_GID, expectedBody);
    }


    private void assertDontCreateAttachmentComment() {
        verify(commentsService, never()).create(any(), any(), any(), any());
    }

    private void assertAttachment(Attachment attachment, String expectedMimeType,
                                  String expectedUploadUrl, String expectedName) {
        assertEquals(expectedMimeType, attachment.getContentType());
        assertEquals(expectedUploadUrl, attachment.getUrl());
        assertEquals(expectedName, attachment.getName());
    }

    private void assertCreateOneAttachmentComment(OffsetDateTime expectedCreationTime, String expectedGid,
                                                  CharSequence expectedBody) {
        var gidCaptor = ArgumentCaptor.forClass(Ticket.class);
        var bodyCaptor = ArgumentCaptor.forClass(CharSequence.class);
        ArgumentCaptor<Map<String, Object>> propsCaptor = ArgumentCaptor.forClass(Map.class);

        verify(commentsService, times(1))
                .create(gidCaptor.capture(), bodyCaptor.capture(), ArgumentMatchers.eq(InternalComment.FQN),
                        propsCaptor.capture());

        assertEquals(expectedGid, gidCaptor.getValue().getGid());

        var actualCreationTime = (OffsetDateTime) propsCaptor.getValue().get(InternalComment.CREATION_TIME);
        assertEquals(expectedCreationTime, actualCreationTime);

        assertEquals(expectedBody, bodyCaptor.getValue());
    }

    private JsonNode getRawSmmObject(String fileName) throws IOException {
        return socialMessagingTestUtils
                .getFirstSmmObjectFromFile("/ru/yandex/market/jmf/module/angry/attachments/%s".formatted(fileName));
    }

    private Item getSmmItem(String fileName) throws IOException {
        var jsonNode = getRawSmmObject(fileName);
        return angrySpaceService.parseSmmObject(jsonNode, Item.class);
    }

    private Message getSmmMessage(String fileName) throws IOException {
        var jsonNode = getRawSmmObject(fileName);
        return angrySpaceService.parseSmmObject(jsonNode, Message.class);
    }

    private void configureAttachmentUploadMock() throws Exception {
        when(attachmentsUploadService.getContentType(any()))
                .thenReturn(DEFAULT_MIME_TYPE);

        when(attachmentsUploadService.upload(any()))
                .thenReturn(DEFAULT_UPLOAD_URL);
    }

}

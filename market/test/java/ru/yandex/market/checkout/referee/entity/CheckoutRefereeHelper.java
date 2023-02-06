package ru.yandex.market.checkout.referee.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import com.google.common.collect.Sets;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.checkouter.cart.CartChange;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.UserGroup;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.checkout.entity.Attachment;
import ru.yandex.market.checkout.entity.AttachmentGroup;
import ru.yandex.market.checkout.entity.Conversation;
import ru.yandex.market.checkout.entity.ConversationObject;
import ru.yandex.market.checkout.entity.ConversationStatus;
import ru.yandex.market.checkout.entity.IssueType;
import ru.yandex.market.checkout.entity.Message;
import ru.yandex.market.checkout.entity.Note;
import ru.yandex.market.checkout.entity.NoteType;
import ru.yandex.market.checkout.entity.OrderInfo;
import ru.yandex.market.checkout.entity.PrivacyMode;
import ru.yandex.market.checkout.entity.RefereeRole;
import ru.yandex.market.checkout.entity.ResolutionSubtype;
import ru.yandex.market.checkout.entity.ResolutionType;
import ru.yandex.market.checkout.entity.ShopStatistic;
import ru.yandex.market.checkout.entity.structures.AttachedListResponse;
import ru.yandex.market.checkout.entity.structures.NotificationChunk;
import ru.yandex.market.checkout.entity.structures.OrderWithConversations;
import ru.yandex.market.checkout.entity.structures.PagedConversations;
import ru.yandex.market.checkout.entity.structures.PagedMessages;
import ru.yandex.market.checkout.entity.structures.PagedOrderWithConversations;
import ru.yandex.market.checkout.util.EnumSetUtil;
import ru.yandex.market.common.report.model.FeedOfferId;

public class CheckoutRefereeHelper {
    public static final long SHOP_ID = 774L;
    private static final long ORDER_ID = 123L;
    public static final long UID = Long.MIN_VALUE;
    public static final long ID = 1L;
    public static final long ITEM_ID = 2L;
    private static final String QUOTED_TEXT = "Text with enter:\\nNew line";
    private static final String TEXT = "Text with enter:\nNew line";
    private static final String NAME = "Kate";
    private static final String MESSAGE_CODE = "CODE_MESSAGE_1";

    // Helper's methods
    public static Message getMessage() {
        return new Message.Builder(ID, UID, RefereeRole.USER)
                .withId(ID)
                .withText(TEXT)
                .withCode(MESSAGE_CODE)
                .withAuthorName("Name-" + UID)
                .withMessageTs(getDate())
                .withConvStatusBefore(ConversationStatus.OPEN)
                .withConvStatusAfter(ConversationStatus.OPEN)
                .withResolutionSubType(ResolutionSubtype.BAD_PRODUCT)
                .withAttachmentGroupId(ID)
                .build();
    }

    public static PagedMessages getPagedMessages() {
        PagedMessages p = new PagedMessages();
        p.setItems(Arrays.asList(getMessage(), getMessage()));
        p.setPager(getPager());
        return p;
    }

    private static Date getDate() {
        Calendar c = Calendar.getInstance();
        c.set(2014, Calendar.JANUARY, 27);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 26);
        c.set(Calendar.SECOND, 5);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    public static String getDateString() {
        // timestamp
        Long d = getDate().getTime();

        // sdf
        // SimpleDateFormat sf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        // sf.setLenient(true);
        // String formatDate = sf.format(getDate());
        return d.toString();
    }

    public static PagedConversations getPagedConversations() {
        PagedConversations p = new PagedConversations();
        p.setItems(Arrays.asList(getConversation(), getConversation()));
        p.setPager(getPager());
        return p;
    }

    private static Pager getPager() {
        return Pager.atPage(1, 2);
    }

    public static Conversation getConversation() {
        Conversation conv = new Conversation();
        Date now = getDate();

        conv.setId(ID);
        conv.setOrder(getOrderInfo());
        conv.setObject(ConversationObject.fromOrderItem(conv.getOrder().getOrderId(), ITEM_ID));

        conv.setUid(UID);
        conv.setShopId(SHOP_ID);
        conv.setTitle("Conversation Title");
        conv.setLastStatus(ConversationStatus.OPEN);
        conv.setCreatedTs(now);
        conv.setLastAuthorRole(RefereeRole.USER);
        conv.setLastStatusTs(now);
        conv.setLastMessageTs(now);
        conv.setReadBy(RefereeRole.USER);
        conv.setReadBy(RefereeRole.SHOP);
        conv.setArchive(false);
        conv.setInquiryType(null);
        conv.setParticipatedBy(RefereeRole.USER);
        conv.setIssueTypes(EnumSetUtil.enumSet(IssueType.DELIVERY_DELAY));
        conv.setResolutionCount(1);
        conv.setResolutionType(ResolutionType.REFUND);
        conv.setResolutionSubType(ResolutionSubtype.NOT_DELIVERED);

        conv.setUpdatedMessage(getMessage(), getMessage());
        return conv;
    }

    public static Conversation getConversationSku() {
        Conversation conv = new Conversation();
        Date now = getDate();
        conv.setId(ID);
        conv.setObject(ConversationObject.fromSku("asdf",
                new ArrayList<>(Arrays.asList(
                        new FeedOfferId("offerId", 1L),
                        new FeedOfferId("offerId2", 2L)
                )))
        );
        conv.setUid(UID);
        conv.setShopId(SHOP_ID);
        conv.setTitle("Conversation Title");
        conv.setLastStatus(ConversationStatus.OPEN);
        conv.setCreatedTs(now);
        conv.setLastAuthorRole(RefereeRole.USER);
        conv.setLastStatusTs(now);
        conv.setLastMessageTs(now);
        conv.setReadBy(RefereeRole.USER);
        conv.setReadBy(RefereeRole.SHOP);
        conv.setArchive(false);
        conv.setParticipatedBy(RefereeRole.USER);

        conv.setUpdatedMessage(getMessage(), getMessage());
        return conv;
    }

    public static OrderInfo getOrderInfo() {
        OrderInfo order = new OrderInfo();
        order.setOrderId(ORDER_ID);
        order.setShopOrderId("12345");
        order.setUid(UID);
        order.setName(NAME);
        order.setEmail("kukabara@yandex-team.ru");
        order.setOrderTs(getDate());
        order.setShopId(SHOP_ID);
        return order;
    }


    public static Order getOrder() {
        Date now = getDate();
        Order o = new Order();
        o.setId(ID);
        o.setShopId(SHOP_ID);
        o.setShopOrderId("12345");
        o.setCreationDate(now);
        o.setBuyer(getBuyer());
        o.setPaymentOptions(Sets.newHashSet(PaymentMethod.YANDEX, PaymentMethod.CASH_ON_DELIVERY));
        o.setPaymentMethod(PaymentMethod.YANDEX);
        o.setDeliveryOptions(Collections.singletonList(getDelivery()));
        o.setCurrency(Currency.RUR);
        o.setItems(Collections.singletonList(getOrderItem()));
        o.setChanges(Sets.newHashSet(CartChange.DELIVERY));
        o.setStatus(OrderStatus.PROCESSING);
        o.setUserGroup(UserGroup.DEFAULT);
        return o;
    }

    private static OrderItem getOrderItem() {
        OrderItem oi = new OrderItem();
        oi.setOfferName("Телевизор");
        oi.setChanges(Sets.newHashSet(ItemChange.COUNT));
        oi.setBuyerPrice(new BigDecimal(100));
        oi.setFeedOfferId(new FeedOfferId("1", 1L));
        return oi;
    }

    private static Delivery getDelivery() {
        AddressImpl a = new AddressImpl();
        a.setCity("Moscow");
        Delivery delivery = new Delivery(213L, a);
        delivery.setDeliveryDates(new DeliveryDates(DateUtil.addDay(getDate(), -3), getDate()));
        return delivery;
    }

    private static Buyer getBuyer() {
        Buyer b = new Buyer();
        b.setEmail("kukabara@yandex-team.ru");
        b.setFirstName("Kate");
        b.setUid(UID);
        return b;
    }

    public static OrderWithConversations getOrderWithConversations() {
        OrderWithConversations oc = new OrderWithConversations();
        oc.setOrder(getOrder());
        oc.addConversation(getConversation());
        oc.addConversation(getConversation());
        return oc;
    }

    public static PagedOrderWithConversations getPagedOrderWithConversations() {
        PagedOrderWithConversations poc = new PagedOrderWithConversations();
        poc.setPager(Pager.atPage(1, 2));
        poc.setItems(Arrays.asList(getOrderWithConversations(), getOrderWithConversations()));
        return poc;
    }

    public static NotificationChunk getNotificationChunk() {
        NotificationChunk c = new NotificationChunk();
        c.addNote(getNote());
        c.addNote(getNote());
        return c;
    }

    public static Note getNote() {
        Conversation conv = getConversation();
        Message message = getMessage();

        Note note = new Note();
        note.setConversationId(conv.getId());
        note.setType(NoteType.NOTIFY_SHOP);
        note.setConvStatusBefore(message.getConvStatusBefore());
        note.setConvStatusAfter(message.getConvStatusAfter());
        note.setAuthorRole(message.getAuthorRole());
        note.setResolutionType(message.getResolutionType());
        note.setResolutionSubtype(message.getResolutionSubType());
        note.setEventTs(getDate().toInstant());
        note.setUid(UID);
        note.setShopId(conv.getShopId());
        OrderInfo order = conv.getOrder();
        if (order != null) {
            note.setUserName(order.getName());
            note.setUserEmail(order.getEmail());
            note.setOrderId(order.getOrderId());
            note.setObject(conv.getObject());
            note.setShopOrderId(order.getShopOrderId());
        }
        note.setInquiryDocs(conv.getInquiryType() != null && message.getMessageTs().equals(conv.getInquiryFromTs()));

        note.setId(ID);
        return note;
    }


    public static AttachmentGroup getAttachmentGroup() {
        AttachmentGroup ag = new AttachmentGroup();
        ag.setId(ID);
        ag.setAuthorRole(RefereeRole.USER);
        ag.setAuthorUid(UID);
        ag.setConversationId(ID);
        ag.setCreatedTs(getDate());
        ag.setAttachments(Arrays.asList(getAttachment(), getAttachment()));
        ag.setMessageId(ID);
        ag.setPrivacyMode(PrivacyMode.PM_TO_USER);
        return ag;
    }

    public static Attachment getAttachment() {
        Attachment a = new Attachment();
        a.setId(ID);
        a.setGroupId(ID);
        a.setContentType(NAME);
        a.setFileSize(ID);
        a.setFileName(NAME);
        a.setUploadTs(getDate());
        a.setLink("http://s3.mdst.yandex.net/market-abo-arbitrage/bea6e022-2ab8-4ac7-bb79-586623417b2b");
        return a;
    }

    // JSON
    public static String getJsonPagedConversations() {
        return "{\"pager\": " + getJsonPager() +
                ", \"conversations\":[" + getJsonConversation() + ", " + getJsonConversation() + "]}";
    }

    private static String getJsonObject() {
        return "\"object\":{\"objectType\":\"ORDER_ITEM\",\"orderId\":" + ORDER_ID + ",\"itemId\":" + ITEM_ID + "},";
    }

    public static String getJsonConversation() {
        return "{\"id\":" + ID + "," +
                "\"title\":\"Conversation Title\"," +
                "\"shopId\":" + SHOP_ID + "," +
                "\"shopOrderId\":\"12345\"," +
                "\"orderDate\":" + getDateString() + "," +
                "\"objectType\": \"ORDER_ITEM\"," +
                getJsonObject() +
                "\"orderId\":" + ORDER_ID + ",\"uid\":" + UID + "," +
                "\"name\":\"" + NAME + "\",\"email\":\"kukabara@yandex-team.ru\"," +
                "\"issueTypes\":[\"DELIVERY_DELAY\"],\"inquiryType\":\"NONE\",\"lastAuthorRole\":\"USER\"," +
                "\"checkType\":\"MANUAL\",\"lastStatus\":\"OPEN\",\"canReopen\":false,\"canRaiseIssue\":false," +
                "\"canEscalate\":false,\"canAddMessage\":false,\"canClose\":false," +
                "\"createdDate\":" + getDateString() + "," +
                "\"lastStatusDate\":" + getDateString() + ", \"lastMessageDate\":" + getDateString() + "," +
                "\"readStatusMask\":[\"USER\",\"SHOP\"]," +
                "\"participationMask\":[\"USER\"]," +
                "\"archive\":false, \"noteEventMask\":[],\"labelMask\":[]," +
                "\"unreadUserCount\":0,\"unreadShopCount\":0,\"unreadArbiterCount\":0, " +
                "\"resolutionCount\":1, " +
                "\"resolutionType\":\"REFUND\", " +
                "\"resolutionSubtype\":\"NOT_DELIVERED\", " +
                "\"updatedMessages\":[" +
                getJsonMessage() + "," + getJsonMessage() +
                "]" +
                "}";
    }

    public static String getJsonConversationSku() {
        return "{\"id\":" + ID + "," +
                "\"title\":\"Conversation Title\"," +
                "\"shopId\":" + SHOP_ID + "," +
                "\"objectType\": \"SKU\"," +

                "\"object\":{\"objectType\":\"SKU\",\"feedGroupIdHash\":\"asdf\",\"feedOfferIds\":[" +
                "{\"id\":\"offerId\",\"feedId\":1}, {\"id\":\"offerId2\",\"feedId\":2}" +
                "]}," +

                "\"uid\":" + UID + "," +
                "\"lastAuthorRole\":\"USER\",\"inquiryType\":\"NONE\"," +
                "\"checkType\":\"MANUAL\", \"lastStatus\":\"OPEN\",\"canReopen\":false,\"canRaiseIssue\":false," +
                "\"canEscalate\":false,\"canAddMessage\":false,\"canClose\":false," +
                "\"createdDate\":" + getDateString() + "," +
                "\"lastStatusDate\":" + getDateString() + ", \"lastMessageDate\":" + getDateString() + "," +
                "\"readStatusMask\":[\"USER\",\"SHOP\"]," +
                "\"participationMask\":[\"USER\"]," +
                "\"archive\":false, \"noteEventMask\":[],\"labelMask\":[]," +
                "\"unreadUserCount\":0,\"unreadShopCount\":0,\"unreadArbiterCount\":0, " +
                "\"resolutionCount\":0, " +
                "\"updatedMessages\":[" +
                getJsonMessage() + "," + getJsonMessage() +
                "]" +
                "}";
    }

    public static String getJsonOrderInfo() {
        return "{\n" +
                "\t\"orderId\":" + ORDER_ID + ",\n" +
                "\t\"shopOrderId\":\"12345\",\n" +
                "\t\"shopId\":" + SHOP_ID + ",\n" +
                "\t\"uid\":" + UID + ",\n" +
                "\t\"name\":\"" + NAME + "\",\n" +
                "\t\"email\":\"kukabara@yandex-team.ru\",\n" +
                "\t\"orderDate\":" + getDateString() + "\n" +
                "}";
    }

    private static String getJsonPager() {
        return "{\"from\":1,\"to\":2,\"page\":1,\"pageSize\":2}";
    }

    public static String getJsonPagedMessages() {
        return "{\"pager\": " + getJsonPager() +
                ", \"messages\":[" + getJsonMessage() + ", " + getJsonMessage() + "]}";
    }

    public static String getJsonMessage() {
        return "{\"id\":" + ID + "," +
                "\"authorRole\":\"USER\"," +
                "\"authorUid\":" + UID + "," +
                "\"authorName\":\"" + "Name-" + UID + "\"," +
                "\"conversationStatusAfter\":\"OPEN\"," +
                "\"conversationStatusBefore\":\"OPEN\"," +
                "\"resolutionSubtype\":\"BAD_PRODUCT\"," +
                "\"attachmentGroupId\":" + ID + "," +
                "\"conversationId\":" + ID + "," +
                "\"submitDate\": " + getDateString() + "," +
                "\"code\":\"" + MESSAGE_CODE + "\"," +
                "\"text\":\"" + QUOTED_TEXT + "\"}";
    }

    public static String getJsonNote() {
        return "{\"id\":" + ID + "," +
                "\"notificationType\":\"NOTIFY_SHOP\"," +
                "\"eventDate\": " + getDateString() + "," +
                "\"conversationId\":" + ID + "," +
                "\"authorRole\":\"USER\"," +
                "\"conversationStatusAfter\":\"OPEN\"," +
                "\"conversationStatusBefore\":\"OPEN\"," +
                "\"shopId\":" + SHOP_ID + "," +
                "\"uid\":" + UID + "," +
                "\"orderId\":" + ORDER_ID + "," +
                getJsonObject() +
                "\"shopOrderId\":\"12345\"," +
                "\"name\":\"" + NAME + "\"," +
                "\"email\":\"kukabara@yandex-team.ru\"," +
                "\"resolutionSubtype\":\"BAD_PRODUCT\"," +
                "\"inquiryDocs\":false" +
                "}";
    }

    public static String getJsonNotificationChunk() {
        return "{\n" +
                "    \"notifications\": [" + getJsonNote() + " ," + getJsonNote() + " ]\n" +
                "}";
    }

    private static String getJsonAttachmentGroup() {
        return "{\"id\":" + ID + "," +
                "\"conversationId\":" + ID + "," +
                "\"authorUid\":" + UID + "," +
                "\"authorRole\":\"USER\"," +
                "\"privacy\":\"PM_TO_USER\"," +
                "\"createdDate\":" + getDateString() + "," +
                "\"messageId\":" + ID + "," +
                "\"attachments\":[" + getJsonAttachment() + ", " + getJsonAttachment() + " ]}";
    }

    private static String getJsonAttachment() {
        return "{\"id\":" + ID + "," +
                "\"attachmentGroupId\":" + ID + "," +
                "\"contentType\":\"" + NAME + "\"," +
                "\"fileName\":\"" + NAME + "\"," +
                "\"fileSize\":" + ID + "," +
                "\"uploadDate\":" + getDateString() + "," +
                "\"link\":\"http://s3.mdst.yandex.net/market-abo-arbitrage/bea6e022-2ab8-4ac7-bb79-586623417b2b\"}";
    }

    public static String getXmlConversation() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<conversation id=\"" + ID + "\" title=\"Conversation Title\" shop-id=\"" + SHOP_ID + "\" " +
                "shop-order-id=\"12345\" order-date=\"" + getDateString() + "\" order-id=\"" + ORDER_ID + "\" " +
                "uid=\"" + UID + "\" name=\"Kate\" email=\"kukabara@yandex-team.ru\" " +
                "resolution-count=\"1\" resolution-type=\"REFUND\" " +
                "resolution-subtype=\"NOT_DELIVERED\" issue-types=\"DELIVERY_DELAY\" inquiry-type=\"NONE\" " +
                "closure-type=\"\" inquiry-due-date=\"\" claim-type=\"\" " +
                "last-author-role=\"USER\" last-status=\"OPEN\" check-type=\"MANUAL\" can-reopen=\"false\" " +
                "can-raise-issue=\"false\" can-escalate=\"false\" can-add-message=\"false\" " +
                "can-close=\"false\" created-date=\"" + getDateString() + "\" " +
                "last-status-date=\"" + getDateString() + "\" last-message-date=\"" + getDateString() +
                "\" resolution-date=\"\" " +
                "read-status-mask=\"3\" " +
                "can-escalate-after-date=\"\" can-raise-issue-after-date=\"\" can-raise-issue-before-date=\"\" " +
                "unread-user-count=\"0\" unread-shop-count=\"0\" unread-arbiter-count=\"0\" " +
                "can-reopen-before-date=\"\"  label-mask=\"0\" last-label=\"\" auto-close-issue-date=\"\" " +
                "participation-mask=\"1\" archive=\"false\" note-event-mask=\"0\" objectType=\"ORDER_ITEM\">" +
                "<object objectType=\"ORDER_ITEM\" orderId=\"" + ORDER_ID + "\" itemId=\"" + ITEM_ID + "\"/>" +
                "<updated-messages>" +
                getXmlMessageInner() + getXmlMessageInner() +
                "</updated-messages>" +
                "</conversation>";
    }

    public static String getXmlMessage() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + getXmlMessageInner();
    }

    private static String getXmlMessageInner() {
        return "<message id=\"" + ID + "\" conversation-id=\"" + ID + "\" author-role=\"USER\" author-uid=\"" + UID +
                "\" author-name=\"Name-" + UID + "\" conversation-status-after=\"OPEN\" conversation-status-before=\"OPEN\" " +
                "resolution-type=\"\" resolution-subtype=\"BAD_PRODUCT\" submit-date=\"" + getDateString() +
                "\" label=\"\" code=\"" + MESSAGE_CODE + "\" attachment-group-id=\"" + ID + "\">" +
                TEXT + "</message>";
    }

    // NoteXmlSerializer
    public static String getXmlNote() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<notification id=\"" + ID + "\" conversation-id=\"" + ID +
                "\" shop-id=\"" + SHOP_ID + "\" order-id=\"" + ORDER_ID +
                "\" shop-order-id=\"12345\" " +
                "author-role=\"USER\" " +
                "conversation-status-before=\"OPEN\" conversation-status-after=\"OPEN\" " +
                "resolution-type=\"\" resolution-subtype=\"BAD_PRODUCT\" " +
                "inquiry-docs=\"false\" warning-date=\"\" payload=\"\" " +
                "notification-type=\"NOTIFY_SHOP\" name=\"" + NAME + "\" email=\"kukabara@yandex-team.ru\"/>";
    }

    public static String getXmlAttachment() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                getAttachmentInner();
    }

    private static String getAttachmentInner() {
        return "<attachment id=\"" + ID + "\" attachment-group-id=\"" + ID + "\" content-type=\"" + NAME + "\" " +
                "file-name=\"" + NAME + "\" file-size=\"" + ID + "\" upload-date=\"" + getDateString() + "\" " +
                "link=\"http://s3.mdst.yandex.net/market-abo-arbitrage/bea6e022-2ab8-4ac7-bb79-586623417b2b\"/>";
    }

    public static String getXmlAttachmentGroup() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<attachment-group id=\"" + ID + "\" conversation-id=\"" + ID + "\" author-uid=\"" + UID + "\" " +
                "author-role=\"USER\" privacy=\"PM_TO_USER\" created-date=\"" + getDateString() + "\" message-id=\"" + ID + "\">" +
                "<attachments>" +
                getAttachmentInner() +
                getAttachmentInner() +
                "</attachments></attachment-group>";
    }

    public static ShopStatistic getShopStatistic() {
        ShopStatistic s = new ShopStatistic();
        s.setShopId(774);
        s.setUnreadMessagesCount(411);
        s.setUnreadConvCount(178);
        s.setConvCount(267);
        s.setIssueCount(1);
        s.setArbitrageCount(20);
        s.setUnreadMesWithRefundCount(128);
        s.setUnreadConvWithRefundCount(19);
        s.setAnswerTime(65360);
        return s;
    }

    public static String getJsonShopStatistic() {
        return "{\n" +
                "    \"shopId\": 774,\n" +
                "    \"unreadMessagesCount\": 411,\n" +
                "    \"unreadConvCount\": 178,\n" +
                "    \"convCount\": 267,\n" +
                "    \"issueCount\": 1,\n" +
                "    \"arbitrageCount\": 20,\n" +
                "    \"unreadMesRefundCount\": 128,\n" +
                "    \"unreadConvRefundCount\": 19,\n" +
                "    \"answerTime\": 65360\n" +
                "}";
    }

    public static AttachedListResponse getAttachedListResponse() {
        AttachedListResponse r = new AttachedListResponse();
        r.add(getAttachmentGroup());
        r.add(getAttachmentGroup());
        return r;
    }

    public static String getJsonAttachedListResponse() {
        return "[ " + getJsonAttachmentGroup() + ", " + getJsonAttachmentGroup() + "]";
    }
}

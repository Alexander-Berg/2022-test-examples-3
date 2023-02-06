package ru.yandex.market.crm.operatorwindow.external;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.crm.operatorwindow.external.platform.converter.EoEntityBuilderWrap;
import ru.yandex.market.crm.platform.commons.NullableBool;
import ru.yandex.market.crm.platform.models.EoTicket;
import ru.yandex.market.jmf.entity.HasGid;
import ru.yandex.market.jmf.logic.def.Attachment;
import ru.yandex.market.jmf.logic.def.HasTitle;
import ru.yandex.market.jmf.logic.wf.conf.Status;
import ru.yandex.market.jmf.metadata.Constants;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.ou.Employee;
import ru.yandex.market.jmf.utils.date.Dates;
import ru.yandex.market.ocrm.module.order.domain.Order;

import static org.mockito.Mockito.when;

public class EoEntityBuilderTest {

    private static final String DEFAULT_STRING_VALUE = "";
    private static final TestBo TEST_BO1 = new TestBo("testGid1", "testTitle1");
    private static final TestBo TEST_BO2 = new TestBo("testGid2", "testTitle2");
    private static final Status TEST_STATUS = new TestStatus("testCode", "testTitle");
    private static final long TEST_DATE_LONG_VALUE = 1573129510000L;
    private static final OffsetDateTime TEST_DATE = Dates.toOffsetDateTime(TEST_DATE_LONG_VALUE);

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSetStringNull() {
        EoEntityBuilderWrap<TestBuilder> builderWrap = new EoEntityBuilderWrap(TestBuilder::new);
        builderWrap.setString(null, TestBuilder::setStringValue);
        TestBuilder testBuilder = builderWrap.getBuilder();
        Assertions.assertEquals(DEFAULT_STRING_VALUE, testBuilder.getStringValue());
    }

    @Test
    public void testSetStringNotNull() {
        EoEntityBuilderWrap<TestBuilder> builderWrap = new EoEntityBuilderWrap(TestBuilder::new);
        String value = "testValue";
        builderWrap.setString(value, TestBuilder::setStringValue);
        TestBuilder testBuilder = builderWrap.getBuilder();
        Assertions.assertEquals(value, testBuilder.getStringValue());
    }

    @Test
    public void testSetEntityNull() {
        EoEntityBuilderWrap<TestBuilder> builderWrap = new EoEntityBuilderWrap(TestBuilder::new);
        builderWrap.setEntity(null, TestBuilder::setEoObject);
        TestBuilder testBuilder = builderWrap.getBuilder();
        Assertions.assertNull(testBuilder.getEoObject());
    }

    @Test
    public void testSetEntityNotNull() {
        EoEntityBuilderWrap<TestBuilder> builderWrap = new EoEntityBuilderWrap(TestBuilder::new);
        builderWrap.setEntity(TEST_BO1, TestBuilder::setEoObject);
        TestBuilder testBuilder = builderWrap.getBuilder();
        assertEoObject(TEST_BO1, testBuilder.getEoObject());
    }

    @Test
    public void testSetEntitiesNull() {
        EoEntityBuilderWrap<TestBuilder> builderWrap = new EoEntityBuilderWrap(TestBuilder::new);
        builderWrap.setEntities(null, TestBuilder::addEoObjectToList);
        TestBuilder testBuilder = builderWrap.getBuilder();
        Assertions.assertEquals(Collections.emptyList(), testBuilder.getEoObjects());
    }

    @Test
    public void testSetEntitiesNotNull() {
        EoEntityBuilderWrap<TestBuilder> builderWrap = new EoEntityBuilderWrap(TestBuilder::new);
        builderWrap.setEntities(List.of(TEST_BO1, TEST_BO2), TestBuilder::addEoObjectToList);
        TestBuilder testBuilder = builderWrap.getBuilder();
        Assertions.assertNotNull(testBuilder.getEoObjects());
        Assertions.assertEquals(2, testBuilder.getEoObjects().size());
        assertEoObject(TEST_BO1, testBuilder.getEoObjects().get(0));
        assertEoObject(TEST_BO2, testBuilder.getEoObjects().get(1));
    }

    @Test
    public void testSetWfStatusNull() {
        EoEntityBuilderWrap<TestBuilder> builderWrap = new EoEntityBuilderWrap(TestBuilder::new);
        builderWrap.setWfStatus(null, TestBuilder::setWfStatus);
        TestBuilder testBuilder = builderWrap.getBuilder();
        Assertions.assertNull(testBuilder.getWfStatus());
    }

    @Test
    public void testSetWfStatusNotNull() {
        EoEntityBuilderWrap<TestBuilder> builderWrap = new EoEntityBuilderWrap(TestBuilder::new);
        builderWrap.setWfStatus(TEST_STATUS, TestBuilder::setWfStatus);
        TestBuilder testBuilder = builderWrap.getBuilder();
        Assertions.assertNotNull(testBuilder.getWfStatus());
        Assertions.assertEquals(TEST_STATUS.getCode(), testBuilder.getWfStatus().getCode());
        Assertions.assertEquals(TEST_STATUS.getTitle(), testBuilder.getWfStatus().getTitle());
    }

    @Test
    public void testSetDateNull() {
        EoEntityBuilderWrap<TestBuilder> builderWrap = new EoEntityBuilderWrap(TestBuilder::new);
        builderWrap.setDate(null, TestBuilder::setDateValue);
        TestBuilder testBuilder = builderWrap.getBuilder();
        Assertions.assertEquals(0, testBuilder.getDateValue());
    }

    @Test
    public void testSetDateNotNull() {
        EoEntityBuilderWrap<TestBuilder> builderWrap = new EoEntityBuilderWrap(TestBuilder::new);
        builderWrap.setDate(TEST_DATE, TestBuilder::setDateValue);
        TestBuilder testBuilder = builderWrap.getBuilder();
        Assertions.assertEquals(TEST_DATE_LONG_VALUE, testBuilder.getDateValue());
    }

    @Test
    public void testSetLongNull() {
        EoEntityBuilderWrap<TestBuilder> builderWrap = new EoEntityBuilderWrap(TestBuilder::new);
        builderWrap.setLong(null, TestBuilder::setLongValue);
        TestBuilder testBuilder = builderWrap.getBuilder();
        Assertions.assertEquals(0, testBuilder.getLongValue());
    }

    @Test
    public void testSetLongNotNull() {
        EoEntityBuilderWrap<TestBuilder> builderWrap = new EoEntityBuilderWrap(TestBuilder::new);
        long value = 123L;
        builderWrap.setLong(value, TestBuilder::setLongValue);
        TestBuilder testBuilder = builderWrap.getBuilder();
        Assertions.assertEquals(value, testBuilder.getLongValue());
    }

    @Test
    public void testSetNullableBoolNull() {
        EoEntityBuilderWrap<TestBuilder> builderWrap = new EoEntityBuilderWrap(TestBuilder::new);
        builderWrap.setNullableBool(null, TestBuilder::setNullableBoolValue);
        TestBuilder testBuilder = builderWrap.getBuilder();
        Assertions.assertEquals(NullableBool.NULL, testBuilder.getNullableBoolValue());
    }

    @Test
    public void testSetNullableBoolNotNull() {
        EoEntityBuilderWrap<TestBuilder> builderWrap = new EoEntityBuilderWrap(TestBuilder::new);
        builderWrap.setNullableBool(true, TestBuilder::setNullableBoolValue);
        TestBuilder testBuilder = builderWrap.getBuilder();
        Assertions.assertEquals(NullableBool.TRUE, testBuilder.getNullableBoolValue());
    }

    @Test
    public void testSetOrderNull() {
        EoEntityBuilderWrap<TestBuilder> builderWrap = new EoEntityBuilderWrap(TestBuilder::new);
        builderWrap.setOrder(null, TestBuilder::setEoObject);
        TestBuilder testBuilder = builderWrap.getBuilder();
        Assertions.assertNull(testBuilder.getEoObject());
    }

    @Test
    public void testSetOrderNotNull() {
        EoEntityBuilderWrap<TestBuilder> builderWrap = new EoEntityBuilderWrap(TestBuilder::new);
        Order order = createOrder("orderGid", 123456L);
        builderWrap.setOrder(order, TestBuilder::setEoObject);
        TestBuilder testBuilder = builderWrap.getBuilder();
        Assertions.assertNotNull(testBuilder.getEoObject());
        Assertions.assertEquals(order.getGid(), testBuilder.getEoObject().getGid());
        Assertions.assertEquals(String.valueOf(order.getTitle()), testBuilder.getEoObject().getTitle());
    }

    @Test
    public void testSetCommentsNull() {
        EoEntityBuilderWrap<TestBuilder> builderWrap = new EoEntityBuilderWrap(TestBuilder::new);
        builderWrap.setComments(null, null, TestBuilder::addComment);
        TestBuilder testBuilder = builderWrap.getBuilder();
        Assertions.assertEquals(Collections.emptyList(), testBuilder.getComments());
    }

    @Test
    public void testSetCommentsNotNull() {
        EoEntityBuilderWrap<TestBuilder> builderWrap = new EoEntityBuilderWrap(TestBuilder::new);
        Comment comment1 = createComment(1);
        Comment comment2 = createComment(2);
        List<Attachment> attachments1 = List.of(createAttachment(1), createAttachment(2));
        List<Attachment> attachments2 = List.of(createAttachment(3));
        Map<Comment, Collection<Attachment>> attachments = Map.of(
                comment1, attachments1,
                comment2, attachments2
        );
        builderWrap.setComments(List.of(comment1, comment2), attachments::get, TestBuilder::addComment);
        TestBuilder testBuilder = builderWrap.getBuilder();
        Assertions.assertNotNull(testBuilder.getComments());
        Assertions.assertEquals(2, testBuilder.getComments().size());
        assertComment(comment1, attachments1, testBuilder.getComments().get(0));
        assertComment(comment2, attachments2, testBuilder.getComments().get(1));
    }

    private void assertEoObject(TestBo expected, EoTicket.EoObject.Builder eoObject) {
        Assertions.assertNotNull(eoObject);
        Assertions.assertEquals(expected.getGid(), eoObject.getGid());
        Assertions.assertEquals(expected.getTitle(), eoObject.getTitle());
    }

    private void assertComment(Comment comment,
                               List<Attachment> attachments,
                               EoTicket.EoComment.Builder eoComment) {
        Assertions.assertNotNull(eoComment);
        Assertions.assertEquals(comment.getCreationTime().toInstant().toEpochMilli(), eoComment.getCreatedAt());
        Assertions.assertEquals(comment.getBody(), eoComment.getBody());
        Assertions.assertEquals(comment.getFqn().toString(), eoComment.getMetaclass());

        Assertions.assertNotNull(eoComment.getAuthor());
        Assertions.assertEquals(comment.getAuthor().getGid(), eoComment.getAuthor().getGid());
        Assertions.assertEquals(comment.getAuthor().getTitle(), eoComment.getAuthor().getTitle());

        Assertions.assertNotNull(eoComment.getAttachmentsList());
        Assertions.assertEquals(attachments.size(), eoComment.getAttachmentsList().size());
        for (int i = 0; i < attachments.size(); i++) {
            assertAttachment(attachments.get(i), eoComment.getAttachments(i));
        }
    }

    private void assertAttachment(Attachment attachment, EoTicket.EoAttachment eoAttachment) {
        Assertions.assertNotNull(eoAttachment);
        Assertions.assertEquals(attachment.getName(), eoAttachment.getName());
        Assertions.assertEquals(attachment.getContentType(), eoAttachment.getContentType());
        Assertions.assertEquals(attachment.getUrl(), eoAttachment.getUrl());
        Assertions.assertEquals(attachment.getCid(), eoAttachment.getCid());
    }

    private Order createOrder(String gid, long title) {
        Order order = Mockito.mock(Order.class);
        when(order.getGid()).thenReturn(gid);
        when(order.getTitle()).thenReturn(title);
        return order;
    }

    private Comment createComment(int index) {
        Comment comment = Mockito.mock(Comment.class);
        when(comment.getCreationTime()).thenReturn(TEST_DATE.plusHours(index));
        when(comment.getBody()).thenReturn("body" + index);
        when(comment.getFqn()).thenReturn(Fqn.of("metaclass" + index));

        Employee author = Mockito.mock(Employee.class);
        when(author.getGid()).thenReturn("authorGid" + index);
        when(author.getTitle()).thenReturn("authorTitle" + index);
        when(comment.getAuthor()).thenReturn(author);
        return comment;
    }

    private Attachment createAttachment(int index) {
        Attachment attachment = Mockito.mock(Attachment.class);
        when(attachment.getName()).thenReturn("attachmentName" + index);
        when(attachment.getContentType()).thenReturn("attachmentContentType" + index);
        when(attachment.getUrl()).thenReturn("attachmentUrl" + index);
        when(attachment.getCid()).thenReturn("attachmentCid" + index);
        return attachment;
    }

    private static class TestBuilder {

        private final List<EoTicket.EoObject.Builder> eoObjects = new ArrayList<>();
        private final List<EoTicket.EoComment.Builder> comments = new ArrayList<>();
        private String stringValue = DEFAULT_STRING_VALUE;
        private EoTicket.EoObject.Builder eoObject;
        private EoTicket.EoStatus.Builder wfStatus;
        private long dateValue;
        private long longValue;
        private NullableBool nullableBoolValue;

        public String getStringValue() {
            return stringValue;
        }

        public void setStringValue(String stringValue) {
            this.stringValue = stringValue;
        }

        public EoTicket.EoObject.Builder getEoObject() {
            return eoObject;
        }

        public void setEoObject(EoTicket.EoObject.Builder eoObject) {
            this.eoObject = eoObject;
        }

        public List<EoTicket.EoObject.Builder> getEoObjects() {
            return eoObjects;
        }

        public void addEoObjectToList(EoTicket.EoObject.Builder eoObject) {
            eoObjects.add(eoObject);
        }

        public EoTicket.EoStatus.Builder getWfStatus() {
            return wfStatus;
        }

        public void setWfStatus(EoTicket.EoStatus.Builder wfStatus) {
            this.wfStatus = wfStatus;
        }

        public long getDateValue() {
            return dateValue;
        }

        public void setDateValue(long dateValue) {
            this.dateValue = dateValue;
        }

        public long getLongValue() {
            return longValue;
        }

        public void setLongValue(long longValue) {
            this.longValue = longValue;
        }

        public NullableBool getNullableBoolValue() {
            return nullableBoolValue;
        }

        public void setNullableBoolValue(NullableBool nullableBoolValue) {
            this.nullableBoolValue = nullableBoolValue;
        }

        public List<EoTicket.EoComment.Builder> getComments() {
            return comments;
        }

        public void addComment(EoTicket.EoComment.Builder eoComment) {
            comments.add(eoComment);
        }
    }

    private static class TestBo implements HasGid, HasTitle {

        private final String gid;
        private final String title;

        public TestBo(String gid, String title) {
            this.gid = gid;
            this.title = title;
        }

        @Override
        public String getGid() {
            return gid;
        }

        @Override
        public String getTitle() {
            return title;
        }
    }

    private static class TestStatus extends Status {

        protected TestStatus(String code, String title) {
            super(code);
            this.title = Map.of(Constants.DEFAULT_LANG, title);
        }
    }
}

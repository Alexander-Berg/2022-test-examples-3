package ru.yandex.market.pers.grade.admin.action.moderation;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.impl.EmptyMap;
import ru.yandex.common.framework.user.UserInfoField;
import ru.yandex.common.framework.user.UserInfoService;
import ru.yandex.common.framework.user.blackbox.BlackBoxUserInfo;
import ru.yandex.market.pers.grade.admin.MockedPersGradeAdminTest;
import ru.yandex.market.pers.grade.admin.action.moderation.complaint.ComplaintTicketType;
import ru.yandex.market.pers.grade.admin.action.moderation.complaint.CreateComplaintTicketController;
import ru.yandex.market.pers.grade.client.model.Delivery;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.model.core.AbstractGrade;
import ru.yandex.market.pers.grade.core.model.core.ModelGrade;
import ru.yandex.market.pers.grade.core.model.core.ShopGrade;
import ru.yandex.market.pers.grade.core.util.MbiApiClient;
import ru.yandex.market.pers.notify.PersNotifyClient;
import ru.yandex.market.pers.notify.PersNotifyClientException;
import ru.yandex.market.pers.notify.model.Email;
import ru.yandex.market.pers.service.common.startrek.StartrekService;
import ru.yandex.market.util.ExecUtils;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueCreate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.grade.admin.action.moderation.complaint.ComplaintTicketType.BERU_COMPLAINT;
import static ru.yandex.market.pers.grade.admin.action.moderation.complaint.ComplaintTicketType.CONTENT_COMPLAINT;
import static ru.yandex.market.pers.grade.admin.action.moderation.complaint.ComplaintTicketType.SHOP_COMPLAINT;

/**
 * @author vvolokh
 * 14.08.2018
 */
public class CreateComplaintTicketControllerTest extends MockedPersGradeAdminTest {

    private static final long FAKE_USER = 1L;
    private static final long SBER_ID = (1L << 61) - 1L;
    private static final long TEST_SHOP_ID = 123321;
    private static final String TEST_ORDER_ID = "-666777888";
    private static final String USER_LOGIN = "login";
    private static final String USER_EMAIL = "usr@yandex.ru";
    private static final Issue ISSUE_1 = new Issue("1", null, "MARKETQUALITY-1", null, 1, new EmptyMap<>(), null);
    private static final Issue ISSUE_2 = new Issue("2", null, "MARKETCLAIMS-2", null, 1, new EmptyMap<>(), null);
    private static final long BERU_SHOP_ID = 431782;
    private static final String GRADE_IN_PI_PART =
            "  ((https://partner.market.yandex.ru/shops/null/reviews?gradeId=%d&activeTab=REVIEWS&euid=0 Ссылка на " +
                    "отзыв в ПИ))\n" +
                    "  %%%%https://partner.market.yandex.ru/shops/null/reviews?gradeId=%d&activeTab=REVIEWS&euid=0" +
                    "%%%%\n";
    private static final String GRADE_WITHOUT_PI_PART =
            "  В данный момент у магазина нет кабинета в ПИ\n";
    private static final String EXPECTED_DESCRIPTION_FOR_SHOP = "Идентификатор заказа: -666777888\n" +
            "**E-mail пользователя:**\n  " +
            USER_EMAIL +
            " \n**Текст отзыва:**\n" +
            "  Best you can afford\n" +
            "  Преимущества: Fast, supports deep class III warp diving\n" +
            "  Недостатки: Price is huge. Required experienced engineers to operate/repair \n" +
            "**Полезные ссылки:** \n" +
            "   ((https://admin.pers.market.yandex.ru/manage-grades.xml?filter_grade_id=%d Ссылка на отзыв в персах))" +
            "\n" +
            "%s" +
            "  ((https://admin.pers.market.yandex.ru/manage-grades" +
            ".xml?filter_shop_id=123321&order_by=CR_TIME&order_by_mode=desc Все отзывы на магазин в персах))\n" +
            "  ((https://market.yandex.ru/shop/123321/reviews Все отзывы на магазин на Маркете))\n" +
            "  ((https://abo.market.yandex-team.ru/shop/shop-problem.xml?shop_id=123321 Страничка магазина в або))\n" +
            "  ((https://admin.pers.market.yandex.ru/forum/lite/index.xml?gradeId=%d Ссылка на комментарии " +
            "(/forum/lite) ))";
    private static final String EXPECTED_DESCRIPTION_FOR_MODEL =
            "**Текст отзыва:**\n" +
                    "  Best you can afford\n" +
                    "  Преимущества: Fast, supports deep class III warp diving\n" +
                    "  Недостатки: Price is huge. Required experienced engineers to operate/repair \n" +
                    "**Полезные ссылки:** \n" +
                    "   ((https://admin.pers.market.yandex.ru/model/manage-grades.xml?filter_grade_id=%d Ссылка на " +
                    "отзыв в персах))\n" +
                    "  ((https://admin.pers.market.yandex.ru/model/manage-grades" +
                    ".xml?filter_shop_id=1&order_by=CR_TIME&order_by_mode=desc Все отзывы на модель в персах))\n" +
                    "  ((https://market.yandex.ru/product/1/reviews Все отзывы на модель на Маркете))\n" +
                    "  ((https://admin.pers.market.yandex.ru/forum/lite/index.xml?gradeId=%d Ссылка на комментарии " +
                    "(/forum/lite) ))";
    private static final String EXPECTED_DESCRIPTION_FOR_MODEL_MARKETCLAIMS_QUEUE =
            String.format("**E-mail пользователя:**\n  %s \n%s", USER_EMAIL, EXPECTED_DESCRIPTION_FOR_MODEL);
    private static final String startrekUrl = "https://st.test.yandex-team.ru/";
    private static final String startrekTicketType = "problem";
    private static final String startrekUserLogin = "robot-market-pers";
    private static final Integer scamComponentId = 41815;
    private static final Integer shopComplaintComponentId = 41814;
    @Autowired
    private StartrekService startrekService;
    @Autowired
    private UserInfoService blackBoxUserService;
    @Autowired
    private CreateComplaintTicketController servantlet;
    @Autowired
    private MbiApiClient mbiApiClient;
    @Autowired
    private PersNotifyClient persNotifyClient;
    @Autowired
    private GradeCreator gradeCreator;

    @Before
    public void setup() {
        when(startrekService.createTicket(any())).thenReturn(ISSUE_1);
        BlackBoxUserInfo userInfo = new BlackBoxUserInfo(FAKE_USER);
        userInfo.addField(UserInfoField.LOGIN, USER_LOGIN);
        when(blackBoxUserService.getUserInfo(anyLong())).thenReturn(userInfo);
    }

    public void mockUserEmail(String value) {
        try {
            when(persNotifyClient.getEmails(anyLong())).then(invocation -> {
                if (value != null) {
                    return Set.of(new Email(value, true));
                } else {
                    return Collections.emptySet();
                }
            });
        } catch (PersNotifyClientException e) {
            throw ExecUtils.silentError(e);
        }
    }

    @Test
    public void testWhiteShopScam() {
        //given
        ShopGrade grade = createTestShopGrade(TEST_SHOP_ID);
        mockUserEmail(USER_EMAIL);
        //when
        Map<String, String> res = getStubServRequest(ComplaintTicketType.SCAM, grade);
        //then
        verify(mbiApiClient).getSuperAdminUid(grade.getShopId());
        ArgumentCaptor<IssueCreate> issueCreateArgumentCaptor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(startrekService).createTicket(issueCreateArgumentCaptor.capture());
        assertResponseHasTicketLink(res);
        assertCorrectTicketDataWhite(grade, scamComponentId, issueCreateArgumentCaptor);
        long gradeId = grade.getId();
        Assert.assertEquals(
                String.format(EXPECTED_DESCRIPTION_FOR_SHOP, gradeId, String.format(GRADE_IN_PI_PART, gradeId,
                        gradeId), gradeId),
                issueCreateArgumentCaptor.getValue().getValues().getOptional("description").get());
        checkUserEmail(USER_EMAIL, issueCreateArgumentCaptor);
    }

    @Test
    public void testWhiteShopScamWithoutSuperAdminUid() {
        //given
        ShopGrade grade = createTestShopGrade(TEST_SHOP_ID);
        Mockito.when(mbiApiClient.getSuperAdminUid(any())).thenReturn(null);
        mockUserEmail(USER_EMAIL);
        //when
        Map<String, String> res = getStubServRequest(ComplaintTicketType.SCAM, grade);
        //then
        verify(mbiApiClient).getSuperAdminUid(grade.getShopId());
        ArgumentCaptor<IssueCreate> issueCreateArgumentCaptor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(startrekService).createTicket(issueCreateArgumentCaptor.capture());
        assertResponseHasTicketLink(res);
        assertCorrectTicketDataWhite(grade, scamComponentId, issueCreateArgumentCaptor);
        long gradeId = grade.getId();
        Assert.assertEquals(
                String.format(EXPECTED_DESCRIPTION_FOR_SHOP, gradeId, GRADE_WITHOUT_PI_PART, gradeId),
                issueCreateArgumentCaptor.getValue().getValues().getOptional("description").get());
    }

    @Test
    public void testWhiteShopScamWithoutUserEmail() {
        //given
        ShopGrade grade = createTestShopGrade(TEST_SHOP_ID);
        mockUserEmail(null);
        //when
        Map<String, String> res = getStubServRequest(ComplaintTicketType.SCAM, grade);
        //then
        ArgumentCaptor<IssueCreate> issueCreateArgumentCaptor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(startrekService).createTicket(issueCreateArgumentCaptor.capture());
        assertResponseHasTicketLink(res);
        assertCorrectTicketDataWhite(grade, scamComponentId, issueCreateArgumentCaptor);
        userEmailIsNotPresent(issueCreateArgumentCaptor);
    }

    @Test
    public void testWhiteShopScamForSberIdUser() {
        //given
        ShopGrade grade = createTestShopGrade(TEST_SHOP_ID, TEST_ORDER_ID, SBER_ID);
        //when
        Map<String, String> res = getStubServRequest(ComplaintTicketType.SCAM, grade);
        //then
        ArgumentCaptor<IssueCreate> issueCreateArgumentCaptor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(startrekService).createTicket(issueCreateArgumentCaptor.capture());
        assertResponseHasTicketLink(res);
        assertCorrectTicketDataWhite(grade, scamComponentId, issueCreateArgumentCaptor);
    }

    @Test
    public void testLinkBetweenLinkedComplaintQueues() {
        //given
        ShopGrade grade = createTestShopGrade(TEST_SHOP_ID);
        mockUserEmail(USER_EMAIL);

        when(startrekService.createTicket(any())).thenReturn(ISSUE_1);
        getStubServRequest(ComplaintTicketType.SHOP_COMPLAINT, grade);
        when(startrekService.createTicket(any())).thenReturn(ISSUE_2);
        getStubServRequest(ComplaintTicketType.FAKE_COMPLAINT, grade);
        when(startrekService.createTicket(any())).thenReturn(ISSUE_1);
        getStubServRequest(ComplaintTicketType.SCAM, grade);

        ArgumentCaptor<IssueCreate> issueCreateArgumentCaptor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(startrekService, times(3)).createTicket(issueCreateArgumentCaptor.capture());
        assertEquals(2, issueCreateArgumentCaptor.getValue().getLinks().size());
    }

    @Test
    public void testLinkBetweenNotLinkedQueues() {
        //given
        ShopGrade grade = createTestShopGrade(TEST_SHOP_ID);
        mockUserEmail(USER_EMAIL);

        when(startrekService.createTicket(any())).thenReturn(
                new Issue("1", null, "MARKETTEST-1", null, 1, new EmptyMap<>(), null));
        getStubServRequest(ComplaintTicketType.FAKE_COMPLAINT, grade);
        when(startrekService.createTicket(any())).thenReturn(ISSUE_1);
        getStubServRequest(ComplaintTicketType.SHOP_COMPLAINT, grade);

        ArgumentCaptor<IssueCreate> issueCreateArgumentCaptor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(startrekService, times(2)).createTicket(issueCreateArgumentCaptor.capture());
        assertEquals(0, issueCreateArgumentCaptor.getValue().getLinks().size());
    }

    /**
     * Проверяем, что id заказа, превышающий пределы Integer, не попадет в запрос на создание тикета.
     */
    @Test
    public void testOrderIdOverflow() {
        //given
        ShopGrade grade = createTestShopGrade(TEST_SHOP_ID, String.valueOf((long) Integer.MAX_VALUE + 1), FAKE_USER);
        //when
        Map<String, String> res = getStubServRequest(ComplaintTicketType.SCAM, grade);
        //then
        ArgumentCaptor<IssueCreate> issueCreateArgumentCaptor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(startrekService).createTicket(issueCreateArgumentCaptor.capture());
        assertResponseHasTicketLink(res);
        assertFalse(issueCreateArgumentCaptor.getValue().getValues().containsKeyTu("idZakaza"));
    }

    @Test
    public void testWhiteShopComplaint() {
        //given
        ShopGrade grade = createTestShopGrade(TEST_SHOP_ID);
        mockUserEmail(USER_EMAIL);
        //when
        Map<String, String> res = getStubServRequest(ComplaintTicketType.SHOP_COMPLAINT, grade);
        //then
        ArgumentCaptor<IssueCreate> issueCreateArgumentCaptor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(startrekService).createTicket(issueCreateArgumentCaptor.capture());
        assertResponseHasTicketLink(res);
        assertCorrectTicketDataWhite(grade, shopComplaintComponentId, issueCreateArgumentCaptor);
        checkUserEmail(USER_EMAIL, issueCreateArgumentCaptor);
    }

    @Test
    public void testWhiteShopComplaintWithoutUserEmail() {
        //given
        ShopGrade grade = createTestShopGrade(TEST_SHOP_ID);
        mockUserEmail(null);
        //when
        Map<String, String> res = getStubServRequest(ComplaintTicketType.SHOP_COMPLAINT, grade);
        //then
        ArgumentCaptor<IssueCreate> issueCreateArgumentCaptor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(startrekService).createTicket(issueCreateArgumentCaptor.capture());
        assertResponseHasTicketLink(res);
        assertCorrectTicketDataWhite(grade, shopComplaintComponentId, issueCreateArgumentCaptor);
        userEmailIsNotPresent(issueCreateArgumentCaptor);
    }

    @Test
    public void testBlueShopScam() {
        //given
        ShopGrade grade = createTestShopGrade(BERU_SHOP_ID);
        //when
        Map<String, String> res = getStubServRequest(ComplaintTicketType.SCAM, grade);
        //then
        ArgumentCaptor<IssueCreate> issueCreateArgumentCaptor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(startrekService).createTicket(issueCreateArgumentCaptor.capture());
        assertResponseHasTicketLink(res);
        assertCorrectTicketDataBlue(grade, scamComponentId, issueCreateArgumentCaptor);
    }

    @Test
    public void testBlueShopComplaint() {
        //given
        ShopGrade grade = createTestShopGrade(BERU_SHOP_ID);
        //when
        Map<String, String> res = getStubServRequest(ComplaintTicketType.SHOP_COMPLAINT, grade);
        //then
        ArgumentCaptor<IssueCreate> issueCreateArgumentCaptor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(startrekService).createTicket(issueCreateArgumentCaptor.capture());
        assertResponseHasTicketLink(res);
        assertCorrectTicketDataBlue(grade, shopComplaintComponentId, issueCreateArgumentCaptor);
    }

    @Test
    public void testModelContentComplaint() {
        //given
        ModelGrade grade = createTestModelGrade(1L, FAKE_USER);
        //when
        Map<String, String> res = getStubServRequest(ComplaintTicketType.CONTENT_COMPLAINT, grade);
        //then
        ArgumentCaptor<IssueCreate> issueCreateArgumentCaptor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(startrekService).createTicket(issueCreateArgumentCaptor.capture());
        assertResponseHasTicketLink(res);

        String[] tags = (String[]) issueCreateArgumentCaptor.getValue().getValues().getOrElse("tags", null);
        assertEquals(1, tags.length);
        assertEquals("отзыв", tags[0]);
        assertBaseFields(grade, issueCreateArgumentCaptor, "TESTCONTENT", "task");
        assertIssueCreateCaptorHasEntry(issueCreateArgumentCaptor, "idOtzyva", grade.getId());
    }

    @Test
    public void testModelShopComplaint() {
        //given
        ModelGrade grade = createTestModelGrade(1L, FAKE_USER);
        //when
        Map<String, String> res = getStubServRequest(ComplaintTicketType.SHOP_COMPLAINT, grade);
        //then
        ArgumentCaptor<IssueCreate> issueCreateArgumentCaptor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(startrekService).createTicket(issueCreateArgumentCaptor.capture());
        assertResponseHasTicketLink(res);

        assertBaseFields(grade, issueCreateArgumentCaptor, "MARKETCLAIMS", "problem");
        assertIssueCreateCaptorHasEntry(issueCreateArgumentCaptor, "idOtzyva", grade.getId());
    }

    @Test
    public void testModelShopComplaintWithoutUserEmail() {
        //given
        ModelGrade grade = createTestModelGrade(1L, FAKE_USER);
        mockUserEmail(null);
        //when
        Map<String, String> res = getStubServRequest(ComplaintTicketType.SHOP_COMPLAINT, grade);
        //then
        ArgumentCaptor<IssueCreate> issueCreateArgumentCaptor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(startrekService).createTicket(issueCreateArgumentCaptor.capture());
        assertResponseHasTicketLink(res);
        userEmailIsNotPresent(issueCreateArgumentCaptor);
    }

    @Test
    public void testModelContentComplaintWithUserIDAndLogin() {
        //given
        ModelGrade grade = createTestModelGrade(1L, FAKE_USER);
        //when
        Map<String, String> res = getStubServRequest(ComplaintTicketType.CONTENT_COMPLAINT, grade);
        //then
        ArgumentCaptor<IssueCreate> issueCreateArgumentCaptor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(startrekService).createTicket(issueCreateArgumentCaptor.capture());

        assertIssueCreateCaptorHasEntry(issueCreateArgumentCaptor, "summary", String.format(
                "Ошибка на карточке товара %s - %s. Пользователь: %s (%s)", grade.getModelId(), grade.getName(),
                grade.getAuthorUid(), USER_LOGIN));
    }

    @Test
    public void testModelShopComplaintWithUserEmailForMarketClaimsQueue() {
        ModelGrade grade = createTestModelGrade(1L, FAKE_USER);
        ArgumentCaptor<IssueCreate> result = createModelComplaint(grade, SHOP_COMPLAINT, USER_EMAIL);
        checkUserEmail(USER_EMAIL, result);
        assertEquals(
                String.format(EXPECTED_DESCRIPTION_FOR_MODEL_MARKETCLAIMS_QUEUE, grade.getId(), grade.getId()),
                result.getValue().getValues().getOptional("description").get());
    }

    @Test
    public void testModelBeruComplaintWithUserEmailForMarketClaimsQueue() {
        ModelGrade grade = createTestModelGrade(1L, FAKE_USER);
        ArgumentCaptor<IssueCreate> result = createModelComplaint(grade, BERU_COMPLAINT, USER_EMAIL);
        userEmailIsNotPresent(result);
        assertEquals(
                String.format(EXPECTED_DESCRIPTION_FOR_MODEL, grade.getId(), grade.getId()),
                result.getValue().getValues().getOptional("description").get());
    }

    @Test
    public void testModelContentComplaintWithUserEmailForMarketClaimsQueue() {
        ModelGrade grade = createTestModelGrade(1L, FAKE_USER);
        ArgumentCaptor<IssueCreate> result = createModelComplaint(grade, CONTENT_COMPLAINT, USER_EMAIL);
        userEmailIsNotPresent(result);
        assertEquals(
                String.format(EXPECTED_DESCRIPTION_FOR_MODEL, grade.getId(), grade.getId()),
                result.getValue().getValues().getOptional("description").get());
    }

    private void checkUserEmail(String userEmail, ArgumentCaptor<IssueCreate> argumentCaptor) {
        assertEquals(userEmail, argumentCaptor.getValue().getValues().getOptional("emailFrom").get());
        assertEquals(userEmail, argumentCaptor.getValue().getValues().getOptional("customerEmail").get());
    }

    private void userEmailIsNotPresent(ArgumentCaptor<IssueCreate> argumentCaptor) {
        assertFalse(argumentCaptor.getValue().getValues().getOptional("emailFrom").isPresent());
        assertFalse(argumentCaptor.getValue().getValues().getOptional("customerEmail").isPresent());
    }

    private ArgumentCaptor<IssueCreate> createModelComplaint(ModelGrade grade, ComplaintTicketType type, String email) {
        mockUserEmail(email);
        getStubServRequest(type, grade);
        ArgumentCaptor<IssueCreate> issueCreateArgumentCaptor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(startrekService).createTicket(issueCreateArgumentCaptor.capture());
        return issueCreateArgumentCaptor;
    }

    @Test
    public void testModelBeruComplaint() {
        //given
        ModelGrade grade = createTestModelGrade(1L, FAKE_USER);
        //when
        Map<String, String> res = getStubServRequest(ComplaintTicketType.BERU_COMPLAINT, grade);
        //then
        ArgumentCaptor<IssueCreate> issueCreateArgumentCaptor = ArgumentCaptor.forClass(IssueCreate.class);
        verify(startrekService).createTicket(issueCreateArgumentCaptor.capture());
        assertResponseHasTicketLink(res);

        assertBaseFields(grade, issueCreateArgumentCaptor, "TESTBERU", "inbox");
        assertIssueCreateCaptorHasEntry(issueCreateArgumentCaptor, "idOtzyva", grade.getId());
    }

    private void assertResponseHasTicketLink(Map<String, String> res) {
        assertEquals(1, res.size());
        assertEquals(res.get("url"), startrekUrl + ISSUE_1.getKey());
    }

    @NotNull
    private Map<String, String> getStubServRequest(ComplaintTicketType complaintType, AbstractGrade grade) {
        return servantlet.createComplaint(complaintType.value(), grade.getId(), mock(HttpServletRequest.class));
    }

    private void assertCorrectTicketDataWhite(ShopGrade grade, Integer componentId,
                                              ArgumentCaptor<IssueCreate> captor) {
        assertCorrectShopTicketData(grade, componentId, captor);
        assertIssueCreateCaptorHasEntry(captor, "idMagazina", TEST_SHOP_ID);
        assertIssueCreateCaptorHasEntry(captor, "cvetPlosadki", "Белый");
    }

    private void assertCorrectTicketDataBlue(ShopGrade grade, Integer componentId,
                                             ArgumentCaptor<IssueCreate> captor) {
        assertCorrectShopTicketData(grade, componentId, captor);
        assertIssueCreateCaptorHasEntry(captor, "idMagazina", BERU_SHOP_ID);
        assertIssueCreateCaptorHasEntry(captor, "cvetPlosadki", "Синий");
    }

    private void assertCorrectShopTicketData(ShopGrade grade, Integer componentId,
                                             ArgumentCaptor<IssueCreate> captor) {
        assertBaseFields(grade, captor, "MARKETCLAIMS", startrekTicketType);
        assertIssueCreateCaptorHasEntry(
                captor, "idZakaza", Long.parseLong(TEST_ORDER_ID));
        if (grade.getAuthorUid() == SBER_ID) {
            // Для СберИД логина нет
            assertFalse(captor.getValue().getValues().getO("loginPolzovatela").isPresent());
        } else {
            assertIssueCreateCaptorHasEntry(captor, "loginPolzovatela", USER_LOGIN);
        }
        assertIssueCreateCaptorHasEntry(captor, "idOtzyva", grade.getId());
        assertTrue(captor.getValue().getValues().containsKeyTs("components"));
        assertSame(
                captor.getValue().getValues().getOrThrow("components").getClass().getComponentType(),
                long.class);
        assertEquals(((long[]) (captor.getValue().getValues().getOrThrow("components")))[0],
                (int) componentId);
    }

    private void assertBaseFields(AbstractGrade grade, ArgumentCaptor<IssueCreate> captor, String queue, String type) {
        Integer isAdvert =
            pgJdbcTemplate.queryForObject("SELECT IS_ADVERT FROM GRADE WHERE ID=?", Integer.class, grade.getId());
        assertEquals(Integer.valueOf(1), isAdvert);
        assertIssueCreateCaptorHasEntry(captor, "author", startrekUserLogin);
        assertIssueCreateCaptorHasEntry(captor, "queue", queue);
        assertIssueCreateCaptorHasEntry(captor, "type", type);
    }

    private void assertIssueCreateCaptorHasEntry(ArgumentCaptor<IssueCreate> captor, String key, Object value) {
        assertTrue(captor.getValue().getValues().containsEntry(key, value));
    }

    private ShopGrade createTestShopGrade(long shopId) {
        return createTestShopGrade(shopId, TEST_ORDER_ID, FAKE_USER);
    }

    private ShopGrade createTestShopGrade(long shopId, String orderId, final long authorId) {
        ShopGrade shopGrade = GradeCreator.constructShopGrade(shopId, authorId)
                .fillShopGradeCreationFields(orderId, Delivery.PICKUP);

        shopGrade.setText("Best you can afford");
        shopGrade.setPro("Fast, supports deep class III warp diving");
        shopGrade.setContra("Price is huge. Required experienced engineers to operate/repair");

        shopGrade.setAverageGrade(1);
        long id = gradeCreator.createGrade(shopGrade);
        shopGrade.setId(id);
        return shopGrade;
    }

    private ModelGrade createTestModelGrade(long modelId, final long authorId) {
        ModelGrade grade = GradeCreator.constructModelGrade(modelId, authorId);

        grade.setText("Best you can afford");
        grade.setPro("Fast, supports deep class III warp diving");
        grade.setContra("Price is huge. Required experienced engineers to operate/repair");

        grade.setAverageGrade(1);
        long id = gradeCreator.createGrade(grade);
        grade.setId(id);
        return grade;
    }

}

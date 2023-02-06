package ru.yandex.market.partner.mvc.controller.contact;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.core.balance.model.BalancePassportInfo;
import ru.yandex.market.core.passport.PassportService;
import ru.yandex.market.core.passport.model.UserInfo;
import ru.yandex.market.core.state.event.ContactChangesProtoLBEvent;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.data.ContactDataOuterClass;
import ru.yandex.market.mbi.data.GeneralData;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.test.util.JsonTestUtil.assertEquals;


/**
 * Тесты для {@link CommitPassportController}.
 *
 * @author Vladislav Bauer
 */
class CommitPassportControllerTest extends FunctionalTest {

    private static final long CORRECT_UID = 234;
    private static final long WRONG_UID = 567;

    @Autowired
    private BalanceContactService balanceContactService;

    @Autowired
    private PassportService passportService;

    @Autowired
    private LogbrokerEventPublisher<ContactChangesProtoLBEvent> logbrokerContactChangesEventPublisher;

    @BeforeEach
    void setUp() {
        when(logbrokerContactChangesEventPublisher.publishEventAsync(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0)));

    }

    @Test
    @DbUnitDataSet
    void testCommitPassportNoSuchDigest() {
        testError(CORRECT_UID, true, "[{\"messageCode\":\"no-such-digest\",\"statusCode\":400}]");
    }

    @Test
    @DbUnitDataSet(before = "testCommitPassportSuccess.before.csv")
    void testCommitPassportWrongUser() {
        testError(WRONG_UID, true, "[{\"messageCode\":\"wrong-user\",\"statusCode\":400}]");
    }

    @Test
    @DbUnitDataSet(
            before = "testCommitDeleteInvite.before.csv",
            after = "testCommitDeleteInvite.after.csv"
    )
    void testCommitDeleteInvite() {
        when(passportService.getUserInfo(CORRECT_UID))
                .thenReturn(new UserInfo(CORRECT_UID, null, null, "pupkin"));
        when(balanceContactService.getPassportByUid(CORRECT_UID)).thenReturn(
                BalancePassportInfo.builder().setUid(CORRECT_UID).setClientId(0L).build());
        final ResponseEntity<String> response = sendRequest(9000L, false);
        assertEquals(response, "[{\"value\":\"NO\"}]");
    }

    @Test
    @DbUnitDataSet(
            before = "testCommitPassportSuccess.before.csv",
            after = "testCommitPassportSuccess.after.csv"
    )
    void testCommitPassportSuccess() {
        //проверяем приглашение для магазинов
        testCommitPassport();
        verify(balanceContactService, times(1)).linkUid(anyLong(), anyLong(), anyLong(), anyLong());
    }

    @Test
    @DbUnitDataSet(
            before = "testCommitSmmPassportSuccess.before.csv",
            after = "testCommitSmmPassportSuccess.after.csv"
    )
    @DisplayName("Проверяем добавление роли SOCIAL_ECOM")
    void testCommitSmmPassportSuccess() {
        testCommitPassport();
    }

    @Test
    @DbUnitDataSet(
            before = "testCommitPassportToSupplierSuccess.before.csv",
            after = "testCommitPassportToSupplierSuccess.after.csv"
    )
    void testCommitPassportToSupplierSuccess() {
        //проверяем приглашение для поставщиков по взаимозачету
        testCommitPassport();
        verify(balanceContactService, never()).linkUid(anyLong(), anyLong(), anyLong(), anyLong());
    }

    private void testCommitPassport() {
        when(passportService.getUserInfo(CORRECT_UID))
                .thenReturn(new UserInfo(CORRECT_UID, null, null, "pupkin"));
        when(balanceContactService.getPassportByUid(CORRECT_UID)).thenReturn(
                BalancePassportInfo.builder().setUid(CORRECT_UID).setClientId(0L).build());
        final ResponseEntity<String> response =  sendRequest(CORRECT_UID, true);
        assertEquals(response, "[{\"value\":\"YES\"}]");

        ArgumentCaptor<ContactChangesProtoLBEvent> captor = ArgumentCaptor.forClass(ContactChangesProtoLBEvent.class);
        verify(logbrokerContactChangesEventPublisher).publishEventAsync(captor.capture());
        ContactChangesProtoLBEvent lbEvent = captor.getValue();
        ContactDataOuterClass.ContactData event = lbEvent.getPayload();
        assertThat(event.getContactId(), is(1L));
        assertThat(event.getGeneralInfo().getActionType(), is(GeneralData.ActionType.CREATE));
    }

    @Test
    @DbUnitDataSet(before = "testCommitPassportSuccess.before.csv")
    void testCommitPassportWithRepresentedClient() {
        when(passportService.getUserInfo(CORRECT_UID))
                .thenReturn(new UserInfo(CORRECT_UID, null, null, "pupkin"));
        when(balanceContactService.getPassportByUid(CORRECT_UID)).thenReturn(
                BalancePassportInfo.builder()
                        .setUid(CORRECT_UID)
                        .setClientId(0L)
                        .setRepresentedClientIds(List.of(100L))
                        .build());
        testError(CORRECT_UID, true, "[{\"messageCode\":\"already-linked\",\"statusCode\":400}]");
    }

    /**
     * Проверяет подтверждение привязки к бизнесу и клиенту владельца бизнеса.
     */
    @Test
    @DbUnitDataSet(
            before = "testCommitPassportSuccessForBusiness.before.csv",
            after = "testCommitPassportSuccessForBusiness.after.csv"
    )
    void testCommitPassportSuccessForBusiness() {
        when(passportService.getUserInfo(CORRECT_UID))
                .thenReturn(new UserInfo(CORRECT_UID, null, null, "pupkin"));
        when(balanceContactService.getPassportByUid(CORRECT_UID)).thenReturn(
                BalancePassportInfo.builder().setUid(CORRECT_UID).setClientId(0L).build());
        final ResponseEntity<String> response = sendRequest(CORRECT_UID, true);
        assertEquals(response, "[{\"value\":\"YES\"}]");
        verify(balanceContactService, times(1)).linkUid(anyLong(), anyLong(), anyLong(), anyLong());
    }

    @Test
    @DbUnitDataSet(
            before = "testCommitPassportToSupplierSuccess.before.csv",
            after = "testCommitPassportToSupplierSuccess.after.csv"
    )
    void testCommitPassportSuccessForBusinessNotLinkInBalance() {
        when(passportService.getUserInfo(CORRECT_UID))
                .thenReturn(new UserInfo(CORRECT_UID, null, null, "pupkin"));
        when(balanceContactService.getPassportByUid(CORRECT_UID)).thenReturn(
                BalancePassportInfo.builder().setUid(CORRECT_UID).setClientId(0L).build());
        final ResponseEntity<String> response = sendRequest(CORRECT_UID, true);
        assertEquals(response, "[{\"value\":\"YES\"}]");
        verify(balanceContactService, never()).linkUid(anyLong(), anyLong(), anyLong(), anyLong());
    }

    @Test
    @DbUnitDataSet(
            before = "testCommitPassportSuccess.before.csv",
            after = "testCommitPassportNotSuccess.after.csv"
    )
    void testCommitPassportNotAgreed() {
        final ResponseEntity<String> response = sendRequest(CORRECT_UID, false);
        assertEquals(response, "[{\"value\":\"NO\"}]");
    }


    /**
     * Приглашаемый клиент имеет связь в балансе с другой кампанией маркета, приглашение - marketOnly.
     */
    @Test
    @DbUnitDataSet(
            before = "testCommitPassportSuccess.marketOnly.before.csv",
            after = "testCommitPassportSuccess.after.csv"
    )
    void testCommitPassportMarketOnlyWithClient() {
        when(balanceContactService.getClientIdByUid(CORRECT_UID)).thenReturn(200L);
        when(passportService.getUserInfo(CORRECT_UID))
                .thenReturn(new UserInfo(CORRECT_UID, null, null, "pupkin"));
        final ResponseEntity<String> response = sendRequest(CORRECT_UID, true);
        assertEquals(response, "[{\"value\":\"YES\"}]");
    }


    /**
     * Приглашаемый клиент имеет связь в балансе, clientId совпадает с clientId кампании.
     */
    @Test
    @DbUnitDataSet(
            before = "testCommitPassportSuccess.before.csv",
            after = "testCommitPassportSuccess.after.csv"
    )
    void testCommitPassportWithClient() {
        when(balanceContactService.getClientIdByUid(CORRECT_UID)).thenReturn(100L);
        when(passportService.getUserInfo(CORRECT_UID))
                .thenReturn(new UserInfo(CORRECT_UID, null, null, "pupkin"));
        when(balanceContactService.getPassportByUid(CORRECT_UID)).thenReturn(
                BalancePassportInfo.builder().setUid(CORRECT_UID).setClientId(100L).build());
        final ResponseEntity<String> response = sendRequest(CORRECT_UID, true);
        assertEquals(response, "[{\"value\":\"YES\"}]");
    }

    /**
     * Приглашаемый клиент имеет связь в балансе, clientId не совпадает с clientId услуг бизнеса.
     */
    @Test
    @DbUnitDataSet(before = "testCommitPassportSuccessForBusiness.before.csv")
    void testCommitPassportWrongClientBusiness() {
        when(balanceContactService.getClientIdByUid(CORRECT_UID)).thenReturn(10L);
        when(balanceContactService.getPassportByUid(CORRECT_UID)).thenReturn(
                BalancePassportInfo.builder().setUid(CORRECT_UID).setClientId(10L).build());
        when(passportService.getUserInfo(CORRECT_UID))
                .thenReturn(new UserInfo(CORRECT_UID, null, null, "pupkin"));
        testError(CORRECT_UID, true, "[{\"messageCode\":\"already-linked\",\"statusCode\":400}]");
    }

    /**
     * Приглашаемый клиент имеет связь в балансе, clientId не совпадает с clientId кампании.
     */
    @Test
    @DbUnitDataSet(before = "testCommitPassportSuccess.before.csv")
    void testCommitPassportWrongClient() {
        when(balanceContactService.getClientIdByUid(CORRECT_UID)).thenReturn(10L);
        when(passportService.getUserInfo(CORRECT_UID))
                .thenReturn(new UserInfo(CORRECT_UID, null, null, "pupkin"));
        when(balanceContactService.getPassportByUid(CORRECT_UID)).thenReturn(
                BalancePassportInfo.builder().setUid(CORRECT_UID).setClientId(10L).build());
        testError(CORRECT_UID, true, "[{\"messageCode\":\"already-linked\",\"statusCode\":400}]");
    }

    private void testError(long uid, boolean agree, String error) {
        HttpClientErrorException.BadRequest badRequest = assertThrows(HttpClientErrorException.BadRequest.class,
                () -> sendRequest(uid, agree));

        String s = JsonTestUtil.parseJson(badRequest.getResponseBodyAsString())
                .getAsJsonObject()
                .get("errors")
                .toString();

        MbiAsserts.assertJsonEquals("" +
                        error,
                JsonTestUtil.parseJson(badRequest.getResponseBodyAsString())
                        .getAsJsonObject()
                        .get("errors")
                        .toString()
        );
    }

    private ResponseEntity<String> sendRequest(final long uid, final boolean agree) {
        final String commit = agree ? "1" : "";
        final String url = String.format(
                "%s/commitPassport?digest=yesiamdigest&confirm=1&commit=%s&format=json&_user_id=%d",
                baseUrl, commit, uid
        );
        return FunctionalTestHelper.get(url);
    }
}

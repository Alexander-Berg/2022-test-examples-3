package ru.yandex.crm.tests.delivery.accounts;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by agroroza on 17.05.2017.
 */
public class _2_1_Metrika extends MailProvider {
    public _2_1_Metrika() throws IOException {
    }

    @Test
    public void engHeaderTroubleshooting() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_1_Metrika/engHeaderTroubleshooting.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("engHeaderTroubleshooting", messageId);
    }

    @Test
    public void engEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_1_Metrika/engEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("engEmail", messageId);
    }

    @Test
    public void appHeaderQanda() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_1_Metrika/appHeaderQanda.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("appHeaderQanda", messageId);
    }

    @Test
    public void appEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_1_Metrika/appEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("appEmail", messageId);
    }

    @Test
    public void mobile_appHeaderDocpage() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_1_Metrika/mobile_appHeaderDocpage.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("mobile_appHeaderDocpage", messageId);
    }

    @Test
    public void mobile_app_engHeaderDocpage() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_1_Metrika/mobile_app_engHeaderDocpage.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("mobile_app_engHeaderDocpage", messageId);
    }

    @Test
    public void mobile_appEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_1_Metrika/mobile_appEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("mobile_appEmail", messageId);
    }

    @Test
    public void comm_servicesHeaderOther() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_1_Metrika/comm_servicesHeaderOther.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("comm_servicesHeaderOther", messageId);
    }

    @Test
    public void comm_servicesHeaderQanda() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_1_Metrika/comm_servicesHeaderQanda.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("comm_servicesHeaderQanda", messageId);
    }

    @Test
    public void comm_servicesHeaderReferrals() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_1_Metrika/comm_servicesHeaderReferrals.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("comm_servicesHeaderReferrals", messageId);
    }

    @Test
    public void install_settingsHeaderCounter() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_1_Metrika/install_settingsHeaderCounter.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("install_settingsHeaderCounter", messageId);
    }

    @Test
    public void monitoringHeaderOther() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_1_Metrika/monitoringHeaderOther.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("monitoringHeaderOther", messageId);
    }

    @Test
    public void otherEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_1_Metrika/otherEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("otherEmail", messageId);
    }

    @Test
    public void otherHeaderQanda() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_1_Metrika/otherHeaderQanda.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("otherHeaderQanda", messageId);
    }

    @Test
    public void reportsHeaderEcommerce() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_1_Metrika/reportsHeaderEcommerce.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("reportsHeaderEcommerce", messageId);
    }

    @Test
    public void reportsHeaderQanda() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_1_Metrika/reportsHeaderQanda.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("reportsHeaderQanda", messageId);
    }

    @Test
    public void target_callHeaderOther() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_1_Metrika/target_callHeaderOther.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("target_callHeaderOther", messageId);
    }

    @Test
    public void transfer_accessHeaderAccess() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_1_Metrika/transfer_accessHeaderAccess.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("transfer_accessHeaderAccess", messageId);
    }

    @Test
    public void webvisorHeaderMap() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_1_Metrika/webvisorHeaderMap.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("webvisorHeaderMap", messageId);
    }

    @Test
    public void webvisorHeaderQanda() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_1_Metrika/webvisorHeaderQanda.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("webvisorHeaderQanda", messageId);
    }

    @Test
    public void uaHeaderTroubleshooting() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_1_Metrika/uaHeaderTroubleshooting.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("uaHeaderTroubleshooting", messageId);
    }

    @Test
    public void uaEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_1_Metrika/uaEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("uaEmail", messageId);
    }

    @Test
    public void trHeaderTroubleshooting() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_1_Metrika/trHeaderTroubleshooting.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("trHeaderTroubleshooting", messageId);
    }

    @Test
    public void trEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_1_Metrika/trEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("trEmail", messageId);
    }

    @Test
    public void defEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_2_1_Metrika/defEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("defEmail", messageId);
    }


}

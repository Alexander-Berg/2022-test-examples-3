package ru.yandex.crm.tests.delivery.accounts;

import org.junit.Test;
import ru.yandex.core.MailProvider;

import java.io.IOException;

/**
 * Created by nasyrov on 11.03.2016.
 */
public class _1_6_Mediaproducts extends MailProvider {
    public _1_6_Mediaproducts() throws IOException {
    }

    @Test
    public void mkb() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_6_Mediaproducts/mkb.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("mkb", messageId);
    }

    @Test
    public void mkbEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_6_Mediaproducts/mkbEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("mkbEmail", messageId);
    }

    @Test
    public void paymentHeaderPromocode() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_6_Mediaproducts/paymentHeaderPromocode.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("mkbHeaderPromocode", messageId);
    }

    @Test
    public void paymentBodyBlock() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_6_Mediaproducts/paymentBodyBlock.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("paymentBodyBlock", messageId);
    }

    @Test
    public void paymentEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_6_Mediaproducts/paymentEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("paymentEmail", messageId);
    }

    @Test
    public void smartbannersBodyAsksmart() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_6_Mediaproducts/smartbannersBodyAsksmart.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("smartbannersBodyAsksmart", messageId);
    }

    @Test
    public void smartbannersEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_6_Mediaproducts/smartbannersEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("smartbannersEmail", messageId);
    }

    @Test
    public void directclonesBodyRaskleitdomeny() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_6_Mediaproducts/directclonesBodyRaskleitdomeny.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("directclonesBodyRaskleitdomeny", messageId);
    }

    @Test
    public void directclonesBodySkleitdomeny() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_6_Mediaproducts/directclonesBodySkleitdomeny.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("directclonesBodySkleitdomeny", messageId);
    }

    @Test
    public void directclonesEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_6_Mediaproducts/directclonesEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("directclonesEmail", messageId);
    }

    @Test
    public void doBodyFeed() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_6_Mediaproducts/doBodyFeed.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("doBodyFeed", messageId);
    }

    @Test
    public void doBodyIrrelevant() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_6_Mediaproducts/doBodyIrrelevant.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("doBodyIrrelevant", messageId);
    }

    @Test
    public void doBodyLongda() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_6_Mediaproducts/doBodyLongda.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("doBodyLongda", messageId);
    }

    @Test
    public void doBodyLowshows() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_6_Mediaproducts/doBodyLowshows.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("doBodyLowshows", messageId);
    }

    @Test
    public void doBodyOther() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_6_Mediaproducts/doBodyOther.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("doBodyOther", messageId);
    }
    @Test
    public void doBodySite() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_6_Mediaproducts/doBodySite.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("doBodySite", messageId);
    }

    @Test
    public void doEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_6_Mediaproducts/doEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("doEmail", messageId);
    }

    @Test
    public void videoEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_6_Mediaproducts/videoEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("videoEmail", messageId);
    }

    @Test
    public void videoHeader() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_6_Mediaproducts/videoHeader.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("videoHeader", messageId);
    }

    @Test
    public void broadmatchBodyAutogrowth() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_6_Mediaproducts/broadmatchBodyAutogrowth.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("broadmatchBodyAutogrowth", messageId);
    }

    @Test
    public void broadmatchBodySynonymcomplain() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_6_Mediaproducts/broadmatchBodySynonymcomplain.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("broadmatchBodySynonymcomplain", messageId);
    }

    @Test
    public void broadmatchBodySynonymlogic() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_6_Mediaproducts/broadmatchBodySynonymlogic.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("broadmatchBodySynonymlogic", messageId);
    }

    @Test
    public void broadmatchEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_6_Mediaproducts/broadmatchEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("broadmatchEmail", messageId);
    }

    @Test
    public void displayHeaderBannery() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_6_Mediaproducts/displayHeaderBannery.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("displayHeaderBannery", messageId);
    }

    @Test
    public void displayBodyAddretargeting() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_6_Mediaproducts/displayBodyAddretargeting.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("displayBodyAddretargeting", messageId);
    }

    @Test
    public void displayEmail() throws IOException {
        String messageId = mailSender().sendFile("./data/support/accounts/_1_6_Mediaproducts/displayEmail.eml", newMessageId(), newClientLogin(),newXuid());
        putStageMessageId("displayEmail", messageId);
    }
}

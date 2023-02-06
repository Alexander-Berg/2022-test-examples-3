const PO = require('../../../../../page-objects/pages/offer');

describe('Внешний оффер / Отправка кандидату', function() {
    it('Отправка оффера кандидату', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePage('', '/offers/80095/')
            .disableAnimations('*')
            .click(PO.pageOffer.actions.send())
            .waitForVisible(PO.offerActionSendDialog.form.fieldReceiver())
            .staticElement(PO.offerActionSendDialog())
            .assertView('offer_send_dialog_form', PO.offerActionSendDialog.form())
            .setSFieldValue(PO.offerActionSendDialog.form.fieldReceiver(), 'abc.123@mail.ru')
            .click(PO.offerActionSendDialog.form.submit())
            .waitForHidden(PO.offerActionSendDialog())
            .assertView('offer', PO.pageOffer());
    });
});

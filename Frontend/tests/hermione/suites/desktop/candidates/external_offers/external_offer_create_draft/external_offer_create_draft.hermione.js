const PO = require('../../../../../page-objects/pages/application');
const PO_OFFER = require('../../../../../page-objects/pages/offer');

describe('Внешний оффер / Создание.Черновик', function() {
    it('Создание оффера в статусе \'Черновик\'', function() {
        return this.browser
            .conditionalLogin('marat')
            .preparePage('', '/applications/1006117/')
            .disableAnimations('*')
            .waitForVisible(PO.pageApplication.actions())
            .assertView('application_actions', PO.pageApplication.actions())
            .click(PO.pageApplication.actions.menu())
            .waitForVisible(PO.applicationMenuPopup())
            .assertView('application_menu_popup', PO.applicationMenuPopup())
            .click(PO.actionCreateOffer())
            .waitForVisible(PO.applicationActionCreateOfferDialog.form())
            .waitForHidden(PO.applicationActionCreateOfferDialog.progress())
            .staticElement(PO.applicationActionCreateOfferDialog())
            .assertView('application_form_create_offer', PO.applicationActionCreateOfferDialog.form())
            .click(PO.applicationActionCreateOfferDialog.form.submit())
            .waitForHidden(PO.applicationActionCreateOfferDialog.form.submitDisabled())
            .waitForVisible(PO_OFFER.pageForm.form())
            .assertView('application_form_create_offer_success', PO_OFFER.pageForm());
    });
});

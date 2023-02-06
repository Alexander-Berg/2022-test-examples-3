const PO = require('../../../../../page-objects/pages/offer');

function prepare(browser, page) {
    return browser
        .conditionalLogin('marat')
        .preparePage('', page)
        .waitForVisible(PO.pageOffer.actions())
        .assertView('offer_actions', PO.pageOffer.actions())
        .click(PO.pageOffer.actions.approve())
        .waitForVisible(PO.offerActionApproveDialog.form())
        .waitForHidden(PO.offerActionApproveDialog.progress())
        .staticElement(PO.offerActionApproveDialog())
        .assertView('offer_form_approve', PO.offerActionApproveDialog.form());
}

describe('Внутренний оффер / Согласование', function() {
    it('Проверка обязательных полей', function() {
        return this.browser
            .then(() => prepare(this.browser, '/offers/81687/'))
            .click(PO.offerActionApproveDialog.form.submit())
            .waitForHidden(PO.offerActionApproveDialog.form.submitDisabled())
            .assertView('offer_form_approve_validation', PO.offerActionApproveDialog.form());
    });

    it('Заполнение формы \'Отправка оффера на согласование\'', function() {
        return this.browser
            .then(() => prepare(this.browser, '/offers/50002'))
            .setSuggestValue({
                block: PO.offerActionApproveDialog.form.fieldAbc.input(),
                menu: PO.abcSuggest.items(),
                text: 'Контест',
                item: PO.abcSuggest.contest(),
            })
            .setSelectValue({
                block: PO.offerActionApproveDialog.form.fieldProfessionalLevel(),
                menu: PO.proLevelSelect(),
                item: PO.proLevelSelect.internship(),
            })
            .setSelectValue({
                block: PO.offerActionApproveDialog.form.fieldSource(),
                menu: PO.sourceSelect(),
                item: PO.sourceSelect.internalReference(),
            })
            .click(PO.offerActionApproveDialog.form.submit())
            .waitForHidden(PO.offerActionApproveDialog.form())
            .assertView('offer_form_approve_success', PO.pageOffer());
    });
});
